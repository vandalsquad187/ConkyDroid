package com.conkydroid.data

import android.content.Context
import com.conkydroid.data.battery.BatteryProvider
import com.conkydroid.data.clock.ClockProvider
import com.conkydroid.data.cpu.CpuProvider
import com.conkydroid.data.memory.MemoryProvider
import com.conkydroid.data.network.NetworkProvider
import com.conkydroid.data.storage.StorageProvider
import com.conkydroid.data.weather.WeatherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DataEngine(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    val weatherProvider = WeatherProvider()

    private val providers: List<DataProvider> = listOf(
        ClockProvider(),
        CpuProvider(),
        MemoryProvider(context),
        BatteryProvider(context),
        NetworkProvider(),
        StorageProvider(),
        weatherProvider,
        NotificationProvider(),
    )

    private val _data = MutableStateFlow(emptyMap<String, String>())
    val data: StateFlow<Map<String, String>> = _data.asStateFlow()

    private val lastSnapshot = mutableMapOf<String, String>()

    fun start() {
        if (job?.isActive == true) return
        var iteration = 0L
        job = scope.launch {
            while (isActive) {
                val tick = if (com.conkydroid.theme.ThemeHolder.batterySaver) 5000L else 500L
                var changed = false
                for (p in providers) {
                    if (iteration % (p.intervalMs / tick.coerceAtMost(p.intervalMs)) == 0L) {
                        try {
                            val result = p.read()
                            for ((k, v) in result) {
                                if (lastSnapshot[k] != v) {
                                    lastSnapshot[k] = v
                                    changed = true
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                if (changed) {
                    _data.value = lastSnapshot.toMap()
                }
                delay(tick)
                iteration++
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
