package com.flatnotes.ui.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFont
import com.flatnotes.data.ParagraphAlign
import com.flatnotes.domain.mediaScaleSteps
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal val CheckGreen = Color(0xFF22A45A)

internal fun NoteFont.family(): FontFamily = when (this) {
    NoteFont.Default -> FontFamily.SansSerif
    NoteFont.Serif -> FontFamily.Serif
    NoteFont.Monospace -> FontFamily.Monospace
    NoteFont.Handwriting -> FontFamily.Cursive
}

internal fun NoteFont.label(): String = when (this) {
    NoteFont.Default -> "系统默认"
    NoteFont.Serif -> "宋体风格(衬线)"
    NoteFont.Monospace -> "等宽代码"
    NoteFont.Handwriting -> "手写体"
}

private val BlockFontSize = 16.sp
private val BlockLineHeight = 24.sp
private val BlockGap = 16.dp

@Composable
fun EditorBody(
    blocks: List<NoteBlock>,
    mediaFile: (String) -> File,
    loadBitmap: (File) -> Bitmap?,
    focusRequesters: MutableMap<String, FocusRequester>,
    mediaMenuId: String?,
    captionEditing: Boolean,
    onFocus: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onRemoveEmpty: (String) -> Unit,
    onToggle: (String) -> Unit,
    onMediaMenu: (String) -> Unit,
    onEnlarge: (String) -> Unit,
    onShrink: (String) -> Unit,
    onCaptionEdit: (String) -> Unit,
    onCaptionChange: (String, String) -> Unit,
    onRemoveMedia: (NoteBlock) -> Unit,
    onAppendText: () -> Unit,
) {
    val showPlaceholder = blocks.all { block ->
        when (block) {
            is NoteBlock.Text -> block.text.isBlank()
            is NoteBlock.Checklist, is NoteBlock.Image, is NoteBlock.Doodle -> false
        }
    }
    val checklistItemGap = with(LocalDensity.current) { (BlockLineHeight * 0.5f).toDp() }
    Column(Modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) {
                Spacer(Modifier.height(gapBetween(blocks[index - 1], block, checklistItemGap)))
            }
            when (block) {
                is NoteBlock.Text -> TextBlockField(
                    text = block.text,
                    align = block.align,
                    font = block.font,
                    placeholder = if (showPlaceholder) "开始输入" else null,
                    focusRequester = focusRequesters.getOrPut(block.id) { FocusRequester() },
                    onFocus = { onFocus(block.id) },
                    onTextChange = { onTextChange(block.id, it) },
                    onRemoveEmpty = { onRemoveEmpty(block.id) },
                )
                is NoteBlock.Checklist -> ChecklistBlockField(
                    text = block.text,
                    done = block.done,
                    align = block.align,
                    font = block.font,
                    focusRequester = focusRequesters.getOrPut(block.id) { FocusRequester() },
                    onFocus = { onFocus(block.id) },
                    onTextChange = { onTextChange(block.id, it) },
                    onRemoveEmpty = { onRemoveEmpty(block.id) },
                    onToggle = { onToggle(block.id) },
                )
                is NoteBlock.Image -> MediaBlock(
                    file = mediaFile(block.fileName),
                    scale = block.scale,
                    caption = block.caption,
                    kindLabel = "图片",
                    menuOpen = mediaMenuId == block.id,
                    captionEditing = mediaMenuId == block.id && captionEditing,
                    loadBitmap = loadBitmap,
                    onToggleMenu = { onMediaMenu(block.id) },
                    onEnlarge = { onEnlarge(block.id) },
                    onShrink = { onShrink(block.id) },
                    onCaptionEdit = { onCaptionEdit(block.id) },
                    onCaptionChange = { onCaptionChange(block.id, it) },
                    onDelete = { onRemoveMedia(block) },
                )
                is NoteBlock.Doodle -> MediaBlock(
                    file = mediaFile(block.fileName),
                    scale = block.scale,
                    caption = block.caption,
                    kindLabel = "涂鸦",
                    menuOpen = mediaMenuId == block.id,
                    captionEditing = mediaMenuId == block.id && captionEditing,
                    loadBitmap = loadBitmap,
                    onToggleMenu = { onMediaMenu(block.id) },
                    onEnlarge = { onEnlarge(block.id) },
                    onShrink = { onShrink(block.id) },
                    onCaptionEdit = { onCaptionEdit(block.id) },
                    onCaptionChange = { onCaptionChange(block.id, it) },
                    onDelete = { onRemoveMedia(block) },
                )
            }
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAppendText,
                ),
        )
    }
}

