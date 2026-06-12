package com.darkempire78.opencalculator.stealth.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.databinding.FragmentLoginBinding
import com.darkempire78.opencalculator.stealth.SessionManager
import com.darkempire78.opencalculator.stealth.StealthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRepository: ChatRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var stealthPrefs: StealthPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as HiddenChatActivity
        chatRepository = activity.chatRepository
        sessionManager = SessionManager(requireContext())
        stealthPrefs = StealthPreferences(requireContext())

        // Pre-fill alias if exists
        binding.aliasEditText.setText(sessionManager.getSenderAlias())

        binding.connectButton.setOnClickListener {
            attemptConnection()
        }

        binding.serverSettingsButton.setOnClickListener {
            showServerSettingsDialog()
        }
    }

    private fun attemptConnection() {
        val roomId = binding.roomIdEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val alias = binding.aliasEditText.text.toString().trim()

        if (roomId.isEmpty()) {
            binding.roomIdInputLayout.error = "Room ID is required"
            return
        }
        binding.roomIdInputLayout.error = null

        if (alias.isEmpty()) {
            binding.aliasInputLayout.error = "Alias is required"
            return
        }
        binding.aliasInputLayout.error = null

        val effectivePassword = password.ifEmpty { null }

        binding.loginProgressBar.visibility = View.VISIBLE
        binding.connectButton.isEnabled = false

        lifecycleScope.launch {
            val result = chatRepository.authenticate(roomId, effectivePassword, alias)

            withContext(Dispatchers.Main) {
                binding.loginProgressBar.visibility = View.GONE
                binding.connectButton.isEnabled = true

                result.fold(
                    onSuccess = { token ->
                        stealthPrefs.setSenderAlias(alias)
                        (activity as? HiddenChatActivity)?.showChatFragment(roomId)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            "Connection failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun showServerSettingsDialog() {
        val editText = EditText(requireContext()).apply {
            setText(stealthPrefs.getServerUrl())
            hint = "http://10.0.2.2:8080"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Server URL")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    stealthPrefs.setServerUrl(url)
                    chatRepository.init(url)
                    Toast.makeText(requireContext(), "Server URL saved", Toast.LENGTH_SHORT).show()
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
