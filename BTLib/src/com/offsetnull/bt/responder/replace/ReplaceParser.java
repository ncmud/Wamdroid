package com.offsetnull.bt.responder.replace;

import android.sax.Element;

import com.offsetnull.bt.service.plugin.settings.BasePluginParser;
import com.offsetnull.bt.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class ReplaceParser {
    public static void registerListeners(Element root, TriggerData current_trigger) {
        Element r = root.getChild(BasePluginParser.TAG_REPLACERESPONDER);
        r.setTextElementListener(new ReplaceElementListener(current_trigger));
    }

    public static void saveReplaceResponderToXML(XmlSerializer out, ReplaceResponder r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_REPLACERESPONDER);

        out.attribute("", BasePluginParser.ATTR_FIRETYPE, r.getFireType().getString());
        if (r.getRetarget() != null) {
            out.attribute("", BasePluginParser.ATTR_RETARGET, r.getRetarget());
            // out.attribute("", BasePluginParser.ATTR_DESTINATION, r.getWindowTarget());
        }
        out.text(r.getWith());

        out.endTag("", BasePluginParser.TAG_REPLACERESPONDER);
    }
}
