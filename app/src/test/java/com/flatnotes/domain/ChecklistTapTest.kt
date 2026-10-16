package com.flatnotes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChecklistTapTest {
    @Test
    fun tapInsideTheSquareHitsThatBox() {
        val hit = nearestChecklistTap(10f, 4f, listOf(TapPoint("a", 0f, 0f)), 44f)
        assertEquals("a", hit)
    }

    @Test
    fun tapOutsideTheSquareMisses() {
        val hit = nearestChecklistTap(23f, 0f, listOf(TapPoint("a", 0f, 0f)), 44f)
        assertNull(hit)
    }

    @Test
    fun edgeOfTheSquareStillHits() {
        val hit = nearestChecklistTap(22f, -22f, listOf(TapPoint("a", 0f, 0f)), 44f)
        assertEquals("a", hit)
    }

    @Test
    fun overlappingBoxesChooseTheCloserOne() {
        val centers = listOf(TapPoint("a", 13f, 0f), TapPoint("b", 13f, 24f))
        assertEquals("a", nearestChecklistTap(13f, 10f, centers, 44f))
        assertEquals("b", nearestChecklistTap(13f, 14f, centers, 44f))
    }
}
