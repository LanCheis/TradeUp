package com.example.tradeup.data.remote

import com.example.tradeup.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    fun saveUserProfile(user: User, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(user.uid).set(user)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun getUserProfile(uid: String, onResult: (User?) -> Unit) {
        usersCollection.document(uid).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.toObject(User::class.java)
                onResult(user)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun deleteUserProfile(uid: String, onComplete: (Boolean, String?) -> Unit) {
        usersCollection.document(uid).delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.message) }
    }
}
