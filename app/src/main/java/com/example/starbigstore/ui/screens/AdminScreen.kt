package com.example.starbigstore.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.starbigstore.R
import com.example.starbigstore.data.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val GOOGLE_SHEETS_URL = "https://script.google.com/macros/s/AKfycbzTKwRkgCmy_m42ZeKjPbczOMr0YHmRKiSmrHPCSEdKixHzI9MG3fhEfEU3pChr45exvw/exec"

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

data class PaymentReport(
    val reference: String = "",
    val bank: String = "",
    val date: String = "",
    val phone: String = "",
    val amount: String = "",
    val installments: String = "1",
    val captureBase64: String? = null,
    val timestamp: Long = 0
)

data class OrderItem(
    val name: String = "",
    val buyQty: Int = 1,
    val paymentMethod: String = "cash",
    val priceUsd: Double = 0.0
)

data class Order(
    val id: String = "",
    val orderId: String = "",
    val customerEmail: String = "",
    val customerName: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalUsd: String = "",
    val totalBss: String = "",
    val status: String = "pending",
    val timestamp: Long = 0,
    val paymentReport: PaymentReport? = null,
    val pointsUsed: Int = 0
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
                        Image(painter = painterResource(id = R.drawable.logo_admin), contentDescription = null, modifier = Modifier.size(240.dp).padding(bottom = 32.dp), contentScale = ContentScale.Fit)
                        Text("ACCESO EXCLUSIVO", color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ADMINISTRACIÓN", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                        Spacer(modifier = Modifier.height(48.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            placeholder = { Text("PIN DE SEGURIDAD", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC5A059), unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { if (pinInput == correctPin) isAuthorized = true }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(0.dp)) {
                            Text("AUTENTICAR", color = Color.Black, fontWeight = FontWeight.Black)
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
    var products by remember { mutableStateOf(listOf<Product>()) }
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var bcvRate by remember { mutableDoubleStateOf(36.5) }
    var paymentSettings by remember { mutableStateOf(mapOf("zelle" to "", "binance" to "", "zinli" to "", "pagomovil" to "")) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    // Notificación moderna para Compose
    var snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showModernToast: (String, Boolean) -> Unit = { msg, isError ->
        scope.launch {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val syncBcv = {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("https://ve.dolarapi.com/v1/dolares/oficial").build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (body != null) {
                    val json = org.json.JSONObject(body)
                    val rate = json.getDouble("promedio")
                    db.collection("config").document("tasa_bcv").set(mapOf("valor" to rate))
                    showModernToast("✅ TASA BCV ACTUALIZADA: $rate", false)
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminBCV", "Error: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("registros_clientes").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) registrations = snapshot.documents.map { doc ->
                    CustomerRegistration(id = doc.id, name = doc.getString("name") ?: "S/N", email = doc.getString("email") ?: "S/E", phone = doc.getString("phone") ?: "S/P", address = doc.getString("address") ?: "S/D", status = doc.getString("status") ?: "unverified", photoUrl = doc.getString("photoUrl") ?: "", idCardUrl = doc.getString("idCardUrl") ?: "")
                }
            }

        db.collection("productos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) products = snapshot.documents.map { doc ->
                val imageUrl = listOf("imageUrl", "imagen", "image", "foto", "url", "imagenUrl")
                    .mapNotNull { doc.getString(it) }
                    .firstOrNull { it.isNotEmpty() && !it.contains("subiendo", ignoreCase = true) }
                    ?: doc.getString("imageUrl") ?: ""
                
                Product(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    priceUsd = doc.getDouble("priceUsd") ?: 0.0,
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    collection = doc.getString("collection") ?: "",
                    imageUrl = imageUrl,
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    allowCredit = doc.getBoolean("allowCredit") ?: false
                )
            }
        }

        db.collection("pedidos").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) orders = snapshot.documents.map { doc ->
                    val itemsRaw = doc.get("items") as? List<*>
                    val items = itemsRaw?.mapNotNull { item ->
                        val map = item as? Map<*, *>
                        if (map != null) {
                            OrderItem(
                                name = map["name"] as? String ?: "",
                                buyQty = (map["buyQty"] as? Number)?.toInt() ?: 1,
                                paymentMethod = map["paymentMethod"] as? String ?: "cash",
                                priceUsd = (map["priceUsd"] as? Number)?.toDouble() ?: 0.0
                            )
                        } else null
                    } ?: emptyList()

                    val reportMap = doc.get("paymentReport") as? Map<*, *>
                    val report = reportMap?.let {
                        PaymentReport(
                            reference = it["reference"] as? String ?: "",
                            bank = it["bank"] as? String ?: "",
                            date = it["date"] as? String ?: "",
                            phone = it["phone"] as? String ?: "",
                            amount = it["amount"]?.toString() ?: "",
                            installments = it["installments"]?.toString() ?: "1",
                            captureBase64 = it["captureBase64"] as? String
                        )
                    }

                    Order(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        items = items,
                        totalUsd = doc.getString("totalUsd") ?: "",
                        totalBss = doc.getString("totalBss") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        paymentReport = report,
                        pointsUsed = (doc.getLong("pointsUsed") ?: 0).toInt()
                    )
                }
            }

        db.collection("config").document("tasa_bcv").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) bcvRate = snapshot.getDouble("valor") ?: 36.5
        }

        db.collection("config").document("metodos_pago").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                paymentSettings = snapshot.data as Map<String, String>
            }
        }

        syncBcv()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF08080A))) {
            Box(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp)) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("CENTRAL DE INTELIGENCIA", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("STARBIG CONTROL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
                Image(painter = painterResource(id = R.drawable.logo_admin), contentDescription = null, modifier = Modifier.size(80.dp).align(Alignment.TopEnd), contentScale = ContentScale.Fit)
            }

            TrafficMonitorSection()
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminNavButton("DB", Icons.Default.Storage, selectedTab == 0, Modifier.width(80.dp)) { selectedTab = 0 }
                AdminNavButton("STOCK", Icons.Default.Inventory, selectedTab == 1, Modifier.width(80.dp)) { selectedTab = 1 }
                AdminNavButton("COBROS", Icons.Default.MonetizationOn, selectedTab == 4, Modifier.width(80.dp)) { selectedTab = 4 }
                AdminNavButton("PAGOS", Icons.Default.Payments, selectedTab == 3, Modifier.width(80.dp)) { selectedTab = 3 }
                AdminNavButton("SOLIC.", Icons.Default.Group, selectedTab == 2, Modifier.width(80.dp)) { selectedTab = 2 }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFC5A059).copy(alpha = 0.15f), thickness = 0.5.dp)

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        val actives = registrations.filter { it.status == "active" }
                        if (actives.isEmpty()) InfoSection("No hay usuarios activos")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(actives) { reg -> CustomerAdminCard(reg, {}, { deleteCustomer(reg, db) }, { expandedImageUrl = it }) }
                        }
                    }
                    1 -> InventorySection(products, bcvRate, { showAddProductDialog = true }, { showRateDialog = true }, { syncBcv() }, { deleteProduct(it, db) }, { productToEdit = it })
                    2 -> {
                        val pendings = registrations.filter { it.status == "unverified" }
                        if (pendings.isEmpty()) InfoSection("No hay solicitudes")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(pendings) { reg -> CustomerAdminCard(reg, { approveCustomer(reg, db) }, { deleteCustomer(reg, db) }, { expandedImageUrl = it }) }
                        }
                    }
                    3 -> PaymentSettingsSection(paymentSettings) { updatedSettings ->
                        isLoading = true
                        db.collection("config").document("metodos_pago").set(updatedSettings).addOnSuccessListener {
                            isLoading = false
                            showModernToast("✅ PAGOS ACTUALIZADOS", false)
                        }.addOnFailureListener {
                            isLoading = false
                            showModernToast("❌ ERROR AL GUARDAR", true)
                        }
                    }
                    4 -> CollectionsSection(
                        orders = orders,
                        onConfirmPayment = { confirmOrderPayment(it, db, showModernToast) },
                        onRejectPayment = { rejectOrderPayment(it, db, showModernToast) },
                        onDeleteSale = { deleteOrder(it, db, showModernToast) },
                        onClearHistory = { clearCompletedOrders(db, showModernToast) },
                        onImageClick = { expandedImageUrl = it }
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFC5A059))
            }
        }

        if (expandedImageUrl != null) {
            Dialog(onDismissRequest = { expandedImageUrl = null }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.9f)).clickable { expandedImageUrl = null }, contentAlignment = Alignment.Center) {
                    val imageData = if (expandedImageUrl!!.startsWith("data:image")) {
                        try {
                            val base64String = expandedImageUrl!!.substringAfter("base64,")
                            android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        } catch(e: Exception) { expandedImageUrl }
                    } else {
                        expandedImageUrl
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageData)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.95f),
                        contentScale = ContentScale.Fit,
                        error = painterResource(R.drawable.logo_admin)
                    )
                }
            }
        }

        if (showAddProductDialog || productToEdit != null) {
            val oldCategory = productToEdit?.category
            val oldName = productToEdit?.name
            val isEdit = productToEdit != null

            AddProductDialog(productToEdit, { showAddProductDialog = false; productToEdit = null }, { newProd, uri ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var url = newProd.imageUrl
                        if (uri != null) {
                            val storage = FirebaseStorage.getInstance()
                            val ref = storage.reference.child("productos/${UUID.randomUUID()}.jpg")
                            try {
                                ref.putFile(uri).await()
                                // Reintento corto para asegurar que el objeto se propague en Google Cloud
                                var downloadUri: Uri? = null
                                repeat(3) {
                                    try {
                                        downloadUri = ref.downloadUrl.await()
                                        if (downloadUri != null) return@repeat
                                    } catch (_: Exception) {
                                        delay(1000)
                                    }
                                }
                                url = downloadUri?.toString() ?: ""
                            } catch (_: Exception) {
                                android.util.Log.e("Admin", "Error subiendo imagen")
                            }
                        }
                        
                        val finalProd = newProd.copy(imageUrl = url)
                        val docId = if (isEdit) {
                            db.collection("productos").document(productToEdit!!.id).set(finalProd).await()
                            productToEdit!!.id
                        } else {
                            db.collection("productos").add(finalProd).await().id
                        }
                        
                        // Convertir a Base64 para Google Drive (Sheets)
                        val base64 = uri?.let { u ->
                            try {
                                context.contentResolver.openInputStream(u)?.use { it.readBytes() }?.let { 
                                    android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                                }
                            } catch (_: Exception) { null }
                        }

                        // Sincronizar en segundo plano y obtener link de Drive
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val normalizedCategory = java.text.Normalizer.normalize(finalProd.category, java.text.Normalizer.Form.NFD)
                                    .replace("[\\u0300-\\u036f]+".toRegex(), "")
                                    .uppercase()

                                val json = org.json.JSONObject().apply {
                                    put("action", "addProduct")
                                    put("sheetName", normalizedCategory)

                                    if (isEdit) {
                                        val normOldCat = if (oldCategory != null) java.text.Normalizer.normalize(oldCategory, java.text.Normalizer.Form.NFD)
                                            .replace("[\\u0300-\\u036f]+".toRegex(), "")
                                            .uppercase() else null
                                        put("oldCategory", normOldCat)
                                        put("oldName", oldName)
                                    }

                                    put("photoBase64", base64)
                                    put("fileName", "PROD_${System.currentTimeMillis()}_${finalProd.name.replace(" ", "_")}.jpg")
                                    put("folderName", "PRODUCTOS_STARBIG")
                                    put("data", org.json.JSONObject().apply {
                                        put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                                        put("nombre", finalProd.name); put("precio", finalProd.priceUsd); put("stock", finalProd.stock)
                                        put("coleccion", finalProd.collection); put("descripcion", finalProd.description)
                                        put("credito", if(finalProd.allowCredit) "SÍ" else "NO"); put("imagen", url)
                                    })
                                }
                                val response = OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
                                val respBody = response.body?.string()
                                if (respBody != null) {
                                    val resJson = org.json.JSONObject(respBody)
                                    val driveUrl = resJson.optString("imageUrl")
                                    if (driveUrl.isNotEmpty()) {
                                        // Actualizamos Firebase con el link de Drive que sí funciona
                                        db.collection("productos").document(docId).update("imageUrl", driveUrl)
                                    }
                                }
                            } catch (_: Exception) {
                                android.util.Log.e("AdminSync", "Error sincronizando link de Drive")
                            }
                        }

                        CoroutineScope(Dispatchers.Main).launch { 
                            isLoading = false
                            showAddProductDialog = false
                            productToEdit = null
                            showModernToast("✅ GUARDADO CON ÉXITO", false)
                        }
                    } catch (_: Exception) {
                        CoroutineScope(Dispatchers.Main).launch { 
                            isLoading = false
                            showModernToast("❌ ERROR AL GUARDAR", true)
                        }
                    }
                }
            })
        }

        if (showRateDialog) {
            BcvRateDialog(bcvRate, { showRateDialog = false }, { db.collection("config").document("tasa_bcv").set(mapOf("valor" to it)); showRateDialog = false })
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        ) { data ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.5f))
            ) {
                Text(
                    text = data.visuals.message.uppercase(),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
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
    LaunchedEffect(Unit) {
        db.collection("registros_clientes").addSnapshotListener { snap, _ -> totalRegistered = snap?.size() ?: 0 }
        while(true) {
            val twoMinutesAgo = System.currentTimeMillis() - 120000
            db.collection("presencia").whereGreaterThan("ultimoPulso", twoMinutesAgo).get().addOnSuccessListener { onlineCount = if (it.size() < 1) 1 else it.size() }
            onlineHistory = onlineHistory.drop(1) + onlineCount
            offlineHistory = offlineHistory.drop(1) + (totalRegistered - onlineCount).coerceAtLeast(0)
            delay(3000)
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp), border = BorderStroke(0.5.dp, Color(0xFFC5A059).copy(0.3f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("MONITOREO DE RED", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("ACTIVIDAD REAL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val stepX = size.width / (onlineHistory.size - 1)
                val maxVal = maxOf(onlineHistory.maxOrNull()?.toFloat() ?: 1f, offlineHistory.maxOrNull()?.toFloat() ?: 1f, 5f)
                fun draw(data: List<Int>, color: Color) {
                    for (i in 0 until data.size - 1) {
                        drawLine(color, androidx.compose.ui.geometry.Offset(i * stepX, size.height - (data[i] / maxVal * size.height)), androidx.compose.ui.geometry.Offset((i + 1) * stepX, size.height - (data[i + 1] / maxVal * size.height)), 2.dp.toPx())
                    }
                }
                draw(offlineHistory, Color.Red)
                draw(onlineHistory, Color.Green)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TrafficStat("ONLINE", "${onlineHistory.last()}")
                TrafficStat("OFFLINE", "${offlineHistory.last()}")
                TrafficStat("TOTAL", "$totalRegistered")
            }
        }
    }
}

@Composable
fun TrafficStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun AdminNavButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFC5A059) else Color(0xFF1A1A20), contentColor = if (isSelected) Color.Black else Color.White)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoSection(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text.uppercase(), color = Color.White.copy(0.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

private fun approveCustomer(reg: CustomerRegistration, db: FirebaseFirestore) {
    db.collection("registros_clientes").document(reg.id).update("status", "active")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { put("action", "updateStatus"); put("email", reg.email.trim()); put("status", "active") }
            OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        } catch (_: Exception) {}
    }
}

private fun deleteCustomer(reg: CustomerRegistration, db: FirebaseFirestore) {
    // 1. Borrar de Firestore
    db.collection("registros_clientes").document(reg.id).delete()
    
    // 2. Sincronizar con Excel y Firebase Auth vía Apps Script
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { 
                put("action", "delete")
                put("email", reg.email.trim())
                put("deleteFromAuth", true) 
                put("photoUrl", reg.photoUrl)
                put("idCardUrl", reg.idCardUrl)
            }
            val request = Request.Builder()
                .url(GOOGLE_SHEETS_URL)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0")
                .build()
            OkHttpClient().newCall(request).execute()
        } catch (_: Exception) {}
    }
}

