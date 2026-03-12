package org.ncmud.mudwammer.service;

import org.ncmud.mudwammer.service.function.SpecialCommand;
import org.ncmud.mudwammer.service.plugin.ConnectionSettingsPlugin;
import org.ncmud.mudwammer.service.plugin.Plugin;

import java.util.ArrayList;
import java.util.Map;

public interface AliasContext {
    ArrayList<Plugin> getPlugins();

    ConnectionSettingsPlugin getConnectionSettings();

    Map<String, String> getCaptureMap();

    void dispatchNoProcess(byte[] data);

    void sendBytesToWindow(byte[] data);

    Map<String, SpecialCommand> getSpecialCommands();

    String getEncoding();

    String getCRLF();
}
