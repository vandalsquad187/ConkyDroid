package com.conkydroid.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.conkydroid.data.DataEngine
import com.conkydroid.engine.ConkyRenderer
import com.conkydroid.engine.Widget
import com.conkydroid.theme.ThemeHolder

private const val HANDLE_RADIUS_DP = 28f
private const val MIN_SIZE_DP = 20f
private const val TOOLBAR_HEIGHT_DP = 40f
private const val GRID_SIZE = 10f
private const val TAG = "ConkyOverlay"

class OverlayView(context: Context) : View(context) {

    private val engine = DataEngine(context)
    private val renderer = ConkyRenderer()
    private val density = context.resources.displayMetrics.density
    private val refreshRunnable = { invalidate() }

    private val boxPaint = Paint().apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f * density
    }
    private val handlePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL
    }
    private val handleStroke = Paint().apply {
        color = 0xFFFF4444.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f * density
    }
    private val labelPaint = Paint().apply {
        color = 0xFFFF4444.toInt(); textSize = 12f * density
    }
    private val resizePaint = Paint().apply {
        color = 0xFFFFFF00.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f * density
    }
    private val toolbarBg = Paint().apply {
        color = 0xCC111111.toInt(); style = Paint.Style.FILL
    }
    private val btnPaint = Paint().apply {
        color = 0xFF333333.toInt(); style = Paint.Style.FILL
    }
    private val btnTextPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt(); textSize = 12f * density
    }
    private val debugPaint = Paint().apply {
        color = 0xFFFF00FF.toInt(); textSize = 14f * density
    }
    private val cornerFill = Paint().apply {
        color = 0x66FF8800.toInt(); style = Paint.Style.FILL
    }

    private var draggedWidgetId: String? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var resizeCorner: String? = null

    private var resizeOrigX = 0f; private var resizeOrigY = 0f; private var resizeOrigW = 0f; private var resizeOrigH = 0f
    private var moveAllDragX = 0f; private var moveAllDragY = 0f

    private val toolbarButtons = mutableListOf<Pair<RectF, () -> Unit>>()
    private var debugText = ""

    private fun snap(v: Float) = kotlin.math.round(v / GRID_SIZE) * GRID_SIZE

    init {
        isClickable = true
        engine.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!ThemeHolder.editMode.value) return false

        val px = event.x; val py = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                debugText = "DOWN $px,$py"

                for ((rect, action) in toolbarButtons) {
                    if (rect.contains(px, py)) { action(); return true }
                }

                if (ThemeHolder.moveAllMode) {
                    moveAllDragX = px; moveAllDragY = py
                    draggedWidgetId = "__all__"
                    debugText = "MOVEALL start ${ThemeHolder.offsetX.toInt()},${ThemeHolder.offsetY.toInt()}"
                    return true
                }

                val widgets = ThemeHolder.widgets.value
                val data = engine.data.value
                val selId = ThemeHolder.selectedWidgetId.value
                val sel = if (selId != null) widgets.find { it.id == selId } else null
                val ox = ThemeHolder.offsetX; val oy = ThemeHolder.offsetY

                if (sel != null) {
                    val r = widgetRectPx(sel, data)
                    val hr = HANDLE_RADIUS_DP * density
                    val corners = listOf(
                        Triple("TL", r.left, r.top),
                        Triple("TR", r.right, r.top),
                        Triple("BL", r.left, r.bottom),
                        Triple("BR", r.right, r.bottom),
                    )
                    for ((name, cx, cy) in corners) {
                        val dx = kotlin.math.abs(px - cx)
                        val dy = kotlin.math.abs(py - cy)
                        if (dx < hr && dy < hr) {
                            resizeCorner = name
                            resizeOrigX = sel.x; resizeOrigY = sel.y
                            resizeOrigW = sel.widgetW(); resizeOrigH = sel.widgetH()
                            draggedWidgetId = sel.id
                            debugText = "CORNER $name ox=${sel.x.toInt()} oy=${sel.y.toInt()} ow=${resizeOrigW.toInt()} oh=${resizeOrigH.toInt()}"
                            return true
                        }
                    }
                }

                for (w in widgets.sortedByDescending { it.zIndex }) {
                    val rect = widgetRectPx(w, data)
                    if (rect.contains(px, py)) {
                        ThemeHolder.selectWidget(w.id)
                        draggedWidgetId = w.id; resizeCorner = null
                        dragOffsetX = px - rect.left; dragOffsetY = py - rect.top
                        debugText = "DRAG ${w.id}"
                        return true
                    }
                }
                ThemeHolder.selectWidget(null)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggedWidgetId == "__all__") {
                    val dx = (px - moveAllDragX) / density
                    val dy = (py - moveAllDragY) / density
                    moveAllDragX = px; moveAllDragY = py
                    ThemeHolder.moveOffset(dx, dy)
                    debugText = "MOVEALL ${ThemeHolder.offsetX.toInt()},${ThemeHolder.offsetY.toInt()}"
                    return true
                }
                val id = draggedWidgetId
                if (id == null) { debugText = "MOVE no id"; return false }

                if (resizeCorner != null) {
                    val w = ThemeHolder.widgets.value.find { it.id == id }
                    if (w == null) { debugText = "MOVE not found $id"; return false }
                    if (w !is Widget.Bar && w !is Widget.Graph && w !is Widget.Text
                        && w !is Widget.HLine && w !is Widget.VLine) return false

                    val xDp = px / density; val yDp = py / density
                    val min = MIN_SIZE_DP
                    var nx = resizeOrigX; var ny = resizeOrigY
                    var nw = resizeOrigW; var nh = resizeOrigH

                    when (resizeCorner) {
                        "BR" -> { nw = (xDp - resizeOrigX).coerceAtLeast(min); nh = (yDp - resizeOrigY).coerceAtLeast(min) }
                        "BL" -> { nw = (resizeOrigX + resizeOrigW - xDp).coerceAtLeast(min); nx = resizeOrigX + resizeOrigW - nw; nh = (yDp - resizeOrigY).coerceAtLeast(min) }
                        "TR" -> { nw = (xDp - resizeOrigX).coerceAtLeast(min); nh = (resizeOrigY + resizeOrigH - yDp).coerceAtLeast(min); ny = resizeOrigY + resizeOrigH - nh }
                        "TL" -> { nw = (resizeOrigX + resizeOrigW - xDp).coerceAtLeast(min); nx = resizeOrigX + resizeOrigW - nw; nh = (resizeOrigY + resizeOrigH - yDp).coerceAtLeast(min); ny = resizeOrigY + resizeOrigH - nh }
                    }
                    debugText = "RESIZE nx=${nx.toInt()} ny=${ny.toInt()} nw=${nw.toInt()} nh=${nh.toInt()}"
                    ThemeHolder.resizeWidget(id, snap(nx), snap(ny), snap(nw), snap(nh))
                } else {
                    val dpX = (px - dragOffsetX) / density
                    val dpY = (py - dragOffsetY) / density
                    ThemeHolder.updateWidgetPosition(id, snap(dpX.coerceAtLeast(0f)), snap(dpY.coerceAtLeast(0f)))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedWidgetId = null; resizeCorner = null; return true
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = engine.data.value
        val widgets = ThemeHolder.widgets.value
        val editMode = ThemeHolder.editMode.value
        val W = width.toFloat(); val H = height.toFloat()

        renderer.render(canvas, W, H, density, widgets, data,
            ThemeHolder.globalScale, ThemeHolder.globalColor, ThemeHolder.globalFont,
            ThemeHolder.offsetX, ThemeHolder.offsetY)

        if (editMode) {
            toolbarButtons.clear()
            val ty = TOOLBAR_HEIGHT_DP * density
            canvas.drawRect(0f, 0f, W, ty, toolbarBg)

            val btnH = (TOOLBAR_HEIGHT_DP - 8f) * density
            val btnY = 4f * density

            data class Btn(val label: String, val action: () -> Unit)
            val moveLabel = if (ThemeHolder.moveAllMode) "Ｍ" else "M"
            val btns = listOf(
                Btn(moveLabel) { ThemeHolder.moveAllMode = !ThemeHolder.moveAllMode; invalidate() },
                Btn("✕") { ThemeHolder.setEditMode(false); ThemeHolder.selectWidget(null) },
                Btn("↩") { ThemeHolder.undo() },
                Btn("↺") { ThemeHolder.resetToOriginal() },
                Btn("✓") { ThemeHolder.setEditMode(false); ThemeHolder.selectWidget(null) },
            )

            var bx = 4f * density
            for (b in btns) {
                val bw = btnTextPaint.measureText(b.label) + 16f * density
                val rect = RectF(bx, btnY, bx + bw, btnY + btnH)
                canvas.drawRoundRect(rect, 4f * density, 4f * density, btnPaint)
                canvas.drawText(b.label, bx + 8f * density, btnY + btnH * 0.68f, btnTextPaint)
                toolbarButtons.add(rect to b.action)
                bx += bw + 4f * density
            }

            val lbl = ThemeHolder.selectedWidgetId.value
            if (lbl != null && widgets.any { it.id == lbl }) {
                val nw = btnTextPaint.measureText(lbl) + 16f * density
                canvas.drawRoundRect(RectF(W - nw - 4f * density, btnY, W - 4f * density, btnY + btnH), 4f * density, 4f * density, btnPaint)
                canvas.drawText(lbl, W - nw + 4f * density, btnY + btnH * 0.68f, btnTextPaint)
            }

            val selId = ThemeHolder.selectedWidgetId.value
            if (selId != null) {
                val sel = widgets.find { it.id == selId }
                if (sel != null) {
                    val r = widgetRectPx(sel, data)
                    val isResizing = draggedWidgetId == selId && resizeCorner != null
                    canvas.drawRoundRect(r, 6f * density, 6f * density, if (isResizing) resizePaint else boxPaint)

                    val hr = 8f * density
                    for ((cx, cy) in listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom)) {
                        canvas.drawCircle(cx, cy, hr, handlePaint)
                        canvas.drawCircle(cx, cy, hr, handleStroke)
                    }

                    val info = when (sel) {
                        is Widget.Bar -> "  w=${sel.width.toInt()} h=${sel.height.toInt()}"
                        is Widget.Graph -> "  w=${sel.width.toInt()} h=${sel.height.toInt()}"
                        is Widget.Text -> "  w=${sel.width.toInt()} h=${sel.height.toInt()}"
                        is Widget.HLine -> "  len=${sel.length.toInt()}"
                        is Widget.VLine -> "  len=${sel.length.toInt()}"
                        else -> ""
                    }
                    canvas.drawText("${sel.id}  x=${sel.x.toInt()}  y=${sel.y.toInt()}$info",
                        r.left + 4f * density, r.top - 10f * density, labelPaint)

                    for ((cx, cy) in listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom)) {
                        canvas.drawCircle(cx, cy, HANDLE_RADIUS_DP * density, cornerFill)
                    }
                }
            }

            var dy = TOOLBAR_HEIGHT_DP * density + 16f * density
            for (line in debugText.split("\n")) {
                canvas.drawText(line, 4f * density, dy, debugPaint)
                dy += 16f * density
            }
        }

        postOnAnimation(refreshRunnable)
    }

    private fun widgetRectPx(w: Widget, data: Map<String, String>): RectF {
        val s = ThemeHolder.globalScale
        val ox = ThemeHolder.offsetX; val oy = ThemeHolder.offsetY
        val left = (w.x + ox) * s * density; val top = (w.y + oy) * s * density
        return when (w) {
            is Widget.Text -> {
                if (w.width > 0f && w.height > 0f) {
                    RectF(left, top, left + w.width * s * density, top + w.height * s * density)
                } else {
                    val tw = btnTextPaint.measureText(resolveText(w, data)).coerceAtLeast(100f * density)
                    RectF(left, top, left + tw, top + w.fontSize * s * density + 4f * density)
                }
            }
            is Widget.Bar -> RectF(left, top, left + w.width * s * density, top + w.height * s * density)
            is Widget.Graph -> RectF(left, top, left + w.width * s * density, top + w.height * s * density)
            is Widget.HLine -> RectF(left, top - w.strokeWidth * s * density * 0.5f,
                left + w.length * s * density, top + w.strokeWidth * s * density * 0.5f)
            is Widget.VLine -> RectF(left - w.strokeWidth * s * density * 0.5f, top,
                left + w.strokeWidth * s * density * 0.5f, top + w.length * s * density)
        }
    }

    private fun Widget.widgetW(): Float = when (this) {
        is Widget.Bar -> width; is Widget.Graph -> width
        is Widget.Text -> if (width > 0f) width else 200f
        is Widget.HLine -> length; is Widget.VLine -> strokeWidth
        else -> 0f
    }

    private fun Widget.widgetH(): Float = when (this) {
        is Widget.Bar -> height; is Widget.Graph -> height
        is Widget.Text -> if (height > 0f) height else 20f
        is Widget.HLine -> strokeWidth; is Widget.VLine -> length
        else -> 0f
    }

    private fun resolveText(w: Widget.Text, data: Map<String, String>): String {
        val value = if (w.source != null) data[w.source] ?: "---" else null
        return if (value != null) w.format.replace("{value}", value)
        else { var r = w.format; for ((k, v) in data) r = r.replace("{${k}}", v); r }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow(); removeCallbacks(refreshRunnable); engine.stop()
    }

    fun destroy() { removeCallbacks(refreshRunnable); engine.stop() }
}
