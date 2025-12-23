package com.example.swand.domian.usecase

import com.example.swand.domian.models.Pattern
import com.example.swand.domian.models.PatternName
import com.example.swand.domian.repository.PatternRepository

class GetPatternsUseCase(
    private val patternRepository: PatternRepository
) {
    suspend operator fun invoke(): Result<List<Pair<PatternName, Pattern>>> {
        return try {
            val patterns = patternRepository.getAllPatterns()
            Result.success(patterns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}