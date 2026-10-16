package com.flatnotes.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.data.Note
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFont
import com.flatnotes.data.ParagraphAlign
import com.flatnotes.data.alignOrNull
import com.flatnotes.data.fontOrNull
import com.flatnotes.data.plainText
import com.flatnotes.data.sampleNotes
import com.flatnotes.domain.EditResult
import com.flatnotes.domain.adjustMediaScale
import com.flatnotes.domain.commitTextChange
import com.flatnotes.domain.insertBlockAfter
import com.flatnotes.domain.removeBlock
import com.flatnotes.domain.removeEmptyBlock
import com.flatnotes.domain.setAlign
import com.flatnotes.domain.setFont
import com.flatnotes.domain.setMediaCaption
import com.flatnotes.domain.toggleChecklist
import com.flatnotes.ui.NotesViewModel
import com.flatnotes.ui.components.FlatConfirmDialog
import com.flatnotes.ui.components.FlatIconButton
import com.flatnotes.ui.components.FlatMenu
import com.flatnotes.ui.components.FlatMenuEntry
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.components.Hairline
import com.flatnotes.ui.icons.AppIcon
import com.flatnotes.ui.icons.FlatIcon
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette
import com.flatnotes.ui.theme.FlatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private enum class EditorTool(val label: String, val icon: AppIcon?) {
    Checklist("清单", AppIcon.Checklist),
    Image("图片", AppIcon.Image),
    Doodle("涂鸦", AppIcon.Pencil),
    Format("格式", null),
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
        mediaFile = { name -> viewModel.mediaFile(noteId, name) },
        loadBitmap = viewModel::decode,
        onTitleChange = { viewModel.updateTitle(noteId, it) },
        onBlocksChange = { viewModel.updateBlocks(noteId, it) },
        onImportImage = { uri -> viewModel.importImage(noteId, uri) },
        onSaveDoodle = { bitmap -> viewModel.saveDoodle(noteId, bitmap) },
        onDeleteMedia = { name -> viewModel.deleteMedia(noteId, name) },
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
    mediaFile: (String) -> File,
    loadBitmap: (File) -> Bitmap?,
    onTitleChange: (String) -> Unit,
    onBlocksChange: (List<NoteBlock>) -> Unit,
    onImportImage: (Uri) -> String?,
    onSaveDoodle: (Bitmap) -> String,
    onDeleteMedia: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var doodling by remember { mutableStateOf(false) }
    var formatOpen by remember { mutableStateOf(false) }
    var mediaMenuId by remember { mutableStateOf<String?>(null) }
    var captionEditing by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }
    var focusedId by remember(note.id) {
        mutableStateOf(note.blocks.firstOrNull { it.alignOrNull() != null }?.id)
    }
    var focusNonce by remember { mutableIntStateOf(0) }
    var pendingFocus by remember { mutableStateOf<String?>(null) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val titleFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    LaunchedEffect(note.id) {
        if (note.title.isEmpty() && note.isTypingBlank()) {
            runCatching { titleFocus.requestFocus() }
        }
    }
    LaunchedEffect(focusNonce) {
        val id = pendingFocus ?: return@LaunchedEffect
        kotlinx.coroutines.yield()
        runCatching { requesters[id]?.requestFocus() }
    }

    BackHandler(enabled = confirmDelete || doodling) {
        when {
            confirmDelete -> confirmDelete = false
            doodling -> doodling = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = withContext(Dispatchers.IO) { onImportImage(uri) }
            if (name == null) {
                banner = "无法读取这张图片"
            } else {
                banner = null
                insertMedia(note.blocks, focusedId, NoteBlock.Image(freshId(), name), onBlocksChange) {
                    focusedId = it
                }
            }
        }
    }

    fun apply(result: EditResult) {
        onBlocksChange(result.blocks)
        val next = result.focusId ?: return
        focusedId = next
        pendingFocus = next
        focusNonce += 1
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
                shareOpen = shareOpen,
                onBack = onBack,
                onShareOpen = {
                    menuOpen = false
                    shareOpen = true
                },
                onShareDismiss = { shareOpen = false },
                onShareText = { shareNote(context, note.title, note.plainText()) },
                onShareImage = {
                    scope.launch {
                        val rendered = runCatching {
                            withContext(Dispatchers.IO) {
                                renderNoteBitmap(note) { name -> loadBitmap(mediaFile(name)) }
                            }
                        }.getOrNull()
                        if (rendered == null) {
                            banner = "无法生成分享图片"
                        } else {
                            banner = null
                            val shared = runCatching { shareNoteImage(context, rendered) }
                            rendered.recycle()
                            if (shared.isFailure) banner = "无法分享图片"
                        }
                    }
                },
                onMenuOpen = {
                    shareOpen = false
                    menuOpen = true
                },
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
                    keyboardActions = KeyboardActions(onNext = {
                        note.blocks.firstOrNull { it.alignOrNull() != null }?.id?.let { id ->
                            pendingFocus = id
                            focusNonce += 1
                        }
                    }),
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
                Spacer(Modifier.height(12.dp))
                EditorBody(
                    blocks = note.blocks,
                    mediaFile = mediaFile,
                    loadBitmap = loadBitmap,
                    focusRequesters = requesters,
                    mediaMenuId = mediaMenuId,
                    captionEditing = captionEditing,
                    onFocus = { id ->
                        focusedId = id
                        mediaMenuId = null
                        captionEditing = false
                    },
                    onTextChange = { id, value ->
                        apply(commitTextChange(note.blocks, id, value, ::freshId))
                    },
                    onRemoveEmpty = { id ->
                        apply(removeEmptyBlock(note.blocks, id, ::freshId))
                    },
                    onToggle = { id -> onBlocksChange(toggleChecklist(note.blocks, id)) },
                    onMediaMenu = { id ->
                        if (mediaMenuId == id) {
                            mediaMenuId = null
                            captionEditing = false
                        } else {
                            mediaMenuId = id
                            captionEditing = false
                            focusedId = id
                        }
                    },
                    onEnlarge = { id ->
                        val next = adjustMediaScale(note.blocks, id, enlarge = true)
                        if (next != note.blocks) onBlocksChange(next)
                    },
                    onShrink = { id ->
                        val next = adjustMediaScale(note.blocks, id, enlarge = false)
                        if (next != note.blocks) onBlocksChange(next)
                    },
                    onCaptionEdit = { id ->
                        mediaMenuId = id
                        captionEditing = !captionEditing
                    },
                    onCaptionChange = { id, value ->
                        onBlocksChange(setMediaCaption(note.blocks, id, value))
                    },
                    onRemoveMedia = { block ->
                        val name = when (block) {
                            is NoteBlock.Image -> block.fileName
                            is NoteBlock.Doodle -> block.fileName
                            else -> null
                        }
                        if (name != null) {
                            if (mediaMenuId == block.id) {
                                mediaMenuId = null
                                captionEditing = false
                            }
                            onDeleteMedia(name)
                            val result = removeBlock(note.blocks, block.id, ::freshId)
                            onBlocksChange(result.blocks)
                            if (focusedId == block.id) focusedId = result.focusId
                        }
                    },
                    onAppendText = {
                        if (mediaMenuId != null) {
                            mediaMenuId = null
                            captionEditing = false
                        }
                        val last = note.blocks.lastOrNull()
                        if (last is NoteBlock.Text && last.text.isEmpty()) {
                            focusedId = last.id
                            pendingFocus = last.id
                            focusNonce += 1
                        } else {
                            apply(
                                insertBlockAfter(
                                    note.blocks,
                                    note.blocks.lastOrNull()?.id,
                                    NoteBlock.Text(freshId(), ""),
                                ),
                            )
                        }
                    },
                )
            }
            if (banner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FlatPalette.HintBackground)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FlatText(text = banner.orEmpty(), color = FlatPalette.HintText, fontSize = 13.sp)
                }
            }
            if (formatOpen) {
                val focused = note.blocks.firstOrNull { it.id == focusedId }
                FormatStrip(
                    align = focused?.alignOrNull(),
                    font = focused?.fontOrNull(),
                    onAlign = { align ->
                        val target = note.blocks.firstOrNull { it.id == focusedId && it.alignOrNull() != null }
                            ?: note.blocks.lastOrNull { it.alignOrNull() != null }
                        if (target == null) {
                            apply(insertBlockAfter(note.blocks, null, NoteBlock.Text(freshId(), "", align)))
                        } else {
                            onBlocksChange(setAlign(note.blocks, target.id, align))
                            focusedId = target.id
                        }
                    },
                    onFont = { font ->
                        val target = note.blocks.firstOrNull { it.id == focusedId && it.fontOrNull() != null }
                            ?: note.blocks.lastOrNull { it.fontOrNull() != null }
                        if (target == null) {
                            apply(insertBlockAfter(note.blocks, null, NoteBlock.Text(freshId(), "", font = font)))
                        } else {
                            onBlocksChange(setFont(note.blocks, target.id, font))
                            focusedId = target.id
                        }
                    },
                )
                Hairline()
            }
            EditorToolbar(
                formatOpen = formatOpen,
                onChecklist = {
                    banner = null
                    apply(
                        insertBlockAfter(
                            note.blocks,
                            focusedId,
                            NoteBlock.Checklist(freshId(), ""),
                        ),
                    )
                },
                onImage = {
                    banner = null
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onDoodle = {
                    banner = null
                    doodling = true
                },
                onFormat = { formatOpen = !formatOpen },
            )
        }
        if (confirmDelete) {
            FlatConfirmDialog(
                title = "删除笔记",
                message = "删除后无法恢复。",
                confirmLabel = "删除",
                onDismiss = { confirmDelete = false },
                onConfirm = onDelete,
            )
        }
        if (doodling) {
            DoodleBoard(
                onCancel = { doodling = false },
                onSave = { bitmap ->
                    doodling = false
                    scope.launch {
                        val name = withContext(Dispatchers.IO) { onSaveDoodle(bitmap) }
                        bitmap.recycle()
                        insertMedia(
                            blocks = note.blocks,
                            afterId = focusedId,
                            block = NoteBlock.Doodle(freshId(), name),
                            onBlocksChange = onBlocksChange,
                            onFocus = { focusedId = it },
                        )
                    }
                },
            )
        }
    }
}

