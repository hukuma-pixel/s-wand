package com.example.swand.data.db

import androidx.room.*

@Dao
interface PatternDao {
    @Query("SELECT * FROM patterns WHERE id = :id")
    suspend fun getPatternById(id: String): PatternEntity?

    @Query("SELECT * FROM patterns")
    suspend fun getAllPatterns(): List<PatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: PatternEntity)

    @Delete
    suspend fun deletePattern(pattern: PatternEntity)
}