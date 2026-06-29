package com.conkydroid.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Build
import android.app.Notification
import com.conkydroid.data.NotificationData
import com.conkydroid.data.NotifEntry

class ConkyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notif = sbn.notification ?: return
        val pkg = sbn.packageName
        val extras = notif.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT, "") ?: ""

        val isMedia = notif.category == Notification.CATEGORY_TRANSPORT
                || notif.category == Notification.CATEGORY_SERVICE
                || notif.isMediaNotification()

        if (isMedia) {
            val artist = extras.getString(Notification.EXTRA_SUB_TEXT, "") ?: ""
            NotificationData.mediaTitle = title
            NotificationData.mediaArtist = artist
            NotificationData.mediaAlbum = extras.getString(Notification.EXTRA_INFO_TEXT, "") ?: ""
        }

        NotificationData.latestNotif = NotifEntry(pkg, title, text, isMedia)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val notif = sbn.notification
        if (notif != null && (notif.category == Notification.CATEGORY_TRANSPORT || notif.isMediaNotification())) {
            NotificationData.mediaTitle = ""
            NotificationData.mediaArtist = ""
            NotificationData.mediaAlbum = ""
        }
        val current = NotificationData.latestNotif
        if (current != null && current.pkg == pkg) {
            NotificationData.latestNotif = null
        }
    }

    private fun Notification.isMediaNotification(): Boolean {
        return extras?.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                || category == Notification.CATEGORY_TRANSPORT
    }

    override fun onListenerConnected() {
        for (sbn in activeNotifications) {
            onNotificationPosted(sbn)
        }
    }
}
