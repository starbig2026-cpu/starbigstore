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
    var bcvRate by remember { mutableDoubleStateOf(36.5) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        db.collection("registros_clientes").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) registrations = snapshot.documents.map { doc ->
                    CustomerRegistration(id = doc.id, name = doc.getString("name") ?: "S/N", email = doc.getString("email") ?: "S/E", phone = doc.getString("phone") ?: "S/P", address = doc.getString("address") ?: "S/D", status = doc.getString("status") ?: "unverified", photoUrl = doc.getString("photoUrl") ?: "", idCardUrl = doc.getString("idCardUrl") ?: "")
                }
            }

        db.collection("productos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) products = snapshot.documents.map { doc ->
                Product(id = doc.id, name = doc.getString("name") ?: "", priceUsd = doc.getDouble("priceUsd") ?: 0.0, description = doc.getString("description") ?: "", category = doc.getString("category") ?: "", collection = doc.getString("collection") ?: "", imageUrl = doc.getString("imageUrl") ?: "", stock = doc.getLong("stock")?.toInt() ?: 0, allowCredit = doc.getBoolean("allowCredit") ?: false)
            }
        }

        db.collection("config").document("tasa_bcv").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) bcvRate = snapshot.getDouble("valor") ?: 36.5
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF08080A))) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 20.dp)) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("CENTRAL DE INTELIGENCIA", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("STARBIG CONTROL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
                Image(painter = painterResource(id = R.drawable.logo_admin), contentDescription = null, modifier = Modifier.size(80.dp).align(Alignment.TopEnd), contentScale = ContentScale.Fit)
            }

            TrafficMonitorSection()
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminNavButton("BASE DE DATOS", Icons.Default.Storage, selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                AdminNavButton("INVENTARIO", Icons.Default.Inventory, selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                AdminNavButton("SOLICITUDES", Icons.Default.Group, selectedTab == 2, Modifier.weight(1.2f)) { selectedTab = 2 }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFC5A059).copy(alpha = 0.15f), thickness = 0.5.dp)

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        val actives = registrations.filter { it.status == "active" }
                        if (actives.isEmpty()) InfoSection("No hay usuarios activos")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(actives) { reg -> CustomerAdminCard(reg, {}, { deleteCustomer(reg, db, GOOGLE_SHEETS_URL) }, { expandedImageUrl = it }) }
                        }
                    }
                    1 -> InventorySection(products, bcvRate, { showAddProductDialog = true }, { showRateDialog = true }, { db.collection("productos").document(it.id).delete() })
                    2 -> {
                        val pendings = registrations.filter { it.status == "unverified" }
                        if (pendings.isEmpty()) InfoSection("No hay solicitudes")
                        else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(pendings) { reg -> CustomerAdminCard(reg, { approveCustomer(reg, db, GOOGLE_SHEETS_URL) }, { deleteCustomer(reg, db, GOOGLE_SHEETS_URL) }, { expandedImageUrl = it }) }
                        }
                    }
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
                    AsyncImage(model = expandedImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth(0.95f), contentScale = ContentScale.Fit)
                }
            }
        }

        if (showAddProductDialog) {
            AddProductDialog(bcvRate, { showAddProductDialog = false }, { newProd, uri ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var url = ""
                        if (uri != null) {
                            val storage = FirebaseStorage.getInstance()
                            val ref = storage.reference.child("productos/${UUID.randomUUID()}.jpg")
                            
                            // Subida manual asegurada
                            val uploadTask = ref.putFile(uri).await()
                            if (uploadTask.metadata != null) {
                                url = ref.downloadUrl.await().toString()
                            }
                        }
                        
                        db.collection("productos").add(newProd.copy(imageUrl = url)).await()
                        
                        CoroutineScope(Dispatchers.Main).launch { 
                            isLoading = false
                            showAddProductDialog = false
                            Toast.makeText(context, "✅ Guardado con éxito", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch { 
                            isLoading = false
                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                            android.util.Log.e("Admin", "Save Error", e)
                        }
                    }
                }
            })
        }

        if (showRateDialog) {
            BcvRateDialog(bcvRate, { showRateDialog = false }, { db.collection("config").document("tasa_bcv").set(mapOf("valor" to it)); showRateDialog = false })
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

private fun approveCustomer(reg: CustomerRegistration, db: FirebaseFirestore, url: String) {
    db.collection("registros_clientes").document(reg.id).update("status", "active")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { put("action", "updateStatus"); put("email", reg.email.trim()); put("status", "active") }
            OkHttpClient().newCall(Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        } catch (e: Exception) {}
    }
}

private fun deleteCustomer(reg: CustomerRegistration, db: FirebaseFirestore, url: String) {
    db.collection("registros_clientes").document(reg.id).delete()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val json = org.json.JSONObject().apply { put("action", "delete"); put("email", reg.email.trim()) }
            OkHttpClient().newCall(Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        } catch (e: Exception) {}
    }
}

