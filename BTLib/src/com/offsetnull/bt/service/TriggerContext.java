package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.window.TextTree;
import java.util.ArrayList;
import java.util.HashMap;

public interface TriggerContext {
    ArrayList<Plugin> getPlugins();
    ConnectionSettingsPlugin getConnectionSettings();
    HashMap<String, String> getCaptureMap();
    void sendBytesToWindow(byte[] data);
    void dispatchNoProcess(byte[] data);
    String getEncoding();
    Processor getProcessor();
    StellarService getService();
    ConnectionWindowManager getWindowManager();
    String getDisplay();
    String getHostName();
    int getPort();
    android.os.Handler getHandler();
}
