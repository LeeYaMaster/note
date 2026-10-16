package com.flatnotes.data

/**
 * 一行一个块的纯 Kotlin 编码，供 SQLite 存储，也方便在 JVM 单测里往返。
 * 字段里的 `|`、反斜杠和换行会转义，所以正文可以包含这些字符。
 */
object NoteCodec {
    fun encode(blocks: List<NoteBlock>): String = blocks.joinToString("\n") { encodeBlock(it) }

    fun decode(raw: String): List<NoteBlock> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            if (line.isEmpty()) null else decodeBlock(line)
        }.toList()
    }

    private fun encodeBlock(block: NoteBlock): String {
        val fields = when (block) {
            is NoteBlock.Text -> listOf("T", block.id, block.align.name, block.font.name, block.text)
            is NoteBlock.Checklist -> listOf(
                "C",
                block.id,
                block.align.name,
                block.font.name,
                if (block.done) "1" else "0",
                block.text,
            )
            is NoteBlock.Image -> listOf("I", block.id, formatScale(block.scale), block.fileName, block.caption)
            is NoteBlock.Doodle -> listOf("D", block.id, formatScale(block.scale), block.fileName, block.caption)
        }
        return fields.joinToString("|") { escape(it) }
    }

    private fun decodeBlock(line: String): NoteBlock? {
        val fields = splitEscaped(line)
        if (fields.isEmpty()) return null
        return when (fields[0]) {
            "T" -> {
                if (fields.size < 5) return null
                NoteBlock.Text(
                    id = fields[1],
                    align = enumOr(fields[2], ParagraphAlign.Start),
                    font = enumOr(fields[3], NoteFont.Default),
                    text = fields.drop(4).joinToString("|"),
                )
            }
            "C" -> {
                if (fields.size < 6) return null
                NoteBlock.Checklist(
                    id = fields[1],
                    align = enumOr(fields[2], ParagraphAlign.Start),
                    font = enumOr(fields[3], NoteFont.Default),
                    done = fields[4] == "1",
                    text = fields.drop(5).joinToString("|"),
                )
            }
            "I" -> media(fields, doodle = false)
            "D" -> media(fields, doodle = true)
            else -> null
        }
    }

    private fun media(fields: List<String>, doodle: Boolean): NoteBlock? {
        if (fields.size < 5) return null
        val scale = fields[2].toFloatOrNull()?.coerceIn(0.3f, 1f) ?: 1f
        val caption = fields.drop(4).joinToString("|")
        return if (doodle) {
            NoteBlock.Doodle(id = fields[1], fileName = fields[3], scale = scale, caption = caption)
        } else {
            NoteBlock.Image(id = fields[1], fileName = fields[3], scale = scale, caption = caption)
        }
    }

    private fun formatScale(scale: Float): String {
        return String.format(java.util.Locale.US, "%.4f", scale.coerceIn(0.3f, 1f))
    }

    private inline fun <reified T : Enum<T>> enumOr(raw: String, fallback: T): T {
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '|' -> append("\\|")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }

    private fun splitEscaped(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < line.length) {
            val ch = line[index]
            if (ch == '\\' && index + 1 < line.length) {
                when (val next = line[index + 1]) {
                    '\\' -> current.append('\\')
                    '|' -> current.append('|')
                    'n' -> current.append('\n')
                    'r' -> current.append('\r')
                    else -> current.append(next)
                }
                index += 2
            } else if (ch == '|') {
                fields += current.toString()
                current.clear()
                index += 1
            } else {
                current.append(ch)
                index += 1
            }
        }
        fields += current.toString()
        return fields
    }
}
