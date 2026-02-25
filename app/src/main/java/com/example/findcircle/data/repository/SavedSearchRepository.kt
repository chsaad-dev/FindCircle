package com.example.findcircle.data.repository

import com.example.findcircle.domain.model.SavedSearch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SavedSearchRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    
    private val searchesCollection = firestore.collection("saved_searches")

    suspend fun createSavedSearch(search: SavedSearch): Result<Unit> {
        return try {
            val docRef = searchesCollection.document()
            val newSearch = search.copy(id = docRef.id)
            docRef.set(newSearch).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSavedSearches(userId: String): Result<List<SavedSearch>> {
        return try {
            val snapshot = searchesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val searches = snapshot.toObjects(SavedSearch::class.java)
            Result.success(searches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllSavedSearches(): Result<List<SavedSearch>> {
        return try {
            val snapshot = searchesCollection.get().await()
            val searches = snapshot.toObjects(SavedSearch::class.java)
            Result.success(searches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSavedSearch(searchId: String): Result<Unit> {
        return try {
            searchesCollection.document(searchId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
