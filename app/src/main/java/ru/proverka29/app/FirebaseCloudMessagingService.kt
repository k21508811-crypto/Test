package ru.proverka29.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseCloudMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Проверка.29"
        val body = message.notification?.body ?: message.data["body"] ?: "Новое уведомление"
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "proverka29_updates"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Уведомления Проверка.29", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, SplashActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher).setContentTitle(title).setContentText(body)
            .setAutoCancel(true).setContentIntent(open).build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
