package com.swordfish.lemuroid.lib.saves

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The games' saves as one zip, written and read through the system's file picker: the battery
 * saves (.srm, what a game itself saves), the save states with their notes, and the pictures of
 * the state slots. Not the games, which are the player's own files, and not the settings.
 *
 * The zip opens with a list of everything in it. [stage] reads the whole zip into a folder of
 * its own and checks it against that list (and every entry against its CRC, which the zip reader
 * does at the end of each entry); only then does [apply] move the files into place. A file in
 * the backup replaces the same file here; saves of games the backup does not have are left alone.
 */
object SavesBackup {
    const val FORMAT = "keitaiyugi-saves"
    const val VERSION = 1
    const val MANIFEST = "keitaiyugi-saves.txt"

    /** The three folders, by the name they have inside the zip. */
    data class Folders(val saves: File, val states: File, val previews: File) {
        fun byName() = linkedMapOf("saves" to saves, "states" to states, "state-previews" to previews)
    }

    class BackupException(val kind: Kind) : Exception(kind.name) {
        enum class Kind { NOT_A_BACKUP, NEWER, DAMAGED, TOO_BIG }
    }

    /** Writes every save in [folders] to [out]; returns how many files went in. */
    fun write(
        out: OutputStream,
        folders: Folders,
        written: Instant = Instant.now(),
    ): Int {
        val files =
            folders.byName().flatMap { (name, dir) ->
                dir.walkTopDown()
                    // A name with a tab or a line break could not be listed; no game names a save so.
                    .filter {
                        it.isFile && it.length() > 0 && !isScratch(it.name) && '\n' !in it.name && '\t' !in it.name
                    }
                    .map { "$name/${it.relativeTo(dir).invariantSeparatorsPath}" to it }
                    .sortedBy { it.first }
                    .toList()
            }
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            val manifest =
                buildString {
                    append("format=").append(FORMAT).append('\n')
                    append("version=").append(VERSION).append('\n')
                    append("written=").append(written).append('\n')
                    for ((path, file) in files) {
                        append("file=").append(file.length()).append('\t').append(path).append('\n')
                    }
                }
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((path, file) in files) {
                zip.putNextEntry(ZipEntry(path).apply { time = file.lastModified() })
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return files.size
    }

    /**
     * Reads the zip in [input] whole into [staging] (emptied first) and checks it. Throws a
     * [BackupException] for anything it cannot use, with [staging] emptied again; nothing outside
     * [staging] is touched. Returns the paths staged, as they are named in the zip.
     */
    fun stage(
        input: InputStream,
        staging: File,
        maxBytes: Long = 1L shl 30,
    ): List<String> {
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            return stageInto(input, staging, maxBytes)
        } catch (e: BackupException) {
            staging.deleteRecursively()
            throw e
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw BackupException(BackupException.Kind.DAMAGED)
        }
    }

    private fun stageInto(
        input: InputStream,
        staging: File,
        maxBytes: Long,
    ): List<String> {
        val zip = ZipInputStream(input)
        val first =
            try {
                zip.nextEntry
            } catch (e: Exception) {
                null
            }
        if (first == null || first.name != MANIFEST) throw BackupException(BackupException.Kind.NOT_A_BACKUP)
        val lines = zip.readBytes().toString(Charsets.UTF_8).lines().filter { it.isNotEmpty() }
        val fields =
            lines.filter { !it.startsWith("file=") }.associate { it.substringBefore('=') to it.substringAfter('=') }
        if (fields["format"] != FORMAT) throw BackupException(BackupException.Kind.NOT_A_BACKUP)
        val version = fields["version"]?.toIntOrNull() ?: throw BackupException(BackupException.Kind.DAMAGED)
        if (version > VERSION) throw BackupException(BackupException.Kind.NEWER)
        if (version < 1) throw BackupException(BackupException.Kind.DAMAGED)
        val expected =
            lines.filter { it.startsWith("file=") }.associate { line ->
                val rest = line.removePrefix("file=")
                val size =
                    rest.substringBefore('\t').toLongOrNull() ?: throw BackupException(BackupException.Kind.DAMAGED)
                val path = rest.substringAfter('\t')
                if (!safe(path)) throw BackupException(BackupException.Kind.DAMAGED)
                path to size
            }
        if (expected.values.sum() > maxBytes) throw BackupException(BackupException.Kind.TOO_BIG)

        val seen = LinkedHashSet<String>()
        while (true) {
            val entry = zip.nextEntry ?: break
            val size = expected[entry.name]
            if (size == null || entry.isDirectory || !seen.add(entry.name)) {
                throw BackupException(BackupException.Kind.DAMAGED)
            }
            val target = File(staging, entry.name)
            target.parentFile?.mkdirs()
            var written = 0L
            target.outputStream().use { out ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = zip.read(buf)
                    if (n < 0) break
                    written += n
                    if (written > size) throw BackupException(BackupException.Kind.DAMAGED)
                    out.write(buf, 0, n)
                }
            }
            if (written != size) throw BackupException(BackupException.Kind.DAMAGED)
            if (entry.time > 0) target.setLastModified(entry.time)
        }
        if (seen != expected.keys) throw BackupException(BackupException.Kind.DAMAGED)
        return seen.toList()
    }

    /**
     * Moves what [stage] left in [staging] into [folders], each file over the one of the same name,
     * and empties [staging]. Returns how many files were put in place.
     */
    fun apply(
        staging: File,
        folders: Folders,
    ): Int {
        var count = 0
        try {
            for ((name, dir) in folders.byName()) {
                val from = File(staging, name)
                if (!from.isDirectory) continue
                from.walkTopDown().filter { it.isFile }.forEach { file ->
                    val target = File(dir, file.relativeTo(from).path)
                    target.parentFile?.mkdirs()
                    // A half-finished write of the old save, left beside it, would be taken
                    // back in place of the restored one the next time the save is read.
                    File(target.path + ".bak").delete()
                    File(target.path + ".new").delete()
                    if (!file.renameTo(target)) {
                        file.copyTo(target, overwrite = true)
                        file.delete()
                    }
                    count++
                }
            }
        } finally {
            staging.deleteRecursively()
        }
        return count
    }

    /** Leftovers of an interrupted write, which are not saves. */
    private fun isScratch(name: String) = name.endsWith(".bak") || name.endsWith(".new") || name.endsWith(".tmp")

    /** A path inside one of the three folders, and nowhere else. */
    internal fun safe(path: String): Boolean {
        if (path.isEmpty() || path.startsWith("/") || '\\' in path || '\u0000' in path) return false
        val parts = path.split('/')
        if (parts.size < 2 || parts[0] !in setOf("saves", "states", "state-previews")) return false
        return parts.none { it.isEmpty() || it == "." || it == ".." } && !isScratch(parts.last())
    }
}
