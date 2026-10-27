package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File

/**
 * Splits saves that two games already share.
 *
 * Before save keys, every game's saves were filed under its file name, so two ROMs called the
 * same in different folders (or "Foo.gb" and "Foo.gbc", which shared one .srm) had one set of
 * saves between them. The upgrade gave each of them its file name as its key, so they still
 * share. Left like that, the second game would now run its own ROM against the first one's
 * battery save and write over it.
 *
 * So for each group of games that share a battery-save name, the first keeps the key it has and
 * every other gets a key of its own, and the saves, states and previews filed under the old key
 * are COPIED to the new one. Nothing is moved or overwritten: the first game's files are left
 * exactly as they were, and a file that already exists under a new key is never replaced. Which
 * of the games a shared save really belonged to cannot be known, so each keeps a copy and
 * nothing is lost.
 *
 * Once the keys are unique there is nothing left to split, so running it again changes nothing.
 */
object SaveKeyMigration {
    /** The keys to give, by game id. Only games that need a new key are in it. */
    fun split(
        games: List<Game>,
        savesDir: File,
        statesDir: File,
        previewsDir: File,
    ): Map<Int, String> {
        val taken = games.mapTo(ArrayList()) { it.saveName() }
        val changes = LinkedHashMap<Int, String>()

        games
            .sortedBy { it.id }
            .groupBy { SaveKeys.stem(it.saveName()).lowercase() }
            .values
            .filter { it.size > 1 }
            .forEach { group ->
                // The first keeps its key; the rest are given their own.
                for (game in group.drop(1)) {
                    val oldKey = game.saveName()
                    val newKey = SaveKeys.assign(game.fileName, game.fileUri, taken)
                    if (stemOf(newKey) == stemOf(oldKey)) continue
                    taken.add(newKey)
                    copyAll(oldKey, newKey, savesDir, statesDir, previewsDir)
                    changes[game.id] = newKey
                }
            }
        return changes
    }

    private fun stemOf(key: String) = SaveKeys.stem(key).lowercase()

    private fun copyAll(
        from: String,
        to: String,
        savesDir: File,
        statesDir: File,
        previewsDir: File,
    ) {
        val oldSrm = File(savesDir, SaveKeys.srmName(from))
        val newSrm = File(savesDir, SaveKeys.srmName(to))
        if (oldSrm.isFile && !newSrm.exists()) oldSrm.copyTo(newSrm)

        for (root in listOf(statesDir, previewsDir)) {
            if (!root.isDirectory) continue
            root.walkTopDown()
                .filter { it.isFile && it.name.startsWith("$from.") }
                .toList()
                .forEach { file ->
                    val copy = File(file.parentFile, to + file.name.removePrefix(from))
                    if (!copy.exists()) file.copyTo(copy)
                }
        }
    }
}
