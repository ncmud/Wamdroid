package com.offsetnull.bt.service;

import com.offsetnull.bt.responder.TriggerResponder;
import com.offsetnull.bt.responder.script.ScriptResponder;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.trigger.TriggerData;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class GMCPHandler {

    private static final int GMCP_PAYLOAD_SIZE = 5;

    private final GMCPContext context;

    public GMCPHandler(GMCPContext context) {
        this.context = context;
    }

    public void loadTriggers() {
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
                                String module = t.getPattern().substring(1, t.getPattern().length());
                                String name = p.getName();
                                context.getProcessor().addWatcher(module, name, callback);
                            }
                        }
                    }
                }
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
        byte bIAC = TC.IAC;
        byte bSB = TC.SB;
        byte bSE = TC.SE;
        byte bGMCP = TC.GMCP;
        int size = gmcpData.length() + GMCP_PAYLOAD_SIZE;
        ByteBuffer fub = ByteBuffer.allocate(size);
        fub.put(bIAC).put(bSB).put(bGMCP);
        try {
            fub.put(gmcpData.getBytes("ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        fub.put(bIAC).put(bSE);
        byte[] fubtmp = new byte[size];
        fub.rewind();
        fub.get(fubtmp);
        DataPumper pump = context.getPump();
        if (pump != null && pump.isConnected()) {
            pump.sendData(fubtmp);
        }
    }
}
