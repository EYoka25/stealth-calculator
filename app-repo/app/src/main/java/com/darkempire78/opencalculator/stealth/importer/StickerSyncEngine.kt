package com.darkempire78.opencalculator.stealth.importer

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import java.io.File

class StickerSyncEngine(private val context: Context) {

    companion object {
        private const val TAG = "StickerSyncEngine"
        private const val STICKER_PATH = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers/"
        private const val STICKER_MIME_TYPE = "image/webp"
    }

    data class StickerInfo(
        val uri: Uri,
        val displayName: String,
        val size: Long
    )

    fun launchFolderPicker(launcher: ActivityResultLauncher<Uri?>) {
        val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:$STICKER_PATH")
        launcher.launch(initialUri)
    }

    fun parseStickersFromTree(treeUri: Uri, contentResolver: ContentResolver): List<StickerInfo> {
        val stickers = mutableListOf<StickerInfo>()

        try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val displayName = cursor.getString(1)
                    val mimeType = cursor.getString(2)
                    val size = cursor.getLong(3)

                    if (mimeType == STICKER_MIME_TYPE || displayName.endsWith(".webp", ignoreCase = true)) {
                        val stickerUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        stickers.add(StickerInfo(stickerUri, displayName, size))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stickers", e)
        }

        return stickers.sortedBy { it.displayName }
    }

    fun copyStickerToCache(stickerUri: Uri, contentResolver: ContentResolver): File? {
        return try {
            val cacheDir = File(context.cacheDir, "stickers").apply { mkdirs() }
            val outFile = File(cacheDir, "sticker_${System.currentTimeMillis()}.webp")

            contentResolver.openInputStream(stickerUri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying sticker", e)
            null
        }
    }

    fun getAllCachedStickers(): List<File> {
        val cacheDir = File(context.cacheDir, "stickers")
        if (!cacheDir.exists()) return emptyList()
        return cacheDir.listFiles { file ->
            file.extension.equals("webp", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    fun clearStickerCache() {
        File(context.cacheDir, "stickers").deleteRecursively()
    }
}
