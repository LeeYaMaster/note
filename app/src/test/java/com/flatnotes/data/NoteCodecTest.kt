package com.flatnotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteCodecTest {
    @Test
    fun roundTripKeepsTextFontScaleAndEscapedCharacters() {
        val blocks = listOf(
            NoteBlock.Text("t", "第一行\n含|竖线\\和斜杠", ParagraphAlign.Center, NoteFont.Monospace),
            NoteBlock.Checklist("c", "买|菜", done = true, align = ParagraphAlign.End, font = NoteFont.Serif),
            NoteBlock.Image("i", "a b.jpg", scale = 0.72f, caption = "窗边\n的光"),
            NoteBlock.Doodle("d", "ink.png", scale = 0.45f, caption = ""),
        )
        val decoded = NoteCodec.decode(NoteCodec.encode(blocks))
        assertEquals(blocks, decoded)
    }

    @Test
    fun blankPayloadDecodesToNoBlocks() {
        assertEquals(emptyList<NoteBlock>(), NoteCodec.decode(""))
        assertEquals(emptyList<NoteBlock>(), NoteCodec.decode("   \n"))
    }
}
