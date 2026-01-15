package com.example.elmnassri

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elmnassri.databinding.FragmentCashierBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.launch

class CashierFragment : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CashierViewModel by viewModels {
        CashierViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) binding.embeddedScanner.resume()
        }

    private var lastScanTime: Long = 0
    private val SCAN_DELAY_MS = 1500L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- SCANNER SETUP ---
        val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A)
        binding.embeddedScanner.barcodeView.decoderFactory = DefaultDecoderFactory(formats)

        binding.embeddedScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScanTime > SCAN_DELAY_MS) {
                        lastScanTime = currentTime
                        // Optional: Add haptic feedback here
                        viewModel.addItemByBarcode(it.text)
                    }
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // --- ADAPTER ---
        val adapter = CashierAdapter(
            onQuantityChange = { item, newQty ->
                if (newQty == -1) showQuantityDialog(item)
                else viewModel.updateQuantity(item, newQty)
            },
            onDelete = { item -> viewModel.removeItem(item) }
        )

        binding.recyclerCashier.adapter = adapter
        binding.recyclerCashier.layoutManager = LinearLayoutManager(context)

        // --- OBSERVABLES ---
        lifecycleScope.launch {
            viewModel.basket.collect { items ->
                adapter.submitList(items)
                if (items.isNotEmpty()) binding.recyclerCashier.smoothScrollToPosition(items.size - 1)
            }
        }

        lifecycleScope.launch {
            viewModel.totalPrice.collect { total ->
                binding.textTotalPrice.text = "${String.format("%.2f", total)} TND"
            }
        }

        // --- FIX: WAKE UP THE CUSTOMER LIST ---
        lifecycleScope.launch {
            viewModel.allCustomers.collect {
                // Do nothing, just keeping the connection alive
            }
        }

        // --- BUTTONS ---
        binding.btnCheckout.setOnClickListener {
            viewModel.submitOrder(null)
            Toast.makeText(context, "Cash Payment Received", Toast.LENGTH_SHORT).show()
        }

        binding.btnKridi.setOnClickListener {
            showCustomerSelector()
        }

        // Manual Item Search Button
        view.findViewById<View>(R.id.btn_manual_search).setOnClickListener {
            showItemSearchDialog()
        }
    }

    // --- MANUAL ITEM SEARCH DIALOG ---
    private fun showItemSearchDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_search_item, null)
        val inputSearch = view.findViewById<EditText>(R.id.input_item_search)
        val listView = view.findViewById<ListView>(R.id.list_search_results)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup Item Adapter (Name + Price)
        val adapter = object : ArrayAdapter<Item>(requireContext(), android.R.layout.simple_list_item_2, android.R.id.text1) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent)
                val item = getItem(position)

                val text1 = row.findViewById<TextView>(android.R.id.text1)
                val text2 = row.findViewById<TextView>(android.R.id.text2)

                text1.text = item?.name
                text1.textSize = 18f
                text1.setTypeface(null, android.graphics.Typeface.BOLD)

                text2.text = String.format("%.2f TND", item?.price)
                text2.setTextColor(Color.parseColor("#2E7D32")) // Dark Green Price

                return row
            }
        }
        listView.adapter = adapter

        // Live Search Logic
        inputSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                lifecycleScope.launch {
                    // Make sure 'searchItems' exists in ViewModel, or use repository.getItems(query)
                    viewModel.searchItems(query).collect { items ->
                        adapter.clear()
                        adapter.addAll(items)
                    }
                }
            }
        })

        // Add Clicked Item to Cart
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            if (selectedItem != null) {
                viewModel.addItemByBarcode(selectedItem.barcode)
                Toast.makeText(context, "${selectedItem.name} added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
        inputSearch.requestFocus()
    }

    // --- CUSTOMER SELECTOR ---
    private fun showCustomerSelector() {
        val allCustomers = viewModel.allCustomers.value

        val view = layoutInflater.inflate(R.layout.dialog_customer_selector, null)
        val searchInput = view.findViewById<EditText>(R.id.input_search)
        val listView = view.findViewById<ListView>(R.id.list_customers)
        val btnAddNew = view.findViewById<Button>(R.id.btn_add_new)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var displayedList = allCustomers.toMutableList()

        val adapter = object : ArrayAdapter<Customer>(requireContext(), R.layout.item_customer_selector_row, displayedList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: layoutInflater.inflate(R.layout.item_customer_selector_row, parent, false)
                val customer = getItem(position)!!

                val nameText = row.findViewById<TextView>(R.id.text_name)
                val avatarText = row.findViewById<TextView>(R.id.text_avatar)

                nameText.text = customer.name
                val initial = if (customer.name.isNotEmpty()) customer.name.first().uppercase() else "?"
                avatarText.text = initial

                return row
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedCustomer = adapter.getItem(position)!!
            viewModel.submitOrder(selectedCustomer.id)
            Toast.makeText(context, "Added to ${selectedCustomer.name}'s Tab", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnAddNew.setOnClickListener {
            dialog.dismiss()
            showAddCustomerDialog()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()

                adapter.clear()
                if (query.isEmpty()) {
                    adapter.addAll(allCustomers)
                } else {
                    val filtered = allCustomers.filter { it.name.lowercase().contains(query) }
                    adapter.addAll(filtered)
                }
                adapter.notifyDataSetChanged()
            }
        })

        dialog.show()
    }

    // --- ADD CUSTOMER ---
    private fun showAddCustomerDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_customer, null)
        val inputName = view.findViewById<EditText>(R.id.input_name)
        val inputPhone = view.findViewById<EditText>(R.id.input_phone)
        val btnSave = view.findViewById<Button>(R.id.btn_save_customer)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val name = inputName.text.toString()
            val phone = inputPhone.text.toString()

            if (name.isNotEmpty()) {
                viewModel.addNewCustomer(name, phone)
                Toast.makeText(context, "Customer Created!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                view.postDelayed({
                    showCustomerSelector()
                }, 300)
            } else {
                Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // --- QUANTITY DIALOG ---
    private fun showQuantityDialog(item: OrderItem) {
        val view = layoutInflater.inflate(R.layout.dialog_quantity, null)

        val textName = view.findViewById<TextView>(R.id.text_item_name)
        val inputQty = view.findViewById<EditText>(R.id.input_quantity)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        textName.text = "Update Quantity: ${item.itemName}"
        inputQty.setText(item.quantity.toString())
        inputQty.selectAll()

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val qtyStr = inputQty.text.toString()
            if (qtyStr.isNotEmpty()) {
                val qty = qtyStr.toInt()
                if (qty > 0) {
                    viewModel.updateQuantity(item, qty)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Quantity must be > 0", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.embeddedScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.embeddedScanner.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}