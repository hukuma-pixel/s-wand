package com.example.swand.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Delete

@Dao
interface PatternDao {
    // PatternName операции
    @Insert
    suspend fun insertPatternName(patternName: PatternNameEntity): Long

    @Query("SELECT * FROM pattern_names")
    suspend fun getAllPatternNames(): List<PatternNameEntity>

    @Query("SELECT * FROM pattern_names WHERE name = :name")
    suspend fun getPatternName(name: String): PatternNameEntity?

    @Delete
    suspend fun deletePatternName(patternName: PatternNameEntity)

    // Pattern операции
    @Insert
    suspend fun insertPattern(pattern: PatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: PatternEntity)

    @Query("SELECT * FROM patterns WHERE patternName = :patternName")
    suspend fun getPattern(patternName: String): PatternEntity?

    @Query("DELETE FROM patterns WHERE patternName = :patternName")
    suspend fun deletePattern(patternName: String)

    // Комбинированные операции
    @Transaction
    @Query("SELECT * FROM pattern_names")
    suspend fun getPatternsWithNames(): List<PatternWithName>

    @Transaction
    suspend fun insertPatternPair(patternName: PatternNameEntity, pattern: PatternEntity) {
        insertPatternName(patternName)
        insertPattern(pattern)
    }

    @Transaction
    suspend fun deletePatternPair(patternName: String) {
        deletePattern(patternName)
        deletePatternName(PatternNameEntity(patternName))
    }
}