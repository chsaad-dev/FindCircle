package com.example.findcircle.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val neighborhood: String = "",
    val profileImageUrl: String = "",
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
    val timestamp: Long = System.currentTimeMillis()
)

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
