package com.example.swand.presentation.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.swand.domian.models.Shift
import com.example.swand.domian.spencontroller.SpenController
import com.example.swand.domian.usecase.*
import kotlinx.coroutines.launch
import kotlin.math.abs

class PatternViewModel(
    application: Application,
    private val createPatternUseCase: CreatePatternUseCase,
    private val getPatternsUseCase: GetPatternsUseCase,
    private val deletePatternUseCase: DeletePatternUseCase,
    private val makePatternFromShiftsUseCase: MakePatternFromShiftsUseCase,
    private val recognizePatternUseCase: RecognizePatternUseCase
) : AndroidViewModel(application) {

    private val spenController = SpenController()

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isButtonPressed = MutableLiveData(false)
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _status = MutableLiveData("Initializing S-Pen...")
    val status: LiveData<String> = _status

    private val _result = MutableLiveData("")
    val result: LiveData<String> = _result

    private val _mode = MutableLiveData("idle") // "record", "recognize", "idle"
    val mode: LiveData<String> = _mode

    private val _patterns = MutableLiveData<List<String>>(emptyList())
    val patterns: LiveData<List<String>> = _patterns

    private val recordedShifts = mutableListOf<Shift>()
    private var currentPatternName = ""

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
                _status.value = "Connection error: $errorCode"
            }
        }

        spenController.onButtonPressed = {
            viewModelScope.launch {
                if (_mode.value != "idle") {
                    _isButtonPressed.value = true
                    recordedShifts.clear()
                    spenController.startAirMotion()
                    _status.value = "Recording started. Move S-Pen."
                }
            }
        }

        spenController.onButtonReleased = {
            viewModelScope.launch {
                _isButtonPressed.value = false
                spenController.stopAirMotion()
                _status.value = "Recording finished."
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
        val result = getPatternsUseCase()
        result.onSuccess { patternPairs ->
            val patternsString = patternPairs.map { (name, _) ->
                name.name
            }
            _patterns.value = patternsString
        }.onFailure { error ->
            _status.value = "Error loading patterns: ${error.message}"
        }
    }

    private suspend fun processRecordedShifts() {
        if (recordedShifts.isEmpty()) {
            _result.value = "Pattern is empty"
            return
        }

        val filteredShifts = recordedShifts.filter { shift ->
            abs(shift.dx) > 0.01f || abs(shift.dy) > 0.01f
        }

        if (filteredShifts.isEmpty()) {
            _result.value = "Pattern is too small"
            return
        }

        when (_mode.value) {
            "record" -> recordPattern(filteredShifts)
            "recognize" -> recognizePattern(filteredShifts)
        }
    }

    private suspend fun recordPattern(shifts: List<Shift>) {
        if (currentPatternName.isBlank()) {
            _result.value = "Please enter pattern name"
            return
        }

        val result = createPatternUseCase(currentPatternName, shifts)
        result.onSuccess {
            loadPatternsFromDatabase()
            _result.value = "Pattern '$currentPatternName' saved successfully"
            _status.value = "Pattern saved"
            currentPatternName = ""
        }.onFailure { error ->
            _result.value = "Error saving pattern: ${error.message}"
            _status.value = "Save failed"
        }
    }

    @SuppressLint("DefaultLocale")
    private suspend fun recognizePattern(shifts: List<Shift>) {
        val pattern = makePatternFromShiftsUseCase(shifts)

        if (pattern.segments.isEmpty()) {
            _result.value = "Pattern is empty after processing"
            return
        }

        val recognitionResult = recognizePatternUseCase(pattern)

        if (recognitionResult.isRecognized) {
            _result.value = "Recognized: ${recognitionResult.recognizedPattern} "
            _status.value = "Pattern recognized"
        } else {
            _result.value = "Unknown pattern"
            _status.value = "Pattern not recognized"
        }
    }

    fun connect(context: Context) {
        try {
            val features = spenController.checkFeatures()
            if (!features.first || !features.second) {
                _status.value = "Error: Missing required S-Pen features"
            } else {
                spenController.connect(context)
            }
        } catch (e: Exception) {
            _status.value = "Connection error: ${e.message}"
        }
    }

    fun disconnect(context: Context) {
        spenController.disconnect(context)
    }

    fun setMode(newMode: String) {
        _mode.value = newMode
        recordedShifts.clear()
        currentPatternName = ""

        when (newMode) {
            "record" -> {
                _status.value = "Record mode: Enter pattern name"
                _result.value = ""
            }
            "recognize" -> {
                _status.value = "Recognize mode: Press button and move S-Pen"
                _result.value = ""
            }
            else -> {
                _status.value = "Idle mode"
                _result.value = ""
            }
        }
    }

    fun setPatternName(name: String) {
        currentPatternName = name
        _status.value = "Pattern name set: '$name'"
    }

    fun deletePattern(patternName: String) {
        viewModelScope.launch {
            val result = deletePatternUseCase(patternName)
            result.onSuccess {
                loadPatternsFromDatabase()
                _status.value = "Pattern '$patternName' deleted"
                _result.value = "Pattern deleted"
            }.onFailure { error ->
                _status.value = "Error deleting pattern: ${error.message}"
                _result.value = "Delete failed"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        spenController.cleanup()
    }
}