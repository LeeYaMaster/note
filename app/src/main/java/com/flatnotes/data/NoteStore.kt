package com.flatnotes.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 笔记正文存在应用私有 SQLite 里。卸载或清除应用数据时，系统和文件一起被清掉。
 */
class NoteStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "flat_notes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                blocks TEXT NOT NULL,
                preview TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                tint TEXT NOT NULL,
                sort_order INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun loadAll(): List<Note> {
        val notes = mutableListOf<Note>()
        readableDatabase.query(
            "notes",
            arrayOf("id", "title", "blocks", "preview", "updated_at", "tint"),
            null,
            null,
            null,
            null,
            "sort_order ASC",
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val titleIndex = cursor.getColumnIndexOrThrow("title")
            val blocksIndex = cursor.getColumnIndexOrThrow("blocks")
            val previewIndex = cursor.getColumnIndexOrThrow("preview")
            val updatedIndex = cursor.getColumnIndexOrThrow("updated_at")
            val tintIndex = cursor.getColumnIndexOrThrow("tint")
            while (cursor.moveToNext()) {
                val blocks = NoteCodec.decode(cursor.getString(blocksIndex))
                notes += Note(
                    id = cursor.getString(idIndex),
                    title = cursor.getString(titleIndex),
                    blocks = blocks.ifEmpty { listOf(NoteBlock.Text(id = cursor.getString(idIndex) + "-body", text = "")) },
                    preview = cursor.getString(previewIndex),
                    updatedAt = cursor.getLong(updatedIndex),
                    tint = runCatching { NoteTint.valueOf(cursor.getString(tintIndex)) }.getOrDefault(NoteTint.White),
                )
            }
        }
        return notes
    }

    fun replaceAll(notes: List<Note>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("notes", null, null)
            val statement = db.compileStatement(
                "INSERT INTO notes (id, title, blocks, preview, updated_at, tint, sort_order) VALUES (?,?,?,?,?,?,?)",
            )
            notes.forEachIndexed { index, note ->
                statement.clearBindings()
                statement.bindString(1, note.id)
                statement.bindString(2, note.title)
                statement.bindString(3, NoteCodec.encode(note.blocks))
                statement.bindString(4, note.preview)
                statement.bindLong(5, note.updatedAt)
                statement.bindString(6, note.tint.name)
                statement.bindLong(7, index.toLong())
                statement.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
