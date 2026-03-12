package org.ncmud.mudwammer.responder.color;

import android.sax.Element;

import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class ColorActionParser {
    public static void registerListeners(Element root, TriggerData current_trigger) {
        Element color = root.getChild(BasePluginParser.TAG_COLORACTION);
        color.setTextElementListener(new ColorElementListener(current_trigger));
    }

    public static void saveColorActionToXML(XmlSerializer out, ColorAction r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_COLORACTION);
        out.attribute("", "text", Integer.toString(r.getColor()));
        out.attribute("", "background", Integer.toString(r.getBackgroundColor()));
        // out.text(Integer.toString(r.getColor()));

        out.endTag("", BasePluginParser.TAG_COLORACTION);
    }
}
