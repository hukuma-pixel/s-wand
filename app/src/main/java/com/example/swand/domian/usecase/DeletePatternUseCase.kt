package com.example.swand.domian.usecase

import com.example.swand.domian.repository.PatternRepository

class DeletePatternUseCase(
    private val patternRepository: PatternRepository
) {
    suspend operator fun invoke(patternName: String): Result<Unit> {
        return try {
            val success = patternRepository.deletePatternPair(patternName)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pattern not found or delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}