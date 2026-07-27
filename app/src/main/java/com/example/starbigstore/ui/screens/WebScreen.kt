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
    fun registerUser(firstName: String, lastName: String, email: String, phone: String, address: String, photoBase64: String, idCardBase64: String) {
        val db = FirebaseFirestore.getInstance()

        // Toast de depuración inicial
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(mContext, "🚀 Procesando registro...", Toast.LENGTH_SHORT).show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Guardar primero en Firestore (sin URLs de imagen aún)
                val userMap = hashMapOf<String, Any>(
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
                    .add(userMap)
                    .addOnSuccessListener { docRef ->
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(mContext, "✅ Datos base guardados en Firebase", Toast.LENGTH_SHORT).show()
                        }

                        // 2. Sincronizar con Google Sheets para subir fotos a Drive
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val client = OkHttpClient.Builder()
                                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                                    .followRedirects(true)
                                    .followSslRedirects(true)
                                    .build()

                                val payload = org.json.JSONObject().apply {
                                    put("firstName", firstName)
                                    put("lastName", lastName)
                                    put("email", email)
                                    put("phone", phone)
                                    put("address", address)
                                    put("photoBase64", photoBase64)
                                    put("idCardBase64", idCardBase64)
                                }
                                
                                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                val request = Request.Builder()
                                    .url(googleSheetsUrl)
                                    .post(body)
                                    .build()
                                
                                client.newCall(request).execute().use { response ->
                                    val responseStr = response.body?.string() ?: ""
                                    android.util.Log.d("WebScreen", "Response: $responseStr")
                                    
                                    if (response.isSuccessful) {
                                        val resJson = org.json.JSONObject(responseStr)
                                        val photoUrl = resJson.optString("photoUrl", "")
                                        val idCardUrl = resJson.optString("idCardUrl", "")
                                        
                                        // 3. Actualizar Firestore con las URLs de Drive
                                        if (photoUrl.isNotEmpty() || idCardUrl.isNotEmpty()) {
                                            docRef.update("photoUrl", photoUrl, "idCardUrl", idCardUrl)
                                            CoroutineScope(Dispatchers.Main).launch {
                                                Toast.makeText(mContext, "📸 Fotos vinculadas desde Drive", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WebScreen", "Sync Error", e)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("WebScreen", "Firestore Error: ${e.message}")
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(mContext, "❌ Error Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("WebScreen", "Error", e)
            }
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
