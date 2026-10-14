package com.flatnotes.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.flatnotes.data.Note
import com.flatnotes.data.NoteKind
import com.flatnotes.data.NoteTint
import com.flatnotes.data.NotesLayout
import com.flatnotes.data.sampleNotes
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var layout by mutableStateOf(readLayout())
        private set

    var notes by mutableStateOf(sampleNotes())
        private set

    fun updateLayout(value: NotesLayout) {
        layout = value
        prefs.edit().putString(KEY_LAYOUT, value.name).apply()
    }

    fun note(id: String): Note? = notes.find { it.id == id }

    fun create(kind: NoteKind): String {
        val id = UUID.randomUUID().toString()
        val tint = NoteTint.entries[notes.size % NoteTint.entries.size]
        notes = listOf(
            Note(
                id = id,
                title = "",
                body = "",
                updatedAt = System.currentTimeMillis(),
                tint = tint,
                kind = kind,
            ),
        ) + notes
        return id
    }

    fun update(id: String, title: String, body: String) {
        notes = notes.map { note ->
            if (note.id != id) {
                note
            } else {
                note.copy(
                    title = title,
                    body = body,
                    preview = if (body == note.body) note.preview else "",
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    fun delete(id: String) {
        notes = notes.filterNot { it.id == id }
    }

    fun discardIfBlank(id: String) {
        val current = note(id) ?: return
        if (current.title.isBlank() && current.body.isBlank()) {
            delete(id)
        }
    }

    private fun readLayout(): NotesLayout {
        val raw = prefs.getString(KEY_LAYOUT, NotesLayout.List.name)
        return runCatching { NotesLayout.valueOf(raw ?: NotesLayout.List.name) }
            .getOrDefault(NotesLayout.List)
    }

    private companion object {
        const val PREFS = "flat_notes"
        const val KEY_LAYOUT = "notes_layout"
    }
}
