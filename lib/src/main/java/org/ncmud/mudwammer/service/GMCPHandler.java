package org.ncmud.mudwammer.service;

import android.util.Log;

import org.ncmud.mudwammer.responder.TriggerResponder;
import org.ncmud.mudwammer.responder.script.ScriptResponder;
import org.ncmud.mudwammer.service.plugin.Plugin;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class GMCPHandler {

    private final GMCPContext context;

    private final HashMap<String, ArrayList<GMCPWatcher>> gmcpWatchers = new HashMap<>();

    private final GMCPData gmcpData = new GMCPData();

    public GMCPHandler(GMCPContext context) {
        this.context = context;
    }

    public void loadTriggers() {
        gmcpWatchers.clear();
        String gmcpChar = context.getConnectionSettings().getGMCPTriggerChar();
        for (int i = 0; i < context.getPlugins().size(); i++) {
            Plugin p = context.getPlugins().get(i);
            HashMap<String, TriggerData> triggers = p.getSettings().getTriggers();
            for (TriggerData t : triggers.values()) {
                if (!t.isInterpretAsRegex()) {
                    if (t.getPattern().startsWith(gmcpChar)) {
                        for (TriggerResponder r : t.getResponders()) {
                            if (r instanceof ScriptResponder) {
                                ScriptResponder s = (ScriptResponder) r;
                                String callback = s.getFunction();
                                String module =
                                        t.getPattern().substring(1, t.getPattern().length());
                                String name = p.getName();
                                addWatcher(module, name, callback);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addWatcher(String module, String plugin, String callback) {
        ArrayList<GMCPWatcher> list = gmcpWatchers.get(module);
        if (list == null) {
            list = new ArrayList<>();
            gmcpWatchers.put(module, list);
        }
        list.add(new GMCPWatcher(plugin, callback));
    }

    public void dispatchGMCPData(String module, String jsonString) {
        try {
            JSONObject jo = new JSONObject(jsonString);
            gmcpData.absorb(module, jo);
        } catch (JSONException e) {
            Log.e("GMCP", "GMCP PARSING FOR: " + jsonString);
            Log.e("GMCP", "REASON: " + e.getMessage());
        }

        ArrayList<GMCPWatcher> list = gmcpWatchers.get(module);
        if (list != null) {
            for (GMCPWatcher w : list) {
                HashMap<String, Object> data = gmcpData.getTable(module);
                context.sendGMCPTriggered(w.plugin, w.callback, data);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void handleCallback(String pluginName, String callback, HashMap<String, Object> data) {
        Plugin gp = context.getPluginMap().get(pluginName);
        if (gp != null) {
            gp.handleGMCPCallback(callback, data);
        }
    }

    public void sendData(String gmcpData) {
        mth.core.client.TelnetClientSession session = context.getTelnetSession();
        if (session != null) {
            int space = gmcpData.indexOf(' ');
            if (space > 0) {
                session.sendGMCP(gmcpData.substring(0, space), gmcpData.substring(space + 1));
            }
        }
    }

    private static class GMCPWatcher {
        final String plugin;
        final String callback;

        GMCPWatcher(String plugin, String callback) {
            this.plugin = plugin;
            this.callback = callback;
        }
    }
}