private fun insertMedia(
    blocks: List<NoteBlock>,
    afterId: String?,
    block: NoteBlock,
    onBlocksChange: (List<NoteBlock>) -> Unit,
    onFocus: (String) -> Unit,
) {
    onBlocksChange(insertBlockAfter(blocks, afterId, block).blocks)
    onFocus(block.id)
}

private fun freshId(): String = UUID.randomUUID().toString()

private fun Note.isTypingBlank(): Boolean {
    return blocks.all { block ->
        when (block) {
            is NoteBlock.Text -> block.text.isBlank()
            else -> false
        }
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    menuOpen: Boolean,
    shareOpen: Boolean,
    onBack: () -> Unit,
    onShareOpen: () -> Unit,
    onShareDismiss: () -> Unit,
    onShareText: () -> Unit,
    onShareImage: () -> Unit,
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
        Box {
            FlatIconButton(icon = AppIcon.Share, contentDescription = "分享", onClick = onShareOpen)
            FlatMenu(
                expanded = shareOpen,
                onDismiss = onShareDismiss,
                entries = listOf(
                    FlatMenuEntry(label = "分享文字", onClick = onShareText),
                    FlatMenuEntry(label = "分享图片", onClick = onShareImage),
                ),
            )
        }
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
private fun FormatStrip(
    align: ParagraphAlign?,
    font: NoteFont?,
    onAlign: (ParagraphAlign) -> Unit,
    onFont: (NoteFont) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlatPalette.Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlignOption(AppIcon.AlignLeft, "左对齐", align == ParagraphAlign.Start) {
                onAlign(ParagraphAlign.Start)
            }
            AlignOption(AppIcon.AlignCenter, "居中", align == ParagraphAlign.Center) {
                onAlign(ParagraphAlign.Center)
            }
            AlignOption(AppIcon.AlignRight, "右对齐", align == ParagraphAlign.End) {
                onAlign(ParagraphAlign.End)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteFont.entries.forEach { option ->
                FontOption(
                    label = option.label(),
                    family = option.family(),
                    selected = font == option,
                    onClick = { onFont(option) },
                )
            }
        }
    }
}

@Composable
private fun FontOption(
    label: String,
    family: androidx.compose.ui.text.font.FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) FlatPalette.Accent else FlatPalette.IconMuted
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) FlatPalette.Accent else FlatPalette.Divider,
                shape = RoundedCornerShape(FlatDimens.Radius),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        FlatText(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontFamily = family,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AlignOption(
    icon: AppIcon,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) FlatPalette.Accent else FlatPalette.IconMuted
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) FlatPalette.Accent else FlatPalette.Divider,
                shape = RoundedCornerShape(FlatDimens.Radius),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlatIcon(icon = icon, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        FlatText(text = label, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun EditorToolbar(
    formatOpen: Boolean,
    onChecklist: () -> Unit,
    onImage: () -> Unit,
    onDoodle: () -> Unit,
    onFormat: () -> Unit,
) {
    val actions = listOf(
        EditorTool.Checklist to onChecklist,
        EditorTool.Image to onImage,
        EditorTool.Doodle to onDoodle,
        EditorTool.Format to onFormat,
    )
    Column(Modifier.background(FlatPalette.Toolbar)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { (item, action) ->
                val active = item == EditorTool.Format && formatOpen
                val tint = if (active) FlatPalette.Accent else FlatPalette.IconMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = item.label }
                        .clickable(onClick = action)
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

private fun shareNote(context: android.content.Context, title: String, body: String) {
    val payload = buildString {
        if (title.isNotBlank()) {
            append(title)
            append("\n\n")
        }
        append(body)
    }.ifBlank { "（空笔记）" }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title.ifBlank { "笔记" })
        putExtra(android.content.Intent.EXTRA_TEXT, payload)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "分享笔记"))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun EditorPreview() {
    FlatTheme {
        EditorScreen(
            note = sampleNotes().first(),
            mediaFile = { File(it) },
            loadBitmap = { null },
            onTitleChange = {},
            onBlocksChange = {},
            onImportImage = { null },
            onSaveDoodle = { "doodle.png" },
            onDeleteMedia = {},
            onDelete = {},
            onBack = {},
        )
    }
}
