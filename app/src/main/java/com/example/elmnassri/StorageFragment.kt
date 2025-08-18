package com.example.elmnassri

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.elmnassri.databinding.FragmentStorageBinding
import kotlinx.coroutines.launch
import java.io.File

class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    private var selectedImageUri: Uri? = null
    private var dialog: AlertDialog? = null

    // Launcher for taking a new picture
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            updateImagePreview()
        }
    }

    // Launcher for selecting an image from the gallery
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            updateImagePreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ItemAdapter { item ->
            showAddItemDialog(item)
        }
        binding.recyclerView.adapter = adapter
        setupSearchView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allItems.collect { items ->
                adapter.submitList(items)
            }
        }

        binding.fabAddItem.setOnClickListener {
            showAddItemDialog(null)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean { return false }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    private fun showAddItemDialog(itemToEdit: Item?) {
        selectedImageUri = null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_name)
        val priceEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_price)
        val barcodeEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_barcode)
        val addPictureButton = dialogView.findViewById<Button>(R.id.btn_select_image)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.image_preview)

        val isEditMode = itemToEdit != null
        val dialogTitle = if (isEditMode) "Edit Item" else "Add New Item"

        if (isEditMode) {
            nameEditText.setText(itemToEdit?.name)
            priceEditText.setText(itemToEdit?.price.toString())
            barcodeEditText.setText(itemToEdit?.barcode)
            itemToEdit?.imageUri?.let {
                // In edit mode, we use the permanent URL, not a temporary URI
                selectedImageUri = it.toUri()
                imagePreview.load(it)
                imagePreview.visibility = View.VISIBLE
            }
        }

        addPictureButton.setOnClickListener {
            showImageSourceDialog()
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString()
                val price = priceEditText.text.toString().toDoubleOrNull()
                val barcode = barcodeEditText.text.toString()

                if (name.isNotBlank() && price != null) {
                    val itemId = itemToEdit?.id ?: 0
                    val updatedItem = Item(
                        id = itemId,
                        name = name,
                        price = price,
                        barcode = barcode.ifEmpty { "N/A" },
                        imageUri = itemToEdit?.imageUri // Use existing URI if not changed
                    )
                    // The selectedImageUri is a temporary local URI, pass it to the ViewModel for upload
                    viewModel.upsertItem(updatedItem, selectedImageUri)
                } else {
                    Toast.makeText(requireContext(), "Name and Price cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        if (isEditMode) {
            builder.setNeutralButton("Delete") { _, _ ->
                itemToEdit?.let { viewModel.deleteItem(it) }
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
            }
        }

        dialog = builder.create()
        dialog?.show()
    }

    // NEW: Function to show the choice between Camera and Gallery
    private fun showImageSourceDialog() {
        val options = arrayOf("Take a photo", "Choose from gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Take a photo
                        getTmpFileUri().let { uri ->
                            selectedImageUri = uri
                            takePictureLauncher.launch(uri)
                        }
                    }
                    1 -> { // Choose from gallery
                        selectImageLauncher.launch("image/*")
                    }
                }
            }
            .show()
    }

    // NEW: Helper to update the preview
    private fun updateImagePreview() {
        dialog?.findViewById<ImageView>(R.id.image_preview)?.apply {
            setImageURI(selectedImageUri)
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}