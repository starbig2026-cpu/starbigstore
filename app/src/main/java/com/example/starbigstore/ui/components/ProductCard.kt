package com.example.starbigstore.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.starbigstore.R
import com.example.starbigstore.data.Product

@Composable
fun ProductCard(product: Product, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        onClick = onClick,
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
                modifier = Modifier.size(110.dp),
                color = Color.White.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.medium
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fixDriveUrl(product.imageUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.logo_admin),
                    placeholder = painterResource(id = R.drawable.logo_admin)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = product.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                color = Color.White
            )
            Text(
                text = "$${product.priceUsd}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC5A059),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun fixDriveUrl(url: String?): String {
    if (url.isNullOrBlank() || url.contains("subiendo")) return "https://via.placeholder.com/200?text=STARBIG"
    if (url.contains("firebasestorage.googleapis.com") || url.contains("appspot.com")) return url
    if (url.contains("drive.google.com/uc") || url.contains("lh3.googleusercontent.com/d/")) return url

    val id = when {
        url.contains("id=") -> url.split("id=").getOrNull(1)?.split("&")?.getOrNull(0)
        url.contains("file/d/") -> url.split("file/d/").getOrNull(1)?.split("/")?.getOrNull(0)
        url.length > 20 && !url.contains("/") && !url.contains(".") -> url
        else -> null
    }
    return id?.let { "https://drive.google.com/uc?export=view&id=$it" } ?: url
}
