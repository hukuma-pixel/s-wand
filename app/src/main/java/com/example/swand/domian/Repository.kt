package com.example.swand.domian

import com.example.swand.data.db.PatternDao
import com.example.swand.data.db.PatternEntity
import com.example.swand.patterns.Pattern
import com.example.swand.patterns.Shift
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PatternRepository(private val dao: PatternDao) {
}