package com.example.findcircle.di

import com.example.findcircle.data.repository.AuthRepository
import com.example.findcircle.data.repository.ChatRepository
import com.example.findcircle.data.repository.ImageRepository
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.data.repository.SavedSearchRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object ServiceLocator {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    val authRepository: AuthRepository by lazy { AuthRepository(auth, firestore) }
    val postRepository: PostRepository by lazy { PostRepository(firestore) }
    val imageRepository: ImageRepository by lazy { ImageRepository(storage) }
    val chatRepository: ChatRepository by lazy { ChatRepository(firestore) }
    val savedSearchRepository: SavedSearchRepository by lazy { SavedSearchRepository(firestore) }
}
