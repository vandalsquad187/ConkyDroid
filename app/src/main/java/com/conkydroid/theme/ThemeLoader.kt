package com.conkydroid.theme

import com.conkydroid.engine.Widget
import org.json.JSONArray
import org.json.JSONObject

class ThemeLoader {

    fun parse(json: String): Theme? {
        return try {
            val root = JSONObject(json)
            val name = root.getString("name")
            val widgets = parseWidgets(root.optJSONArray("widgets") ?: JSONArray())

            Theme(
                name = name,
                author = root.optString("author", ""),
                version = root.optInt("version", 1),
                refreshMs = root.optLong("refreshMs", 1000),
                widgets = widgets,
                globalScale = root.optDouble("globalScale", 1.0).toFloat(),
                globalColor = if (root.has("globalColor")) root.optString("globalColor") else null,
                globalFont = if (root.has("globalFont")) root.optString("globalFont") else null,
            )
        } catch (_: Exception) { null }
    }

    private fun parseWidgets(arr: JSONArray): List<Widget> {
        val result = mutableListOf<Widget>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = obj.optString("type", "text")
            val widget = when (type) {
                "text" -> parseText(obj)
                "bar" -> parseBar(obj)
                "graph" -> parseGraph(obj)
                "hline" -> parseHLine(obj)
                "vline" -> parseVLine(obj)
                else -> null
            }
            if (widget != null) result.add(widget)
        }
        return result
    }

    private fun parseText(obj: JSONObject): Widget.Text {
        return Widget.Text(
            id = obj.optString("id", "text_${obj.hashCode()}"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            zIndex = obj.optInt("zIndex", 0),
            format = obj.optString("format", ""),
            fontSize = obj.optDouble("fontSize", 14.0).toFloat(),
            color = parseColor(obj.optString("color", "#FFFFFF")),
            source = if (obj.has("source")) obj.optString("source") else null,
            width = obj.optDouble("width", 0.0).toFloat(),
            height = obj.optDouble("height", 0.0).toFloat(),
        )
    }

    private fun parseBar(obj: JSONObject): Widget.Bar {
        return Widget.Bar(
            id = obj.optString("id", "bar_${obj.hashCode()}"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            zIndex = obj.optInt("zIndex", 0),
            width = obj.optDouble("width", 100.0).toFloat(),
            height = obj.optDouble("height", 10.0).toFloat(),
            source = obj.optString("source", "cpu"),
            fgColor = parseColor(obj.optString("fgColor", "#00FF88")),
            bgColor = parseColor(obj.optString("bgColor", "#333333")),
            max = obj.optDouble("max", 1.0).toFloat(),
        )
    }

    private fun parseGraph(obj: JSONObject): Widget.Graph {
        return Widget.Graph(
            id = obj.optString("id", "graph_${obj.hashCode()}"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            zIndex = obj.optInt("zIndex", 0),
            width = obj.optDouble("width", 200.0).toFloat(),
            height = obj.optDouble("height", 50.0).toFloat(),
            source = obj.optString("source", "cpu"),
            lineColor = parseColor(obj.optString("lineColor", "#00FF88")),
            fillColor = parseColor(obj.optString("fillColor", "#2200FF88")),
            historyLength = obj.optInt("history", 60),
            max = obj.optDouble("max", 1.0).toFloat(),
        )
    }

    private fun parseHLine(obj: JSONObject): Widget.HLine {
        return Widget.HLine(
            id = obj.optString("id", "hline_${obj.hashCode()}"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            zIndex = obj.optInt("zIndex", 0),
            length = obj.optDouble("length", 200.0).toFloat(),
            color = parseColor(obj.optString("color", "#FFFFFF")),
            strokeWidth = obj.optDouble("strokeWidth", 2.0).toFloat(),
        )
    }

    private fun parseVLine(obj: JSONObject): Widget.VLine {
        return Widget.VLine(
            id = obj.optString("id", "vline_${obj.hashCode()}"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            zIndex = obj.optInt("zIndex", 0),
            length = obj.optDouble("length", 200.0).toFloat(),
            color = parseColor(obj.optString("color", "#FFFFFF")),
            strokeWidth = obj.optDouble("strokeWidth", 2.0).toFloat(),
        )
    }

    private fun parseColor(hex: String): Int {
        return try {
            val clean = hex.removePrefix("#")
            val argb = when (clean.length) {
                3 -> "FF${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
                4 -> "${clean[3]}${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
                6 -> "FF$clean"
                8 -> clean
                else -> "FFFFFFFF"
            }
            android.graphics.Color.parseColor("#$argb")
        } catch (_: Exception) { android.graphics.Color.WHITE }
    }

    fun toJson(theme: Theme): String {
        val root = JSONObject()
        root.put("name", theme.name)
        root.put("author", theme.author)
        root.put("version", theme.version)
        root.put("refreshMs", theme.refreshMs)
        if (theme.globalScale != 1f) root.put("globalScale", theme.globalScale.toDouble())
        if (theme.globalColor != null) root.put("globalColor", theme.globalColor)
        if (theme.globalFont != null) root.put("globalFont", theme.globalFont)

        val arr = JSONArray()
        for (w in theme.widgets) {
            arr.put(widgetToJson(w))
        }
        root.put("widgets", arr)
        return root.toString(2)
    }

    private fun widgetToJson(w: Widget): JSONObject {
        val obj = JSONObject()
        when (w) {
            is Widget.Text -> {
                obj.put("type", "text")
                obj.put("format", w.format)
                obj.put("fontSize", w.fontSize.toDouble())
                obj.put("color", colorToHex(w.color))
                if (w.source != null) obj.put("source", w.source)
                if (w.width > 0f) obj.put("width", w.width.toDouble())
                if (w.height > 0f) obj.put("height", w.height.toDouble())
            }
            is Widget.Bar -> {
                obj.put("type", "bar")
                obj.put("source", w.source)
                obj.put("width", w.width.toDouble())
                obj.put("height", w.height.toDouble())
                obj.put("fgColor", colorToHex(w.fgColor))
                obj.put("bgColor", colorToHex(w.bgColor))
                obj.put("max", w.max.toDouble())
            }
            is Widget.Graph -> {
                obj.put("type", "graph")
                obj.put("source", w.source)
                obj.put("width", w.width.toDouble())
                obj.put("height", w.height.toDouble())
                obj.put("lineColor", colorToHex(w.lineColor))
                obj.put("fillColor", colorToHex(w.fillColor))
                obj.put("history", w.historyLength)
                obj.put("max", w.max.toDouble())
            }
            is Widget.HLine -> {
                obj.put("type", "hline")
                obj.put("length", w.length.toDouble())
                obj.put("color", colorToHex(w.color))
                obj.put("strokeWidth", w.strokeWidth.toDouble())
            }
            is Widget.VLine -> {
                obj.put("type", "vline")
                obj.put("length", w.length.toDouble())
                obj.put("color", colorToHex(w.color))
                obj.put("strokeWidth", w.strokeWidth.toDouble())
            }
        }
        obj.put("x", w.x.toDouble())
        obj.put("y", w.y.toDouble())
        obj.put("zIndex", w.zIndex)
        obj.put("id", w.id)
        return obj
    }

    private fun colorToHex(color: Int): String {
        val a = android.graphics.Color.alpha(color)
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        return if (a == 255) "#%02x%02x%02x".format(r, g, b)
        else "#%02x%02x%02x%02x".format(a, r, g, b)
    }
}
