package com.bloodconnect.notifications

/**
 * Pure helper object managing a comma-separated string (CSV) of notified request IDs
 * with duplication prevention and maximum entry capacity capping.
 */
object NotifiedRequests {

    fun contains(csv: String, id: String): Boolean {
        if (csv.isBlank() || id.isBlank()) return false
        val list = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return id in list
    }

    fun add(csv: String, id: String, cap: Int = 50): String {
        if (id.isBlank()) return csv
        val list = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != id }.toMutableList()
        list.add(id)
        val trimmed = if (list.size > cap) list.takeLast(cap) else list
        return trimmed.joinToString(",")
    }
}
