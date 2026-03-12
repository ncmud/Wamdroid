package org.ncmud.mudwammer.responder.script;

import android.sax.Element;

import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class ScriptResponderParser {
    public static void registerListeners(
            Element root, Object obj, TriggerData current_trigger, TimerData current_timer) {
        Element script = root.getChild(BasePluginParser.TAG_SCRIPTRESPONDER);
        script.setStartElementListener(
                new ScriptElementListener(obj, current_trigger, current_timer));
    }

    public static void saveScriptResponderToXML(XmlSerializer out, ScriptResponder r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_SCRIPTRESPONDER);
        out.attribute("", BasePluginParser.ATTR_FUNCTION, r.getFunction());
        out.attribute("", BasePluginParser.ATTR_FIRETYPE, r.getFireType().getString());
        out.endTag("", BasePluginParser.TAG_SCRIPTRESPONDER);
    }
}
