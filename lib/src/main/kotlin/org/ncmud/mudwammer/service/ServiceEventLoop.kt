package org.ncmud.mudwammer.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Replaces StellarService's Handler/ServiceHandler with a coroutine Channel.
 * Dispatches [ServiceCommand]s to [ServiceDispatcher] on the main thread,
 * then invokes side-effect callbacks.
 */
class ServiceEventLoop(
    private val dispatcher: ServiceDispatcher,
    private val sideEffects: SideEffects,
) {
    private val channel = Channel<ServiceCommand>(Channel.BUFFERED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    interface SideEffects {
        fun onNewConnection(display: String)
        fun onReloadWindows()
        fun onSwitchTo(display: String)
    }

    fun start() {
        scope.launch {
            for (command in channel) {
                try {
                    dispatcher.dispatch(command)
                    when (command) {
                        is ServiceCommand.NewConnection ->
                            sideEffects.onNewConnection(command.display)
                        is ServiceCommand.ReloadSettings -> sideEffects.onReloadWindows()
                        is ServiceCommand.SwitchConnection ->
                            sideEffects.onSwitchTo(command.display)
                        else -> {}
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Log.e("ServiceEventLoop", "Error dispatching $command", e)
                }
            }
        }
    }

    fun send(command: ServiceCommand) {
        channel.trySend(command)
    }

    fun shutdown() {
        channel.close()
        scope.cancel()
    }
}
