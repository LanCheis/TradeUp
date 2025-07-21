package com.example.tradeup.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.*

object FirebaseStorageHelper {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    fun uploadImage(
        imageUri: Uri,
        folder: String = "listing_images",
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child("$folder/$fileName")

        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            onProgress?.invoke(progress)
        }.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }
}