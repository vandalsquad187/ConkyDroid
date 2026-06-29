package com.conkydroid.theme

import com.conkydroid.engine.Widget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeHolder {
    private val _widgets = MutableStateFlow<List<Widget>>(emptyList())
    val widgets: StateFlow<List<Widget>> = _widgets.asStateFlow()

    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    private val _selectedWidgetId = MutableStateFlow<String?>(null)
    val selectedWidgetId: StateFlow<String?> = _selectedWidgetId.asStateFlow()

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    var globalScale: Float = 1f
    var globalColor: Int? = null
    var globalFont: String? = null
    var globalAlpha: Float = 1f
    var offsetX: Float = 0f
    var offsetY: Float = 0f
    var moveAllMode: Boolean = false
    var batterySaver: Boolean = false

    private var originalWidgets: List<Widget> = emptyList()
    private val undoStack = mutableListOf<List<Widget>>()

    fun update(widgets: List<Widget>, scale: Float = 1f, color: Int? = null, font: String? = null) {
        originalWidgets = widgets.toList()
        undoStack.clear()
        _widgets.value = widgets
        globalScale = scale
        globalColor = color
        globalFont = font
        offsetX = 0f
        offsetY = 0f
        _version.value++
    }

    fun setEditMode(enabled: Boolean) {
        _editMode.value = enabled
    }

    fun updateWidgetPosition(id: String, newX: Float, newY: Float) {
        undoStack.add(_widgets.value)
        _widgets.value = _widgets.value.map { w ->
            when {
                w.id != id -> w
                w is Widget.Text -> w.copy(x = newX, y = newY)
                w is Widget.Bar -> w.copy(x = newX, y = newY)
                w is Widget.Graph -> w.copy(x = newX, y = newY)
                w is Widget.HLine -> w.copy(x = newX, y = newY)
                w is Widget.VLine -> w.copy(x = newX, y = newY)
                else -> w
            }
        }
        _version.value++
    }

    fun resizeWidget(id: String, newX: Float, newY: Float, newW: Float, newH: Float) {
        undoStack.add(_widgets.value)
        _widgets.value = _widgets.value.map { w ->
            when {
                w.id != id -> w
                w is Widget.Bar -> w.copy(x = newX, y = newY, width = newW, height = newH)
                w is Widget.Graph -> w.copy(x = newX, y = newY, width = newW, height = newH)
                w is Widget.Text -> w.copy(x = newX, y = newY, width = newW, height = newH)
                w is Widget.HLine -> w.copy(x = newX, y = newY, length = newW.coerceAtLeast(10f))
                w is Widget.VLine -> w.copy(x = newX, y = newY, length = newH.coerceAtLeast(10f))
                else -> w
            }
        }
        _version.value++
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        _widgets.value = undoStack.removeLast()
        _version.value++
        return true
    }

    fun selectWidget(id: String?) {
        _selectedWidgetId.value = id
    }

    fun nudgeWidget(id: String, dx: Float, dy: Float) {
        undoStack.add(_widgets.value)
        _widgets.value = _widgets.value.map { w ->
            when {
                w.id != id -> w
                w is Widget.Text -> w.copy(x = w.x + dx, y = w.y + dy)
                w is Widget.Bar -> w.copy(x = w.x + dx, y = w.y + dy)
                w is Widget.Graph -> w.copy(x = w.x + dx, y = w.y + dy)
                w is Widget.HLine -> w.copy(x = w.x + dx, y = w.y + dy)
                w is Widget.VLine -> w.copy(x = w.x + dx, y = w.y + dy)
                else -> w
            }
        }
        _version.value++
    }

    fun nudgeWidgetSize(id: String, dw: Float, dh: Float) {
        undoStack.add(_widgets.value)
        _widgets.value = _widgets.value.map { w ->
            when {
                w.id != id -> w
                w is Widget.Bar -> w.copy(width = (w.width + dw).coerceAtLeast(10f), height = (w.height + dh).coerceAtLeast(5f))
                w is Widget.Graph -> w.copy(width = (w.width + dw).coerceAtLeast(10f), height = (w.height + dh).coerceAtLeast(5f))
                w is Widget.Text -> w.copy(width = (w.width + dw).coerceAtLeast(10f), height = (w.height + dh).coerceAtLeast(5f))
                w is Widget.HLine -> w.copy(length = (w.length + dw).coerceAtLeast(10f))
                w is Widget.VLine -> w.copy(length = (w.length + dh).coerceAtLeast(10f))
                else -> w
            }
        }
        _version.value++
    }

    fun clearUndo() {
        undoStack.clear()
        _version.value++
    }

    fun moveOffset(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
        _version.value++
    }

    fun resetToOriginal() {
        undoStack.clear()
        _widgets.value = originalWidgets.toList()
        _version.value++
    }
}
