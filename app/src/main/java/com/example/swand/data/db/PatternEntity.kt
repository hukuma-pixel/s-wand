package com.example.swand.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_names")
data class PatternNameEntity(
    @PrimaryKey
    val name: String
)

@Entity(
    tableName = "patterns",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = PatternNameEntity::class,
            parentColumns = ["name"],
            childColumns = ["patternName"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class PatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patternName: String,
    val segmentsJson: String
)