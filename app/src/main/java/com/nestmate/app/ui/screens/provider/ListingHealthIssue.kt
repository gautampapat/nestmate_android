package com.nestmate.app.ui.screens.provider

data class ListingHealthIssue(
    val listingId: String,
    val listingTitle: String,
    val issueType: IssueType
)

enum class IssueType { NO_PHOTOS, NO_PRICE_SET, PAUSED_TOO_LONG, LOW_INQUIRY_RATE }
