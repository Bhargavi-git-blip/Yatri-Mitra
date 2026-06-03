package com.yatrimitra.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yatrimitra.app.R
import com.yatrimitra.app.ui.MainActivity
import com.yatrimitra.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val vm: AuthViewModel by viewModels()

    private lateinit var etEmail    : EditText
    private lateinit var etPassword : EditText
    private lateinit var btnLogin   : Button
    private lateinit var tvError    : TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRegister : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail     = findViewById(R.id.etEmail)
        etPassword  = findViewById(R.id.etPassword)
        btnLogin    = findViewById(R.id.btnLogin)
        tvError     = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
        tvRegister  = findViewById(R.id.tvRegister)

        btnLogin.setOnClickListener {
            vm.login(etEmail.text.toString(), etPassword.text.toString())
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Observe state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.loginState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    btnLogin.isEnabled     = !state.isLoading
                    tvError.visibility     = if (state.errorMessage != null) View.VISIBLE else View.GONE
                    tvError.text           = state.errorMessage ?: ""

                    if (state.isSuccess) {
                        Toast.makeText(this@LoginActivity, "Welcome back, ${state.userName}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); vm.resetLoginState() }
}
