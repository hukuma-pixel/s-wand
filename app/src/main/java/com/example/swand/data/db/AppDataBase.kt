package com.example.swand.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.swand.data.db.converter.Converters
import com.example.swand.data.db.dao.PatternDao
import com.example.swand.data.db.entity.PatternEntity
import com.example.swand.data.db.entity.PatternNameEntity

@Database(
    entities = [PatternNameEntity::class, PatternEntity::class],
    version = 1,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class PatternDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao

    companion object {
        @Volatile
        private var INSTANCE: PatternDatabase? = null

        fun getDatabase(context: Context): PatternDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PatternDatabase::class.java,
                    "pattern_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}