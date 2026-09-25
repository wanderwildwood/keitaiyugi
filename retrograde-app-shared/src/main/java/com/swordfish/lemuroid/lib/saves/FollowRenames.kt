package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import java.io.File

/**
 * Carries a game's saves across a rename of its ROM.
 *
 * Saves, states and their previews are filed under the ROM's file name, so renaming the ROM
 * stranded all of them: the game came back looking as if it had never been played, and the
 * files stayed on disk under the old name for good.
 *
 * A renamed ROM is recognised as the one that vanished in a rescan and the one that appeared in
 * the same rescan with the same title on the same system -- and the title is the one Lemuroid
 * looks up by the ROM's checksum, so a match there is a match on content. The saves follow only
 * into a game that has none of its own. Nothing is ever overwritten.
 */
object FollowRenames {

    /** Whether any save, state or preview is filed under this ROM's name. */
    fun hasAny(dirs: DirectoriesManager, fileName: String): Boolean =
        srm(dirs, fileName).exists() || named(dirs, fileName).isNotEmpty()

    /** Moves every save, state and preview from one ROM's name to another's. */
    fun move(dirs: DirectoriesManager, from: String, to: String): Int {
        var moved = 0
        val oldSrm = srm(dirs, from)
        if (oldSrm.exists() && oldSrm.renameTo(srm(dirs, to))) moved++
        for (file in named(dirs, from)) {
            val renamed = File(file.parentFile, to + file.name.removePrefix(from))
            if (!renamed.exists() && file.renameTo(renamed)) moved++
        }
        return moved
    }

    /** The battery save, named for the ROM without its extension. See `SavesManager`. */
    private fun srm(dirs: DirectoriesManager, fileName: String) =
        File(dirs.getSavesDirectory(), "${fileName.substringBeforeLast(".")}.srm")

    /** States, their metadata and their previews: "<rom>.state", "<rom>.slotN", ".jpg", per core. */
    private fun named(dirs: DirectoriesManager, fileName: String): List<File> =
        listOf(dirs.getStatesDirectory(), dirs.getStatesPreviewDirectory())
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.name.startsWith("$fileName.") }.toList() }

    /** The same game, as far as a rescan can tell. */
    fun sameGame(old: Game, new: Game): Boolean =
        old.systemId == new.systemId && old.title == new.title && old.fileName != new.fileName
}
