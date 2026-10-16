package com.flatnotes.domain

import com.flatnotes.data.NoteBlock
import com.flatnotes.data.ParagraphAlign

data class EditResult(
    val blocks: List<NoteBlock>,
    val focusId: String? = null,
)

fun commitTextChange(
    blocks: List<NoteBlock>,
    id: String,
    raw: String,
    newId: () -> String,
): EditResult {
    val index = blocks.indexOfFirst { it.id == id }
    if (index < 0) return EditResult(blocks)
    val current = blocks[index]
    if (current !is NoteBlock.Text && current !is NoteBlock.Checklist) return EditResult(blocks)
    if ('\n' !in raw) {
        val updated = blocks.toMutableList()
        updated[index] = current.withText(raw)
        return EditResult(updated)
    }
    val created = mutableListOf<NoteBlock>()
    var focus: String? = null
    raw.split('\n').forEachIndexed { partIndex, part ->
        if (partIndex == 0) {
            created += current.withText(part)
        } else {
            val spawnedId = newId()
            created += current.spawn(spawnedId, part)
            focus = spawnedId
        }
    }
    val updated = blocks.toMutableList()
    updated.removeAt(index)
    updated.addAll(index, created)
    return EditResult(updated, focus)
}

fun toggleChecklist(blocks: List<NoteBlock>, id: String): List<NoteBlock> {
    return blocks.map { block ->
        if (block is NoteBlock.Checklist && block.id == id) block.copy(done = !block.done) else block
    }
}

fun setAlign(blocks: List<NoteBlock>, id: String, align: ParagraphAlign): List<NoteBlock> {
    return blocks.map { block ->
        when {
            block.id != id -> block
            block is NoteBlock.Text -> block.copy(align = align)
            block is NoteBlock.Checklist -> block.copy(align = align)
            else -> block
        }
    }
}

fun insertBlockAfter(blocks: List<NoteBlock>, afterId: String?, block: NoteBlock): EditResult {
    if (afterId == null) return EditResult(blocks + block, block.id)
    val index = blocks.indexOfFirst { it.id == afterId }
    if (index < 0) return EditResult(blocks + block, block.id)
    val updated = blocks.toMutableList()
    updated.add(index + 1, block)
    return EditResult(updated, block.id)
}

fun removeEmptyBlock(
    blocks: List<NoteBlock>,
    id: String,
    newId: () -> String,
): EditResult {
    val index = blocks.indexOfFirst { it.id == id }
    if (index < 0) return EditResult(blocks)
    val current = blocks[index]
    val empty = when (current) {
        is NoteBlock.Text -> current.text.isEmpty()
        is NoteBlock.Checklist -> current.text.isEmpty()
        else -> return EditResult(blocks)
    }
    if (!empty) return EditResult(blocks)
    if (blocks.size == 1) {
        val fresh = NoteBlock.Text(id = newId(), text = "")
        return EditResult(listOf(fresh), fresh.id)
    }
    val updated = blocks.toMutableList()
    updated.removeAt(index)
    val focus = updated.getOrNull((index - 1).coerceAtLeast(0))?.id
    return EditResult(updated, focus)
}

fun removeBlock(blocks: List<NoteBlock>, id: String, newId: () -> String): EditResult {
    val updated = blocks.filterNot { it.id == id }
    if (updated.isEmpty()) {
        val fresh = NoteBlock.Text(id = newId(), text = "")
        return EditResult(listOf(fresh), fresh.id)
    }
    return EditResult(updated)
}

private fun NoteBlock.withText(text: String): NoteBlock = when (this) {
    is NoteBlock.Text -> copy(text = text)
    is NoteBlock.Checklist -> copy(text = text)
    is NoteBlock.Image, is NoteBlock.Doodle -> this
}

private fun NoteBlock.spawn(id: String, text: String): NoteBlock = when (this) {
    is NoteBlock.Text -> NoteBlock.Text(id = id, text = text, align = align)
    is NoteBlock.Checklist -> NoteBlock.Checklist(id = id, text = text, done = false, align = align)
    is NoteBlock.Image, is NoteBlock.Doodle -> NoteBlock.Text(id = id, text = text)
}
