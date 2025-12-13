package com.example.swand.patterns

import android.util.Log
import kotlin.math.abs

data class PatternMakerConfig(
    val minShiftMagnitude: Float = 0.002f,
    val minSegmentWeight: Float = 0.03f
)

class PatternMaker(
    private val discretizer: Discretizer,
    private val config: PatternMakerConfig
) {

    private val TAG = "PatternMaker"
    fun make(shifts: List<Shift>): Pattern {
        var maxWeight: Float = 0f
        val segments: MutableList<PatternSegment> = mutableListOf()

        for (shift in shifts) {
            if (abs(shift.dx) < config.minShiftMagnitude
                && abs(shift.dy) < config.minShiftMagnitude) {
                continue
            }

            val weight = shift.magnitude
            val direction = discretizer.directionForm(shift)

            if (segments.isEmpty()) {
                segments.add(PatternSegment(direction, weight))
                if (weight > maxWeight) maxWeight = weight
                continue
            }

            if (direction.index == segments.last().direction.index) {
                segments.last().weight += weight
                if (segments.last().weight > maxWeight) {
                    maxWeight = segments.last().weight
                }
            } else {
                segments.add(PatternSegment(direction, weight))
                if (weight > maxWeight) {
                    maxWeight = weight
                }
            }
        }

        Log.w(TAG, "вес $maxWeight , size ${segments.size}")

        if (maxWeight < config.minSegmentWeight) {
            return Pattern(emptyList())
        }

        val iterator = segments.iterator()
        while (iterator.hasNext()) {
            val segment = iterator.next()
            segment.weight /= maxWeight

            if (segment.weight < config.minSegmentWeight) {
                iterator.remove()
            }
        }

        return Pattern(segments)
    }
}