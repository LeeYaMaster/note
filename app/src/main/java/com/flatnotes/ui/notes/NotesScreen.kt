@file:OptIn(ExperimentalFoundationApi::class)

package com.flatnotes.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.data.Note
import com.flatnotes.data.NotesLayout
import com.flatnotes.data.displayPreview
import com.flatnotes.data.plainText
import com.flatnotes.data.sampleNotes
import com.flatnotes.domain.formatNoteTime
import com.flatnotes.domain.groupNotes
import com.flatnotes.ui.NotesViewModel
import com.flatnotes.ui.components.FlatConfirmDialog
import com.flatnotes.ui.components.FlatFab
import com.flatnotes.ui.components.FlatIconButton
import com.flatnotes.ui.components.FlatMenu
import com.flatnotes.ui.components.FlatMenuEntry
import com.flatnotes.ui.components.FlatText
import com.flatnotes.ui.components.FlatTextButton
import com.flatnotes.ui.components.Hairline
import com.flatnotes.ui.icons.AppIcon
import com.flatnotes.ui.theme.FlatDimens
import com.flatnotes.ui.theme.FlatPalette
import com.flatnotes.ui.theme.FlatTheme
import com.flatnotes.ui.theme.toStyle

@Composable
fun NotesRoute(
    viewModel: NotesViewModel,
    onOpenNote: (String) -> Unit,
    onCreateNote: () -> Unit,
) {
    NotesScreen(
        notes = viewModel.notes,
        ready = viewModel.ready,
        layout = viewModel.layout,
        onLayoutChange = viewModel::updateLayout,
        onOpenNote = onOpenNote,
        onCreateNote = onCreateNote,
        onDeleteNotes = viewModel::deleteAll,
    )
}

@Composable
fun NotesScreen(
    notes: List<Note>,
    ready: Boolean = true,
    layout: NotesLayout,
    onLayoutChange: (NotesLayout) -> Unit,
    onOpenNote: (String) -> Unit,
    onCreateNote: () -> Unit,
    onDeleteNotes: (Set<String>) -> Unit,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nowMillis = System.currentTimeMillis()
    val visible = filterNotes(notes, query)

    BackHandler(enabled = selecting || searching) {
        when {
            selecting -> {
                selecting = false
                selected = emptySet()
                confirmDelete = false
            }
            searching -> {
                searching = false
                query = ""
            }
        }
    }

    fun openSelection(id: String) {
        searching = false
        query = ""
        menuOpen = false
        selecting = true
        selected = selected + id
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
            when {
                selecting -> SelectionBar(
                    count = selected.size,
                    onClose = {
                        selecting = false
                        selected = emptySet()
                    },
                    onDelete = { if (selected.isNotEmpty()) confirmDelete = true },
                )
                searching -> SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = {
                        searching = false
                        query = ""
                    },
                )
                else -> TitleBar(
                    menuOpen = menuOpen,
                    layout = layout,
                    onSearch = { searching = true },
                    onMenuOpen = { menuOpen = true },
                    onMenuDismiss = { menuOpen = false },
                    onLayoutChange = onLayoutChange,
                )
            }
            Hairline()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (!ready) {
                    Box(Modifier.fillMaxSize())
                } else if (visible.isEmpty()) {
                    EmptyHint(message = if (query.isNotBlank()) "没有找到相关内容" else "暂无笔记")
                } else when (layout) {
                    NotesLayout.List -> NotesList(
                        notes = visible,
                        nowMillis = nowMillis,
                        selecting = selecting,
                        selected = selected,
                        onClick = { id ->
                            if (selecting) {
                                selected = if (id in selected) selected - id else selected + id
                            } else {
                                onOpenNote(id)
                            }
                        },
                        onLongClick = ::openSelection,
                    )
                    NotesLayout.Waterfall -> NotesWaterfall(
                        notes = visible,
                        nowMillis = nowMillis,
                        selecting = selecting,
                        selected = selected,
                        onClick = { id ->
                            if (selecting) {
                                selected = if (id in selected) selected - id else selected + id
                            } else {
                                onOpenNote(id)
                            }
                        },
                        onLongClick = ::openSelection,
                    )
                }
                if (ready && !selecting) {
                    FlatFab(
                        onClick = onCreateNote,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp),
                    )
                }
            }
        }
        if (confirmDelete) {
            FlatConfirmDialog(
                title = if (selected.size == 1) "删除这条笔记？" else "删除 ${selected.size} 条笔记？",
                message = "删除后无法恢复。",
                confirmLabel = "删除",
                onDismiss = { confirmDelete = false },
                onConfirm = {
                    onDeleteNotes(selected)
                    confirmDelete = false
                    selecting = false
                    selected = emptySet()
                },
            )
        }
    }
}

