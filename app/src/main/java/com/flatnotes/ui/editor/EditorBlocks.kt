package com.flatnotes.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.ParagraphAlign
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.components.FlatTextButton
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditorBody(
    blocks: List<NoteBlock>,
    mediaFile: (String) -> File,
    loadBitmap: (File) -> Bitmap?,
    focusRequesters: MutableMap<String, FocusRequester>,
    onFocus: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onRemoveEmpty: (String) -> Unit,
    onToggle: (String) -> Unit,
    onRemoveMedia: (NoteBlock) -> Unit,
    onAppendText: () -> Unit,
) {
    val showPlaceholder = blocks.all { block ->
        when (block) {
            is NoteBlock.Text -> block.text.isBlank()
            is NoteBlock.Checklist, is NoteBlock.Image, is NoteBlock.Doodle -> false
        }
    }
    Column(Modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.Text -> TextBlockField(
                    text = block.text,
                    align = block.align,
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
                    focusRequester = focusRequesters.getOrPut(block.id) { FocusRequester() },
                    onFocus = { onFocus(block.id) },
                    onTextChange = { onTextChange(block.id, it) },
                    onRemoveEmpty = { onRemoveEmpty(block.id) },
                    onToggle = { onToggle(block.id) },
                )
                is NoteBlock.Image -> MediaBlock(
                    file = mediaFile(block.fileName),
                    loadBitmap = loadBitmap,
                    description = "图片",
                    onRemove = { onRemoveMedia(block) },
                )
                is NoteBlock.Doodle -> MediaBlock(
                    file = mediaFile(block.fileName),
                    loadBitmap = loadBitmap,
                    description = "涂鸦",
                    onRemove = { onRemoveMedia(block) },
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
            .padding(vertical = 2.dp)
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
        textStyle = blockTextStyle(align, done = false),
        cursorBrush = SolidColor(FlatPalette.Accent),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty() && placeholder != null) {
                    FlatText(text = placeholder, color = FlatPalette.Placeholder, fontSize = 16.sp)
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
    focusRequester: FocusRequester,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemoveEmpty: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .semantics { contentDescription = if (done) "已完成" else "未完成" }
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                FlatText(text = "✅", fontSize = 18.sp)
            } else {
                Box(
                    Modifier
                        .size(18.dp)
                        .border(1.5.dp, FlatPalette.IconMuted, RoundedCornerShape(3.dp)),
                )
            }
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
            textStyle = blockTextStyle(align, done),
            cursorBrush = SolidColor(FlatPalette.Accent),
        )
    }
}

@Composable
private fun MediaBlock(
    file: File,
    loadBitmap: (File) -> Bitmap?,
    description: String,
    onRemove: () -> Unit,
) {
    var bitmap by remember(file.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(file.absolutePath) { mutableStateOf(false) }
    LaunchedEffect(file.absolutePath) {
        val decoded = withContext(Dispatchers.IO) { loadBitmap(file) }
        if (decoded == null) failed = true else bitmap = decoded
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val shape = RoundedCornerShape(FlatDimens.Radius)
        val frame = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, FlatPalette.Divider, shape)
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = description,
                modifier = frame,
                contentScale = ContentScale.FillWidth,
            )
            failed -> FlatText(
                text = "无法显示$description",
                color = FlatPalette.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            else -> Box(frame.height(140.dp))
        }
        FlatTextButton(
            text = "移除",
            onClick = onRemove,
            color = FlatPalette.TextSecondary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

private fun blockTextStyle(align: ParagraphAlign, done: Boolean): TextStyle {
    return TextStyle(
        color = if (done) FlatPalette.TextSecondary else FlatPalette.TextPrimary,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        textAlign = when (align) {
            ParagraphAlign.Start -> TextAlign.Start
            ParagraphAlign.Center -> TextAlign.Center
            ParagraphAlign.End -> TextAlign.End
        },
        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
    )
}
