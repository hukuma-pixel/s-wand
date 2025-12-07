package com.example.swand.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patterns")
data class PatternEntity(
    @PrimaryKey val id: String,
    val pointsJson: String
)