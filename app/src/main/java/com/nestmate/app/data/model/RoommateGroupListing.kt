package com.nestmate.app.data.model

data class RoommateGroupListing(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val flatDescription: String = "",
    val location: String = "",
    val rent: Long = 0L,
    val spotsNeeded: Int = 0,
    val spotsConfirmed: Int = 0,
    val memberIds: List<String> = emptyList(),
    val pendingRequestIds: List<String> = emptyList(),
    val status: GroupStatus = GroupStatus.OPEN,
    val createdAt: Long = 0L,
)
