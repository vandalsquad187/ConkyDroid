package com.conkydroid.data.cpu

import com.conkydroid.data.DataProvider
import java.io.RandomAccessFile

class CpuProvider : DataProvider {

    override val name = "cpu"
    override val intervalMs: Long = 500

    private var prevIdle = 0L
    private var prevTotal = 0L

    override suspend fun read(): Map<String, String> {
        val stats = readCpuStats()
        val deltaIdle = stats.idle - prevIdle
        val deltaTotal = stats.total - prevTotal
        val usage = if (deltaTotal > 0) {
            ((1f - deltaIdle.toFloat() / deltaTotal) * 100).coerceIn(0f, 100f)
        } else 0f

        prevIdle = stats.idle
        prevTotal = stats.total

        val freq = readCpuFreq()

        return mapOf(
            "cpu_usage" to "%.1f".format(usage),
            "cpu_raw" to java.lang.String.format(java.util.Locale.US, "%.2f", usage / 100f),
            "cpu_freq" to "${freq}MHz",
        )
    }

    private data class CpuStats(val idle: Long, val total: Long)

    private fun readCpuStats(): CpuStats {
        return try {
            val line = RandomAccessFile("/proc/stat", "r").use { it.readLine() } ?: return CpuStats(0, 0)
            val parts = line.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return CpuStats(0, 0)
            val idle = parts[3] + (parts.getOrNull(4) ?: 0L)
            val total = parts.sum()
            CpuStats(idle, total)
        } catch (_: Exception) {
            CpuStats(0, 0)
        }
    }

    private fun readCpuFreq(): Int {
        return try {
            val raw = RandomAccessFile(
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r"
            ).use { it.readLine() } ?: "0"
            raw.trim().toIntOrNull()?.let { it / 1000 } ?: 0
        } catch (_: Exception) { 0 }
    }
}
