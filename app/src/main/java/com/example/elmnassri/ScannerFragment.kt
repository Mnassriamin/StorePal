package com.example.elmnassri

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.elmnassri.databinding.FragmentScannerBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory((requireActivity().application as InventoryApplication).repository)
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var isScanning = true

    private var selectedImageUri: Uri? = null
    private var dialog: Dialog? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            updateImagePreview()
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            updateImagePreview()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        if (isScanning) {
                            isScanning = false
                            barcode.rawValue?.let { barcodeValue ->
                                checkBarcodeInDatabase(barcodeValue)
                            }
                        }
                    })
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("ScannerFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun checkBarcodeInDatabase(barcodeValue: String) {
        lifecycleScope.launch {
            val existingItem = viewModel.findItemByBarcode(barcodeValue)
            requireActivity().runOnUiThread {
                if (existingItem != null) {
                    showItemDetailsDialog(existingItem)
                } else {
                    showAddNewItemDialog(barcodeValue)
                }
            }
        }
    }

    private fun showItemDetailsDialog(item: Item) {
        dialog = Dialog(requireContext())
        dialog?.setContentView(R.layout.dialog_item_details)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog?.setCancelable(true)
        val itemImageView = dialog?.findViewById<ShapeableImageView>(R.id.dialog_item_image)
        val itemNameView = dialog?.findViewById<TextView>(R.id.dialog_item_name)
        val itemPriceView = dialog?.findViewById<TextView>(R.id.dialog_item_price)
        val doneButton = dialog?.findViewById<Button>(R.id.btn_done)
        itemNameView?.text = item.name
        itemPriceView?.text = "${String.format("%.2f", item.price)} TND"
        itemImageView?.load(item.imageUri) {
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_foreground)
        }
        doneButton?.setOnClickListener {
            dialog?.dismiss()
        }
        dialog?.setOnDismissListener {
            isScanning = true
        }
        dialog?.show()
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    private fun showAddNewItemDialog(barcode: String) {
        selectedImageUri = null
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_name)
        val priceEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_price)
        val barcodeEditText = dialogView.findViewById<EditText>(R.id.edit_text_item_barcode)
        val addPictureButton = dialogView.findViewById<Button>(R.id.btn_select_image)

        barcodeEditText.setText(barcode)
        barcodeEditText.isEnabled = false

        addPictureButton.setOnClickListener {
            showImageSourceDialog()
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Add New Item")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEditText.text.toString()
                val price = priceEditText.text.toString().toDoubleOrNull()
                if (name.isNotBlank() && price != null) {
                    val newItem = Item(
                        name = name,
                        price = price,
                        barcode = barcode
                    )
                    viewModel.upsertItem(newItem, selectedImageUri)
                    findNavController().navigate(R.id.nav_storage)
                } else {
                    Toast.makeText(requireContext(), "Name and Price cannot be empty", Toast.LENGTH_SHORT).show()
                    isScanning = true
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                isScanning = true
            }
            .setOnCancelListener {
                isScanning = true
            }

        dialog = builder.create()
        dialog?.show()
    }

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

    private fun updateImagePreview() {
        // Here, we need to handle both Dialog and AlertDialog
        val imageView = (dialog as? AlertDialog)?.findViewById<ImageView>(R.id.image_preview)
            ?: dialog?.findViewById<ImageView>(R.id.image_preview)
        imageView?.apply {
            setImageURI(selectedImageUri)
            visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}

class BarcodeAnalyzer(private val onBarcodeDetected: (Barcode) -> Unit) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder().build()
    private val scanner = BarcodeScanning.getClient(options)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onBarcodeDetected(barcodes[0])
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}