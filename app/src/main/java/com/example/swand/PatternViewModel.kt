package com.example.swand

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.swand.patterns.*
import kotlinx.coroutines.launch
import android.util.Log
import com.example.swand.data.db.PatternDatabase
import com.example.swand.domian.PatternRepository
import com.google.gson.Gson

class PatternViewModel(application: Application) : AndroidViewModel(application) {
    private val spenController = SpenController()
    private val discretizer = Discretizer(8)
    private val patternMakerConfig = PatternMakerConfig()
    private val patternMatcherConfig = PatternMatcherConfig()
    private val patternMaker = PatternMaker(discretizer, patternMakerConfig)
    private val patternMatcher = PatternMatcher(patternMatcherConfig)

    private val database = PatternDatabase.getDatabase(application)
    private val repository = PatternRepository(database.patternDao())
    private val gson = Gson()

    private val SIMILARITY_THRESHOLD = 0.7f

    // LiveData для UI
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isButtonPressed = MutableLiveData(false)
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _status = MutableLiveData("Initial S-Pen...")
    val status: LiveData<String> = _status

    private val _result = MutableLiveData("")
    val result: LiveData<String> = _result

    private val _mode = MutableLiveData("idle") // "record", "recognize", "idle"
    val mode: LiveData<String> = _mode

    private val _patterns = MutableLiveData<List<String>>(emptyList())
    val patterns: LiveData<List<String>> = _patterns

    private val recordedShifts = mutableListOf<Shift>()
    private var currentPatternName = ""

    private val TAG = "ViewModelMatcher"

    init {
        spenController.init()
        setupSpenCallbacks()

        viewModelScope.launch {
            loadPatternsFromDatabase()
        }
    }

    private fun setupSpenCallbacks() {
        spenController.onConnected = {
            viewModelScope.launch {
                _isConnected.value = true
                _status.value = "S-Pen connected"
            }
        }

        spenController.onDisconnected = {
            viewModelScope.launch {
                _isConnected.value = false
                _isButtonPressed.value = false
                _status.value = "S-Pen disconnected"
            }
        }

        spenController.onConnectionError = { errorCode ->
            viewModelScope.launch {
                _isConnected.value = false
                _status.value = "Error connection: $errorCode"
            }
        }

        spenController.onButtonPressed = {
            viewModelScope.launch {
                if (_mode.value != "idle") {
                    _isButtonPressed.value = true
                    recordedShifts.clear()
                    spenController.startAirMotion()
                    _status.value = "Reading started. Move S-Pen."
                }
            }
        }

        spenController.onButtonReleased = {
            viewModelScope.launch {
                _isButtonPressed.value = false
                spenController.stopAirMotion()
                _status.value = "Reading finished."
                processRecordedShifts()
            }
        }

        spenController.onAirMotion = { deltaX, deltaY ->
            viewModelScope.launch {
                if (_isButtonPressed.value == true) {
                    val shift = Shift(deltaX, deltaY)
                    recordedShifts.add(shift)
                }
            }
        }
    }

    private suspend fun loadPatternsFromDatabase() {
        try {
            val patternPairs = repository.getAllPatterns()
            updatePatternsList(patternPairs)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved patterns: ${e.message}")
        }
    }

    private fun updatePatternsList(patternPairs: List<Pair<PatternName, Pattern>>) {
        val patternsString = patternPairs.map { (name, pattern) ->
            name.name
        }.toList()
        _patterns.value = patternsString
    }

    private suspend fun processRecordedShifts() {
        if (recordedShifts.isEmpty()) {
            _result.value = "Pattern is empty"
            return
        }

        val pattern = patternMaker.make(recordedShifts)

        when (_mode.value) {
            "record" -> {
                if (currentPatternName.isNotBlank()) {
                    try {
                        val success = repository.savePatternPair(
                            PatternName(currentPatternName),
                            pattern
                        )

                        if (success) {
                            val patternPairs = repository.getAllPatterns()
                            updatePatternsList(patternPairs)

                            _result.value = "Saved"
                            _status.value = "Pattern '$currentPatternName' saved"
                            currentPatternName = ""
                        } else {
                            _result.value = "Error saving pattern"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving to DB: ${e.message}")
                        _result.value = "Error saving"
                    }
                } else {
                    _result.value = "Enter pattern name"
                }
            }
            "recognize" -> {
                try {
                    val savedPatterns = repository.getAllPatterns()
                    var foundPattern: String? = null
                    var maxSimilarity: Float = 0f

                    for ((patternName, savedPattern) in savedPatterns) {
                        val similarity = patternMatcher.match(pattern, savedPattern)
                        if (similarity > maxSimilarity) {
                            maxSimilarity = similarity
                            foundPattern = patternName.name
                        }

                        Log.d(TAG, "Comparing with ${patternName.name}: similarity = $similarity")
                    }

                    Log.d(TAG, "Best similarity with: $foundPattern value: $maxSimilarity")

                    if (foundPattern != null && maxSimilarity >= SIMILARITY_THRESHOLD) {
                        _result.value = "Detected: $foundPattern (similarity: ${"%.2f".format(maxSimilarity)})"
                        _status.value = "Pattern looks like $foundPattern"
                    } else {
                        _result.value = "Unknown pattern (max similarity: ${"%.2f".format(maxSimilarity)})"
                        _status.value = "Pattern not detected"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading saved patterns from DB: ${e.message}")
                    _result.value = "Error detecting"
                }
            }
        }
    }

    fun connect(context: Context) {
        try {
            if (!spenController.checkFeatures().first || !spenController.checkFeatures().second)
            {
                _status.value = "Error connection: missing main features"
            }
            else {
                spenController.connect(context)
            }

        } catch (e: Exception) {
            _status.value = "Error connection: ${e.message}"
        }
    }

    fun disconnect(context: Context) {
        spenController.disconnect(context)
    }

    fun setMode(newMode: String) {
        _mode.value = newMode
        currentPatternName = ""

        when (newMode) {
            "record" -> {
                _status.value = "Writing. Enter pattern name."
                _result.value = ""
            }
            "recognize" -> {
                _status.value = "Reading"
                _result.value = ""
            }
            else -> {
                _status.value = "Wait"
                _result.value = ""
            }
        }
    }

    fun setPatternName(name: String) {
        currentPatternName = name
        _status.value = "Name is set: $name"
    }

    fun deletePattern(patternName: String) {
        viewModelScope.launch {
            try {
                val success = repository.deletePatternPair(patternName)
                if (success) {
                    loadPatternsFromDatabase()
                    _status.value = "Pattern '$patternName' deleted"
                } else {
                    _status.value = "Error deleting pattern"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting pattern: ${e.message}")
                _status.value = "Error deleting"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        spenController.cleanup()
    }
}