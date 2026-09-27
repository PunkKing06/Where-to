package com.example.whereto

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.provider.Settings

/** Exposes the currently active player only after the user enables Notification Access. */
class MediaControlService : NotificationListenerService() {
    companion object {
        fun hasNotificationAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return enabled.contains(ComponentName(context, MediaControlService::class.java).flattenToString())
        }

        fun activeController(context: Context): MediaController? = try {
            val manager = context.getSystemService(MediaSessionManager::class.java)
            manager.getActiveSessions(ComponentName(context, MediaControlService::class.java)).firstOrNull()
        } catch (_: SecurityException) {
            null
        }
    }
}
