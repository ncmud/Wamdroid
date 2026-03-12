package org.ncmud.mudwammer.timer;

import android.sax.Element;

import org.ncmud.mudwammer.responder.TriggerResponder;
import org.ncmud.mudwammer.responder.ack.AckResponderParser;
import org.ncmud.mudwammer.responder.notification.NotificationResponderParser;
import org.ncmud.mudwammer.responder.script.ScriptResponderParser;
import org.ncmud.mudwammer.responder.toast.ToastResponderParser;
import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.service.plugin.settings.PluginParser;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class TimerParser {
    public static void registerListeners(
            Element root,
            PluginParser.NewItemCallback callback,
            Object obj,
            TriggerData current_trigger,
            TimerData current_timer) {
        Element timer = root.getChild(BasePluginParser.TAG_TIMER);
        // Element timer = timers.getChild(BasePluginParser.TAG_TIMER);
        timer.setElementListener(new TimerElementListener(callback, current_timer));

        AckResponderParser.registerListeners(timer, obj, current_timer, current_trigger);
        ToastResponderParser.registerListeners(timer, obj, current_trigger, current_timer);
        NotificationResponderParser.registerListeners(timer, obj, current_trigger, current_timer);
        ScriptResponderParser.registerListeners(timer, obj, current_trigger, current_timer);
    }

    public static void saveTimerToXML(XmlSerializer out, TimerData timer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        // not implemented yet. simple serialization routine.
        out.startTag("", BasePluginParser.TAG_TIMER);
        // out.startTag("", BasePluginParser.TAG_TIMER);
        out.attribute("", BasePluginParser.ATTR_TIMERNAME, timer.getName());
        // out.attribute("", BasePluginParser.ATTR_ORDINAL, timer.getOrdinal().toString());
        out.attribute("", BasePluginParser.ATTR_SECONDS, String.valueOf(timer.getSeconds()));
        out.attribute("", BasePluginParser.ATTR_REPEAT, (timer.isRepeat()) ? "true" : "false");
        out.attribute("", BasePluginParser.ATTR_PLAYING, (timer.isPlaying()) ? "true" : "false");
        for (TriggerResponder r : timer.getResponders()) {
            r.saveResponderToXML(out);
        }
        out.endTag("", BasePluginParser.TAG_TIMER);
    }
}