@Composable
private fun TextBlockField(
    text: String,
    align: ParagraphAlign,
    font: NoteFont,
    placeholder: String?,
    focusRequester: FocusRequester,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemoveEmpty: () -> Unit,
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Backspace || event.key == Key.Delete) &&
                    text.isEmpty()
                ) {
                    onRemoveEmpty()
                    true
                } else {
                    false
                }
            },
        textStyle = blockTextStyle(align, font, done = false),
        cursorBrush = SolidColor(FlatPalette.Accent),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty() && placeholder != null) {
                    FlatText(text = placeholder, color = FlatPalette.Placeholder, fontSize = BlockFontSize)
                }
                inner()
            }
        },
    )
}

@Composable
private fun ChecklistBlockField(
    text: String,
    done: Boolean,
    align: ParagraphAlign,
    font: NoteFont,
    focusRequester: FocusRequester,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemoveEmpty: () -> Unit,
    onToggle: () -> Unit,
) {
    val line = with(LocalDensity.current) { BlockLineHeight.toDp() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(line)
                .semantics { contentDescription = if (done) "已完成" else "未完成" }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ChecklistMark(done)
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Backspace || event.key == Key.Delete) &&
                        text.isEmpty()
                    ) {
                        onRemoveEmpty()
                        true
                    } else {
                        false
                    }
                },
            textStyle = blockTextStyle(align, font, done),
            cursorBrush = SolidColor(FlatPalette.Accent),
        )
    }
}

@Composable
private fun ChecklistMark(done: Boolean) {
    Canvas(Modifier.size(14.dp)) {
        val stroke = 1.4.dp.toPx()
        val inset = stroke / 2f
        val side = size.minDimension - inset * 2
        val topLeft = Offset(inset, inset)
        val corner = CornerRadius(3.dp.toPx())
        if (done) {
            drawRoundRect(
                color = CheckGreen,
                topLeft = topLeft,
                size = Size(side, side),
                cornerRadius = corner,
            )
            val path = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.68f)
                lineTo(size.width * 0.73f, size.height * 0.34f)
            }
            drawPath(
                path,
                Color.White,
                style = Stroke(
                    width = size.minDimension * 0.12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        } else {
            drawRoundRect(
                color = FlatPalette.IconMuted,
                topLeft = topLeft,
                size = Size(side, side),
                cornerRadius = corner,
                style = Stroke(width = stroke),
            )
        }
    }
}

@Composable
private fun MediaBlock(
    file: File,
    scale: Float,
    caption: String,
    kindLabel: String,
    menuOpen: Boolean,
    captionEditing: Boolean,
    loadBitmap: (File) -> Bitmap?,
    onToggleMenu: () -> Unit,
    onEnlarge: () -> Unit,
    onShrink: () -> Unit,
    onCaptionEdit: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(file.absolutePath) { mutableStateOf(false) }
    LaunchedEffect(file.absolutePath) {
        val decoded = withContext(Dispatchers.IO) { loadBitmap(file) }
        if (decoded == null) failed = true else bitmap = decoded
    }
    val fraction = scale.coerceIn(mediaScaleSteps.first(), mediaScaleSteps.last())
    val alignStart = fraction < mediaScaleSteps.last() - 0.01f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (menuOpen) 1f else 0f),
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        MediaFrame(
            bitmap = bitmap,
            failed = failed,
            fraction = fraction,
            kindLabel = kindLabel,
            caption = caption,
            menuOpen = menuOpen,
            captionEditing = captionEditing,
            alignStart = alignStart,
            canEnlarge = fraction < mediaScaleSteps.last() - 0.01f,
            canShrink = fraction > mediaScaleSteps.first() + 0.01f,
            onToggleMenu = onToggleMenu,
            onEnlarge = onEnlarge,
            onShrink = onShrink,
            onDelete = onDelete,
            onCaptionEdit = onCaptionEdit,
            onCaptionChange = onCaptionChange,
        )
        if (caption.isNotBlank()) {
            BasicText(
                text = caption,
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .padding(top = 4.dp),
                style = TextStyle(
                    color = FlatPalette.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = if (alignStart) TextAlign.Start else TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun MediaFrame(
    bitmap: Bitmap?,
    failed: Boolean,
    fraction: Float,
    kindLabel: String,
    caption: String,
    menuOpen: Boolean,
    captionEditing: Boolean,
    alignStart: Boolean,
    canEnlarge: Boolean,
    canShrink: Boolean,
    onToggleMenu: () -> Unit,
    onEnlarge: () -> Unit,
    onShrink: () -> Unit,
    onDelete: () -> Unit,
    onCaptionEdit: () -> Unit,
    onCaptionChange: (String) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        val shape = RoundedCornerShape(FlatDimens.Radius)
        val frame = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, FlatPalette.Divider, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleMenu,
            )
        Box(
            Modifier
                .align(if (alignStart) Alignment.CenterStart else Alignment.Center)
                .fillMaxWidth(fraction),
        ) {
            when {
                bitmap != null -> Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = kindLabel,
                    modifier = frame,
                    contentScale = ContentScale.FillWidth,
                )
                failed -> Box(
                    modifier = frame.height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FlatText(text = "无法显示$kindLabel", color = FlatPalette.TextSecondary, fontSize = 14.sp)
                }
                else -> Box(frame.height(140.dp))
            }
            AnimatedVisibility(
                visible = menuOpen,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize(Alignment.Center, unbounded = true),
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.86f, animationSpec = tween(180)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.86f, animationSpec = tween(120)),
            ) {
            MediaActions(
                kindLabel = kindLabel,
                caption = caption,
                captionEditing = captionEditing,
                canEnlarge = canEnlarge,
                canShrink = canShrink,
                onEnlarge = onEnlarge,
                onShrink = onShrink,
                onDelete = onDelete,
                onCaptionEdit = onCaptionEdit,
                onCaptionChange = onCaptionChange,
            )
            }
        }
    }
}

