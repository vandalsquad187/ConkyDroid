package com.conkydroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.conkydroid.engine.Widget
import com.conkydroid.theme.Theme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ThemeEditorScreen(
    theme: Theme,
    onBack: () -> Unit,
    onSave: (Theme) -> Unit,
    onPreview: (Theme) -> Unit,
) {
    var editTheme by remember { mutableStateOf(theme) }
    var editWidget by remember { mutableStateOf<Widget?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSaveAs by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf(theme.name + " copy") }
    var draggedId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun preview() = onPreview(editTheme)

    fun genId(prefix: String): String {
        return "${prefix}_${System.currentTimeMillis() % 100000}_${(0..999).random()}"
    }

    fun move(from: Int, to: Int) {
        if (from == to) return
        val list = editTheme.widgets.toMutableList()
        val item = list.removeAt(from)
        list.add(to.coerceIn(0, list.size), item)
        editTheme = editTheme.copy(widgets = list)
        preview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editTheme.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { preview() }) {
                        Icon(Icons.Default.Star, contentDescription = "Preview")
                    }
                    IconButton(onClick = { showSaveAs = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Save as new")
                    }
                    IconButton(onClick = { onSave(editTheme) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Save")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add widget")
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Text") },
                        onClick = {
                            editTheme = editTheme.copy(
                                widgets = editTheme.widgets + Widget.Text(
                                    id = genId("text"),
                                    x = 20f, y = 40f, format = "New text", fontSize = 14f,
                                )
                            )
                            showAddMenu = false; preview()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Bar") },
                        onClick = {
                            editTheme = editTheme.copy(
                                widgets = editTheme.widgets + Widget.Bar(
                                    id = genId("bar"),
                                    x = 20f, y = 100f, width = 200f, height = 10f,
                                    source = "cpu_raw",
                                )
                            )
                            showAddMenu = false; preview()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Graph") },
                        onClick = {
                            editTheme = editTheme.copy(
                                widgets = editTheme.widgets + Widget.Graph(
                                    id = genId("graph"),
                                    x = 20f, y = 120f, width = 280f, height = 60f,
                                    source = "cpu_raw",
                                )
                            )
                            showAddMenu = false; preview()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("HLine") },
                        onClick = {
                            editTheme = editTheme.copy(
                                widgets = editTheme.widgets + Widget.HLine(
                                    id = genId("hline"),
                                    x = 10f, y = 50f, length = 200f, color = 0xFFFFFFFF.toInt(),
                                )
                            )
                            showAddMenu = false; preview()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("VLine") },
                        onClick = {
                            editTheme = editTheme.copy(
                                widgets = editTheme.widgets + Widget.VLine(
                                    id = genId("vline"),
                                    x = 100f, y = 10f, length = 200f, color = 0xFFFFFFFF.toInt(),
                                )
                            )
                            showAddMenu = false; preview()
                        },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("${editTheme.widgets.size} widgets  •  long-press ☰ to drag, tap to reorder", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(editTheme.widgets, key = { _, w -> w.id }) { index, widget ->
                var dragAccum by remember { mutableFloatStateOf(0f) }
                val isDragging = draggedId == widget.id
                WidgetCard(
                    modifier = Modifier
                        .animateItem()
                        .then(if (isDragging) Modifier.shadow(8.dp, CardDefaults.shape) else Modifier)
                        .pointerInput(widget.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = widget.id
                                    dragAccum = 0f
                                },
                                onDragEnd = { draggedId = null; dragAccum = 0f },
                                onDragCancel = { draggedId = null; dragAccum = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccum += dragAmount.y
                                    val threshold = 56.dp.toPx()
                                    if (dragAccum > threshold && index < editTheme.widgets.size - 1) {
                                        move(index, index + 1)
                                        dragAccum = 0f
                                    } else if (dragAccum < -threshold && index > 0) {
                                        move(index, index - 1)
                                        dragAccum = 0f
                                    }
                                }
                            )
                        },
                    index = index,
                    widget = widget,
                    total = editTheme.widgets.size,
                    isDragging = isDragging,
                    onEdit = { editWidget = widget },
                    onMoveUp = {
                        if (index > 0) move(index, index - 1)
                    },
                    onMoveDown = {
                        if (index < editTheme.widgets.size - 1) move(index, index + 1)
                    },
                    onDelete = {
                        editTheme = editTheme.copy(
                            widgets = editTheme.widgets.toMutableList().apply { removeAt(index) }
                        )
                        preview()
                    },
                )
            }
        }
    }

    if (showSaveAs) {
        AlertDialog(
            onDismissRequest = { showSaveAs = false },
            title = { Text("Save as new Conky") },
            text = {
                Column {
                    Text("Speichert aktuelle Widgets als neuen Conky")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = saveAsName, onValueChange = { saveAsName = it }, label = { Text("Name") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (saveAsName.isNotBlank()) {
                        onSave(editTheme.copy(name = saveAsName.trim()))
                        showSaveAs = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveAs = false }) { Text("Cancel") } },
        )
    }

    editWidget?.let { w ->
        WidgetEditDialog(
            widget = w,
            onDismiss = { editWidget = null },
            onSave = { updated ->
                editTheme = editTheme.copy(
                    widgets = editTheme.widgets.map { if (it.id == w.id) updated else it }
                )
                editWidget = null
                preview()
            },
        )
    }
}

@Composable
private fun WidgetCard(
    modifier: Modifier = Modifier,
    index: Int,
    widget: Widget,
    total: Int,
    isDragging: Boolean = false,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Drag handle", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(widget)
                    Spacer(Modifier.width(8.dp))
                    Text(widget.id, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Text("#$index", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    when (widget) {
                        is Widget.Text -> "x=${widget.x.toInt()} y=${widget.y.toInt()} fSize=${widget.fontSize.toInt()} src=${widget.source ?: "-"}"
                        is Widget.Bar -> "x=${widget.x.toInt()} y=${widget.y.toInt()} w=${widget.width.toInt()} h=${widget.height.toInt()} src=${widget.source}"
                        is Widget.Graph -> "x=${widget.x.toInt()} y=${widget.y.toInt()} w=${widget.width.toInt()} h=${widget.height.toInt()} src=${widget.source}"
                        is Widget.HLine -> "x=${widget.x.toInt()} y=${widget.y.toInt()} len=${widget.length.toInt()}"
                        is Widget.VLine -> "x=${widget.x.toInt()} y=${widget.y.toInt()} len=${widget.length.toInt()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TypeBadge(widget: Widget) {
    val label = when (widget) {
        is Widget.Text -> "T"
        is Widget.Bar -> "B"
        is Widget.Graph -> "G"
        is Widget.HLine -> "H"
        is Widget.VLine -> "V"
    }
    val color = when (widget) {
        is Widget.Text -> Color(0xFF00FF88)
        is Widget.Bar -> Color(0xFF4488FF)
        is Widget.Graph -> Color(0xFFFF4444)
        is Widget.HLine -> Color(0xFFAA44FF)
        is Widget.VLine -> Color(0xFFFFAA44)
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WidgetEditDialog(
    widget: Widget,
    onDismiss: () -> Unit,
    onSave: (Widget) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${widget.id}") },
        text = {
            when (widget) {
                is Widget.Text -> TextEditorContent(widget, onSave)
                is Widget.Bar -> BarEditorContent(widget, onSave)
                is Widget.Graph -> GraphEditorContent(widget, onSave)
                is Widget.HLine -> LineEditorContent(widget, true, onSave)
                is Widget.VLine -> LineEditorContent(widget, false, onSave)
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun TextEditorContent(widget: Widget.Text, onSave: (Widget) -> Unit) {
    var format by remember { mutableStateOf(widget.format) }
    var fontSize by remember { mutableStateOf(widget.fontSize.toString()) }
    var source by remember { mutableStateOf(widget.source ?: "") }
    var color by remember { mutableStateOf(colorToHex(widget.color)) }
    var x by remember { mutableStateOf(widget.x.toString()) }
    var y by remember { mutableStateOf(widget.y.toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = format, onValueChange = { format = it }, label = { Text("Format") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = fontSize, onValueChange = { fontSize = it }, label = { Text("Size") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color #RRGGBB") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("X") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Y") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSave(widget.copy(
                    x = x.toFloatOrNull() ?: widget.x,
                    y = y.toFloatOrNull() ?: widget.y,
                    format = format,
                    fontSize = fontSize.toFloatOrNull() ?: widget.fontSize,
                    source = source.ifBlank { null },
                    color = parseHexColor(color) ?: widget.color,
                ))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

@Composable
private fun BarEditorContent(widget: Widget.Bar, onSave: (Widget) -> Unit) {
    var x by remember { mutableStateOf(widget.x.toString()) }
    var y by remember { mutableStateOf(widget.y.toString()) }
    var width by remember { mutableStateOf(widget.width.toString()) }
    var height by remember { mutableStateOf(widget.height.toString()) }
    var source by remember { mutableStateOf(widget.source) }
    var max by remember { mutableStateOf(widget.max.toString()) }
    var fgColor by remember { mutableStateOf(colorToHex(widget.fgColor)) }
    var bgColor by remember { mutableStateOf(colorToHex(widget.bgColor)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("X") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Y") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Width") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("Max") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = fgColor, onValueChange = { fgColor = it }, label = { Text("Fg #RRGGBB") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = bgColor, onValueChange = { bgColor = it }, label = { Text("Bg #RRGGBB") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSave(widget.copy(
                    x = x.toFloatOrNull() ?: widget.x,
                    y = y.toFloatOrNull() ?: widget.y,
                    width = width.toFloatOrNull() ?: widget.width,
                    height = height.toFloatOrNull() ?: widget.height,
                    source = source,
                    max = max.toFloatOrNull() ?: widget.max,
                    fgColor = parseHexColor(fgColor) ?: widget.fgColor,
                    bgColor = parseHexColor(bgColor) ?: widget.bgColor,
                ))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

@Composable
private fun GraphEditorContent(widget: Widget.Graph, onSave: (Widget) -> Unit) {
    var x by remember { mutableStateOf(widget.x.toString()) }
    var y by remember { mutableStateOf(widget.y.toString()) }
    var width by remember { mutableStateOf(widget.width.toString()) }
    var height by remember { mutableStateOf(widget.height.toString()) }
    var source by remember { mutableStateOf(widget.source) }
    var max by remember { mutableStateOf(widget.max.toString()) }
    var historyLength by remember { mutableStateOf(widget.historyLength.toString()) }
    var lineColor by remember { mutableStateOf(colorToHex(widget.lineColor)) }
    var fillColor by remember { mutableStateOf(colorToHex(widget.fillColor)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("X") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Y") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Width") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = max, onValueChange = { max = it }, label = { Text("Max") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = historyLength, onValueChange = { historyLength = it }, label = { Text("History") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = lineColor, onValueChange = { lineColor = it }, label = { Text("Line #RRGGBB") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = fillColor, onValueChange = { fillColor = it }, label = { Text("Fill #RRGGBB(or #AARRGGBB)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSave(widget.copy(
                    x = x.toFloatOrNull() ?: widget.x,
                    y = y.toFloatOrNull() ?: widget.y,
                    width = width.toFloatOrNull() ?: widget.width,
                    height = height.toFloatOrNull() ?: widget.height,
                    source = source,
                    max = max.toFloatOrNull() ?: widget.max,
                    historyLength = historyLength.toIntOrNull() ?: widget.historyLength,
                    lineColor = parseHexColor(lineColor) ?: widget.lineColor,
                    fillColor = parseHexColor(fillColor) ?: widget.fillColor,
                ))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

@Composable
private fun LineEditorContent(widget: Widget, horizontal: Boolean, onSave: (Widget) -> Unit) {
    val isH = horizontal
    var x by remember { mutableStateOf(widget.x.toString()) }
    var y by remember { mutableStateOf(widget.y.toString()) }
    var length by remember { mutableStateOf(
        if (isH) (widget as? Widget.HLine)?.length?.toString() ?: "100"
        else (widget as? Widget.VLine)?.length?.toString() ?: "100"
    ) }
    var strokeWidth by remember { mutableStateOf(
        if (isH) (widget as? Widget.HLine)?.strokeWidth?.toString() ?: "2"
        else (widget as? Widget.VLine)?.strokeWidth?.toString() ?: "2"
    ) }
    var color by remember { mutableStateOf(
        colorToHex(if (isH) (widget as? Widget.HLine)?.color ?: -1 else (widget as? Widget.VLine)?.color ?: -1)
    ) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("X") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = y, onValueChange = { y = it }, label = { Text("Y") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text(if (isH) "Length" else "Height") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = strokeWidth, onValueChange = { strokeWidth = it }, label = { Text("Stroke") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color #RRGGBB") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val nx = x.toFloatOrNull() ?: widget.x
                val ny = y.toFloatOrNull() ?: widget.y
                val nl = length.toFloatOrNull() ?: 100f
                val nsw = strokeWidth.toFloatOrNull() ?: 2f
                val col = parseHexColor(color) ?: (if (isH) (widget as? Widget.HLine)?.color ?: 0xFFFFFFFF.toInt() else (widget as? Widget.VLine)?.color ?: 0xFFFFFFFF.toInt())
                if (isH) onSave(Widget.HLine(id = widget.id, x = nx, y = ny, zIndex = widget.zIndex, length = nl, color = col, strokeWidth = nsw))
                else onSave(Widget.VLine(id = widget.id, x = nx, y = ny, zIndex = widget.zIndex, length = nl, color = col, strokeWidth = nsw))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

private fun parseHexColor(hex: String): Int? {
    return try {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            3 -> "FF${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            4 -> "${clean[3]}${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            6 -> "FF$clean"
            8 -> clean
            else -> return null
        }
        android.graphics.Color.parseColor("#$argb")
    } catch (_: Exception) { null }
}

private fun colorToHex(color: Int): String {
    val a = android.graphics.Color.alpha(color)
    val r = android.graphics.Color.red(color)
    val g = android.graphics.Color.green(color)
    val b = android.graphics.Color.blue(color)
    return if (a == 255) "#%02x%02x%02x".format(r, g, b)
    else "#%02x%02x%02x%02x".format(a, r, g, b)
}
