package com.example.starbigstore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starbigstore.data.Product

@Composable
fun ProductCard(product: Product, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        onClick = onClick,
        // Efecto cristal: fondo semi-transparente
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                color = Color.White.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("IMG", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                color = Color.White
            )
            Text(
                text = "$${product.priceUsd}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary // Usando el dorado de la marca
            )
        }
    }
}
