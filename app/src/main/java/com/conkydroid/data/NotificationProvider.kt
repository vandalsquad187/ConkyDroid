package com.conkydroid.data

class NotificationProvider : DataProvider {
    override val name: String = "notifications"
    override val intervalMs: Long = 1000L
    override suspend fun read(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val n = NotificationData.latestNotif
        if (n != null) {
            map["notif_title"] = n.title
            map["notif_text"] = n.text
            map["notif_pkg"] = n.pkg
        }
        map["media_title"] = NotificationData.mediaTitle.ifEmpty { "-" }
        map["media_artist"] = NotificationData.mediaArtist.ifEmpty { "-" }
        map["media_album"] = NotificationData.mediaAlbum.ifEmpty { "-" }
        return map
    }
}
