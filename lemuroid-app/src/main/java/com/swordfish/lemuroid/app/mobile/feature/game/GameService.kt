package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.swordfish.lemuroid.app.mobile.shared.NotificationsManager
import dagger.android.DaggerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class GameService : DaggerService() {
    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        current = WeakReference(this)
        startConsumer()
    }

    override fun onStartCommand(
        intent: Intent,
        flags: Int,
        startId: Int,
    ): Int {
        displayNotification(intent)
        return START_NOT_STICKY
    }

    private fun displayNotification(intent: Intent) {
        val gameIntent =
            intent.getParcelableExtra<Intent>(EXTRA_GAME_ACTIVITY_INTENT)
                ?: return
        val notification = NotificationsManager(applicationContext).gameRunningNotification(gameIntent)
        ServiceCompat.startForeground(
            this,
            NotificationsManager.GAME_RUNNING_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun hideNotification() {
        NotificationManagerCompat.from(this).cancel(NotificationsManager.GAME_RUNNING_NOTIFICATION_ID)
    }

    // Android 15 and later give a data-sync service six hours, then call this; one that does
    // not stop is ended with an error. The game was saved when it left the screen. Its saves and
    // its leaving are not the service's to run, so they still happen once it has stopped.
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        hideNotification()
    }

    companion object {
        private const val EXTRA_GAME_ACTIVITY_INTENT = "EXTRA_GAME_ACTIVITY_INTENT"

        private data class GameProcessTask(
            val task: suspend () -> Unit = {},
            val terminate: Boolean = false,
        )

        private val tasks = Channel<GameProcessTask>(capacity = Channel.BUFFERED)

        // The game process's own: it runs the saves and the leaving whether or not the service is
        // still there, and the process ends with the first request to leave.
        private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val consuming = AtomicBoolean(false)

        @Volatile
        private var current: WeakReference<GameService>? = null

        private fun startConsumer() {
            if (!consuming.compareAndSet(false, true)) return
            processScope.launch {
                awaitTermination()
                withContext(Dispatchers.Main) {
                    current?.get()?.let { service ->
                        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        service.stopSelf()
                    }
                    exitProcess(0)
                }
            }
        }

        fun startService(
            context: Context,
            gameActivityIntent: Intent,
        ) {
            context.startService(
                Intent(context, GameService::class.java).apply {
                    putExtra(EXTRA_GAME_ACTIVITY_INTENT, gameActivityIntent)
                },
            )
        }

        fun schedule(task: suspend () -> Unit) {
            startConsumer()
            val result = tasks.trySend(GameProcessTask(task = task))
            Timber.i("GameService.schedule sent=%s", result.isSuccess)
        }

        fun requestTermination() {
            startConsumer()
            val result = tasks.trySend(GameProcessTask(terminate = true))
            Timber.i("GameService.requestTermination sent=%s", result.isSuccess)
        }

        private suspend fun awaitTermination() {
            for (task in tasks) {
                runCatching { task.task() }
                    .onFailure { Timber.e(it, "GameService task failed") }

                if (task.terminate) {
                    return
                }
            }
        }
    }
}
