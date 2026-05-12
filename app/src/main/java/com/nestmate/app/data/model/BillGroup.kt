package com.nestmate.app.data.model

data class BillGroup(
    val id: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String? = null,
    val totalAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val serviceChargeAmount: Long = 0L,
    val tipAmount: Long = 0L,
    val currency: String = "INR",
    val exchangeRate: Float = 1f,
    val splitMethod: SplitMethod = SplitMethod.EQUAL,
    val participants: List<Participant> = emptyList(),
    val items: List<BillItem> = emptyList(),
    val isSettled: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class SplitMethod {
    EQUAL,
    CUSTOM,
    ITEM_WISE,
}

enum class PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
}

data class Participant(
    val id: String = "",
    val name: String = "",
    val shareAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
)

data class BillItem(
    val id: String = "",
    val name: String = "",
    val price: Long = 0L,
    val assignedParticipantIds: List<String> = emptyList(),
)

data class SettlementTransfer(
    val fromParticipantId: String,
    val fromName: String,
    val toParticipantId: String,
    val toName: String,
    val amount: Long,
)
