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

class RegisterActivity : AppCompatActivity() {

    private val vm: AuthViewModel by viewModels()

    private lateinit var etName           : EditText
    private lateinit var etEmail          : EditText
    private lateinit var etPhone          : EditText
    private lateinit var etPassword       : EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister      : Button
    private lateinit var tvError          : TextView
    private lateinit var progressBar      : ProgressBar
    private lateinit var tvLogin          : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etName            = findViewById(R.id.etName)
        etEmail           = findViewById(R.id.etEmail)
        etPhone           = findViewById(R.id.etPhone)
        etPassword        = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister       = findViewById(R.id.btnRegister)
        tvError           = findViewById(R.id.tvError)
        progressBar       = findViewById(R.id.progressBar)
        tvLogin           = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            vm.register(
                name            = etName.text.toString(),
                email           = etEmail.text.toString(),
                phone           = etPhone.text.toString(),
                password        = etPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString()
            )
        }

        tvLogin.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.registerState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    btnRegister.isEnabled  = !state.isLoading
                    tvError.visibility     = if (state.errorMessage != null) View.VISIBLE else View.GONE
                    tvError.text           = state.errorMessage ?: ""

                    if (state.isSuccess) {
                        Toast.makeText(this@RegisterActivity, "Welcome, ${state.userName}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); vm.resetRegisterState() }
}
