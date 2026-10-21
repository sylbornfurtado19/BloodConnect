package com.bloodconnect.ui.profile

import com.bloodconnect.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class DonationHistoryHelperTest {

    @Test
    fun resolveDonorName_withFullName_returnsFullName() {
        val user = User(
            fullName = "Alice Smith",
            displayName = "Alice"
        )
        assertEquals("Alice Smith", resolveDonorName(user))
    }

    @Test
    fun resolveDonorName_withEmptyFullName_returnsDisplayName() {
        val user = User(
            fullName = "",
            displayName = "Bob Donor"
        )
        assertEquals("Bob Donor", resolveDonorName(user))
    }

    @Test
    fun resolveDonorName_withBlankNames_returnsVolunteerDonor() {
        val user = User(
            fullName = "   ",
            displayName = ""
        )
        assertEquals("Volunteer Donor", resolveDonorName(user))
    }

    @Test
    fun resolveDonorName_withNullUser_returnsVolunteerDonor() {
        assertEquals("Volunteer Donor", resolveDonorName(null))
    }
}
