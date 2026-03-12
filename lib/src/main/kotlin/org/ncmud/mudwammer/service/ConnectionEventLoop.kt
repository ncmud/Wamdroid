package org.ncmud.mudwammer.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Replaces Connection's Handler/ConnectionHandler with a coroutine Channel.
 * Dispatches [ConnectionCommand]s to [ConnectionDispatcher] on the main thread.
 */
class ConnectionEventLoop(
    private val dispatcher: ConnectionDispatcher,
) {
    private val channel = Channel<ConnectionCommand>(Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        scope.launch {
            for (command in channel) {
                try {
                    dispatcher.dispatch(command)
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    android.util.Log.e("ConnectionEventLoop", "Error dispatching $command", e)
                }
            }
        }
    }

    fun send(command: ConnectionCommand) {
        val result = channel.trySend(command)
        if (result.isFailure) {
            android.util.Log.e("ConnectionEventLoop", "trySend failed for $command", result.exceptionOrNull())
        }
    }

    /** For delayed sends (reconnect timer, GMCP retry). Returns a Job that can be cancelled. */
    fun sendDelayed(command: ConnectionCommand, delayMs: Long): Job {
        return scope.launch {
            delay(delayMs)
            channel.send(command)
        }
    }

    fun shutdown() {
        channel.close()
        scope.cancel()
    }
}
