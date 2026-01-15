package com.example.elmnassri

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.elmnassri.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. SECURITY: If no user is logged in, kick them back to Login
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. SETUP NAVIGATION
        // Note: Make sure the ID matches your XML (nav_host_fragment or nav_host_fragment_activity_main)
        // Based on your code, it seems to be nav_host_fragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 3. LINK BOTTOM BAR
        // This automatically handles clicks for Storage, Cashier, Kridi, and Menu
        binding.bottomNavView.setupWithNavController(navController)


    }
}