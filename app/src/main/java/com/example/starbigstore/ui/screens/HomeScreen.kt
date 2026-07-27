package com.example.starbigstore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starbigstore.data.Product
import com.example.starbigstore.ui.components.ProductCard
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var products by remember { mutableStateOf(listOf<Product>()) }
    val db = FirebaseFirestore.getInstance()

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
                        stock = doc.getLong("stock")?.toInt() ?: 0
                    )
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Catálogo Premium",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                ProductCard(product = product)
            }
        }
    }
}
