package com.flatnotes.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flatnotes.data.Note
import com.flatnotes.data.NoteKind
import com.flatnotes.data.NotesLayout
import com.flatnotes.data.displayPreview
import com.flatnotes.data.sampleNotes
import com.flatnotes.domain.formatNoteTime
import com.flatnotes.domain.groupNotes
import com.flatnotes.ui.NotesViewModel
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

private enum class HomeTab(val label: String) {
    Notes("笔记"),
    Todos("待办"),
}

@Composable
fun NotesRoute(
    viewModel: NotesViewModel,
    onOpenNote: (String) -> Unit,
    onCreateNote: (NoteKind) -> Unit,
) {
    NotesScreen(
        notes = viewModel.notes,
        layout = viewModel.layout,
        onLayoutChange = viewModel::updateLayout,
        onOpenNote = onOpenNote,
        onCreateNote = onCreateNote,
    )
}

@Composable
fun NotesScreen(
    notes: List<Note>,
    layout: NotesLayout,
    onLayoutChange: (NotesLayout) -> Unit,
    onOpenNote: (String) -> Unit,
    onCreateNote: (NoteKind) -> Unit,
) {
    var tab by remember { mutableStateOf(HomeTab.Notes) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val nowMillis = System.currentTimeMillis()
    val visible = filterNotes(notes, tab, query)

    BackHandler(enabled = searching) {
        searching = false
        query = ""
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
            if (searching) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = {
                        searching = false
                        query = ""
                    },
                )
            } else {
                TitleBar(
                    menuOpen = menuOpen,
                    layout = layout,
                    onSearch = { searching = true },
                    onMenuOpen = { menuOpen = true },
                    onMenuDismiss = { menuOpen = false },
                    onLayoutChange = onLayoutChange,
                )
            }
            Hairline()
            TabRow(selected = tab, onSelect = { tab = it })
            Hairline()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (visible.isEmpty()) {
                    EmptyHint(message = emptyMessage(tab, query))
                } else when (layout) {
                    NotesLayout.List -> NotesList(
                        notes = visible,
                        nowMillis = nowMillis,
                        onOpenNote = onOpenNote,
                    )
                    NotesLayout.Waterfall -> NotesWaterfall(
                        notes = visible,
                        nowMillis = nowMillis,
                        onOpenNote = onOpenNote,
                    )
                }
                FlatFab(
                    onClick = {
                        val kind = if (tab == HomeTab.Todos) NoteKind.Todo else NoteKind.Note
                        onCreateNote(kind)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp),
                )
            }
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
private fun TabRow(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        HomeTab.entries.forEachIndexed { index, tab ->
            if (index > 0) Spacer(Modifier.width(28.dp))
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val active = tab == selected
                FlatText(
                    text = tab.label,
                    color = if (active) FlatPalette.TextPrimary else FlatPalette.TabInactive,
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .fillMaxWidth()
                        .background(if (active) FlatPalette.Accent else FlatPalette.Background),
                )
            }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    nowMillis: Long,
    onOpenNote: (String) -> Unit,
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
                    NoteRow(note = note, nowMillis = nowMillis, onClick = { onOpenNote(note.id) })
                    Hairline()
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, nowMillis: Long, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
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

@Composable
private fun NotesWaterfall(
    notes: List<Note>,
    nowMillis: Long,
    onOpenNote: (String) -> Unit,
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
            WaterfallCard(note = note, nowMillis = nowMillis, onClick = { onOpenNote(note.id) })
        }
    }
}

@Composable
private fun WaterfallCard(note: Note, nowMillis: Long, onClick: () -> Unit) {
    val tint = note.tint.toStyle()
    val shape = RoundedCornerShape(FlatDimens.Radius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, tint.border, shape)
            .background(tint.background)
            .clickable(onClick = onClick)
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
}

@Composable
private fun EmptyHint(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FlatText(text = message, color = FlatPalette.TextSecondary, fontSize = 15.sp)
    }
}

private fun filterNotes(notes: List<Note>, tab: HomeTab, query: String): List<Note> {
    val byTab = when (tab) {
        HomeTab.Notes -> notes
        HomeTab.Todos -> notes.filter { it.kind == NoteKind.Todo }
    }
    val keyword = query.trim()
    if (keyword.isEmpty()) return byTab
    return byTab.filter { note ->
        note.title.contains(keyword, ignoreCase = true) ||
            note.displayPreview().contains(keyword, ignoreCase = true) ||
            note.body.contains(keyword, ignoreCase = true)
    }
}

private fun emptyMessage(tab: HomeTab, query: String): String {
    if (query.isNotBlank()) return "没有找到相关内容"
    return if (tab == HomeTab.Todos) "暂无待办" else "暂无笔记"
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
        )
    }
}
