package com.example.swand

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.penremote.*
import com.samsung.android.sdk.penremote.SpenRemote.FEATURE_TYPE_AIR_MOTION
import com.samsung.android.sdk.penremote.SpenRemote.FEATURE_TYPE_BUTTON

class SpenController {

    private var spenRemote: SpenRemote? = null
    private var spenUnitManager: SpenUnitManager? = null

    var onConnected: ((SpenUnitManager) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onConnectionError: ((Int) -> Unit)? = null
    var onButtonPressed: (() -> Unit)? = null
    var onButtonReleased: (() -> Unit)? = null
    var onAirMotion: ((Float, Float) -> Unit)? = null

    private var isAirMotionListening = false
    private val TAG = "SpenController"

    fun init() {
        try {
            spenRemote = SpenRemote.getInstance()
            Log.d(TAG, "S Pen SDK version: ${spenRemote?.versionName}")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to initialize S Pen Remote SDK: ${e.message}")
        }
    }

    fun connect(context: Context) {
        if (isConnected()) {
            Log.w(TAG, "Has connected")
            return
        }

        spenRemote?.apply {
            setConnectionStateChangeListener { state ->
                Log.d(TAG, "State: $state")
                if (state == SpenRemote.State.DISCONNECTED) {
                    onDisconnected?.invoke()
                }
            }

            connect(context, object : SpenRemote.ConnectionResultCallback {
                override fun onSuccess(unitManager: SpenUnitManager) {
                    handleConnectionSuccess(unitManager)
                }

                override fun onFailure(errorCode: Int) {
                    handleConnectionFailure(errorCode)
                }
            })
        }
    }

    fun disconnect(context: Context) {
        stopAirMotion()
        spenRemote?.disconnect(context)
        spenUnitManager = null
        onDisconnected?.invoke()
        Log.d(TAG, "Disconnected")
    }

    fun isConnected(): Boolean {
        return spenRemote?.isConnected == true
    }

    fun checkFeatures(): Pair<Boolean, Boolean> {
        return try {
            Pair(
                spenRemote?.isFeatureEnabled(FEATURE_TYPE_BUTTON) ?: false,
                spenRemote?.isFeatureEnabled(FEATURE_TYPE_AIR_MOTION) ?: false
            )
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "Error checking features due to missing Samsung classes: ${e.message}")
            Pair(false, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking features: ${e.message}")
            Pair(false, false)
        }
    }


    fun listenToButton() {
        spenUnitManager?.let { manager ->
            val buttonUnit = manager.getUnit(SpenUnit.TYPE_BUTTON)

            val listener = SpenEventListener { event ->
                val buttonEvent = ButtonEvent(event)

                when (buttonEvent.action) {
                    ButtonEvent.ACTION_DOWN -> {
                        // Log.d(TAG, "Button pressed")
                        onButtonPressed?.invoke()
                    }

                    ButtonEvent.ACTION_UP -> {
                        // Log.d(TAG, "Button released")
                        onButtonReleased?.invoke()
                    }
                }
            }

            manager.registerSpenEventListener(listener, buttonUnit)
        }
    }

    fun startAirMotion() {
        if (!isAirMotionListening) {
            spenUnitManager?.let { manager ->
                val motionUnit = manager.getUnit(SpenUnit.TYPE_AIR_MOTION)

                val listener = SpenEventListener { event ->
                    val motionEvent = AirMotionEvent(event)
                    val x = motionEvent.deltaX
                    val y = motionEvent.deltaY

                    // Log.d(TAG, "Motion: X=$x, Y=$y")
                    onAirMotion?.invoke(x, y)
                }

                manager.registerSpenEventListener(listener, motionUnit)
                isAirMotionListening = true
            }
        }
    }

    fun stopAirMotion() {
        if (isAirMotionListening) {
            spenUnitManager?.let { manager ->
                val motionUnit = manager.getUnit(SpenUnit.TYPE_AIR_MOTION)
                manager.unregisterSpenEventListener(motionUnit)
                isAirMotionListening = false
            }
        }
    }

    fun cleanup() {
        onConnected = null
        onDisconnected = null
        onConnectionError = null
        onButtonPressed = null
        onButtonReleased = null
        onAirMotion = null
        Log.d(TAG, "Cleaned")
    }

    private fun handleConnectionSuccess(unitManager: SpenUnitManager) {
        spenUnitManager = unitManager
        Log.d(TAG, "Connected successfully")

        listenToButton()

        onConnected?.invoke(unitManager)
    }

    private fun handleConnectionFailure(errorCode: Int) {
        Log.e(TAG, "Error connection: $errorCode")
        onConnectionError?.invoke(errorCode)
    }
}