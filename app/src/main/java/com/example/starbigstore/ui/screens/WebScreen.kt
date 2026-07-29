package com.example.starbigstore.ui.screens

import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.tasks.await
import java.util.*

@Suppress("unused")
class WebAppInterface(
    private val mContext: Context,
    private val webView: WebView,
    private val onAdminRequest: () -> Unit,
    private val onBiometricRequest: () -> Unit
) {
    private val googleSheetsUrl = "https://script.google.com/macros/s/AKfycbzTKwRkgCmy_m42ZeKjPbczOMr0YHmRKiSmrHPCSEdKixHzI9MG3fhEfEU3pChr45exvw/exec"

    @JavascriptInterface
    fun registerUser(firstName: String, lastName: String, email: String, phone: String, address: String, photoBase64: String, idCardBase64: String, pass: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        webView.post {
            webView.evaluateJavascript("showNotification('🚀 CREANDO PERFIL PREMIUM...', 'success')", null)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Crear usuario en Firebase Auth
                auth.createUserWithEmailAndPassword(email.trim(), pass).await()

                // 2. Guardar en Firestore con estado inicial
                val userMap = hashMapOf<String, Any>(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "name" to "$firstName $lastName",
                    "email" to email.trim(),
                    "phone" to phone,
                    "address" to address,
                    "status" to "unverified",
                    "photoUrl" to "subiendo...", 
                    "idCardUrl" to "subiendo...",
                    "timestamp" to System.currentTimeMillis()
                )

                val docRef = db.collection("registros_clientes").add(userMap).await()

                // 3. Sincronizar con Google (esto genera los links permanentes de las fotos)
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val payload = org.json.JSONObject().apply {
                    put("action", "register")
                    put("sheetName", "USUARIOS")
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("email", email.trim())
                    put("phone", phone)
                    put("address", address)
                    put("status", "unverified")
                    put("photoBase64", photoBase64)
                    put("idCardBase64", idCardBase64)
                    put("date", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
                }
                
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(googleSheetsUrl).post(body).build()
                
                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val resJson = org.json.JSONObject(responseStr)
                        val photoUrl = resJson.optString("photoUrl", "")
                        val idCardUrl = resJson.optString("idCardUrl", "")
                        
                        if (photoUrl.isNotEmpty()) {
                            docRef.update("photoUrl", photoUrl, "idCardUrl", idCardUrl).await()
                        }
                    }
                }

                webView.post {
                    webView.evaluateJavascript("showNotification('✅ REGISTRO EXITOSO. EN REVISIÓN.', 'success'); closeAuth();", null)
                }

            } catch (e: Exception) {
                webView.post {
                    webView.evaluateJavascript("showNotification('ERROR: ${e.localizedMessage?.uppercase()}', 'error')", null)
                }
            }
        }
    }

    @JavascriptInterface
    fun loginUser(email: String, pass: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { fetchUserData(email.trim()) }
            .addOnFailureListener { e ->
                webView.post {
                    webView.evaluateJavascript("showNotification('ERROR: ${e.localizedMessage?.uppercase()}', 'error')", null)
                }
            }
    }

    fun fetchUserData(email: String) {
        FirebaseFirestore.getInstance().collection("registros_clientes")
            .whereEqualTo("email", email.trim()).get().addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val u = docs.documents[0]
                    val points = u.getLong("points") ?: 0
                    webView.post { 
                        webView.evaluateJavascript("window.updateUserProfile('${u.getString("name")}', '$email', '${u.getString("photoUrl")}', '${u.getString("status")}', $points)", null) 
                    }
                }
            }
    }

    @JavascriptInterface
    fun goToAdmin() { onAdminRequest() }
    
    @JavascriptInterface
    fun requestBiometric() { onBiometricRequest() }

    @JavascriptInterface
    fun updateUserPhoto(photoBase64: String) {
        val db = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build()
                val payload = org.json.JSONObject().apply {
                    put("action", "updatePhoto")
                    put("email", email)
                    put("photoBase64", photoBase64)
                }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(googleSheetsUrl).post(body).build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val resJson = org.json.JSONObject(response.body?.string() ?: "{}")
                        val photoUrl = resJson.optString("photoUrl", "")
                        if (photoUrl.isNotEmpty()) {
                            db.collection("registros_clientes").whereEqualTo("email", email).get().await().documents.firstOrNull()?.reference?.update("photoUrl", photoUrl)
                            webView.post {
                                webView.evaluateJavascript("showNotification('✅ FOTO ACTUALIZADA'); location.reload();", null)
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
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
                val webInterface = WebAppInterface(context, this, onAdminRequest, onBiometricRequest)
                addJavascriptInterface(webInterface, "AndroidApp")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        FirebaseAuth.getInstance().currentUser?.email?.let { email ->
                            webInterface.fetchUserData(email)
                        }
                    }
                }

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
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadUrl(url)
            }
        }
    )
}
