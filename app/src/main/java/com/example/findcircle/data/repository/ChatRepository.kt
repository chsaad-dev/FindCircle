package com.example.findcircle.data.repository

import com.example.findcircle.domain.model.Chat
import com.example.findcircle.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore
) {
    private val chatsCollection = firestore.collection("chats")

    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = chatsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val chats = snapshot.toObjects(Chat::class.java)
                    trySend(chats)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    fun getChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(messages)
                }
            }
            
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String): Result<Unit> {
        return try {
            val messagesRef = chatsCollection.document(chatId).collection("messages")
            val newMessageRef = messagesRef.document()
            
            val message = Message(
                id = newMessageRef.id,
                chatId = chatId,
                senderId = senderId,
                text = text
            )

            newMessageRef.set(message).await()
            
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChat(userId1: String, userName1: String, userId2: String, userName2: String, postId: String): Result<String> {
        return try {
            val existingChats = chatsCollection
                .whereArrayContains("participantIds", userId1)
                .get().await()

            for (doc in existingChats) {
                val participantIds = doc.get("participantIds") as? List<String>
                val existingPostId = doc.getString("postId")
                if (participantIds != null && participantIds.contains(userId2) && existingPostId == postId) {
                    return Result.success(doc.id)
                }
            }

            val newChatRef = chatsCollection.document()
            val newChat = Chat(
                id = newChatRef.id,
                postId = postId,
                participantIds = listOf(userId1, userId2),
                participantNames = mapOf(userId1 to userName1, userId2 to userName2)
            )
            
            newChatRef.set(newChat).await()
            Result.success(newChatRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrGetChat(otherUserId: String, otherUserName: String, postId: String): Result<String> {
        val currentUserId = com.example.findcircle.di.ServiceLocator.auth.currentUser?.uid
        val currentUserName = com.example.findcircle.di.ServiceLocator.auth.currentUser?.displayName 
            ?: com.example.findcircle.di.ServiceLocator.auth.currentUser?.email 
            ?: "Unknown User"
            
        if (currentUserId == null) return Result.failure(Exception("User not logged in"))
        
        return createChat(currentUserId, currentUserName, otherUserId, otherUserName, postId)
    }

    suspend fun deleteChatsForUser(userId: String): Result<Unit> {
        return try {
            val userChats = chatsCollection.whereArrayContains("participantIds", userId).get().await()
            for (chatDoc in userChats) {
                val messages = chatDoc.reference.collection("messages").get().await()
                for (messageDoc in messages) {
                    messageDoc.reference.delete().await()
                }
                chatDoc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSingleChat(chatId: String): Result<Unit> {
        return try {
            val chatDocRef = chatsCollection.document(chatId)
            val messages = chatDocRef.collection("messages").get().await()
            for (messageDoc in messages) {
                messageDoc.reference.delete().await()
            }
            chatDocRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
