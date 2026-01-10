package com.example.elmnassri

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        // USE THE NEW LAYOUT HERE:
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind to the new IDs in item_order.xml
        private val priceView: TextView = itemView.findViewById(R.id.order_price)
        private val timeView: TextView = itemView.findViewById(R.id.order_time)
        private val workerView: TextView = itemView.findViewById(R.id.order_worker)

        fun bind(order: Order) {
            val date = Date(order.timestamp)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            priceView.text = "${String.format("%.2f", order.totalPrice)} TND"
            timeView.text = format.format(date)
            workerView.text = "Sold by: ${order.workerName}"
        }
    }

    class OrderComparator : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}