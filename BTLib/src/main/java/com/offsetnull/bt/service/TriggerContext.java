package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;

public interface TriggerContext {
    ArrayList<Plugin> getPlugins();

    ConnectionSettingsPlugin getConnectionSettings();

    HashMap<String, String> getCaptureMap();

    void sendBytesToWindow(byte[] data);

    void dispatchNoProcess(byte[] data);

    String getEncoding();

    mth.core.client.TelnetClientSession getTelnetSession();

    StellarService getService();

    ConnectionWindowManager getWindowManager();

    String getDisplay();

    String getHostName();

    int getPort();

    /** @deprecated Use {@link #sendCommand(ConnectionCommand)} instead. */
    @Deprecated
    android.os.Handler getHandler();

    /** Sends a command through the connection event loop. */
    void sendCommand(ConnectionCommand command);
}
