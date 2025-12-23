package com.example.swand.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swand.data.db.PatternDatabase
import com.example.swand.data.db.repository.PatternRepositoryImpl
import com.example.swand.domian.patterns.Discretizer
import com.example.swand.domian.patterns.PatternMaker
import com.example.swand.domian.patterns.PatternMakerConfig
import com.example.swand.domian.patterns.impl.PatternMatcherImpl
import com.example.swand.domian.patterns.impl.PatternMatcherConfig
import com.example.swand.domian.repository.PatternRepository
import com.example.swand.domian.usecase.*
import com.example.swand.presentation.viewmodel.PatternViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    private val discretizer: Discretizer by lazy { Discretizer(8) }
    private val patternMaker: PatternMaker by lazy {
        PatternMaker(discretizer, PatternMakerConfig())
    }
    private val patternMatcher: PatternMatcherImpl by lazy {
        PatternMatcherImpl(PatternMatcherConfig())
    }

    private val patternRepository: PatternRepository by lazy {
        val database = PatternDatabase.getDatabase(application)
        PatternRepositoryImpl(database.patternDao())
    }

    private val createPatternUseCase: CreatePatternUseCase by lazy {
        CreatePatternUseCase(patternMaker, patternRepository)
    }

    private val getPatternsUseCase: GetPatternsUseCase by lazy {
        GetPatternsUseCase(patternRepository)
    }

    private val deletePatternUseCase: DeletePatternUseCase by lazy {
        DeletePatternUseCase(patternRepository)
    }

    private val makePatternFromShiftsUseCase: MakePatternFromShiftsUseCase by lazy {
        MakePatternFromShiftsUseCase(patternMaker)
    }

    private val recognizePatternUseCase: RecognizePatternUseCase by lazy {
        RecognizePatternUseCase(patternRepository, patternMatcher)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatternViewModel::class.java)) {
            return PatternViewModel(
                application = application,
                createPatternUseCase = createPatternUseCase,
                getPatternsUseCase = getPatternsUseCase,
                deletePatternUseCase = deletePatternUseCase,
                makePatternFromShiftsUseCase = makePatternFromShiftsUseCase,
                recognizePatternUseCase = recognizePatternUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}