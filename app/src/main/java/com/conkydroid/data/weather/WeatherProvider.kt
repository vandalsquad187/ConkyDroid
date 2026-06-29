package com.conkydroid.data.weather

import com.conkydroid.data.DataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WeatherProvider : DataProvider {

    override val name: String = "weather"
    override val intervalMs: Long = 300_000L

    companion object {
        @Volatile var latitude: Double = 51.5
        @Volatile var longitude: Double = -0.12
    }

    override suspend fun read(): Map<String, String> {
        return try {
            withContext(Dispatchers.IO) {
                val url = URL("https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&timezone=auto")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = reader.readText()
                reader.close()
                conn.disconnect()

                val root = org.json.JSONObject(json)
                val current = root.optJSONObject("current") ?: return@withContext emptyMap()
                val code = current.optInt("weather_code", 0)

                mapOf(
                    "weather_temp" to format1(current.optDouble("temperature_2m", 0.0)),
                    "weather_feels" to format1(current.optDouble("apparent_temperature", 0.0)),
                    "weather_humidity" to format1(current.optDouble("relative_humidity_2m", 0.0)),
                    "weather_wind" to format1(current.optDouble("wind_speed_10m", 0.0)),
                    "weather_code" to code.toString(),
                    "weather_icon" to weatherEmoji(code),
                    "weather_label" to weatherLabel(code),
                )
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun format1(v: Double) = java.lang.String.format(java.util.Locale.US, "%.1f", v)

    private fun weatherEmoji(code: Int): String = when {
        code == 0 -> "☀️"
        code <= 3 -> "⛅"
        code <= 48 -> "🌫️"
        code <= 57 -> "🌧️"
        code <= 67 -> "🌧️"
        code <= 77 -> "❄️"
        code <= 82 -> "🌦️"
        code <= 86 -> "🌨️"
        else -> "⛈️"
    }

    private fun weatherLabel(code: Int): String = when {
        code == 0 -> "Clear"
        code <= 3 -> "Cloudy"
        code <= 48 -> "Fog"
        code <= 57 -> "Drizzle"
        code <= 67 -> "Rain"
        code <= 77 -> "Snow"
        code <= 82 -> "Showers"
        code <= 86 -> "Snow"
        else -> "Thunder"
    }
}
