package com.swordfish.lemuroid.app.shared.box

import android.content.Context
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.lib.box.BoxLink
import com.swordfish.lemuroid.lib.box.BoxNotTheBoxException
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs the whole sync with the game box: games first, then cheats, then every save. It runs
 * when the app opens (quietly, and only on Wi-Fi) and when "Sync now" is pressed in Settings.
 * One game's save is also sent as soon as that game closes.
 */
class BoxSyncRunner(
    private val context: Context,
    private val link: BoxLink,
    private val database: RetrogradeDatabase,
) {
    sealed interface Status {
        data object Idle : Status

        data object Connecting : Status

        data class Games(val done: Int, val total: Int) : Status

        data object Cheats : Status

        data object Library : Status

        data object Saves : Status

        data class Done(val games: Int, val savesIn: Int, val savesOut: Int) : Status

        data object Unreachable : Status

        /** Something answers at the address, but not the box the first sync found there. */
        data object NotTheBox : Status
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)
    private val state = MutableStateFlow<Status>(Status.Idle)

    val status: StateFlow<Status> = state

    fun lastSynced(): Long = SharedPreferencesHelper.getSharedPreferences(context).getLong(PREF_LAST, 0L)

    /**
     * [byHand] is a press of "Sync now": it waits longer for the box and says so when the box
     * does not answer. The sync on opening the app gives up quickly and says nothing.
     */
    fun syncNow(byHand: Boolean) {
        if (!link.isSetUp()) return
        if (!byHand && !link.onHomeNetwork()) return
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            try {
                state.value = Status.Connecting
                val before = database.gameDao().selectAll()
                val games =
                    link.open(patient = byHand).use { share ->
                        val result =
                            link.sync.syncGames(share, before) { done, total ->
                                state.value = Status.Games(done, total)
                            }
                        state.value = Status.Cheats
                        link.sync.syncCheats(share)
                        result
                    }

                if (games.copied > 0) {
                    // The new games have to be in the library before their saves can be matched.
                    state.value = Status.Library
                    LibraryIndexScheduler.scheduleLibrarySync(context)
                    delay(INDEX_START_MILLIS)
                    withTimeoutOrNull(INDEX_WAIT_MILLIS) {
                        PendingOperationsMonitor(context).anyLibraryOperationInProgress().first { !it }
                    }
                }

                state.value = Status.Saves
                val saves = link.open(patient = byHand).use { link.sync.syncSaves(it, database.gameDao().selectAll()) }

                SharedPreferencesHelper.getSharedPreferences(context).edit()
                    .putLong(PREF_LAST, System.currentTimeMillis()).apply()
                state.value = Status.Done(games.copied, saves.pulled, saves.pushed)
            } catch (e: Exception) {
                Timber.i(e, "Game box: sync stopped")
                state.value =
                    when {
                        !byHand -> Status.Idle
                        e is BoxNotTheBoxException -> Status.NotTheBox
                        else -> Status.Unreachable
                    }
            } finally {
                running.set(false)
            }
        }
    }

    /** Sends the save of the game that just closed, if the box is in reach. */
    fun afterGame(game: Game) {
        scope.launch { link.syncSaveQuietly(game) }
    }

    private companion object {
        const val PREF_LAST = "box_last_synced"
        const val INDEX_START_MILLIS = 2_000L
        const val INDEX_WAIT_MILLIS = 10 * 60_000L
    }
}
