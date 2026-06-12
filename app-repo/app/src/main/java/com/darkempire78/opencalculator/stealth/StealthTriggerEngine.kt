package com.darkempire78.opencalculator.stealth

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button

class StealthTriggerEngine(
    private val getResultDisplayText: () -> String,
    private val onTrigger: () -> Unit
) {
    private var squareButtonPressed = false
    private var equalsButtonPressed = false
    private var squarePressTime = 0L
    private var equalsPressTime = 0L

    companion object {
        private const val LONG_PRESS_DURATION_MS = 800L
        private const val SIMULTANEOUS_WINDOW_MS = 300L
        private const val TARGET_RESULT = "2084"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var triggerRunnable: Runnable? = null

    fun attachToButtons(squareButton: Button, equalsButton: Button) {
        squareButton.setOnTouchListener { view, event ->
            handleSquareTouch(event)
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false // Retain false so normal calculator button functions still trigger
        }

        equalsButton.setOnTouchListener { view, event ->
            handleEqualsTouch(event)
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            false // Retain false so normal calculator button functions still trigger
        }
    }

    private fun handleSquareTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                squareButtonPressed = true
                squarePressTime = SystemClock.elapsedRealtime()
                checkSimultaneousLongPress()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                squareButtonPressed = false
                cancelTrigger()
            }
        }
    }

    private fun handleEqualsTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                equalsButtonPressed = true
                equalsPressTime = SystemClock.elapsedRealtime()
                checkSimultaneousLongPress()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                equalsButtonPressed = false
                cancelTrigger()
            }
        }
    }

    private fun checkSimultaneousLongPress() {
        if (squareButtonPressed && equalsButtonPressed) {
            val timeDiff = kotlin.math.abs(squarePressTime - equalsPressTime)
            if (timeDiff <= SIMULTANEOUS_WINDOW_MS) {
                val earliestPress = minOf(squarePressTime, equalsPressTime)
                val delay = maxOf(0L, LONG_PRESS_DURATION_MS - (SystemClock.elapsedRealtime() - earliestPress))

                triggerRunnable?.let { handler.removeCallbacks(it) }
                triggerRunnable = Runnable {
                    if (squareButtonPressed && equalsButtonPressed) {
                        validateAndTrigger()
                    }
                }
                handler.postDelayed(triggerRunnable!!, delay)
            }
        }
    }

    private fun validateAndTrigger() {
        val rawResult = getResultDisplayText()
        
        // 🛡️ Robust Check: Strip out commas, decimals, trailing zeros, or spaces
        // If rawResult is "2,084.00" -> sanitized becomes "2084"
        val sanitizedResult = rawResult.split(".")[0].filter { it.isDigit() }
        
        if (sanitizedResult == TARGET_RESULT) {
            onTrigger()
        }
    }

    fun cancelTrigger() {
        triggerRunnable?.let { handler.removeCallbacks(it) }
        triggerRunnable = null
    }

    fun detach() {
        cancelTrigger()
        squareButtonPressed = false
        equalsButtonPressed = false
    }
}