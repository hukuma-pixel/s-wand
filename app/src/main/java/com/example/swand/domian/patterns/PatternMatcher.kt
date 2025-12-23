package com.example.swand.domian.patterns

import com.example.swand.domian.models.Pattern

interface PatternMatcher {
    fun match(pattern1: Pattern, pattern2: Pattern): Float
}
