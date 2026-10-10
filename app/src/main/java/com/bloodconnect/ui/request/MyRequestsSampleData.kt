package com.bloodconnect.ui.request

/**
 * Fictional demonstration blood request entry representing user's past requests.
 * Note: These are local simulation models for project evaluation and do not represent real patients.
 */
data class MySampleRequest(
    val id: String,
    val bloodGroup: String,
    val unitsNeeded: Int,
    val hospital: String,
    val city: String,
    val urgency: String,
    val status: String,
    val requestDate: String,
    val notes: String = ""
)

/**
 * Centralized demonstration dataset for My Requests history.
 */
val sampleMyRequestsList = listOf(
    MySampleRequest(
        id = "REQ-101",
        bloodGroup = "O+",
        unitsNeeded = 2,
        hospital = "City General Hospital",
        city = "Central Sector",
        urgency = "Critical",
        status = "Pending (Demo)",
        requestDate = "Today, 10:30 AM",
        notes = "Emergency request registered for hospital blood bank verification."
    ),
    MySampleRequest(
        id = "REQ-102",
        bloodGroup = "A-",
        unitsNeeded = 1,
        hospital = "Metro Care Medical Center",
        city = "Westside District",
        urgency = "Urgent",
        status = "Fulfilled (Demo)",
        requestDate = "Yesterday, 3:15 PM",
        notes = "Fulfilled successfully by volunteer donor dispatch in local simulation."
    ),
    MySampleRequest(
        id = "REQ-103",
        bloodGroup = "B+",
        unitsNeeded = 3,
        hospital = "St. Jude Regional Clinic",
        city = "North Valley Ward",
        urgency = "Routine",
        status = "Pending (Demo)",
        requestDate = "Oct 8, 2026",
        notes = "Scheduled surgical requirement queued for donor matching."
    ),
    MySampleRequest(
        id = "REQ-104",
        bloodGroup = "AB+",
        unitsNeeded = 1,
        hospital = "Downtown Health Center",
        city = "Downtown Core",
        urgency = "Urgent",
        status = "Cancelled (Demo)",
        requestDate = "Oct 5, 2026",
        notes = "Request cancelled; patient transferred to specialty trauma hospital."
    ),
    MySampleRequest(
        id = "REQ-105",
        bloodGroup = "O-",
        unitsNeeded = 2,
        hospital = "Eastside Community Hospital",
        city = "Eastside Sector",
        urgency = "Critical",
        status = "Fulfilled (Demo)",
        requestDate = "Sep 28, 2026",
        notes = "Transfusion completed via regional blood cooperative simulation."
    )
)
