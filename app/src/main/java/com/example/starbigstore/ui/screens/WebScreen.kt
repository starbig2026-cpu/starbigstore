package com.example.starbigstore.ui.screens

import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
                try {
                    auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                } catch (e: Exception) {
                    if (e.localizedMessage?.contains("already in use") == true) {
                        // Si ya existe en Auth pero no en Firestore, permitimos continuar para recrear el perfil
                        android.util.Log.d("WebScreen", "Usuario ya existe en Auth, procediendo a recrear en Firestore")
                    } else {
                        throw e
                    }
                }

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
                    put("photoName", "PERFIL_${System.currentTimeMillis()}_${email.trim().split("@")[0]}.jpg")
                    put("idCardName", "ID_${System.currentTimeMillis()}_${email.trim().split("@")[0]}.jpg")
                    put("folderName", "REGISTROS_NUEVOS")
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
                    val errorMsg = e.localizedMessage ?: ""
                    val finalMsg = when {
                        errorMsg.contains("already in use", ignoreCase = true) -> "ESTE CORREO YA ESTÁ REGISTRADO. INICIA SESIÓN."
                        errorMsg.contains("network", ignoreCase = true) -> "ERROR DE CONEXIÓN. REINTENTA."
                        else -> "ERROR: ${errorMsg.uppercase()}"
                    }
                    webView.evaluateJavascript("showNotification('$finalMsg', 'error')", null)
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
                    val errorMsg = e.localizedMessage ?: ""
                    val finalMsg = when {
                        errorMsg.contains("invalid-login-credentials", ignoreCase = true) -> "CORREO O CONTRASEÑA INCORRECTOS"
                        errorMsg.contains("invalid-email", ignoreCase = true) -> "FORMATO DE CORREO INVÁLIDO"
                        errorMsg.contains("user-not-found", ignoreCase = true) -> "USUARIO NO REGISTRADO"
                        errorMsg.contains("wrong-password", ignoreCase = true) -> "CONTRASEÑA INCORRECTA"
                        else -> "ERROR: ${errorMsg.uppercase()}"
                    }
                    webView.evaluateJavascript("showNotification('$finalMsg', 'error')", null)
                }
            }
    }

    fun fetchUserData(email: String) {
        FirebaseFirestore.getInstance().collection("registros_clientes")
            .whereEqualTo("email", email.trim()).get().addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val u = docs.documents[0]
                    val points = u.getLong("points") ?: 0
                    val status = u.getString("status") ?: "unverified"
                    val userDataJson = org.json.JSONObject(u.data ?: emptyMap<String, Any>()).toString()
                    webView.post { 
                        webView.evaluateJavascript("window.updateUserProfile('${u.getString("name")}', '$email', '${u.getString("photoUrl")}', '$status', $points, $userDataJson)", null)
                    }
                } else {
                    webView.post {
                        webView.evaluateJavascript("showNotification('PERFIL NO ENCONTRADO EN BASE DE DATOS', 'error')", null)
                    }
                }
            }
    }

    @JavascriptInterface
    fun goToAdmin() { onAdminRequest() }
    
    @JavascriptInterface
    fun requestBiometric() { onBiometricRequest() }

    @JavascriptInterface
    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    @JavascriptInterface
    fun updateUserPhoto(photoBase64: String) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email ?: return

        webView.post {
            webView.evaluateJavascript("showNotification('🚀 ACTUALIZANDO FOTO...', 'success')", null)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Sincronizar con Google Script (subir a Drive y actualizar Sheets)
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val payload = org.json.JSONObject().apply {
                    put("action", "updatePhoto")
                    put("sheetName", "USUARIOS")
                    put("email", email.trim())
                    put("photoBase64", photoBase64)
                    put("fileName", "PERFIL_${System.currentTimeMillis()}_${email.trim().split("@")[0]}.jpg")
                    put("folderName", "PERFILES_USUARIOS")
                }
                
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(googleSheetsUrl).post(body).build()
                
                client.newCall(request).execute().use { response ->
                    val responseStr = (response.body?.string() ?: "").trim()
                    
                    if (response.isSuccessful && responseStr.startsWith("{")) {
                        val resJson = org.json.JSONObject(responseStr)
                        val photoUrl = resJson.optString("photoUrl", "")
                        
                        if (photoUrl.isNotEmpty()) {
                            // 2. Actualizar Firestore
                            val userQuery = db.collection("registros_clientes").whereEqualTo("email", email.trim()).get().await()
                            userQuery.documents.firstOrNull()?.reference?.update("photoUrl", photoUrl)?.await()
                            
                            // 3. Actualizar Perfil de Firebase Auth
                            val profileUpdates = userProfileChangeRequest {
                                photoUri = Uri.parse(photoUrl)
                            }
                            currentUser.updateProfile(profileUpdates).await()

                            webView.post {
                                webView.evaluateJavascript("""
                                    showNotification('✅ FOTO ACTUALIZADA CON ÉXITO');
                                    document.getElementById('profile-img-large').src = '${fixDriveUrl(photoUrl)}';
                                    document.getElementById('user-photo-nav').src = '${fixDriveUrl(photoUrl)}';
                                """.trimIndent(), null)
                            }
                        } else {
                            throw Exception("EL SCRIPT NO DEVOLVIÓ URL")
                        }
                    } else {
                        throw Exception("ERROR EN SERVIDOR DE GOOGLE")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WebScreen", "Update Error", e)
                webView.post {
                    val msg = e.localizedMessage?.uppercase() ?: "ERROR AL ACTUALIZAR"
                    webView.evaluateJavascript("showNotification('$msg', 'error')", null)
                }
            }
        }
    }
}

@Composable
fun WebScreen(
    url: String,
    onAdminRequest: () -> Unit,
    onBiometricRequest: () -> Unit,
    onFileChoose: (ValueCallback<Array<Uri>>?) -> Unit,
    modifier: Modifier = Modifier,
    triggerAuth: Boolean = false,
    onAuthTriggered: () -> Unit = {}
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(triggerAuth, webViewInstance) {
        if (triggerAuth && webViewInstance != null) {
            webViewInstance?.evaluateJavascript("openAuth()", null)
            onAuthTriggered()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewInstance = this
                val webInterface = WebAppInterface(context, this, onAdminRequest, onBiometricRequest)
                addJavascriptInterface(webInterface, "AndroidApp")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (triggerAuth) {
                            evaluateJavascript("openAuth()", null)
                            onAuthTriggered()
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
