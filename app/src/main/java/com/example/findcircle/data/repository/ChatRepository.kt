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

    // Real-time flow of all chats the user is part of
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

    // Real-time flow of messages for a specific chat
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

            // Transaction or batch could be used here to update the parent Chat's lastMessage
            // For simplicity, we do it in two steps
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

    suspend fun createChat(userId1: String, userName1: String, userId2: String, userName2: String): Result<String> {
        return try {
            // Check if chat exists
            val existingChats = chatsCollection
                .whereArrayContains("participantIds", userId1)
                .get().await()

            for (doc in existingChats) {
                val participantIds = doc.get("participantIds") as? List<String>
                if (participantIds != null && participantIds.contains(userId2)) {
                    // Chat already exists
                    return Result.success(doc.id)
                }
            }

            // Create new chat
            val newChatRef = chatsCollection.document()
            val newChat = Chat(
                id = newChatRef.id,
                participantIds = listOf(userId1, userId2),
                participantNames = mapOf(userId1 to userName1, userId2 to userName2)
            )
            
            newChatRef.set(newChat).await()
            Result.success(newChatRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrGetChat(otherUserId: String, otherUserName: String): Result<String> {
        val currentUserId = com.example.findcircle.di.ServiceLocator.auth.currentUser?.uid
        val currentUserName = com.example.findcircle.di.ServiceLocator.auth.currentUser?.displayName 
            ?: com.example.findcircle.di.ServiceLocator.auth.currentUser?.email 
            ?: "Unknown User"
            
        if (currentUserId == null) return Result.failure(Exception("User not logged in"))
        
        return createChat(currentUserId, currentUserName, otherUserId, otherUserName)
    }

    suspend fun deleteChatsForUser(userId: String): Result<Unit> {
        return try {
            val userChats = chatsCollection.whereArrayContains("participantIds", userId).get().await()
            for (chatDoc in userChats) {
                // Delete all messages inside the chat
                val messages = chatDoc.reference.collection("messages").get().await()
                for (messageDoc in messages) {
                    messageDoc.reference.delete().await()
                }
                // Delete the chat itself
                chatDoc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
