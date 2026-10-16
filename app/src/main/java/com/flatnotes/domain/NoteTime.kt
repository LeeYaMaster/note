package com.flatnotes.domain

import com.flatnotes.data.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DateBucket(val label: String) {
    Today("今天"),
    Yesterday("昨天"),
    Earlier("更早"),
}

data class NoteGroup(
    val bucket: DateBucket,
    val notes: List<Note>,
)

fun formatNoteTime(
    epochMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val date = dateTime.toLocalDate()
    val clock = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        date == today -> clock
        date == today.minusDays(1) -> "昨天 $clock"
        date == today.minusDays(2) -> "前天 $clock"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("M月d日"))
        else -> date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
    }
}

fun groupNotes(
    notes: List<Note>,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<NoteGroup> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val grouped = notes.groupBy { note ->
        val date = Instant.ofEpochMilli(note.updatedAt).atZone(zone).toLocalDate()
        when {
            date == today -> DateBucket.Today
            date == today.minusDays(1) -> DateBucket.Yesterday
            else -> DateBucket.Earlier
        }
    }
    return DateBucket.entries.mapNotNull { bucket ->
        val items = grouped[bucket].orEmpty().sortedByDescending { it.updatedAt }
        if (items.isEmpty()) null else NoteGroup(bucket, items)
    }
}
