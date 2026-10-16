package com.flatnotes.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.flatnotes.data.Note
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFiles
import com.flatnotes.data.NoteStore
import com.flatnotes.data.NoteTint
import com.flatnotes.data.NotesLayout
import com.flatnotes.data.isContentBlank
import com.flatnotes.data.sampleNotes
import com.flatnotes.domain.EditorSpacing
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val files = NoteFiles(application)
    private val store = NoteStore(application)
    private val writer = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    var layout by mutableStateOf(readLayout())
        private set

    var spacing by mutableStateOf(readSpacing())
        private set

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var ready by mutableStateOf(false)
        private set

    init {
        writer.execute {
            val loaded = runCatching { loadOrSeed() }.getOrElse { sampleNotes() }
            main.post {
                if (!ready) {
                    notes = loaded
                    ready = true
                }
            }
        }
    }

    override fun onCleared() {
        writer.shutdown()
    }

    fun updateLayout(value: NotesLayout) {
        layout = value
        prefs.edit().putString(KEY_LAYOUT, value.name).apply()
    }

    fun updateSpacing(value: EditorSpacing) {
        val next = value.clamped()
        spacing = next
        prefs.edit()
            .putInt(KEY_SPACING_BODY, next.body)
            .putInt(KEY_SPACING_CHECKLIST, next.checklist)
            .putInt(KEY_SPACING_BLOCK, next.block)
            .apply()
    }

    fun note(id: String): Note? = notes.find { it.id == id }

    fun create(): String {
        val id = UUID.randomUUID().toString()
        val tint = NoteTint.entries[notes.size % NoteTint.entries.size]
        publish(
            listOf(
                Note(
                    id = id,
                    title = "",
                    blocks = listOf(NoteBlock.Text(id = UUID.randomUUID().toString(), text = "")),
                    updatedAt = System.currentTimeMillis(),
                    tint = tint,
                ),
            ) + notes,
        )
        return id
    }

    fun updateTitle(id: String, title: String) {
        publish(
            notes.map { note ->
                if (note.id == id) note.copy(title = title, updatedAt = System.currentTimeMillis()) else note
            },
        )
    }

    fun updateBlocks(id: String, blocks: List<NoteBlock>) {
        publish(
            notes.map { note ->
                if (note.id == id) {
                    note.copy(blocks = blocks, preview = "", updatedAt = System.currentTimeMillis())
                } else {
                    note
                }
            },
        )
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
        publish(notes.filterNot { it.id in ids }) {
            ids.forEach { files.deleteNote(it) }
        }
    }

    fun discardIfBlank(id: String) {
        val current = note(id) ?: return
        if (current.isContentBlank()) delete(id)
    }

    private fun loadOrSeed(): List<Note> {
        if (!prefs.getBoolean(KEY_SEEDED, false)) {
            val samples = sampleNotes()
            store.replaceAll(samples)
            prefs.edit().putBoolean(KEY_SEEDED, true).commit()
            return samples
        }
        return store.loadAll()
    }

    private fun publish(next: List<Note>, afterSave: (() -> Unit)? = null) {
        notes = next
        if (!ready || writer.isShutdown) return
        val snapshot = next.toList()
        writer.execute {
            val saved = runCatching { store.replaceAll(snapshot) }.isSuccess
            if (saved) afterSave?.invoke()
        }
    }

    private fun readLayout(): NotesLayout {
        val raw = prefs.getString(KEY_LAYOUT, NotesLayout.List.name)
        return runCatching { NotesLayout.valueOf(raw ?: NotesLayout.List.name) }
            .getOrDefault(NotesLayout.List)
    }

    private fun readSpacing(): EditorSpacing {
        return EditorSpacing(
            body = prefs.getInt(KEY_SPACING_BODY, EditorSpacing.BODY_DEFAULT),
            checklist = prefs.getInt(KEY_SPACING_CHECKLIST, EditorSpacing.CHECKLIST_DEFAULT),
            block = prefs.getInt(KEY_SPACING_BLOCK, EditorSpacing.BLOCK_DEFAULT),
        ).clamped()
    }

    private companion object {
        const val PREFS = "flat_notes"
        const val KEY_LAYOUT = "notes_layout"
        const val KEY_SEEDED = "notes_seeded"
        const val KEY_SPACING_BODY = "spacing_body"
        const val KEY_SPACING_CHECKLIST = "spacing_checklist"
        const val KEY_SPACING_BLOCK = "spacing_block"
    }
}
