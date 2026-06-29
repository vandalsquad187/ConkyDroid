package com.conkydroid.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class ConkyRenderer {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true; isDither = true
    }
    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isAntiAlias = true }
    private val barFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isAntiAlias = true }
    private val graphLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val graphFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; isAntiAlias = true
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }

    private val history = mutableMapOf<String, MutableList<Float>>()

    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        density: Float,
        widgets: List<Widget>,
        data: Map<String, String>,
        globalScale: Float = 1f,
        globalColor: Int? = null,
        globalFont: String? = null,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
    ) {
        for (w in widgets.sortedBy { it.zIndex }) {
            when (w) {
                is Widget.Text -> drawText(canvas, w, data, density, globalScale, globalColor, offsetX, offsetY)
                is Widget.Bar -> drawBar(canvas, w, data, density, globalScale, offsetX, offsetY)
                is Widget.Graph -> drawGraph(canvas, w, data, density, globalScale, offsetX, offsetY)
                is Widget.HLine -> drawHLine(canvas, w, density, globalScale, offsetX, offsetY)
                is Widget.VLine -> drawVLine(canvas, w, density, globalScale, offsetX, offsetY)
            }
        }
    }

    fun pushHistory(source: String, value: Float) {
        history.getOrPut(source) { mutableListOf() }.let { list ->
            list.add(value)
            if (list.size > 120) list.removeAt(0)
        }
    }

    private fun dpToPx(dp: Float, density: Float): Float = dp * density

    private fun drawText(
        canvas: Canvas, w: Widget.Text, data: Map<String, String>,
        density: Float, scale: Float, globalColor: Int?,
        ox: Float, oy: Float,
    ) {
        val value = if (w.source != null) data[w.source] ?: "---" else null
        val text = if (value != null) w.format.replace("{value}", value)
        else resolveTemplate(w.format, data)
        textPaint.color = globalColor ?: w.color
        textPaint.textSize = dpToPx(w.fontSize * scale, density)
        val xPx = dpToPx((w.x + ox) * scale, density)
        val yPx = dpToPx((w.y + oy) * scale, density) + textPaint.textSize
        if (w.width > 0f && w.height > 0f) {
            val wPx = dpToPx(w.width * scale, density)
            val hPx = dpToPx(w.height * scale, density)
            val bg = Paint().apply { color = 0x22000000.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(xPx, yPx - textPaint.textSize, xPx + wPx, yPx - textPaint.textSize + hPx, bg)
            canvas.save()
            canvas.clipRect(xPx, yPx - textPaint.textSize, xPx + wPx, yPx - textPaint.textSize + hPx)
            canvas.drawText(text, xPx, yPx, textPaint)
            canvas.restore()
        } else {
            canvas.drawText(text, xPx, yPx, textPaint)
        }
    }

    private fun drawBar(
        canvas: Canvas, w: Widget.Bar, data: Map<String, String>,
        density: Float, scale: Float, ox: Float, oy: Float,
    ) {
        val raw = data[w.source]?.toFloatOrNull() ?: return
        val fraction = (raw / w.max).coerceIn(0f, 1f)
        val xPx = dpToPx((w.x + ox) * scale, density)
        val yPx = dpToPx((w.y + oy) * scale, density)
        val wPx = dpToPx(w.width * scale, density)
        val hPx = dpToPx(w.height * scale, density)
        barBgPaint.color = w.bgColor
        canvas.drawRoundRect(xPx, yPx, xPx + wPx, yPx + hPx, 4f, 4f, barBgPaint)
        barFgPaint.color = w.fgColor
        canvas.drawRoundRect(xPx, yPx, xPx + wPx * fraction, yPx + hPx, 4f, 4f, barFgPaint)
    }

    private fun drawGraph(
        canvas: Canvas, w: Widget.Graph, data: Map<String, String>,
        density: Float, scale: Float, ox: Float, oy: Float,
    ) {
        val raw = data[w.source]?.toFloatOrNull() ?: return
        pushHistory(w.source, raw)
        val points = history[w.source] ?: return
        if (points.size < 2) return
        val xPx = dpToPx((w.x + ox) * scale, density)
        val yPx = dpToPx((w.y + oy) * scale, density)
        val wPx = dpToPx(w.width * scale, density)
        val hPx = dpToPx(w.height * scale, density)
        val visible = points.takeLast(w.historyLength)
        val stepX = wPx / (visible.size - 1).coerceAtLeast(1)
        val path = Path()
        for (i in visible.indices) {
            val px = xPx + i * stepX
            val py = yPx + hPx - (visible[i] / w.max).coerceIn(0f, 1f) * hPx
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        graphLinePaint.color = w.lineColor
        canvas.drawPath(path, graphLinePaint)
        val fillPath = Path(path)
        fillPath.lineTo(xPx + (visible.size - 1) * stepX, yPx + hPx)
        fillPath.lineTo(xPx, yPx + hPx)
        fillPath.close()
        graphFillPaint.color = w.fillColor
        canvas.drawPath(fillPath, graphFillPaint)
    }

    private fun drawHLine(canvas: Canvas, w: Widget.HLine, density: Float, scale: Float, ox: Float, oy: Float) {
        linePaint.color = w.color
        linePaint.strokeWidth = dpToPx(w.strokeWidth * scale, density)
        val xPx = dpToPx((w.x + ox) * scale, density)
        val yPx = dpToPx((w.y + oy) * scale, density)
        val lenPx = dpToPx(w.length * scale, density)
        canvas.drawLine(xPx, yPx, xPx + lenPx, yPx, linePaint)
    }

    private fun drawVLine(canvas: Canvas, w: Widget.VLine, density: Float, scale: Float, ox: Float, oy: Float) {
        linePaint.color = w.color
        linePaint.strokeWidth = dpToPx(w.strokeWidth * scale, density)
        val xPx = dpToPx((w.x + ox) * scale, density)
        val yPx = dpToPx((w.y + oy) * scale, density)
        val lenPx = dpToPx(w.length * scale, density)
        canvas.drawLine(xPx, yPx, xPx, yPx + lenPx, linePaint)
    }

    private fun resolveTemplate(format: String, data: Map<String, String>): String {
        var result = format
        for ((key, value) in data) {
            result = result.replace("{${key}}", value)
        }
        return result
    }
}
