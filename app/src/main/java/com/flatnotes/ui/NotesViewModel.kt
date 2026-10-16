package com.flatnotes.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.flatnotes.data.Note
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFiles
import com.flatnotes.data.NoteTint
import com.flatnotes.data.NotesLayout
import com.flatnotes.data.isContentBlank
import com.flatnotes.data.sampleNotes
import java.io.File
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val files = NoteFiles(application)

    var layout by mutableStateOf(readLayout())
        private set

    var notes by mutableStateOf(sampleNotes())
        private set

    fun updateLayout(value: NotesLayout) {
        layout = value
        prefs.edit().putString(KEY_LAYOUT, value.name).apply()
    }

    fun note(id: String): Note? = notes.find { it.id == id }

    fun create(): String {
        val id = UUID.randomUUID().toString()
        val tint = NoteTint.entries[notes.size % NoteTint.entries.size]
        notes = listOf(
            Note(
                id = id,
                title = "",
                blocks = listOf(NoteBlock.Text(id = UUID.randomUUID().toString(), text = "")),
                updatedAt = System.currentTimeMillis(),
                tint = tint,
            ),
        ) + notes
        return id
    }

    fun updateTitle(id: String, title: String) {
        notes = notes.map { note ->
            if (note.id == id) note.copy(title = title, updatedAt = System.currentTimeMillis()) else note
        }
    }

    fun updateBlocks(id: String, blocks: List<NoteBlock>) {
        notes = notes.map { note ->
            if (note.id == id) {
                note.copy(blocks = blocks, preview = "", updatedAt = System.currentTimeMillis())
            } else {
                note
            }
        }
    }

    fun mediaFile(noteId: String, name: String): File = files.file(noteId, name)

    fun decode(file: File): Bitmap? = files.decodePreview(file)

    fun importImage(noteId: String, uri: Uri): String? = files.importImage(noteId, uri)

    fun saveDoodle(noteId: String, bitmap: Bitmap): String = files.savePng(noteId, bitmap)

    fun deleteMedia(noteId: String, name: String) {
        files.deleteFile(noteId, name)
    }

    fun delete(id: String) {
        deleteAll(setOf(id))
    }

    fun deleteAll(ids: Set<String>) {
        if (ids.isEmpty()) return
        notes = notes.filterNot { it.id in ids }
        ids.forEach { files.deleteNote(it) }
    }

    fun discardIfBlank(id: String) {
        val current = note(id) ?: return
        if (current.isContentBlank()) delete(id)
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
