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
    val remainingDebt: Double? = null
)

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
    val pointsUsed: Int = 0,
    val collection: String = "pedidos"
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

    fun refreshCombinedList(db: FirebaseFirestore) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pedidos = db.collection("pedidos").orderBy("timestamp", Query.Direction.DESCENDING).get().await()
                val credits = db.collection("solicitudes_credito").whereEqualTo("status", "awaiting_verification").get().await()

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

                    allOrders.add(Order(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "ORDEN",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        items = items,
                        totalUsd = doc.getString("totalUsd") ?: "",
                        totalBss = doc.getString("totalBss") ?: "",
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        paymentReport = report,
                        pointsUsed = (doc.getLong("pointsUsed") ?: 0).toInt(),
                        collection = "pedidos"
                    ))
                }

                credits.documents.forEach { doc ->
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

                    allOrders.add(Order(
                        id = doc.id,
                        orderId = "CRÉDITO",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        items = listOf(OrderItem(name = "ABONO A CRÉDITO PERSONAL", buyQty = 1, paymentMethod = "CRÉDITO", priceUsd = doc.getDouble("amountUsd") ?: 0.0)),
                        totalUsd = "$${doc.getDouble("amountUsd") ?: 0.0}",
                        totalBss = doc.getString("amountBss") ?: "",
                        status = "awaiting_verification",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        paymentReport = report,
                        collection = "solicitudes_credito"
                    ))
                }

                orders = allOrders.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                android.util.Log.e("AdminScreen", "Refresh Error", e)
            }
        }
    }

    fun syncCombinedOrders(db: FirebaseFirestore) {
        // Escuchar Pedidos
        db.collection("pedidos").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { _, _ ->
                refreshCombinedList(db)
            }
        // Escuchar Abonos de Créditos
        db.collection("solicitudes_credito").whereEqualTo("status", "awaiting_verification")
            .addSnapshotListener { _, _ ->
                refreshCombinedList(db)
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

        syncCombinedOrders(db)

        db.collection("novedades").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) newsList = snapshot.documents.map { doc ->
                    News(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
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
                        adminComment = doc.getString("adminComment") ?: ""
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
                val menuItems = listOf(
                    Triple("USUARIOS", Icons.Default.Group, 0),
                    Triple("STOCK", Icons.Default.Inventory, 1),
                    Triple("SOLICITUD", Icons.Default.HourglassEmpty, 2),
                    Triple("PAGOS", Icons.Default.Payments, 3),
                    Triple("COBROS", Icons.Default.MonetizationOn, 4),
                    Triple("NOVEDAD", Icons.Default.Campaign, 5),
                    Triple("CRÉDITOS", Icons.Default.CreditCard, 6)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(menuItems.size) { index ->
                        val item = menuItems[index]
                        AdminNavButton(
                            text = item.first,
                            icon = item.second,
                            isSelected = selectedTab == item.third,
                            modifier = Modifier.fillMaxWidth()
                        ) { selectedTab = item.third }
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
                        orders = orders,
                        onConfirmPayment = { confirmOrderPayment(it, db, showModernToast) },
                        onRejectPayment = { rejectOrderPayment(it, db, showModernToast) },
                        onDeleteSale = { deleteOrder(it, db, showModernToast) },
                        onClearHistory = { clearCompletedOrders(db, showModernToast) },
                        onImageClick = { expandedImageUrl = it }
                    )
                    5 -> NewsAdminSection(
                        newsList = newsList,
                        onAddNews = { showAddNewsDialog = true },
                        onDeleteNews = { newsToDelete = it },
                        onImageClick = { expandedImageUrl = it }
                    )
                    6 -> CreditRequestsSection(
                        requests = creditRequests,
                        onApprove = { req: CreditRequest, comment: String -> updateCreditRequest(req, "approved", comment, db, showModernToast) },
                        onDeny = { req: CreditRequest, comment: String -> updateCreditRequest(req, "denied", comment, db, showModernToast) }
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
            AddProductDialog(
                editingProduct = productToEdit,
                onDismiss = {
                    showAddProductDialog = false
                    productToEdit = null
                },
                onConfirm = { product, uri ->
                    isLoading = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val base64 = uri?.let { u ->
                                context.contentResolver.openInputStream(u)?.use { it.readBytes() }?.let {
                                    android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                                }
                            }

                            val editing = productToEdit
                            val oldCategory = editing?.category?.let {
                                java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                                    .uppercase()
                            }
                            val oldName = editing?.name

                            val normalizedCat = java.text.Normalizer.normalize(product.category, java.text.Normalizer.Form.NFD)
                                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                                .uppercase()

                            val json = org.json.JSONObject().apply {
                                put("action", "addProduct")
                                put("sheetName", normalizedCat)
                                if (editing != null) {
                                    put("oldCategory", oldCategory)
                                    put("oldName", oldName)
                                }
                                put("photoBase64", base64 ?: "")
                                put("fileName", "PROD_${System.currentTimeMillis()}.jpg")
                                put("folderName", "PRODUCTOS_STARBIG")
                                put("data", org.json.JSONObject().apply {
                                    put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                                    put("nombre", product.name)
                                    put("precio", product.priceUsd)
                                    put("stock", product.stock)
                                    put("coleccion", product.collection)
                                    put("descripcion", product.description)
                                    put("credito", if(product.allowCredit) "SÍ" else "NO")
                                    put("imagen", if(base64 == null) product.imageUrl else "")
                                })
                            }

                            val response = OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
                            val respBody = response.body?.string()
                            
                            val driveUrl = if (respBody != null && respBody.startsWith("{")) {
                                org.json.JSONObject(respBody).optString("imageUrl")
                            } else ""

                            val finalProductMap = hashMapOf(
                                "name" to product.name,
                                "priceUsd" to product.priceUsd,
                                "stock" to product.stock,
                                "category" to product.category,
                                "collection" to product.collection,
                                "description" to product.description,
                                "allowCredit" to product.allowCredit,
                                "imageUrl" to if (driveUrl.isNotEmpty()) driveUrl else product.imageUrl,
                                "timestamp" to System.currentTimeMillis()
                            )
                            
                            if (editing != null) {
                                db.collection("productos").document(editing.id).update(finalProductMap as Map<String, Any>).await()
                            } else {
                                db.collection("productos").add(finalProductMap).await()
                            }

                            CoroutineScope(Dispatchers.Main).launch {
                                isLoading = false
                                showAddProductDialog = false
                                productToEdit = null
                                showModernToast("✅ PRODUCTO GUARDADO", false)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AdminProduct", "Error: ${e.message}")
                            CoroutineScope(Dispatchers.Main).launch {
                                isLoading = false
                                showModernToast("❌ ERROR AL GUARDAR", true)
                            }
                        }
                    }
                }
            )
        }

        if (productToDelete != null) {
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                containerColor = Color(0xFF121216),
                title = { Text("¿ELIMINAR PRODUCTO?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Esta acción eliminará ${productToDelete?.name?.uppercase()} permanentemente de Firebase y Excel.", color = Color.White.copy(0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            val p = productToDelete!!
                            productToDelete = null
                            deleteProduct(p, db)
                            showModernToast("✅ PRODUCTO ELIMINADO", false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("ELIMINAR", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) }
                }
            )
        }

        if (showAddNewsDialog) {
            AddNewsDialog(
                onDismiss = { showAddNewsDialog = false },
                onConfirm = { uri ->
                    isLoading = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Convertimos la imagen a Base64 para subirla a Google Drive
                            val base64 = try {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let {
                                    android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                                }
                            } catch (_: Exception) { null }

                            if (base64 == null) throw Exception("Error al procesar imagen")

                            // Sincronizar con Google Drive para obtener un link permanente funcional
                            val json = org.json.JSONObject().apply {
                                put("action", "addProduct") // Usamos addProduct porque tu script ya maneja subida a Drive aquí
                                put("sheetName", "NOVEDADES")
                                put("photoBase64", base64)
                                put("fileName", "NEWS_${System.currentTimeMillis()}.jpg")
                                put("folderName", "NOVEDADES_STARBIG")
                                put("data", org.json.JSONObject().apply {
                                    put("fecha", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                                    put("nombre", "FLYER APP")
                                    put("precio", 0)
                                })
                            }

                            val response = OkHttpClient().newCall(Request.Builder().url(GOOGLE_SHEETS_URL).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
                            val respBody = response.body?.string()
                            
                            if (respBody != null) {
                                val resJson = org.json.JSONObject(respBody)
                                val driveUrl = resJson.optString("imageUrl")
                                
                                if (driveUrl.isNotEmpty()) {
                                    // Guardamos en Firestore con el link de Drive funcional
                                    val news = News(imageUrl = driveUrl, timestamp = Date().time)
                                    db.collection("novedades").add(news).await()

                                    CoroutineScope(Dispatchers.Main).launch {
                                        isLoading = false
                                        showAddNewsDialog = false
                                        showModernToast("✅ NOVEDAD PUBLICADA", false)
                                    }
                                } else throw Exception("Link de Drive vacío")
                            } else throw Exception("Sin respuesta del servidor")

                        } catch (e: Exception) {
                            android.util.Log.e("AdminNews", "Error: ${e.message}")
                            CoroutineScope(Dispatchers.Main).launch {
                                isLoading = false
                                showModernToast("❌ ERROR AL PUBLICAR", true)
                            }
                        }
                    }
                }
            )
        }

        if (customerToDelete != null) {
            AlertDialog(
                onDismissRequest = { customerToDelete = null },
                containerColor = Color(0xFF121216),
                title = { Text("¿ELIMINAR USUARIO?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Esta acción eliminará a ${customerToDelete?.name?.uppercase()} de todas las bases de datos.", color = Color.White.copy(0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            val c = customerToDelete!!
                            customerToDelete = null
                            deleteCustomer(c, db)
                            showModernToast("✅ USUARIO ELIMINADO", false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("ELIMINAR", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { customerToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) }
                }
            )
        }

        if (newsToDelete != null) {
            AlertDialog(
                onDismissRequest = { newsToDelete = null },
                containerColor = Color(0xFF121216),
                title = { Text("¿ELIMINAR NOVEDAD?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("¿Seguro que desea eliminar este flyer promocional?", color = Color.White.copy(0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            val n = newsToDelete!!
                            newsToDelete = null
                            deleteNews(n, db, showModernToast)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("ELIMINAR", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { newsToDelete = null }) { Text("CANCELAR", color = Color.White.copy(0.6f)) }
                }
            )
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
fun AdminNavButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) Color(0xFFC5A059) else Color(0xFF08080A),
        label = "bg"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) Color.Black else Color.White.copy(0.5f),
        label = "content"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) Color.White.copy(0.4f) else Color(0xFFC5A059).copy(0.15f),
        label = "border"
    )
    
    Box(
        modifier = modifier
            .height(85.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = contentColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text, 
                fontSize = 8.sp, 
                fontWeight = FontWeight.Black, 
                color = contentColor, 
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
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
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if(isActive) Color.Green.copy(0.3f) else Color(0xFFC5A059).copy(0.2f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFF1A1A20), RoundedCornerShape(8.dp)).clickable { onImageClick(reg.photoUrl) }) { 
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
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black, RoundedCornerShape(8.dp)).clickable { onImageClick(reg.idCardUrl) }) { 
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A20),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("TASA BCV OFICIAL", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$bcv BSS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = onSync, modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(8.dp))) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFC5A059), modifier = Modifier.size(20.dp))
                    }
                }
                Button(onClick = onRate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { 
                    Text("AJUSTAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) 
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val tabIndex = categories.indexOf(selectedCategory).let { if (it == -1) 0 else it }
        
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFC5A059),
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                if (tabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = Color(0xFFC5A059),
                        height = 3.dp
                    )
                }
            }
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(selectedCategory == cat) Color(0xFFC5A059) else Color.White.copy(0.4f)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GESTIÓN DE STOCK", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059).copy(0.1f)), shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, null, tint = Color(0xFFC5A059), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NUEVO", color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            val filtered = if(selectedCategory == "Todos") products else products.filter { it.category == selectedCategory }
            items(filtered) { ProductAdminItem(it, bcv, onDelete, onEdit) }
        }
    }
}

