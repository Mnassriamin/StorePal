package com.example.elmnassri

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CashierAdapter(
    private val onQuantityChange: (OrderItem, Int) -> Unit, // Callback for manual edits
    private val onDelete: (OrderItem) -> Unit
) : ListAdapter<OrderItem, CashierAdapter.CashierViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cashier, parent, false)
        return CashierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CashierViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CashierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.text_item_name)
        private val price: TextView = itemView.findViewById(R.id.text_item_price)
        private val totalRow: TextView = itemView.findViewById(R.id.text_total_row_price)
        private val qtyText: TextView = itemView.findViewById(R.id.text_quantity)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btn_plus)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btn_minus)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: OrderItem) {
            name.text = item.itemName
            price.text = "${String.format("%.2f", item.priceAtSale)} TND"
            totalRow.text = "Total: ${String.format("%.2f", item.priceAtSale * item.quantity)}"
            qtyText.text = item.quantity.toString()

            // 1. Plus Button
            btnPlus.setOnClickListener {
                onQuantityChange(item, item.quantity + 1)
            }

            // 2. Minus Button
            btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQuantityChange(item, item.quantity - 1)
                }
            }

            // 3. Click Number to Type manually
            qtyText.setOnClickListener {
                // We pass -1 to signal "Open Dialog"
                onQuantityChange(item, -1)
            }

            // 4. Delete
            btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem.barcode == newItem.barcode
        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem) = oldItem == newItem
    }
}