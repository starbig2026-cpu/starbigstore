package com.example.starbigstore.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
                title = { }, // Quitamos el título para que el logo suba más
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent) // Transparente para ganar espacio
            )
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            if (!isAuthorized) {
                // ... (resto de la pantalla de bloqueo igual)
            } else {
                AdminListContent()
            }
        }
    }
}

@Composable
fun AdminListContent() {
    var registrations by remember { mutableStateOf(listOf<CustomerRegistration>()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()
    val googleSheetsUrl = "https://script.google.com/macros/s/AKfycbzvorSsMtjvqzw6l6FUKwkCBgWjl3rOyhle7AjaGalXfnet6jtDAsjdtxehUxxqwSmPtg/exec"

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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFC5A059))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(), 
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp), // Eliminamos padding superior
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera Formal con Logo
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 0.dp, bottom = 20.dp), // Ajustado al tope
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_admin),
                        contentDescription = null,
                        modifier = Modifier.size(320.dp).offset(y = (-30).dp), // Subimos el logo con un offset negativo
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        "REGISTRO OFICIAL DE CLIENTES",
                        modifier = Modifier.offset(y = (-50).dp), // Subimos el texto para que acompañe al logo
                        color = Color(0xFFC5A059),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontSize = 14.sp
                    )
                    Divider(modifier = Modifier.padding(top = 0.dp).offset(y = (-40).dp), color = Color(0xFFC5A059).copy(alpha = 0.3f))
                }
            }

            items(registrations) { reg ->
                CustomerAdminCard(reg, 
                    onApprove = { db.collection("registros_clientes").document(reg.id).update("status", "active") },
                    onReject = { deleteCustomer(reg) }
                )
            }
        }
    }
}

@Composable
fun CustomerAdminCard(reg: CustomerRegistration, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if(reg.status == "active") Color.Green.copy(0.3f) else Color(0xFFC5A059).copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = reg.photoUrl.ifEmpty { "https://cdn-icons-png.flaticon.com/512/149/149071.png" },
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(25.dp)).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(reg.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(reg.email, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Badge(containerColor = if(reg.status == "active") Color.Green else Color.Yellow) {
                    Text(reg.status.uppercase(), modifier = Modifier.padding(4.dp), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("📍 DIRECCIÓN: ${reg.address}", color = Color.LightGray, fontSize = 13.sp)
            Text("📱 WHATSAPP: ${reg.phone}", color = Color.LightGray, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = reg.idCardUrl.ifEmpty { "https://via.placeholder.com/400x200/181822/C5A059?text=Sin+Foto+ID" },
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) { Text("RECHAZAR", color = Color.Red, fontSize = 12.sp) }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) { Text("APROBAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
        }
    }
}
