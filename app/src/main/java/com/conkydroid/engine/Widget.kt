package com.conkydroid.engine

sealed class Widget {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val zIndex: Int

    data class Text(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val zIndex: Int = 0,
        val format: String,
        val fontSize: Float = 14f,
        val color: Int = 0xFFFFFFFF.toInt(),
        val source: String? = null,
        val width: Float = 0f,
        val height: Float = 0f,
    ) : Widget()

    data class Bar(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val zIndex: Int = 0,
        val width: Float,
        val height: Float,
        val source: String,
        val fgColor: Int = 0xFF00FF88.toInt(),
        val bgColor: Int = 0xFF333333.toInt(),
        val max: Float = 1f,
    ) : Widget()

    data class Graph(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val zIndex: Int = 0,
        val width: Float,
        val height: Float,
        val source: String,
        val lineColor: Int = 0xFF00FF88.toInt(),
        val fillColor: Int = 0x2200FF88.toInt(),
        val historyLength: Int = 60,
        val max: Float = 1f,
    ) : Widget()

    data class HLine(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val zIndex: Int = 0,
        val length: Float,
        val color: Int = 0xFFFFFFFF.toInt(),
        val strokeWidth: Float = 2f,
    ) : Widget()

    data class VLine(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val zIndex: Int = 0,
        val length: Float,
        val color: Int = 0xFFFFFFFF.toInt(),
        val strokeWidth: Float = 2f,
    ) : Widget()
}
