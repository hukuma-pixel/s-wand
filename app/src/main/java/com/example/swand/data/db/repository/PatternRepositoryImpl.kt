package com.example.swand.data.db.repository

import com.example.swand.data.db.dao.PatternDao
import com.example.swand.data.db.entity.PatternEntity
import com.example.swand.data.db.entity.PatternNameEntity
import com.example.swand.domian.models.Pattern
import com.example.swand.domian.models.PatternName
import com.example.swand.domian.models.PatternSegment
import com.example.swand.domian.repository.PatternRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PatternRepositoryImpl(
    private val patternDao: PatternDao
) : PatternRepository {
    private val gson = Gson()

    override suspend fun savePatternPair(
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

    override suspend fun getAllPatterns(): List<Pair<PatternName, Pattern>> {
        return patternDao.getPatternsWithNames().map { patternWithName ->
            val segments = gson.fromJson<List<PatternSegment>>(
                patternWithName.pattern.segmentsJson,
                object : TypeToken<List<PatternSegment>>() {}.type
            )

            PatternName(patternWithName.patternName.name) to
                    Pattern(segments)
        }
    }

    override suspend fun getPatternByName(name: String): Pair<PatternName, Pattern>? {
        val patternName = patternDao.getPatternName(name) ?: return null
        val patternEntity = patternDao.getPattern(name) ?: return null

        val segments = gson.fromJson<List<PatternSegment>>(
            patternEntity.segmentsJson,
            object : TypeToken<List<PatternSegment>>() {}.type
        )

        return PatternName(patternName.name) to Pattern(segments)
    }

    override suspend fun updatePattern(patternName: String, pattern: Pattern): Boolean {
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

    override suspend fun deletePatternPair(patternName: String): Boolean {
        return try {
            patternDao.deletePatternPair(patternName)
            true
        } catch (e: Exception) {
            false
        }
    }
}