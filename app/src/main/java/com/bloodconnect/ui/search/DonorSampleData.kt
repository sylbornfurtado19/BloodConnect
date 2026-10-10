package com.bloodconnect.ui.search

/**
 * Fictional sample donor representation for UI demonstration.
 * Note: These do not represent real individuals, phone numbers, or medical records.
 */
data class SampleDonor(
    val id: String,
    val label: String,
    val bloodGroup: String,
    val location: String,
    val distanceKm: Int,
    val status: String,
    val lastDonation: String = "4 months ago",
    val eligibilityStatus: String = "Eligible (Demo)",
    val notes: String = "Fictional demonstration volunteer donor record for MAD project evaluation."
)

val defaultSampleDonors = listOf(
    SampleDonor(
        id = "D1",
        label = "Volunteer Donor #101",
        bloodGroup = "O+",
        location = "Central City District",
        distanceKm = 4,
        status = "Available",
        lastDonation = "3 months ago"
    ),
    SampleDonor(
        id = "D2",
        label = "Volunteer Donor #102",
        bloodGroup = "A+",
        location = "Metro West Medical Park",
        distanceKm = 8,
        status = "Available",
        lastDonation = "5 months ago"
    ),
    SampleDonor(
        id = "D3",
        label = "Volunteer Donor #103",
        bloodGroup = "B+",
        location = "Northside Civic Zone",
        distanceKm = 12,
        status = "Available",
        lastDonation = "2 months ago"
    ),
    SampleDonor(
        id = "D4",
        label = "Volunteer Donor #104",
        bloodGroup = "AB+",
        location = "Downtown Health Center",
        distanceKm = 18,
        status = "Available",
        lastDonation = "6 months ago"
    ),
    SampleDonor(
        id = "D5",
        label = "Volunteer Donor #105",
        bloodGroup = "O-",
        location = "Eastside Community Ward",
        distanceKm = 22,
        status = "Available",
        lastDonation = "4 months ago"
    ),
    SampleDonor(
        id = "D6",
        label = "Volunteer Donor #106",
        bloodGroup = "A-",
        location = "South Bay Regional Sector",
        distanceKm = 28,
        status = "Available",
        lastDonation = "7 months ago"
    ),
    SampleDonor(
        id = "D7",
        label = "Volunteer Donor #107",
        bloodGroup = "B-",
        location = "Highland Suburban Area",
        distanceKm = 35,
        status = "Available",
        lastDonation = "1 month ago"
    ),
    SampleDonor(
        id = "D8",
        label = "Volunteer Donor #108",
        bloodGroup = "AB-",
        location = "University Campus Area",
        distanceKm = 42,
        status = "Available",
        lastDonation = "8 months ago"
    )
)

/**
 * Resolves a donor from the demonstration dataset by their unique ID.
 */
fun getSampleDonorById(donorId: String?): SampleDonor? {
    if (donorId.isNullOrBlank()) return null
    return defaultSampleDonors.find { it.id.equals(donorId, ignoreCase = true) }
}
