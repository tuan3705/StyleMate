package com.example.stylemate.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationBus {

    data class InAppNotification(
        val title: String,
        val body: String,
        val weatherCode: String,
        val temp: String
    )

    private val _events = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emit(notification: InAppNotification) {
        _events.emit(notification)
    }
}

