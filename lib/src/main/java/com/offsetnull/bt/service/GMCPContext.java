package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface GMCPContext {
    List<Plugin> getPlugins();

    Map<String, Plugin> getPluginMap();

    ConnectionSettingsPlugin getConnectionSettings();

    mth.core.client.TelnetClientSession getTelnetSession();

    void sendGMCPTriggered(String plugin, String callback, HashMap<String, Object> data);
}
