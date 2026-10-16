package com.flatnotes.domain

import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFont
import com.flatnotes.data.ParagraphAlign
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteDocumentTest {
    @Test
    fun enterSplitsTheCurrentLineAndKeepsAlignment() {
        val blocks = listOf(
            NoteBlock.Text("a", "你好世界", ParagraphAlign.Center),
        )
        val result = commitTextChange(blocks, "a", "你好\n世界") { "b" }
        assertEquals(
            listOf(
                NoteBlock.Text("a", "你好", ParagraphAlign.Center),
                NoteBlock.Text("b", "世界", ParagraphAlign.Center),
            ),
            result.blocks,
        )
        assertEquals("b", result.focusId)
    }

    @Test
    fun enterInsideChecklistStartsANewUncheckedItem() {
        val blocks = listOf(NoteBlock.Checklist("a", "牛奶", done = true, align = ParagraphAlign.End))
        val result = commitTextChange(blocks, "a", "牛奶\n面包") { "b" }
        assertEquals(
            listOf(
                NoteBlock.Checklist("a", "牛奶", done = true, align = ParagraphAlign.End),
                NoteBlock.Checklist("b", "面包", done = false, align = ParagraphAlign.End),
            ),
            result.blocks,
        )
    }

    @Test
    fun toggleAndAlignTouchOnlyTheTargetLine() {
        val blocks = listOf(
            NoteBlock.Checklist("a", "西红柿"),
            NoteBlock.Text("b", "备注", ParagraphAlign.Start),
        )
        val toggled = toggleChecklist(blocks, "a")
        assertEquals(true, (toggled[0] as NoteBlock.Checklist).done)
        assertEquals(blocks[1], toggled[1])
        val aligned = setAlign(toggled, "b", ParagraphAlign.Center)
        assertEquals(ParagraphAlign.Center, (aligned[1] as NoteBlock.Text).align)
        assertEquals(toggled[0], aligned[0])
    }

    @Test
    fun checklistInsertsAfterTheFocusedLine() {
        val blocks = listOf(NoteBlock.Text("a", "标题"))
        val result = insertBlockAfter(blocks, "a", NoteBlock.Checklist("b", ""))
        assertEquals(listOf("a", "b"), result.blocks.map { it.id })
        assertEquals("b", result.focusId)
    }

    @Test
    fun newLineKeepsTheTypeface() {
        val blocks = listOf(NoteBlock.Text("a", "甲", font = NoteFont.Handwriting))
        val result = commitTextChange(blocks, "a", "甲\n乙") { "b" }
        assertEquals(NoteFont.Handwriting, (result.blocks[0] as NoteBlock.Text).font)
        assertEquals(NoteFont.Handwriting, (result.blocks[1] as NoteBlock.Text).font)
    }

    @Test
    fun fontAndScaleStayOnTheTargetBlock() {
        val blocks = listOf(
            NoteBlock.Text("a", "正文", font = NoteFont.Default),
            NoteBlock.Image("b", "photo.jpg", scale = 1f, caption = ""),
        )
        val fonted = setFont(blocks, "a", NoteFont.Serif)
        assertEquals(NoteFont.Serif, (fonted[0] as NoteBlock.Text).font)
        assertEquals(blocks[1], fonted[1])
        val shrunk = adjustMediaScale(fonted, "b", enlarge = false)
        assertEquals(0.72f, (shrunk[1] as NoteBlock.Image).scale)
        val smallest = adjustMediaScale(adjustMediaScale(shrunk, "b", enlarge = false), "b", enlarge = false)
        assertEquals(0.45f, (smallest[1] as NoteBlock.Image).scale)
        val captioned = setMediaCaption(smallest, "b", "门口")
        assertEquals("门口", (captioned[1] as NoteBlock.Image).caption)
        val enlarged = adjustMediaScale(captioned, "b", enlarge = true)
        assertEquals(0.72f, (enlarged[1] as NoteBlock.Image).scale)
        assertEquals("门口", (enlarged[1] as NoteBlock.Image).caption)
    }

    @Test
    fun removingAnEmptyLineFocusesThePreviousOne() {
        val blocks = listOf(
            NoteBlock.Text("a", "留下"),
            NoteBlock.Text("b", ""),
        )
        val result = removeEmptyBlock(blocks, "b") { "fresh" }
        assertEquals(listOf("a"), result.blocks.map { it.id })
        assertEquals("a", result.focusId)
    }
}