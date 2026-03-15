package com.example.findcircle.data.repository

import com.example.findcircle.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            syncFcmToken(result.user?.uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String, neighborhood: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")

            val fcmToken = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()

            val user = User(
                uid = userId,
                name = name,
                email = email,
                neighborhood = neighborhood,
                fcmToken = fcmToken
            )

            firestore.collection("users").document(userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid ?: throw Exception("Google Sign In failed")
            val document = firestore.collection("users").document(userId).get().await()
            
            val fcmToken = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            
            if (!document.exists()) {
                val user = User(
                    uid = userId,
                    name = result.user?.displayName ?: "",
                    email = result.user?.email ?: "",
                    neighborhood = "",
                    fcmToken = fcmToken
                )
                firestore.collection("users").document(userId).set(user).await()
            } else {
                firestore.collection("users").document(userId).update("fcmToken", fcmToken).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncFcmToken(userId: String?) {
        if (userId == null) return
        try {
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            firestore.collection("users").document(userId).update("fcmToken", token).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reauthenticate(password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val email = user.email ?: throw Exception("User email not found")
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            user.verifyBeforeUpdateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
