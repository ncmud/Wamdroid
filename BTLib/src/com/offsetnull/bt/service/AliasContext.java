package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.service.function.SpecialCommand;
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
