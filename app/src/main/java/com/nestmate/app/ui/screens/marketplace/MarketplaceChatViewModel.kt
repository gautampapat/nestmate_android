package com.nestmate.app.ui.screens.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ChatMessage
import com.nestmate.app.data.model.MarketplaceChat
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketplaceChatViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val threads: StateFlow<List<MarketplaceChat>> =
        (currentUserId?.let { repository.getChatThreads(it) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _chatId = MutableStateFlow<String?>(null)

    val chat: StateFlow<MarketplaceChat?> = _chatId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf<MarketplaceChat?>(null)
            else threads.let { tf ->
                kotlinx.coroutines.flow.flow {
                    tf.collect { list -> emit(list.firstOrNull { it.id == id }) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val messages: StateFlow<List<ChatMessage>> = _chatId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getChatMessages(id)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun bindChat(chatId: String) {
        _chatId.value = chatId
        currentUserId?.let { uid ->
            viewModelScope.launch {
                repository.markChatRead(chatId, uid)
                    .onFailure { /* silent: read markers are best-effort */ }
            }
        }
    }

    fun sendMessage(text: String) {
        val uid = currentUserId ?: return
        val id = _chatId.value ?: return
        viewModelScope.launch {
            val message = ChatMessage(senderId = uid, text = text)
            val result = repository.sendMessage(id, message)
            result.onFailure { _error.value = it.message ?: "Could not send message" }
        }
    }

    fun markItemSoldFromChat(itemId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.markAsSold(itemId)
            result.onFailure { _error.value = it.message ?: "Could not mark sold" }
            onDone(result.isSuccess)
        }
    }

    fun isUnread(thread: MarketplaceChat): Boolean {
        val uid = currentUserId ?: return false
        val lastRead = thread.lastReadBy[uid] ?: 0L
        return thread.lastMessageAt > lastRead
    }

    fun clearError() { _error.value = null }
}
