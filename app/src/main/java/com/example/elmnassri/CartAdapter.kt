package com.example.elmnassri

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(private val onDeleteClicked: (CartItem) -> Unit) :
    ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartItemComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, onDeleteClicked)
    }

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.cart_item_name)
        private val priceView: TextView = itemView.findViewById(R.id.cart_item_price)
        private val quantityView: TextView = itemView.findViewById(R.id.cart_item_quantity)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.btn_remove_item)

        fun bind(cartItem: CartItem, onDeleteClicked: (CartItem) -> Unit) {
            nameView.text = cartItem.item.name
            priceView.text = "${String.format("%.2f", cartItem.lineTotal)} TND"
            quantityView.text = "x${cartItem.quantity}"

            deleteBtn.setOnClickListener {
                onDeleteClicked(cartItem)
            }
        }
    }

    class CartItemComparator : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}