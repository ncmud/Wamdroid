/*
 * Copyright (C) Dan Block 2013
 */
package org.ncmud.mudwammer.service;

import org.ncmud.mudwammer.service.plugin.ConnectionSettingsPlugin;
import org.ncmud.mudwammer.service.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;

public interface SettingsContext {
    ArrayList<Plugin> getPlugins();

    Map<String, Plugin> getPluginMap();

    ConnectionSettingsPlugin getConnectionSettings();

    StellarService getService();

    String getDisplay();

    String getHostName();

    int getPort();

    android.content.Context getContext();

    Map<String, ArrayList<String>> getLinkMap();

    ConnectionWindowManager getWindowManager();

    void updateSetting(String key, String value);

    SettingsChangedListener createWindowSettingsChangedListener(String windowName);
}
