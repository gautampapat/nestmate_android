package com.nestmate.app.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nestmate.app.data.model.Event
import com.nestmate.app.data.model.ForumPost
import com.nestmate.app.data.repository.CommunityRepository
import com.nestmate.app.utils.FirebaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ForumState {
    object Loading : ForumState()
    data class Success(val posts: List<ForumPost>) : ForumState()
    data class Error(val message: String) : ForumState()
}

sealed class EventsState {
    object Loading : EventsState()
    data class Success(val events: List<Event>) : EventsState()
    data class Error(val message: String) : EventsState()
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _forumState = MutableStateFlow<ForumState>(ForumState.Loading)
    val forumState: StateFlow<ForumState> = _forumState.asStateFlow()

    private val _eventsState = MutableStateFlow<EventsState>(EventsState.Loading)
    val eventsState: StateFlow<EventsState> = _eventsState.asStateFlow()

    init {
        // Collect the real-time Flow — posts update automatically when Firestore changes
        viewModelScope.launch {
            communityRepository.getForumPostsFlow().collect { posts ->
                _forumState.value = ForumState.Success(posts)
            }
        }
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _eventsState.value = EventsState.Loading
            val result = communityRepository.getEvents()
            _eventsState.value = if (result.isSuccess)
                EventsState.Success(result.getOrNull() ?: emptyList())
            else EventsState.Error("Could not load events.")
        }
    }

    fun upvotePost(postId: String) {
        viewModelScope.launch {
            // Optimistic update
            if (_forumState.value is ForumState.Success) {
                val current = (_forumState.value as ForumState.Success).posts.toMutableList()
                val idx = current.indexOfFirst { it.postId == postId }
                if (idx != -1) {
                    current[idx] = current[idx].copy(upvotes = current[idx].upvotes + 1)
                    _forumState.value = ForumState.Success(current)
                }
            }
            communityRepository.upvotePost(postId)
        }
    }

    /**
     * Writes the post to Firestore. The real-time listener in init{} will
     * automatically update forumState so no manual refresh needed.
     */
    fun addPost(title: String, content: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            val userName = auth.currentUser?.displayName ?: "Student"
            val post = ForumPost(
                authorId = userId,
                authorName = userName,
                title = title,
                content = content,
                collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID
            )
            communityRepository.addPost(post)
            // No manual refresh needed — Firestore listener triggers automatically
        }
    }
}
