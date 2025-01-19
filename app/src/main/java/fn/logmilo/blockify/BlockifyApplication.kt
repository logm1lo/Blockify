package fn.logmilo.blockify

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import fn.logmilo.blockify.data.SpamDatabase

class BlockifyApplication : Application() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "blockify_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        SpamDatabase.initialize(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Blockify Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running call screening service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
} 