private fun deleteProduct(p: Product, db: FirebaseFirestore) {
    // 1. Borrar de Firestore
    db.collection("productos").document(p.id).delete()
    
    // 2. Sincronizar con Excel
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val normalizedCategory = java.text.Normalizer.normalize(p.category, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .uppercase()

            val json = org.json.JSONObject().apply {
                put("action", "deleteProduct")
                put("sheetName", normalizedCategory)
                put("nombre", p.name)
            }
            val request = Request.Builder()
                .url(GOOGLE_SHEETS_URL)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0")
                .build()
            OkHttpClient().newCall(request).execute()
        } catch (_: Exception) {}
    }
}

@Composable
fun CustomerAdminCard(reg: CustomerRegistration, onApprove: () -> Unit, onReject: () -> Unit, onImageClick: (String) -> Unit) {
    val isActive = reg.status == "active"
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp), border = BorderStroke(0.5.dp, if(isActive) Color.Green.copy(0.4f) else Color(0xFFC5A059).copy(0.2f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFF1A1A20)).clickable { onImageClick(reg.photoUrl) }) { 
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fixDriveUrl(reg.photoUrl))
                            .setHeader("User-Agent", "Mozilla/5.0")
                            .crossfade(true)
                            .build(),
                        contentDescription = null, 
                        modifier = Modifier.fillMaxSize(), 
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.logo_admin),
                        placeholder = painterResource(R.drawable.logo_admin)
                    ) 
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(reg.name.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                    Text(reg.email, color = Color(0xFFC5A059), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusBadge(reg.status)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(Icons.Default.Place, reg.address)
            InfoRow(Icons.Default.Phone, reg.phone)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black).clickable { onImageClick(reg.idCardUrl) }) { 
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fixDriveUrl(reg.idCardUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null, 
                    modifier = Modifier.fillMaxSize(), 
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.logo_admin)
                ) 
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red), shape = RoundedCornerShape(0.dp)) { Text("ELIMINAR") }
                if(!isActive) Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(0.dp)) { Text("APROBAR", color = Color.Black) }
            }
        }
    }
}