@Composable
private fun TitleBar(
    menuOpen: Boolean,
    layout: NotesLayout,
    onSearch: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onLayoutChange: (NotesLayout) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlatText(
            text = "笔记",
            modifier = Modifier.weight(1f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        FlatIconButton(icon = AppIcon.Search, contentDescription = "搜索", onClick = onSearch)
        Box {
            FlatIconButton(icon = AppIcon.More, contentDescription = "更多", onClick = onMenuOpen)
            FlatMenu(
                expanded = menuOpen,
                onDismiss = onMenuDismiss,
                entries = listOf(
                    FlatMenuEntry(
                        label = "列表视图",
                        checked = layout == NotesLayout.List,
                        onClick = { onLayoutChange(NotesLayout.List) },
                    ),
                    FlatMenuEntry(
                        label = "瀑布流视图",
                        checked = layout == NotesLayout.Waterfall,
                        onClick = { onLayoutChange(NotesLayout.Waterfall) },
                    ),
                ),
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlatTextButton(text = "取消", onClick = onClose, color = FlatPalette.TextSecondary)
        FlatText(
            text = "已选择 $count",
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        FlatTextButton(
            text = "删除",
            onClick = onDelete,
            color = if (count == 0) FlatPalette.Placeholder else FlatPalette.Destructive,
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlatIconButton(icon = AppIcon.Back, contentDescription = "返回", onClick = onClose)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = FlatPalette.TextPrimary, fontSize = 18.sp),
            singleLine = true,
            cursorBrush = SolidColor(FlatPalette.Accent),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        FlatText(text = "搜索", color = FlatPalette.Placeholder, fontSize = 18.sp)
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            FlatTextButton(text = "清除", onClick = { onQueryChange("") })
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    nowMillis: Long,
    selecting: Boolean,
    selected: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
) {
    val groups = groupNotes(notes, nowMillis)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        groups.forEach { group ->
            item(key = "header-${group.bucket.name}") {
                FlatText(
                    text = group.bucket.label,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                    color = FlatPalette.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            items(group.notes, key = { it.id }) { note ->
                Column {
                    NoteRow(
                        note = note,
                        nowMillis = nowMillis,
                        selecting = selecting,
                        selected = note.id in selected,
                        onClick = { onClick(note.id) },
                        onLongClick = { onLongClick(note.id) },
                    )
                    Hairline()
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    nowMillis: Long,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFFFF6D8) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            SelectionMark(selected)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlatText(
                    text = note.title.ifBlank { "无标题" },
                    modifier = Modifier.weight(1f),
                    color = if (note.title.isBlank()) FlatPalette.Placeholder else FlatPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.width(12.dp))
                FlatText(
                    text = formatNoteTime(note.updatedAt, nowMillis),
                    color = FlatPalette.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            val preview = note.displayPreview()
            if (preview.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                FlatText(
                    text = preview,
                    color = FlatPalette.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NotesWaterfall(
    notes: List<Note>,
    nowMillis: Long,
    selecting: Boolean,
    selected: Set<String>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FlatDimens.GridPadding,
            end = FlatDimens.GridPadding,
            top = FlatDimens.GridPadding,
            bottom = 96.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(FlatDimens.GridGap),
        verticalItemSpacing = FlatDimens.GridGap,
    ) {
        items(notes, key = { it.id }) { note ->
            WaterfallCard(
                note = note,
                nowMillis = nowMillis,
                selecting = selecting,
                selected = note.id in selected,
                onClick = { onClick(note.id) },
                onLongClick = { onLongClick(note.id) },
            )
        }
    }
}

@Composable
private fun WaterfallCard(
    note: Note,
    nowMillis: Long,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tint = note.tint.toStyle()
    val shape = RoundedCornerShape(FlatDimens.Radius)
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) FlatPalette.Accent else tint.border,
                    shape = shape,
                )
                .background(tint.background)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            FlatText(
                text = note.title.ifBlank { "无标题" },
                color = if (note.title.isBlank()) FlatPalette.Placeholder else FlatPalette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            val preview = note.displayPreview()
            if (preview.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlatText(
                    text = preview,
                    color = FlatPalette.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                )
            }
            Spacer(Modifier.height(10.dp))
            FlatText(
                text = formatNoteTime(note.updatedAt, nowMillis),
                modifier = Modifier.align(Alignment.End),
                color = FlatPalette.TextSecondary,
                fontSize = 12.sp,
            )
        }
        if (selecting) {
            SelectionMark(
                selected = selected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val accent = FlatPalette.Accent
    val idle = FlatPalette.TextSecondary
    Canvas(
        modifier
            .size(22.dp)
            .semantics { contentDescription = if (selected) "已选择" else "未选择" },
    ) {
        val stroke = 1.5.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - stroke / 2f
        if (selected) {
            drawCircle(color = accent, radius = radius, center = center)
        } else {
            drawCircle(color = Color.White, radius = radius, center = center)
        }
        drawCircle(
            color = if (selected) accent else idle,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )
        if (selected) {
            val path = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.68f)
                lineTo(size.width * 0.73f, size.height * 0.34f)
            }
            drawPath(
                path,
                Color.White,
                style = Stroke(
                    width = size.minDimension * 0.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun EmptyHint(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FlatText(text = message, color = FlatPalette.TextSecondary, fontSize = 15.sp)
    }
}

private fun filterNotes(notes: List<Note>, query: String): List<Note> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return notes
    return notes.filter { note ->
        note.title.contains(keyword, ignoreCase = true) ||
            note.displayPreview().contains(keyword, ignoreCase = true) ||
            note.plainText().contains(keyword, ignoreCase = true)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun NotesListPreview() {
    FlatTheme {
        NotesScreen(
            notes = sampleNotes(),
            layout = NotesLayout.List,
            onLayoutChange = {},
            onOpenNote = {},
            onCreateNote = {},
            onDeleteNotes = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun NotesWaterfallPreview() {
    FlatTheme {
        NotesScreen(
            notes = sampleNotes(),
            layout = NotesLayout.Waterfall,
            onLayoutChange = {},
            onOpenNote = {},
            onCreateNote = {},
            onDeleteNotes = {},
        )
    }
}
