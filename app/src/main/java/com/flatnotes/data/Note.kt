package com.flatnotes.data

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

enum class ParagraphAlign {
    Start,
    Center,
    End,
}

sealed interface NoteBlock {
    val id: String

    data class Text(
        override val id: String,
        val text: String,
        val align: ParagraphAlign = ParagraphAlign.Start,
    ) : NoteBlock

    data class Checklist(
        override val id: String,
        val text: String,
        val done: Boolean = false,
        val align: ParagraphAlign = ParagraphAlign.Start,
    ) : NoteBlock

    data class Image(
        override val id: String,
        val fileName: String,
    ) : NoteBlock

    data class Doodle(
        override val id: String,
        val fileName: String,
    ) : NoteBlock
}

data class Note(
    val id: String,
    val title: String,
    val blocks: List<NoteBlock>,
    val preview: String = "",
    val updatedAt: Long,
    val tint: NoteTint,
)

fun NoteBlock.alignOrNull(): ParagraphAlign? = when (this) {
    is NoteBlock.Text -> align
    is NoteBlock.Checklist -> align
    is NoteBlock.Image, is NoteBlock.Doodle -> null
}

fun Note.displayPreview(): String {
    if (preview.isNotBlank()) return preview
    blocks.forEach { block ->
        when (block) {
            is NoteBlock.Text -> if (block.text.isNotBlank()) return block.text.trim()
            is NoteBlock.Checklist -> if (block.text.isNotBlank()) return block.text.trim()
            is NoteBlock.Image, is NoteBlock.Doodle -> Unit
        }
    }
    return when {
        blocks.any { it is NoteBlock.Image } -> "[图片]"
        blocks.any { it is NoteBlock.Doodle } -> "[涂鸦]"
        else -> ""
    }
}

fun Note.plainText(): String = blocks.joinToString("\n") { block ->
    when (block) {
        is NoteBlock.Text -> block.text
        is NoteBlock.Checklist -> if (block.done) "✅ ${block.text}" else "☐ ${block.text}"
        is NoteBlock.Image -> "[图片]"
        is NoteBlock.Doodle -> "[涂鸦]"
    }
}

fun Note.isContentBlank(): Boolean {
    if (title.isNotBlank()) return false
    return blocks.all { block ->
        when (block) {
            is NoteBlock.Text -> block.text.isBlank()
            is NoteBlock.Checklist, is NoteBlock.Image, is NoteBlock.Doodle -> false
        }
    }
}
