package com.example.elmnassri

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elmnassri.databinding.FragmentCashierBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class CashierFragment : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CashierViewModel by viewModels {
        CashierViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    // Camera Launcher
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.addItemByBarcode(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CashierAdapter(
            onQuantityChange = { item, newQty ->
                if (newQty == -1) {
                    // Show Dialog for Manual Entry
                    showQuantityDialog(item)
                } else {
                    // Standard +/- update
                    viewModel.updateQuantity(item, newQty)
                }
            },
            onDelete = { item ->
                viewModel.removeItem(item)
            }
        )

        binding.recyclerCashier.adapter = adapter
        binding.recyclerCashier.layoutManager = LinearLayoutManager(context)

        // Observe Cart Items (Updated to 'basket')
        lifecycleScope.launch {
            viewModel.basket.collect { items ->
                adapter.submitList(items)
            }
        }

        // Observe Total Price
        lifecycleScope.launch {
            viewModel.totalPrice.collect { total ->
                binding.textTotalPrice.text = "${String.format("%.2f", total)} TND"
            }
        }

        // Scan Button
        binding.btnScan.setOnClickListener {
            val options = ScanOptions()
            options.setOrientationLocked(true)
            options.setBeepEnabled(true)
            barcodeLauncher.launch(options)
        }

        // Checkout Button (Updated to 'submitOrder')
        binding.btnCheckout.setOnClickListener {
            viewModel.submitOrder()
            Toast.makeText(context, "Order Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQuantityDialog(item: OrderItem) {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter Quantity"

        AlertDialog.Builder(context)
            .setTitle("Set Quantity for ${item.itemName}")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val qtyStr = input.text.toString()
                if (qtyStr.isNotEmpty()) {
                    val qty = qtyStr.toInt()
                    // Call the function in ViewModel to update quantity
                    viewModel.updateQuantity(item, qty)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}