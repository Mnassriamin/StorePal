package com.example.elmnassri

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.elmnassri.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels {
        AdminViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = OrderAdapter()
        binding.recyclerOrders.adapter = adapter
        binding.recyclerOrders.layoutManager = LinearLayoutManager(context)

        // 1. Observe Monthly Income (Dynamic)
        lifecycleScope.launch {
            viewModel.monthlyIncome.collect { income ->
                binding.textMonthIncome.text = "${String.format("%.2f", income)} TND"
            }
        }

        // 2. Observe Month Name (e.g., change label to "Revenue (January)")
        lifecycleScope.launch {
            viewModel.currentMonthName.collect { name ->
                // You might need to give the small text view an ID in the XML first
                // Let's assume you added android:id="@+id/label_month_title" to the "Total Revenue (This Month)" TextView
                binding.labelMonthTitle.text = "Total Revenue ($name)"
            }
        }

        // 3. Observe Daily Income
        lifecycleScope.launch {
            viewModel.selectedDateIncome.collect { income ->
                binding.textSelectedDateTotal.text = "${String.format("%.2f", income)} TND"
            }
        }

        // 4. Observe Orders List
        lifecycleScope.launch {
            viewModel.ordersForSelectedDate.collect { orders ->
                adapter.submitList(orders)
            }
        }

        // 5. Calendar Click
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            viewModel.setDate(year, month, dayOfMonth)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}