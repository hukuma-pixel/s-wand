package com.example.swand.patterns

import android.util.Log
import kotlin.math.abs

data class PatternMatcherConfig(
    // Базовые параметры
    val acceptableDiff: Float = 0.1f,
    val useCyclicMatching: Boolean = true,
    val minPatternSize: Int = 1,
    val maxCountErrors: Int = 6,

    // Весовые коэффициенты ошибок (чем выше вес, тем серьезнее ошибка)
    val weightErrorWeight: Float = 1.0f,      // Вес ошибки по весу сегмента
    val directionErrorWeight: Float = 2.0f,   // Вес ошибки по направлению
    val lengthErrorWeight: Float = 3.0f,      // Вес ошибки по длине паттерна
    val orderErrorWeight: Float = 1.5f,       // Вес ошибки в порядке сегментов
    val missingSegmentWeight: Float = 2.0f,   // Вес за отсутствующий сегмент

    // Пороговые значения
    val maxTotalErrorScore: Float = 5.0f,     // Максимальный допустимый общий балл ошибок
    val minSimilarityScore: Float = 0.7f      // Минимальный балл схожести (0-1)
)

data class PatternMatchResult(
    val isMatch: Boolean,
    val errorScore: Float,           // Общий балл ошибок (чем меньше, тем лучше)
    val similarityScore: Float,      // Балл схожести (0-1)
    val errors: MatchErrors          // Детализированные ошибки
)

data class MatchErrors(
    var weightErrors: Int = 0,       // Количество ошибок по весу
    var directionErrors: Int = 0,    // Количество ошибок по направлению
    var lengthMismatch: Boolean = false,
    var orderErrors: Int = 0,        // Ошибки порядка (при циклическом сравнении)
    val missingSegments: Int = 0     // Количество отсутствующих сегментов
) {
    val totalErrors: Int get() = weightErrors + directionErrors + orderErrors + missingSegments
}

class PatternMatcher(
    private val config: PatternMatcherConfig = PatternMatcherConfig()
) {
    private val TAG = "PatternMatcher"
    fun match(pattern1: Pattern, pattern2: Pattern): Boolean {
        val result = if (config.useCyclicMatching) {
            matchWithCyclicShift(pattern1, pattern2)
        } else {
            matchDirect(pattern1, pattern2)
        }

        Log.w(TAG, "Total errors: ${result.errors.totalErrors}")
        return result.errors.totalErrors <= config.maxCountErrors
    }

    private fun matchDirect(pattern1: Pattern, pattern2: Pattern): PatternMatchResult {
        val errors = MatchErrors()
        var similarity = 0f

        // Проверка длины
        if (pattern1.segments.size != pattern2.segments.size) {
            errors.lengthMismatch = true
            return calculateResult(errors, pattern1, pattern2)
        }

        // Сравнение сегментов
        var totalSegments = pattern1.segments.size
        var matchedSegments = 0

        for (i in pattern1.segments.indices) {
            val seg1 = pattern1.segments[i]
            val seg2 = pattern2.segments[i]

            val segmentMatch = compareSegments(seg1, seg2, errors)
            if (segmentMatch) {
                matchedSegments++
            }
        }

        similarity = if (totalSegments > 0) {
            matchedSegments.toFloat() / totalSegments
        } else 1f

        return calculateResult(errors, pattern1, pattern2, similarity)
    }

    private fun matchWithCyclicShift(pattern1: Pattern, pattern2: Pattern): PatternMatchResult {
        if (pattern1.segments.size != pattern2.segments.size) {
            return matchDirect(pattern1, pattern2)
        }

        val n = pattern1.segments.size
        if (n == 0) return PatternMatchResult(true, 0f, 1f, MatchErrors())

        var bestResult = PatternMatchResult(false, Float.MAX_VALUE, 0f, MatchErrors())

        // Перебираем все возможные сдвиги
        for (shift in 0 until n) {
            val errors = MatchErrors()
            var matchedSegments = 0

            for (i in 0 until n) {
                val seg1 = pattern1.segments[i]
                val seg2 = pattern2.segments[(i + shift) % n]

                val segmentMatch = compareSegments(seg1, seg2, errors)
                if (segmentMatch) {
                    matchedSegments++
                }
            }

            // Учитываем ошибку порядка (чем больше сдвиг, тем хуже)
            if (shift > 0) {
                errors.orderErrors = shift
            }

            val similarity = matchedSegments.toFloat() / n
            val currentResult = calculateResult(errors, pattern1, pattern2, similarity)

            if (currentResult.errorScore < bestResult.errorScore) {
                bestResult = currentResult
            }
        }

        return bestResult
    }

    private fun compareSegments(seg1: PatternSegment, seg2: PatternSegment,
                                errors: MatchErrors): Boolean {
        var isMatch = true

        // Проверка направления
        if (seg1.direction.discretization != seg2.direction.discretization ||
            seg1.direction.index != seg2.direction.index) {
            errors.directionErrors++
            isMatch = false
        }

        // Проверка веса
        if (abs(seg1.weight - seg2.weight) > config.acceptableDiff) {
            errors.weightErrors++
            isMatch = false
        }

        return isMatch
    }

    private fun calculateResult(
        errors: MatchErrors,
        pattern1: Pattern,
        pattern2: Pattern,
        similarity: Float = 0f
    ): PatternMatchResult {

        // Рассчитываем взвешенный балл ошибок
        val errorScore = calculateErrorScore(errors, pattern1, pattern2)

        // Проверяем, проходит ли по порогу ошибок
        val passesErrorThreshold = errorScore <= config.maxTotalErrorScore

        // Проверяем схожесть
        val passesSimilarityThreshold = similarity >= config.minSimilarityScore

        // Итоговое решение
        val isMatch = passesErrorThreshold && passesSimilarityThreshold &&
                pattern1.segments.size >= config.minPatternSize &&
                pattern2.segments.size >= config.minPatternSize

        return PatternMatchResult(isMatch, errorScore, similarity, errors)
    }

    private fun calculateErrorScore(errors: MatchErrors,
                                    pattern1: Pattern,
                                    pattern2: Pattern): Float {
        var totalScore = 0f

        // Ошибки веса
        totalScore += errors.weightErrors * config.weightErrorWeight

        // Ошибки направления
        totalScore += errors.directionErrors * config.directionErrorWeight

        // Ошибка длины
        if (errors.lengthMismatch) {
            val lengthDiff = abs(pattern1.segments.size - pattern2.segments.size)
            totalScore += lengthDiff * config.lengthErrorWeight
        }

        // Ошибки порядка (при циклическом сравнении)
        totalScore += errors.orderErrors * config.orderErrorWeight

        // Отсутствующие сегменты
        totalScore += errors.missingSegments * config.missingSegmentWeight

        // Нормализуем относительно размера паттернов
        val maxSize = maxOf(pattern1.segments.size, pattern2.segments.size)
        if (maxSize > 0) {
            totalScore /= maxSize
        }

        return totalScore
    }

}