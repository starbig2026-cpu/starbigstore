package com.example.starbigstore.ui.screens

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WebAppInterface(
    private val mContext: Context,
    private val webView: WebView,
    private val onAdminRequest: () -> Unit,
    private val onBiometricRequest: () -> Unit
) {
    private val googleSheetsUrl = "https://script.google.com/macros/s/AKfycbzvorSsMtjvqzw6l6FUKwkCBgWjl3rOyhle7AjaGalXfnet6jtDAsjdtxehUxxqwSmPtg/exec"

    @JavascriptInterface
    fun registerUser(firstName: String, lastName: String, email: String, phone: String, address: String) {
        val db = FirebaseFirestore.getInstance()
        val user: HashMap<String, Any> = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "name" to "$firstName $lastName",
            "email" to email,
            "phone" to phone,
            "address" to address,
            "status" to "unverified", // Estatus inicial bloqueado
            "photoUrl" to "",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("registros_clientes")
            .add(user)
            .addOnSuccessListener {
                syncToGoogleSheets(user)
                Toast.makeText(mContext, "Registro enviado. Espera la verificación del administrador.", Toast.LENGTH_LONG).show()
            }
    }

    private fun syncToGoogleSheets(user: HashMap<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = "{\"name\":\"${user["name"]}\",\"email\":\"${user["email"]}\",\"phone\":\"${user["phone"]}\",\"address\":\"${user["address"]}\",\"status\":\"pending\"}"
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(googleSheetsUrl).post(body).build()
                client.newCall(request).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JavascriptInterface
    fun loginUser(email: String, pass: String) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                fetchUserDataAndPopulateProfile(email)
            }
            .addOnFailureListener {
                Toast.makeText(mContext, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserDataAndPopulateProfile(email: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("registros_clientes")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val user = docs.documents[0]
                    val name = user.getString("name") ?: ""
                    val photoUrl = user.getString("photoUrl") ?: ""
                    val status = user.getString("status") ?: "En verificación"
                    
                    // Llamamos a la función de JavaScript en index.html
                    webView.post {
                        webView.evaluateJavascript("window.updateUserProfile('$name', '$email', '$photoUrl', '$status')", null)
                    }
                }
            }
    }

    @JavascriptInterface
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(mContext)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    @JavascriptInterface
    fun requestBiometric() { onBiometricRequest() }

    @JavascriptInterface
    fun goToAdmin() { onAdminRequest() }
}

@Composable
fun WebScreen(
    url: String, 
    onAdminRequest: () -> Unit, 
    onBiometricRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(WebAppInterface(context, this, onAdminRequest, onBiometricRequest), "AndroidApp")
                loadUrl(url)
            }
        }
    )
}
