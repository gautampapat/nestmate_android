package com.nestmate.app.data.model

data class RoommateConnection(
    val id: String = "",
    val requesterId: String = "",
    val receiverId: String = "",
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val chatId: String? = null,
    val note: String = "",
    val connectedUserName: String = "",
    val connectedUserPhotoUrl: String? = null,
    val connectedUserCollege: String = "",
) {
    companion object {
        fun canonicalId(a: String, b: String): String =
            if (a < b) "${a}_${b}" else "${b}_${a}"
    }
}
