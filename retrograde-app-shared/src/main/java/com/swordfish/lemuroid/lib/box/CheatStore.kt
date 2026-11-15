package com.swordfish.lemuroid.lib.box

import android.content.SharedPreferences
import java.io.File

/** The cheats copied from the box, and which of them the player has switched on for each game. */
class CheatStore(
    private val cheatsDir: File,
    private val preferences: SharedPreferences,
) {
    /** Every usable cheat for the game, in file order, each code once. */
    fun cheatsFor(key: BoxKey?): List<Cheat> {
        if (key == null) return emptyList()
        val files =
            cheatsDir.listFiles { file -> file.isFile && BoxNames.cheatFileStem(file.name) == key.stem }
                ?.sortedBy { it.name }
                ?: return emptyList()
        return files
            .flatMap { file -> runCatching { ChtFile.parse(file.readText()) }.getOrDefault(emptyList()) }
            .distinctBy { it.code.uppercase() }
    }

    fun enabledCodes(key: BoxKey): Set<String> = preferences.getStringSet(prefKey(key), emptySet()).orEmpty()

    fun setEnabled(
        key: BoxKey,
        cheat: Cheat,
        enabled: Boolean,
    ) {
        val codes = enabledCodes(key).toMutableSet()
        if (enabled) codes.add(cheat.code) else codes.remove(cheat.code)
        preferences.edit().putStringSet(prefKey(key), codes).apply()
    }

    /**
     * What to hand the core. Gambatte keeps one list of Game Genie codes and one of GameShark
     * codes, and each call replaces the whole list of its kind, so the codes that are on are
     * sent as two joined strings rather than one by one.
     */
    fun codesToApply(key: BoxKey?): List<String> {
        if (key == null) return emptyList()
        val enabled = enabledCodes(key)
        if (enabled.isEmpty()) return emptyList()
        val (genie, shark) = cheatsFor(key).filter { it.code in enabled }.partition { it.isGameGenie }
        return listOf(genie, shark).filter { it.isNotEmpty() }.map { list -> list.joinToString("+") { it.code } }
    }

    private fun prefKey(key: BoxKey) = "cheats_on:$key"
}
