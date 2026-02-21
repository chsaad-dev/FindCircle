package com.example.findcircle.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ImageRepository(
    private val storage: FirebaseStorage
) {
    suspend fun uploadImage(imageUri: Uri, folderName: String): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val fileRef = storage.reference.child("$folderName/$fileName")
            
            // Upload the file
            fileRef.putFile(imageUri).await()
            
            // Get the download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
