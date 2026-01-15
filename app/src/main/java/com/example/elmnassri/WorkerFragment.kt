package com.example.elmnassri

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elmnassri.databinding.FragmentWorkerManagementBinding
import kotlinx.coroutines.launch

class WorkerFragment : Fragment() {

    private var _binding: FragmentWorkerManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkerViewModel by viewModels {
        WorkerViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Adapter
        val adapter = object : RecyclerView.Adapter<WorkerViewHolder>() {
            var items: List<User> = emptyList()

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_worker, parent, false)
                return WorkerViewHolder(v)
            }

            override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
                val user = items[position]
                holder.name.text = user.name
                holder.role.text = if (user.role == "admin") "Admin (PIN Hidden)" else "PIN: ${user.pin}"
                holder.initial.text = user.name.firstOrNull()?.toString()?.uppercase() ?: "?"

                // Prevent deleting yourself (Assuming you are logged in as Admin/0000)
                if (user.role == "admin") {
                    holder.deleteBtn.visibility = View.GONE
                } else {
                    holder.deleteBtn.visibility = View.VISIBLE
                    holder.deleteBtn.setOnClickListener {
                        confirmDelete(user)
                    }
                }
            }

            override fun getItemCount() = items.size
        }

        binding.recyclerWorkers.layoutManager = LinearLayoutManager(context)
        binding.recyclerWorkers.adapter = adapter

        // 2. Observe Data
        lifecycleScope.launch {
            viewModel.allWorkers.collect { users ->
                adapter.items = users
                adapter.notifyDataSetChanged()
            }
        }

        // 3. Add Button
        binding.fabAddWorker.setOnClickListener {
            showAddWorkerDialog()
        }
    }

    private fun showAddWorkerDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_customer, null) // Reusing the clean layout
        val title = view.findViewById<TextView>(R.id.sheet_title) // Assuming you used sheet_title or similar in layout
        // If reusing dialog_add_customer.xml, ids might be input_name, input_phone.
        // Let's assume we create a quick layout or reuse carefully.
        // Better: Let's build the dialog programmatically with the layout we made for Customer but change hints.

        val inputName = view.findViewById<EditText>(R.id.input_name)
        val inputPin = view.findViewById<EditText>(R.id.input_phone) // Reusing phone input for PIN
        val btnSave = view.findViewById<android.widget.Button>(R.id.btn_save_customer)

        // Update hints for this context
        inputName.hint = "Worker Name"
        inputPin.hint = "Login PIN (e.g. 1234)"
        inputPin.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        btnSave.text = "ADD WORKER"

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val name = inputName.text.toString()
            val pin = inputPin.text.toString()

            if (name.isNotEmpty() && pin.isNotEmpty()) {
                viewModel.addWorker(name, pin)
                Toast.makeText(context, "Worker Added", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Name and PIN are required", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(context)
            .setTitle("Delete Worker?")
            .setMessage("Are you sure you want to remove ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteWorker(user)
                Toast.makeText(context, "Worker Removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ViewHolder Class
    class WorkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.text_name)
        val role: TextView = itemView.findViewById(R.id.text_pin)
        val initial: TextView = itemView.findViewById(R.id.text_initial)
        val deleteBtn: ImageView = itemView.findViewById(R.id.btn_delete)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}