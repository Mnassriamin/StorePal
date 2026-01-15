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
import kotlin.text.set

class ItemRepository(
    private val itemDao: ItemDao,
    private val orderDao: OrderDao,
    private val userDao: UserDao,
    private val customerDao: CustomerDao
) {

    private val firestoreDb = FirebaseFirestore.getInstance()
    private val itemsCollection = firestoreDb.collection("items")
    private val usersCollection = firestoreDb.collection("users")
    private val ordersCollection = firestoreDb.collection("orders")
    private val customersCollection = firestoreDb.collection("customers")
    private val creditLogsCollection = firestoreDb.collection("credit_logs")

    // --- REALTIME SYNC ---
    fun startRealtimeSync() {
        // 1. ITEMS (The Fix for Duplicates)
        itemsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    try {
                        val remoteItem = doc.toObject(Item::class.java)
                        if (remoteItem != null && remoteItem.barcode.isNotEmpty()) {
                            // STOP DUPLICATES: Check if we have this barcode locally
                            val localItem = itemDao.getItemByBarcode(remoteItem.barcode)

                            if (localItem != null) {
                                // Exists! Force the remote item to use our LOCAL ID so it updates instead of adding new
                                val itemToUpdate = remoteItem.copy(id = localItem.id)
                                itemDao.upsertItem(itemToUpdate)
                            } else {
                                // New item! Insert normally
                                itemDao.upsertItem(remoteItem)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SYNC", "Item sync error: ${e.message}")
                    }
                }
            }
        }

        // 2. USERS (Staff) - FIXED DUPLICATES
        usersCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val remoteUser = doc.toObject(User::class.java)
                    if (remoteUser != null) {
                        // FIX: Check if we already have a user with this PIN
                        val localUser = userDao.getUserByPin(remoteUser.pin)

                        if (localUser != null) {
                            val userToUpdate = remoteUser.copy(id = localUser.id)
                            userDao.insertUser(userToUpdate)
                        } else {
                            // New User! Insert normally.
                            userDao.insertUser(remoteUser)
                        }
                    }
                }
            }
        }

        // 3. Orders
        ordersCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val remoteOrder = doc.toObject(Order::class.java)
                    if (remoteOrder != null) {
                        val exists = orderDao.getOrderCountByTimestamp(remoteOrder.timestamp) > 0
                        if (!exists) {
                            val localOrderId = orderDao.insertOrder(remoteOrder)
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
                        }
                    }
                }
            }
        }

        // 4. Customers
        customersCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val remoteCustomer = doc.toObject(Customer::class.java)
                    if (remoteCustomer != null) {
                        val localCustomer = customerDao.getCustomerByName(remoteCustomer.name)
                        if (localCustomer != null) {
                            val updated = localCustomer.copy(
                                totalDebt = remoteCustomer.totalDebt,
                                phoneNumber = remoteCustomer.phoneNumber,
                                id = localCustomer.id // Preserve Local ID
                            )
                            customerDao.updateCustomer(updated)
                        } else {
                            customerDao.insertCustomer(remoteCustomer)
                        }
                    }
                }
            }
        }

        // 5. Credit Logs
        creditLogsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                for (doc in snapshots!!.documents) {
                    val remoteLog = doc.toObject(CreditLog::class.java)
                    if (remoteLog != null) {
                        val exists = customerDao.getCreditLogByDetails(remoteLog.customerId, remoteLog.timestamp) != null
                        if (!exists) {
                            customerDao.insertCreditLog(remoteLog.copy(id = 0))
                        }
                    }
                }
            }
        }
    }
