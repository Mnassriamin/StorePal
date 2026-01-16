package com.example.elmnassri

import android.app.Application
import com.cloudinary.android.MediaManager

class InventoryApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }

    // FIX: Removed 'database.userDao()' from this list
    val repository by lazy {
        ItemRepository(database.itemDao(), database.orderDao(), database.customerDao())
    }

    override fun onCreate() {
        super.onCreate()

        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dgbw7vqfo"
        config["api_key"] = "566929444877999"
        config["api_secret"] = "b3i5sQV06GrKky3eBoP79ZIS-YI"
        MediaManager.init(this, config)
    }
}