package com.nestmate.app.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val sentAt: Long = 0L,
)
