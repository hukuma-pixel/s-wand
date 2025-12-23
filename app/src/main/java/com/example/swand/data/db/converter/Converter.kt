package com.example.swand.data.db.converter

import androidx.room.TypeConverter
import com.example.swand.domian.models.Direction
import com.example.swand.domian.models.PatternSegment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromDirection(direction: Direction): String {
        return gson.toJson(direction)
    }

    @TypeConverter
    fun toDirection(directionString: String): Direction {
        return gson.fromJson(directionString, Direction::class.java)
    }

    @TypeConverter
    fun fromPatternSegmentList(segments: List<PatternSegment>): String {
        return gson.toJson(segments)
    }

    @TypeConverter
    fun toPatternSegmentList(segmentsString: String): List<PatternSegment> {
        val type = object : TypeToken<List<PatternSegment>>() {}.type
        return gson.fromJson(segmentsString, type)
    }
}