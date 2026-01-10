package com.example.elmnassri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.elmnassri.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup the bottom navigation
        binding.bottomNavView.setupWithNavController(navController)

        // --- ROLE CHECK ---
        // If the logged-in user is NOT an admin, hide the Dashboard tab.
        if (!UserSession.isAdmin()) {
            val menu = binding.bottomNavView.menu
            val dashboardItem = menu.findItem(R.id.nav_dashboard)
            dashboardItem.isVisible = false

            // Also, make sure the app doesn't start on the Dashboard screen
            // (Start on Scanner instead)
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(R.id.nav_scanner)
            navController.graph = graph
        }
    }
}