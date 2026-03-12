package org.ncmud.mudwammer.responder.gag;

import android.sax.Element;

import org.ncmud.mudwammer.responder.TriggerResponder.FIRE_WHEN;
import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class GagActionParser {
    public static void registerListeners(Element root, TriggerData current_trigger) {
        Element gag = root.getChild(BasePluginParser.TAG_GAGACTION);
        gag.setStartElementListener(new GagElementListener(current_trigger));
    }

    public static void saveGagActionToXML(XmlSerializer out, GagAction r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_GAGACTION);
        if (r.getRetarget() != null) {
            out.attribute("", BasePluginParser.ATTR_RETARGET, r.getRetarget());
        }

        if (r.isGagLog()) {

        } else {
            out.attribute("", BasePluginParser.ATTR_GAGLOG, "false");
        }

        if (r.isGagOutput()) {

        } else {
            out.attribute("", BasePluginParser.ATTR_GAGOUTPUT, "false");
        }
        if (r.getFireType() != FIRE_WHEN.WINDOW_BOTH) {
            out.attribute("", BasePluginParser.ATTR_FIRETYPE, r.getFireType().getString());
        }
        out.endTag("", BasePluginParser.TAG_GAGACTION);
    }
}
