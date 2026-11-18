package com.swordfish.lemuroid.lib.box

import java.io.Closeable
import java.io.File
import java.io.IOException

/** One file or folder on the game box's share. */
data class BoxEntry(
    val name: String,
    val isFolder: Boolean,
    val size: Long,
    val modified: Long,
)

/** The box did not answer, or stopped answering partway. */
open class BoxUnreachableException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** What answers at the box's address is not the box the first sync found there (see [BoxIdentity]). */
class BoxNotTheBoxException(message: String) : BoxUnreachableException(message)

/** One file on the box is larger than any game, save or cheat file; the box itself is fine. */
class BoxFileTooLargeException(message: String) : IOException(message)

/**
 * The game box's shared drive: the folder a Batocera box shares on the home network, with
 * `roms`, `saves` and `cheats` inside it. Paths are relative to that folder and use `/`.
 */
interface BoxShare : Closeable {
    /** What is in [folder]. Nothing when the folder is not there. */
    fun list(folder: String): List<BoxEntry>

    fun read(path: String): ByteArray

    fun download(
        path: String,
        to: File,
    )

    /** Writes [data] over whatever is at [path], making the folders above it if they are missing. */
    fun write(
        path: String,
        data: ByteArray,
    )

    /** Moves a file, making the folders above the new place if they are missing. */
    fun move(
        from: String,
        to: String,
    )
}
