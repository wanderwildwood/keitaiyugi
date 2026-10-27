package com.swordfish.lemuroid.lib.saves

import java.security.MessageDigest

/**
 * The name a game's saves are filed under.
 *
 * Saves used to be filed under the ROM's bare file name, so two ROMs with the same name in
 * different folders shared one battery save and one set of states, and "Foo.gb" and "Foo.gbc"
 * shared one .srm (it is named without the extension). A game now carries a `saveKey`. Games that
 * were already in the library keep their file name as the key, so nothing they saved moves. A
 * game that arrives under a name whose stem another game already uses gets the stem, a short hash
 * of its address, and its own extension: "Foo~1a2b3c4d.gb".
 */
object SaveKeys {
    /** The name without its extension: what the battery save is named after. */
    fun stem(name: String): String = name.substringBeforeLast(".")

    fun srmName(saveKey: String): String = "${stem(saveKey)}.srm"

    fun stateName(saveKey: String): String = "$saveKey.state"

    fun slotName(
        saveKey: String,
        index: Int,
    ): String = "$saveKey.slot${index + 1}"

    fun previewName(
        saveKey: String,
        index: Int,
    ): String = "${slotName(saveKey, index)}.jpg"

    /** Eight hex digits of the SHA-1 of the address; the same address always gives the same ones. */
    fun hash(fileUri: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(fileUri.toByteArray(Charsets.UTF_8))
            .take(4)
            .joinToString("") { "%02x".format(it) }

    /**
     * The key for a game that has none yet. [taken] is every key in use, including those handed
     * out earlier in the same scan. Stems are compared without regard to case.
     */
    fun assign(
        fileName: String,
        fileUri: String,
        taken: Collection<String>,
    ): String {
        val stems = taken.mapTo(HashSet()) { stem(it).lowercase() }
        if (stem(fileName).lowercase() !in stems) return fileName

        val ext = fileName.substringAfterLast(".", "")
        val tail = if (ext.isEmpty()) "" else ".$ext"
        var candidate = "${stem(fileName)}~${hash(fileUri)}$tail"
        var n = 2
        while (stem(candidate).lowercase() in stems) {
            candidate = "${stem(fileName)}~${hash(fileUri)}-$n$tail"
            n++
        }
        return candidate
    }
}
