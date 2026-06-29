package com.conkydroid.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.conkydroid.data.DataProvider

class BatteryProvider(private val context: Context) : DataProvider {

    override val name = "battery"
    override val intervalMs: Long = 5000

    override suspend fun read(): Map<String, String> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) return emptyMap()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (scale > 0) level.toFloat() / scale * 100 else 0f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging"
            else -> "unknown"
        }

        val plugText = when {
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> ""
        }

        val tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        return mapOf(
            "battery" to "%.0f".format(pct),
            "battery_raw" to java.lang.String.format(java.util.Locale.US, "%.2f", pct / 100f),
            "battery_status" to statusText,
            "battery_plugged" to plugText,
            "battery_temp" to "%.1f°C".format(tempC),
        )
    }
}
