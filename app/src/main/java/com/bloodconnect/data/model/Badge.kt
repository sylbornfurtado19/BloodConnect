package com.bloodconnect.data.model

/**
 * Donor achievement badges earned through confirmed blood donations and streaks.
 */
enum class Badge(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    FIRST_DROP("First Drop", "Make your 1st confirmed donation", "🩸"),
    HELPING_HAND("Helping Hand", "Complete 3 confirmed donations", "🤝"),
    LIFESAVER("Lifesaver", "Complete 5 confirmed donations", "⭐"),
    HERO("Hero", "Complete 10 confirmed donations", "🦸"),
    LEGEND("Legend", "Complete 25 confirmed donations", "🏆"),
    ON_A_ROLL("On a Roll", "Maintain a donation streak of 3 or more", "🔥");

    companion object {
        fun earnedBadges(donationCount: Int, streak: Int): List<Badge> {
            val list = mutableListOf<Badge>()
            if (donationCount >= 1) list.add(FIRST_DROP)
            if (donationCount >= 3) list.add(HELPING_HAND)
            if (donationCount >= 5) list.add(LIFESAVER)
            if (donationCount >= 10) list.add(HERO)
            if (donationCount >= 25) list.add(LEGEND)
            if (streak >= 3) list.add(ON_A_ROLL)
            return list
        }
    }
}
