package com.flatnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.unit.IntOffset
import com.flatnotes.ui.icons.AppIcon
import com.flatnotes.ui.icons.FlatIcon
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette

@Composable
fun FlatText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FlatPalette.TextPrimary,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
        ),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FlatPalette.Divider),
    )
}

@Composable
fun FlatIconButton(
    icon: AppIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = FlatPalette.Icon,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FlatIcon(icon = icon, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun FlatFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(FlatDimens.FabSize)
            .semantics { contentDescription = "新建笔记" }
            .clip(CircleShape)
            .background(if (pressed) FlatPalette.AccentPressed else FlatPalette.Accent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        FlatIcon(
            icon = AppIcon.Add,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.2.dp,
        )
    }
}

data class FlatMenuEntry(
    val label: String,
    val checked: Boolean = false,
    val color: Color = FlatPalette.TextPrimary,
    val onClick: () -> Unit,
)

@Composable
fun FlatMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<FlatMenuEntry>,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return
    val offsetY = with(LocalDensity.current) { 40.dp.roundToPx() }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier
                .width(188.dp)
                .border(1.dp, FlatPalette.Divider, RoundedCornerShape(FlatDimens.Radius))
                .background(FlatPalette.Background, RoundedCornerShape(FlatDimens.Radius))
                .padding(vertical = 4.dp),
        ) {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            entry.onClick()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlatText(
                        text = entry.label,
                        modifier = Modifier.weight(1f),
                        color = entry.color,
                        fontSize = 15.sp,
                    )
                    if (entry.checked) {
                        FlatIcon(
                            icon = AppIcon.Check,
                            tint = FlatPalette.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Spacer(Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FlatTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FlatPalette.TextPrimary,
) {
    FlatText(
        text = text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
fun FlatConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmColor: Color = FlatPalette.Destructive,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlatPalette.Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .border(1.dp, FlatPalette.Divider, RoundedCornerShape(FlatDimens.Radius))
                .background(FlatPalette.Background, RoundedCornerShape(FlatDimens.Radius))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(20.dp),
        ) {
            FlatText(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlatText(text = message, color = FlatPalette.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                FlatTextButton(text = "取消", onClick = onDismiss, color = FlatPalette.TextSecondary)
                FlatTextButton(text = confirmLabel, onClick = onConfirm, color = confirmColor)
            }
        }
    }
}
