package com.darkempire78.opencalculator.stealth

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.darkempire78.opencalculator.activities.MainActivity

class StealthLifecycleObserver(
    private val activity: Activity,
    private val roomIdProvider: () -> String?,
    private val scrollIndexProvider: () -> Int
) : LifecycleEventObserver {

    companion object {
        private val DECOY_EXPRESSIONS = listOf(
            "52 + 13",
            "144 / 12",
            "92 - 37",
            "16 × 8",
            "225 + 47",
            "1000 - 256",
            "64 × 3",
            "81 / 9",
            "15 + 28",
            "360 / 12"
        )
    }

    private var isPanicActive = false

    override fun onStateChanged(source: LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
        when (event) {
            androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
            androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                if (!isPanicActive) {
                    executePanicSequence()
                }
            }
            else -> {}
        }
    }

    private fun executePanicSequence() {
        isPanicActive = true

        val prefs = StealthPreferences(activity)
        val roomId = roomIdProvider()

        // 1. State Preservation: Save roomId + scrollIndex
        if (roomId != null) {
            prefs.saveActiveSession(roomId, scrollIndexProvider())
        }

        // 2. Stack Wiping: Clear back stack completely
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("stealth_panic", true)
        }

        // 3. Visual Camouflage: Set a random believable expression
        val randomExpression = DECOY_EXPRESSIONS.random()
        prefs.saveRandomExpression(randomExpression)

        // 4. Force-Redirect
        activity.startActivity(intent)
        activity.finishAffinity()
    }

    fun setPanicActive(active: Boolean) {
        isPanicActive = active
    }
}