// --- WORKER MANAGEMENT ---

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun addWorker(name: String, pin: String, role: String) {
        val newUser = User(name = name, pin = pin, role = role)
        // 1. Save Local
        userDao.insertUser(newUser)
        // 2. Save Cloud (Using PIN as ID to ensure uniqueness in login)
        usersCollection.document(pin).set(newUser)
    }

    suspend fun deleteWorker(user: User) {
        // 1. Delete Local
        userDao.deleteUser(user)
        // 2. Delete Cloud
        usersCollection.document(user.pin).delete()
    }
    // --- ITEM ACTIONS (Updated with Duplicate Check) ---

    suspend fun upsertItem(item: Item, imageUri: Uri?) {
        var itemToSave = item

        // 1. Upload Image (Optional)
        if (imageUri != null) {
            try {
                val downloadUrl = uploadImageToCloudinary(imageUri)
                if (downloadUrl != null) itemToSave = item.copy(imageUri = downloadUrl)
            } catch (e: Exception) {
                Log.e("REPO", "Image upload failed", e)
            }
        }

        // 2. CHECK LOCAL EXISTENCE
        // Even if we are adding "new", check if the barcode exists locally first.
        // If it does, grab its ID so we UPDATE instead of INSERT.
        val existingItem = itemDao.getItemByBarcode(itemToSave.barcode)
        if (existingItem != null) {
            itemToSave = itemToSave.copy(id = existingItem.id)
        }

        // 3. Save Local
        itemDao.upsertItem(itemToSave)

        // 4. Save Cloud (Document ID = Barcode)
        itemsCollection.document(itemToSave.barcode).set(itemToSave)
            .addOnFailureListener { Log.e("REPO", "Cloud sync failed", it) }
    }

    // --- STANDARD FUNCTIONS (No changes below) ---

    suspend fun login(pin: String): User? = userDao.getUserByPin(pin)

    suspend fun createDefaultAdmin() {
        if (userDao.getUserCount() == 0) {
            val admin = User(name = "Admin", pin = "0000", role = "admin")
            userDao.insertUser(admin)
            usersCollection.document(admin.pin).set(admin)
        }
    }

    fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()
    fun getCustomerLogs(customerId: Int): Flow<List<CreditLog>> = customerDao.getLogsForCustomer(customerId)

    suspend fun addCustomer(name: String, phone: String) {
        val newCustomer = Customer(name = name, phoneNumber = phone)
        customerDao.insertCustomer(newCustomer)
        customersCollection.document(name).set(newCustomer)
    }

    suspend fun payDebt(customer: Customer, amountPaid: Double) {
        val newBalance = customer.totalDebt - amountPaid
        val updatedCustomer = customer.copy(totalDebt = newBalance)
        customerDao.updateCustomer(updatedCustomer)

        val log = CreditLog(customerId = customer.id, amount = -amountPaid, type = "PAYMENT")
        customerDao.insertCreditLog(log)

        customersCollection.document(customer.name).update("totalDebt", newBalance)
        creditLogsCollection.add(log)
    }

    suspend fun saveOrder(totalPrice: Double, items: List<OrderItem>, customerId: Int? = null) {
        val currentWorkerName = UserSession.currentUser?.name ?: "Unknown"
        val isKridi = customerId != null

        val newOrder = Order(
            totalPrice = totalPrice,
            workerName = currentWorkerName,
            customerId = customerId,
            isKridi = isKridi
        )
        val localOrderId = orderDao.insertOrder(newOrder)
        val itemsWithId = items.map { it.copy(orderId = localOrderId) }
        orderDao.insertOrderItems(itemsWithId)

        if (customerId != null) {
            val customer = customerDao.getCustomerById(customerId)
            if (customer != null) {
                val newDebt = customer.totalDebt + totalPrice
                customerDao.updateCustomer(customer.copy(totalDebt = newDebt))

                val log = CreditLog(
                    customerId = customer.id,
                    amount = totalPrice,
                    type = "PURCHASE",
                    orderId = localOrderId
                )
                customerDao.insertCreditLog(log)

                customersCollection.document(customer.name).update("totalDebt", newDebt)
                creditLogsCollection.add(log)
            }
        }

        val orderData = hashMapOf(
            "orderId" to localOrderId,
            "timestamp" to newOrder.timestamp,
            "totalPrice" to newOrder.totalPrice,
            "workerName" to newOrder.workerName,
            "customerId" to customerId,
            "items" to itemsWithId
        )
        ordersCollection.add(orderData)
    }

    fun getItems(searchQuery: String): Flow<List<Item>> = itemDao.getItems(searchQuery)

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
}


