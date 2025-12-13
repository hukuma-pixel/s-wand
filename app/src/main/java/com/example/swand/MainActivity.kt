package com.example.swand

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PatternViewModel

    // UI элементы
    private lateinit var statusTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var connectionStatusTextView: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var recordingRadioButton: RadioButton
    private lateinit var recognizingRadioButton: RadioButton
    private lateinit var idleRadioButton: RadioButton
    private lateinit var patternNameEditText: EditText
    private lateinit var saveNameButton: Button
    private lateinit var patternsListView: ListView
    private lateinit var recordingIndicator: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем тему перед super.onCreate
        setTheme(R.style.Theme_SWand)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация ViewModel
        viewModel = ViewModelProvider(this).get(PatternViewModel::class.java)

        // Инициализация UI элементов
        initViews()
        setupListeners()
        setupObservers()
    }

    private fun initViews() {
        statusTextView = findViewById(R.id.statusTextView)
        resultTextView = findViewById(R.id.resultTextView)
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        recordingRadioButton = findViewById(R.id.recordingRadioButton)
        recognizingRadioButton = findViewById(R.id.recognizingRadioButton)
        idleRadioButton = findViewById(R.id.idleRadioButton)
        patternNameEditText = findViewById(R.id.patternNameEditText)
        saveNameButton = findViewById(R.id.saveNameButton)
        patternsListView = findViewById(R.id.patternsListView)
        recordingIndicator = findViewById(R.id.recordingIndicator)
    }

    private fun setupListeners() {
        // Кнопки подключения/отключения
        connectButton.setOnClickListener {
            viewModel.connect(this)
            Toast.makeText(this, "Подключение...", Toast.LENGTH_SHORT).show()
        }

        disconnectButton.setOnClickListener {
            viewModel.disconnect(this)
            Toast.makeText(this, "Отключение...", Toast.LENGTH_SHORT).show()
        }

        // Радиокнопки режима
        recordingRadioButton.setOnClickListener {
            viewModel.setMode("record")
            patternNameEditText.visibility = EditText.VISIBLE
            saveNameButton.visibility = Button.VISIBLE
            patternNameEditText.text.clear()
        }

        recognizingRadioButton.setOnClickListener {
            viewModel.setMode("recognize")
            patternNameEditText.visibility = EditText.GONE
            saveNameButton.visibility = Button.GONE
        }

        idleRadioButton.setOnClickListener {
            viewModel.setMode("idle")
            patternNameEditText.visibility = EditText.GONE
            saveNameButton.visibility = Button.GONE
        }

        // Кнопка сохранения имени
        saveNameButton.setOnClickListener {
            val name = patternNameEditText.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.setPatternName(name)
                Toast.makeText(this, "Имя установлено", Toast.LENGTH_SHORT).show()
                patternNameEditText.text.clear()
            } else {
                Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        // Статус подключения
        viewModel.isConnected.observe(this, Observer { isConnected ->
            if (isConnected) {
                connectionStatusTextView.text = "Подключено ✓"
                connectionStatusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
            } else {
                connectionStatusTextView.text = "Отключено ✗"
                connectionStatusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                connectButton.isEnabled = true
                disconnectButton.isEnabled = false
            }
        })

        // Статус
        viewModel.status.observe(this, Observer { status ->
            statusTextView.text = status
        })

        // Результат
        viewModel.result.observe(this, Observer { result ->
            resultTextView.text = result
            updateResultColor(result)
        })

        // Состояние кнопки S-Pen
        viewModel.isButtonPressed.observe(this, Observer { isPressed ->
            if (isPressed) {
                recordingIndicator.visibility = TextView.VISIBLE
            } else {
                recordingIndicator.visibility = TextView.GONE
            }
        })

        // Режим
        viewModel.mode.observe(this, Observer { mode ->
            updateModeUI(mode)
        })

        // Список паттернов
        viewModel.patterns.observe(this, Observer { patterns ->
            // Паттерны уже содержат информацию о direction и weight
            // благодаря изменениям в ViewModel
            updatePatternsList(patterns)
        })
    }

    private fun updateResultColor(result: String) {
        when {
            result == "Неизвестный паттерн" ->
                resultTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            result.startsWith("Сохранено") || result.startsWith("Распознан") ->
                resultTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            else ->
                resultTextView.setTextColor(resources.getColor(android.R.color.black))
        }
    }

    private fun updateModeUI(mode: String) {
        when (mode) {
            "record" -> {
                recordingRadioButton.isChecked = true
                patternNameEditText.visibility = EditText.VISIBLE
                saveNameButton.visibility = Button.VISIBLE
            }
            "recognize" -> {
                recognizingRadioButton.isChecked = true
                patternNameEditText.visibility = EditText.GONE
                saveNameButton.visibility = Button.GONE
            }
            "idle" -> {
                idleRadioButton.isChecked = true
                patternNameEditText.visibility = EditText.GONE
                saveNameButton.visibility = Button.GONE
            }
        }
    }

    private fun updatePatternsList(patterns: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, patterns)
        patternsListView.adapter = adapter

        patternsListView.setOnItemClickListener { _, _, position, _ ->
            val patternName = patterns[position]
            Toast.makeText(this, "Выбран: $patternName", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отключаем S-Pen при закрытии приложения
        if (viewModel.isConnected.value == true) {
            viewModel.disconnect(this)
        }
    }
}