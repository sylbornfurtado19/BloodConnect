package com.bloodconnect.utils

import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.Urgency
import org.junit.Assert.assertNotNull
import org.junit.Test

class AnalyticsHelperTest {

    @Test
    fun analyticsHelper_canBeInstantiatedAndMethodsInvokedWithoutExceptions() {
        val helper = AnalyticsHelper()
        assertNotNull(helper)

        // Verify calling all analytics helper methods succeeds cleanly
        helper.logRequestCreated("O+", "Urgent")
        helper.logRequestCreated(BloodGroup.O_POSITIVE, Urgency.URGENT)
        helper.logPledgeMade()
        helper.logPledgeWithdrawn()
        helper.logDonationConfirmed()
    }
}
