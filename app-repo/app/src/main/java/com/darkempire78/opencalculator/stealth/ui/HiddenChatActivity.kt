package com.darkempire78.opencalculator.stealth.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.activities.MainActivity
import com.darkempire78.opencalculator.stealth.SessionManager
import com.darkempire78.opencalculator.stealth.StealthLifecycleObserver
import com.darkempire78.opencalculator.stealth.StealthPreferences
import com.darkempire78.opencalculator.stealth.network.ChatRepository

class HiddenChatActivity : AppCompatActivity() {

    lateinit var chatRepository: ChatRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var stealthObserver: StealthLifecycleObserver
    private var currentRoomId: String? = null
    private var currentScrollIndex: Int = 0

    companion object {
        private const val EXTRA_ROOM_ID = "room_id"
        private const val EXTRA_SCROLL_INDEX = "scroll_index"
        private const val EXTRA_FAST_TRACK = "fast_track"

        fun createIntent(context: Context, roomId: String? = null, scrollIndex: Int = 0, fastTrack: Boolean = false): Intent {
            return Intent(context, HiddenChatActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_SCROLL_INDEX, scrollIndex)
                putExtra(EXTRA_FAST_TRACK, fastTrack)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden_chat)

        chatRepository = ChatRepository(this)
        sessionManager = SessionManager(this)

        currentRoomId = intent.getStringExtra(EXTRA_ROOM_ID)
        currentScrollIndex = intent.getIntExtra(EXTRA_SCROLL_INDEX, 0)
        val fastTrack = intent.getBooleanExtra(EXTRA_FAST_TRACK, false)

        // Initialize server connection
        val serverUrl = sessionManager.getServerUrl()
        chatRepository.init(serverUrl)

        // Setup panic lifecycle observer
        stealthObserver = StealthLifecycleObserver(
            activity = this,
            roomIdProvider = { currentRoomId },
            scrollIndexProvider = { currentScrollIndex }
        )
        lifecycle.addObserver(stealthObserver)

        // Determine initial fragment
        if (savedInstanceState == null) {
            val hasSession = sessionManager.isAuthenticated() || fastTrack
            if (hasSession && currentRoomId != null) {
                // Fast-track to chat
                chatRepository.init(serverUrl)
                showChatFragment(currentRoomId!!, currentScrollIndex)
            } else {
                // Show login wall
                showLoginFragment()
            }
        }
    }

    fun showLoginFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    fun showChatFragment(roomId: String, scrollIndex: Int = 0) {
        currentRoomId = roomId
        val fragment = ChatFragment.newInstance(roomId, scrollIndex)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun updateScrollIndex(index: Int) {
        currentScrollIndex = index
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(stealthObserver)
        chatRepository.disconnect()
    }
}
