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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

@Composable
fun FlatIcon(
    icon: AppIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.7.dp,
) {
    Canvas(modifier) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (icon) {
            AppIcon.Search -> {
                drawCircle(
                    color = tint,
                    radius = 6.dp.toPx(),
                    center = Offset(10.2.dp.toPx(), 10.2.dp.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(14.8.dp.toPx(), 14.8.dp.toPx()),
                    end = Offset(20.2.dp.toPx(), 20.2.dp.toPx()),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            AppIcon.More -> {
                val radius = 1.45.dp.toPx()
                val y = 12.dp.toPx()
                floatArrayOf(5.2.dp.toPx(), 12.dp.toPx(), 18.8.dp.toPx()).forEach { x ->
                    drawCircle(color = tint, radius = radius, center = Offset(x, y))
                }
            }

            AppIcon.Back -> {
                val path = Path().apply {
                    moveTo(15.2.dp.toPx(), 5.dp.toPx())
                    lineTo(8.dp.toPx(), 12.dp.toPx())
                    lineTo(15.2.dp.toPx(), 19.dp.toPx())
                }
                drawPath(path, tint, style = stroke)
            }

            AppIcon.Share -> {
                val left = Offset(6.dp.toPx(), 12.dp.toPx())
                val top = Offset(18.dp.toPx(), 6.2.dp.toPx())
                val bottom = Offset(18.dp.toPx(), 17.8.dp.toPx())
                drawLine(tint, left, top, stroke.width, StrokeCap.Round)
                drawLine(tint, left, bottom, stroke.width, StrokeCap.Round)
                val radius = 2.15.dp.toPx()
                drawCircle(tint, radius, left, style = stroke)
                drawCircle(tint, radius, top, style = stroke)
                drawCircle(tint, radius, bottom, style = stroke)
            }

            AppIcon.Add -> {
                drawLine(
                    tint,
                    Offset(12.dp.toPx(), 5.dp.toPx()),
                    Offset(12.dp.toPx(), 19.dp.toPx()),
                    stroke.width,
                    StrokeCap.Round,
                )
                drawLine(
                    tint,
                    Offset(5.dp.toPx(), 12.dp.toPx()),
                    Offset(19.dp.toPx(), 12.dp.toPx()),
                    stroke.width,
                    StrokeCap.Round,
                )
            }

            AppIcon.Checklist -> {
                val row: DrawScope.(Dp) -> Unit = { top ->
                    val left = 2.6.dp.toPx()
                    val boxTop = top.toPx()
                    val box = 7.2.dp.toPx()
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(left, boxTop),
                        size = Size(box, box),
                        cornerRadius = CornerRadius(1.4.dp.toPx()),
                        style = stroke,
                    )
                    val mark = Path().apply {
                        moveTo(left + 1.5.dp.toPx(), boxTop + 3.6.dp.toPx())
                        lineTo(left + 3.dp.toPx(), boxTop + 5.2.dp.toPx())
                        lineTo(left + 5.7.dp.toPx(), boxTop + 2.1.dp.toPx())
                    }
                    drawPath(
                        mark,
                        tint,
                        style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    val mid = boxTop + box / 2f
                    drawLine(
                        tint,
                        Offset(12.2.dp.toPx(), mid),
                        Offset(21.2.dp.toPx(), mid),
                        1.45.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
                row(3.2.dp)
                row(13.4.dp)
            }

            AppIcon.Image -> {
                val left = 3.dp.toPx()
                val top = 5.dp.toPx()
                val width = 18.dp.toPx()
                val height = 14.dp.toPx()
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = stroke,
                )
                drawCircle(
                    color = tint,
                    radius = 1.45.dp.toPx(),
                    center = Offset(left + 4.2.dp.toPx(), top + 4.dp.toPx()),
                    style = stroke,
                )
                val mountain = Path().apply {
                    moveTo(left + 0.8.dp.toPx(), top + height - 1.6.dp.toPx())
                    lineTo(left + 6.4.dp.toPx(), top + 7.dp.toPx())
                    lineTo(left + 10.dp.toPx(), top + 10.2.dp.toPx())
                    lineTo(left + 13.2.dp.toPx(), top + 7.4.dp.toPx())
                    lineTo(left + width - 0.8.dp.toPx(), top + height - 1.6.dp.toPx())
                }
                drawPath(mountain, tint, style = stroke)
            }

            AppIcon.AlignLeft -> {
                val start = 4.dp.toPx()
                drawLine(tint, Offset(start, 7.dp.toPx()), Offset(16.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(start, 12.dp.toPx()), Offset(20.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(start, 17.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
            }

            AppIcon.AlignCenter -> {
                drawLine(tint, Offset(6.dp.toPx(), 7.dp.toPx()), Offset(18.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(4.dp.toPx(), 12.dp.toPx()), Offset(20.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(8.dp.toPx(), 17.dp.toPx()), Offset(16.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
            }

            AppIcon.AlignRight -> {
                val end = 20.dp.toPx()
                drawLine(tint, Offset(8.dp.toPx(), 7.dp.toPx()), Offset(end, 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(4.dp.toPx(), 12.dp.toPx()), Offset(end, 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(12.dp.toPx(), 17.dp.toPx()), Offset(end, 17.dp.toPx()), stroke.width, StrokeCap.Round)
            }

            AppIcon.Pencil -> {
                val body = Path().apply {
                    moveTo(20.dp.toPx(), 6.2.dp.toPx())
                    lineTo(17.6.dp.toPx(), 3.8.dp.toPx())
                    lineTo(7.2.dp.toPx(), 14.2.dp.toPx())
                    lineTo(6.2.dp.toPx(), 17.8.dp.toPx())
                    lineTo(9.8.dp.toPx(), 16.8.dp.toPx())
                    close()
                }
                drawPath(body, tint, style = stroke)
                drawLine(
                    tint,
                    Offset(15.6.dp.toPx(), 5.8.dp.toPx()),
                    Offset(18.2.dp.toPx(), 8.4.dp.toPx()),
                    1.2.dp.toPx(),
                    StrokeCap.Round,
                )
            }

            AppIcon.Check -> {
                val path = Path().apply {
                    moveTo(5.dp.toPx(), 12.4.dp.toPx())
                    lineTo(10.dp.toPx(), 17.4.dp.toPx())
                    lineTo(19.2.dp.toPx(), 7.dp.toPx())
                }
                drawPath(path, tint, style = stroke)
            }
        }
    }
}
