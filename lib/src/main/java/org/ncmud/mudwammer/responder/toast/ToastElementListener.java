package org.ncmud.mudwammer.responder.toast;

import android.sax.StartElementListener;

import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;

import org.xml.sax.Attributes;

public class ToastElementListener implements StartElementListener {
    // PluginSettings settings = null;
    TriggerData current_trigger = null;
    TimerData current_timer = null;
    Object selector = null;

    public ToastElementListener(
            Object selector, TriggerData current_trigger, TimerData current_timer) {
        // this.settings = settings;
        this.current_trigger = current_trigger;
        this.selector = selector;
        this.current_timer = current_timer;
    }

    public void start(Attributes attributes) {
        // TODO Auto-generated method stub

    }
}
