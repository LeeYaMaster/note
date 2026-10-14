package com.flatnotes.data

enum class NoteKind {
    Note,
    Todo,
}

enum class NoteTint {
    Yellow,
    Blue,
    Green,
    White,
}

enum class NotesLayout {
    List,
    Waterfall,
}

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val preview: String = "",
    val updatedAt: Long,
    val tint: NoteTint,
    val kind: NoteKind,
)

fun Note.displayPreview(): String {
    if (preview.isNotBlank()) return preview
    return body
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
}
