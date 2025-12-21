package com.example.swand.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class PatternWithName(
    @Embedded
    val patternName: PatternNameEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "patternName"
    )
    val pattern: PatternEntity
)