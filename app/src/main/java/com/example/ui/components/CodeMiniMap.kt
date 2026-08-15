package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bird's-eye Code Mini-map component rendered on the right side of the editor.
 * Provides micro line-level visual overview and interactive viewport scrolling.
 */
@Composable
fun CodeMiniMap(
    content: String,
    scrollState: ScrollState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    keywordColor: Color = Color(0xFF569CD6),
    stringColor: Color = Color(0xFFCE9178),
    commentColor: Color = Color(0xFF6A9955)
) {
    val lines = remember(content) { content.lines() }
    val lineCount = lines.size.coerceAtLeast(1)

    Box(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight()
            .background(Color(0xFF181818))
            .testTag("code_minimap_container")
            .pointerInput(lineCount, scrollState.maxValue) {
                detectTapGestures { offset ->
                    val totalHeight = size.height.toFloat()
                    if (totalHeight > 0 && scrollState.maxValue > 0) {
                        val fraction = (offset.y / totalHeight).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            scrollState.scrollTo((fraction * scrollState.maxValue).toInt())
                        }
                    }
                }
            }
            .pointerInput(lineCount, scrollState.maxValue) {
                detectDragGestures { change, _ ->
                    val totalHeight = size.height.toFloat()
                    if (totalHeight > 0 && scrollState.maxValue > 0) {
                        val fraction = (change.position.y / totalHeight).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            scrollState.scrollTo((fraction * scrollState.maxValue).toInt())
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val lineHeight = (canvasHeight / lineCount.toFloat()).coerceIn(1.5f, 4f)
            val maxDrawLines = (canvasHeight / lineHeight).toInt().coerceAtMost(lineCount)

            // Draw micro lines
            for (i in 0 until maxDrawLines) {
                val line = lines.getOrNull(i) ?: ""
                val y = i * lineHeight
                val trimmed = line.trimStart()
                val leadingSpaces = (line.length - trimmed.length).coerceAtMost(30)
                val startX = (leadingSpaces * 1.5f).coerceIn(4f, canvasWidth - 10f)
                val textLength = trimmed.length.coerceAtMost(80)
                val lineWidth = (textLength * 1.2f).coerceIn(4f, (canvasWidth - startX - 4f).coerceAtLeast(4f))

                val lineColor = when {
                    trimmed.startsWith("//") || trimmed.startsWith("--") || trimmed.startsWith("#") || trimmed.startsWith("/*") ->
                        commentColor.copy(alpha = 0.7f)
                    trimmed.startsWith("\"") || trimmed.startsWith("'") || trimmed.startsWith("`") ->
                        stringColor.copy(alpha = 0.7f)
                    trimmed.startsWith("fun ") || trimmed.startsWith("val ") || trimmed.startsWith("var ") ||
                            trimmed.startsWith("class ") || trimmed.startsWith("import ") || trimmed.startsWith("package ") ||
                            trimmed.startsWith("function ") || trimmed.startsWith("const ") || trimmed.startsWith("select ") ->
                        keywordColor.copy(alpha = 0.8f)
                    else -> Color(0xFF6C6C6C)
                }

                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(startX, y),
                    size = Size(lineWidth, (lineHeight - 0.5f).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(1f, 1f)
                )
            }

            // Draw Viewport highlight box
            val scrollFraction = if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else 0f

            val viewportHeight = (canvasHeight * 0.25f).coerceIn(24f, canvasHeight)
            val availableTravel = (canvasHeight - viewportHeight).coerceAtLeast(0f)
            val viewportY = (availableTravel * scrollFraction).coerceIn(0f, availableTravel)

            // Viewport background
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(0f, viewportY),
                size = Size(canvasWidth, viewportHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Viewport borders
            drawRoundRect(
                color = Color(0xFF007ACC).copy(alpha = 0.6f),
                topLeft = Offset(0f, viewportY),
                size = Size(canvasWidth, viewportHeight),
                cornerRadius = CornerRadius(2f, 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
    }
}
