package com.conkydroid.theme

import android.content.Context
import com.conkydroid.engine.Widget
import java.io.File

class ThemeRepository(private val context: Context) {

    private val loader = ThemeLoader()

    fun loadAll(): List<Theme> {
        val themes = mutableMapOf<String, Theme>()
        for (t in loadBuiltin()) themes[t.name] = t
        for (t in loadUser()) themes[t.name] = t
        return themes.values.toList()
    }

    private fun loadBuiltin(): List<Theme> {
        val list = mutableListOf<Theme>()
        try {
            for (file in context.assets.list("themes")?.filter { it.endsWith(".json") } ?: emptyList()) {
                val json = context.assets.open("themes/$file").bufferedReader().readText()
                loader.parse(json)?.let { list.add(it) }
            }
        } catch (_: Exception) { }
        return list
    }

    private fun loadUser(): List<Theme> {
        val list = mutableListOf<Theme>()
        val dir = getThemesDir()
        if (!dir.exists()) return list
        for (file in dir.listFiles()?.filter { it.extension == "json" } ?: emptyList()) {
            try {
                loader.parse(file.readText())?.let { list.add(it) }
            } catch (_: Exception) { }
        }
        return list
    }

    private fun sanitize(name: String): String {
        return name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "theme" } + ".json"
    }

    fun save(name: String, json: String): Boolean {
        return try {
            val dir = getThemesDir()
            dir.mkdirs()
            File(dir, sanitize(name)).writeText(json)
            true
        } catch (_: Exception) { false }
    }

    fun delete(name: String): Boolean {
        return try {
            val f = File(getThemesDir(), sanitize(name))
            if (f.exists()) f.delete() else false
        } catch (_: Exception) { false }
    }

    fun createNew(name: String): Theme {
        return Theme(
            name = name,
            author = "",
            widgets = listOf(
                Widget.Text(
                    id = "clock", x = 20f, y = 40f,
                    format = "{time}", fontSize = 36f, color = 0xFFFFFFFF.toInt(),
                ),
            ),
        )
    }

    fun duplicate(source: Theme, newName: String): Theme {
        return source.copy(name = newName)
    }

    fun exportJson(theme: Theme): String = loader.toJson(theme)

    fun importJson(json: String): Theme? = loader.parse(json)

    fun isUserTheme(name: String): Boolean {
        val dir = getThemesDir()
        if (!dir.exists()) return false
        return File(dir, sanitize(name)).exists()
    }

    private fun getThemesDir(): File {
        return File(context.filesDir, "themes")
    }
}
