package com.example.findcircle.data.repository

import com.example.findcircle.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String, neighborhood: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")

            val user = User(
                uid = userId,
                name = name,
                email = email,
                neighborhood = neighborhood
            )

            firestore.collection("users").document(userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun logout() {
        auth.signOut()
    }
}
