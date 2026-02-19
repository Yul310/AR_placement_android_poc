package com.willitfit.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    val id: String,
    val name: String,
    val category: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("allow_rotate") val allowRotate: Boolean = false,
    @SerializedName("image_url") val imageUrl: String? = null
) {
    val canMountOnWall: Boolean
        get() = category.equals("TV", ignoreCase = true)

    val dimensionsText: String
        get() = "${widthCm.toInt()} x ${heightCm.toInt()} x ${depthCm.toInt()} cm"

    companion object {
        fun sample() = Product(
            id = "sample",
            name = "Sample Product",
            category = "Appliance",
            widthCm = 60.0,
            heightCm = 85.0,
            depthCm = 60.0
        )
    }
}
