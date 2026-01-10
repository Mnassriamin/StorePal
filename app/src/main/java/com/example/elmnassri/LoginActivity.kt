package com.example.elmnassri

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Setup ViewModel
        val repository = (application as InventoryApplication).repository
        val factory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        val etPin = findViewById<EditText>(R.id.et_pin)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        btnLogin.setOnClickListener {
            val pin = etPin.text.toString()
            viewModel.attemptLogin(pin)
        }

        // Observe the login result
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginViewModel.LoginResult.Success -> {
                        // Login successful!
                        Toast.makeText(this@LoginActivity, "Welcome ${state.user.name}", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    is LoginViewModel.LoginResult.Error -> {
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                        etPin.text.clear()
                    }
                    else -> {} // Do nothing for idle
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity so pressing "Back" doesn't bring you here again
    }
}