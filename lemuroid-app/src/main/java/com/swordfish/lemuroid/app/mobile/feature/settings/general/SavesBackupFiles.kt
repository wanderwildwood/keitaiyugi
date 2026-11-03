package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.swordfish.lemuroid.lib.saves.SavesBackup
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Saves backup and restore against a file the player picked: the Android half of [SavesBackup]. */
object SavesBackupFiles {
    sealed interface Result {
        data class Done(val files: Int) : Result

        data class Refused(val kind: SavesBackup.BackupException.Kind) : Result

        data object Unreadable : Result

        /** The file was good, and putting it in place stopped part of the way through. */
        data object StoppedPartway : Result
    }

    private fun folders(context: Context): SavesBackup.Folders {
        val directories = DirectoriesManager(context.applicationContext)
        return SavesBackup.Folders(
            saves = directories.getSavesDirectory(),
            states = directories.getStatesDirectory(),
            previews = directories.getStatesPreviewDirectory(),
        )
    }

    /** Beside the saves, on the same storage, so putting a file in place is a rename. */
    private fun staging(context: Context) = File(context.getExternalFilesDir(null), "restore-staging")

    /** Writes the backup to [uri]; false, with the file removed if it was left empty, when that fails. */
    suspend fun save(
        context: Context,
        uri: Uri,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val ok =
                runCatching {
                    val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("no stream")
                    out.use { SavesBackup.write(it, folders(context)) }
                }.isSuccess
            if (!ok) runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
            ok
        }

    /** Reads and checks the whole file first; only a file that passes puts anything in place. */
    suspend fun restore(
        context: Context,
        uri: Uri,
    ): Result =
        withContext(Dispatchers.IO) {
            val staging = staging(context)
            val input =
                runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
                    ?: return@withContext Result.Unreadable
            try {
                input.use { SavesBackup.stage(it, staging) }
            } catch (e: SavesBackup.BackupException) {
                return@withContext Result.Refused(e.kind)
            } catch (e: Exception) {
                staging.deleteRecursively()
                return@withContext Result.Unreadable
            }
            runCatching { Result.Done(SavesBackup.apply(staging, folders(context))) }
                .getOrElse { Result.StoppedPartway }
        }
}
