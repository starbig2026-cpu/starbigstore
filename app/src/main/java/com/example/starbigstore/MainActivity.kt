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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.starbigstore.ui.screens.AdminScreen
import com.example.starbigstore.ui.screens.HomeScreen
import com.example.starbigstore.ui.screens.WebScreen
import com.example.starbigstore.ui.theme.StarbigStoreTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
                
                var newsList by remember { mutableStateOf(listOf<String>()) }
                var showNewsOverlay by remember { mutableStateOf(false) }
                var hasShownNews by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Usamos snapshotListener para tiempo real y filtramos duplicados
                    db.collection("novedades").orderBy("timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot != null) {
                                // Usamos distinct() para asegurar que no haya URLs repetidas
                                val list = snapshot.documents.mapNotNull { it.getString("imageUrl") }.distinct()
                                if (list.isNotEmpty()) {
                                    newsList = list
                                    // Solo mostramos el overlay automáticamente la primera vez que carga
                                    if (!hasShownNews) {
                                        showNewsOverlay = true
                                        hasShownNews = true
                                    }
                                }
                            }
                        }
                }
                
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

                if (showNewsOverlay && newsList.isNotEmpty()) {
                    NewsOverlay(
                        images = newsList,
                        onDismiss = { showNewsOverlay = false }
                    )
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

@Composable
fun NewsOverlay(images: List<String>, onDismiss: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(images) {
        if (images.size > 1) {
            while (true) {
                delay(5000)
                currentIndex = (currentIndex + 1) % images.size
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFFC5A059), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fixDriveUrl(images[currentIndex]))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f) // Flyers are usually portrait
                    .clickable { onDismiss() },
                contentScale = ContentScale.Fit,
                error = painterResource(id = R.drawable.main_bg_app)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(50.dp))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    images.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (index == currentIndex) Color(0xFFC5A059) else Color.White.copy(0.3f))
                        )
                    }
                }
            }
        }
    }
}

fun fixDriveUrl(url: String?): String {
    if (url.isNullOrBlank() || url.contains("subiendo")) return "https://via.placeholder.com/200?text=STARBIG"
    if (url.contains("firebasestorage.googleapis.com") || url.contains("appspot.com")) return url

    val id = when {
        url.contains("id=") -> url.split("id=").getOrNull(1)?.split("&")?.getOrNull(0)
        url.contains("file/d/") -> url.split("file/d/").getOrNull(1)?.split("/")?.getOrNull(0)
        url.length > 20 && !url.contains("/") && !url.contains(".") -> url
        else -> null
    }

    return id?.let { "https://drive.google.com/thumbnail?id=$it&sz=w1000" } ?: url
}
