package com.example.swand.domian.patterns

import com.example.swand.domian.models.Direction
import com.example.swand.domian.models.Pattern
import com.example.swand.domian.models.PatternSegment
import com.example.swand.domian.models.Shift
import kotlin.math.abs

data class PatternMakerConfig(
    val minShiftMagnitude: Float = 0.0001f,
    val minSegmentWeight: Float = 0.01f,
    val noiseFilterThreshold: Float = 0.5f,
    val mergeSimilarDirections: Boolean = true
)

class PatternMaker(
    private val discretizer: Discretizer,
    private val config: PatternMakerConfig
) {

    private val TAG = "PatternMaker"

    fun make(shifts: List<Shift>): Pattern {

        if (shifts.isEmpty()) {
            return Pattern(emptyList())
        }

        val filteredShifts = filterNoiseShifts(shifts)

        if (filteredShifts.isEmpty()) {
            return Pattern(emptyList())
        }

        val rawSegments = aggregateToSegments(filteredShifts)

        if (rawSegments.isEmpty()) {
            return Pattern(emptyList())
        }

        val mergedSegments = if (config.mergeSimilarDirections) {
            mergeSimilarDirectionSegments(rawSegments)
        } else {
            rawSegments
        }

        return normalizeAndFilterSegments(mergedSegments)
    }

    private fun filterNoiseShifts(shifts: List<Shift>): List<Shift> {
        return shifts.filter { shift ->
            val isSignificant = abs(shift.dx) >= config.minShiftMagnitude &&
                    abs(shift.dy) >= config.minShiftMagnitude

            val magnitude = shift.magnitude
            val isNotOutlier = magnitude <= calculateMagnitudeThreshold(shifts)

            isSignificant && isNotOutlier
        }
    }

    private fun calculateMagnitudeThreshold(shifts: List<Shift>): Float {
        if (shifts.size < 3) return Float.MAX_VALUE

        val magnitudes = shifts.map { it.magnitude }.sorted()
        val q3Index = (magnitudes.size * 0.75).toInt()
        val q3 = magnitudes[q3Index]
        val iqr = q3 - magnitudes[(magnitudes.size * 0.25).toInt()]

        return q3 + 1.5f * iqr
    }

    private fun aggregateToSegments(shifts: List<Shift>): MutableList<PatternSegment> {
        val segments: MutableList<PatternSegment> = mutableListOf()

        for (shift in shifts) {
            val direction = discretizer.directionForm(shift)
            val weight = shift.magnitude

            if (segments.isEmpty()) {
                segments.add(PatternSegment(direction, weight))
                continue
            }

            val lastSegment = segments.last()
            if (direction.index == lastSegment.direction.index) {
                lastSegment.weight += weight
            } else {
                if (config.mergeSimilarDirections &&
                    isSimilarDirection(lastSegment.direction, direction)) {
                    lastSegment.weight += weight * config.noiseFilterThreshold
                } else {
                    segments.add(PatternSegment(direction, weight))
                }
            }
        }

        return segments
    }

    private fun isSimilarDirection(dir1: Direction, dir2: Direction): Boolean {
        if (dir1.index == dir2.index) return true

        val diff = abs(dir1.index - dir2.index)
        val totalDirections = 8

        return diff == 1 || diff == totalDirections - 1
    }


    private fun mergeSimilarDirectionSegments(segments: List<PatternSegment>): MutableList<PatternSegment> {
        if (segments.size <= 1) return segments.toMutableList()

        val result = mutableListOf<PatternSegment>()
        var currentSegment = segments[0].copy()

        for (i in 1 until segments.size) {
            val nextSegment = segments[i]

            if (isSimilarDirection(currentSegment.direction, nextSegment.direction)) {
                currentSegment.weight += nextSegment.weight * config.noiseFilterThreshold
            } else {
                result.add(currentSegment)
                currentSegment = nextSegment.copy()
            }
        }

        result.add(currentSegment)

        return result
    }

    private fun normalizeAndFilterSegments(segments: List<PatternSegment>): Pattern {
        val maxWeight = segments.maxOfOrNull { it.weight } ?: 0f

        if (maxWeight < config.minSegmentWeight) {
            return Pattern(emptyList())
        }

        val normalizedSegments = segments.map { segment ->
            segment.copy(weight = segment.weight / maxWeight)
        }.filter { segment ->
            segment.weight >= config.minSegmentWeight
        }

        return Pattern(normalizedSegments)
    }
}