/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import com.offsetnull.bt.service.plugin.Plugin;
import java.util.ArrayList;

public class ImportedSettings {
    public final ArrayList<Plugin> plugins;
    public final String summary;

    public ImportedSettings(ArrayList<Plugin> plugins, String summary) {
        this.plugins = plugins;
        this.summary = summary;
    }
}
