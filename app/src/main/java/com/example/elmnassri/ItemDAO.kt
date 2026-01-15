package com.example.elmnassri

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // Note: Since 'id' is your primary key, this only updates if you pass the correct 'id'.
    // Passing id=0 will still create a new row (duplicate) unless handled in the Repository.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    // FIX 1: Use 'item_name' and 'barcode_data' to match your @ColumnInfo
    @Query("SELECT * FROM items_table WHERE item_name LIKE '%' || :searchQuery || '%' OR barcode_data LIKE '%' || :searchQuery || '%' ORDER BY item_name ASC")
    fun getItems(searchQuery: String): Flow<List<Item>>

    // FIX 2: Use 'barcode_data' to find by barcode
    @Query("SELECT * FROM items_table WHERE barcode_data = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): Item?
}