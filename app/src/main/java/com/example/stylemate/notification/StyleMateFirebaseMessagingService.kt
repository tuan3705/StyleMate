package com.example.stylemate.notification

import android.content.Context
import android.util.Log
import com.example.stylemate.StyleMateApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StyleMateFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("StyleMateFcmService", "onNewToken len=${'$'}{token.length}")
        val app = applicationContext as StyleMateApp
        CoroutineScope(Dispatchers.IO).launch {
            app.fcmRepository.syncFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Thời tiết hôm nay"
        val body = message.notification?.body ?: data["body"] ?: ""
        val weatherCode = data["weatherCode"] ?: ""
        val temp = data["temp"] ?: ""

        if (AppForegroundTracker.isForeground) {
            CoroutineScope(Dispatchers.Default).launch {
                NotificationBus.emit(
                    NotificationBus.InAppNotification(
                        title = title,
                        body = body,
                        weatherCode = weatherCode,
                        temp = temp
                    )
                )
            }
            return
        }

        showSystemNotification(applicationContext, title, body)
    }

    private fun showSystemNotification(context: Context, title: String, body: String) {
        NotificationHelper.showWeatherNotification(context, title, body)
    }
}
