package com.offsetnull.bt.responder.ack;

import android.sax.Element;

import com.offsetnull.bt.service.plugin.settings.BasePluginParser;
import com.offsetnull.bt.timer.TimerData;
import com.offsetnull.bt.trigger.TriggerData;

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
