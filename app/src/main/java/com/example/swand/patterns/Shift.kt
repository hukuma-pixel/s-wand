package com.example.swand.patterns

import kotlin.math.atan2
import kotlin.math.sqrt

data class Shift(
    val dx: Float = 0f,
    val dy: Float = 0f
) {

    val magnitude: Float by lazy {
        sqrt(dx * dx + dy * dy)
    }

    val angle: Float by lazy {
        atan2(dy, dx)
    }
}