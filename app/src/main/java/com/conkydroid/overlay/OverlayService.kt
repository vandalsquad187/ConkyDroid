package com.conkydroid.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import com.conkydroid.theme.ThemeHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var blocklistJob: Job? = null
    private var lastForegroundPkg: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        scope.launch {
            ThemeHolder.editMode.collectLatest { editing ->
                val v = overlayView ?: return@collectLatest
                val lp = layoutParams ?: return@collectLatest
                try {
                    if (editing) {
                        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    } else {
                        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    }
                    windowManager.updateViewLayout(v, lp)
                    v.alpha = if (editing) 1f else ThemeHolder.globalAlpha
                } catch (_: Exception) { }
                try { updateNotification(editing) } catch (_: Exception) { }
            }
        }

        blocklistJob = scope.launch {
            val prefs = applicationContext.getSharedPreferences("conkydroid", MODE_PRIVATE)
            while (isActive) {
                val blocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
                if (blocked.isNotEmpty()) {
                    val fg = getForegroundPackage()
                    if (fg != null) lastForegroundPkg = fg
                    val checkPkg = fg ?: lastForegroundPkg
                    val shouldHide = checkPkg != null && blocked.contains(checkPkg)
                    overlayView?.let { v ->
                        v.visibility = if (shouldHide) View.GONE else View.VISIBLE
                    }
                } else {
                    overlayView?.let { v -> v.visibility = View.VISIBLE }
                }
                delay(2000)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 10000
        return try {
            val events = usm.queryEvents(start, end)
            var current: String? = null
            val e = UsageEvents.Event()
            while (events.getNextEvent(e)) {
                if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    current = e.packageName
                }
            }
            current
        } catch (_: SecurityException) {
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            try {
                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )
                layoutParams = lp
                overlayView = OverlayView(this)
                windowManager.addView(overlayView, lp)
            } catch (_: Exception) { }
        }

        try { startForeground(1, buildNotification(ThemeHolder.editMode.value)) } catch (_: Exception) { }
        return START_STICKY
    }

    override fun onDestroy() {
        blocklistJob?.cancel()
        overlayView?.let { v ->
            try { windowManager.removeView(v) } catch (_: Exception) { }
            v.destroy()
        }
        overlayView = null
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) { }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "ConkyDroid Overlay",
                    NotificationManager.IMPORTANCE_LOW,
                )
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            } catch (_: Exception) { }
        }
    }

    private fun buildNotification(editing: Boolean): Notification {
        val title = if (editing) "ConkyDroid – Edit mode" else "ConkyDroid"
        val text = if (editing) "Exit via ✕ in overlay toolbar" else "Overlay running"
        return Notification.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .build()
    }

    private fun updateNotification(editing: Boolean) {
        startForeground(1, buildNotification(editing))
    }

    companion object {
        private const val CHANNEL_ID = "conkydroid_overlay"
    }
}
