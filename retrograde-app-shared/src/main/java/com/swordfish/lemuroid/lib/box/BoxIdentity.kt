package com.swordfish.lemuroid.lib.box

/**
 * Which box answers at the saved address. Samba gives every server a GUID of its own, sent in
 * the first answer of every SMB connection. The first good sync remembers it; after that a
 * different one at that address is another machine, not the game box, and counts as the box
 * being out of reach, so no save goes to it and nothing it holds comes onto the phone.
 * No location permission is needed, which telling home networks apart by name would take.
 */
object BoxIdentity {
    enum class Verdict { REMEMBER, SAME, DIFFERENT }

    private const val NONE = "00000000-0000-0000-0000-000000000000"

    /** [seen] is the GUID the box gave now, [kept] the one remembered; either may be missing. */
    fun check(
        kept: String?,
        seen: String?,
    ): Verdict {
        val now = seen?.lowercase()?.takeIf { it.isNotBlank() && it != NONE }
        val before = kept?.lowercase()?.takeIf { it.isNotBlank() }
        return when {
            // A server that gives no GUID cannot be told apart; it is taken as it was before.
            now == null -> Verdict.SAME
            before == null -> Verdict.REMEMBER
            now == before -> Verdict.SAME
            else -> Verdict.DIFFERENT
        }
    }
}
