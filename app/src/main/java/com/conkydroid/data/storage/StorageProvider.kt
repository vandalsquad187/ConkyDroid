package com.conkydroid.data.storage

import android.os.Environment
import android.os.StatFs
import com.conkydroid.data.DataProvider

class StorageProvider : DataProvider {

    override val name = "storage"
    override val intervalMs: Long = 10000

    override suspend fun read(): Map<String, String> {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong

        val totalGb = totalBlocks * blockSize / 1_000_000_000f
        val availGb = availBlocks * blockSize / 1_000_000_000f
        val usedGb = totalGb - availGb
        val pct = if (totalGb > 0) usedGb / totalGb * 100 else 0f

        return mapOf(
            "storage_usage" to "%.1f".format(pct),
            "storage_raw" to java.lang.String.format(java.util.Locale.US, "%.2f", pct / 100f),
            "storage_used" to "%.1f GB".format(usedGb),
            "storage_total" to "%.1f GB".format(totalGb),
            "storage_avail" to "%.1f GB".format(availGb),
        )
    }
}
