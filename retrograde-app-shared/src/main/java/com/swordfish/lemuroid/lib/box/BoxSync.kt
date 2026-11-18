package com.swordfish.lemuroid.lib.box

import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveKeys
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps this phone's Game Boy games, battery saves and cheats in step with a Batocera box.
 *
 * Games and cheats only ever come from the box. Saves go both ways, one game at a time; see
 * [SaveMerge]. Whatever a sync replaces is kept: on the phone under [replacedDir], on the box
 * under its `saves-sync-backup` folder. Nothing is ever deleted on either side.
 *
 * Only battery saves (`.srm`) are shared. Save states are a different format on each side and
 * stay where they were made.
 */
class BoxSync(
    stateDir: File,
    /** Where the box's games are kept on the phone: `<romsDir>/gb`, `<romsDir>/gbc`. */
    private val romsDir: File,
    private val savesDir: File,
    /** The app's save states, a folder for each core. Only a game's auto state is ever touched. */
    private val statesDir: File,
    private val cheatsDir: File,
    private val replacedDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val state = BoxState(stateDir)

    data class GamesResult(
        val copied: Int,
        val onBox: Int,
    )

    data class SavesResult(
        val pulled: Int,
        val pushed: Int,
    )

    /** The box's name for [game], or null for a game the box does not have. */
    fun keyFor(game: Game): BoxKey? = boxFileKey(game) ?: state.twins()[game.fileUri]

    /**
     * Copies every game the box has and the phone lacks, and any the box has since replaced.
     * A game the phone already had under the same name is not copied a second time: it is
     * paired with the box's and shares its saves and cheats from then on.
     */
    fun syncGames(
        share: BoxShare,
        games: List<Game>,
        progress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): GamesResult {
        val onBox =
            BoxNames.SYSTEMS.flatMap { system ->
                share.list(BoxNames.romsFolder(system))
                    .filter { !it.isFolder && BoxNames.isRom(it.name) }
                    .map { system to it }
            }
        val own = games.filter { boxFileKey(it) == null }
        val records = HashMap(state.roms())
        val twins = HashMap<String, BoxKey>()
        var copied = 0

        onBox.forEachIndexed { index, (system, entry) ->
            progress(index, onBox.size)
            val key = BoxKey(system, BoxNames.stem(entry.name))
            val target = File(File(romsDir, system), entry.name)
            // A game of the box's already copied here owns its saves; an own ROM of the same name
            // found later is left unpaired, so the two never push over each other's save.
            val twin =
                own.firstOrNull { it.systemId == system && sameGame(it, key.stem) }
                    ?.takeIf { !target.isFile }
            if (twin != null) {
                twins[twin.fileUri] = key
                return@forEachIndexed
            }
            val record = records[key]
            val upToDate =
                target.isFile && target.length() == entry.size &&
                    (record == null || record.modified == entry.modified)
            if (!upToDate) {
                // A file larger than any handheld game is passed over, and the sync goes on.
                if (entry.size > SmbBoxShare.MAX_DOWNLOAD) return@forEachIndexed
                target.parentFile?.mkdirs()
                val part = File(target.parentFile, ".${entry.name}.part")
                try {
                    share.download("${BoxNames.romsFolder(system)}/${entry.name}", part)
                    if (!part.renameTo(target)) throw BoxUnreachableException("could not keep ${entry.name}")
                } catch (e: BoxFileTooLargeException) {
                    return@forEachIndexed
                } finally {
                    part.delete()
                }
                copied++
            }
            records[key] = BoxState.RomRecord(entry.name, entry.size, entry.modified)
            // Written as it goes, so a sync cut short does not copy the same games again.
            if (!upToDate && copied % 20 == 0) state.putRoms(records)
        }
        progress(onBox.size, onBox.size)
        state.putRoms(records)
        state.putTwins(twins)
        return GamesResult(copied, onBox.size)
    }

    /** Every cheat file the box has for a game it has. Returns how many were copied. */
    fun syncCheats(share: BoxShare): Int {
        val stems = state.roms().keys.mapTo(HashSet()) { it.stem } + state.twins().values.map { it.stem }
        var copied = 0
        for (entry in share.list(BoxNames.CHEATS_FOLDER)) {
            if (entry.isFolder || BoxNames.cheatFileStem(entry.name) !in stems) continue
            val target = File(cheatsDir, entry.name)
            if (target.isFile && target.length() == entry.size) continue
            // A file larger than any cheat file is passed over, and the sync goes on.
            if (entry.size > SmbBoxShare.MAX_READ) continue
            val data =
                try {
                    share.read("${BoxNames.CHEATS_FOLDER}/${entry.name}")
                } catch (e: BoxFileTooLargeException) {
                    continue
                }
            cheatsDir.mkdirs()
            writeAtomic(target, data)
            copied++
        }
        return copied
    }

    fun syncSaves(
        share: BoxShare,
        games: List<Game>,
    ): SavesResult {
        val listings = HashMap<String, List<BoxEntry>>()
        var pulled = 0
        var pushed = 0
        for (game in games) {
            val key = keyFor(game) ?: continue
            val listing = listings.getOrPut(key.system) { share.list(BoxNames.savesFolder(key.system)) }
            val action =
                try {
                    syncSave(share, game, listing)
                } catch (e: BoxFileTooLargeException) {
                    // Not a save; the other games' saves still go.
                    SaveAction.NOTHING
                }
            when (action) {
                SaveAction.PULL -> pulled++
                SaveAction.PUSH -> pushed++
                SaveAction.NOTHING -> Unit
            }
        }
        return SavesResult(pulled, pushed)
    }

    /** Brings one game's battery save into step. [listing] is the box's save folder, if already read. */
    fun syncSave(
        share: BoxShare,
        game: Game,
        listing: List<BoxEntry>? = null,
    ): SaveAction {
        val key = keyFor(game) ?: return SaveAction.NOTHING
        val folder = BoxNames.savesFolder(key.system)
        val entries = listing ?: share.list(folder)
        val remoteName = BoxNames.saveName(key.stem)
        val remotePath = "$folder/$remoteName"
        val remoteEntry = entries.firstOrNull { !it.isFolder && it.name == remoteName && it.size > 0 }
        val localFile = File(savesDir, SaveKeys.srmName(game.saveName()))
        val record = state.save(key)

        val localBytes = if (localFile.isFile && localFile.length() > 0) localFile.readBytes() else null
        val local = localBytes?.let { SaveMerge.Side(SaveMerge.hash(it), localFile.lastModified()) }

        var remoteBytes: ByteArray? = null
        val remote =
            when {
                remoteEntry == null -> null
                record != null && record.remoteSize == remoteEntry.size &&
                    record.remoteModified == remoteEntry.modified ->
                    SaveMerge.Side(record.hash, remoteEntry.modified)
                else -> {
                    remoteBytes = share.read(remotePath)
                    SaveMerge.Side(SaveMerge.hash(remoteBytes), remoteEntry.modified)
                }
            }

        val action = SaveMerge.decide(local, remote, record?.hash)
        when (action) {
            SaveAction.PULL -> {
                val bytes = remoteBytes ?: share.read(remotePath)
                val kept = File(replacedDir, stamp())
                if (localBytes != null) {
                    kept.mkdirs()
                    File(kept, localFile.name).writeBytes(localBytes)
                }
                // The app resumes a game from its own auto state, which carries the old save
                // inside it: left in place, the game would open on that, never show the save
                // just received, and send the old one back on closing. It is set aside with
                // the save it belonged to, as the box's is when a save goes the other way.
                setAutoStateAside(game, kept)
                savesDir.mkdirs()
                writeAtomic(localFile, bytes)
                state.putSave(key, BoxState.SaveRecord(SaveMerge.hash(bytes), remoteEntry!!.size, remoteEntry.modified))
            }
            SaveAction.PUSH -> {
                val backup = "${BoxNames.BACKUP_FOLDER}/${stamp()}-phone/${key.system}"
                if (remoteEntry != null) share.move(remotePath, "$backup/$remoteName")
                // The box resumes a game from its own auto state and never looks at the battery
                // save while one is there, so the save just sent would not show. The auto state
                // is set aside with the save it belonged to.
                for (name in BoxNames.autoStateNames(key.stem)) {
                    if (entries.any { !it.isFolder && it.name == name }) share.move("$folder/$name", "$backup/$name")
                }
                share.write(remotePath, localBytes!!)
                val written = share.list(folder).firstOrNull { it.name == remoteName }
                state.putSave(
                    key,
                    BoxState.SaveRecord(local!!.hash, written?.size ?: -1, written?.modified ?: -1),
                )
            }
            SaveAction.NOTHING ->
                if (local != null && remoteEntry != null && record?.remoteModified != remoteEntry.modified) {
                    state.putSave(key, BoxState.SaveRecord(local.hash, remoteEntry.size, remoteEntry.modified))
                }
        }
        return action
    }

    private fun setAutoStateAside(
        game: Game,
        to: File,
    ) {
        val name = SaveKeys.stateName(game.saveName())
        for (core in statesDir.listFiles { file -> file.isDirectory }.orEmpty()) {
            for (file in listOf(File(core, name), File(core, "$name.metadata"))) {
                if (!file.isFile) continue
                to.mkdirs()
                val kept = File(to, file.name)
                if (!file.renameTo(kept)) {
                    file.copyTo(kept, overwrite = true)
                    file.delete()
                }
            }
        }
    }

    /** A game whose file is one this app copied from the box. */
    private fun boxFileKey(game: Game): BoxKey? {
        val path = runCatching { URI(game.fileUri).takeIf { it.scheme == "file" }?.path }.getOrNull() ?: return null
        val root = romsDir.path.trimEnd('/') + "/"
        if (!path.startsWith(root)) return null
        val parts = path.removePrefix(root).split('/')
        if (parts.size != 2 || parts[0] !in BoxNames.SYSTEMS) return null
        return BoxKey(parts[0], BoxNames.stem(parts[1]))
    }

    /** True when [game], a game the phone had already, goes by the box game's name. */
    private fun sameGame(
        game: Game,
        stem: String,
    ): Boolean {
        val storedAs =
            runCatching {
                URLDecoder.decode(game.fileUri.substringAfterLast('/'), "UTF-8").substringAfterLast('/')
            }.getOrNull()
        return BoxNames.stem(game.fileName).equals(stem, ignoreCase = true) ||
            (storedAs != null && BoxNames.stem(storedAs).equals(stem, ignoreCase = true))
    }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now()))

    private fun writeAtomic(
        file: File,
        data: ByteArray,
    ) {
        // rename replaces the old file in one step on the phone's own storage; when it cannot,
        // the old file stays as it was and the sync says the box could not be finished.
        val temp = File(file.parentFile, ".${file.name}.tmp")
        // On the storage before the rename, so the name never points at data not yet written.
        FileOutputStream(temp).use { out ->
            out.write(data)
            out.fd.sync()
        }
        if (!temp.renameTo(file)) {
            temp.delete()
            throw BoxUnreachableException("could not keep ${file.name}")
        }
    }
}