@Composable
private fun MediaActions(
    kindLabel: String,
    caption: String,
    captionEditing: Boolean,
    canEnlarge: Boolean,
    canShrink: Boolean,
    onEnlarge: () -> Unit,
    onShrink: () -> Unit,
    onDelete: () -> Unit,
    onCaptionEdit: () -> Unit,
    onCaptionChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(FlatDimens.Radius)
    val focus = remember { FocusRequester() }
    LaunchedEffect(captionEditing) {
        if (captionEditing) runCatching { focus.requestFocus() }
    }
    Column(
        modifier = Modifier
            .width(228.dp)
            .border(1.dp, FlatPalette.Divider, shape)
            .background(FlatPalette.Background, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        if (captionEditing) {
            BasicTextField(
                value = caption,
                onValueChange = { onCaptionChange(it.replace("\n", "")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                textStyle = TextStyle(color = FlatPalette.TextPrimary, fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(FlatPalette.Accent),
                decorationBox = { inner ->
                    Box {
                        if (caption.isEmpty()) {
                            FlatText(text = "${kindLabel}描述", color = FlatPalette.Placeholder, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FlatPalette.Divider),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionLabel("放大", enabled = canEnlarge, onClick = onEnlarge)
            ActionLabel("缩小", enabled = canShrink, onClick = onShrink)
            ActionLabel("删除", enabled = true, color = FlatPalette.Destructive, onClick = onDelete)
            ActionLabel("描述", enabled = true, selected = captionEditing, onClick = onCaptionEdit)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ActionLabel(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = FlatPalette.TextPrimary,
    selected: Boolean = false,
) {
    val tint = when {
        !enabled -> FlatPalette.Placeholder
        selected -> FlatPalette.Accent
        else -> color
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        FlatText(text = label, color = tint, fontSize = 13.sp)
    }
}

private fun gapBetween(previous: NoteBlock, current: NoteBlock, checklistItemGap: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    val previousMedia = previous is NoteBlock.Image || previous is NoteBlock.Doodle
    val currentMedia = current is NoteBlock.Image || current is NoteBlock.Doodle
    return when {
        previous is NoteBlock.Checklist && current is NoteBlock.Checklist -> checklistItemGap
        previousMedia || currentMedia -> BlockGap
        previous is NoteBlock.Checklist || current is NoteBlock.Checklist -> BlockGap
        else -> 0.dp
    }
}

private fun blockTextStyle(align: ParagraphAlign, font: NoteFont, done: Boolean): TextStyle {
    return TextStyle(
        color = if (done) FlatPalette.TextSecondary else FlatPalette.TextPrimary,
        fontSize = BlockFontSize,
        fontFamily = font.family(),
        lineHeight = BlockLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        textAlign = when (align) {
            ParagraphAlign.Start -> TextAlign.Start
            ParagraphAlign.Center -> TextAlign.Center
            ParagraphAlign.End -> TextAlign.End
        },
        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
    )
}
