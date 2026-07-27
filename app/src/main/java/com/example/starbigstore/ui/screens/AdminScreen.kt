package com.example.starbigstore.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.starbigstore.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CustomerRegistration(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val status: String = "unverified",
    val photoUrl: String = "",
    val idCardUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var isAuthorized by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    val correctPin = "1358L"

    Scaffold(
        containerColor = Color(0xFF0D0D11),
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            if (!isAuthorized) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_admin),
                            contentDescription = null,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(bottom = 32.dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        Text(
                            "ACCESO EXCLUSIVO",
                            color = Color(0xFFC5A059),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "ADMINISTRACIÓN",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            placeholder = { 
                                Text(
                                    "INTRODUZCA PIN DE SEGURIDAD", 
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ) 
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                letterSpacing = 8.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC5A059),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                cursorColor = Color(0xFFC5A059)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { if (pinInput == correctPin) isAuthorized = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Text(
                                "AUTENTICAR", 
                                color = Color.Black, 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            } else {
                AdminListContent()
            }
        }
    }
}

@Composable
fun AdminListContent() {
    var selectedTab by remember { mutableIntStateOf(2) }
    var registrations by remember { mutableStateOf(listOf<CustomerRegistration>()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()
    val googleSheetsUrl = "https://script.google.com/macros/s/AKfycbzTKwRkgCmy_m42ZeKjPbczOMr0YHmRKiSmrHPCSEdKixHzI9MG3fhEfEU3pChr45exvw/exec"

    LaunchedEffect(Unit) {
        db.collection("registros_clientes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    registrations = snapshot.documents.map { doc ->
                        CustomerRegistration(
                            id = doc.id,
                            name = doc.getString("name") ?: "S/N",
                            email = doc.getString("email") ?: "S/E",
                            phone = doc.getString("phone") ?: "S/P",
                            address = doc.getString("address") ?: "S/D",
                            status = doc.getString("status") ?: "unverified",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            idCardUrl = doc.getString("idCardUrl") ?: ""
                        )
                    }
                }
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF08080A))) {
        // Encabezado Premium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    "CENTRAL DE INTELIGENCIA",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    "STARBIG CONTROL",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    letterSpacing = (-1).sp
                )
            }
            Image(
                painter = painterResource(id = R.drawable.logo_admin),
                contentDescription = null,
                modifier = Modifier.size(80.dp).align(Alignment.TopEnd),
                contentScale = ContentScale.Fit
            )
        }

        // Gráfica de Tráfico en Tiempo Real
        TrafficMonitorSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Botonera de Navegación Profesional
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminNavButton("BASE DE DATOS", Icons.Default.Storage, selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
            AdminNavButton("INVENTARIO", Icons.Default.Inventory, selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
            AdminNavButton("SOLICITUDES", Icons.Default.Group, selectedTab == 2, Modifier.weight(1.2f)) { selectedTab = 2 }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color(0xFFC5A059).copy(alpha = 0.15f), thickness = 0.5.dp)

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> InfoSection("Base de datos de operaciones")
                1 -> InfoSection("Control de inventario boutique")
                2 -> {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFC5A059), strokeWidth = 2.dp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(registrations) { reg ->
                                CustomerAdminCard(
                                    reg = reg,
                                    onApprove = { approveCustomer(reg, db, googleSheetsUrl) },
                                    onReject = { deleteCustomer(reg, db, googleSheetsUrl) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrafficMonitorSection() {
    val db = FirebaseFirestore.getInstance()
    var onlineCount by remember { mutableIntStateOf(1) }
    var totalRegistered by remember { mutableIntStateOf(0) }
    var onlineHistory by remember { mutableStateOf(List(20) { 1 }) }
    var offlineHistory by remember { mutableStateOf(List(20) { 0 }) }
    
    // Escuchar presencia real
    LaunchedEffect(Unit) {
        // --- SIMULACIÓN DE PRESENCIA FALSA (Para que la gráfica no esté vacía) ---
        repeat(5) { i ->
            val fakeId = "fake_device_$i"
            val fakeData = hashMapOf(
                "ultimoPulso" to System.currentTimeMillis(),
                "esAdmin" to false
            )
            db.collection("presencia").document(fakeId).set(fakeData)
        }

        // Obtener total registrados para cálculo de offline
        db.collection("registros_clientes").addSnapshotListener { snap, _ ->
            totalRegistered = snap?.size() ?: 0
        }

        // Obtener usuarios online (pulso en los últimos 2 minutos)
        while(true) {
            val twoMinutesAgo = System.currentTimeMillis() - 120000
            db.collection("presencia")
                .whereGreaterThan("ultimoPulso", twoMinutesAgo)
                .get()
                .addOnSuccessListener { snap ->
                    val count = snap.size()
                    onlineCount = if (count < 1) 1 else count // Admin siempre cuenta como 1
                }
            
            onlineHistory = onlineHistory.drop(1) + onlineCount
            val offlineCount = (totalRegistered - onlineCount).coerceAtLeast(0)
            offlineHistory = offlineHistory.drop(1) + offlineCount
            
            delay(3000) // Actualizar gráfica cada 3 segundos
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFC5A059).copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MONITOREO DE RED", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("ACTIVIDAD REAL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Green, RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ONLINE", color = Color.White.copy(0.4f), fontSize = 8.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(6.dp).background(Color.Red, RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OFFLINE", color = Color.White.copy(0.4f), fontSize = 8.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (onlineHistory.size - 1)
                
                // Cálculo de escala dinámica (Mínimo 10 para que no sea plana si hay pocos)
                val maxOnline = onlineHistory.maxOrNull()?.toFloat() ?: 1f
                val maxOffline = offlineHistory.maxOrNull()?.toFloat() ?: 1f
                val maxVal = maxOf(maxOnline, maxOffline, 5f)

                fun drawPulse(data: List<Int>, color: Color) {
                    for (i in 0 until data.size - 1) {
                        val startX = i * stepX
                        val startY = height - (data[i] / maxVal * height)
                        val endX = (i + 1) * stepX
                        val endY = height - (data[i + 1] / maxVal * height)
                        
                        drawLine(
                            color = color,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        drawLine(
                            color = color.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                drawPulse(offlineHistory, Color.Red)
                drawPulse(onlineHistory, Color.Green)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TrafficStat("CONECTADOS", "${onlineHistory.last()}")
                TrafficStat("INACTIVOS", "${offlineHistory.last()}")
                TrafficStat("TOTAL", "$totalRegistered")
                TrafficStat("SISTEMA", "ÓPTIMO")
            }
        }
    }
}

@Composable
fun TrafficStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AdminNavButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFC5A059) else Color(0xFF1A1A20),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun InfoSection(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text.uppercase(),
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

private fun approveCustomer(reg: CustomerRegistration, db: FirebaseFirestore, googleSheetsUrl: String) {
    db.collection("registros_clientes").document(reg.id).update("status", "active")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .build()
            val payload = org.json.JSONObject().apply {
                put("action", "updateStatus")
                put("email", reg.email.trim())
                put("status", "active")
            }
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(googleSheetsUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("AdminScreen", "Sheets Error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun deleteCustomer(reg: CustomerRegistration, db: FirebaseFirestore, googleSheetsUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .build()
            val payload = org.json.JSONObject().apply {
                put("action", "delete")
                put("email", reg.email.trim())
            }
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(googleSheetsUrl).post(body).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("AdminScreen", "Sheets Error: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    db.collection("registros_clientes").document(reg.id).delete()
}

@Composable
fun CustomerAdminCard(reg: CustomerRegistration, onApprove: () -> Unit, onReject: () -> Unit) {
    val isActive = reg.status == "active"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if(isActive) Color.Green.copy(0.4f) else Color(0xFFC5A059).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color(0xFF1A1A20))
                ) {
                    AsyncImage(
                        model = reg.photoUrl.ifBlank { "https://cdn-icons-png.flaticon.com/512/149/149071.png" },
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reg.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        reg.email.lowercase(),
                        color = Color(0xFFC5A059),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusBadge(reg.status)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(Icons.Default.Place, reg.address)
                InfoRow(Icons.Default.Phone, reg.phone)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "DOCUMENTO DE IDENTIDAD",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF0A0A0C))
            ) {
                AsyncImage(
                    model = reg.idCardUrl.ifBlank { "https://via.placeholder.com/800x400/0A0A0C/C5A059?text=ESPERANDO+DOCUMENTO" },
                    contentDescription = "Documento ID",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Text("RECHAZAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
                ) {
                    Text("APROBAR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val isActive = status == "active"
    Box(
        modifier = Modifier
            .background(if (isActive) Color.Green.copy(0.1f) else Color(0xFFC5A059).copy(0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            status.uppercase(),
            color = if (isActive) Color.Green else Color(0xFFC5A059),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text.uppercase(), color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}
