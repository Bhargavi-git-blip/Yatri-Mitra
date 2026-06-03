package com.yatrimitra.app.network

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * ApiClient — lightweight HTTP client.
 * BASE_URL: 10.0.2.2 = host machine localhost from Android emulator.
 * For real device: change to your PC's local IP e.g. "http://192.168.1.5:3000"
 */
object ApiClient {

    const val BASE_URL = ""

    // Short timeouts so we fail fast instead of hanging the UI
    private const val CONNECT_TIMEOUT = 5_000
    private const val READ_TIMEOUT    = 7_000

    data class ApiResponse(
        val success: Boolean,
        val message: String,
        val data: JSONObject?
    )

    fun post(endpoint: String, body: JSONObject, token: String? = null): ApiResponse {
        return try {
            val conn = (URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection).apply {
                requestMethod  = "POST"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                doOutput       = true
                doInput        = true
                setRequestProperty("Content-Type",  "application/json")
                setRequestProperty("Accept",         "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body.toString()) }

            val code   = conn.responseCode
            val stream = try {
                if (code in 200..299) conn.inputStream else conn.errorStream
            } catch (e: Exception) { conn.errorStream }

            val text = BufferedReader(InputStreamReader(stream ?: conn.inputStream, "UTF-8"))
                .use { it.readText() }

            val json = JSONObject(text)
            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", "Server error"),
                data    = json
            )
        } catch (e: java.net.ConnectException) {
            ApiResponse(false, "Cannot connect to server. Is the backend running?", null)
        } catch (e: java.net.SocketTimeoutException) {
            ApiResponse(false, "Server timeout. Check backend is running on port 3000.", null)
        } catch (e: Exception) {
            ApiResponse(false, "Error: ${e.message}", null)
        }
    }

    fun get(endpoint: String, token: String? = null): ApiResponse {
        return try {
            val conn = (URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection).apply {
                requestMethod  = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
            }

            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text   = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            val json   = JSONObject(text)
            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", "Server error"),
                data    = json
            )
        } catch (e: Exception) {
            ApiResponse(false, "Error: ${e.message}", null)
        }
    }
}
