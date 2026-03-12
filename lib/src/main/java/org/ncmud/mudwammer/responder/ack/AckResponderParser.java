package org.ncmud.mudwammer.responder.ack;

import android.sax.Element;

import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class AckResponderParser {
    public static void registerListeners(
            Element root, Object obj, TimerData current_timer, TriggerData current_trigger) {
        Element ack = root.getChild(BasePluginParser.TAG_ACKRESPONDER);
        ack.setStartElementListener(
                new AckElementListener(new TriggerData(), current_trigger, current_timer));
    }

    public static void saveResponderToXML(XmlSerializer out, AckResponder r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_ACKRESPONDER);
        out.attribute("", BasePluginParser.ATTR_ACKWITH, r.getAckWith());
        out.attribute("", BasePluginParser.ATTR_FIRETYPE, r.getFireType().getString());
        out.endTag("", BasePluginParser.TAG_ACKRESPONDER);
    }
}
