package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveKeys
import java.io.File
import java.io.IOException

object GameCacheUtils {
    fun getDataFileForGame(
        folderName: String,
        context: Context,
        game: Game,
        dataFile: DataFile,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return inside(gamesCacheDir, dataFile.fileName)
    }

    fun getCacheFileForGame(
        folderName: String,
        context: Context,
        game: Game,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return inside(gamesCacheDir, game.fileName)
    }

    /** [name] joined to [dir], refused when it would land anywhere but directly inside it. */
    private fun inside(
        dir: File,
        name: String,
    ): File {
        val file = File(dir, name)
        if (file.canonicalFile.parentFile != dir.canonicalFile) {
            throw IOException("$name is not a plain file name")
        }
        return file
    }

    private fun getCacheDirForGame(
        folderName: String,
        game: Game,
        context: Context,
    ): File {
        // One folder per game, named for its address: two ROMs with the same file name in different
        // folders used to share one cached copy, and the second game started the first one's ROM.
        val gamesCachePath = buildPath(folderName, game.systemId, SaveKeys.hash(game.fileUri))
        val gamesCacheDir = File(context.cacheDir, gamesCachePath)
        gamesCacheDir.mkdirs()
        return gamesCacheDir
    }

    private fun buildPath(vararg chunks: String): String {
        return chunks.joinToString(separator = File.separator)
    }
}
