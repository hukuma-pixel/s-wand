package com.example.swand.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_names")
data class PatternNameEntity(
    @PrimaryKey
    val name: String
)

@Entity(
    tableName = "patterns",
    foreignKeys = [
        ForeignKey(
            entity = PatternNameEntity::class,
            parentColumns = ["name"],
            childColumns = ["patternName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patternName: String,
    val segmentsJson: String
)