package com.example.elmnassri

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elmnassri.databinding.FragmentKridiBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat

class KridiFragment : Fragment() {

    private var _binding: FragmentKridiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: KridiViewModel by viewModels {
        KridiViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKridiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CustomerAdapter { customer ->
            showCustomerDetailSheet(customer)
        }

        binding.recyclerCustomers.adapter = adapter
        binding.recyclerCustomers.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            viewModel.allCustomers.collect { customers ->
                adapter.submitList(customers)
            }
        }
    }

    private fun showCustomerDetailSheet(customer: Customer) {
        viewModel.selectCustomer(customer)

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_kridi_details, null)
        dialog.setContentView(view)

        val title = view.findViewById<TextView>(R.id.sheet_title)
        val debt = view.findViewById<TextView>(R.id.sheet_debt)
        val listHistory = view.findViewById<ListView>(R.id.list_history)
        val btnPay = view.findViewById<View>(R.id.btn_pay_debt)

        title.text = customer.name
        debt.text = "${String.format("%.2f", customer.totalDebt)} TND"

        // --- CUSTOM ADAPTER FOR HISTORY (The Key Fix) ---
        lifecycleScope.launch {
            viewModel.customerLogs.collect { logs ->
                // Create a custom adapter to use our new item_credit_log.xml
                val historyAdapter = object : ArrayAdapter<CreditLog>(requireContext(), R.layout.item_credit_log, logs) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val row = convertView ?: layoutInflater.inflate(R.layout.item_credit_log, parent, false)
                        val log = getItem(position)!!

                        val txtDate = row.findViewById<TextView>(R.id.text_date)
                        val txtType = row.findViewById<TextView>(R.id.text_type)
                        val txtAmount = row.findViewById<TextView>(R.id.text_amount)
                        val imgArrow = row.findViewById<ImageView>(R.id.img_arrow)

                        txtDate.text = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(log.timestamp))

                        if (log.amount > 0) {
                            // PURCHASE (Red)
                            txtType.text = "Added to Tab"
                            txtAmount.text = "+ ${String.format("%.2f", log.amount)}"
                            txtAmount.setTextColor(Color.parseColor("#D32F2F")) // Red
                            imgArrow.setImageResource(android.R.drawable.arrow_up_float)
                            imgArrow.setColorFilter(Color.parseColor("#D32F2F"))
                        } else {
                            // PAYMENT (Green)
                            txtType.text = "Payment Received"
                            txtAmount.text = "- ${String.format("%.2f", kotlin.math.abs(log.amount))}"
                            txtAmount.setTextColor(Color.parseColor("#2E7D32")) // Green
                            imgArrow.setImageResource(android.R.drawable.arrow_down_float)
                            imgArrow.setColorFilter(Color.parseColor("#2E7D32"))
                        }
                        return row
                    }
                }
                listHistory.adapter = historyAdapter
            }
        }

        btnPay.setOnClickListener {
            showPaymentDialog()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPaymentDialog() {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount Paid"
        input.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_edit_text)
        input.setPadding(40,40,40,40)

        // Add padding around the input
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Accept Payment")
            .setView(container) // Use the nice container
            .setPositiveButton("Confirm") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDouble()
                    viewModel.acceptPayment(amount)
                    Toast.makeText(context, "Payment Recorded", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}