package com.example.swand.domian.usecase

import com.example.swand.domian.models.PatternName
import com.example.swand.domian.models.Shift
import com.example.swand.domian.patterns.PatternMaker
import com.example.swand.domian.repository.PatternRepository

class CreatePatternUseCase(
    private val patternMaker: PatternMaker,
    private val patternRepository: PatternRepository
) {
    suspend operator fun invoke(
        name: String,
        shifts: List<Shift>
    ): Result<Unit> {
        return try {
            val pattern = patternMaker.make(shifts)

            if (pattern.segments.isEmpty()) {
                return Result.failure(IllegalArgumentException("Pattern is empty after processing"))
            }

            val success = patternRepository.savePatternPair(
                patternName = PatternName(name),
                pattern = pattern
            )

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save pattern"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}