package com.example.starbigstore.data

data class Product(
    val id: String = "",
    val name: String = "",
    val priceUsd: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val collection: String = "",
    val imageUrl: String = "",
    val stock: Int = 0,
    val allowCredit: Boolean = false
)
