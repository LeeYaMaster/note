package com.flatnotes.domain

import com.flatnotes.data.NoteBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorSpacingTest {
    @Test
    fun defaultsMatchTheCurrentRhythm() {
        val spacing = EditorSpacing.Default
        assertEquals(0, spacing.body)
        assertEquals(0, spacing.checklist)
        assertEquals(22, spacing.block)
    }

    @Test
    fun outOfRangeValuesClamp() {
        val spacing = EditorSpacing(body = -4, checklist = 80, block = 3).clamped()
        assertEquals(0, spacing.body)
        assertEquals(24, spacing.checklist)
        assertEquals(8, spacing.block)
    }

    @Test
    fun gapsFollowTheThreeControls() {
        val spacing = EditorSpacing(body = 6, checklist = 10, block = 22)
        val text = NoteBlock.Text("t", "正文")
        val checklist = NoteBlock.Checklist("c", "清单")
        val image = NoteBlock.Image("i", "a.jpg")
        val doodle = NoteBlock.Doodle("d", "a.png")
        assertEquals(6, blockGapDp(text, text.copy(id = "t2"), spacing))
        assertEquals(10, blockGapDp(checklist, checklist.copy(id = "c2"), spacing))
        assertEquals(22, blockGapDp(text, checklist, spacing))
        assertEquals(22, blockGapDp(text, image, spacing))
        assertEquals(22, blockGapDp(text, doodle, spacing))
        assertEquals(22, blockGapDp(checklist, image, spacing))
    }

    @Test
    fun sliderMapsTheTrackOntoTheRange() {
        assertEquals(8, spacingValueAt(0f, 100f, EditorSpacing.BLOCK_RANGE))
        assertEquals(40, spacingValueAt(100f, 100f, EditorSpacing.BLOCK_RANGE))
        assertEquals(24, spacingValueAt(50f, 100f, EditorSpacing.BLOCK_RANGE))
        assertEquals(0, spacingValueAt(-10f, 100f, EditorSpacing.BODY_RANGE))
        assertEquals(24, spacingValueAt(1000f, 100f, EditorSpacing.BODY_RANGE))
    }
}
