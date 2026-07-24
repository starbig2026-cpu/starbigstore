package com.example.starbigstore

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        executor = ContextCompat.getMainExecutor(this)
        
        setContent {
            StarbigStoreTheme(darkTheme = true) {
                var currentTab by remember { mutableIntStateOf(0) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Fondo de la App
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
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.Home, null) },
                                        label = { Text("Tienda") }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.ShoppingCart, null) },
                                        label = { Text("Catálogo") }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3 },
                                        icon = { Icon(Icons.Default.CreditCard, null) },
                                        label = { Text("Crédito") }
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 4,
                                        onClick = { currentTab = 4 },
                                        icon = { Icon(Icons.Default.ShoppingBag, null) },
                                        label = { Text("Carrito") }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        // Box que contiene las pantallas con márgenes para evitar notch/cámara
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .statusBarsPadding() // Esto baja el contenido para que no lo tape la cámara
                        ) {
                            when (currentTab) {
                                0 -> WebScreen(
                                    url = "file:///android_asset/index.html",
                                    onAdminRequest = { currentTab = 2 },
                                    onBiometricRequest = { showBiometricPrompt() },
                                    modifier = Modifier.fillMaxSize()
                                )
                                1 -> HomeScreen(modifier = Modifier.fillMaxSize())
                                2 -> AdminScreen(onBack = { currentTab = 0 }, modifier = Modifier.fillMaxSize())
                                3 -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { 
                                    Text("Solicitud de Crédito", color = Color.White) 
                                }
                                4 -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { 
                                    Text("Carrito de Compras", color = Color.White) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación Starbig")
            .setSubtitle("Usa tu huella para entrar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "¡Huella reconocida! Bienvenido.", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}
