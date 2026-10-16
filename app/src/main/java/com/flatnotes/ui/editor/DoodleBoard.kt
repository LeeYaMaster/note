package com.flatnotes.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.components.FlatTextButton
import com.flatnotes.ui.components.Hairline
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette

private val PenColors = listOf(
    Color(0xFF1A1A1A),
    Color(0xFFFFC107),
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
)

private data class InkStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
)

@Composable
fun DoodleBoard(
    onCancel: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    val strokes = remember { mutableStateListOf<InkStroke>() }
    var active by remember { mutableStateOf<InkStroke?>(null) }
    var penColor by remember { mutableStateOf(PenColors.first()) }
    var eraser by remember { mutableStateOf(false) }
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val penWidth = with(density) { 3.5.dp.toPx() }
    val eraserWidth = with(density) { 22.dp.toPx() }
    val drawn = strokes.toList()
    val preview = active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlatPalette.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlatTextButton(text = "取消", onClick = onCancel, color = FlatPalette.TextSecondary)
            FlatText(
                text = "涂鸦",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            FlatTextButton(
                text = "完成",
                color = FlatPalette.Accent,
                onClick = {
                    val finished = drawn + listOfNotNull(preview)
                    if (finished.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) {
                        onCancel()
                    } else {
                        onSave(renderInk(finished, canvasWidth, canvasHeight))
                    }
                },
            )
        }
        Hairline()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, FlatPalette.Divider, RoundedCornerShape(FlatDimens.Radius))
                .background(Color.White)
                .onSizeChanged {
                    canvasWidth = it.width
                    canvasHeight = it.height
                }
                .pointerInput(penColor, eraser, penWidth, eraserWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            active = InkStroke(
                                points = listOf(offset),
                                color = if (eraser) Color.White else penColor,
                                width = if (eraser) eraserWidth else penWidth,
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val current = active ?: return@detectDragGestures
                            active = current.copy(points = current.points + change.position)
                        },
                        onDragEnd = {
                            active?.let { strokes.add(it) }
                            active = null
                        },
                        onDragCancel = { active = null },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                (drawn + listOfNotNull(preview)).forEach { stroke -> drawInk(stroke) }
            }
        }
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlatPalette.Toolbar)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PenColors.forEach { color ->
                val selected = !eraser && penColor == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) FlatPalette.TextPrimary else FlatPalette.Divider,
                            shape = CircleShape,
                        )
                        .clickable {
                            penColor = color
                            eraser = false
                        }
                        .padding(4.dp)
                        .background(color, CircleShape),
                )
            }
            ToolChip(label = "橡皮", selected = eraser, onClick = { eraser = true })
            ToolChip(
                label = "撤销",
                selected = false,
                enabled = drawn.isNotEmpty(),
                onClick = {
                    if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                },
            )
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val color = when {
        !enabled -> FlatPalette.Placeholder
        selected -> FlatPalette.Accent
        else -> FlatPalette.IconMuted
    }
    FlatText(
        text = label,
        color = color,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) FlatPalette.Accent else FlatPalette.Divider,
                shape = RoundedCornerShape(FlatDimens.Radius),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun DrawScope.drawInk(stroke: InkStroke) {
    if (stroke.points.isEmpty()) return
    if (stroke.points.size == 1) {
        drawCircle(color = stroke.color, radius = stroke.width / 2f, center = stroke.points.first())
        return
    }
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun renderInk(strokes: List<InkStroke>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        paint.color = stroke.color.toArgb()
        paint.strokeWidth = stroke.width
        if (stroke.points.size == 1) {
            paint.style = android.graphics.Paint.Style.FILL
            val point = stroke.points.first()
            canvas.drawCircle(point.x, point.y, stroke.width / 2f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
        } else {
            val path = android.graphics.Path().apply {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
