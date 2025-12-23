package com.example.swand.domian.repository

import com.example.swand.domian.models.Pattern
import com.example.swand.domian.models.PatternName

interface PatternRepository {
    suspend fun savePatternPair(
        patternName: PatternName,
        pattern: Pattern
    ): Boolean

    suspend fun getAllPatterns(): List<Pair<PatternName, Pattern>>

    suspend fun getPatternByName(name: String): Pair<PatternName, Pattern>?

    suspend fun updatePattern(patternName: String, pattern: Pattern): Boolean

    suspend fun deletePatternPair(patternName: String): Boolean
}