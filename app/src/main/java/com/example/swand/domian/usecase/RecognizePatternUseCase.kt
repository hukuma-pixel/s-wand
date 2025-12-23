package com.example.swand.domian.usecase

import com.example.swand.domian.models.Pattern
import com.example.swand.domian.patterns.PatternMatcher
import com.example.swand.domian.repository.PatternRepository

class RecognizePatternUseCase(
    private val patternRepository: PatternRepository,
    private val patternMatcher: PatternMatcher,
    private val similarityThreshold: Float = 0.7f
) {
    suspend operator fun invoke(targetPattern: Pattern): RecognitionResult {
        val allPatterns = patternRepository.getAllPatterns()
        val similarities = allPatterns.map { (patternName, pattern) ->
            val similarity = patternMatcher.match(targetPattern, pattern)
            patternName.name to similarity
        }

        val bestMatch = similarities
            .filter { (_, similarity) -> similarity >= similarityThreshold }
            .maxByOrNull { (_, similarity) -> similarity }

        return RecognitionResult(
            allSimilarities = similarities,
            bestMatch = bestMatch,
            threshold = similarityThreshold
        )
    }

    data class RecognitionResult(
        val allSimilarities: List<Pair<String, Float>>,
        val bestMatch: Pair<String, Float>?,
        val threshold: Float
    ) {
        val isRecognized: Boolean get() = bestMatch != null
        val recognizedPattern: String? get() = bestMatch?.first
        val similarityScore: Float? get() = bestMatch?.second
    }
}