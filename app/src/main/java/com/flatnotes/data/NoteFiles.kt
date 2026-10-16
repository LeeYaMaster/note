package com.flatnotes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

class NoteFiles(private val context: Context) {
    private val root = File(context.filesDir, "note-media")

    fun file(noteId: String, name: String): File = File(root, "$noteId/$name")

    fun deleteNote(noteId: String) {
        File(root, noteId).deleteRecursively()
    }

    fun deleteFile(noteId: String, name: String) {
        file(noteId, name).delete()
    }

    fun importImage(noteId: String, uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.isEmpty()) return null
        val bitmap = decodeSampled(bytes, maxSide = 1600) ?: return null
        return try {
            val name = "${UUID.randomUUID()}.jpg"
            val dest = file(noteId, name)
            dest.parentFile?.mkdirs()
            dest.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            name
        } finally {
            bitmap.recycle()
        }
    }

    fun savePng(noteId: String, bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.png"
        val dest = file(noteId, name)
        dest.parentFile?.mkdirs()
        dest.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return name
    }

    fun decodePreview(file: File, maxSide: Int = 1600): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (width / sample > maxSide || height / sample > maxSide) {
            sample *= 2
        }
        return sample
    }
}
