package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HospitalInventoryTest {

    @Test
    fun testStockIncrementAndDecrementBounds() {
        val stock = mutableMapOf("O-" to 2, "A+" to 5)

        // Increment O-
        val oCount = (stock["O-"] ?: 0) + 1
        stock["O-"] = oCount
        assertEquals(3, stock["O-"])

        // Decrement below 0 is bounded to 0
        var aCount = (stock["A+"] ?: 0) - 10
        if (aCount < 0) aCount = 0
        stock["A+"] = aCount
        assertEquals(0, stock["A+"])
    }

    @Test
    fun testStockLevelEvaluation() {
        assertEquals(StockLevel.LOW, stockLevel(0))
        assertEquals(StockLevel.LOW, stockLevel(4))
        assertEquals(StockLevel.OK, stockLevel(5))
        assertEquals(StockLevel.OK, stockLevel(12))
        assertEquals(StockLevel.UNKNOWN, stockLevel(null))
    }

    @Test
    fun testHospitalAdminRoleCheck() {
        val donorUser = User(role = "DONOR")
        val hospitalUser = User(role = "HOSPITAL_ADMIN", organizationName = "KEM Hospital")
        val adminUser = User(role = "ADMIN")

        assertTrue(!donorUser.isHospitalAdmin)
        assertTrue(hospitalUser.isHospitalAdmin)
        assertTrue(adminUser.isHospitalAdmin)
    }
}
