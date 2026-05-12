package com.nestmate.app.data.model

data class ForumPost(
    val postId: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val collegeId: String = "",
    val upvotes: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ForumComment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: Long = 0L,
    val collegeId: String = "",
    val organizerId: String = "",
    val rsvpCount: Int = 0
)
