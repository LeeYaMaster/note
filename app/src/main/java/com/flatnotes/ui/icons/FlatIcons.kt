package com.flatnotes.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppIcon {
    Search,
    More,
    Back,
    Share,
    Add,
    Checklist,
    Image,
    Pencil,
    Check,
    AlignLeft,
    AlignCenter,
    AlignRight,
}

/**
 * 图标按 24×24 网格绘制，再缩放到画布短边。这样 28dp 的加号和 14dp 的勾
 * 都会落在画布正中，而不是按绝对 dp 画到左上角被裁掉。
 */
@Composable
fun FlatIcon(
    icon: AppIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.7.dp,
) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        if (unit <= 0f) return@Canvas
        val designed = 24.dp.toPx()
        val strokePx = strokeWidth.toPx() * (if (designed > 0f) size.minDimension / designed else 1f)
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun u(value: Float) = value * unit
        when (icon) {
            AppIcon.Search -> {
                drawCircle(color = tint, radius = u(6f), center = p(10.2f, 10.2f), style = stroke)
                drawLine(tint, p(14.8f, 14.8f), p(20.2f, 20.2f), stroke.width, StrokeCap.Round)
            }

            AppIcon.More -> {
                val radius = u(1.45f)
                val y = u(12f)
                floatArrayOf(5.2f, 12f, 18.8f).forEach { x ->
                    drawCircle(color = tint, radius = radius, center = Offset(u(x), y))
                }
            }

            AppIcon.Back -> {
                val path = Path().apply {
                    moveTo(u(15.2f), u(5f))
                    lineTo(u(8f), u(12f))
                    lineTo(u(15.2f), u(19f))
                }
                drawPath(path, tint, style = stroke)
            }

            AppIcon.Share -> {
                val left = p(6f, 12f)
                val top = p(18f, 6.2f)
                val bottom = p(18f, 17.8f)
                drawLine(tint, left, top, stroke.width, StrokeCap.Round)
                drawLine(tint, left, bottom, stroke.width, StrokeCap.Round)
                val radius = u(2.15f)
                drawCircle(tint, radius, left, style = stroke)
                drawCircle(tint, radius, top, style = stroke)
                drawCircle(tint, radius, bottom, style = stroke)
            }

            AppIcon.Add -> {
                drawLine(tint, p(12f, 5f), p(12f, 19f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(5f, 12f), p(19f, 12f), stroke.width, StrokeCap.Round)
            }

            AppIcon.Checklist -> {
                fun row(top: Float) {
                    val left = u(2.6f)
                    val boxTop = u(top)
                    val box = u(7.2f)
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, boxTop),
                        size = Size(box, box),
                        cornerRadius = CornerRadius(u(1.4f)),
                        style = stroke,
                    )
                    val mark = Path().apply {
                        moveTo(left + u(1.5f), boxTop + u(3.6f))
                        lineTo(left + u(3f), boxTop + u(5.2f))
                        lineTo(left + u(5.7f), boxTop + u(2.1f))
                    }
                    drawPath(mark, tint, style = Stroke(width = u(1.35f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val mid = boxTop + box / 2f
                    drawLine(tint, Offset(u(12.2f), mid), Offset(u(21.2f), mid), u(1.45f), StrokeCap.Round)
                }
                row(3.2f)
                row(13.4f)
            }

            AppIcon.Image -> {
                val left = u(3f)
                val top = u(5f)
                val width = u(18f)
                val height = u(14f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(u(2f)),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = u(1.45f),
                    center = Offset(left + u(4.2f), top + u(4f)),
                    style = stroke,
                )
                val mountain = Path().apply {
                    moveTo(left + u(0.8f), top + height - u(1.6f))
                    lineTo(left + u(6.4f), top + u(7f))
                    lineTo(left + u(10f), top + u(10.2f))
                    lineTo(left + u(13.2f), top + u(7.4f))
                    lineTo(left + width - u(0.8f), top + height - u(1.6f))
                }
                drawPath(mountain, tint, style = stroke)
            }

            AppIcon.AlignLeft -> {
                val start = u(4f)
                drawLine(tint, Offset(start, u(7f)), p(16f, 7f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(start, u(12f)), p(20f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(start, u(17f)), p(12f, 17f), stroke.width, StrokeCap.Round)
            }

            AppIcon.AlignCenter -> {
                drawLine(tint, p(6f, 7f), p(18f, 7f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 12f), p(20f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(8f, 17f), p(16f, 17f), stroke.width, StrokeCap.Round)
            }

            AppIcon.AlignRight -> {
                val end = u(20f)
                drawLine(tint, p(8f, 7f), Offset(end, u(7f)), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 12f), Offset(end, u(12f)), stroke.width, StrokeCap.Round)
                drawLine(tint, p(12f, 17f), Offset(end, u(17f)), stroke.width, StrokeCap.Round)
            }

            AppIcon.Pencil -> {
                val body = Path().apply {
                    moveTo(u(20f), u(6.2f))
                    lineTo(u(17.6f), u(3.8f))
                    lineTo(u(7.2f), u(14.2f))
                    lineTo(u(6.2f), u(17.8f))
                    lineTo(u(9.8f), u(16.8f))
                    close()
                }
                drawPath(body, tint, style = stroke)
                drawLine(tint, p(15.6f, 5.8f), p(18.2f, 8.4f), u(1.2f), StrokeCap.Round)
            }

            AppIcon.Check -> {
                val path = Path().apply {
                    moveTo(u(5f), u(12.4f))
                    lineTo(u(10f), u(17.4f))
                    lineTo(u(19.2f), u(7f))
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}
