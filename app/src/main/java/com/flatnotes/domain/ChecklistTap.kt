package com.flatnotes.domain

import kotlin.math.abs

data class TapPoint(
    val id: String,
    val x: Float,
    val y: Float,
)

/**
 * Picks the checklist whose square tap target contains [x], [y].
 * When two squares overlap, the nearer center wins.
 */
fun nearestChecklistTap(
    x: Float,
    y: Float,
    centers: List<TapPoint>,
    tapSize: Float,
): String? {
    val half = tapSize / 2f
    var bestId: String? = null
    var best = Float.POSITIVE_INFINITY
    for (point in centers) {
        val dx = abs(x - point.x)
        val dy = abs(y - point.y)
        if (dx <= half && dy <= half) {
            val score = dx * dx + dy * dy
            if (score < best) {
                best = score
                bestId = point.id
            }
        }
    }
    return bestId
}
