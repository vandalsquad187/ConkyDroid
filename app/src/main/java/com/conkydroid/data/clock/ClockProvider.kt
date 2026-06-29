package com.conkydroid.data.clock

import com.conkydroid.data.DataProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockProvider : DataProvider {

    override val name = "clock"
    override val intervalMs: Long = 1000

    override suspend fun read(): Map<String, String> {
        val now = Date()
        return mapOf(
            "time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
            "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
            "datetime" to SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(now),
        )
    }
}
