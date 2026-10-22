package com.flatnotes.domain

import com.flatnotes.data.NoteBlock
import kotlin.math.roundToInt

data class EditorSpacing(
    val body: Int = BODY_DEFAULT,
    val checklist: Int = CHECKLIST_DEFAULT,
    val block: Int = BLOCK_DEFAULT,
) {
    fun clamped(): EditorSpacing = EditorSpacing(
        body = body.coerceIn(BODY_RANGE),
        checklist = checklist.coerceIn(CHECKLIST_RANGE),
        block = block.coerceIn(BLOCK_RANGE),
    )

    companion object {
        const val BODY_DEFAULT = 0
        const val CHECKLIST_DEFAULT = 0
        const val BLOCK_DEFAULT = 22
        val BODY_RANGE = 0..24
        val CHECKLIST_RANGE = 0..24
        val BLOCK_RANGE = 8..40
        val Default = EditorSpacing()
    }
}

fun blockGapDp(previous: NoteBlock, current: NoteBlock, spacing: EditorSpacing): Int {
    val resolved = spacing.clamped()
    return when {
        previous is NoteBlock.Text && current is NoteBlock.Text -> resolved.body
        previous is NoteBlock.Checklist && current is NoteBlock.Checklist -> resolved.checklist
        else -> resolved.block
    }
}

fun spacingValueAt(x: Float, width: Float, range: IntRange): Int {
    if (width <= 0f || range.isEmpty()) return range.first
    val fraction = (x / width).coerceIn(0f, 1f)
    val span = range.last - range.first
    return (range.first + fraction * span).roundToInt().coerceIn(range)
}
