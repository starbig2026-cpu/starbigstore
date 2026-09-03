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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import java.util.Date
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.print.PrintHelper
import java.io.File
import java.io.FileOutputStream
import android.content.Intent

import java.util.UUID

private const val GOOGLE_SHEETS_URL = "https://script.google.com/macros/s/AKfycbzTKwRkgCmy_m42ZeKjPbczOMr0YHmRKiSmrHPCSEdKixHzI9MG3fhEfEU3pChr45exvw/exec"

data class News(
    val id: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0
)

data class CreditRequest(
    val id: String = "",
    val customerEmail: String = "",
    val customerName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val reason: String = "",
    val bank: String = "",
    val account: String = "",
    val amountUsd: Double = 0.0,
    val amountBss: String = "",
    val status: String = "pending",
    val timestamp: Long = 0,
    val adminComment: String = "",
    val paymentReport: PaymentReport? = null,
    val plan: String = "",
    val installmentsPaid: Int = 0,
    val remainingDebt: Double? = null,
    val dueDate: String = "",
    val bcvAtRequest: Double = 0.0
)

data class CustomerRegistration(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val idNumber: String = "",
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
    val idNumber: String = "",
    val phone: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalUsd: String = "",
    val totalBss: String = "",
    val status: String = "pending",
    val timestamp: Long = 0,
    val paymentReport: PaymentReport? = null,
    val pointsUsed: Int = 0,
    val collection: String = "pedidos",
    val hasCredit: Boolean = false,
    val remainingDebt: Double? = null,
    val numCuotas: Int = 2,
    val archivedFromCobros: Boolean = false
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC5A059),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color(0xFF1A1A20),
                                unfocusedContainerColor = Color(0xFF1A1A20)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { if (pinInput == correctPin) isAuthorized = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
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
    var isMenuExpanded by remember { mutableStateOf(true) }
    var registrations by remember { mutableStateOf(listOf<CustomerRegistration>()) }
    var products by remember { mutableStateOf(listOf<Product>()) }
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var creditRequests by remember { mutableStateOf(listOf<CreditRequest>()) }
    var newsList by remember { mutableStateOf(listOf<News>()) }
    var bcvRate by remember { mutableDoubleStateOf(36.5) }
    var paymentSettings by remember { mutableStateOf(mapOf("zelle" to "", "binance" to "", "zinli" to "", "pagomovil" to "")) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddNewsDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerRegistration?>(null) }
    var newsToDelete by remember { mutableStateOf<News?>(null) }
    var creditRequestToDelete by remember { mutableStateOf<CreditRequest?>(null) }
    var creditRequestToReceipt by remember { mutableStateOf<CreditRequest?>(null) }
    var orderToReceipt by remember { mutableStateOf<Order?>(null) }
    var showRateDialog by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    // Notificación moderna para Compose
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showModernToast: (String, Boolean) -> Unit = { msg, _ ->
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

    fun refreshCombinedList(db: FirebaseFirestore) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pedidos = db.collection("pedidos").orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                val credits = db.collection("solicitudes_credito").orderBy("timestamp", Query.Direction.DESCENDING).get().await()

                val allOrders = mutableListOf<Order>()

                pedidos.documents.forEach { doc ->
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

                    val hasCreditDoc = doc.getBoolean("hasCredit") ?: items.any { it.paymentMethod == "credit" }

                    allOrders.add(Order(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "ORDEN",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        idNumber = doc.getString("idNumber") ?: "",
                        phone = doc.getString("phone") ?: "",
                        items = items,
                        totalUsd = doc.getString("totalUsd") ?: "",
                        totalBss = doc.getString("totalBss") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        paymentReport = report,
                        pointsUsed = (doc.getLong("pointsUsed") ?: 0).toInt(),
                        collection = "pedidos",
                        hasCredit = hasCreditDoc,
                        remainingDebt = doc.getDouble("remainingDebt"),
                        numCuotas = (doc.getLong("numCuotas") ?: 2).toInt(),
                        archivedFromCobros = doc.getBoolean("archivedFromCobros") ?: false
                    ))
                }

                credits.documents.forEach { doc ->
                    val reportMap = doc.get("paymentReport") as? Map<*, *>
                    if (reportMap != null) {
                        val report = PaymentReport(
                            reference = reportMap["reference"] as? String ?: "",
                            bank = reportMap["bank"] as? String ?: "",
                            date = reportMap["date"] as? String ?: "",
                            phone = reportMap["phone"] as? String ?: "",
                            amount = reportMap["amount"]?.toString() ?: "",
                            installments = reportMap["installments"]?.toString() ?: "1",
                            captureBase64 = reportMap["captureBase64"] as? String
                        )

                        allOrders.add(Order(
                            id = doc.id,
                            orderId = "CRÉDITO",
                            customerEmail = doc.getString("customerEmail") ?: "",
                            customerName = doc.getString("customerName") ?: "",
                            idNumber = doc.getString("idNumber") ?: "",
                            phone = doc.getString("phone") ?: "",
                            items = listOf(OrderItem(name = "ABONO A CRÉDITO PERSONAL", buyQty = 1, paymentMethod = "CRÉDITO", priceUsd = doc.getDouble("amountUsd") ?: 0.0)),
                            totalUsd = "$${doc.getDouble("amountUsd") ?: 0.0}",
                            totalBss = doc.getString("amountBss") ?: "",
                            status = doc.getString("status") ?: "pending",
                            timestamp = doc.getLong("timestamp") ?: 0,
                            paymentReport = report,
                            collection = "solicitudes_credito",
                            hasCredit = true,
                            remainingDebt = doc.getDouble("remainingDebt")
                        ))
                    }
                }

                orders = allOrders.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                android.util.Log.e("AdminScreen", "Refresh Error", e)
            }
        }
    }

    fun syncCombinedOrders(db: FirebaseFirestore) {
        db.collection("pedidos").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { _, _ -> refreshCombinedList(db) }
        db.collection("solicitudes_credito").addSnapshotListener { _, _ -> refreshCombinedList(db) }
    }

    LaunchedEffect(Unit) {
        db.collection("registros_clientes").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) registrations = snapshot.documents.map { doc ->
                    CustomerRegistration(
                        id = doc.id, 
                        name = doc.getString("name") ?: "S/N", 
                        email = doc.getString("email") ?: "S/E", 
                        phone = doc.getString("phone") ?: "S/P", 
                        address = doc.getString("address") ?: "S/D", 
                        idNumber = doc.getString("idNumber") ?: "S/C",
                        status = doc.getString("status") ?: "unverified", 
                        photoUrl = doc.getString("photoUrl") ?: "", 
                        idCardUrl = doc.getString("idCardUrl") ?: ""
                    )
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

        syncCombinedOrders(db)

        db.collection("novedades").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) newsList = snapshot.documents.map { doc ->
                    News(id = doc.id, imageUrl = doc.getString("imageUrl") ?: "", timestamp = doc.getLong("timestamp") ?: 0)
                }
            }
        db.collection("solicitudes_credito").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) creditRequests = snapshot.documents.map { doc ->
                    CreditRequest(
                        id = doc.id,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        idNumber = doc.getString("idNumber") ?: "",
                        phone = doc.getString("phone") ?: "",
                        reason = doc.getString("reason") ?: "",
                        bank = doc.getString("bank") ?: "",
                        account = doc.getString("account") ?: "",
                        amountUsd = doc.getDouble("amountUsd") ?: 0.0,
                        amountBss = doc.getString("amountBss") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        adminComment = doc.getString("adminComment") ?: "",
                        plan = doc.getString("plan") ?: "",
                        dueDate = doc.getString("dueDate") ?: "",
                        bcvAtRequest = doc.getDouble("bcvAtRequest") ?: 0.0
                    )
                }
            }

        db.collection("config").document("tasa_bcv").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) bcvRate = snapshot.getDouble("valor") ?: 36.5
        }

        db.collection("config").document("metodos_pago").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                paymentSettings = snapshot.data?.mapValues { it.value.toString() } ?: emptyMap()
            }
        }

        syncBcv()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF08080A))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121216))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("PANEL DE CONTROL", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("ADMINISTRACIÓN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
                Image(painter = painterResource(id = R.drawable.logo_admin), contentDescription = null, modifier = Modifier.size(60.dp).align(Alignment.CenterEnd), contentScale = ContentScale.Fit)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Color(0xFF121216),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC5A059).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMenuExpanded = !isMenuExpanded }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = when(selectedTab) {
                                    0 -> Icons.Default.Group
                                    1 -> Icons.Default.Inventory
                                    2 -> Icons.Default.HourglassEmpty
                                    3 -> Icons.Default.Payments
                                    4 -> Icons.Default.MonetizationOn
                                    5 -> Icons.Default.Campaign
                                    6 -> Icons.Default.CreditCard
                                    7 -> Icons.AutoMirrored.Filled.FactCheck
                                    8 -> Icons.Default.Assessment
                                    else -> Icons.Default.Menu
                                },
                                contentDescription = null,
                                tint = Color(0xFFC5A059),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when(selectedTab) {
                                    0 -> "USUARIOS ACTIVOS"
                                    1 -> "INVENTARIO Y STOCK"
                                    2 -> "SOLICITUDES CLIENTE"
                                    3 -> "AJUSTES DE PAGOS"
                                    4 -> "HISTORIAL COBRANZAS"
                                    5 -> "NOVEDADES Y FLYERS"
                                    6 -> "SOLICITUDES CRÉDITO"
                                    7 -> "CRÉDITOS OTORGADOS"
                                    8 -> "RESUMEN DE VENTAS"
                                    else -> "MENÚ PRINCIPAL"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isMenuExpanded) "OCULTAR MENÚ ▲" else "VER MENÚ ▼",
                            color = Color(0xFFC5A059),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isMenuExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminNavButton("USUARIOS", Icons.Default.Group, selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0; isMenuExpanded = false }
                                AdminNavButton("STOCK", Icons.Default.Inventory, selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1; isMenuExpanded = false }
                                AdminNavButton("SOLICITUD", Icons.Default.HourglassEmpty, selectedTab == 2, Modifier.weight(1f)) { selectedTab = 2; isMenuExpanded = false }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminNavButton("PAGOS", Icons.Default.Payments, selectedTab == 3, Modifier.weight(1f)) { selectedTab = 3; isMenuExpanded = false }
                                AdminNavButton("COBROS", Icons.Default.MonetizationOn, selectedTab == 4, Modifier.weight(1f)) { selectedTab = 4; isMenuExpanded = false }
                                AdminNavButton("NOVEDAD", Icons.Default.Campaign, selectedTab == 5, Modifier.weight(1f)) { selectedTab = 5; isMenuExpanded = false }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminNavButton("OTORGADOS", Icons.AutoMirrored.Filled.FactCheck, selectedTab == 7, Modifier.weight(1f)) { selectedTab = 7; isMenuExpanded = false }
                                AdminNavButton("CRÉDITOS", Icons.Default.CreditCard, selectedTab == 6, Modifier.weight(1f)) { selectedTab = 6; isMenuExpanded = false }
                                AdminNavButton("RESUMEN", Icons.Default.Assessment, selectedTab == 8, Modifier.weight(1f)) { selectedTab = 8; isMenuExpanded = false }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        val actives = registrations.filter { it.status == "active" }
                        if (actives.isEmpty()) InfoSection("No hay usuarios activos")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(actives) { reg -> CustomerAdminCard(reg, {}, { customerToDelete = reg }, { expandedImageUrl = it }) }
                        }
                    }
                    1 -> InventorySection(products, bcvRate, { showAddProductDialog = true }, { showRateDialog = true }, { syncBcv() }, { productToDelete = it }, { productToEdit = it })
                    2 -> {
                        val pendings = registrations.filter { it.status == "unverified" }
                        if (pendings.isEmpty()) InfoSection("No hay solicitudes")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(pendings) { reg -> CustomerAdminCard(reg, { approveCustomer(reg, db) }, { customerToDelete = reg }, { expandedImageUrl = it }) }
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
                        orders = orders.filter { !it.archivedFromCobros },
                        onConfirmPayment = { confirmOrderPayment(it, db, showModernToast) },
                        onRejectPayment = { rejectOrderPayment(it, db, showModernToast) },
                        onDeleteSale = { deleteOrder(it, db, showModernToast) },
                        onClearHistory = { clearCompletedOrders(db, showModernToast) },
                        onViewReceipt = { orderToReceipt = it },
                        onImageClick = { expandedImageUrl = it }
                    )
                    5 -> NewsAdminSection(
                        newsList = newsList,
                        onAddNews = { showAddNewsDialog = true },
                        onDeleteNews = { newsToDelete = it },
                        onImageClick = { expandedImageUrl = it }
                    )
                    6 -> CreditRequestsSection(
                        requests = creditRequests.filter { it.status == "pending" || it.status == "denied" },
                        registrations = registrations,
                        onApprove = { req: CreditRequest, comment: String -> updateCreditRequest(req, "approved", comment, db, showModernToast) },
                        onDeny = { req: CreditRequest, comment: String -> updateCreditRequest(req, "denied", comment, db, showModernToast) },
                        onDelete = { creditRequestToDelete = it },
                        onImageClick = { expandedImageUrl = it }
                    )
                    7 -> CreditRequestsSection(
                        requests = creditRequests.filter { it.status == "approved" || it.status == "paid" },
                        registrations = registrations,
                        onApprove = { _, _ -> },
                        onDeny = { _, _ -> },
                        onDelete = { creditRequestToDelete = it },
                        onViewReceipt = { creditRequestToReceipt = it },
                        onImageClick = { expandedImageUrl = it }
                    )
                    8 -> SalesSummaryAndSearchSection(
                        orders = orders,
                        creditRequests = creditRequests,
                        registrations = registrations,
                        onViewOrderReceipt = { orderToReceipt = it },
                        onViewCreditReceipt = { creditRequestToReceipt = it },
                        onDeleteTx = { id, coll -> deleteOrderById(id, coll, db, showModernToast) }
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
                    } else { expandedImageUrl }
                    AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageData).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxWidth(0.95f), contentScale = ContentScale.Fit, error = painterResource(R.drawable.logo_admin))
                }
            }
        }

        if (showAddProductDialog || productToEdit != null) {
            AddProductDialog(editingProduct = productToEdit, bcvRate = bcvRate, onDismiss = { showAddProductDialog = false; productToEdit = null }, onConfirm = { product, uri ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val base64 = uri?.let { u -> context.contentResolver.openInputStream(u)?.use { it.readBytes() }?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } }
                        val editing = productToEdit
                        val oldCategory = editing?.category?.let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").uppercase() }
                        val oldName = editing?.name
                        val normalizedCat = java.text.Normalizer.normalize(product.category, java.text.Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").uppercase()
                        val json = org.json.JSONObject().apply {
                            put("action", "addProduct"); put("sheetName", normalizedCat)
                            if (editing != null) { put("oldCategory", oldCategory); put("oldName", oldName) }
                            put("photoBase64", base64 ?: ""); put("fileName", "PROD_${System.currentTimeMillis()}.jpg"); put("folderName", "PRODUCTOS_STARBIG")
                            put("data", org.json.JSONObject().apply {
                                put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                                put("nombre", product.name); put("precio", product.priceUsd); put("stock", product.stock); put("coleccion", product.collection); put("descripcion", product.description); put("credito", if(product.allowCredit) "SÍ" else "NO"); put("imagen", if(base64 == null) product.imageUrl else "")
                            })
                        }
                        val response = OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
                        val driveUrl = response.body?.string()?.let { if(it.startsWith("{")) org.json.JSONObject(it).optString("imageUrl") else "" } ?: ""
                        val finalProductMap = hashMapOf("name" to product.name, "priceUsd" to product.priceUsd, "stock" to product.stock, "category" to product.category, "collection" to product.collection, "description" to product.description, "allowCredit" to product.allowCredit, "imageUrl" to if (driveUrl.isNotEmpty()) driveUrl else product.imageUrl, "timestamp" to System.currentTimeMillis())
                        if (editing != null) db.collection("productos").document(editing.id).update(finalProductMap as Map<String, Any>).await()
                        else db.collection("productos").add(finalProductMap).await()
                        CoroutineScope(Dispatchers.Main).launch { isLoading = false; showAddProductDialog = false; productToEdit = null; showModernToast("✅ PRODUCTO GUARDADO", false) }
                    } catch (e: Exception) { CoroutineScope(Dispatchers.Main).launch { isLoading = false; showModernToast("❌ ERROR AL GUARDAR", true) } }
                }
            })
        }

        if (productToDelete != null) {
            AlertDialog(onDismissRequest = { productToDelete = null }, containerColor = Color(0xFF121216), title = { Text("¿ELIMINAR PRODUCTO?", color = Color.White, fontWeight = FontWeight.Black) }, text = { Text("Esta acción eliminará ${productToDelete?.name?.uppercase()} permanentemente.", color = Color.White.copy(0.7f)) }, confirmButton = { Button(onClick = { val p = productToDelete!!; productToDelete = null; deleteProduct(p, db); showModernToast("✅ PRODUCTO ELIMINADO", false) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR", color = Color.White) } }, dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) } })
        }

        if (showAddNewsDialog) {
            AddNewsDialog(onDismiss = { showAddNewsDialog = false }, onConfirm = { uri ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val base64 = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: throw Exception("Error al procesar imagen")
                        val json = org.json.JSONObject().apply { put("action", "addProduct"); put("sheetName", "NOVEDADES"); put("photoBase64", base64); put("fileName", "NEWS_${System.currentTimeMillis()}.jpg"); put("folderName", "NOVEDADES_STARBIG"); put("data", org.json.JSONObject().apply { put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())); put("nombre", "FLYER NOVEDAD"); put("precio", 0) }) }
                        val response = OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
                        val driveUrl = response.body?.string()?.let { org.json.JSONObject(it).optString("imageUrl") } ?: throw Exception("Link de Drive vacío")
                        db.collection("novedades").add(News(imageUrl = driveUrl, timestamp = Date().time)).await()
                        CoroutineScope(Dispatchers.Main).launch { isLoading = false; showAddNewsDialog = false; showModernToast("✅ NOVEDAD PUBLICADA", false) }
                    } catch (e: Exception) { CoroutineScope(Dispatchers.Main).launch { isLoading = false; showModernToast("❌ ERROR AL PUBLICAR", true) } }
                }
            })
        }

        if (customerToDelete != null) {
            AlertDialog(onDismissRequest = { customerToDelete = null }, containerColor = Color(0xFF121216), title = { Text("¿ELIMINAR USUARIO?", color = Color.White, fontWeight = FontWeight.Black) }, text = { Text("Esta acción eliminará a ${customerToDelete?.name?.uppercase()} de todas las bases de datos.", color = Color.White.copy(0.7f)) }, confirmButton = { Button(onClick = { val c = customerToDelete!!; customerToDelete = null; deleteCustomer(c, db); showModernToast("✅ USUARIO ELIMINADO", false) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR", color = Color.White) } }, dismissButton = { TextButton(onClick = { customerToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) } })
        }

        if (newsToDelete != null) {
            AlertDialog(onDismissRequest = { newsToDelete = null }, containerColor = Color(0xFF121216), title = { Text("¿ELIMINAR NOVEDAD?", color = Color.White, fontWeight = FontWeight.Black) }, text = { Text("¿Seguro que desea eliminar este flyer promocional?", color = Color.White.copy(0.7f)) }, confirmButton = { Button(onClick = { val n = newsToDelete!!; newsToDelete = null; deleteNews(n, db, showModernToast) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR", color = Color.White) } }, dismissButton = { TextButton(onClick = { newsToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) } })
        }

        if (showRateDialog) {
            BcvRateDialog(bcvRate, { showRateDialog = false }, { db.collection("config").document("tasa_bcv").set(mapOf("valor" to it)); showRateDialog = false })
        }

        if (creditRequestToDelete != null) {
            AlertDialog(onDismissRequest = { creditRequestToDelete = null }, containerColor = Color(0xFF121216), title = { Text("¿ELIMINAR SOLICITUD?", color = Color.White, fontWeight = FontWeight.Black) }, text = { Text("Esta acción eliminará el registro permanentemente.", color = Color.White.copy(0.7f)) }, confirmButton = { Button(onClick = { val r = creditRequestToDelete!!; creditRequestToDelete = null; deleteCreditRequest(r, db, showModernToast) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR", color = Color.White) } }, dismissButton = { TextButton(onClick = { creditRequestToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) } })
        }

        if (creditRequestToReceipt != null) {
            CreditReceiptDialog(request = creditRequestToReceipt!!, onDismiss = { creditRequestToReceipt = null })
        }
        
        if (orderToReceipt != null) {
            OrderReceiptDialog(order = orderToReceipt!!, onDismiss = { orderToReceipt = null })
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)) { data ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)), shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.5f))) {
                Text(text = data.visuals.message.uppercase(), color = Color.White, modifier = Modifier.padding(16.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun AdminNavButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor by androidx.compose.animation.animateColorAsState(if (isSelected) Color(0xFFC5A059) else Color(0xFF08080A), label = "bg")
    val contentColor by androidx.compose.animation.animateColorAsState(if (isSelected) Color.Black else Color.White.copy(0.5f), label = "content")
    val borderColor by androidx.compose.animation.animateColorAsState(if (isSelected) Color.White.copy(0.4f) else Color(0xFFC5A059).copy(0.15f), label = "border")
    Box(modifier = modifier.height(65.dp).background(backgroundColor, RoundedCornerShape(10.dp)).border(1.dp, borderColor, RoundedCornerShape(10.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, fontSize = 8.sp, fontWeight = FontWeight.Black, color = contentColor, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
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
    db.collection("registros_clientes").document(reg.id).delete()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { put("action", "delete"); put("email", reg.email.trim()); put("deleteFromAuth", true); put("photoUrl", reg.photoUrl); put("idCardUrl", reg.idCardUrl) }
            OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        } catch (_: Exception) {}
    }
}

private fun deleteProduct(p: Product, db: FirebaseFirestore) {
    db.collection("productos").document(p.id).delete()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { put("action", "deleteProduct"); put("sheetName", java.text.Normalizer.normalize(p.category, java.text.Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").uppercase()); put("nombre", p.name) }
            OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        } catch (_: Exception) {}
    }
}

@Composable
fun CustomerAdminCard(reg: CustomerRegistration, onApprove: () -> Unit, onReject: () -> Unit, onImageClick: (String) -> Unit) {
    val isActive = reg.status == "active"
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if(isActive) Color.Green.copy(0.3f) else Color(0xFFC5A059).copy(0.2f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFF1A1A20), RoundedCornerShape(8.dp)).clickable { onImageClick(reg.photoUrl) }) { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(fixDriveUrl(reg.photoUrl)).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, error = painterResource(R.drawable.logo_admin)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(reg.name.uppercase(), fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                    Text("C.I: ${reg.idNumber}", color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(reg.email, color = Color.White.copy(0.4f), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusBadge(reg.status)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(Icons.Default.Place, reg.address); InfoRow(Icons.Default.Phone, reg.phone)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black, RoundedCornerShape(8.dp)).clickable { onImageClick(reg.idCardUrl) }) { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(fixDriveUrl(reg.idCardUrl)).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, error = painterResource(R.drawable.logo_admin)) }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red), shape = RoundedCornerShape(12.dp)) { Text("ELIMINAR") }
                if(!isActive) Button(onClick = onApprove, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(12.dp)) { Text("APROBAR", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun InventorySection(products: List<Product>, bcv: Double, onAdd: () -> Unit, onRate: () -> Unit, onSync: () -> Unit, onDelete: (Product) -> Unit, onEdit: (Product) -> Unit) {
    var selectedCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos") + products.map { it.category }.distinct().sorted()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A1A20), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("TASA BCV OFICIAL", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("$bcv BSS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = onSync, modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(8.dp))) { Icon(Icons.Default.Sync, null, tint = Color(0xFFC5A059), modifier = Modifier.size(20.dp)) }
                }
                Button(onClick = onRate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(8.dp)) { Text("AJUSTAR", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        ScrollableTabRow(selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0), containerColor = Color.Transparent, contentColor = Color(0xFFC5A059), edgePadding = 0.dp, divider = {}, indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(it[categories.indexOf(selectedCategory).coerceAtLeast(0)]), color = Color(0xFFC5A059), height = 3.dp) }) {
            categories.forEach { cat -> Tab(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, text = { Text(cat.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold) }) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("STOCK", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black); Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059).copy(0.1f))) { Text("+ NUEVO", color = Color(0xFFC5A059)) } }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(if(selectedCategory == "Todos") products else products.filter { it.category == selectedCategory }) { ProductAdminItem(it, bcv, onDelete, onEdit) } }
    }
}

@Composable
fun ProductAdminItem(p: Product, bcv: Double, onDelete: (Product) -> Unit, onEdit: (Product) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = fixDriveUrl(p.imageUrl), contentDescription = null, modifier = Modifier.size(56.dp).background(Color.Black, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(p.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black); Text("$${p.priceUsd} | ${(p.priceUsd * bcv).format(2)} BSS", color = Color(0xFFC5A059)); Text("STOCK: ${p.stock}", color = if(p.stock < 5) Color.Red else Color.Green, fontSize = 10.sp) }
            IconButton(onClick = { onEdit(p) }) { Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.4f)) }
            IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.4f)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    editingProduct: Product? = null,
    bcvRate: Double = 36.5,
    onDismiss: () -> Unit,
    onConfirm: (Product, Uri?) -> Unit
) {
    var n by remember { mutableStateOf(editingProduct?.name ?: "") }
    var p by remember { mutableStateOf(editingProduct?.priceUsd?.toString() ?: "") }
    var s by remember { mutableStateOf(editingProduct?.stock?.toString() ?: "") }
    var d by remember { mutableStateOf(editingProduct?.description ?: "") }
    var c by remember { mutableStateOf(editingProduct?.allowCredit ?: false) }
    var cat by remember { mutableStateOf(editingProduct?.category ?: "Perfumes") }
    var col by remember { mutableStateOf(editingProduct?.collection ?: "Nueva Temporada") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri = it }

    var isCatExpanded by remember { mutableStateOf(false) }
    val categoryOptions = listOf("Perfumes", "Tecnología", "Ropa", "Calzado", "Belleza", "Otros")

    val priceUsdVal = p.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121216),
        title = { Text(if (editingProduct != null) "EDITAR PRODUCTO" else "NUEVO PRODUCTO", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFC5A059).copy(0.3f), RoundedCornerShape(8.dp))
                        .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (uri != null || (editingProduct?.imageUrl?.isNotEmpty() == true)) {
                        AsyncImage(model = uri ?: fixDriveUrl(editingProduct?.imageUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    } else {
                        Text("+ AÑADIR FOTO", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AdminTextField(n, { n = it }, "Nombre del Producto")
                AdminTextField(p, { p = it }, "Precio en Dólares (USD)")

                // Real-time BSS Price Preview Badge
                if (priceUsdVal > 0.0) {
                    val bssContado = priceUsdVal * bcvRate
                    val bssCredito = priceUsdVal * bcvRate * 1.10
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CÁLCULO EN BOLÍVARES (TASA BCV: $bcvRate BSS)", color = Color(0xFFC5A059), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text("Contado: ${bssContado.format(2)} BSS", color = Color(0xFF25D366), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Crédito (+10%): ${bssCredito.format(2)} BSS", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AdminTextField(s, { s = it }, "Cantidad en Stock")

                // Category Selector with Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = cat,
                        onValueChange = { cat = it },
                        label = { Text("Categoría del Producto", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { isCatExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar Categoría", tint = Color(0xFFC5A059))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC5A059),
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1A1A20),
                            unfocusedContainerColor = Color(0xFF1A1A20)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = isCatExpanded,
                        onDismissRequest = { isCatExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A1A20))
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    cat = option
                                    isCatExpanded = false
                                }
                            )
                        }
                    }
                }

                AdminTextField(d, { d = it }, "Descripción")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = c,
                        onCheckedChange = { c = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC5A059))
                    )
                    Text("APTO PARA CRÉDITO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Product(
                            id = editingProduct?.id ?: "",
                            name = n,
                            priceUsd = priceUsdVal,
                            description = d,
                            category = cat,
                            collection = col,
                            stock = s.toIntOrNull() ?: 0,
                            allowCredit = c,
                            imageUrl = editingProduct?.imageUrl ?: ""
                        ),
                        uri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
            ) {
                Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color.White.copy(0.3f))) {
                Text("CANCELAR", color = Color.White)
            }
        }
    )
}

