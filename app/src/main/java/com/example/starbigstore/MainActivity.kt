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
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                var shouldShowAuth by remember { mutableStateOf(false) }
                var shouldShowCart by remember { mutableStateOf(false) }
                var addToCartCommand by remember { mutableStateOf<Triple<String, Int, String>?>(null) }
                
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
                                    containerColor = Color(0xFF08080A),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.navigationBarsPadding()
                                ) {
                                    val items = listOf(
                                        Triple(0, Icons.Default.Home, "Tienda"),
                                        Triple(1, Icons.Default.ShoppingCart, "Catálogo"),
                                        Triple(3, Icons.Default.CreditCard, "Crédito"),
                                        Triple(4, Icons.Default.ShoppingBag, "Carrito")
                                    )

                                    items.forEach { (index, icon, label) ->
                                        NavigationBarItem(
                                            selected = currentTab == index,
                                            onClick = {
                                                if (index == 4) {
                                                    shouldShowCart = true
                                                    currentTab = 4
                                                } else {
                                                    currentTab = index
                                                }
                                            },
                                            icon = { Icon(icon, null) },
                                            label = { Text(label, fontSize = 10.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFFC5A059),
                                                selectedTextColor = Color(0xFFC5A059),
                                                unselectedIconColor = Color.White.copy(0.4f),
                                                unselectedTextColor = Color.White.copy(0.4f),
                                                indicatorColor = Color(0xFFC5A059).copy(0.1f)
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()).statusBarsPadding()) {
                            // Capa de la Web (siempre presente para no perder sesión)
                            WebScreen(
                                url = "file:///android_asset/index.html",
                                onAdminRequest = { currentTab = 2 },
                                onBiometricRequest = { showBiometricPrompt() },
                                onFileChoose = { callback ->
                                    filePathCallback?.onReceiveValue(null)
                                    filePathCallback = callback
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { 
                                        alpha = if (currentTab == 0 || currentTab == 4) 1f else 0f 
                                    },
                                triggerAuth = shouldShowAuth,
                                onAuthTriggered = { shouldShowAuth = false },
                                triggerCart = shouldShowCart,
                                onCartTriggered = { shouldShowCart = false },
                                currentTab = currentTab,
                                addToCartCommand = addToCartCommand,
                                onAddToCartProcessed = { addToCartCommand = null }
                            )

                            // Capa de Pantallas Nativas
                            if (currentTab != 0 && currentTab != 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.95f))
                                ) {
                                    when (currentTab) {
                                        1 -> HomeScreen(
                                            onNavigateToLogin = {
                                                shouldShowAuth = true
                                                currentTab = 0
                                            },
                                            onAddToCart = { id, qty, method ->
                                                addToCartCommand = Triple(id, qty, method)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        2 -> AdminScreen(onBack = { currentTab = 0 }, modifier = Modifier.fillMaxSize())
                                        3 -> Box(Modifier.fillMaxSize()) // Placeholder para Crédito
                                    }
                                }
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
