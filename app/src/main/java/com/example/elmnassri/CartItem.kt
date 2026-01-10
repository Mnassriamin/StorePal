package com.example.elmnassri

data class CartItem(
    val item: Item,
    var quantity: Int = 1
) {
    val lineTotal: Double
        get() = item.price * quantity
}