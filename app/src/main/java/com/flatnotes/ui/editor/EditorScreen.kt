package com.flatnotes.ui.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.data.Note
import com.flatnotes.data.sampleNotes
import com.flatnotes.domain.insertChecklistLine
import com.flatnotes.ui.NotesViewModel
import com.flatnotes.ui.components.FlatIconButton
import com.flatnotes.ui.components.FlatMenu
import com.flatnotes.ui.components.FlatMenuEntry
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.components.FlatTextButton
import com.flatnotes.ui.components.Hairline
import com.flatnotes.ui.icons.AppIcon
import com.flatnotes.ui.icons.FlatIcon
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette
import com.flatnotes.ui.theme.FlatTheme

private enum class EditorTool(val label: String, val icon: AppIcon?) {
    Checklist("清单", AppIcon.Checklist),
    Image("图片", AppIcon.Image),
    Voice("语音", AppIcon.Mic),
    Doodle("涂鸦", AppIcon.Pencil),
    Format("格式", null),
}

private enum class BodySize(val label: String, val fontSize: TextUnit) {
    Small("小", 14.sp),
    Normal("标准", 16.sp),
    Large("大", 20.sp),
}

@Composable
fun EditorRoute(
    viewModel: NotesViewModel,
    noteId: String,
    onBack: () -> Unit,
) {
    val note = viewModel.note(noteId)
    DisposableEffect(noteId) {
        onDispose { viewModel.discardIfBlank(noteId) }
    }
    LaunchedEffect(noteId) {
        if (viewModel.note(noteId) == null) onBack()
    }
    if (note == null) return
    EditorScreen(
        note = note,
        onTitleChange = { viewModel.update(noteId, title = it, body = note.body) },
        onBodyChange = { viewModel.update(noteId, title = note.title, body = it) },
        onDelete = {
            viewModel.delete(noteId)
            onBack()
        },
        onBack = onBack,
    )
}

@Composable
fun EditorScreen(
    note: Note,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf<EditorTool?>(null) }
    var bold by remember { mutableStateOf(false) }
    var bodySize by remember { mutableStateOf(BodySize.Normal) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    LaunchedEffect(note.id) {
        if (note.title.isEmpty() && note.body.isEmpty()) {
            titleFocus.requestFocus()
        }
    }

    BackHandler(enabled = confirmDelete) { confirmDelete = false }

    val hint = when (tool) {
        EditorTool.Checklist -> "已在正文末尾插入清单"
        EditorTool.Image -> "图片功能即将推出"
        EditorTool.Voice -> "语音功能即将推出"
        EditorTool.Doodle -> "涂鸦功能即将推出"
        EditorTool.Format, null -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlatPalette.Background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime)),
        ) {
            EditorTopBar(
                title = note.title,
                menuOpen = menuOpen,
                onBack = onBack,
                onShare = {
                    shareNote(context, note.title, note.body)
                },
                onMenuOpen = { menuOpen = true },
                onMenuDismiss = { menuOpen = false },
                onDelete = { confirmDelete = true },
            )
            Hairline()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                BasicTextField(
                    value = note.title,
                    onValueChange = { onTitleChange(it.replace("\n", "")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus),
                    textStyle = TextStyle(
                        color = FlatPalette.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(FlatPalette.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                    decorationBox = { inner ->
                        Box {
                            if (note.title.isEmpty()) {
                                FlatText(
                                    text = "标题",
                                    color = FlatPalette.Placeholder,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = note.body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 240.dp)
                        .focusRequester(bodyFocus),
                    textStyle = TextStyle(
                        color = FlatPalette.TextPrimary,
                        fontSize = bodySize.fontSize,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = bodySize.fontSize * 1.6,
                    ),
                    cursorBrush = SolidColor(FlatPalette.Accent),
                    decorationBox = { inner ->
                        Box {
                            if (note.body.isEmpty()) {
                                FlatText(
                                    text = "开始输入",
                                    color = FlatPalette.Placeholder,
                                    fontSize = bodySize.fontSize,
                                    lineHeight = bodySize.fontSize * 1.6,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (hint != null) {
                HintBar(hint)
            }
            if (tool == EditorTool.Format) {
                FormatStrip(
                    bold = bold,
                    size = bodySize,
                    onBold = { bold = !bold },
                    onSize = { bodySize = it },
                )
                Hairline()
            }
            EditorToolbar(
                selected = tool,
                onSelect = { next ->
                    if (next == EditorTool.Checklist) {
                        tool = next
                        onBodyChange(insertChecklistLine(note.body))
                    } else {
                        tool = if (tool == next) null else next
                    }
                },
            )
        }
        if (confirmDelete) {
            DeleteDialog(
                onDismiss = { confirmDelete = false },
                onConfirm = onDelete,
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    menuOpen: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlatIconButton(icon = AppIcon.Back, contentDescription = "返回", onClick = onBack)
        FlatText(
            text = title.ifBlank { "无标题" },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            color = if (title.isBlank()) FlatPalette.Placeholder else FlatPalette.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        FlatIconButton(icon = AppIcon.Share, contentDescription = "分享", onClick = onShare)
        Box {
            FlatIconButton(icon = AppIcon.More, contentDescription = "更多", onClick = onMenuOpen)
            FlatMenu(
                expanded = menuOpen,
                onDismiss = onMenuDismiss,
                entries = listOf(
                    FlatMenuEntry(
                        label = "删除",
                        color = FlatPalette.Destructive,
                        onClick = onDelete,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun HintBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlatPalette.HintBackground)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        FlatText(text = text, color = FlatPalette.HintText, fontSize = 13.sp)
    }
}

@Composable
private fun FormatStrip(
    bold: Boolean,
    size: BodySize,
    onBold: () -> Unit,
    onSize: (BodySize) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlatPalette.Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormatChip(label = "粗体", selected = bold, onClick = onBold)
        BodySize.entries.forEach { option ->
            FormatChip(label = option.label, selected = size == option, onClick = { onSize(option) })
        }
    }
}

@Composable
private fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) FlatPalette.Accent else FlatPalette.IconMuted
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
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EditorToolbar(selected: EditorTool?, onSelect: (EditorTool) -> Unit) {
    Column(Modifier.background(FlatPalette.Toolbar)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorTool.entries.forEach { item ->
                val active = item == selected
                val tint = if (active) FlatPalette.Accent else FlatPalette.IconMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = item.label }
                        .clickable { onSelect(item) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        val icon = item.icon
                        if (icon == null) {
                            FlatText(
                                text = "Aa",
                                color = tint,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        } else {
                            FlatIcon(icon = icon, tint = tint, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    FlatText(text = item.label, color = tint, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DeleteDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
            FlatText(text = "删除笔记", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlatText(text = "删除后无法恢复。", color = FlatPalette.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                FlatTextButton(text = "取消", onClick = onDismiss, color = FlatPalette.TextSecondary)
                FlatTextButton(text = "删除", onClick = onConfirm, color = FlatPalette.Destructive)
            }
        }
    }
}

private fun shareNote(context: android.content.Context, title: String, body: String) {
    val payload = buildString {
        if (title.isNotBlank()) {
            append(title)
            append("\n\n")
        }
        append(body)
    }.ifBlank { "（空笔记）" }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "笔记" })
        putExtra(Intent.EXTRA_TEXT, payload)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "分享笔记"))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun EditorPreview() {
    FlatTheme {
        EditorScreen(
            note = sampleNotes().first(),
            onTitleChange = {},
            onBodyChange = {},
            onDelete = {},
            onBack = {},
        )
    }
}
