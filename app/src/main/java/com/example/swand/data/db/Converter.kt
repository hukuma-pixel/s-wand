package com.example.swand.data.db

import androidx.room.TypeConverter
import com.example.swand.patterns.Shift
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromString(value: String): List<Shift> {
        val listType = object : TypeToken<List<Shift>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromPointList(list: List<Shift>): String {
        return Gson().toJson(list)
    }
}