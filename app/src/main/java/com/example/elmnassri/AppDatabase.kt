package com.example.elmnassri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ERROR WAS HERE: You must list all 3 entities inside the brackets [ ]
@Database(entities = [Item::class, Order::class, OrderItem::class, User::class, Customer::class, CreditLog::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile







        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "store_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}