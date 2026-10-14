package com.flatnotes.data

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 演示数据的顺序与瀑布流效果图一致（从左到右、从上到下）。
 * 列表模式会再按日期分组，不依赖这份顺序。
 */
fun sampleNotes(now: LocalDateTime = LocalDateTime.now()): List<Note> {
    val zone = ZoneId.systemDefault()
    fun at(daysAgo: Long, hour: Int, minute: Int): Long {
        return now.toLocalDate()
            .minusDays(daysAgo)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    return listOf(
        Note(
            id = "note-meeting",
            title = "开会纪要",
            preview = "讨论了项目进度和下阶段计划。",
            body = """
                会议时间：2024 年 5 月 28 日 10:00 - 11:30
                会议地点：公司 3 号会议室
                参会人员：产品团队、设计团队、研发团队
                会议纪要：

                本次会议主要讨论了新产品 v2.0 的需求规划和设计方案。与会人员对产品定位、核心功能、用户体验和开发计划进行了深入讨论，明确了下一步的工作重点和时间安排。
            """.trimIndent(),
            updatedAt = at(0, 10, 30),
            tint = NoteTint.Yellow,
            kind = NoteKind.Note,
        ),
        Note(
            id = "note-spark",
            title = "灵感碎片",
            preview = "记录突如其来的创意和灵感。",
            body = """
                记录突如其来的创意和灵感。

                把一闪而过的想法先记下来，回头再整理成完整的方案。
            """.trimIndent(),
            updatedAt = at(1, 14, 20),
            tint = NoteTint.Green,
            kind = NoteKind.Note,
        ),
        Note(
            id = "note-grocery",
            title = "买菜清单",
            preview = "西红柿、鸡蛋、青菜、牛奶。",
            body = """
                西红柿
                鸡蛋
                青菜
                牛奶
            """.trimIndent(),
            updatedAt = at(0, 9, 15),
            tint = NoteTint.Blue,
            kind = NoteKind.Todo,
        ),
        Note(
            id = "note-reading",
            title = "读书摘抄",
            preview = "今天读到了一段发人深省的话。",
            body = """
                今天读到了一段发人深省的话。

                我们反复做的事情，造就了我们。因此，优秀不是一种行为，而是一种习惯。
            """.trimIndent(),
            updatedAt = at(1, 9, 0),
            tint = NoteTint.Yellow,
            kind = NoteKind.Note,
        ),
        Note(
            id = "note-weekend",
            title = "周末计划",
            preview = "整理房间，看书，户外活动。",
            body = "整理房间，看书，户外活动。",
            updatedAt = at(1, 17, 45),
            tint = NoteTint.White,
            kind = NoteKind.Note,
        ),
        Note(
            id = "note-travel",
            title = "旅行清单",
            preview = "护照、充电器、换洗衣物。",
            body = """
                护照
                充电器
                换洗衣物
            """.trimIndent(),
            updatedAt = at(2, 18, 30),
            tint = NoteTint.Blue,
            kind = NoteKind.Todo,
        ),
    )
}
