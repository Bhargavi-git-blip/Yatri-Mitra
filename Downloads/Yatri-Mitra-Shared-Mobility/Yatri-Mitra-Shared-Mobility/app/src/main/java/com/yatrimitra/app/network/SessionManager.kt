package com.yatrimitra.app.network

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager — persists auth token and user info in SharedPreferences.
 * Call SessionManager.init(context) once in Application or SplashActivity.
 */
object SessionManager {

    private const val PREF_NAME = "yatrimitra_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_NAME  = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, name: String, email: String, phone: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME,  name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getName():  String  = prefs.getString(KEY_NAME,  "Passenger") ?: "Passenger"
    fun getEmail(): String  = prefs.getString(KEY_EMAIL, "") ?: ""
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearSession() = prefs.edit().clear().apply()
}
