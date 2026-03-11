package com.offsetnull.bt.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ServiceDispatcherTest {

    @Test
    fun `newConnection creates Connection and stores it`() {
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ -> FakeConnection(display) }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("TestMUD", "mud.example.com", 4000))

        assertNotNull(dispatcher.connections["TestMUD"])
    }

    @Test
    fun `newConnection sets active connection`() {
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ -> FakeConnection(display) }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("TestMUD", "mud.example.com", 4000))

        assertEquals("TestMUD", dispatcher.activeConnection)
    }

    @Test
    fun `duplicate newConnection does not replace existing`() {
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ -> FakeConnection(display) }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("MUD1", "a.com", 1))
        val first = dispatcher.connections["MUD1"]
        dispatcher.dispatch(ServiceCommand.NewConnection("MUD1", "b.com", 2))

        assertEquals(first, dispatcher.connections["MUD1"])
    }

    @Test
    fun `switchConnection updates active clutch`() {
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ -> FakeConnection(display) }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("MUD1", "a.com", 1))
        dispatcher.dispatch(ServiceCommand.NewConnection("MUD2", "b.com", 2))
        dispatcher.dispatch(ServiceCommand.SwitchConnection("MUD1"))

        assertEquals("MUD1", dispatcher.activeConnection)
    }

    @Test
    fun `startup sends startup to active connection`() {
        val events = mutableListOf<String>()
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ ->
                FakeConnection(display).also { it.onStartup = { events.add("startup:$display") } }
            }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("TestMUD", "mud.example.com", 4000))
        dispatcher.dispatch(ServiceCommand.Startup)

        assertEquals(listOf("startup:TestMUD"), events)
    }

    @Test
    fun `reloadSettings delegates to active connection`() {
        val events = mutableListOf<String>()
        val dispatcher = ServiceDispatcher(
            connectionFactory = { display, _, _ ->
                FakeConnection(display).also { it.onReload = { events.add("reload:$display") } }
            }
        )
        dispatcher.dispatch(ServiceCommand.NewConnection("TestMUD", "mud.example.com", 4000))
        dispatcher.dispatch(ServiceCommand.ReloadSettings)

        assertEquals(listOf("reload:TestMUD"), events)
    }
}

private class FakeConnection(val display: String) : ConnectionHandle {
    var onStartup: (() -> Unit)? = null
    var onReload: (() -> Unit)? = null
    override fun startup() { onStartup?.invoke() }
    override fun reloadSettings() { onReload?.invoke() }
    override fun initWindows() {}
}
