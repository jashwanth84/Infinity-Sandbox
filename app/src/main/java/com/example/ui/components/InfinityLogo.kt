package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.InfinityCyan
import com.example.ui.theme.InfinityPink
import com.example.ui.theme.InfinityViolet

@Composable
fun InfinityLogo(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 36.dp,
    animate: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinity_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height

        // Background rounded square (squircle)
        val corner = w * 0.26f
        val bgBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF281E48), Color(0xFF140F2A)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = bgBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Gradient border
        val borderBrush = Brush.sweepGradient(
            colors = listOf(InfinityViolet, InfinityCyan, InfinityPink, InfinityViolet),
            center = Offset(w / 2f, h / 2f)
        )
        drawRoundRect(
            brush = borderBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = w * 0.05f)
        )

        // Infinity Symbol (∞)
        val strokeW = w * 0.08f
        val infinityBrush = Brush.linearGradient(
            colors = listOf(InfinityCyan, InfinityViolet, InfinityPink),
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f)
        )

        val cy = h / 2f
        val leftX = w * 0.32f
        val rightX = w * 0.68f
        val loopR = w * 0.16f

        val path = Path().apply {
            // Left loop (start from center, loop left and back to center)
            moveTo(w / 2f, cy)
            cubicTo(
                w * 0.42f, cy - loopR * 1.1f,
                leftX - loopR, cy - loopR * 1.1f,
                leftX - loopR, cy
            )
            cubicTo(
                leftX - loopR, cy + loopR * 1.1f,
                w * 0.42f, cy + loopR * 1.1f,
                w / 2f, cy
            )
            // Right loop (from center, loop right and back to center)
            cubicTo(
                w * 0.58f, cy - loopR * 1.1f,
                rightX + loopR, cy - loopR * 1.1f,
                rightX + loopR, cy
            )
            cubicTo(
                rightX + loopR, cy + loopR * 1.1f,
                w * 0.58f, cy + loopR * 1.1f,
                w / 2f, cy
            )
            close()
        }

        drawPath(
            path = path,
            brush = infinityBrush,
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
