package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;

import java.util.List;
import java.util.Map;

public interface GMCPContext {
    List<Plugin> getPlugins();

    Map<String, Plugin> getPluginMap();

    Processor getProcessor();

    DataPumper getPump();

    ConnectionSettingsPlugin getConnectionSettings();
}
