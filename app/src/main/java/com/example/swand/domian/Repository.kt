package com.example.swand.domian

import com.example.swand.data.db.PatternDao
import com.example.swand.data.db.PatternEntity
import com.example.swand.data.db.PatternNameEntity
import com.example.swand.patterns.Pattern
import com.example.swand.patterns.PatternName
import com.example.swand.patterns.PatternSegment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PatternRepository(private val patternDao: PatternDao) {
    private val gson = Gson()

    suspend fun savePatternPair(
        patternName: PatternName,
        pattern: Pattern
    ): Boolean {
        try {
            val patternNameEntity = PatternNameEntity(patternName.name)
            val patternEntity = PatternEntity(
                patternName = patternName.name,
                segmentsJson = gson.toJson(pattern.segments)
            )

            patternDao.insertPatternPair(patternNameEntity, patternEntity)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun getAllPatterns(): List<Pair<PatternName, Pattern>> {
        return patternDao.getPatternsWithNames().map { patternWithName ->
            val segments = gson.fromJson<List<PatternSegment>>(
                patternWithName.pattern.segmentsJson,
                object : TypeToken<List<PatternSegment>>() {}.type
            )

            PatternName(patternWithName.patternName.name) to
                    Pattern(segments)
        }
    }

    suspend fun getPatternByName(name: String): Pair<PatternName, Pattern>? {
        val patternName = patternDao.getPatternName(name) ?: return null
        val patternEntity = patternDao.getPattern(name) ?: return null

        val segments = gson.fromJson<List<PatternSegment>>(
            patternEntity.segmentsJson,
            object : TypeToken<List<PatternSegment>>() {}.type
        )

        return PatternName(patternName.name) to Pattern(segments)
    }

    suspend fun updatePattern(patternName: String, pattern: Pattern): Boolean {
        try {
            val patternEntity = patternDao.getPattern(patternName)
                ?: return false

            patternDao.updatePattern(
                patternEntity.copy(
                    segmentsJson = gson.toJson(pattern.segments)
                )
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun deletePatternPair(patternName: String): Boolean {
        return try {
            patternDao.deletePatternPair(patternName)
            true
        } catch (e: Exception) {
            false
        }
    }
}