package com.conkydroid.data.memory

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.conkydroid.data.DataProvider

class MemoryProvider(private val context: Context) : DataProvider {

    override val name = "memory"
    override val intervalMs: Long = 2000

    override suspend fun read(): Map<String, String> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return emptyMap()
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val totalMb = mi.totalMem / 1024 / 1024
        val availMb = mi.availMem / 1024 / 1024
        val usedMb = totalMb - availMb
        val pct = if (totalMb > 0) usedMb.toFloat() / totalMb * 100 else 0f

        return mapOf(
            "mem_usage" to "%.1f".format(pct),
            "mem_raw" to java.lang.String.format(java.util.Locale.US, "%.2f", pct / 100f),
            "mem_used" to "${usedMb}MB",
            "mem_total" to "${totalMb}MB",
            "mem_avail" to "${availMb}MB",
        )
    }
}
