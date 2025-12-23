package com.example.swand.domian.usecase

import com.example.swand.domian.models.Pattern
import com.example.swand.domian.models.Shift
import com.example.swand.domian.patterns.PatternMaker

class MakePatternFromShiftsUseCase(
    private val patternMaker: PatternMaker
) {
    operator fun invoke(shifts: List<Shift>): Pattern {
        return patternMaker.make(shifts)
    }
}