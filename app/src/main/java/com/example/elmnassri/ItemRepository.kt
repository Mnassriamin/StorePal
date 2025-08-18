package com.example.elmnassri

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore // CORRECTED import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ItemRepository(private val itemDao: ItemDao) {

    // CORRECTED: Modern Firebase initialization
    private val firestoreDb = FirebaseFirestore.getInstance()
    private val itemsCollection = firestoreDb.collection("items")

    fun startRealtimeSync() {
        itemsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firebase", "Listen failed.", e)
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val item = doc.toObject(Item::class.java)
                    item?.let {
                        itemDao.upsertItem(it)
                    }
                }
            }
        }
    }

    fun getItems(searchQuery: String): Flow<List<Item>> {
        return itemDao.getItems(searchQuery)
    }

    suspend fun upsertItem(item: Item, imageUri: Uri?) {
        var itemToSave = item

        if (imageUri != null) {
            val downloadUrl = uploadImageToCloudinary(imageUri)
            if (downloadUrl != null) {
                itemToSave = item.copy(imageUri = downloadUrl)
                Log.d("Cloudinary", "Image uploaded successfully: $downloadUrl")
            } else {
                Log.e("Cloudinary", "Image upload failed")
            }
        }

        itemsCollection.document(itemToSave.barcode).set(itemToSave)
            .addOnSuccessListener { Log.d("Firebase", "Item successfully written online!") }
            .addOnFailureListener { e -> Log.w("Firebase", "Error writing item online", e) }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started...")
                }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    continuation.resume(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload error: ${error.description}")
                    continuation.resume(null)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    suspend fun deleteItem(item: Item) {
        // This does not delete the image from Cloudinary, only the database record.
        itemsCollection.document(item.barcode).delete()
        itemDao.deleteItem(item)
    }

    suspend fun getItemByBarcode(barcode: String): Item? {
        return itemDao.getItemByBarcode(barcode)
    }
}