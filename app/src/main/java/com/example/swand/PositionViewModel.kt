package com.example.swand

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.swand.patterns.*
import kotlinx.coroutines.launch

class PatternViewModel(application: Application) : AndroidViewModel(application) {
    private val spenController = SpenController()
    private val discretizer = Discretizer(8)
    private val patternMakerConfig = PatternMakerConfig()
    private val patternMatcherConfig = PatternMatcherConfig()
    private val patternMaker = PatternMaker(discretizer, patternMakerConfig)
    private val patternMatcher = PatternMatcher(patternMatcherConfig)

    // LiveData для UI
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isButtonPressed = MutableLiveData(false)
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _status = MutableLiveData("Инициализация S-Pen...")
    val status: LiveData<String> = _status

    private val _result = MutableLiveData("")
    val result: LiveData<String> = _result

    private val _mode = MutableLiveData("idle") // "record", "recognize", "idle"
    val mode: LiveData<String> = _mode

    private val _patterns = MutableLiveData<List<String>>(emptyList())
    val patterns: LiveData<List<String>> = _patterns

    // Данные для паттернов
    private val recordedShifts = mutableListOf<Shift>()
    private val savedPatterns = mutableMapOf<String, Pattern>()
    private var currentPatternName = ""

    init {
        spenController.init()
        setupSpenCallbacks()
    }

    private fun setupSpenCallbacks() {
        spenController.onConnected = {
            viewModelScope.launch {
                _isConnected.value = true
                _status.value = "S-Pen подключен"
            }
        }

        spenController.onDisconnected = {
            viewModelScope.launch {
                _isConnected.value = false
                _isButtonPressed.value = false
                _status.value = "S-Pen отключен"
            }
        }

        spenController.onConnectionError = { errorCode ->
            viewModelScope.launch {
                _isConnected.value = false
                _status.value = "Ошибка подключения: $errorCode"
            }
        }

        spenController.onButtonPressed = {
            viewModelScope.launch {
                if (_mode.value != "idle") {
                    _isButtonPressed.value = true
                    recordedShifts.clear()
                    spenController.startAirMotion()
                    _status.value = "Запись начата. Двигайте S-Pen."
                }
            }
        }

        spenController.onButtonReleased = {
            viewModelScope.launch {
                _isButtonPressed.value = false
                spenController.stopAirMotion()
                _status.value = "Запись завершена."
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

    private fun patternToString(pattern: Pattern): String {
        return pattern.segments.joinToString(", ") {
            "(${it.direction.index}, ${it.weight})"
        }
    }

    private fun processRecordedShifts() {
        if (recordedShifts.isEmpty()) {
            _result.value = "Паттерн пуст"
            return
        }

        val pattern = patternMaker.make(recordedShifts)
        val patternString = patternToString(pattern) // Получаем строковое представление

        when (_mode.value) {
            "record" -> {
                if (currentPatternName.isNotBlank()) {
                    savedPatterns[currentPatternName] = pattern
                    // Обновляем список паттернов с информацией о направлении и весе
                    _patterns.value = savedPatterns.map { (name, pat) ->
                        "$name: ${patternToString(pat)}"
                    }.toList()
                    _result.value = "Сохранено: $currentPatternName\n${patternToString(pattern)}" // Добавляем информацию о паттерне
                    _status.value = "Паттерн '$currentPatternName' сохранен"
                    currentPatternName = ""
                } else {
                    _result.value = "Введите имя паттерна"
                }
            }
            "recognize" -> {
                var foundPattern: String? = null
                for ((name, savedPattern) in savedPatterns) {
                    if (patternMatcher.match(pattern, savedPattern)) {
                        foundPattern = name
                        break
                    }
                }

                if (foundPattern != null) {
                    _result.value = "Распознан: $foundPattern"
                    _status.value = "Паттерн '$patternString' распознан"
                } else {
                    _result.value = "Неизвестный паттерн"
                    _status.value = "Паттерн '$patternString' распознан"
                }
            }
        }
    }

    fun connect(context: Context) {
        spenController.connect(context)
    }

    fun disconnect(context: Context) {
        spenController.disconnect(context)
    }

    fun setMode(newMode: String) {
        _mode.value = newMode
        currentPatternName = ""

        when (newMode) {
            "record" -> {
                _status.value = "Режим записи. Введите имя паттерна."
                _result.value = ""
            }
            "recognize" -> {
                _status.value = "Режим распознавания. Нажмите кнопку S-Pen."
                _result.value = ""
            }
            else -> {
                _status.value = "Режим ожидания"
                _result.value = ""
            }
        }
    }

    fun setPatternName(name: String) {
        currentPatternName = name
        _status.value = "Имя установлено: $name"
    }

    fun getSavedPatterns(): List<String> {
        return savedPatterns.map { (name, pattern) ->
            "$name: ${patternToString(pattern)}"
        }.toList()
    }

    override fun onCleared() {
        super.onCleared()
        spenController.cleanup()
    }
}