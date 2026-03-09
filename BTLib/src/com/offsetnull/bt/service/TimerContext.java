package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;
import java.util.List;

public interface TimerContext {
    List<Plugin> getPlugins();
    ConnectionSettingsPlugin getConnectionSettings();
    void dispatchNoProcess(byte[] data);
    android.content.Context getContext();
}
