package com.swordfish.lemuroid.lib.box

/** A game as the box knows it: the system folder it is in and its file name without the ending. */
data class BoxKey(
    val system: String,
    val stem: String,
) {
    override fun toString(): String = "$system/$stem"

    companion object {
        fun parse(text: String): BoxKey? {
            val slash = text.indexOf('/')
            if (slash <= 0 || slash == text.length - 1) return null
            return BoxKey(text.substring(0, slash), text.substring(slash + 1))
        }
    }
}

/** Where a Batocera box keeps things, and how its files are named. */
object BoxNames {
    /** The system folders this app plays. They are also the systems' names in the library. */
    val SYSTEMS = listOf("gb", "gbc")

    private val ROM_ENDINGS = setOf("zip", "gb", "gbc")

    /** RetroArch looks for a game's cheats under the core's name, in a file named after the ROM. */
    const val CHEATS_FOLDER = "cheats/cht/Gambatte"

    const val BACKUP_FOLDER = "saves-sync-backup"

    private val CHEAT_TAILS = listOf(" (Game Genie)", " (GameShark)")

    fun romsFolder(system: String) = "roms/$system"

    fun savesFolder(system: String) = "saves/$system"

    fun stem(name: String): String = name.substringBeforeLast('.')

    /** A file name that stays inside the folder it is joined to: no separator, no NUL, not "." or "..". */
    fun isPlainName(name: String): Boolean =
        name.isNotEmpty() && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it == '\u0000' }

    fun isRom(name: String): Boolean =
        !name.startsWith(".") && name.substringAfterLast('.', "").lowercase() in ROM_ENDINGS

    fun saveName(stem: String) = "$stem.srm"

    /** The box's own resume point for a game, which it loads in place of the battery save. */
    fun autoStateNames(stem: String) = listOf("$stem.state.auto", "$stem.state.auto.png")

    /**
     * The ROM name a cheat file is for, or null when it is not a cheat file. A game can have up
     * to three: "Name.cht", "Name (Game Genie).cht" and "Name (GameShark).cht".
     */
    fun cheatFileStem(fileName: String): String? {
        if (!fileName.endsWith(".cht", ignoreCase = true)) return null
        val base = fileName.dropLast(4)
        val tail = CHEAT_TAILS.firstOrNull { base.endsWith(it) } ?: return base
        return base.dropLast(tail.length)
    }
}
