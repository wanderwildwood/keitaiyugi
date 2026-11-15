package com.swordfish.lemuroid.lib.box

import java.security.MessageDigest

enum class SaveAction { NOTHING, PULL, PUSH }

/**
 * Which way a battery save goes between the phone and the box.
 *
 * The phone writes the save again every time a game closes, changed or not, so a date alone
 * says little. What is compared is the content: [agreed] is the save both sides held when they
 * last matched. A side that still holds it has not been played since, and takes the other's.
 * Only when both have moved on (or have never been matched) does the newer file win.
 */
object SaveMerge {
    data class Side(
        val hash: String,
        val modified: Long,
    )

    fun decide(
        local: Side?,
        remote: Side?,
        agreed: String?,
    ): SaveAction =
        when {
            local == null && remote == null -> SaveAction.NOTHING
            local == null -> SaveAction.PULL
            remote == null -> SaveAction.PUSH
            local.hash == remote.hash -> SaveAction.NOTHING
            agreed != null && local.hash == agreed -> SaveAction.PULL
            agreed != null && remote.hash == agreed -> SaveAction.PUSH
            local.modified > remote.modified -> SaveAction.PUSH
            else -> SaveAction.PULL
        }

    fun hash(data: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(data).joinToString("") { "%02x".format(it) }
}
