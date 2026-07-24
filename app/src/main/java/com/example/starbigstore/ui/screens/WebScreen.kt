package com.example.starbigstore.ui.screens

import android.content.Context
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class WebAppInterface(
    private val mContext: Context,
    private val webView: WebView,
    private val onAdminRequest: () -> Unit,
    private val onBiometricRequest: () -> Unit
) {
    private val googleSheetsUrl = "https://script.google.com/macros/s/AKfycbzvorSsMtjvqzw6l6FUKwkCBgWjl3rOyhle7AjaGalXfnet6jtDAsjdtxehUxxqwSmPtg/exec"

    @JavascriptInterface
    fun registerUser(firstName: String, lastName: String, email: String, phone: String, address: String) {
        // En un entorno real, las fotos se subirían primero. 
        // Por ahora registramos los datos y marcamos como 'unverified'
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "name" to "$firstName $lastName",
            "email" to email,
            "phone" to phone,
            "address" to address,
            "status" to "unverified",
            "photoUrl" to "", 
            "idCardUrl" to "",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("registros_clientes")
            .add(user)
            .addOnSuccessListener {
                syncToGoogleSheets(user)
                Toast.makeText(mContext, "Solicitud enviada. Verificaremos tu perfil.", Toast.LENGTH_LONG).show()
            }
    }

    private fun syncToGoogleSheets(user: Map<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                // Enviamos nombre y apellido por separado para la planilla
                val json = """
                {
                    "firstName": "${user["firstName"]}",
                    "lastName": "${user["lastName"]}",
                    "email": "${user["email"]}",
                    "phone": "${user["phone"]}",
                    "address": "${user["address"]}",
                    "status": "unverified",
                    "photoUrl": "${user["photoUrl"]}",
                    "idCardUrl": "${user["idCardUrl"]}"
                }
                """.trimIndent()
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(googleSheetsUrl).post(body).build()
                client.newCall(request).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JavascriptInterface
    fun loginUser(email: String, pass: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { fetchUserData(email) }
            .addOnFailureListener { Toast.makeText(mContext, "Credenciales incorrectas", Toast.LENGTH_SHORT).show() }
    }

    private fun fetchUserData(email: String) {
        FirebaseFirestore.getInstance().collection("registros_clientes")
            .whereEqualTo("email", email).get().addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val u = docs.documents[0]
                    webView.post { 
                        webView.evaluateJavascript("window.updateUserProfile('${u.getString("name")}', '$email', '${u.getString("photoUrl")}', '${u.getString("status")}')", null) 
                    }
                }
            }
    }

    @JavascriptInterface
    fun isBiometricAvailable() = BiometricManager.from(mContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

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
    onFileChoose: (ValueCallback<Array<Uri>>?) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(wv: WebView?, fpc: ValueCallback<Array<Uri>>?, fci: FileChooserParams?): Boolean {
                        onFileChoose(fpc)
                        return true
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(WebAppInterface(context, this, onAdminRequest, onBiometricRequest), "AndroidApp")
                loadUrl(url)
            }
        }
    )
}