@Composable
fun InventorySection(products: List<Product>, bcv: Double, onAdd: () -> Unit, onRate: () -> Unit, onSync: () -> Unit, onDelete: (Product) -> Unit, onEdit: (Product) -> Unit) {
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos") + products.map { it.category }.distinct().sorted()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TASA BCV", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$bcv BSS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onSync, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFC5A059), modifier = Modifier.size(18.dp))
                }
            }
            Button(onClick = onRate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A20)), shape = RoundedCornerShape(0.dp)) { Text("TASA", color = Color(0xFFC5A059)) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        
        val tabIndex = categories.indexOf(selectedCategory).let { if (it == -1) 0 else it }
        
        SecondaryScrollableTabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFC5A059),
            edgePadding = 0.dp,
            divider = {}
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(selectedCategory == cat) Color(0xFFC5A059) else Color.White.copy(0.4f)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PRODUCTOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = Color(0xFFC5A059)) }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val filtered = if(selectedCategory == "Todos") products else products.filter { it.category == selectedCategory }
            items(filtered) { ProductAdminItem(it, bcv, onDelete, onEdit) }
        }
    }
}

@Composable
fun ProductAdminItem(p: Product, bcv: Double, onDelete: (Product) -> Unit, onEdit: (Product) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fixDriveUrl(p.imageUrl))
                    .setHeader("User-Agent", "Mozilla/5.0")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(50.dp).background(Color.Black),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.logo_admin),
                placeholder = painterResource(R.drawable.logo_admin)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row {
                    Text("$${p.priceUsd}", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${(p.priceUsd * bcv).format(2)} BSS", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                Text("STOCK: ${p.stock}", color = Color.White.copy(0.5f), fontSize = 9.sp)
            }
            IconButton(onClick = { onEdit(p) }) { Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f), modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(editingProduct: Product? = null, onDismiss: () -> Unit, onConfirm: (Product, Uri?) -> Unit) {
    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var priceText by remember { mutableStateOf(editingProduct?.priceUsd?.toString() ?: "") }
    var stock by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "") }
    var description by remember { mutableStateOf(editingProduct?.description ?: "") }
    var allowCredit by remember { mutableStateOf(editingProduct?.allowCredit ?: false) }
    var selectedCat by remember { mutableStateOf(editingProduct?.category ?: "Perfumes") }
    var selectedColl by remember { mutableStateOf(editingProduct?.collection ?: "Nueva Temporada") }
    var catExp by remember { mutableStateOf(false) }
    var collExp by remember { mutableStateOf(false) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri = it }
    
    val pUsd = priceText.toDoubleOrNull() ?: 0.0

    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF121216), shape = RoundedCornerShape(0.dp),
        title = { Text(if(editingProduct == null) "NUEVO PRODUCTO" else "EDITAR PRODUCTO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Black).clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    if(uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize())
                    else if(editingProduct?.imageUrl?.isNotEmpty() == true) AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fixDriveUrl(editingProduct.imageUrl))
                            .crossfade(true)
                            .build(),
                        contentDescription = null, 
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(R.drawable.logo_admin)
                    )
                    else Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFC5A059))
                }
                AdminTextField(name, { name = it }, "Nombre")
                AdminTextField(priceText, { priceText = it }, "Precio USD")
                AdminTextField(stock, { stock = it }, "Stock")
                AdminTextField(description, { description = it }, "Descripción")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(allowCredit, { allowCredit = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC5A059))); Text("CRÉDITO DISPONIBLE", color = Color.White, fontSize = 11.sp) }
                
                ExposedDropdownMenuBox(catExp, { catExp = it }) {
                    OutlinedTextField(selectedCat, {}, readOnly = true, label = {Text("Categoría")}, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), colors = dropdownColors())
                    ExposedDropdownMenu(catExp, { catExp = false }) {
                        listOf("Perfumes", "Tecnología", "Ropa", "Calzado", "Belleza").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedCat = it; catExp = false }) }
                    }
                }
                ExposedDropdownMenuBox(collExp, { collExp = it }) {
                    OutlinedTextField(selectedColl, {}, readOnly = true, label = {Text("Colección")}, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(collExp) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), colors = dropdownColors())
                    ExposedDropdownMenu(collExp, { collExp = false }) {
                        listOf("Nueva Temporada", "Edición Limitada", "Best Sellers").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedColl = it; collExp = false }) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(Product(id = editingProduct?.id ?: "", name = name, priceUsd = pUsd, description = description, category = selectedCat, collection = selectedColl, stock = stock.toIntOrNull() ?: 0, allowCredit = allowCredit, imageUrl = editingProduct?.imageUrl ?: ""), uri) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))) { Text("GUARDAR", color = Color.Black) } }
    )
}

