package com.example.swand.patterns

import android.util.Log
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class PatternMatcherConfig(
    val directionWeight: Float = 0.6f,
    val magnitudeWeight: Float = 0.4f,
    val positionWeight: Float = 0.8f,
    val minSimilarityThreshold: Float = 0.7f,
    val allowPartialMatch: Boolean = true,
    val maxSegmentDifference: Int = 2,
    val useDynamicTimeWarping: Boolean = false
)

class PatternMatcher(
    private val config: PatternMatcherConfig = PatternMatcherConfig()
) {
    private val TAG = "PatternMatcher"

    fun match(pattern1: Pattern, pattern2: Pattern): Float {
        Log.d(TAG, "Comparing patterns: ${pattern1.segments.size} vs ${pattern2.segments.size}")

        if (pattern1.segments.isEmpty() && pattern2.segments.isEmpty()) {
            return 1.0f
        }

        if (pattern1.segments.isEmpty() || pattern2.segments.isEmpty()) {
            return 0.0f
        }

        return if (config.useDynamicTimeWarping) {
            matchWithDTW(pattern1, pattern2)
        } else {
            matchWithSequenceAlignment(pattern1, pattern2)
        }
    }

    private fun matchWithSequenceAlignment(pattern1: Pattern, pattern2: Pattern): Float {
        val seq1 = pattern1.segments
        val seq2 = pattern2.segments

        val lengthDiff = abs(seq1.size - seq2.size)
        if (lengthDiff > config.maxSegmentDifference && !config.allowPartialMatch) {
            return 0.0f
        }

        val similarityMatrix = calculateSimilarityMatrix(seq1, seq2)

        return findBestAlignment(similarityMatrix, seq1.size, seq2.size)
    }

    private fun calculateSimilarityMatrix(
        seq1: List<PatternSegment>,
        seq2: List<PatternSegment>
    ): Array<FloatArray> {
        val matrix = Array(seq1.size) { FloatArray(seq2.size) }

        for (i in seq1.indices) {
            for (j in seq2.indices) {
                matrix[i][j] = segmentSimilarity(seq1[i], seq2[j], i, j)
            }
        }

        return matrix
    }

    private fun segmentSimilarity(
        seg1: PatternSegment,
        seg2: PatternSegment,
        position1: Int,
        position2: Int
    ): Float {
        val directionSim = directionSimilarity(seg1.direction, seg2.direction)

        val magnitudeSim = magnitudeSimilarity(seg1.weight, seg2.weight)

        val positionSim = positionSimilarity(position1, position2, seg1.weight, seg2.weight)

        val baseSimilarity = (
                directionSim * config.directionWeight +
                        magnitudeSim * config.magnitudeWeight
                ) / (config.directionWeight + config.magnitudeWeight)

        return baseSimilarity * positionSim
    }

    private fun directionSimilarity(dir1: Direction, dir2: Direction): Float {
        if (dir1.index == dir2.index) {
            return 1.0f
        }

        val totalDirections = 8
        val diff = abs(dir1.index - dir2.index)
        val cyclicDiff = min(diff, totalDirections - diff)

        return exp(-cyclicDiff.toFloat() / 2.0f)
    }

    private fun magnitudeSimilarity(mag1: Float, mag2: Float): Float {
        val diff = abs(mag1 - mag2)
        return exp(-diff * diff / 0.1f)
    }

    private fun positionSimilarity(
        pos1: Int,
        pos2: Int,
        weight1: Float,
        weight2: Float
    ): Float {
        val diff = abs(pos1 - pos2).toFloat()
        val maxDiff = max(pos1, pos2).toFloat()

        if (maxDiff == 0f) return 1.0f

        val weightImportance = (weight1 + weight2) / 2.0f

        return exp(-diff / (maxDiff + 1)) * (0.5f + 0.5f * weightImportance)
    }

    private fun findBestAlignment(
        similarityMatrix: Array<FloatArray>,
        len1: Int,
        len2: Int
    ): Float {
        val dp = Array(len1 + 1) { FloatArray(len2 + 1) }

        for (i in 1..len1) {
            dp[i][0] = dp[i-1][0] - 0.1f
        }
        for (j in 1..len2) {
            dp[0][j] = dp[0][j-1] - 0.1f
        }

        for (i in 1..len1) {
            for (j in 1..len2) {
                val matchScore = dp[i-1][j-1] + similarityMatrix[i-1][j-1]
                val skipI = dp[i-1][j] - 0.1f
                val skipJ = dp[i][j-1] - 0.1f

                dp[i][j] = maxOf(matchScore, skipI, skipJ)
            }
        }

        val maxPossibleScore = min(len1, len2).toFloat()
        val similarity = dp[len1][len2] / maxPossibleScore

        Log.d(TAG, "Alignment similarity: $similarity")

        return similarity.coerceIn(0f, 1f)
    }

    private fun matchWithDTW(pattern1: Pattern, pattern2: Pattern): Float {
        val seq1 = pattern1.segments
        val seq2 = pattern2.segments

        val dtw = Array(seq1.size + 1) { FloatArray(seq2.size + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f

        for (i in 1..seq1.size) {
            for (j in 1..seq2.size) {
                val cost = 1.0f - segmentSimilarity(seq1[i-1], seq2[j-1], i-1, j-1)
                dtw[i][j] = cost + minOf(
                    dtw[i-1][j],
                    dtw[i][j-1],
                    dtw[i-1][j-1]
                )
            }
        }

        val dtwDistance = dtw[seq1.size][seq2.size]
        val maxLength = max(seq1.size, seq2.size)
        val normalizedDistance = dtwDistance / maxLength

        return exp(-normalizedDistance)
    }
}