@Composable
fun CustomerAdminCard(reg: CustomerRegistration, onApprove: () -> Unit, onReject: () -> Unit, onImageClick: (String) -> Unit) {
    val isActive = reg.status == "active"
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp), border = BorderStroke(0.5.dp, if(isActive) Color.Green.copy(0.4f) else Color(0xFFC5A059).copy(0.2f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFF1A1A20)).clickable { onImageClick(reg.photoUrl) }) { AsyncImage(model = reg.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
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
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black).clickable { onImageClick(reg.idCardUrl) }) { AsyncImage(model = reg.idCardUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red), shape = RoundedCornerShape(0.dp)) { Text("ELIMINAR") }
                if(!isActive) Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)), shape = RoundedCornerShape(0.dp)) { Text("APROBAR", color = Color.Black) }
            }
        }
    }
}

@Composable
fun InventorySection(products: List<Product>, bcv: Double, onAdd: () -> Unit, onRate: () -> Unit, onDelete: (Product) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("TASA BCV", color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${bcv} BSS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Button(onClick = onRate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A20)), shape = RoundedCornerShape(0.dp)) { Text("TASA", color = Color(0xFFC5A059)) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("STOCK", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null, tint = Color(0xFFC5A059)) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            products.groupBy { it.category }.forEach { (cat, list) ->
                item { Text(cat.uppercase(), color = Color(0xFFC5A059), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) }
                items(list) { ProductAdminItem(it, bcv, onDelete) }
            }
        }
    }
}

@Composable
fun ProductAdminItem(p: Product, bcv: Double, onDelete: (Product) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), shape = RoundedCornerShape(0.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = p.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).background(Color.Black), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(p.name.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row {
                    Text("$${p.priceUsd}", color = Color(0xFFC5A059), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${(p.priceUsd * bcv).format(2)} BSS", color = Color.White.copy(0.6f), fontSize = 11.sp)
                }
                if(p.allowCredit) Text("CRÉDITO: ${(p.priceUsd * bcv * 1.1).format(2)} BSS", color = Color.Yellow.copy(0.8f), fontSize = 9.sp)
            }
            IconButton(onClick = { onDelete(p) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f), modifier = Modifier.size(18.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(bcv: Double, onDismiss: () -> Unit, onConfirm: (Product, Uri?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var allowCredit by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf("Perfumes") }
    var selectedColl by remember { mutableStateOf("Nueva Temporada") }
    var catExp by remember { mutableStateOf(false) }
    var collExp by remember { mutableStateOf(false) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri = it }
    
    val pUsd = priceText.toDoubleOrNull() ?: 0.0

    AlertDialog(onDismissRequest = onDismiss, containerColor = Color(0xFF121216), shape = RoundedCornerShape(0.dp),
        title = { Text("NUEVO PRODUCTO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Black).clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                    if(uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFFC5A059))
                }
                AdminTextField(name, { name = it }, "Nombre")
                AdminTextField(priceText, { priceText = it }, "Precio USD")
                if(pUsd > 0) {
                    Text("Contado: ${(pUsd * bcv).format(2)} BSS", color = Color.Green.copy(0.7f), fontSize = 10.sp)
                    Text("Crédito (+10%): ${(pUsd * bcv * 1.1).format(2)} BSS", color = Color.Yellow.copy(0.7f), fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(allowCredit, { allowCredit = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC5A059))); Text("CRÉDITO DISPONIBLE", color = Color.White, fontSize = 11.sp) }
                
                ExposedDropdownMenuBox(catExp, { catExp = it }) {
                    OutlinedTextField(selectedCat, {}, readOnly = true, label = {Text("Categoría")}, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = dropdownColors())
                    ExposedDropdownMenu(catExp, { catExp = false }) {
                        listOf("Perfumes", "Tecnología", "Ropa", "Calzado", "Belleza").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedCat = it; catExp = false }) }
                    }
                }
                ExposedDropdownMenuBox(collExp, { collExp = it }) {
                    OutlinedTextField(selectedColl, {}, readOnly = true, label = {Text("Colección")}, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(collExp) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = dropdownColors())
                    ExposedDropdownMenu(collExp, { collExp = false }) {
                        listOf("Nueva Temporada", "Edición Limitada", "Best Sellers").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { selectedColl = it; collExp = false }) }
                    }
                }
                AdminTextField(stock, { stock = it }, "Stock")
            }
        },
        confirmButton = { Button(onClick = { onConfirm(Product(name = name, priceUsd = pUsd, category = selectedCat, collection = selectedColl, stock = stock.toIntOrNull() ?: 0, allowCredit = allowCredit), uri) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059))) { Text("GUARDAR", color = Color.Black) } }
    )
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
