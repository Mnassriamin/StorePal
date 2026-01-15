package com.example.elmnassri

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.navigation.fragment.findNavController
import com.example.elmnassri.databinding.FragmentMenuBinding

// Change Fragment -> BottomSheetDialogFragment
class MenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = UserSession.currentUser

        // --- SECURITY LOGIC ---
        if (currentUser?.role == "admin") {
            // If Admin: Show the hidden section
            binding.adminSection.visibility = View.VISIBLE
        } else {
            // If Worker: COMPLETELY REMOVE the section. It will look like it never existed.
            binding.adminSection.visibility = View.GONE
        }

        // --- CLICKS ---

        binding.btnOpenScanner.setOnClickListener {
            dismiss() // Close popup
            findNavController().navigate(R.id.nav_scanner)
        }

        binding.btnOpenDashboard.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.nav_dashboard)
        }

        binding.btnOpenStaff.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.navigation_staff)
        }

        binding.btnLogout.setOnClickListener {
            UserSession.currentUser = null
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}