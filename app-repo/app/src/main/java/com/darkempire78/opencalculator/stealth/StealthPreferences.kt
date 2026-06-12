package com.darkempire78.opencalculator.stealth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StealthPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "stealth_encrypted_prefs"
        private const val KEY_ACTIVE_ROOM_ID = "active_room_id"
        private const val KEY_SCROLL_INDEX = "scroll_index"
        private const val KEY_SENDER_ALIAS = "sender_alias"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }

    fun saveActiveSession(roomId: String, scrollIndex: Int) {
        prefs.edit {
            putString(KEY_ACTIVE_ROOM_ID, roomId)
            putInt(KEY_SCROLL_INDEX, scrollIndex)
        }
    }

    fun getSavedRoomId(): String? = prefs.getString(KEY_ACTIVE_ROOM_ID, null)

    fun getSavedScrollIndex(): Int = prefs.getInt(KEY_SCROLL_INDEX, 0)

    fun hasActiveSession(): Boolean = !getSavedRoomId().isNullOrBlank()

    fun clearSession() {
        prefs.edit {
            remove(KEY_ACTIVE_ROOM_ID)
            remove(KEY_SCROLL_INDEX)
            remove(KEY_AUTH_TOKEN)
        }
    }

    fun setSenderAlias(alias: String) {
        prefs.edit { putString(KEY_SENDER_ALIAS, alias) }
    }

    fun getSenderAlias(): String = prefs.getString(KEY_SENDER_ALIAS, "User") ?: "User"

    fun setServerUrl(url: String) {
        prefs.edit { putString(KEY_SERVER_URL, url) }
    }

    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"

    fun setAuthToken(token: String) {
        prefs.edit { putString(KEY_AUTH_TOKEN, token) }
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    fun markFirstLaunchComplete() {
        prefs.edit { putBoolean(KEY_IS_FIRST_LAUNCH, false) }
    }

    fun saveRandomExpression(expression: String) {
        prefs.edit { putString("last_expression", expression) }
    }

    fun getLastExpression(): String? = prefs.getString("last_expression", null)
}
