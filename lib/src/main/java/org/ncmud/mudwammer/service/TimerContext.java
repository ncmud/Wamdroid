package org.ncmud.mudwammer.service;

import org.ncmud.mudwammer.service.plugin.ConnectionSettingsPlugin;
import org.ncmud.mudwammer.service.plugin.Plugin;

import java.util.List;

public interface TimerContext {
    List<Plugin> getPlugins();

    ConnectionSettingsPlugin getConnectionSettings();

    void dispatchNoProcess(byte[] data);

    android.content.Context getContext();
}
