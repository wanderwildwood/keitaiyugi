package com.swordfish.lemuroid.lib.box

/** One cheat: what it does, and the code that does it. */
data class Cheat(
    val description: String,
    val code: String,
) {
    /** Game Genie codes are written with dashes; everything else here is GameShark. */
    val isGameGenie: Boolean get() = code.contains('-')
}

/**
 * Reads RetroArch's cheat files:
 *
 *     cheats = 2
 *     cheat0_desc = "Infinite Lives"
 *     cheat0_code = "0109E8FF"
 *     cheat0_enable = false
 *
 * A code may be several joined with `+`. A code with a `?` in it wants a digit filled in by
 * hand and cannot be used as it stands, so it is left out, as is anything that is not a code.
 */
object ChtFile {
    private val LINE = Regex("""^\s*cheat(\d+)_(desc|code)\s*=\s*"?(.*?)"?\s*$""")
    private val CODE = Regex("""^[0-9A-Fa-f]+(-[0-9A-Fa-f]+)*$""")

    fun parse(text: String): List<Cheat> {
        val descriptions = HashMap<Int, String>()
        val codes = HashMap<Int, String>()
        for (line in text.lineSequence()) {
            val match = LINE.matchEntire(line) ?: continue
            val index = match.groupValues[1].toIntOrNull() ?: continue
            val value = match.groupValues[3].trim()
            if (match.groupValues[2] == "desc") descriptions[index] = value else codes[index] = value
        }
        return codes.keys.sorted().mapNotNull { index ->
            val code = codes.getValue(index).replace(" ", "")
            if (!isUsable(code)) return@mapNotNull null
            Cheat(descriptions[index].orEmpty().ifBlank { code }, code)
        }
    }

    fun isUsable(code: String): Boolean = code.isNotEmpty() && code.split('+').all { CODE.matches(it) }
}
