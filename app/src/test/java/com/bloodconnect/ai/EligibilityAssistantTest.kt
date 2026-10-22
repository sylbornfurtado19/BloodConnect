package com.bloodconnect.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EligibilityAssistantTest {

    @Test
    fun trimHistory_moreThanTenTurns_returnsLastTenTurns() {
        val history = (1..15).map { i ->
            ChatTurn(user = "Question $i", assistant = "Answer $i")
        }

        val trimmed = trimHistory(history, maxTurns = 10)

        assertEquals(10, trimmed.size)
        // Should contain items 6 to 15 in order
        assertEquals("Question 6", trimmed.first().user)
        assertEquals("Question 15", trimmed.last().user)
        assertEquals(history.takeLast(10), trimmed)
    }

    @Test
    fun trimHistory_exactlyTenTurns_returnsAllTen() {
        val history = (1..10).map { i ->
            ChatTurn(user = "Question $i", assistant = "Answer $i")
        }

        val trimmed = trimHistory(history, maxTurns = 10)

        assertEquals(10, trimmed.size)
        assertEquals("Question 1", trimmed.first().user)
        assertEquals("Question 10", trimmed.last().user)
    }

    @Test
    fun trimHistory_fewerThanTenTurns_returnsAllTurns() {
        val history = listOf(
            ChatTurn(user = "Can I donate after a cold?", assistant = "Wait until fully recovered."),
            ChatTurn(user = "What to eat before?", assistant = "Drink water and have a light meal.")
        )

        val trimmed = trimHistory(history, maxTurns = 10)

        assertEquals(2, trimmed.size)
        assertEquals(history, trimmed)
    }

    @Test
    fun trimHistory_emptyList_returnsEmptyList() {
        val trimmed = trimHistory(emptyList(), maxTurns = 10)
        assertTrue(trimmed.isEmpty())
    }

    @Test
    fun trimHistory_customMaxTurns_respectsLimit() {
        val history = (1..5).map { i -> ChatTurn("Q$i", "A$i") }
        val trimmed = trimHistory(history, maxTurns = 3)
        assertEquals(3, trimmed.size)
        assertEquals("Q3", trimmed[0].user)
        assertEquals("Q4", trimmed[1].user)
        assertEquals("Q5", trimmed[2].user)
    }
}
