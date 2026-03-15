const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Trigger: When a new message is added to /chats/{chatId}/messages/{messageId}
 * Action: Look up the receiver's FCM token and send a push notification.
 */
exports.sendChatNotification = functions.firestore
    .document("chats/{chatId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
      const messageData = snap.data();

      const senderId = messageData.senderId;
      const text = messageData.text;

      // 1. Fetch the Chat document to find the receiverId
      const chatId = context.params.chatId;
      const chatRef = admin.firestore().collection("chats").doc(chatId);
      const chatDoc = await chatRef.get();

      if (!chatDoc.exists) {
        console.log("No chat found for ID:", chatId);
        return null;
      }

      const chatData = chatDoc.data();
      
      // Determine receiver (the person who is NOT the sender)
      const participants = chatData.participantIds || [];
      const receiverId = participants.find((id) => id !== senderId);

      if (!receiverId) {
        console.log("No receiver found in chat participants.");
        return null;
      }

      // 2. Fetch Receiver User Profile to get fcmToken
      const userDoc = await admin.firestore().collection("users").doc(receiverId).get();
      if (!userDoc.exists) {
        console.log("Receiver user not found.", receiverId);
        return null;
      }
      const receiverData = userDoc.data();
      const fcmToken = receiverData.fcmToken;

      if (!fcmToken) {
        console.log("Receiver has no FCM token. Aborting push.", receiverId);
        return null;
      }

      // 3. Fetch Sender User Profile for name
      const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
      const senderName = senderDoc.exists ? senderDoc.data().name : "Someone";

      // 4. Construct Payload (Notification + Data)
      const payload = {
        notification: {
          title: senderName,
          body: text.length > 50 ? text.substring(0, 50) + "..." : text,
        },
        data: {
          title: senderName,
          body: text.length > 50 ? text.substring(0, 50) + "..." : text,
          type: "CHAT",
          chatId: chatId,
          otherUserName: senderName,
        },
        token: fcmToken,
      };

      // 5. Send Notification via FCM
      try {
        const response = await admin.messaging().send(payload);
        console.log("Successfully sent chat message:", response);
      } catch (error) {
        console.log("Error sending chat message:", error);
      }

      return null;
    });

/**
 * Trigger: When a new post is marked as `isUrgent` (Amber Alert)
 * Action: Send a notification to users in the same neighborhood.
 */
exports.sendAmberAlert = functions.firestore
    .document("posts/{postId}")
    .onCreate(async (snap, context) => {
      const postData = snap.data();

      if (!postData.isUrgent) {
        return null; // Don't alert for normal posts
      }

      const neighborhood = postData.neighborhood;
      const category = postData.category || "Item";
      
      // Find all users in this neighborhood
      const usersQuery = await admin.firestore()
          .collection("users")
          .where("neighborhood", "==", neighborhood)
          .get();

      const tokens = [];
      usersQuery.forEach((doc) => {
        const userData = doc.data();
        if (userData.fcmToken && doc.id !== postData.ownerId) { // don't notify the person who posted it
          tokens.push(userData.fcmToken);
        }
      });

      if (tokens.length === 0) {
        console.log("No users with tokens found in neighborhood:", neighborhood);
        return null;
      }

      const payload = {
        notification: {
          title: `🚨 URGENT: Lost ${category}`,
          body: postData.title,
        },
        data: {
          title: `🚨 URGENT: Lost ${category}`,
          body: postData.title,
          type: "AMBER_ALERT",
          postId: context.params.postId,
        },
        tokens: tokens, // Multicast message
      };

      try {
        const response = await admin.messaging().sendMulticast(payload);
        console.log(`Successfully sent ${response.successCount} Amber Alerts`);
      } catch (error) {
        console.error("Error sending Amber Alert multicast:", error);
      }

      return null;
    });

// Helper: Haversine distance formula
function getDistanceFromLatLonInKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Radius of the earth in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // Distance in km
}

/**
 * Trigger: When a new post is created
 * Action: Match against active saved searches and fire Data-Only push notifications.
 */
exports.sendMatchNotification = functions.firestore
    .document("posts/{postId}")
    .onCreate(async (snap, context) => {
      const postData = snap.data();

      // Only alert on newly created OPEN posts
      if (postData.status !== "OPEN") {
        return null;
      }

      const postLat = postData.latitude;
      const postLon = postData.longitude;
      const postOwnerId = postData.ownerId;
      const postTitle = postData.title || "";
      const postDescription = postData.description || "";
      const postTags = postData.tags || [];
      const postCategory = postData.category;

      // Fetch all active saved searches from all users
      const searchesQuery = await admin.firestore().collection("saved_searches").get();
      const notificationsToSend = [];

      searchesQuery.forEach((doc) => {
        const search = doc.data();

        // 1. Never notify the post owner about their own post
        if (search.userId === postOwnerId) return;

        // 2. Geographic distance check (must be within user's configured radius)
        const distance = getDistanceFromLatLonInKm(postLat, postLon, search.latitude, search.longitude);
        if (distance > search.radiusKm) return;

        // 3. Category match (if search specifies a category)
        if (search.category && search.category !== postCategory) return;

        // 4. Keyword text search match
        const query = (search.query || "").toLowerCase();
        let matchesQuery = false;
        
        if (!query) {
            matchesQuery = true; // Blank search queries match everything in the radius/category
        } else {
            matchesQuery = postTitle.toLowerCase().includes(query) ||
                           postDescription.toLowerCase().includes(query) ||
                           postTags.some((tag) => tag.toLowerCase().includes(query));
        }

        if (!matchesQuery) return;

        // Survived all checks -> Queue notification!
        notificationsToSend.push({
            userId: search.userId,
            postId: context.params.postId,
            searchId: doc.id
        });
      });

      if (notificationsToSend.length === 0) {
        console.log("No matches found for new post:", context.params.postId);
        return null;
      }

      // Dispatch specific notifications to all matched users concurrently
      const sendPromises = notificationsToSend.map(async (match) => {
          const userDoc = await admin.firestore().collection("users").doc(match.userId).get();
          if (!userDoc.exists) return null;
          
          const userData = userDoc.data();
          if (!userData.fcmToken) return null;

          // Notification + Data Payload guarantees system delivery
          const payload = {
            notification: {
              title: "Potential Match Found! 🎯",
              body: `A new ${postData.type.toLowerCase()} item was just posted that matches your saved search filters.`,
            },
            data: {
              title: "Potential Match Found! 🎯",
              body: `A new ${postData.type.toLowerCase()} item was just posted that matches your saved search filters.`,
              type: "MATCH",
              postId: match.postId,
            },
            token: userData.fcmToken,
          };

          return admin.messaging().send(payload).catch(err => {
              console.error(`Failed to send match to ${match.userId}`, err);
              return null;
          });
      });

      await Promise.all(sendPromises);
      console.log(`Successfully dispatched ${sendPromises.length} Match push alerts.`);
      return null;
    });
