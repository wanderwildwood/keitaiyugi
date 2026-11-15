package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * The games folder the player chose, and whether the phone can still read it. A folder deleted
 * or renamed, a card taken out, or the permission to read it gone all look the same from here:
 * the folder no longer answers. The library then keeps every game, favorite and save as it was,
 * and Home says to choose the folder again.
 */
object ChosenFolder {
    /** True when the folder at [treeUri] answers a question about itself. */
    fun isReadable(
        context: Context,
        treeUri: Uri,
    ): Boolean =
        runCatching {
            val root = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            context.contentResolver.query(
                root,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null,
            )?.use { it.moveToFirst() } == true
        }.getOrDefault(false)

    /**
     * A name for a folder that can no longer be asked its own: the last part of its document id
     * ("primary:Games/Game Boy" is "Game Boy"; the top of a card, "1234-5678:", is the card's id).
     */
    fun nameFromDocumentId(documentId: String): String {
        val path = documentId.substringAfter(':', documentId).trimEnd('/')
        if (path.isEmpty()) return documentId.substringBefore(':')
        return path.substringAfterLast('/')
    }

    /** [nameFromDocumentId] for a tree address, or null for one that is not a tree. */
    fun nameFromTree(treeUri: Uri): String? =
        runCatching { nameFromDocumentId(DocumentsContract.getTreeDocumentId(treeUri)) }.getOrNull()
}

/** Thrown by a scan whose chosen folder does not answer, so the library is left as it is. */
class FolderUnreadableException(message: String) : Exception(message)
