package com.nestmate.app.ui.screens.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.model.Inquiry
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.InquiryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderInquiriesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val inquiryRepository: InquiryRepository,
) : ViewModel() {

    private val providerId: String get() = authRepository.getCurrentUserId() ?: ""

    val inquiries: StateFlow<List<Inquiry>> = run {
        if (providerId.isBlank()) flowOf(emptyList())
        else inquiryRepository.getInquiriesForProvider(providerId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Per-inquiry chat state ──────────────────────────────────────────────

    private val _currentInquiry = MutableStateFlow<Inquiry?>(null)
    val currentInquiry: StateFlow<Inquiry?> = _currentInquiry.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatId = MutableStateFlow<String?>(null)
    val chatId: StateFlow<String?> = _chatId.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    fun loadInquiry(inquiryId: String) {
        viewModelScope.launch {
            val inquiry = inquiryRepository.getInquiryById(inquiryId).getOrNull() ?: return@launch
            _currentInquiry.value = inquiry

            val cid = inquiry.chatId ?: inquiryRepository.getOrCreateChat(
                inquiryId = inquiryId,
                providerId = providerId,
                studentId = inquiry.studentId,
            ).getOrNull() ?: return@launch

            _chatId.value = cid

            inquiryRepository.getChatMessages(cid).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun sendMessage(text: String) {
        val cid = _chatId.value ?: return
        val inquiry = _currentInquiry.value ?: return
        _isSendingMessage.value = true
        viewModelScope.launch {
            val message = ChatMessage(
                senderId = providerId,
                text = text,
                sentAt = System.currentTimeMillis(),
            )
            inquiryRepository.sendMessage(cid, message)
            inquiryRepository.markAsResponded(inquiry.id)
            _isSendingMessage.value = false
        }
    }

    fun markAsRead(inquiryId: String) {
        viewModelScope.launch { inquiryRepository.markAsRead(inquiryId) }
    }
}