@Composable
fun PaymentSettingsSection(curr: Map<String, String>, onS: (Map<String, String>) -> Unit) {
    var z by remember(curr) { mutableStateOf(curr["zelle"] ?: "") }; var b by remember(curr) { mutableStateOf(curr["binance"] ?: "") }; var zi by remember(curr) { mutableStateOf(curr["zinli"] ?: "") }; var pm by remember(curr) { mutableStateOf(curr["pagomovil"] ?: "") }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PAGOS Y CONFIGURACIÓN", color = Color.White, fontWeight = FontWeight.Black)
        AdminLargeTextField(z, { z = it }, "ZELLE"); AdminLargeTextField(b, { b = it }, "BINANCE"); AdminLargeTextField(zi, { zi = it }, "ZINLI"); AdminLargeTextField(pm, { pm = it }, "PAGO MÓVIL")
        Button(onClick = { onS(mapOf("zelle" to z, "binance" to b, "zinli" to zi, "pagomovil" to pm)) }, Modifier.fillMaxWidth()) { Text("GUARDAR METODOS DE PAGO") }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val db = FirebaseFirestore.getInstance()
                val updateData = mapOf(
                    "version" to "1.0.${System.currentTimeMillis()}",
                    "message" to "✨ ¡Catálogo, precios y ofertas actualizados en tiempo real!",
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("config").document("app_version").set(updateData)
                    .addOnSuccessListener {
                        android.widget.Toast.makeText(context, "✨ Notificación enviada a todos los usuarios", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        android.widget.Toast.makeText(context, "❌ Error al notificar", android.widget.Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("✨ NOTIFICAR ACTUALIZACIÓN EN TIEMPO REAL", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}

@Composable
fun AdminLargeTextField(v: String, onV: (String) -> Unit, l: String) {
    OutlinedTextField(value = v, onValueChange = onV, label = { Text(l, fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth().height(100.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
}

@Composable
fun CollectionsSection(orders: List<Order>, onConfirmPayment: (Order) -> Unit, onRejectPayment: (Order) -> Unit, onDeleteSale: (Order) -> Unit, onClearHistory: () -> Unit, onViewReceipt: (Order) -> Unit, onImageClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("HISTORIAL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black); TextButton(onClick = onClearHistory) { Text("LIMPIAR", color = Color.Red) } }
        if (orders.isEmpty()) InfoSection("Vacío") else LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { items(orders) { OrderAdminCard(it, onConfirmPayment, onRejectPayment, onDeleteSale, onViewReceipt, onImageClick) } }
    }
}

@Composable
fun OrderAdminCard(
    order: Order,
    onConfirm: (Order) -> Unit,
    onReject: (Order) -> Unit,
    onDelete: (Order) -> Unit,
    onViewReceipt: (Order) -> Unit,
    onImageClick: (String) -> Unit
) {
    val date = if (order.timestamp > 0) java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp)) else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: ID Orden, Fecha, Status, Eliminar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(order.orderId, color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    if (date.isNotEmpty()) Text(date, color = Color.White.copy(0.4f), fontSize = 10.sp)
                    StatusBadge(order.status)
                }
                IconButton(onClick = { onDelete(order) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(0.5f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Datos del Cliente
            Text(order.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (order.idNumber.isNotEmpty()) Text("C.I: ${order.idNumber}", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (order.phone.isNotEmpty()) Text("TLF: ${order.phone}", color = Color.White.copy(0.7f), fontSize = 11.sp)
            }
            if (order.customerEmail.isNotEmpty()) {
                Text("CORREO: ${order.customerEmail}", color = Color.White.copy(0.4f), fontSize = 10.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Productos de la Orden
            Text("PRODUCTOS DE LA VENTA:", color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                order.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("• ${item.buyQty}x ${item.name}", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("$${item.priceUsd} (${item.paymentMethod.uppercase()})", color = Color(0xFFC5A059), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Detalle Reporte de Pago
            order.paymentReport?.let { report ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFC5A059).copy(0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("DETALLES DEL REPORTE DE PAGO", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text("NRO. REFERENCIA: ${report.reference.ifEmpty { "S/R" }}", color = Color.Yellow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("BANCO ORIGEN: ${report.bank.ifEmpty { "S/B" }}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("MONTO REPORTADO: ${report.amount} BSS", color = Color(0xFF25D366), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        if (report.phone.isNotEmpty()) Text("TLF PAGO MÓVIL: ${report.phone}", color = Color.White.copy(0.8f), fontSize = 10.sp)
                        if (report.date.isNotEmpty()) Text("FECHA REPORTE: ${report.date}", color = Color.White.copy(0.6f), fontSize = 10.sp)

                        report.captureBase64?.let { base64 ->
                            if (base64.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("COMPROBANTE CAPTURA:", color = Color.White.copy(0.5f), fontSize = 9.sp)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                        .clickable { onImageClick("data:image/jpeg;base64,$base64") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = try { android.util.Base64.decode(base64, android.util.Base64.DEFAULT) } catch(_: Exception) { null },
                                        contentDescription = "Comprobante",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Text("PAGO AÚN NO REPORTADO POR EL CLIENTE", color = Color.White.copy(0.4f), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Totales y Botones de Acción
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("TOTAL ORDEN:", color = Color.White.copy(0.5f), fontSize = 9.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.totalUsd, color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("(${order.totalBss})", color = Color.White.copy(0.8f), fontSize = 12.sp)
                    }
                }

                when (order.status) {
                    "pending", "awaiting_verification" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onConfirm(order) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("APROBAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onReject(order) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("RECHAZAR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                    "paid", "active_credit" -> {
                        Button(
                            onClick = { onViewReceipt(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color(0xFFC5A059)),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = Color(0xFFC5A059), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("VER RECIBO", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> StatusBadge(order.status)
                }
            }
        }
    }
}

@Composable
fun PaymentDetailItem(label: String, value: String) { Row { Text("$label: ", color = Color.White.copy(0.4f), fontSize = 10.sp); Text(value, color = Color.White, fontSize = 10.sp) } }

private suspend fun subtractProductStock(db: FirebaseFirestore, items: List<OrderItem>) {
    try {
        items.forEach { item ->
            val prodsSnap = db.collection("productos").whereEqualTo("name", item.name).get().await()
            if (!prodsSnap.isEmpty) {
                val doc = prodsSnap.documents[0]
                val currentStock = (doc.getLong("stock") ?: 0).toInt()
                val newStock = maxOf(0, currentStock - item.buyQty)
                doc.reference.update("stock", newStock).await()
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AdminScreen", "Error actualizando stock: ${e.message}")
    }
}

private fun confirmOrderPayment(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val itemRef = db.collection(order.collection).document(order.id)
            val itemDoc = itemRef.get().await()

            val reportMap = itemDoc.get("paymentReport") as? Map<*, *>
            val rawAmountStr = reportMap?.get("amount")?.toString()
                ?: order.paymentReport?.amount
                ?: ""
            val cleanAmountStr = rawAmountStr.replace(" BSS", "").replace("BSS", "").trim()
            val amountPaid = cleanAmountStr.toDoubleOrNull() ?: 0.0

            if (order.collection == "pedidos") {
                val hasCredit = order.items.any { it.paymentMethod == "credit" }
                        || (itemDoc.getBoolean("hasCredit") ?: false)
                        || (itemDoc.get("items") as? List<*>)?.any {
                            (it as? Map<*, *>)?.get("paymentMethod") == "credit"
                        } == true

                val docRemaining = itemDoc.getDouble("remainingDebt")

                if (hasCredit) {
                    val totalBssStr = itemDoc.getString("totalBss") ?: order.totalBss
                    val totalBss = totalBssStr.replace(" BSS", "").replace("BSS", "").trim().toDoubleOrNull() ?: 0.0
                    val initial = totalBss * 0.25

                    if (docRemaining == null) {
                        // Confirmación del pago inicial del 25% al crear el pedido a crédito
                        val debt = maxOf(0.0, totalBss - initial)
                        itemRef.update(
                            mapOf(
                                "status" to if (debt <= 0.0) "paid" else "active_credit",
                                "remainingDebt" to debt,
                                "installmentsPaid" to 0,
                                "paymentReport" to null
                            )
                        ).await()
                    } else {
                        // Confirmación de pago de cuota o abono posterior
                        val newRemaining = maxOf(0.0, docRemaining - amountPaid)
                        val currentInstallments = (itemDoc.getLong("installmentsPaid") ?: 0).toInt()
                        val newInstallments = currentInstallments + 1

                        itemRef.update(
                            mapOf(
                                "remainingDebt" to newRemaining,
                                "installmentsPaid" to newInstallments,
                                "status" to if (newRemaining <= 0.0) "paid" else "active_credit",
                                "paymentReport" to null
                            )
                        ).await()
                    }
                } else {
                    itemRef.update(
                        mapOf(
                            "status" to "paid",
                            "paymentReport" to null
                        )
                    ).await()
                }
                showToast("✅ PAGO CONFIRMADO Y STOCK ACTUALIZADO", false)
            } else {
                // Para solicitudes_credito (créditos personales)
                val amountBssStr = itemDoc.getString("amountBss") ?: itemDoc.getDouble("amountBss")?.toString() ?: "0"
                var totalBss = amountBssStr.replace(" BSS", "").replace("BSS", "").trim().toDoubleOrNull() ?: 0.0
                val plan = itemDoc.getString("plan") ?: ""
                if (plan == "weekly_4") totalBss *= 1.1
                else if (plan == "full_30_days") totalBss *= 1.3

                val docRemaining = itemDoc.getDouble("remainingDebt") ?: totalBss
                val newRemaining = maxOf(0.0, docRemaining - amountPaid)
                val currentInstallments = (itemDoc.getLong("installmentsPaid") ?: 0).toInt()
                val newInstallments = currentInstallments + 1

                itemRef.update(
                    mapOf(
                        "status" to if (newRemaining <= 0.0) "paid" else "approved",
                        "remainingDebt" to newRemaining,
                        "installmentsPaid" to newInstallments,
                        "paymentReport" to null
                    )
                ).await()
            }

            // Asignar puntos al cliente
            val userSnap = db.collection("registros_clientes").whereEqualTo("email", order.customerEmail).get().await()
            if (!userSnap.isEmpty) {
                val userDoc = userSnap.documents[0]
                val currentPoints = userDoc.getLong("points") ?: 0
                userDoc.reference.update("points", currentPoints + 10).await()
            }

            // Notificación por correo al cliente
            notifyEmail(mapOf(
                "action" to "confirmPaymentNotification",
                "customerEmail" to order.customerEmail,
                "customerName" to order.customerName,
                "orderId" to order.orderId,
                "amount" to amountPaid,
                "status" to "APROBADO"
            ))

            CoroutineScope(Dispatchers.Main).launch { showToast("✅ PAGO CONFIRMADO - CORREO ENVIADO", false) }
        } catch (e: Exception) {
            android.util.Log.e("AdminScreen", "Error al confirmar pago", e)
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ ERROR AL CONFIRMAR PAGO", true) }
        }
    }
}

private fun rejectOrderPayment(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val itemRef = db.collection(order.collection).document(order.id)
            val itemDoc = itemRef.get().await()
            val hasCredit = order.items.any { it.paymentMethod == "credit" } || (itemDoc.getBoolean("hasCredit") ?: false)
            val newStatus = if (order.collection == "pedidos") {
                if (hasCredit) "active_credit" else "rejected"
            } else {
                "approved"
            }
            itemRef.update(mapOf("status" to newStatus, "paymentReport" to null)).await()

            // Notificación por correo al cliente
            notifyEmail(mapOf(
                "action" to "rejectPaymentNotification",
                "customerEmail" to order.customerEmail,
                "customerName" to order.customerName,
                "orderId" to order.orderId,
                "status" to "RECHAZADO"
            ))

            CoroutineScope(Dispatchers.Main).launch { showToast("❌ RECHAZADO - CORREO ENVIADO AL CLIENTE", true) }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ ERROR AL RECHAZAR", true) }
        }
    }
}

private fun deleteOrder(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection(order.collection).document(order.id).update("archivedFromCobros", true)
        .addOnSuccessListener { showToast("✅ Oculto de Cobros (Conservado en Resumen)", false) }
        .addOnFailureListener { showToast("❌ Error al ocultar", true) }
}

private fun deleteOrderById(id: String, collection: String, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection(collection).document(id).delete()
        .addOnSuccessListener { showToast("✅ REGISTRO ELIMINADO", false) }
        .addOnFailureListener { showToast("❌ ERROR AL ELIMINAR", true) }
}

private fun clearCompletedOrders(db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("pedidos").whereIn("status", listOf("paid", "rejected")).get().addOnSuccessListener { snap ->
        val batch = db.batch()
        snap.documents.forEach { batch.update(it.reference, "archivedFromCobros", true) }
        batch.commit().addOnSuccessListener { showToast("✅ COBROS LIMPIOS (Conservado en Resumen)", false) }
    }
}

@Composable
fun StatusBadge(s: String) { Box(modifier = Modifier.background(if (s == "active") Color.Green.copy(0.1f) else Color.Yellow.copy(0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(s.uppercase(), color = if (s == "active") Color.Green else Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun InfoRow(i: androidx.compose.ui.graphics.vector.ImageVector, t: String) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(i, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(0.3f)); Spacer(Modifier.width(8.dp)); Text(t.uppercase(), color = Color.White.copy(0.6f), fontSize = 10.sp) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dropdownColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC5A059), unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)

@Composable
fun AdminTextField(v: String, onV: (String) -> Unit, l: String) { OutlinedTextField(value = v, onValueChange = onV, label = { Text(l, fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFC5A059), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF1A1A20), unfocusedContainerColor = Color(0xFF1A1A20)), shape = RoundedCornerShape(12.dp), singleLine = true) }

@Composable
fun BcvRateDialog(curr: Double, onD: () -> Unit, onC: (Double) -> Unit) { var r by remember { mutableStateOf(curr.toString()) }; AlertDialog(onDismissRequest = onD, containerColor = Color(0xFF121216), title = { Text("TASA BCV", color = Color.White) }, text = { AdminTextField(r, { r = it }, "BSS") }, confirmButton = { Button(onClick = { onC(r.toDoubleOrNull() ?: curr) }) { Text("OK") } }) }

fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
fun NewsAdminSection(newsList: List<News>, onAddNews: () -> Unit, onDeleteNews: (News) -> Unit, onImageClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("NOVEDADES", color = Color.White, fontWeight = FontWeight.Black); IconButton(onClick = onAddNews) { Icon(Icons.Default.Add, null, tint = Color(0xFFC5A059)) } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) { items(newsList) { NewsAdminCard(it, onDeleteNews, onImageClick) } }
    }
}

@Composable
fun NewsAdminCard(news: News, onDelete: (News) -> Unit, onImageClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.2f))) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(220.dp).background(Color.Black).clickable { onImageClick(news.imageUrl) }) { AsyncImage(model = fixDriveUrl(news.imageUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            IconButton(onClick = { onDelete(news) }, Modifier.align(Alignment.End)) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun AddNewsDialog(onDismiss: () -> Unit, onConfirm: (Uri) -> Unit) {
    var uri by remember { mutableStateOf<Uri?>(null) }; val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri = it }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF121216), title = { Text("NUEVA NOVEDAD", color = Color.White) }, text = { Box(Modifier.fillMaxWidth().height(200.dp).background(Color.Black).clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Alignment.Center) { if(uri != null) AsyncImage(uri, null, Modifier.fillMaxSize()) else Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFC5A059)) } }, confirmButton = { Button(onClick = { uri?.let { onConfirm(it) } }, enabled = uri != null) { Text("PUBLICAR") } })
}

data class SummaryTx(
    val id: String = "",
    val refId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val date: String = "",
    val timestamp: Long = 0,
    val type: String = "",
    val description: String = "",
    val totalUsd: Double = 0.0,
    val totalBss: String = "",
    val status: String = "",
    val remainingDebt: Double = 0.0,
    val referenceCode: String = "",
    val rawOrder: Order? = null,
    val rawCreditRequest: CreditRequest? = null
)

@Composable
fun SalesSummaryAndSearchSection(
    orders: List<Order>,
    creditRequests: List<CreditRequest>,
    registrations: List<CustomerRegistration>,
    onViewOrderReceipt: (Order) -> Unit,
    onViewCreditReceipt: (CreditRequest) -> Unit,
    onDeleteTx: (String, String) -> Unit
) {
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }

    val allTxs = remember(orders, creditRequests, registrations) {
        val list = mutableListOf<SummaryTx>()

        orders.forEach { ord ->
            val matchingReg = registrations.firstOrNull { reg ->
                (reg.email.isNotEmpty() && reg.email.equals(ord.customerEmail, ignoreCase = true)) ||
                        (reg.idNumber.isNotEmpty() && reg.idNumber == ord.idNumber)
            }
            val idNum = ord.idNumber.ifEmpty { matchingReg?.idNumber ?: "" }
            val phone = ord.phone.ifEmpty { matchingReg?.phone ?: "" }
            val usdVal = ord.totalUsd.replace("$", "").trim().toDoubleOrNull() ?: 0.0
            val refCode = ord.paymentReport?.reference ?: ""
            val remDebt = ord.remainingDebt ?: if (ord.hasCredit) usdVal * 0.75 else 0.0

            val itemsSummary = ord.items.joinToString(", ") { "${it.buyQty}x ${it.name}" }

            list.add(
                SummaryTx(
                    id = ord.id,
                    refId = ord.orderId.ifEmpty { ord.id.take(8) },
                    customerName = ord.customerName.ifEmpty { matchingReg?.name ?: "S/N" },
                    customerEmail = ord.customerEmail,
                    idNumber = idNum,
                    phone = phone,
                    date = if (ord.timestamp > 0) sdf.format(java.util.Date(ord.timestamp)) else "N/A",
                    timestamp = ord.timestamp,
                    type = if (ord.hasCredit) "COMPRA A CRÉDITO" else "COMPRA AL CONTADO",
                    description = itemsSummary.ifEmpty { "Pedido de productos" },
                    totalUsd = usdVal,
                    totalBss = ord.totalBss,
                    status = ord.status,
                    remainingDebt = remDebt,
                    referenceCode = refCode,
                    rawOrder = ord
                )
            )
        }

        creditRequests.forEach { req ->
            val matchingReg = registrations.firstOrNull { reg ->
                (reg.email.isNotEmpty() && reg.email.equals(req.customerEmail, ignoreCase = true)) ||
                        (reg.idNumber.isNotEmpty() && reg.idNumber == req.idNumber)
            }
            val idNum = req.idNumber.ifEmpty { matchingReg?.idNumber ?: "" }
            val phone = req.phone.ifEmpty { matchingReg?.phone ?: "" }
            val refCode = req.paymentReport?.reference ?: ""
            val remDebt = req.remainingDebt ?: (req.amountUsd)

            list.add(
                SummaryTx(
                    id = req.id,
                    refId = req.id.take(12).uppercase(),
                    customerName = req.customerName.ifEmpty { matchingReg?.name ?: "S/N" },
                    customerEmail = req.customerEmail,
                    idNumber = idNum,
                    phone = phone,
                    date = if (req.timestamp > 0) sdf.format(java.util.Date(req.timestamp)) else "N/A",
                    timestamp = req.timestamp,
                    type = "CRÉDITO PERSONAL",
                    description = "Plan: ${req.plan.ifEmpty { "N/A" }} - ${req.reason.ifEmpty { "Sin motivo" }}",
                    totalUsd = req.amountUsd,
                    totalBss = req.amountBss,
                    status = req.status,
                    remainingDebt = remDebt,
                    referenceCode = refCode,
                    rawCreditRequest = req
                )
            )
        }

        list.sortedByDescending { it.timestamp }
    }

    var searchInputText by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var activePeriod by remember { mutableStateOf("all") }
    var showClosureDialog by remember { mutableStateOf(false) }

    val filteredTxs = remember(allTxs, activeSearchQuery, activePeriod) {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
        val startOfYear = cal.timeInMillis

        val startOfWeek = now - (7 * 24 * 60 * 60 * 1000L)

        val txsByPeriod = when(activePeriod) {
            "today" -> allTxs.filter { it.timestamp >= startOfToday }
            "week" -> allTxs.filter { it.timestamp >= startOfWeek }
            "month" -> allTxs.filter { it.timestamp >= startOfMonth }
            "year" -> allTxs.filter { it.timestamp >= startOfYear }
            else -> allTxs
        }

        val q = activeSearchQuery.trim().lowercase()
        if (q.isBlank()) txsByPeriod
        else txsByPeriod.filter { tx ->
            tx.idNumber.lowercase().contains(q) ||
                    tx.refId.lowercase().contains(q) ||
                    tx.referenceCode.lowercase().contains(q) ||
                    tx.customerName.lowercase().contains(q) ||
                    tx.customerEmail.lowercase().contains(q) ||
                    tx.phone.lowercase().contains(q) ||
                    tx.id.lowercase().contains(q)
        }
    }

    val groupedByCustomer = remember(filteredTxs) {
        filteredTxs.groupBy { tx ->
            val key = tx.customerEmail.ifEmpty { tx.idNumber.ifEmpty { tx.customerName } }
            key.ifEmpty { "ANÓNIMO" }
        }
    }

    val totalUsdSum = filteredTxs.sumOf { it.totalUsd }
    val totalDebtSum = filteredTxs.sumOf { it.remainingDebt }

    if (showClosureDialog) {
        AccountingClosureDialog(
            periodName = when(activePeriod) {
                "today" -> "Diario (Hoy)"
                "week" -> "Semanal (7 Días)"
                "month" -> "Mensual"
                "year" -> "Anual"
                else -> "General Todo"
            },
            txs = filteredTxs,
            totalUsd = totalUsdSum,
            totalDebt = totalDebtSum,
            onDismiss = { showClosureDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("RESUMEN DE VENTAS Y BÚSQUEDA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInputText,
                onValueChange = {
                    searchInputText = it
                    activeSearchQuery = it
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cédula, Ref, Nombre, Correo...", fontSize = 11.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFFC5A059)) },
                trailingIcon = {
                    if (searchInputText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInputText = ""
                            activeSearchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFC5A059),
                    unfocusedBorderColor = Color.White.copy(0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF121216),
                    unfocusedContainerColor = Color(0xFF121216)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = { activeSearchQuery = searchInputText },
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("BUSCAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // BARRA DE CIERRE CONTABLE
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121216),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CIERRE CONTABLE Y FILTROS DE PERÍODO", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val periods = listOf("today" to "HOY (DIARIO)", "week" to "7 DÍAS (SEMANAL)", "month" to "ESTE MES", "year" to "ESTE AÑO", "all" to "TODOS")
                    periods.forEach { (pKey, pLabel) ->
                        FilterChip(
                            selected = activePeriod == pKey,
                            onClick = { activePeriod = pKey },
                            label = { Text(pLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC5A059),
                                selectedLabelColor = Color.Black,
                                containerColor = Color.Black.copy(0.3f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showClosureDialog = true },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("REPORTE CIERRE CONTABLE (TAMAÑO CARTA)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121216),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REGISTROS", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${filteredTxs.size} Transacciones", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL VENTAS", color = Color(0xFFC5A059), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("$${totalUsdSum.format(2)}", color = Color(0xFFC5A059), fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DEUDA PENDIENTE", color = Color.Red.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("$${totalDebtSum.format(2)}", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (groupedByCustomer.isEmpty()) {
            InfoSection("No se encontraron registros de ventas o clientes")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(groupedByCustomer.entries.toList()) { entry ->
                    val txList = entry.value
                    val firstTx = txList.first()
                    val custTotalUsd = txList.sumOf { it.totalUsd }
                    val custDebtUsd = txList.sumOf { it.remainingDebt }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(firstTx.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                    Text("C.I: ${firstTx.idNumber.ifEmpty { "S/C" }} | TLF: ${firstTx.phone.ifEmpty { "S/T" }}", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (firstTx.customerEmail.isNotEmpty()) {
                                        Text(firstTx.customerEmail, color = Color.White.copy(0.4f), fontSize = 10.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TOTAL: $${custTotalUsd.format(2)}", color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    if (custDebtUsd > 0) {
                                        Text("DEUDA: $${custDebtUsd.format(2)}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    } else {
                                        Text("AL DÍA", color = Color(0xFF25D366), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))
                            Spacer(Modifier.height(10.dp))

                            Text("HISTORIAL DE TRANSACCIONES (${txList.size}):", color = Color.White.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                txList.forEach { tx ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.Black.copy(0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(tx.refId, color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                    StatusBadge(tx.status)
                                                }
                                                Text("${tx.type} • ${tx.date}", color = Color.White.copy(0.5f), fontSize = 9.sp)
                                                Text(tx.description, color = Color.White.copy(0.8f), fontSize = 10.sp, maxLines = 2)
                                                if (tx.referenceCode.isNotEmpty()) {
                                                    Text("Ref Pago: ${tx.referenceCode}", color = Color.Yellow.copy(0.8f), fontSize = 9.sp)
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("$${tx.totalUsd.format(2)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                                Text(tx.totalBss, color = Color(0xFFC5A059), fontSize = 10.sp)
                                                Spacer(Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { onDeleteTx(tx.id, if (tx.rawOrder != null) tx.rawOrder.collection else "solicitudes_credito") },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar de Resumen", tint = Color.Red.copy(0.6f), modifier = Modifier.size(16.dp))
                                                    }
                                                    Button(
                                                        onClick = {
                                                            if (tx.rawOrder != null) onViewOrderReceipt(tx.rawOrder)
                                                            else if (tx.rawCreditRequest != null) onViewCreditReceipt(tx.rawCreditRequest)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                        border = BorderStroke(1.dp, Color(0xFFC5A059))
                                                    ) {
                                                        Text("RECIBO", color = Color(0xFFC5A059), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditRequestsSection(
    requests: List<CreditRequest>,
    registrations: List<CustomerRegistration> = emptyList(),
    onApprove: (CreditRequest, String) -> Unit,
    onDeny: (CreditRequest, String) -> Unit,
    onDelete: (CreditRequest) -> Unit,
    onViewReceipt: ((CreditRequest) -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("SOLICITUDES DE CRÉDITO", color = Color.White, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        if (requests.isEmpty()) {
            InfoSection("No hay solicitudes de crédito")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(requests) { req ->
                    val matchingReg = registrations.firstOrNull { reg ->
                        (reg.email.isNotEmpty() && reg.email.equals(req.customerEmail, ignoreCase = true)) ||
                                (reg.idNumber.isNotEmpty() && reg.idNumber == req.idNumber)
                    }
                    CreditRequestCard(
                        req = req,
                        userRegistration = matchingReg,
                        onApprove = onApprove,
                        onDeny = onDeny,
                        onDelete = onDelete,
                        onViewReceipt = onViewReceipt,
                        onImageClick = onImageClick
                    )
                }
            }
        }
    }
}

@Composable
fun CreditRequestCard(
    req: CreditRequest,
    userRegistration: CustomerRegistration? = null,
    onApprove: (CreditRequest, String) -> Unit,
    onDeny: (CreditRequest, String) -> Unit,
    onDelete: (CreditRequest) -> Unit,
    onViewReceipt: ((CreditRequest) -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null
) {
    var comm by remember { mutableStateOf("") }
    val date = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.timestamp))

    val planText = when(req.plan) {
        "weekly_4" -> "4 Pagos Semanales (Monto + 10%)"
        "weekly_interest" -> "Interés Semanal (10%)"
        "full_30_days" -> "Pago Único a 30 Días (Monto + 30%)"
        else -> if (req.plan.isNotEmpty()) req.plan.uppercase() else "No especificado"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header: Fecha, Status Badge, Botón Eliminar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(date, color = Color.White.copy(0.4f), fontSize = 10.sp)
                    StatusBadge(req.status)
                }
                IconButton(onClick = { onDelete(req) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red.copy(0.5f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sección Cliente
            Text("DATOS DEL SOLICITANTE", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(req.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            InfoRow(Icons.Default.Person, "C.I: ${req.idNumber.ifEmpty { "No registrada" }}")
            InfoRow(Icons.Default.Phone, "TLF: ${req.phone.ifEmpty { "No registrado" }}")
            InfoRow(Icons.Default.Email, "CORREO: ${req.customerEmail.ifEmpty { "No registrado" }}")

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(12.dp))

            // Sección Crédito Solicitado
            Text("DETALLES DEL CRÉDITO", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$${req.amountUsd}", color = Color(0xFFC5A059), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text("(${req.amountBss} BSS)", color = Color.White.copy(0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (req.bcvAtRequest > 0.0) {
                Text("Tasa BCV al solicitar: ${req.bcvAtRequest} BSS/USD", color = Color.White.copy(0.5f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text("PLAN: $planText", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (req.dueDate.isNotEmpty()) {
                Text("FECHA LÍMITE: ${req.dueDate}", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(12.dp))

            // Datos Bancarios del Cliente para Desembolso
            Text("DATOS BANCARIOS CLIENTE (PARA TRANSFERIR)", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("BANCO: ${req.bank.ifEmpty { "No especificado" }}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("CUENTA / PAGO MÓVIL: ${req.account.ifEmpty { "No especificada" }}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(12.dp))

            // Motivo
            Text("MOTIVO DE LA SOLICITUD", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(0.3f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.05f))
            ) {
                Text(
                    text = req.reason.ifEmpty { "Sin motivo especificado" },
                    color = Color.White.copy(0.9f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Documentos / Verificación de Identidad del Registro de Usuario
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(12.dp))

            Text("VERIFICACIÓN DE IDENTIDAD DEL CLIENTE", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (userRegistration != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ESTADO REGISTRO: ", color = Color.White.copy(0.6f), fontSize = 10.sp)
                    StatusBadge(userRegistration.status)
                }
                if (userRegistration.address.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    InfoRow(Icons.Default.Place, userRegistration.address)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Foto Perfil
                    if (userRegistration.photoUrl.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FOTO PERFIL", color = Color.White.copy(0.5f), fontSize = 9.sp)
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .clickable { onImageClick?.invoke(userRegistration.photoUrl) },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(fixDriveUrl(userRegistration.photoUrl)).crossfade(true).build(),
                                    contentDescription = "Foto Perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.logo_admin)
                                )
                            }
                        }
                    }
                    // Cédula de Identidad
                    if (userRegistration.idCardUrl.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DOCUMENTO CÉDULA", color = Color.White.copy(0.5f), fontSize = 9.sp)
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .clickable { onImageClick?.invoke(userRegistration.idCardUrl) },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(fixDriveUrl(userRegistration.idCardUrl)).crossfade(true).build(),
                                    contentDescription = "Cédula",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    error = painterResource(R.drawable.logo_admin)
                                )
                            }
                        }
                    }
                }
            } else {
                Text("No se encontró registro con foto de perfil o documento de identidad para este usuario.", color = Color.White.copy(0.4f), fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Spacer(Modifier.height(16.dp))

            // Acciones / Comentario
            if (req.status == "pending") {
                AdminTextField(comm, { comm = it }, "Comentario de revisión para el cliente")
                Row(Modifier.padding(top = 12.dp), Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApprove(req, comm) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("APROBAR", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onDeny(req, comm) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("DENEGAR", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                if (req.adminComment.isNotEmpty()) {
                    Text("NOTA ADMIN: ${req.adminComment}", color = Color.White.copy(0.6f), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                }
                if (req.status == "approved" || req.status == "paid") {
                    Button(
                        onClick = { onViewReceipt?.invoke(req) },
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFFC5A059))
                    ) {
                        Text("VER COMPROBANTE", color = Color(0xFFC5A059), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val googleSheetsUrlNotification = "https://script.google.com/macros/s/AKfycbzTKwRkgCmy_m42ZeKjPbczOMr0YHmRKiSmrHPCSEdKixHzI9MG3fhEfEU3pChr45exvw/exec"

private fun notifyEmail(jsonMap: Map<String, Any>) {
    try {
        val json = org.json.JSONObject(jsonMap)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(googleSheetsUrlNotification).post(body).build()
        OkHttpClient().newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {}
        })
    } catch (_: Exception) {}
}

private fun updateCreditRequest(req: CreditRequest, status: String, comment: String, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    val updateMap = mutableMapOf<String, Any>(
        "status" to status,
        "adminComment" to comment,
        "processedTimestamp" to System.currentTimeMillis()
    )

    if (status == "approved") {
        val subBss = req.amountBss.replace(" BSS", "").trim().toDoubleOrNull() ?: 0.0
        val totalBss = when(req.plan) {
            "weekly_4" -> subBss * 1.1
            "full_30_days" -> subBss * 1.3
            else -> subBss
        }
        updateMap["remainingDebt"] = totalBss
    }

    db.collection("solicitudes_credito").document(req.id).update(updateMap)
        .addOnSuccessListener {
            notifyEmail(mapOf(
                "action" to "creditStatusNotification",
                "customerEmail" to req.customerEmail,
                "customerName" to req.customerName,
                "status" to status.uppercase(),
                "amountUsd" to req.amountUsd,
                "amountBss" to req.amountBss,
                "plan" to req.plan,
                "comment" to comment
            ))
            showToast("✅ ACTUALIZADO - CORREO ENVIADO AL CLIENTE", false)
        }
        .addOnFailureListener { showToast("❌ ERROR AL ACTUALIZAR", true) }
}

private fun deleteCreditRequest(req: CreditRequest, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) { db.collection("solicitudes_credito").document(req.id).delete().addOnSuccessListener { showToast("Eliminado", false) } }

@Composable
fun CreditReceiptDialog(request: CreditRequest, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(request.timestamp))
    val verifyId = request.id.take(12).uppercase()

    val cleanedBssStr = request.amountBss.replace("BSS", "", ignoreCase = true).replace("Bs", "", ignoreCase = true).replace(",", ".").trim()
    val subBss = cleanedBssStr.toDoubleOrNull() ?: if (request.amountUsd > 0 && request.bcvAtRequest > 0) request.amountUsd * request.bcvAtRequest else 0.0
    val rate = if (request.amountUsd > 0 && subBss > 0) subBss / request.amountUsd else request.bcvAtRequest

    var totalBss = subBss
    var intBss = 0.0
    val sch = mutableListOf<Pair<String, Double>>()
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = if (request.timestamp > 0) request.timestamp else System.currentTimeMillis() }

    val plan = when(request.plan) {
        "weekly_4" -> {
            intBss = subBss * 0.10
            totalBss = subBss + intBss
            val inst = totalBss / 4
            for(i in 1..4) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                sch.add("CUOTA $i (${sdf.format(cal.time)})" to inst)
            }
            "4 PAGOS SEMANALES (10%)"
        }
        "weekly_interest" -> {
            val w = subBss * 0.10
            intBss = w * 4
            totalBss = subBss + intBss
            for(i in 1..3) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                sch.add("INTERÉS (${sdf.format(cal.time)})" to w)
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
            sch.add("CAPITAL + FINAL" to (subBss + w))
            "INTERÉS SEMANAL (10%)"
        }
        "full_30_days" -> {
            intBss = subBss * 0.30
            totalBss = subBss + intBss
            cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
            sch.add("PAGO ÚNICO (${sdf.format(cal.time)})" to totalBss)
            "PAGO ÚNICO 30 DÍAS (30%)"
        }
        else -> request.plan.ifEmpty { "CRÉDITO PERSONAL" }.uppercase()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                drawRect(Color.White)
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(R.drawable.logo_admin), null, Modifier.size(70.dp).padding(bottom = 6.dp))
                    Text("STARBIG STORE", color = Color(0xFFC5A059), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("BOUTIQUE EXCLUSIVA", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("RIF: V-22727679-3", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    DashedDivider(Modifier.padding(vertical = 8.dp))
                    Text("COMPROBANTE DE SOLICITUD DE CRÉDITO", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("REF: $verifyId", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    DashedDivider(Modifier.padding(vertical = 8.dp))

                    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                        ReceiptRow("FECHA:", date)
                        ReceiptRow("CLIENTE:", request.customerName.uppercase())
                        ReceiptRow("CÉDULA:", request.idNumber.ifEmpty { "N/A" })
                        ReceiptRow("TELÉFONO:", request.phone.ifEmpty { "N/A" })
                        ReceiptRow("CORREO:", request.customerEmail.ifEmpty { "N/A" })
                    }
                    Spacer(Modifier.height(8.dp)); DashedDivider()

                    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                        ReceiptRow("MONTO USD:", "$${request.amountUsd} USD")
                        ReceiptRow("MONTO BASE BSS:", "${subBss.format(2)} BSS")
                        if (rate > 0) ReceiptRow("TASA BCV:", "${rate.format(2)} BSS/USD")
                        ReceiptRow("PLAN DE PAGO:", plan)
                        if (request.dueDate.isNotEmpty()) ReceiptRow("FECHA LÍMITE:", request.dueDate)
                    }
                    Spacer(Modifier.height(8.dp)); DashedDivider()

                    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                        ReceiptRow("BANCO CLIENTE:", request.bank.ifEmpty { "N/A" })
                        ReceiptRow("CUENTA / TLF:", request.account.ifEmpty { "N/A" })
                        if (request.reason.isNotEmpty()) ReceiptRow("MOTIVO:", request.reason)
                    }
                    Spacer(Modifier.height(8.dp)); DashedDivider()

                    if (sch.isNotEmpty()) {
                        Text("CRONOGRAMA DE CUOTAS", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp)) {
                            sch.forEach { item ->
                                ReceiptRow(item.first, "${item.second.format(2)} BSS")
                            }
                        }
                        Spacer(Modifier.height(8.dp)); DashedDivider()
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("TOTAL A PAGAR:", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 13.sp)
                        Text("${totalBss.format(2)} BSS", fontWeight = FontWeight.Black, color = Color(0xFFC5A059), fontSize = 15.sp)
                    }
                    DashedDivider()
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val details = listOf(
                                    "MONTO USD:" to "$${request.amountUsd} USD",
                                    "MONTO BSS:" to "${subBss.format(2)} BSS",
                                    "PLAN:" to plan,
                                    "BANCO:" to (request.bank.ifEmpty { "N/A" }),
                                    "CUENTA:" to (request.account.ifEmpty { "N/A" })
                                ) + sch.map { it.first to "${it.second.format(2)} BSS" }

                                val bitmap = createReceiptBitmap(
                                    title = "COMPROBANTE DE CRÉDITO",
                                    verifyId = verifyId,
                                    date = date,
                                    customerName = request.customerName.uppercase(),
                                    idNumber = request.idNumber,
                                    phone = request.phone,
                                    email = request.customerEmail,
                                    details = details,
                                    totalBss = "${totalBss.format(2)} BSS"
                                )
                                shareReceiptBitmap(context, bitmap, verifyId)
                            } catch(e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
                ) {
                    Text("COMPARTIR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val details = listOf(
                                    "MONTO USD:" to "$${request.amountUsd} USD",
                                    "MONTO BSS:" to "${subBss.format(2)} BSS",
                                    "PLAN:" to plan,
                                    "BANCO:" to (request.bank.ifEmpty { "N/A" }),
                                    "CUENTA:" to (request.account.ifEmpty { "N/A" })
                                ) + sch.map { it.first to "${it.second.format(2)} BSS" }

                                val bitmap = createReceiptBitmap(
                                    title = "COMPROBANTE DE CRÉDITO",
                                    verifyId = verifyId,
                                    date = date,
                                    customerName = request.customerName.uppercase(),
                                    idNumber = request.idNumber,
                                    phone = request.phone,
                                    email = request.customerEmail,
                                    details = details,
                                    totalBss = "${totalBss.format(2)} BSS"
                                )
                                printReceiptBitmap(context, bitmap, verifyId)
                            } catch(e: Exception) {
                                Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A20)),
                    border = BorderStroke(1.dp, Color(0xFFC5A059))
                ) {
                    Text("IMPRIMIR", color = Color(0xFFC5A059), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f))
                ) {
                    Text("CERRAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun OrderReceiptDialog(order: Order, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope()
    val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))
    val verifyId = order.id.take(12).uppercase()

    val totalUsdVal = order.totalUsd.replace("$", "").trim().toDoubleOrNull() ?: 0.0
    val totalBssVal = order.totalBss.replace("BSS", "", ignoreCase = true).replace("Bs", "", ignoreCase = true).replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    val hasCredit = order.hasCredit || order.items.any { it.paymentMethod == "credit" }

    val baseUsd = if (hasCredit) totalUsdVal / 1.10 else totalUsdVal
    val incrementUsd = if (hasCredit) totalUsdVal - baseUsd else 0.0
    val baseBss = if (hasCredit) totalBssVal / 1.10 else totalBssVal
    val incrementBss = if (hasCredit) totalBssVal - baseBss else 0.0

    val initialBss = if (hasCredit) totalBssVal * 0.25 else 0.0
    val remainingFinancedBss = if (hasCredit) totalBssVal * 0.75 else 0.0

    val numCuotas = if (order.numCuotas > 0) order.numCuotas else 2
    val installmentBss = if (numCuotas > 0) remainingFinancedBss / numCuotas else 0.0

    val sch = mutableListOf<Pair<String, Double>>()
    if (hasCredit) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = if (order.timestamp > 0) order.timestamp else System.currentTimeMillis() }
        for(i in 1..numCuotas) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
            sch.add("CUOTA $i (${sdf.format(cal.time)})" to installmentBss)
        }
    }

    val currentRemaining = order.remainingDebt ?: if (hasCredit) remainingFinancedBss else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                drawRect(Color.White)
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.logo_admin), null, Modifier.size(70.dp).padding(bottom = 6.dp))
                    Text("STARBIG STORE", color = Color(0xFFC5A059), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("BOUTIQUE EXCLUSIVA", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("RIF: V-22727679-3", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    DashedDivider(Modifier.padding(vertical = 8.dp))
                    Text("COMPROBANTE DE COMPRA", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("REF: $verifyId", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    DashedDivider(Modifier.padding(vertical = 8.dp))

                    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                        ReceiptRow("ORDEN:", order.orderId)
                        ReceiptRow("FECHA:", date)
                        ReceiptRow("CLIENTE:", order.customerName.uppercase())
                        if (order.idNumber.isNotEmpty()) ReceiptRow("CÉDULA:", order.idNumber)
                        if (order.phone.isNotEmpty()) ReceiptRow("TELÉFONO:", order.phone)
                        if (order.customerEmail.isNotEmpty()) ReceiptRow("CORREO:", order.customerEmail)
                    }
                    Spacer(Modifier.height(8.dp)); DashedDivider()

                    Text("DETALLE DE PRODUCTOS", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        order.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(
                                    text = "${item.buyQty}x ${item.name}" + if(item.paymentMethod == "credit") " (CRÉDITO)" else "",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                                Text("$${item.priceUsd}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    DashedDivider()

                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween) {
                        Text("TOTAL COMPRA USD:", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                        Text("$${totalUsdVal.format(2)}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), Arrangement.SpaceBetween) {
                        Text("TOTAL COMPRA BSS:", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                        Text("${totalBssVal.format(2)} BSS", color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    DashedDivider()

                    if (hasCredit) {
                        Spacer(Modifier.height(8.dp))
                        Text("DESGLOSE DE CRÉDITO Y FINANCIAMIENTO (+10%)", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp)) {
                            ReceiptRow("MONTO BASE CONTADO:", "$${baseUsd.format(2)} (${baseBss.format(2)} BSS)")
                            ReceiptRow("RECARGO CRÉDITO (+10%):", "+$${incrementUsd.format(2)} (+${incrementBss.format(2)} BSS)")
                            ReceiptRow("TOTAL CON INCREMENTO:", "$${totalUsdVal.format(2)} (${totalBssVal.format(2)} BSS)")
                            ReceiptRow("INICIAL PAGADA (25%):", "-${initialBss.format(2)} BSS")
                            ReceiptRow("SALDO A FINANCIAR:", "${remainingFinancedBss.format(2)} BSS")
                        }
                        Spacer(Modifier.height(8.dp)); DashedDivider()

                        if (sch.isNotEmpty()) {
                            Text("CRONOGRAMA DE FECHAS DE PAGO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp)) {
                                sch.forEach { item ->
                                    ReceiptRow(item.first, "${item.second.format(2)} BSS")
                                }
                            }
                            Spacer(Modifier.height(8.dp)); DashedDivider()
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("SALDO PENDIENTE ACTUAL:", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 12.sp)
                        Text("${currentRemaining.format(2)} BSS", fontWeight = FontWeight.Black, color = if(currentRemaining <= 0) Color(0xFF25D366) else Color.Red, fontSize = 13.sp)
                    }
                    DashedDivider()
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val details = mutableListOf<Pair<String, String>>()
                                details.add("TOTAL USD:" to "$${totalUsdVal.format(2)}")
                                details.add("TOTAL BSS:" to "${totalBssVal.format(2)} BSS")
                                if (hasCredit) {
                                    details.add("BASE CONTADO:" to "${baseBss.format(2)} BSS")
                                    details.add("RECARGO (+10%):" to "+${incrementBss.format(2)} BSS")
                                    details.add("INICIAL (25%):" to "-${initialBss.format(2)} BSS")
                                }
                                sch.forEach { details.add(it.first to "${it.second.format(2)} BSS") }

                                val bitmap = createReceiptBitmap(
                                    title = "COMPROBANTE DE COMPRA",
                                    verifyId = verifyId,
                                    date = date,
                                    customerName = order.customerName.uppercase(),
                                    idNumber = order.idNumber,
                                    phone = order.phone,
                                    email = order.customerEmail,
                                    details = details,
                                    totalBss = "${totalBssVal.format(2)} BSS"
                                )
                                shareReceiptBitmap(context, bitmap, verifyId)
                            } catch(e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
                ) {
                    Text("COMPARTIR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val details = mutableListOf<Pair<String, String>>()
                                details.add("TOTAL USD:" to "$${totalUsdVal.format(2)}")
                                details.add("TOTAL BSS:" to "${totalBssVal.format(2)} BSS")
                                if (hasCredit) {
                                    details.add("BASE CONTADO:" to "${baseBss.format(2)} BSS")
                                    details.add("RECARGO (+10%):" to "+${incrementBss.format(2)} BSS")
                                    details.add("INICIAL (25%):" to "-${initialBss.format(2)} BSS")
                                }
                                sch.forEach { details.add(it.first to "${it.second.format(2)} BSS") }

                                val bitmap = createReceiptBitmap(
                                    title = "COMPROBANTE DE COMPRA",
                                    verifyId = verifyId,
                                    date = date,
                                    customerName = order.customerName.uppercase(),
                                    idNumber = order.idNumber,
                                    phone = order.phone,
                                    email = order.customerEmail,
                                    details = details,
                                    totalBss = "${totalBssVal.format(2)} BSS"
                                )
                                printReceiptBitmap(context, bitmap, verifyId)
                            } catch(e: Exception) {
                                Toast.makeText(context, "Error al imprimir", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A20)),
                    border = BorderStroke(1.dp, Color(0xFFC5A059))
                ) {
                    Text("IMPRIMIR", color = Color(0xFFC5A059), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(0.3f))
                ) {
                    Text("CERRAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(color = Color.Black.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), pathEffect = pathEffect, strokeWidth = 2f)
    }
}

@Composable
fun ReceiptRow(label: String, value: String) { Row(modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp)); Text(value, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Medium) } }

@Composable
fun ReceiptRowFin(label: String, usd: String, bss: String) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Medium); Text(if(usd.isNotEmpty()) "$usd $bss" else bss, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

fun fixDriveUrl(url: String?): String {
    if (url.isNullOrBlank() || url.contains("subiendo")) return "https://via.placeholder.com/200?text=STARBIG"
    if (url.contains("firebasestorage.googleapis.com") || url.contains("appspot.com")) return url

    val id = when {
        url.contains("id=") -> url.split("id=").getOrNull(1)?.split("&")?.getOrNull(0)
        url.contains("file/d/") -> url.split("file/d/").getOrNull(1)?.split("/")?.getOrNull(0)
        url.length > 20 && !url.contains("/") && !url.contains(".") -> url
        else -> null
    }
    return if (id != null) "https://lh3.googleusercontent.com/d/$id" else url ?: ""
}

private fun createReceiptBitmap(
    title: String,
    verifyId: String,
    date: String,
    customerName: String,
    idNumber: String,
    phone: String,
    email: String,
    details: List<Pair<String, String>>,
    totalBss: String
): Bitmap {
    val width = 1000
    val height = 1400
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
        textSize = 22f
    }
    val boldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
        textSize = 24f
        isFakeBoldText = true
    }
    val goldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#C5A059")
        isAntiAlias = true
        textSize = 34f
        isFakeBoldText = true
    }

    val leftMargin = 110f
    val rightMargin = width - 110f
    var y = 90f

    // Header
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("STARBIG STORE", width / 2f, y, goldPaint)
    y += 42f
    paint.textSize = 20f
    canvas.drawText("BOUTIQUE EXCLUSIVA", width / 2f, y, paint)
    y += 32f
    canvas.drawText("RIF: V-22727679-3", width / 2f, y, paint)
    y += 35f

    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#CCCCCC")
        strokeWidth = 2f
    }
    canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
    y += 45f

    boldPaint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText(title, width / 2f, y, boldPaint)
    y += 32f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("REF: $verifyId", width / 2f, y, paint)
    y += 35f
    canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
    y += 45f

    // Customer info
    paint.textAlign = android.graphics.Paint.Align.LEFT
    canvas.drawText("FECHA:", leftMargin, y, boldPaint)
    canvas.drawText(date, leftMargin + 150f, y, paint)
    y += 35f

    canvas.drawText("CLIENTE:", leftMargin, y, boldPaint)
    canvas.drawText(customerName.take(30), leftMargin + 150f, y, boldPaint)
    y += 35f

    if (idNumber.isNotEmpty()) {
        canvas.drawText("CÉDULA:", leftMargin, y, boldPaint)
        canvas.drawText(idNumber, leftMargin + 150f, y, paint)
        y += 34f
    }
    if (phone.isNotEmpty()) {
        canvas.drawText("TELÉFONO:", leftMargin, y, boldPaint)
        canvas.drawText(phone, leftMargin + 150f, y, paint)
        y += 34f
    }
    if (email.isNotEmpty()) {
        canvas.drawText("CORREO:", leftMargin, y, boldPaint)
        canvas.drawText(email.take(32), leftMargin + 150f, y, paint)
        y += 34f
    }

    y += 15f
    canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
    y += 40f

    // Breakdown details
    details.forEach { (label, value) ->
        canvas.drawText(label, leftMargin, y, boldPaint)
        canvas.drawText(value, leftMargin + 280f, y, paint)
        y += 36f
    }

    y += 20f
    canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
    y += 55f

    // Total
    canvas.drawText("TOTAL A PAGAR:", leftMargin, y, boldPaint)
    goldPaint.textAlign = android.graphics.Paint.Align.RIGHT
    canvas.drawText(totalBss, rightMargin, y, goldPaint)

    return bitmap
}

private fun shareReceiptBitmap(context: android.content.Context, bitmap: Bitmap, verifyId: String) {
    val file = File(File(context.cacheDir, "images").apply { mkdirs() }, "Comprobante_${verifyId}.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri("", uri)
    }, "Compartir Comprobante"))
}

private fun printReceiptBitmap(context: android.content.Context, bitmap: Bitmap, verifyId: String) {
    PrintHelper(context).apply {
        scaleMode = PrintHelper.SCALE_MODE_FIT
        printBitmap("Comprobante_${verifyId}", bitmap)
    }
}

private fun deleteNews(news: News, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("novedades").document(news.id).delete().addOnSuccessListener {
        showToast("✅ NOVEDAD ELIMINADA", false)
    }.addOnFailureListener {
        showToast("❌ ERROR AL ELIMINAR", true)
    }
}

@Composable
fun AccountingClosureDialog(
    periodName: String,
    txs: List<SummaryTx>,
    totalUsd: Double,
    totalDebt: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val nowStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Centrado Correctamente
                Image(painterResource(R.drawable.logo_admin), null, Modifier.size(70.dp).padding(bottom = 4.dp))
                Text("STARBIG STORE", color = Color(0xFFC5A059), fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("BOUTIQUE EXCLUSIVA", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("RIF: V-22727679-3", color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                DashedDivider(Modifier.padding(vertical = 10.dp))

                Text("REPORTE DE CIERRE CONTABLE TAMAÑO CARTA", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("PERÍODO: ${periodName.uppercase()} | EMISIÓN: $nowStr", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Resumen de Cuentas
                Column(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    ReceiptRow("TRANSACCIONES TOTALES:", "${txs.size}")
                    ReceiptRow("TOTAL VENTAS USD:", "$${totalUsd.format(2)}")
                    ReceiptRow("DEUDA PENDIENTE:", "$${totalDebt.format(2)}")
                }
                DashedDivider(Modifier.padding(vertical = 10.dp))

                // Desglose Detallado Completo
                Text("DESGLOSE DETALLADO DE CUENTAS:", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))

                txs.forEach { tx ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("${tx.date} • ${tx.refId}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            Text("$${tx.totalUsd.format(2)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFC5A059))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("CLIENTE: ${tx.customerName.uppercase()} | C.I: ${tx.idNumber.ifEmpty { "S/C" }}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        if (tx.phone.isNotEmpty()) Text("TELÉFONO: ${tx.phone}", fontSize = 8.sp, color = Color.Gray)
                        if (tx.customerEmail.isNotEmpty()) Text("CORREO: ${tx.customerEmail}", fontSize = 8.sp, color = Color.Gray)
                        Text("CONCEPTO: ${tx.description}", fontSize = 9.sp, color = Color.Black.copy(0.8f))
                        if (tx.referenceCode.isNotEmpty()) Text("REF PAGO: ${tx.referenceCode}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5A059))
                        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(1.dp).background(Color.LightGray.copy(0.4f)))
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val details = mutableListOf<Pair<String, String>>()
                                    details.add("PERÍODO CIERRE:" to periodName.uppercase())
                                    details.add("TRANSACCIONES:" to "${txs.size}")
                                    details.add("TOTAL VENTAS USD:" to "$${totalUsd.format(2)}")
                                    details.add("DEUDA PENDIENTE:" to "$${totalDebt.format(2)}")

                                    txs.take(20).forEach {
                                        details.add("${it.refId} (${it.customerName.take(12)}):" to "$${it.totalUsd.format(2)}")
                                    }

                                    val bitmap = createReceiptBitmap(
                                        title = "CIERRE CONTABLE $periodName",
                                        verifyId = "CIERRE-${System.currentTimeMillis().toString().takeLast(6)}",
                                        date = nowStr,
                                        customerName = "STARBIG STORE ADMIN",
                                        idNumber = "V-22727679-3",
                                        phone = "CONTABILIDAD",
                                        email = "ADMINISTRACION",
                                        details = details,
                                        totalBss = "$${totalUsd.format(2)}"
                                    )
                                    printReceiptBitmap(context, bitmap, "Cierre_${periodName}")
                                } catch(e: Exception) {
                                    Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
                    ) {
                        Text("IMPRIMIR CARTA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Text("CERRAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
