package com.conkydroid.data.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.conkydroid.data.DataProvider
import android.net.TrafficStats

class NetworkProvider : DataProvider {

    override val name = "network"
    override val intervalMs: Long = 3000

    private var prevRx = 0L
    private var prevTx = 0L
    private var lastRead = 0L

    override suspend fun read(): Map<String, String> {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = System.currentTimeMillis()

        val rxSpeed = if (prevRx > 0 && lastRead > 0) {
            ((rx - prevRx).toFloat() / ((now - lastRead) / 1000f)).toLong()
        } else 0L
        val txSpeed = if (prevTx > 0 && lastRead > 0) {
            ((tx - prevTx).toFloat() / ((now - lastRead) / 1000f)).toLong()
        } else 0L

        prevRx = rx
        prevTx = tx
        lastRead = now

        return mapOf(
            "net_rx" to formatBytes(rxSpeed),
            "net_tx" to formatBytes(txSpeed),
            "net_rx_raw" to rxSpeed.toString(),
            "net_tx_raw" to txSpeed.toString(),
        )
    }

    private fun formatBytes(bytesPerSec: Long): String {
        return when {
            bytesPerSec > 1_000_000 -> "%.1f MB/s".format(bytesPerSec / 1_000_000f)
            bytesPerSec > 1_000 -> "%.0f KB/s".format(bytesPerSec / 1_000f)
            else -> "${bytesPerSec} B/s"
        }
    }
}
