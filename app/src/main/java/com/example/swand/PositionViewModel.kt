package com.example.swand

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PositionViewModel(application: Application) : AndroidViewModel(application) {
    private val positionManager = PositionManager()
    private val spenController = SpenController()

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isButtonPressed = MutableLiveData(false)
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _currentPosition = MutableLiveData(Pair(0f, 0f))
    val currentPosition: LiveData<Pair<Float, Float>> = _currentPosition

    private val _drawingPath = MutableLiveData<List<Pair<Float, Float>>>()
    val drawingPath: LiveData<List<Pair<Float, Float>>> = _drawingPath

    init {
        spenController.init()
        setupSpenCallbacks()
    }

    private fun setupSpenCallbacks() {
        spenController.onConnected = {
            viewModelScope.launch {
                _isConnected.value = true
            }
        }

        spenController.onDisconnected = {
            viewModelScope.launch {
                _isConnected.value = false
                _isButtonPressed.value = false
            }
        }

        spenController.onConnectionError = { _ ->
            viewModelScope.launch {
                _isConnected.value = false
            }
        }

        spenController.onButtonPressed = {
            viewModelScope.launch {
                _isButtonPressed.value = true
                positionManager.resetPosition()
                _currentPosition.value = Pair(0f, 0f)
                _drawingPath.value = emptyList()
                spenController.startAirMotion()
            }
        }

        spenController.onButtonReleased = {
            viewModelScope.launch {
                _isButtonPressed.value = false
                spenController.stopAirMotion()
            }
        }

        spenController.onAirMotion = { deltaX, deltaY ->
            viewModelScope.launch {
                positionManager.changePosition(deltaX, deltaY)
                val x = positionManager.getX()
                val y = positionManager.getY()
                _currentPosition.value = Pair(x, y)

                if (_isButtonPressed.value ?: false) {
                    val currentPath = _drawingPath.value.toMutableList()
                    currentPath.add(Pair(x, y))
                    _drawingPath.value = currentPath
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

    override fun onCleared() {
        super.onCleared()
        spenController.cleanup()
    }
}