package com.flatnotes.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.flatnotes.data.Note
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFont
import com.flatnotes.data.ParagraphAlign

private const val POSTER_WIDTH = 1080
private const val PADDING = 72f
private const val TEXT = 0xFF1A1A1A.toInt()
private const val MUTED = 0xFF8E8E8E.toInt()
private const val GREEN = 0xFF22A45A.toInt()

fun renderNoteBitmap(note: Note, decode: (String) -> Bitmap?): Bitmap {
    val contentWidth = POSTER_WIDTH - PADDING * 2
    val borrowed = mutableListOf<Bitmap>()
    val pieces = mutableListOf<Piece>()
    val title = note.title.ifBlank { "无标题" }
    pieces += textPiece(
        text = title,
        width = contentWidth.toInt(),
        textSize = 72f,
        color = if (note.title.isBlank()) MUTED else TEXT,
        typeface = Typeface.DEFAULT_BOLD,
        align = ParagraphAlign.Start,
        strike = false,
    )
    note.blocks.forEach { block ->
        when (block) {
            is NoteBlock.Text -> {
                if (block.text.isEmpty()) {
                    pieces += Piece(28f) { _, _ -> }
                } else {
                    pieces += textPiece(
                        text = block.text,
                        width = contentWidth.toInt(),
                        textSize = 42f,
                        color = TEXT,
                        typeface = block.font.toTypeface(),
                        align = block.align,
                        strike = false,
                    )
                }
            }
            is NoteBlock.Checklist -> pieces += checklistPiece(block, contentWidth)
            is NoteBlock.Image -> pieces += mediaPiece(block.fileName, block.scale, block.caption, contentWidth, decode, borrowed)
            is NoteBlock.Doodle -> pieces += mediaPiece(block.fileName, block.scale, block.caption, contentWidth, decode, borrowed)
        }
    }
    val gap = 28f
    var height = PADDING
    pieces.forEachIndexed { index, piece ->
        height += piece.height
        if (index != pieces.lastIndex) height += gap
    }
    height += PADDING
    val bitmap = Bitmap.createBitmap(POSTER_WIDTH, height.toInt().coerceIn(480, 16000), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    var top = PADDING
    pieces.forEachIndexed { index, piece ->
        piece.draw(canvas, top)
        top += piece.height
        if (index != pieces.lastIndex) top += gap
    }
    borrowed.forEach { image ->
        if (!image.isRecycled) image.recycle()
    }
    return bitmap
}

fun shareNoteImage(context: android.content.Context, bitmap: Bitmap) {
    val dir = java.io.File(context.cacheDir, "share").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val file = java.io.File(dir, "note-share.png")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newRawUri("笔记", uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = android.content.Intent.createChooser(send, "分享图片").apply {
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
}

private class Piece(
    val height: Float,
    val draw: (Canvas, Float) -> Unit,
)

private fun textPiece(
    text: String,
    width: Int,
    textSize: Float,
    color: Int,
    typeface: Typeface,
    align: ParagraphAlign,
    strike: Boolean,
): Piece {
    val layout = staticLayout(text, width, textSize, color, typeface, align, strike)
    return Piece(layout.height.toFloat()) { canvas, top ->
        canvas.save()
        canvas.translate(PADDING, top)
        layout.draw(canvas)
        canvas.restore()
    }
}

private fun checklistPiece(block: NoteBlock.Checklist, contentWidth: Float): Piece {
    val circle = 40f
    val gap = 20f
    val textWidth = (contentWidth - circle - gap).toInt().coerceAtLeast(40)
    val layout = staticLayout(
        text = block.text.ifEmpty { " " },
        width = textWidth,
        textSize = 42f,
        color = if (block.done) MUTED else TEXT,
        typeface = block.font.toTypeface(),
        align = block.align,
        strike = block.done,
    )
    val height = maxOf(circle, layout.height.toFloat())
    return Piece(height) { canvas, top ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = PADDING + circle / 2f
        val cy = top + height / 2f
        if (block.done) {
            paint.style = Paint.Style.FILL
            paint.color = GREEN
            canvas.drawCircle(cx, cy, circle / 2f, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            val path = Path().apply {
                moveTo(cx - 10f, cy + 1f)
                lineTo(cx - 3f, cy + 9f)
                lineTo(cx + 12f, cy - 8f)
            }
            canvas.drawPath(path, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = MUTED
            canvas.drawCircle(cx, cy, circle / 2f - 1.5f, paint)
        }
        canvas.save()
        canvas.translate(PADDING + circle + gap, top + (height - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}

private fun mediaPiece(
    fileName: String,
    scale: Float,
    caption: String,
    contentWidth: Float,
    decode: (String) -> Bitmap?,
    borrowed: MutableList<Bitmap>,
): Piece {
    val image = decode(fileName)
    val width = contentWidth * scale.coerceIn(0.45f, 1f)
    val imageHeight = if (image == null || image.width <= 0) {
        120f
    } else {
        borrowed += image
        width * image.height / image.width.toFloat()
    }
    val captionLayout = if (caption.isBlank()) {
        null
    } else {
        staticLayout(caption, width.toInt().coerceAtLeast(40), 34f, MUTED, Typeface.SANS_SERIF, ParagraphAlign.Center, false)
    }
    val captionGap = if (captionLayout == null) 0f else 16f
    val height = imageHeight + captionGap + (captionLayout?.height?.toFloat() ?: 0f)
    return Piece(height) { canvas, top ->
        val left = PADDING + (contentWidth - width) / 2f
        if (image == null || image.isRecycled) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = MUTED
                textSize = 36f
            }
            canvas.drawText("[图片]", left, top + 48f, paint)
        } else {
            val dest = android.graphics.RectF(left, top, left + width, top + imageHeight)
            canvas.drawBitmap(image, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        if (captionLayout != null) {
            canvas.save()
            canvas.translate(left, top + imageHeight + captionGap)
            captionLayout.draw(canvas)
            canvas.restore()
        }
    }
}

private fun staticLayout(
    text: String,
    width: Int,
    textSize: Float,
    color: Int,
    typeface: Typeface,
    align: ParagraphAlign,
    strike: Boolean,
): StaticLayout {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        this.typeface = typeface
        isStrikeThruText = strike
    }
    val alignment = when (align) {
        ParagraphAlign.Start -> Layout.Alignment.ALIGN_NORMAL
        ParagraphAlign.Center -> Layout.Alignment.ALIGN_CENTER
        ParagraphAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
    }
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(alignment)
        .setLineSpacing(8f, 1f)
        .setIncludePad(false)
        .build()
}

private fun NoteFont.toTypeface(): Typeface = when (this) {
    NoteFont.Default -> Typeface.SANS_SERIF
    NoteFont.Serif -> Typeface.SERIF
    NoteFont.Monospace -> Typeface.MONOSPACE
    NoteFont.Handwriting -> Typeface.create("cursive", Typeface.NORMAL)
}
