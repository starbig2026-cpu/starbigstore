package com.example.starbigstore.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.starbigstore.R
import com.example.starbigstore.data.Product
import com.example.starbigstore.ui.components.ProductCard
import com.example.starbigstore.ui.components.fixDriveUrl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onAddToCart: (String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var products by remember { mutableStateOf(listOf<Product>()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productForQuantity by remember { mutableStateOf<Product?>(null) }
    var bcvRate by remember { mutableDoubleStateOf(36.5) }
    var userStatus by remember { mutableStateOf("guest") }
    var userPoints by remember { mutableIntStateOf(0) }
    
    // Flag para navegar después de que el diálogo se cierre
    var pendingLoginNavigation by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) products
        else {
            val query = searchQuery.lowercase().trim()
            products.filter { 
                it.name.lowercase().contains(query) || 
                it.category.lowercase().contains(query) ||
                it.description.lowercase().contains(query) ||
                it.collection.lowercase().contains(query)
            }
        }
    }

    val searchSuggestions = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) emptyList<String>()
        else {
            val query = searchQuery.lowercase().trim()
            val names = products.map { it.name }.filter { it.lowercase().contains(query) }
            val categories = products.map { it.category }.distinct().filter { it.lowercase().contains(query) }
            (categories + names).take(5)
        }
    }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Manejar la navegación después de cerrar el diálogo
    LaunchedEffect(selectedProduct) {
        if (selectedProduct == null && pendingLoginNavigation) {
            pendingLoginNavigation = false
            onNavigateToLogin()
        }
    }

    LaunchedEffect(Unit) {
        db.collection("productos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                products = snapshot.documents.map { doc ->
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        priceUsd = doc.getDouble("priceUsd") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "",
                        collection = doc.getString("collection") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        allowCredit = doc.getBoolean("allowCredit") ?: false
                    )
                }
            }
        }
        db.collection("config").document("tasa_bcv").addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                bcvRate = doc.getDouble("valor") ?: 36.5
            }
        }
    }

    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("registros_clientes")
                .whereEqualTo("email", user.email?.trim())
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        userStatus = doc.getString("status") ?: "unverified"
                        userPoints = doc.getLong("points")?.toInt() ?: 0
                    }
                }
        } else {
            userStatus = "guest"
            userPoints = 0
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Catálogo Premium",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar perfume, tecnología...", color = Color.White.copy(0.4f), fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC5A059),
                unfocusedBorderColor = Color.White.copy(0.1f),
                cursorColor = Color(0xFFC5A059),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(0.4f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        )

        if (searchSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20)),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.2f))
            ) {
                Column {
                    searchSuggestions.forEach { suggestion ->
                        TextButton(
                            onClick = { searchQuery = suggestion },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = Color(0xFFC5A059).copy(0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    suggestion.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredProducts) { product ->
                ProductCard(product = product, onClick = { selectedProduct = product })
            }
        }
    }

    if (selectedProduct != null) {
        ProductDetailDialog(
            product = selectedProduct!!,
            allProducts = products,
            bcvRate = bcvRate,
            userStatus = userStatus,
            onGuestClick = {
                pendingLoginNavigation = true
                selectedProduct = null
            },
            onBuyClick = {
                productForQuantity = selectedProduct
                selectedProduct = null
            },
            onProductClick = { selectedProduct = it },
            onDismiss = { selectedProduct = null }
        )
    }

    if (productForQuantity != null) {
        QuantitySelectorDialog(
            product = productForQuantity!!,
            bcvRate = bcvRate,
            userPoints = userPoints,
            onDismiss = { productForQuantity = null },
            onConfirm = { qty, method, initialPerc ->
                val id = productForQuantity?.id ?: ""
                val name = productForQuantity?.name ?: ""
                productForQuantity = null
                onAddToCart(id, qty, "$method - Inicial $initialPerc%")
                Toast.makeText(context, "✅ ${qty}x $name ($method) AGREGADO AL CARRITO", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun QuantitySelectorDialog(
    product: Product,
    bcvRate: Double,
    userPoints: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Int) -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableIntStateOf(1) }
    var isCredit by remember { mutableStateOf(false) }
    var initialPercent by remember { mutableIntStateOf(25) }
    
    val totalCashBss = product.priceUsd * quantity * bcvRate
    val subtotalUsd = product.priceUsd * quantity
    val totalCreditBss = totalCashBss * 1.1
    val initialPayBss = totalCreditBss * (initialPercent / 100f)
    val remainingBss = totalCreditBss - initialPayBss
    
    val extraCuotas = userPoints / 550
    val numCuotas = (2 + extraCuotas).coerceAtMost(12)
    val installmentVal = remainingBss / numCuotas
    val installmentPerc = if (numCuotas > 0) ((remainingBss / totalCreditBss) / numCuotas) * 100 else 0f

    // Forzar contado si la compra es menor a 10$
    LaunchedEffect(subtotalUsd) {
        if (subtotalUsd < 10.0) {
            isCredit = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("¿CUÁNTOS DESEAS?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AsyncImage(
                    model = fixDriveUrl(product.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(product.name.uppercase(), color = Color(0xFFC5A059), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if(quantity > 1) quantity-- }) {
                        Icon(Icons.Default.Remove, null, tint = Color.White)
                    }
                    Text(quantity.toString(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                }
                
                if (product.allowCredit) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { isCredit = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(!isCredit) Color(0xFFC5A059) else Color(0xFF1A1A20),
                                contentColor = if(!isCredit) Color.Black else Color.White.copy(0.4f)
                            )
                        ) { Text("CONTADO", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        
                        Button(
                            onClick = { 
                                if (subtotalUsd >= 10.0) {
                                    isCredit = true
                                } else {
                                    Toast.makeText(context, "MÍNIMO $10 PARA CRÉDITO", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if(isCredit) Color(0xFFC5A059) else Color(0xFF1A1A20),
                                contentColor = if(isCredit) Color.Black else Color.White.copy(0.4f)
                            )
                        ) { Text("CRÉDITO", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                    
                    if (subtotalUsd < 10.0) {
                        Text(
                            "Crédito disponible desde $10", 
                            color = Color.White.copy(0.4f), 
                            fontSize = 8.sp, 
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (isCredit) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("PORCENTAJE DE INICIAL", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(25, 40, 60, 100).forEach { perc ->
                                Button(
                                    onClick = { initialPercent = perc },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if(initialPercent == perc) Color(0xFFC5A059) else Color(0xFF1A1A20),
                                        contentColor = if(initialPercent == perc) Color.Black else Color.White.copy(0.4f)
                                    )
                                ) { Text("$perc%", fontSize = 9.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = Color.White.copy(0.03f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!isCredit) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PAGO DE CONTADO:", color = Color.White.copy(0.5f), fontSize = 10.sp)
                                Text("${totalCashBss.toInt()} BSS", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOTAL CRÉDITO:", color = Color(0xFFC5A059), fontSize = 10.sp)
                                    Text("${totalCreditBss.toInt()} BSS", color = Color(0xFFC5A059), fontWeight = FontWeight.Black)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("INICIAL ($initialPercent%):", color = Color.White.copy(0.6f), fontSize = 9.sp)
                                    Text("${initialPayBss.toInt()} BSS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                if (initialPercent < 100) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        val percStr = java.lang.String.format(java.util.Locale.US, "%.1f", installmentPerc)
                                        Text("$numCuotas CUOTAS ($percStr% c/u):", color = Color.White.copy(0.6f), fontSize = 9.sp)
                                        Text("${installmentVal.toInt()} BSS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onConfirm(quantity, if(isCredit) "CRÉDITO" else "CONTADO", if(isCredit) initialPercent else 100) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("AGREGAR AL CARRITO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    allProducts: List<Product>,
    bcvRate: Double,
    userStatus: String,
    onGuestClick: () -> Unit,
    onBuyClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val relatedProducts = remember(product, allProducts) {
        allProducts.filter { it.category == product.category && it.id != product.id }.take(4)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D11)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFC5A059).copy(0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fixDriveUrl(product.imageUrl))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.logo_admin)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = product.category.uppercase(),
                        color = Color(0xFFC5A059),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = product.name.uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$${product.priceUsd}",
                            color = Color(0xFFC5A059),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "(${(product.priceUsd * bcvRate).toInt()} BSS)",
                            color = Color.White.copy(0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Surface(
                        color = Color.White.copy(0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = product.description.ifEmpty { "Sin descripción disponible para este producto exclusivo de Starbig Store." },
                            color = Color.White.copy(0.8f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoBadge(label = "STOCK", value = "${product.stock} UNIDADES", modifier = Modifier.weight(1f))
                        InfoBadge(
                            label = "PAGO", 
                            value = if(product.allowCredit) "APTO CRÉDITO" else "CONTADO",
                            modifier = Modifier.weight(1f),
                            valueColor = if(product.allowCredit) Color(0xFFC5A059) else Color(0xFFFF4444)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Button(
                        onClick = {
                            when (userStatus) {
                                "active" -> {
                                    onBuyClick()
                                }
                                "unverified" -> {
                                    Toast.makeText(context, "⚠️ CUENTA EN REVISIÓN. ESPERE VERIFICACIÓN PARA COMPRAR.", Toast.LENGTH_LONG).show()
                                }
                                "guest" -> {
                                    onGuestClick()
                                }
                                else -> {
                                    Toast.makeText(context, "ESTADO DE CUENTA: ${userStatus.uppercase()}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .widthIn(min = 200.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5A059)),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("COMPRAR AHORA", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    if (relatedProducts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(30.dp))
                        HorizontalDivider(color = Color(0xFFC5A059).copy(0.1f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "TAMBIÉN TE PUEDE GUSTAR",
                            color = Color(0xFFC5A059),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            relatedProducts.forEach { rp ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.White.copy(0.03f), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Card(
                                        onClick = { onProductClick(rp) },
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = fixDriveUrl(rp.imageUrl),
                                                contentDescription = null,
                                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                rp.name.uppercase(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "$${rp.priceUsd}",
                                                color = Color(0xFFC5A059),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(50.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.White) {
    Column(
        modifier = modifier
            .background(Color.White.copy(0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.White.copy(0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}
