package com.example.starbigstore

import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.starbigstore.ui.screens.AdminScreen
import com.example.starbigstore.ui.screens.HomeScreen
import com.example.starbigstore.ui.screens.WebScreen
import com.example.starbigstore.ui.theme.StarbigStoreTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val deviceId = remember { UUID.randomUUID().toString() }
            val db = FirebaseFirestore.getInstance()
            
            // Sistema de Pulso (Heartbeat) para tráfico real
            LaunchedEffect(Unit) {
                while(true) {
                    val data = hashMapOf(
                        "ultimoPulso" to System.currentTimeMillis(),
                        "esAdmin" to false // Puedes cambiar esto si es el admin
                    )
                    db.collection("presencia").document(deviceId).set(data)
                    delay(30000) // Cada 30 segundos
                }
            }

            StarbigStoreTheme(darkTheme = true) {
                var currentTab by remember { mutableIntStateOf(0) }
                
                val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    uri?.let { filePathCallback?.onReceiveValue(arrayOf(it)) } ?: filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.main_bg_app),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(8.dp),
                        contentScale = ContentScale.Crop
                    )
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.5f)) {}

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (currentTab != 2) {
                                NavigationBar(
                                    containerColor = Color.Black.copy(alpha = 0.7f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.navigationBarsPadding()
                                ) {
                                    NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Tienda") })
                                    NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Catálogo") })
                                    NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.CreditCard, null) }, label = { Text("Crédito") })
                                    NavigationBarItem(selected = currentTab == 4, onClick = { currentTab = 4 }, icon = { Icon(Icons.Default.ShoppingBag, null) }, label = { Text("Carrito") })
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()).statusBarsPadding()) {
                            when (currentTab) {
                                0 -> WebScreen(
                                    url = "file:///android_asset/index.html",
                                    onAdminRequest = { currentTab = 2 },
                                    onBiometricRequest = { showBiometricPrompt() },
                                    onFileChoose = { callback ->
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = callback
                                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> HomeScreen(modifier = Modifier.fillMaxSize())
                                2 -> AdminScreen(onBack = { currentTab = 0 }, modifier = Modifier.fillMaxSize())
                                else -> Box(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "Bienvenido", Toast.LENGTH_SHORT).show()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Starbig Auth").setSubtitle("Usa tu huella").setNegativeButtonText("Cancelar").build()
        biometricPrompt.authenticate(promptInfo)
    }
}