@Composable
fun ProductAdminItem(p: Product, bcv: Double, onDelete: (Product) -> Unit, onEdit: (Product) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fixDriveUrl(p.imageUrl))
                    .setHeader("User-Agent", "Mozilla/5.0")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(56.dp).background(Color.Black, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.logo_admin),
                placeholder = painterResource(R.drawable.logo_admin)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$${p.priceUsd}", color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${(p.priceUsd * bcv).format(2)} BSS", color = Color.White.copy(0.5f), fontSize = 11.sp)
                }
                Text("EN STOCK: ${p.stock}", color = if(p.stock < 5) Color.Red.copy(0.7f) else Color.Green.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onEdit(p) }) { Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.4f), modifier = Modifier.size(20.dp)) }
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
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderId, color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date, color = Color.White.copy(0.4f), fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = { onDelete(order) }, modifier = Modifier.size(28.dp).background(Color.Red.copy(0.1f), RoundedCornerShape(6.dp))) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Text(order.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.03f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                order.items.forEach { item ->
                    Text("• ${item.buyQty}x ${item.name} (${item.paymentMethod.uppercase()})", color = Color.White.copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            order.paymentReport?.let { report ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = Color(0xFFC5A059).copy(0.05f), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.2f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("COMPROBANTE DE PAGO", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        PaymentDetailItem("REFERENCIA", report.reference)
                        PaymentDetailItem("ENTIDAD", report.bank)
                        PaymentDetailItem("MONTO", "${report.amount} BSS")
                        if(report.installments.toInt() > 1) PaymentDetailItem("CUOTAS", report.installments)
                        
                        report.captureBase64?.let { base64 ->
                            if (base64.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
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
            
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.totalUsd, color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 20.sp)
                
                when (order.status) {
                    "pending", "awaiting_verification" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onConfirm(order) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), contentPadding = PaddingValues(horizontal = 20.dp), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                                Text("APROBAR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Button(onClick = { onReject(order) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.2f)), contentPadding = PaddingValues(horizontal = 20.dp), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp), border = BorderStroke(1.dp, Color.Red)) {
                                Text("RECHAZAR", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    "paid" -> Surface(color = Color(0xFF25D366).copy(0.1f), shape = RoundedCornerShape(6.dp)) { Text("PAGADO", color = Color(0xFF25D366), fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    "rejected" -> Surface(color = Color.Red.copy(0.1f), shape = RoundedCornerShape(6.dp)) { Text("RECHAZADO", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
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
            if (order.collection == "pedidos") {
                order.items.forEach { if(it.paymentMethod == "cash") totalPointsToGain += 25 }
                if (order.items.any { it.paymentMethod == "credit" }) totalPointsToGain += 10
            } else {
                totalPointsToGain = 10
            }
            
            if (totalPointsToGain > 0) {
                val userSnap = db.collection("registros_clientes").whereEqualTo("email", order.customerEmail).get().await()
                if (!userSnap.isEmpty) {
                    val userDoc = userSnap.documents[0]
                    val currentPoints = userDoc.getLong("points") ?: 0
                    userDoc.reference.update("points", currentPoints + totalPointsToGain).await()
                }
            }
            
            db.collection(order.collection).document(order.id).update("status", "paid", "paymentReport", null).await()
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
            // Si es un abono a crédito activo, regresamos al estado correspondiente
            val isCreditOrder = order.collection == "pedidos" && order.items.any { it.paymentMethod == "credit" }
            val newStatus = if (isCreditOrder) "active_credit" else "rejected"
            db.collection(order.collection).document(order.id).update("status", newStatus, "paymentReport", null).await()
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ PAGO RECHAZADO", true) }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch { showToast("❌ ERROR AL RECHAZAR", true) }
        }
    }
}

private fun deleteOrder(order: Order, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection(order.collection).document(order.id).delete().addOnSuccessListener {
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
    OutlinedTextField(
        value = v,
        onValueChange = onV,
        label = { Text(l, fontSize = 10.sp) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFC5A059),
            unfocusedBorderColor = Color.White.copy(0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF1A1A20),
            unfocusedContainerColor = Color(0xFF1A1A20)
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
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

@Composable
fun NewsAdminSection(
    newsList: List<News>,
    onAddNews: () -> Unit,
    onDeleteNews: (News) -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NOVEDADES & FLYERS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            IconButton(onClick = onAddNews) { Icon(Icons.Default.Add, null, tint = Color(0xFFC5A059)) }
        }
        
        if (newsList.isEmpty()) {
            InfoSection("No hay novedades publicadas")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(newsList) { news ->
                    NewsAdminCard(news, onDeleteNews, onImageClick)
                }
            }
        }
    }
}

@Composable
fun NewsAdminCard(news: News, onDelete: (News) -> Unit, onImageClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black, RoundedCornerShape(12.dp)).clickable { onImageClick(news.imageUrl) }) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fixDriveUrl(news.imageUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.logo_admin)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onDelete(news) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(0.3f)),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINAR", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddNewsDialog(onDismiss: () -> Unit, onConfirm: (Uri) -> Unit) {
    var uri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri = it }

    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF121216), shape = RoundedCornerShape(0.dp),
        title = { Text("NUEVA NOVEDAD", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black).clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    if(uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFC5A059), modifier = Modifier.size(48.dp))
                        Text("SELECCIONAR FLYER", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { uri?.let { onConfirm(it) } }, 
                enabled = uri != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))
            ) { 
                Text("PUBLICAR", color = Color.Black) 
            } 
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.White.copy(0.6f)) }
        }
    )
}

