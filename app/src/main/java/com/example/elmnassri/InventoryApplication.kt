package com.example.elmnassri

import android.app.Application
import com.cloudinary.android.MediaManager

class InventoryApplication : Application() {
    // Using 'lazy' so the database and repository are only created when they're needed
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { ItemRepository(database.itemDao()) }

    override fun onCreate() { // Add this whole function
        super.onCreate()

        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dgbw7vqfo"
        config["api_key"] = "566929444877999"
        config["api_secret"] = "b3i5sQV06GrKky3eBoP79ZIS-YI"
        MediaManager.init(this, config)
    }
}