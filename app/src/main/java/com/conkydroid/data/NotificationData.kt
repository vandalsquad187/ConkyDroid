package com.conkydroid.data

data class NotifEntry(
    val pkg: String,
    val title: String,
    val text: String,
    val isMedia: Boolean,
)

object NotificationData {
    @Volatile var latestNotif: NotifEntry? = null
    @Volatile var mediaTitle: String = ""
    @Volatile var mediaArtist: String = ""
    @Volatile var mediaAlbum: String = ""
}
