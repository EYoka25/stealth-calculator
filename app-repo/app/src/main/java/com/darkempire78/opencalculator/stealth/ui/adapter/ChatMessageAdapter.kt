package com.darkempire78.opencalculator.stealth.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.stealth.model.ChatMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    private class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem.copy(payloadText = newItem.payloadText)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isOutgoing) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_sent, parent, false)
            SentMessageHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_received, parent, false)
            ReceivedMessageHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageHolder -> holder.bind(message)
            is ReceivedMessageHolder -> holder.bind(message)
        }
    }

    inner class SentMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.messageTimestamp)
        private val messageStatus: ImageView = itemView.findViewById(R.id.messageStatus)
        private val messageMedia: ImageView = itemView.findViewById(R.id.messageMedia)

        fun bind(message: ChatMessage) {
            messageText.text = message.payloadText
            messageTimestamp.text = TIME_FORMAT.format(Date(message.timestamp))

            // Status ticks
            when (message.statusTick) {
                0 -> messageStatus.setImageResource(R.drawable.ic_tick_single)
                1 -> messageStatus.setImageResource(R.drawable.ic_tick_double_grey)
                2 -> messageStatus.setImageResource(R.drawable.ic_tick_double_blue)
                else -> messageStatus.setImageResource(R.drawable.ic_tick_single)
            }

            // Media handling
            if (message.mediaLocalPath != null || message.mediaUrl != null) {
                messageMedia.visibility = View.VISIBLE
                val mediaFile = message.mediaLocalPath?.let { File(it) }
                if (mediaFile != null && mediaFile.exists()) {
                    messageMedia.setImageURI(android.net.Uri.fromFile(mediaFile))
                } else {
                    messageMedia.setImageResource(R.drawable.ic_sticker_placeholder)
                }
            } else {
                messageMedia.visibility = View.GONE
            }
        }
    }

    inner class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTimestamp: TextView = itemView.findViewById(R.id.messageTimestamp)
        private val senderName: TextView = itemView.findViewById(R.id.senderName)
        private val messageMedia: ImageView = itemView.findViewById(R.id.messageMedia)

        fun bind(message: ChatMessage) {
            messageText.text = message.payloadText
            messageTimestamp.text = TIME_FORMAT.format(Date(message.timestamp))

            // Show sender name for received messages
            senderName.text = message.senderAlias
            senderName.visibility = View.VISIBLE

            // Media handling
            if (message.mediaLocalPath != null || message.mediaUrl != null) {
                messageMedia.visibility = View.VISIBLE
                val mediaFile = message.mediaLocalPath?.let { File(it) }
                if (mediaFile != null && mediaFile.exists()) {
                    messageMedia.setImageURI(android.net.Uri.fromFile(mediaFile))
                } else {
                    messageMedia.setImageResource(R.drawable.ic_sticker_placeholder)
                }
            } else {
                messageMedia.visibility = View.GONE
            }
        }
    }
}
