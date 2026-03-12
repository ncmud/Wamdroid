package org.ncmud.mudwammer.responder.script;

import android.sax.StartElementListener;

import org.ncmud.mudwammer.responder.TriggerResponder;
import org.ncmud.mudwammer.responder.TriggerResponder.FIRE_WHEN;
import org.ncmud.mudwammer.service.plugin.settings.BasePluginParser;
import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xml.sax.Attributes;

public class ScriptElementListener implements StartElementListener {

    // PluginSettings settings = null;
    TriggerData current_trigger = null;
    TimerData current_timer = null;
    Object selector = null;

    public ScriptElementListener(
            Object selector, TriggerData current_trigger, TimerData current_timer) {
        // this.settings = settings;
        this.current_trigger = current_trigger;
        this.selector = selector;
        this.current_timer = current_timer;
    }

    public void start(Attributes a) {
        ScriptResponder scr = new ScriptResponder();
        scr.setFunction(a.getValue("", BasePluginParser.ATTR_FUNCTION));

        String fireType = a.getValue("", BasePluginParser.ATTR_FIRETYPE);
        if (fireType == null) fireType = "";

        if (fireType.equals(TriggerResponder.FIRE_WINDOW_OPEN)) {
            scr.setFireType(TriggerResponder.FIRE_WHEN.WINDOW_OPEN);
        } else if (fireType.equals(TriggerResponder.FIRE_WINDOW_CLOSED)) {
            scr.setFireType(TriggerResponder.FIRE_WHEN.WINDOW_CLOSED);
        } else if (fireType.equals(TriggerResponder.FIRE_ALWAYS)) {
            scr.setFireType(TriggerResponder.FIRE_WHEN.WINDOW_BOTH);
        } else if (fireType.equals(TriggerResponder.FIRE_NEVER)) {
            scr.setFireType(FIRE_WHEN.WINDOW_NEVER);
        } else {
            scr.setFireType(TriggerResponder.FIRE_WHEN.WINDOW_BOTH);
        }

        if (selector instanceof TriggerData) {
            current_trigger.getResponders().add(scr);
        }

        if (selector instanceof TimerData) {
            current_timer.getResponders().add(scr);
        }
    }
}
