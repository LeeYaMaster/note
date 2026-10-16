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

enum class NoteFont {
    Default,
    Serif,
    Monospace,
    Handwriting,
}

sealed interface NoteBlock {
    val id: String

    data class Text(
        override val id: String,
        val text: String,
        val align: ParagraphAlign = ParagraphAlign.Start,
        val font: NoteFont = NoteFont.Default,
    ) : NoteBlock

    data class Checklist(
        override val id: String,
        val text: String,
        val done: Boolean = false,
        val align: ParagraphAlign = ParagraphAlign.Start,
        val font: NoteFont = NoteFont.Default,
    ) : NoteBlock

    data class Image(
        override val id: String,
        val fileName: String,
        val scale: Float = 1f,
        val caption: String = "",
    ) : NoteBlock

    data class Doodle(
        override val id: String,
        val fileName: String,
        val scale: Float = 1f,
        val caption: String = "",
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

fun NoteBlock.fontOrNull(): NoteFont? = when (this) {
    is NoteBlock.Text -> font
    is NoteBlock.Checklist -> font
    is NoteBlock.Image, is NoteBlock.Doodle -> null
}

fun Note.displayPreview(): String {
    if (preview.isNotBlank()) return preview
    var caption: String? = null
    blocks.forEach { block ->
        when (block) {
            is NoteBlock.Text -> if (block.text.isNotBlank()) return block.text.trim()
            is NoteBlock.Checklist -> if (block.text.isNotBlank()) return block.text.trim()
            is NoteBlock.Image -> if (caption == null && block.caption.isNotBlank()) caption = block.caption.trim()
            is NoteBlock.Doodle -> if (caption == null && block.caption.isNotBlank()) caption = block.caption.trim()
        }
    }
    if (caption != null) return caption
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
        is NoteBlock.Image -> if (block.caption.isBlank()) "[图片]" else "[图片] ${block.caption}"
        is NoteBlock.Doodle -> if (block.caption.isBlank()) "[涂鸦]" else "[涂鸦] ${block.caption}"
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
