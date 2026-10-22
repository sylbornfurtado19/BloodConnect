package com.bloodconnect.ui.trends

import com.bloodconnect.data.model.BloodRequest

enum class Direction {
    RISING,
    FALLING,
    STABLE
}

data class GroupTrend(
    val group: String,
    val last7: Int,
    val previous7: Int,
    val direction: Direction
)

val STANDARD_BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val SEVEN_DAYS_MILLIS = 7L * DAY_MILLIS
private const val FOURTEEN_DAYS_MILLIS = 14L * DAY_MILLIS

/**
 * Computes demand trends for the 8 standard blood groups over a 14-day window.
 * Counts total units needed (not requests) in the last 7 days vs the 7 days prior.
 *
 * Direction rules:
 * - RISING if last7 >= previous7 * 1.25 and (last7 - previous7) >= 2
 * - FALLING if last7 <= previous7 * 0.75 and (previous7 - last7) >= 2
 * - Otherwise STABLE
 */
fun computeTrends(
    requests: List<BloodRequest>,
    now: Long = System.currentTimeMillis()
): List<GroupTrend> {
    val last7Start = now - SEVEN_DAYS_MILLIS
    val prev7Start = now - FOURTEEN_DAYS_MILLIS

    val last7Map = mutableMapOf<String, Int>()
    val prev7Map = mutableMapOf<String, Int>()

    STANDARD_BLOOD_GROUPS.forEach { group ->
        last7Map[group] = 0
        prev7Map[group] = 0
    }

    for (req in requests) {
        val group = req.bloodGroup.trim()
        if (group !in last7Map) continue

        val units = if (req.unitsNeeded > 0) req.unitsNeeded else req.unitsRequired.coerceAtLeast(1)
        val created = req.createdAt

        when {
            created in last7Start..now -> {
                last7Map[group] = (last7Map[group] ?: 0) + units
            }
            created in prev7Start until last7Start -> {
                prev7Map[group] = (prev7Map[group] ?: 0) + units
            }
            else -> {
                // Requests older than 14 days or in the future are ignored
            }
        }
    }

    return STANDARD_BLOOD_GROUPS.map { group ->
        val last7 = last7Map[group] ?: 0
        val previous7 = prev7Map[group] ?: 0

        val direction = when {
            last7.toDouble() >= previous7.toDouble() * 1.25 && (last7 - previous7) >= 2 -> Direction.RISING
            last7.toDouble() <= previous7.toDouble() * 0.75 && (previous7 - last7) >= 2 -> Direction.FALLING
            else -> Direction.STABLE
        }

        GroupTrend(
            group = group,
            last7 = last7,
            previous7 = previous7,
            direction = direction
        )
    }
}
