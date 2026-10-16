package com.flatnotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.domain.EditorSpacing
import com.flatnotes.domain.spacingValueAt
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette

@Composable
internal fun SpacingSheet(
    spacing: EditorSpacing,
    onChange: (EditorSpacing) -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = FlatDimens.Radius, topEnd = FlatDimens.Radius)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlatPalette.Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
                .border(1.dp, FlatPalette.Divider, shape)
                .background(FlatPalette.Background, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlatText(text = "间距", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            SpacingSlider(
                label = "正文间距",
                value = spacing.body,
                range = EditorSpacing.BODY_RANGE,
                onChange = { onChange(spacing.copy(body = it).clamped()) },
            )
            SpacingSlider(
                label = "清单间距",
                value = spacing.checklist,
                range = EditorSpacing.CHECKLIST_RANGE,
                onChange = { onChange(spacing.copy(checklist = it).clamped()) },
            )
            SpacingSlider(
                label = "块间距（正文与清单/图片/涂鸦）",
                value = spacing.block,
                range = EditorSpacing.BLOCK_RANGE,
                onChange = { onChange(spacing.copy(block = it).clamped()) },
            )
        }
    }
}

@Composable
private fun SpacingSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlatText(text = label, modifier = Modifier.weight(1f), fontSize = 15.sp)
            Spacer(Modifier.size(8.dp))
            FlatText(text = "$value", color = FlatPalette.TextSecondary, fontSize = 14.sp)
        }
        FlatSlider(value = value, range = range, onChange = onChange)
    }
}

@Composable
private fun FlatSlider(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    val thumb = 16.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(range) {
                fun update(x: Float) {
                    onChange(spacingValueAt(x, size.width.toFloat(), range))
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position.x)
                    down.consume()
                    horizontalDrag(down.id) { change ->
                        update(change.position.x)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val span = (range.last - range.first).coerceAtLeast(1)
        val fraction = (value - range.first).toFloat() / span.toFloat()
        val travel = maxWidth - thumb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(FlatPalette.Divider),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(2.dp)
                .background(FlatPalette.Accent),
        )
        Box(
            modifier = Modifier
                .offset(x = travel * fraction.coerceIn(0f, 1f))
                .size(thumb)
                .background(FlatPalette.Accent, CircleShape),
        )
    }
}
