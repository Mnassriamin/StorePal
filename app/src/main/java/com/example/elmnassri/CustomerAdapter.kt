package com.example.elmnassri

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CustomerAdapter(
    private val onClick: (Customer) -> Unit
) : ListAdapter<Customer, CustomerAdapter.CustomerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val initial: TextView = itemView.findViewById(R.id.text_initial)
        private val name: TextView = itemView.findViewById(R.id.text_name)
        private val phone: TextView = itemView.findViewById(R.id.text_phone)
        private val debt: TextView = itemView.findViewById(R.id.text_debt)

        fun bind(customer: Customer) {
            name.text = customer.name
            phone.text = customer.phoneNumber.ifEmpty { "No Phone" }
            debt.text = "${String.format("%.2f", customer.totalDebt)} TND"
            initial.text = customer.name.firstOrNull()?.toString()?.uppercase() ?: "?"

            itemView.setOnClickListener { onClick(customer) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Customer, newItem: Customer) = oldItem == newItem
    }
}