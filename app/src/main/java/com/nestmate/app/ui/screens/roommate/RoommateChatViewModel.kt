package com.nestmate.app.ui.screens.roommate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.RoommateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoommateChatViewModel @Inject constructor(
    private val repository: RoommateRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _chatId = MutableStateFlow<String?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _chatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getChatMessages(id)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun bindChat(chatId: String) { _chatId.value = chatId }

    fun sendMessage(text: String) {
        val uid = currentUserId ?: return
        val id = _chatId.value ?: return
        viewModelScope.launch {
            val result = repository.sendChatMessage(id, ChatMessage(senderId = uid, text = text))
            result.onFailure { _error.value = it.message ?: "Could not send message" }
        }
    }

    fun clearError() { _error.value = null }
}
