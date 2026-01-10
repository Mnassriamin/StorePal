package com.example.elmnassri

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ItemRepository(
    private val itemDao: ItemDao,
    private val orderDao: OrderDao,
    private val userDao: UserDao
) {

    private val firestoreDb = FirebaseFirestore.getInstance()
    private val itemsCollection = firestoreDb.collection("items")
    private val usersCollection = firestoreDb.collection("users")
    private val ordersCollection = firestoreDb.collection("orders")

    // --- USER / LOGIN LOGIC ---
    suspend fun login(pin: String): User? {
        return userDao.getUserByPin(pin)
    }

    suspend fun createDefaultAdmin() {
        if (userDao.getUserCount() == 0) {
            val admin = User(name = "Admin", pin = "0000", role = "admin")
            userDao.insertUser(admin)
            usersCollection.document(admin.pin).set(admin)
        }
    }

    // --- SYNC LOGIC ---
    fun startRealtimeSync() {
        // 1. Sync ITEMS
        itemsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    try {
                        val item = doc.toObject(Item::class.java)
                        item?.let { itemDao.upsertItem(it) }
                    } catch (e: Exception) {
                        Log.e("SYNC_ERROR", "Failed to sync item: ${doc.id}", e)
                    }
                }
            }
        }

        // 2. Sync USERS
        usersCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val user = doc.toObject(User::class.java)
                    user?.let { userDao.insertUser(it) }
                }
            }
        }

        // 3. Sync ORDERS (With Debugging)
        ordersCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e("SYNC_ERROR", "Order sync failed", e)
                return@addSnapshotListener
            }

            Log.d("SYNC_DEBUG", "Found ${snapshots!!.size()} orders in Firebase")

            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots.documents) {
                    try {
                        // A. Try to parse the order
                        val remoteOrder = doc.toObject(Order::class.java)

                        if (remoteOrder != null) {
                            // B. Check duplicates
                            val exists = orderDao.getOrderCountByTimestamp(remoteOrder.timestamp) > 0

                            if (!exists) {
                                Log.d("SYNC_DEBUG", "New Order found! Saving: ${remoteOrder.totalPrice}")

                                // C. Save Order
                                val localOrderId = orderDao.insertOrder(remoteOrder)

                                // D. Save Items
                                val itemsList = doc.get("items") as? List<HashMap<String, Any>>
                                if (itemsList != null) {
                                    val orderItemsToSave = itemsList.map { map ->
                                        OrderItem(
                                            orderId = localOrderId,
                                            barcode = map["barcode"] as? String ?: "",
                                            itemName = map["itemName"] as? String ?: "",
                                            priceAtSale = (map["priceAtSale"] as? Number)?.toDouble() ?: 0.0,
                                            quantity = (map["quantity"] as? Number)?.toInt() ?: 0
                                        )
                                    }
                                    orderDao.insertOrderItems(orderItemsToSave)
                                }
                            } else {
                                Log.d("SYNC_DEBUG", "Skipping duplicate order: ${remoteOrder.timestamp}")
                            }
                        } else {
                            Log.e("SYNC_ERROR", "Order is null after parsing: ${doc.id}")
                        }
                    } catch (exc: Exception) {
                        Log.e("SYNC_ERROR", "CRASH while syncing order: ${doc.id}", exc)
                    }
                }
            }
        }
    }

    // --- STANDARD FUNCTIONS (Unchanged) ---
    fun getItems(searchQuery: String): Flow<List<Item>> = itemDao.getItems(searchQuery)

    suspend fun upsertItem(item: Item, imageUri: Uri?) {
        var itemToSave = item
        if (imageUri != null) {
            val downloadUrl = uploadImageToCloudinary(imageUri)
            if (downloadUrl != null) itemToSave = item.copy(imageUri = downloadUrl)
        }
        itemsCollection.document(itemToSave.barcode).set(itemToSave)
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String? = suspendCoroutine { continuation ->
        MediaManager.get().upload(imageUri).callback(object : UploadCallback {
            override fun onStart(requestId: String) {}
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                continuation.resume(resultData["secure_url"] as? String)
            }
            override fun onError(requestId: String, error: ErrorInfo) {
                continuation.resume(null)
            }
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
        }).dispatch()
    }

    suspend fun deleteItem(item: Item) {
        itemsCollection.document(item.barcode).delete()
        itemDao.deleteItem(item)
    }

    suspend fun getItemByBarcode(barcode: String): Item? = itemDao.getItemByBarcode(barcode)

    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrdersByDate(startDate: Long, endDate: Long): Flow<List<Order>> = orderDao.getOrdersByDateRange(startDate, endDate)

    suspend fun saveOrder(totalPrice: Double, items: List<OrderItem>) {
        val currentWorkerName = UserSession.currentUser?.name ?: "Unknown"
        val newOrder = Order(totalPrice = totalPrice, workerName = currentWorkerName)

        val localOrderId = orderDao.insertOrder(newOrder)
        val itemsWithId = items.map { it.copy(orderId = localOrderId) }
        orderDao.insertOrderItems(itemsWithId)

        val orderData = hashMapOf(
            "orderId" to localOrderId,
            "timestamp" to newOrder.timestamp,
            "totalPrice" to newOrder.totalPrice,
            "workerName" to newOrder.workerName,
            "items" to itemsWithId
        )
        ordersCollection.add(orderData)
    }
}