package com.swordfish.lemuroid.lib.box

import java.io.File
import java.io.RandomAccessFile

/**
 * What the last sync left behind, kept as small tab-separated files in one folder.
 *
 * `saves.tsv` is written by two processes (the game's, before a game opens, and the app's), so
 * every change to it is made under a file lock and from a fresh read.
 */
class BoxState(private val folder: File) {
    /** The save both sides held when they last matched, and the box's file as it stood then. */
    data class SaveRecord(
        val hash: String,
        val remoteSize: Long,
        val remoteModified: Long,
    )

    data class RomRecord(
        val fileName: String,
        val size: Long,
        val modified: Long,
    )

    fun save(key: BoxKey): SaveRecord? =
        rows(SAVES).firstOrNull { it.size == 4 && it[0] == key.toString() }?.let {
            SaveRecord(it[1], it[2].toLongOrNull() ?: -1, it[3].toLongOrNull() ?: -1)
        }

    fun putSave(
        key: BoxKey,
        record: SaveRecord,
    ) = locked {
        val others = rows(SAVES).filter { it.firstOrNull() != key.toString() }
        write(
            SAVES,
            others + listOf(listOf(key.toString(), record.hash, "${record.remoteSize}", "${record.remoteModified}")),
        )
    }

    fun roms(): Map<BoxKey, RomRecord> =
        rows(ROMS).filter { it.size == 4 }.mapNotNull { row ->
            val key = BoxKey.parse(row[0]) ?: return@mapNotNull null
            key to RomRecord(row[1], row[2].toLongOrNull() ?: -1, row[3].toLongOrNull() ?: -1)
        }.toMap()

    fun putRoms(roms: Map<BoxKey, RomRecord>) =
        locked {
            write(ROMS, roms.map { (k, r) -> listOf(k.toString(), r.fileName, "${r.size}", "${r.modified}") })
        }

    /** Games that were on the phone already and are the same game as one on the box, by address. */
    fun twins(): Map<String, BoxKey> =
        rows(TWINS).filter { it.size == 2 }.mapNotNull { row ->
            BoxKey.parse(row[1])?.let { row[0] to it }
        }.toMap()

    fun putTwins(twins: Map<String, BoxKey>) =
        locked {
            write(TWINS, twins.map { (uri, key) -> listOf(uri, key.toString()) })
        }

    private fun rows(name: String): List<List<String>> {
        val file = File(folder, name)
        if (!file.isFile) return emptyList()
        return runCatching { file.readLines().filter { it.isNotEmpty() }.map { it.split('\t') } }
            .getOrDefault(emptyList())
    }

    private fun write(
        name: String,
        rows: List<List<String>>,
    ) {
        folder.mkdirs()
        // A name with a tab or a line break in it would break the file; no ROM has one.
        val clean = rows.filter { row -> row.none { it.contains('\t') || it.contains('\n') } }
        val temp = File(folder, "$name.tmp")
        temp.writeText(clean.joinToString("") { it.joinToString("\t") + "\n" })
        if (!temp.renameTo(File(folder, name))) {
            File(folder, name).writeText(temp.readText())
            temp.delete()
        }
    }

    // The file lock keeps other processes out; two threads of this one would instead collide on
    // it (OverlappingFileLockException), so they take turns here first.
    private fun <T> locked(block: () -> T): T =
        synchronized(IN_PROCESS) {
            folder.mkdirs()
            RandomAccessFile(File(folder, "lock"), "rw").use { file ->
                file.channel.lock().use { block() }
            }
        }

    private companion object {
        val IN_PROCESS = Any()
        const val SAVES = "saves.tsv"
        const val ROMS = "roms.tsv"
        const val TWINS = "twins.tsv"
    }
}
