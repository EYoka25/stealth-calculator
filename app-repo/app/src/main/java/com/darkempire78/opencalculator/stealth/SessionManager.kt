package com.darkempire78.opencalculator.stealth

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = StealthPreferences(context)

    fun isAuthenticated(): Boolean = prefs.hasActiveSession() && prefs.getAuthToken() != null

    fun getCurrentRoomId(): String? = prefs.getSavedRoomId()

    fun getSenderAlias(): String = prefs.getSenderAlias()

    fun saveSession(roomId: String, token: String, alias: String) {
        prefs.saveActiveSession(roomId, 0)
        prefs.setAuthToken(token)
        prefs.setSenderAlias(alias)
        prefs.markFirstLaunchComplete()
    }

    fun clearSession() {
        prefs.clearSession()
    }

    fun getServerUrl(): String = prefs.getServerUrl()

    fun getScrollIndex(): Int = prefs.getSavedScrollIndex()

    fun saveScrollIndex(index: Int) {
        prefs.saveActiveSession(prefs.getSavedRoomId() ?: "", index)
    }
}
