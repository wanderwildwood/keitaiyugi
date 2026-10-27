package com.swordfish.lemuroid.lib.library

/** What this app lists as a game. */
object RomFilter {
    private val EXTENSIONS = setOf("gb", "gbc")
    private val SYSTEMS = setOf(SystemID.GB.dbname, SystemID.GBC.dbname)

    /** [fileName] is the ROM's own name: the entry's name for a ROM inside a zip. */
    fun isGameBoyRom(
        systemDbname: String?,
        fileName: String,
    ): Boolean {
        if (systemDbname !in SYSTEMS) return false
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return extension in EXTENSIONS
    }
}
