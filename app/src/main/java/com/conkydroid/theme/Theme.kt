package com.conkydroid.theme

import com.conkydroid.engine.Widget

data class Theme(
    val name: String,
    val author: String = "",
    val version: Int = 1,
    val refreshMs: Long = 1000,
    val background: Background = Background(),
    val widgets: List<Widget> = emptyList(),
    val globalScale: Float = 1f,
    val globalColor: String? = null,
    val globalFont: String? = null,
) {
    data class Background(
        val color: Int = 0x00000000,
        val width: Float = 360f,
        val height: Float = 600f,
    )
}