private fun deleteNews(news: News, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("novedades").document(news.id).delete().addOnSuccessListener {
        showToast("Novedad eliminada", false)
    }
}

@Composable
fun CreditRequestsSection(
    requests: List<CreditRequest>,
    onApprove: (CreditRequest, String) -> Unit,
    onDeny: (CreditRequest, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("SOLICITUDES DE CRÉDITO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (requests.isEmpty()) {
            InfoSection("No hay solicitudes")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(requests) { req ->
                    CreditRequestCard(req, onApprove, onDeny)
                }
            }
        }
    }
}

@Composable
fun CreditRequestCard(
    req: CreditRequest,
    onApprove: (CreditRequest, String) -> Unit,
    onDeny: (CreditRequest, String) -> Unit
) {
    var adminComment by remember { mutableStateOf("") }
    val date = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.timestamp))
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, color = Color.White.copy(0.4f), fontSize = 10.sp)
                StatusBadge(req.status)
            }
            
            Text(req.customerName.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("DOCUMENTO: ${req.idNumber}", color = Color(0xFFC5A059), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("CONTACTO: ${req.phone}", color = Color.White.copy(0.6f), fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("JUSTIFICACIÓN DE SOLICITUD", color = Color(0xFFC5A059), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(req.reason, color = Color.White.copy(0.9f), fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(0.05f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BANCO: ${req.bank}", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    Text("CUENTA: ${req.account}", color = Color.White.copy(0.7f), fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("MONTO GLOBAL SOLICITADO", color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$${req.amountUsd} (${req.amountBss} BSS)", color = Color(0xFFC5A059), fontWeight = FontWeight.Black, fontSize = 22.sp)
            
            if (req.status == "pending") {
                Spacer(modifier = Modifier.height(20.dp))
                AdminTextField(adminComment, { adminComment = it }, "Comentarios internos o instrucciones")
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { onApprove(req, adminComment) }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), shape = RoundedCornerShape(12.dp)) {
                        Text("APROBAR", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Button(onClick = { onDeny(req, adminComment) }, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(12.dp)) {
                        Text("DENEGAR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else if (req.adminComment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color.White.copy(0.03f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("NOTA ADMIN: ${req.adminComment}", color = Color.White.copy(0.4f), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

private fun updateCreditRequest(req: CreditRequest, status: String, comment: String, db: FirebaseFirestore, showToast: (String, Boolean) -> Unit) {
    db.collection("solicitudes_credito").document(req.id).update(
        "status", status,
        "adminComment", comment,
        "processedTimestamp", System.currentTimeMillis()
    ).addOnSuccessListener {
        showToast("✅ SOLICITUD ${status.uppercase()}", false)
    }
}
