package com.example.starbigstore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starbigstore.data.Product
import com.example.starbigstore.ui.components.ProductCard

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val sampleProducts = listOf(
        Product(1, "Camiseta Star", 25.0, "Camiseta de algodón"),
        Product(2, "Pantalón Big", 45.0, "Pantalón denim"),
        Product(3, "Gorra Store", 15.0, "Gorra ajustable"),
        Product(4, "Zapatillas Runner", 85.0, "Para correr"),
        Product(5, "Chaqueta Pro", 120.0, "Chaqueta técnica"),
        Product(6, "Reloj Digital", 55.0, "Resistente al agua")
    )

    // Eliminamos el padding extra del Column para que el grid use el espacio de Scaffold
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
            items(sampleProducts) { product ->
                ProductCard(product = product)
            }
        }
    }
}
