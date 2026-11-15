package com.swordfish.lemuroid.lib.box

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The way to the game box: the address the player gave in Settings, and whether it is worth
 * trying at all. Away from home the phone is not on Wi-Fi, or the box does not answer within a
 * couple of seconds, and the game simply opens with the save the phone has.
 */
class BoxLink(
    private val context: Context,
    val sync: BoxSync,
) {
    fun address(): String =
        SharedPreferencesHelper.getSharedPreferences(context).getString(PREF_ADDRESS, "").orEmpty().trim()

    fun setAddress(address: String) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().putString(PREF_ADDRESS, address.trim()).apply()
    }

    fun isSetUp(): Boolean = address().isNotEmpty()

    fun onHomeNetwork(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** @throws BoxUnreachableException when the box does not answer in time. */
    fun open(patient: Boolean): BoxShare =
        SmbBoxShare.connect(address(), if (patient) PATIENT_SECONDS else QUICK_SECONDS)

    /**
     * One game's save, before it opens or after it closes. Never throws and never waits long:
     * a box that cannot be reached leaves the save for the next time it can.
     */
    suspend fun syncSaveQuietly(game: Game): SaveAction =
        withContext(Dispatchers.IO) {
            if (!isSetUp() || !onHomeNetwork() || sync.keyFor(game) == null) return@withContext SaveAction.NOTHING
            try {
                open(patient = false).use { sync.syncSave(it, game) }
            } catch (e: Exception) {
                Timber.i("Game box: save of ${game.id} left for later (${e.message})")
                SaveAction.NOTHING
            }
        }

    companion object {
        const val PREF_ADDRESS = "box_address"
        private const val QUICK_SECONDS = 3L
        private const val PATIENT_SECONDS = 15L
    }
}
