package com.nestmate.app.data.model

enum class InquiryStatus { UNREAD, READ, RESPONDED }

/**
 * Inquiry document stored at inquiries/{inquiryId}.
 * Created by a student on any ProviderListing detail screen.
 * Provider reads from this collection in their inbox.
 */
data class Inquiry(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingType: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentPhone: String = "",
    val providerId: String = "",
    val message: String = "",
    val status: String = InquiryStatus.UNREAD.name,
    val chatId: String? = null,          // set after first reply creates the chat thread
    val createdAt: Long = 0L,
) {
    val isUnread: Boolean get() = status == InquiryStatus.UNREAD.name
}