@Composable
fun PaymentSettingsSection(currentSettings: Map<String, String>, onSave: (Map<String, String>) -> Unit) {
    var zelle by remember(currentSettings) { mutableStateOf(currentSettings["zelle"] ?: "") }
    var binance by remember(currentSettings) { mutableStateOf(currentSettings["binance"] ?: "") }
    var zinli by remember(currentSettings) { mutableStateOf(currentSettings["zinli"] ?: "") }
    var pagomovil by remember(currentSettings) { mutableStateOf(currentSettings["pagomovil"] ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("CONFIGURACIÓN DE PAGOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        
        AdminLargeTextField(zelle, { zelle = it }, "DATOS ZELLE")
        AdminLargeTextField(binance, { binance = it }, "DATOS BINANCE")
        AdminLargeTextField(zinli, { zinli = it }, "DATOS ZINLI")
        AdminLargeTextField(pagomovil, { pagomovil = it }, "DATOS PAGO MÓVIL")

        Button(
            onClick = { onSave(mapOf("zelle" to zelle, "binance" to binance, "zinli" to zinli, "pagomovil" to pagomovil)) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Text("GUARDAR CAMBIOS", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AdminLargeTextField(v: String, onV: (String) -> Unit, l: String) {
    OutlinedTextField(
        value = v,
        onValueChange = onV,
        label = { Text(l, fontSize = 10.sp, color = Color(0xFFC5A059)) },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFC5A059),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(0.dp)
    )
}

@Composable
fun CollectionsSection(
    orders: List<Order>,
    onConfirmPayment: (Order) -> Unit,
    onRejectPayment: (Order) -> Unit,
    onDeleteSale: (Order) -> Unit,
    onClearHistory: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORIAL DE COBROS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = onClearHistory) {
                Text("LIMPIAR", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        if (orders.isEmpty()) {
            InfoSection("No hay pedidos registrados")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(orders) { order ->
                    OrderAdminCard(order, onConfirmPayment, onRejectPayment, onDeleteSale, onImageClick)
                }
            }
        }
    }
}

@Composable
fun OrderAdminCard(
    order: Order,
    onConfirm: (Order) -> Unit,
    onReject: (Order) -> Unit,
    onDelete: (Order) -> Unit,
    onImageClick: (String) -> Unit
) {
    val date = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp), border = BorderStroke(0.5.dp, Color(0xFFC5A059).copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderId, color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date, color = Color.White.copy(0.4f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDelete(order) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.4f), modifier = Modifier.size(14.dp))
                    }
                }
            }
            
            Text(order.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            order.items.forEach { item ->
                Text("• ${item.buyQty}x ${item.name} (${item.paymentMethod.uppercase()})", color = Color.White.copy(0.6f), fontSize = 11.sp)
            }
            
            order.paymentReport?.let { report ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = Color(0xFFC5A059).copy(0.05f), border = BorderStroke(0.5.dp, Color(0xFFC5A059).copy(0.2f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("DETALLES DEL PAGO:", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        PaymentDetailItem("REF", report.reference)
                        PaymentDetailItem("BANCO", report.bank)
                        PaymentDetailItem("MONTO", "${report.amount} BSS")
                        if(report.installments.toInt() > 1) PaymentDetailItem("CUOTAS", report.installments)
                        
                        report.captureBase64?.let { base64 ->
                            if (base64.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(Color.Black)
                                        .clickable { onImageClick("data:image/jpeg;base64,$base64") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(try { android.util.Base64.decode(base64, android.util.Base64.DEFAULT) } catch(e: Exception) { null })
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        error = painterResource(R.drawable.logo_admin),
                                        placeholder = painterResource(R.drawable.logo_admin)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.totalUsd, color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 16.sp)
                
                when (order.status) {
                    "pending", "awaiting_verification" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onConfirm(order) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(32.dp)) {
                                Text("APROBAR", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Button(onClick = { onReject(order) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(4.dp), modifier = Modifier.height(32.dp)) {
                                Text("RECHAZAR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    "paid" -> Text("PAGADO", color = Color(0xFF25D366), fontWeight = FontWeight.Black, fontSize = 10.sp)
                    "rejected" -> Text("RECHAZADO", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun PaymentDetailItem(label: String, value: String) {
    Row {
        Text("$label: ", color = Color.White.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 10.sp)
    }
}

private fun confirmOrderPayment(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            var totalPointsToGain = 0
            order.items.forEach { if(it.paymentMethod == "cash") totalPointsToGain += 25 }
            if (order.items.any { it.paymentMethod == "credit" }) totalPointsToGain += 10
            
            if (totalPointsToGain > 0) {
                val userSnap = db.collection("registros_clientes").whereEqualTo("email", order.customerEmail).get().await()
                if (!userSnap.isEmpty) {
                    val userDoc = userSnap.documents[0]
                    val currentPoints = userDoc.getLong("points") ?: 0
                    userDoc.reference.update("points", currentPoints + totalPointsToGain).await()
                }
            }
            
            db.collection("pedidos").document(order.id).update("status", "paid").await()
            CoroutineScope(Dispatchers.Main).launch { showToast("✅ PAGO CONFIRMADO", false) }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ ERROR AL CONFIRMAR", true) }
        }
    }
}

private fun rejectOrderPayment(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            if (order.pointsUsed > 0) {
                val userSnap = db.collection("registros_clientes").whereEqualTo("email", order.customerEmail).get().await()
                if (!userSnap.isEmpty) {
                    val userDoc = userSnap.documents[0]
                    val currentPoints = userDoc.getLong("points") ?: 0
                    userDoc.reference.update("points", currentPoints + order.pointsUsed).await()
                }
            }
            db.collection("pedidos").document(order.id).update("status", "rejected").await()
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ PAGO RECHAZADO", true) }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ ERROR AL RECHAZAR", true) }
        }
    }
}

private fun deleteOrder(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("pedidos").document(order.id).delete().addOnSuccessListener {
        showToast("Registro eliminado", false)
    }
}

private fun clearCompletedOrders(db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("pedidos").whereIn("status", listOf("paid", "rejected")).get().addOnSuccessListener { snap ->
        val batch = db.batch()
        snap.documents.forEach { batch.delete(it.reference) }
        batch.commit().addOnSuccessListener { showToast("✅ HISTORIAL LIMPIO", false) }
    }
}

@Composable
fun StatusBadge(s: String) {
    Box(modifier = Modifier.background(if (s == "active") Color.Green.copy(0.1f) else Color.Yellow.copy(0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(s.uppercase(), color = if (s == "active") Color.Green else Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(i: androidx.compose.ui.graphics.vector.ImageVector, t: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(i, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(t.uppercase(), color = Color.White.copy(0.6f), fontSize = 10.sp) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dropdownColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC5A059), unfocusedBorderColor = Color.White.copy(0.1f), focusedLabelColor = Color(0xFFC5A059), focusedTextColor = Color.White, unfocusedTextColor = Color.White)

@Composable
fun AdminTextField(v: String, onV: (String) -> Unit, l: String) {
    OutlinedTextField(v, onV, label = { Text(l, fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC5A059), unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(0.dp), singleLine = true)
}

@Composable
fun BcvRateDialog(curr: Double, onD: () -> Unit, onC: (Double) -> Unit) {
    var r by remember { mutableStateOf(curr.toString()) }
    AlertDialog(onDismissRequest = onD, containerColor = Color(0xFF121216), title = { Text("TASA BCV", color = Color.White) }, text = { AdminTextField(r, { r = it }, "Valor BSS") }, confirmButton = { Button(onClick = { onC(r.toDoubleOrNull() ?: curr) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))) { Text("OK", color = Color.Black) } })
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)

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
