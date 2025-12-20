package com.example.swand.patterns

import kotlin.math.atan2
import kotlin.math.sqrt

data class Shift(
    val dx: Float = 0f,
    val dy: Float = 0f
) {
    val magnitude: Float = sqrt(dx * dx + dy * dy)
    val angle: Float = atan2(dy, dx)
}