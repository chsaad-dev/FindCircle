package com.example.findcircle.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val neighborhood: String = "",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "",
    val bio: String = "",
    val fcmToken: String = ""
)

data class Post(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val type: PostType = PostType.LOST,
    val status: PostStatus = PostStatus.OPEN,
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateReported: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(), // ML tags
    @get:PropertyName("isUrgent")
    @set:PropertyName("isUrgent")
    var isUrgent: Boolean = false,
    val neighborhood: String = ""
)

object PostCategories {
    val ALL = listOf(
        "Electronics",
        "Pets",
        "Wallets/IDs",
        "Jewelry",
        "Clothing",
        "Other"
    )
}

enum class PostType {
    LOST, FOUND
}

enum class PostStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved")
}

data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedSearch(
    val id: String = "",
    val userId: String = "",
    val query: String = "",
    val radiusKm: Float = 10f,
    val category: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
