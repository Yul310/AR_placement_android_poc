package com.willitfit.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.willitfit.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val context: Context) {

    private val gson = Gson()

    suspend fun loadProducts(): List<Product> = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open("products.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<ProductsResponse>() {}.type
            val response: ProductsResponse = gson.fromJson(json, type)
            response.products
        } catch (e: Exception) {
            e.printStackTrace()
            defaultProducts()
        }
    }

    private fun defaultProducts(): List<Product> = listOf(
        Product(
            id = "washer-1",
            name = "Front Load Washer",
            category = "Appliance",
            widthCm = 60.0,
            heightCm = 85.0,
            depthCm = 60.0
        ),
        Product(
            id = "fridge-1",
            name = "French Door Refrigerator",
            category = "Appliance",
            widthCm = 91.0,
            heightCm = 178.0,
            depthCm = 74.0
        ),
        Product(
            id = "sofa-1",
            name = "3-Seater Sofa",
            category = "Furniture",
            widthCm = 220.0,
            heightCm = 85.0,
            depthCm = 95.0
        ),
        Product(
            id = "tv-65",
            name = "65\" OLED TV",
            category = "TV",
            widthCm = 145.0,
            heightCm = 83.0,
            depthCm = 5.0,
            allowRotate = true
        ),
        Product(
            id = "desk-1",
            name = "Standing Desk",
            category = "Furniture",
            widthCm = 160.0,
            heightCm = 72.0,
            depthCm = 80.0
        ),
        Product(
            id = "bookshelf-1",
            name = "Tall Bookshelf",
            category = "Furniture",
            widthCm = 80.0,
            heightCm = 200.0,
            depthCm = 30.0
        )
    )

    private data class ProductsResponse(
        val products: List<Product>
    )
}
