package org.ncmud.mudwammer.responder.toast;

import android.sax.Element;

import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class ToastResponderParser {
    public static void registerListeners(
            Element root, Object obj, TriggerData current_trigger, TimerData current_timer) {
        Element toast = root.getChild(BasePluginParser.TAG_TOASTRESPONDER);
        toast.setStartElementListener(
                new ToastElementListener(obj, current_trigger, current_timer));
    }

    public static void saveToastResponderToXML(XmlSerializer out, ToastResponder r)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", BasePluginParser.TAG_TOASTRESPONDER);
        out.attribute("", BasePluginParser.ATTR_TOASTMESSAGE, r.getMessage());
        out.attribute(
                "", BasePluginParser.ATTR_TOASTDELAY, Integer.valueOf(r.getDelay()).toString());
        out.attribute("", BasePluginParser.ATTR_FIRETYPE, r.getFireType().getString());
        out.endTag("", BasePluginParser.TAG_TOASTRESPONDER);
    }
}
