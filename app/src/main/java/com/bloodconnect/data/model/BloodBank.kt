package com.bloodconnect.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Stock availability level for a specific blood group at a blood bank.
 */
enum class StockLevel {
    LOW,
    OK,
    UNKNOWN
}

/**
 * Pure function determining the [StockLevel] for a given blood unit count.
 * - LOW: count < 5
 * - OK: count >= 5
 * - UNKNOWN: count is null
 */
fun stockLevel(count: Int?): StockLevel {
    return when {
        count == null -> StockLevel.UNKNOWN
        count < 5 -> StockLevel.LOW
        else -> StockLevel.OK
    }
}

/**
 * Represents a blood bank or medical blood storage center with stock information.
 * All fields have defaults so Firestore can deserialize them.
 */
data class BloodBank(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val stock: Map<String, Int> = emptyMap(),
    val updatedAt: Long = 0L
)
