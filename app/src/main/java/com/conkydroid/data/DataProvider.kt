package com.conkydroid.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DataProvider {
    val name: String
    val intervalMs: Long
    suspend fun read(): Map<String, String>

    fun observe(): Flow<Map<String, String>> = flow {
        while (true) {
            emit(read())
            delay(intervalMs)
        }
    }
}
