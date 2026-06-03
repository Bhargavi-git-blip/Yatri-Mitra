package com.yatrimitra.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = ""
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow(AuthState())
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthState())
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    fun login(email: String, password: String) {

        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AuthState(
                errorMessage = "Please fill all fields"
            )
            return
        }

        _loginState.value = AuthState(isLoading = true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    _loginState.value = AuthState(
                        isSuccess = true,
                        userName = user?.email ?: "User"
                    )

                } else {

                    _loginState.value = AuthState(
                        errorMessage = task.exception?.message
                    )
                }
            }
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {

        if (password != confirmPassword) {
            _registerState.value = AuthState(
                errorMessage = "Passwords do not match"
            )
            return
        }

        _registerState.value = AuthState(isLoading = true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    _registerState.value = AuthState(
                        isSuccess = true,
                        userName = name
                    )

                } else {

                    _registerState.value = AuthState(
                        errorMessage = task.exception?.message
                    )
                }
            }
    }

    fun resetLoginState() {
        _loginState.value = AuthState()
    }

    fun resetRegisterState() {
        _registerState.value = AuthState()
    }
}