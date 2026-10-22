package com.flatnotes.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.flatnotes.data.Note
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.NoteFont
import com.flatnotes.data.ParagraphAlign

private const val POSTER_WIDTH = 1080
private const val FRAME_INSET = 40f
private const val SIDE_PADDING = 72f
private const val TOP_PADDING = 168f
private const val BOTTOM_PADDING = 96f
private const val CONTENT_X = FRAME_INSET + SIDE_PADDING
private const val TEXT = 0xFF1A1A1A.toInt()
private const val MUTED = 0xFF8E8E8E.toInt()
private const val GREEN = 0xFF22A45A.toInt()

fun renderNoteBitmap(note: Note, decode: (String) -> Bitmap?): Bitmap {
    val contentWidth = POSTER_WIDTH - CONTENT_X * 2
    val borrowed = mutableListOf<Bitmap>()
    val pieces = mutableListOf<Piece>()
    val title = note.title.ifBlank { "无标题" }
    pieces += textPiece(
        text = title,
        width = contentWidth.toInt(),
        textSize = 108f,
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
    var height = FRAME_INSET + TOP_PADDING
    pieces.forEachIndexed { index, piece ->
        height += piece.height
        if (index != pieces.lastIndex) height += if (index == 0) 48f else 28f
    }
    height += BOTTOM_PADDING + FRAME_INSET
    val bitmap = Bitmap.createBitmap(POSTER_WIDTH, height.toInt().coerceIn(640, 16000), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawBackdrop(canvas, POSTER_WIDTH, bitmap.height)
    var top = FRAME_INSET + TOP_PADDING
    pieces.forEachIndexed { index, piece ->
        piece.draw(canvas, top)
        top += piece.height
        if (index != pieces.lastIndex) top += if (index == 0) 48f else 28f
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

private fun drawBackdrop(canvas: Canvas, width: Int, height: Int) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(
        0f,
        0f,
        0f,
        height.toFloat(),
        intArrayOf(0xFFFFF1C4.toInt(), 0xFFFFF7E8.toInt(), 0xFFF2F6FF.toInt()),
        floatArrayOf(0f, 0.46f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    paint.color = 0x66E4CF8C.toInt()
    val step = 26f
    var y = 14f
    var row = 0
    while (y < height) {
        var x = if (row % 2 == 0) 12f else 25f
        while (x < width) {
            canvas.drawCircle(x, y, 1.7f, paint)
            x += step
        }
        y += step
        row += 1
    }
    val card = RectF(
        FRAME_INSET,
        FRAME_INSET,
        width - FRAME_INSET,
        height - FRAME_INSET,
    )
    paint.shader = LinearGradient(
        0f,
        card.top,
        0f,
        card.bottom,
        0xFFFFF8EA.toInt(),
        0xFFFFFDF9.toInt(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRoundRect(card, 36f, 36f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = 0xFFE6D7A6.toInt()
    canvas.drawRoundRect(card, 36f, 36f, paint)
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
        canvas.translate(CONTENT_X, top)
        layout.draw(canvas)
        canvas.restore()
    }
}

private fun checklistPiece(block: NoteBlock.Checklist, contentWidth: Float): Piece {
    val box = 36f
    val gap = 20f
    val textWidth = (contentWidth - box - gap).toInt().coerceAtLeast(40)
    val layout = staticLayout(
        text = block.text.ifEmpty { " " },
        width = textWidth,
        textSize = 42f,
        color = if (block.done) MUTED else TEXT,
        typeface = block.font.toTypeface(),
        align = block.align,
        strike = block.done,
    )
    val height = maxOf(box, layout.height.toFloat())
    return Piece(height) { canvas, top ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val left = CONTENT_X
        val topBox = top + (height - box) / 2f
        val radius = 8f
        if (block.done) {
            paint.style = Paint.Style.FILL
            paint.color = GREEN
            canvas.drawRoundRect(left, topBox, left + box, topBox + box, radius, radius, paint)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4.5f
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            val path = Path().apply {
                moveTo(left + 9f, topBox + 18f)
                lineTo(left + 15f, topBox + 25f)
                lineTo(left + 27f, topBox + 12f)
            }
            canvas.drawPath(path, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = MUTED
            canvas.drawRoundRect(
                left + 1.5f,
                topBox + 1.5f,
                left + box - 1.5f,
                topBox + box - 1.5f,
                radius,
                radius,
                paint,
            )
        }
        canvas.save()
        canvas.translate(CONTENT_X + box + gap, top + (height - layout.height) / 2f)
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
        staticLayout(caption, width.toInt().coerceAtLeast(40), 34f, MUTED, Typeface.SANS_SERIF, ParagraphAlign.Start, false)
    }
    val captionGap = if (captionLayout == null) 0f else 16f
    val height = imageHeight + captionGap + (captionLayout?.height?.toFloat() ?: 0f)
    return Piece(height) { canvas, top ->
        val left = CONTENT_X
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
