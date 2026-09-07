package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SciFiBackgroundDark
import com.example.ui.theme.SciFiGridLine

@Composable
fun TechGridBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SciFiBackgroundDark,
    gridColor: Color = SciFiGridLine,
    gridSizeDp: Dp = 26.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val gridSizePx = with(density) { gridSizeDp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw vertical grid lines
                var x = 0f
                while (x <= width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSizePx
                }

                // Draw horizontal grid lines
                var y = 0f
                while (y <= height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSizePx
                }
            }
    ) {
        content()
    }
}
