package com.darkempire78.opencalculator.stealth.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.databinding.FragmentChatBinding
import com.darkempire78.opencalculator.stealth.SessionManager
import com.darkempire78.opencalculator.stealth.importer.StickerSyncEngine
import com.darkempire78.opencalculator.stealth.importer.WhatsAppTxtImporter
import com.darkempire78.opencalculator.stealth.model.ChatMessage
import com.darkempire78.opencalculator.stealth.model.LocalMessageEntity
import com.darkempire78.opencalculator.stealth.network.ChatRepository
import com.darkempire78.opencalculator.stealth.ui.adapter.ChatMessageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRepository: ChatRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var stickerSyncEngine: StickerSyncEngine
    private lateinit var txtImporter: WhatsAppTxtImporter

    private var roomId: String = ""
    private var savedScrollIndex: Int = 0

    private val stickerFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { uri ->
            val stickers = stickerSyncEngine.parseStickersFromTree(uri, requireActivity().contentResolver)
            if (stickers.isNotEmpty()) {
                Toast.makeText(requireContext(), "Found ${stickers.size} stickers", Toast.LENGTH_SHORT).show()
                showStickerTray(stickers)
            } else {
                Toast.makeText(requireContext(), "No stickers found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importWhatsAppChat(it) }
    }

    companion object {
        private const val ARG_ROOM_ID = "room_id"
        private const val ARG_SCROLL_INDEX = "scroll_index"

        fun newInstance(roomId: String, scrollIndex: Int): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ROOM_ID, roomId)
                    putInt(ARG_SCROLL_INDEX, scrollIndex)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = arguments?.getString(ARG_ROOM_ID) ?: ""
        savedScrollIndex = arguments?.getInt(ARG_SCROLL_INDEX, 0) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as HiddenChatActivity
        chatRepository = activity.chatRepository
        sessionManager = SessionManager(requireContext())
        stickerSyncEngine = StickerSyncEngine(requireContext())
        txtImporter = WhatsAppTxtImporter(chatRepository.db.messageDao())

        setupRecyclerView()
        setupToolbar()
        setupInputArea()
        observeMessages()
        loadMessages()

        // Connect WebSocket
        chatRepository.connectWebSocket()
    }

    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter()
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupToolbar() {
        binding.toolbarRoomId.text = "Room: $roomId"
        binding.toolbarMenuButton.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.chat_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_import_chat -> {
                    filePickerLauncher.launch("text/plain")
                    true
                }
                R.id.action_sync_stickers -> {
                    stickerSyncEngine.launchFolderPicker(stickerFolderLauncher)
                    true
                }
                R.id.action_disconnect -> {
                    disconnectAndExit()
                    true
                }
                R.id.action_change_alias -> {
                    showChangeAliasDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupInputArea() {
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.messageInput.text?.clear()
            }
        }

        binding.attachButton.setOnClickListener {
            // Show sticker tray or attachment options
            toggleStickerTray()
        }

        binding.emojiButton.setOnClickListener {
            // Toggle emoji keyboard
        }
    }

    private fun sendMessage(text: String, mediaUrl: String? = null) {
        lifecycleScope.launch {
            chatRepository.sendMessage(text, mediaUrl)
        }
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            chatRepository.newMessageFlow.collectLatest { message ->
                val currentList = messageAdapter.currentList.toMutableList()
                currentList.add(message)
                messageAdapter.submitList(currentList) {
                    binding.chatRecyclerView.scrollToPosition(currentList.size - 1)
                }
                // Save scroll position
                (activity as? HiddenChatActivity)?.updateScrollIndex(currentList.size - 1)
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            val messages = chatRepository.loadMessagesForRoom(roomId)
            messageAdapter.submitList(messages) {
                if (savedScrollIndex > 0 && savedScrollIndex < messages.size) {
                    binding.chatRecyclerView.scrollToPosition(savedScrollIndex)
                } else if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun importWhatsAppChat(uri: Uri) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Importing Chat")
            .setMessage("Processing...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            txtImporter.importFile(uri, roomId, requireActivity().contentResolver) { progress ->
                lifecycleScope.launch(Dispatchers.Main) {
                    if (progress.isComplete) {
                        progressDialog.dismiss()
                        if (progress.error != null) {
                            Toast.makeText(requireContext(), "Error: ${progress.error}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Imported ${progress.importedMessages} messages",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadMessages()
                        }
                    }
                }
            }
        }
    }

    private fun showStickerTray(stickers: List<StickerSyncEngine.StickerInfo>) {
        if (binding.stickerTrayRecyclerView.visibility == View.VISIBLE) {
            binding.stickerTrayRecyclerView.visibility = View.GONE
            return
        }

        binding.stickerTrayRecyclerView.visibility = View.VISIBLE
        // TODO: Implement sticker tray adapter
    }

    private fun toggleStickerTray() {
        binding.stickerTrayRecyclerView.visibility =
            if (binding.stickerTrayRecyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun disconnectAndExit() {
        sessionManager.clearSession()
        chatRepository.disconnect()
        (activity as? HiddenChatActivity)?.finish()
    }

    private fun showChangeAliasDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            setText(sessionManager.getSenderAlias())
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Change Alias")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newAlias = editText.text.toString().trim()
                if (newAlias.isNotEmpty()) {
                    // Note: In a real implementation, you'd want to update the alias in the backend too
                    Toast.makeText(requireContext(), "Alias updated (local only)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
