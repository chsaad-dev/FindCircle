package com.example.findcircle.data.repository

import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post): Result<Unit> {
        return try {
            val docRef = postsCollection.document()
            val newPost = post.copy(id = docRef.id)
            docRef.set(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentPosts(limit: Long = 50): Result<List<Post>> {
        return try {
            val snapshot = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val posts = snapshot.toObjects(Post::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(postId: String): Result<Post?> {
        return try {
            val snapshot = postsCollection.document(postId).get().await()
            val post = snapshot.toObject(Post::class.java)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time flow of comments for a specific post
    fun getPostComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(comments)
                }
            }
            
        awaitClose { subscription.remove() }
    }
    
    suspend fun addComment(postId: String, text: String, authorId: String, authorName: String): Result<Unit> {
        return try {
            val commentsRef = postsCollection.document(postId).collection("comments")
            val newCommentRef = commentsRef.document()
            
            val comment = Comment(
                id = newCommentRef.id,
                postId = postId,
                authorId = authorId,
                authorName = authorName,
                text = text
            )
            
            newCommentRef.set(comment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
