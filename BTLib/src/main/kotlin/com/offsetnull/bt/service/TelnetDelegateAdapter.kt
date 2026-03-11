package com.offsetnull.bt.service

import android.util.Log
import mth.core.client.TelnetClientDelegate

internal class TelnetDelegateAdapter(private val conn: Connection) : TelnetClientDelegate {

    override fun write(data: ByteArray) {
        conn.mPump?.sendData(data)
    }

    override fun onLocalEchoChanged(enabled: Boolean) {
        conn.mSettings.isLocalEcho = enabled
    }

    override fun onGMCPNegotiated() {
        val session = conn.mTelnetSession ?: return
        session.sendGMCP("core.hello", "{\"client\": \"BlowTorch\",\"version\": \"1.4\"}")
        val supports = conn.mGMCPSupports
        session.sendGMCP("core.supports.set", "[$supports]")
    }

    override fun onGMCPReceived(module: String, json: String) {
        conn.mGMCPHandler.dispatchGMCPData(module, json)
    }

    override fun onMSDPVariable(name: String, value: String) {
        Log.d("MTH", "MSDP: $name = $value")
    }

    override fun onPromptReceived() {
        // Future: prompt detection via EOR/GA
    }

    override fun onBellReceived() {
        conn.sendCommand(ConnectionCommand.BellReceived())
    }

    override fun log(message: String) {
        Log.d("MTH", message)
    }
}
