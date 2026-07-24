package com.example.starbigstore.data

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String,
    val imageRes: Int? = null // Para recursos locales por ahora
)
