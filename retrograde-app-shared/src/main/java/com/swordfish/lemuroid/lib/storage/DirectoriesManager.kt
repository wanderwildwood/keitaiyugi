package com.swordfish.lemuroid.lib.storage

import android.content.Context
import java.io.File

class DirectoriesManager(private val appContext: Context) {
    @Deprecated("Use the external states directory")
    fun getInternalStatesDirectory(): File =
        File(appContext.filesDir, "states").apply {
            mkdirs()
        }

    fun getCoresDirectory(): File =
        File(appContext.filesDir, "cores").apply {
            mkdirs()
        }

    fun getSystemDirectory(): File =
        File(appContext.filesDir, "system").apply {
            mkdirs()
        }

    fun getStatesDirectory(): File =
        File(appContext.getExternalFilesDir(null), "states").apply {
            mkdirs()
        }

    fun getStatesPreviewDirectory(): File =
        File(appContext.getExternalFilesDir(null), "state-previews").apply {
            mkdirs()
        }

    fun getSavesDirectory(): File =
        File(appContext.getExternalFilesDir(null), "saves").apply {
            mkdirs()
        }

    /** The cheat files copied from the game box. */
    fun getCheatsDirectory(): File =
        File(appContext.getExternalFilesDir(null), "cheats").apply {
            mkdirs()
        }

    /** Battery saves that a sync with the game box replaced, a folder for each time. */
    fun getReplacedSavesDirectory(): File =
        File(appContext.getExternalFilesDir(null), "saves-replaced").apply {
            mkdirs()
        }

    /** The games copied from the game box. It sits inside the app's own games folder. */
    fun getBoxRomsDirectory(): File =
        File(getInternalRomsDirectory(), "box").apply {
            mkdirs()
        }

    fun getInternalRomsDirectory(): File =
        File(appContext.getExternalFilesDir(null), "roms").apply {
            mkdirs()
        }
}
