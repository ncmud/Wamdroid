package org.ncmud.mudwammer.trigger;

import android.sax.Element;

import org.ncmud.mudwammer.responder.TriggerResponder;
import org.ncmud.mudwammer.responder.ack.AckResponderParser;
import org.ncmud.mudwammer.responder.color.ColorActionParser;
import org.ncmud.mudwammer.responder.gag.GagActionParser;
import org.ncmud.mudwammer.responder.notification.NotificationResponderParser;
import org.ncmud.mudwammer.responder.replace.ReplaceParser;
import org.ncmud.mudwammer.responder.script.ScriptResponderParser;
import org.ncmud.mudwammer.responder.toast.ToastResponderParser;
import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.service.plugin.settings.PluginParser;
import org.ncmud.mudwammer.timer.TimerData;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public final class TriggerParser {
    public static void registerListeners(
            Element root,
            PluginParser.NewItemCallback callback,
            Object obj,
            TriggerData current_trigger,
            TimerData current_timer) {
        // Element triggers = root.getChild("triggers");
        Element trigger = root.getChild(BasePluginParser.TAG_TRIGGER);
        TriggerElementListener listener = new TriggerElementListener(callback, current_trigger);

        trigger.setElementListener(listener);
        // trigger.sete

        AckResponderParser.registerListeners(trigger, obj, current_timer, current_trigger);
        ToastResponderParser.registerListeners(trigger, obj, current_trigger, current_timer);
        NotificationResponderParser.registerListeners(trigger, obj, current_trigger, current_timer);
        ScriptResponderParser.registerListeners(trigger, obj, current_trigger, current_timer);
        ReplaceParser.registerListeners(trigger, current_trigger);
        ColorActionParser.registerListeners(trigger, current_trigger);
        GagActionParser.registerListeners(trigger, current_trigger);
    }

    public static void saveTriggerToXML(XmlSerializer out, TriggerData trigger)
            throws IllegalArgumentException, IllegalStateException, IOException {
        if (trigger.isSave()) {
            out.startTag("", BasePluginParser.TAG_TRIGGER);
            out.attribute("", BasePluginParser.ATTR_TRIGGERTITLE, trigger.getName());
            out.attribute("", BasePluginParser.ATTR_TRIGGERPATTERN, trigger.getPattern());
            if (trigger.isInterpretAsRegex()) {
                out.attribute("", "regexp", trigger.isInterpretAsRegex() ? "true" : "false");
            }
            if (trigger.isFireOnce()) {
                out.attribute(
                        "",
                        BasePluginParser.ATTR_TRIGGERONCE,
                        trigger.isFireOnce() ? "true" : "false");
            }
            if (trigger.isHidden()) out.attribute("", BasePluginParser.ATTR_TRIGGERHIDDEN, "true");
            if (!trigger.isEnabled()) {
                out.attribute(
                        "",
                        BasePluginParser.ATTR_TRIGGERENEABLED,
                        trigger.isEnabled() ? "true" : "false");
            }
            if (trigger.getSequence() != TriggerData.DEFAULT_SEQUENCE) {
                out.attribute(
                        "",
                        BasePluginParser.ATTR_SEQUENCE,
                        Integer.toString(trigger.getSequence()));
            }
            if (!trigger.getGroup().equals(TriggerData.DEFAULT_GROUP))
                out.attribute("", BasePluginParser.ATTR_GROUP, trigger.getGroup());

            // if(trigger.isKeepEvaluating()) {
            // out.attribute("", BasePluginParser.ATTR_KEEPEVALUATING, trigger.isKeepEvaluating() ?
            // "true" : "false");
            // }

            for (TriggerResponder r : trigger.getResponders()) {
                r.saveResponderToXML(out);
            }
            // OutputResponders(out,trigger.getResponders());
            out.endTag("", BasePluginParser.TAG_TRIGGER);
        }
    }
}
