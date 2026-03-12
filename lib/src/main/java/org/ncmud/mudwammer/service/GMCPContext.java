package org.ncmud.mudwammer.service;

import org.ncmud.mudwammer.service.plugin.ConnectionSettingsPlugin;
import org.ncmud.mudwammer.service.plugin.Plugin;

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
