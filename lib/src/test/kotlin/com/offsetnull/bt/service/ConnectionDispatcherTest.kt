package com.offsetnull.bt.service

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConnectionDispatcherTest {

    @Test
    fun `Process dispatches data to trigger manager`() {
        val triggers = FakeTriggerManager()
        val dispatcher = ConnectionDispatcher(triggerManager = triggers)

        val data = "You are standing in a forest.\n".toByteArray()
        dispatcher.dispatch(ConnectionCommand.Process(data))

        assertEquals(1, triggers.dispatched.size)
        assertArrayEquals(data, triggers.dispatched[0])
    }

    @Test
    fun `SendDataString encodes and sends to pump`() {
        val pump = FakePump()
        val dispatcher = ConnectionDispatcher(pump = pump, encoding = "UTF-8")

        dispatcher.dispatch(ConnectionCommand.SendDataString("look\n"))

        assertEquals(1, pump.sent.size)
        assertArrayEquals("look\n".toByteArray(), pump.sent[0])
    }

    @Test
    fun `SendDataBytes sends raw bytes to pump`() {
        val pump = FakePump()
        val dispatcher = ConnectionDispatcher(pump = pump)

        val data = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x01)
        dispatcher.dispatch(ConnectionCommand.SendDataBytes(data))

        assertEquals(1, pump.sent.size)
        assertArrayEquals(data, pump.sent[0])
    }

    @Test
    fun `Disconnected kills net threads and marks disconnected`() {
        val pump = FakePump()
        val lifecycle = FakeLifecycle()
        val dispatcher = ConnectionDispatcher(pump = pump, lifecycle = lifecycle)

        dispatcher.dispatch(ConnectionCommand.Disconnected)

        assertTrue(lifecycle.netThreadsKilled)
        assertTrue(lifecycle.disconnected)
    }

    @Test
    fun `TerminatedByPeer kills net threads and marks disconnected`() {
        val pump = FakePump()
        val lifecycle = FakeLifecycle()
        val dispatcher = ConnectionDispatcher(pump = pump, lifecycle = lifecycle)

        dispatcher.dispatch(ConnectionCommand.TerminatedByPeer())

        assertTrue(lifecycle.netThreadsKilled)
        assertTrue(lifecycle.disconnectedByPeer)
    }

    @Test
    fun `BellReceived triggers vibrate when enabled`() {
        val service = FakeServiceCallbacks(vibrateOnBell = true)
        val dispatcher = ConnectionDispatcher(serviceCallbacks = service)

        dispatcher.dispatch(ConnectionCommand.BellReceived())

        assertTrue(service.vibrated)
    }

    @Test
    fun `BellReceived does not vibrate when disabled`() {
        val service = FakeServiceCallbacks(vibrateOnBell = false)
        val dispatcher = ConnectionDispatcher(serviceCallbacks = service)

        dispatcher.dispatch(ConnectionCommand.BellReceived())

        assertTrue(!service.vibrated)
    }

    @Test
    fun `ProcessorWarning dispatches text to window`() {
        val display = FakeDisplay()
        val dispatcher = ConnectionDispatcher(display = display)

        dispatcher.dispatch(ConnectionCommand.ProcessorWarning("Warning text"))

        assertEquals(listOf("Warning text"), display.sent)
    }

    @Test
    fun `Connected resets auto reconnect counter`() {
        val lifecycle = FakeLifecycle()
        val dispatcher = ConnectionDispatcher(lifecycle = lifecycle)

        dispatcher.dispatch(ConnectionCommand.Connected)

        assertTrue(lifecycle.reconnectReset)
    }

    @Test
    fun `ReloadSettings delegates to lifecycle`() {
        val lifecycle = FakeLifecycle()
        val dispatcher = ConnectionDispatcher(lifecycle = lifecycle)

        dispatcher.dispatch(ConnectionCommand.ReloadSettings)

        assertTrue(lifecycle.settingsReloaded)
    }
}

private class FakeTriggerManager : TriggerManagerHandle {
    val dispatched = mutableListOf<ByteArray>()
    override fun dispatch(data: ByteArray) { dispatched.add(data) }
}

private class FakePump : PumpHandle {
    val sent = mutableListOf<ByteArray>()
    var compressionStarted = false
    override fun send(data: ByteArray) { sent.add(data) }
    override fun startCompression(trailingData: ByteArray?) { compressionStarted = true }
}

private class FakeServiceCallbacks(
    override val vibrateOnBell: Boolean = false,
    override val notifyOnBell: Boolean = false,
    override val displayOnBell: Boolean = false,
) : BellCallbacks {
    var vibrated = false
    var notified = false
    var displayed = false
    override fun doVibrateBell() { vibrated = true }
    override fun doNotifyBell() { notified = true }
    override fun doDisplayBell() { displayed = true }
}

private class FakeDisplay : DisplayHandle {
    val sent = mutableListOf<String>()
    override fun sendDataToWindow(text: String) { sent.add(text) }
    override fun dispatchNoProcess(data: ByteArray) {}
    override fun dispatchDialog(message: String) {}
}

private class FakeLifecycle : LifecycleHandle {
    var netThreadsKilled = false
    var disconnected = false
    var disconnectedByPeer = false
    var reconnectReset = false
    var settingsReloaded = false
    var started = false
    override fun killNetThreads() { netThreadsKilled = true }
    override fun doDisconnect(byPeer: Boolean) {
        if (byPeer) disconnectedByPeer = true else disconnected = true
    }
    override fun doReconnect() {}
    override fun doStartup() { started = true }
    override fun resetAutoReconnect() { reconnectReset = true }
    override fun reloadSettings() { settingsReloaded = true }
}
