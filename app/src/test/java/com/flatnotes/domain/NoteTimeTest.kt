package com.flatnotes.domain

import com.flatnotes.data.Note
import com.flatnotes.data.NoteKind
import com.flatnotes.data.NoteTint
import com.flatnotes.data.displayPreview
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NoteTimeTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 10, 14)
    private val now = millis(today, 12, 0)

    @Test
    fun todayShowsClockOnly() {
        assertEquals("10:30", formatNoteTime(millis(today, 10, 30), now, zone))
    }

    @Test
    fun yesterdayAndDayBeforeKeepRelativeWords() {
        assertEquals("昨天 17:45", formatNoteTime(millis(today.minusDays(1), 17, 45), now, zone))
        assertEquals("前天 18:30", formatNoteTime(millis(today.minusDays(2), 18, 30), now, zone))
    }

    @Test
    fun olderDatesUseCalendarLabels() {
        assertEquals("5月28日", formatNoteTime(millis(LocalDate.of(2026, 5, 28), 9, 0), now, zone))
        assertEquals(
            "2025年12月1日",
            formatNoteTime(millis(LocalDate.of(2025, 12, 1), 8, 0), now, zone),
        )
    }

    @Test
    fun groupsSkipEmptyBucketsAndSortNewestFirst() {
        val notes = listOf(
            note("morning", millis(today, 9, 15)),
            note("meeting", millis(today, 10, 30)),
            note("reading", millis(today.minusDays(1), 9, 0)),
            note("weekend", millis(today.minusDays(1), 17, 45)),
            note("travel", millis(today.minusDays(2), 18, 30)),
        )
        val groups = groupNotes(notes, now, zone)
        assertEquals(
            listOf(DateBucket.Today, DateBucket.Yesterday, DateBucket.Earlier),
            groups.map { it.bucket },
        )
        assertEquals(listOf("今天", "昨天", "更早"), groups.map { it.bucket.label })
        assertEquals(listOf("meeting", "morning"), groups[0].notes.map { it.id })
        assertEquals(listOf("weekend", "reading"), groups[1].notes.map { it.id })
        assertEquals(listOf("travel"), groups[2].notes.map { it.id })
    }

    @Test
    fun previewFallsBackToFirstBodyLine() {
        val explicit = note("a", now).copy(preview = "摘要", body = "正文第一行")
        val derived = note("b", now).copy(preview = "", body = "\n正文第一行\n第二行")
        val empty = note("c", now).copy(preview = "  ", body = "\n  \n")
        assertEquals("摘要", explicit.displayPreview())
        assertEquals("正文第一行", derived.displayPreview())
        assertEquals("", empty.displayPreview())
    }

    @Test
    fun checklistInsertsOnItsOwnLine() {
        assertEquals("☐ ", insertChecklistLine(""))
        assertEquals("买菜\n☐ ", insertChecklistLine("买菜"))
        assertEquals("买菜\n☐ ", insertChecklistLine("买菜\n"))
    }

    private fun millis(date: LocalDate, hour: Int, minute: Int): Long {
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    private fun note(id: String, updatedAt: Long): Note {
        return Note(
            id = id,
            title = id,
            body = "",
            updatedAt = updatedAt,
            tint = NoteTint.White,
            kind = NoteKind.Note,
        )
    }
}
