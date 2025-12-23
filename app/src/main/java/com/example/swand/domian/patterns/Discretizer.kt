package com.example.swand.domian.patterns

import com.example.swand.domian.models.Direction
import com.example.swand.domian.models.Shift
import kotlin.math.PI
import kotlin.math.abs

class Discretizer (
    private val k: Int
) {
    private val halfSegment: Double = PI / k

    private val TAG = "Discretizer"
    init {
        require(k % 4 == 0) { "k must be %4 == 0, got $k" }
    }

    fun directionForm(shift: Shift): Direction
    {
        // Log.w(TAG, "shift $shift angle ${shift.angle}")

        val r = (abs(shift.angle) / halfSegment).toInt()

        val index : Int = if (shift.angle > 0 ) {
            when (r) {
                0 -> 1
                k - 1 -> k / 2 + 1
                else -> (r - 1) / 2 + 2
            }
        } else {
            when (r) {
                0 -> 1
                k - 1 -> k / 2 + 1
                else -> k - (r - 1) / 2
            }
        }
        // Log.w(TAG, "angle ${shift.angle} r $r index $index")

        return Direction(index, k)
    }
}