package com.bloodconnect.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StockLevelTest {

    @Test
    fun stockLevel_nullCount_returnsUnknown() {
        assertEquals(StockLevel.UNKNOWN, stockLevel(null))
    }

    @Test
    fun stockLevel_countBelowFive_returnsLow() {
        assertEquals(StockLevel.LOW, stockLevel(0))
        assertEquals(StockLevel.LOW, stockLevel(1))
        assertEquals(StockLevel.LOW, stockLevel(2))
        assertEquals(StockLevel.LOW, stockLevel(3))
        assertEquals(StockLevel.LOW, stockLevel(4))
        assertEquals(StockLevel.LOW, stockLevel(-1))
    }

    @Test
    fun stockLevel_countFiveOrGreater_returnsOk() {
        assertEquals(StockLevel.OK, stockLevel(5))
        assertEquals(StockLevel.OK, stockLevel(6))
        assertEquals(StockLevel.OK, stockLevel(10))
        assertEquals(StockLevel.OK, stockLevel(50))
    }
}
