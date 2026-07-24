package com.example.starbigstore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class CustomerRegistration(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val status: String = "pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var isAuthorized by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    val correctPin = "1358L"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Panel de Seguridad", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            if (!isAuthorized) {
                // Pantalla de Bloqueo
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Acceso Restringido", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text("PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { if (pinInput == correctPin) isAuthorized = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Desbloquear", color = Color.Black) }
                        }
                    }
                }
            } else {
                AdminContent()
            }
        }
    }
}

@Composable
fun AdminContent() {
    var registrations by remember { mutableStateOf(listOf<CustomerRegistration>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("registros_clientes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    registrations = snapshot.documents.map { doc ->
                        CustomerRegistration(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            phone = doc.getString("phone") ?: "Sin WhatsApp",
                            address = doc.getString("address") ?: "Sin Dirección",
                            status = doc.getString("status") ?: "pending"
                        )
                    }
                }
                isLoading = false
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(registrations) { reg ->
                CustomerCard(reg)
            }
        }
    }
}

@Composable
fun CustomerCard(reg: CustomerRegistration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(reg.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("WhatsApp: ${reg.phone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Dirección: ${reg.address}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (reg.status == "pending") Color(0xFFFFC107) else Color(0xFF4CAF50),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        if (reg.status == "pending") "PENDIENTE" else "APROBADO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
