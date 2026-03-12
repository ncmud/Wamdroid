package com.offsetnull.bt.service.plugin.settings;

import android.util.Log;

import com.offsetnull.bt.service.SettingsChangedListener;

import java.util.ArrayList;
import java.util.HashMap;

public class SettingsGroup extends Option {

    private HashMap<String, Option> optionsMap = new HashMap<String, Option>();
    private HashMap<String, SettingsChangedListener> listenerMap =
            new HashMap<String, SettingsChangedListener>();

    private ArrayList<Option> options;

    private SettingsChangedListener listener;
    private boolean skip = false;

    public SettingsGroup() {
        options = new ArrayList<Option>();
        type = TYPE.GROUP;
    }

    public ArrayList<Option> getOptions() {
        return options;
    }

    public void addOption(Option option) {
        updateOptionsMap(option, listener);
        options.add(option);
    }

    private void updateOptionsMap(Option option, SettingsChangedListener listener) {
        switch (option.type) {
            case GROUP:
                SettingsGroup sg = (SettingsGroup) option;
                if (sg.getListener() == null) {
                    sg.setListener(listener);
                }
                for (int i = 0; i < sg.getOptions().size(); i++) {
                    updateOptionsMap(sg.getOptions().get(i), sg.getListener());
                }
                break;
            default:
                optionsMap.put(option.getKey(), option);
                listenerMap.put(option.getKey(), listener);
                break;
        }
    }

    public Option findOptionByKey(String key) {
        return optionsMap.get(key);
    }

    public void updateBoolean(String key, boolean value) {
        BaseOption o = (BaseOption) optionsMap.get(key);
        if (o != null) {
            o.setValue(value);
            SettingsChangedListener tmp = listenerMap.get(key);
            if (tmp != null) {
                tmp.updateSetting(key, Boolean.toString(value));
            }
        } else {
            // type mismatch, don't do anything.
        }
    }

    public void updateInteger(String key, int value) {
        BaseOption o = (BaseOption) optionsMap.get(key);
        if (o != null) {
            o.setValue(value);
            SettingsChangedListener tmp = listenerMap.get(key);
            if (tmp != null) {
                tmp.updateSetting(key, Integer.toString(value));
            }
        } else {
            // type mismatch, don't do anything.
        }
    }

    public void updateFloat(String key, float value) {
        BaseOption o = (BaseOption) optionsMap.get(key);
        if (o != null) {
            o.setValue(value);
            SettingsChangedListener tmp = listenerMap.get(key);
            if (tmp != null) {
                tmp.updateSetting(key, Float.toString(value));
            }
        }
        // if(o instanceof FloatOption) {
        //	((IntegerOption) o).setValue(value);
        // } else {
        // type mismatch, don't do anything.
        // }

    }

    public void updateString(String key, String value) {
        BaseOption o = (BaseOption) optionsMap.get(key);
        if (o != null) {
            o.setValue(value);
            SettingsChangedListener tmp = listenerMap.get(key);
            if (tmp != null) {
                tmp.updateSetting(key, value);
            }
        }
    }

    // since all options sould take a string as a value, this is a "force update on setting with
    // string" functoin
    // that the xml parser will use.
    public void setOption(String key, String value) {
        BaseOption o = (BaseOption) optionsMap.get(key);
        if (o != null) {
            if (key.equals("color_option")) {
                Log.e("WINDOW", "WINDOW OPTION CHANGED TO: " + value);
            }
            o.setValue(value);
            if (key.equals("color_option")) {
                Log.e(
                        "WINDOW",
                        "WINDOW OPTION CHANGED TO: "
                                + value
                                + " actual:"
                                + o.getValue().toString());
            }
            SettingsChangedListener tmp = listenerMap.get(key);
            if (tmp != null) {
                tmp.updateSetting(key, value);
            }
        }
    }

    public String getOptionValue(String key) {
        BaseOption o = (BaseOption) optionsMap.get(key);

        if (o != null) {
            return o.getValue().toString();
        }

        return null;
    }

    public SettingsChangedListener getListener() {
        return listener;
    }

    public void setListener(SettingsChangedListener listener) {
        this.listener = listener;

        recursiveListenerUpdate(this);
    }

    private void recursiveListenerUpdate(SettingsGroup group) {
        listenerMap.clear();
        int size = group.getOptions().size();
        for (int i = 0; i < size; i++) {
            Option tmp = group.getOptions().get(i);
            if (tmp.type == TYPE.GROUP) {
                SettingsGroup sg = (SettingsGroup) tmp;
                sg.setListener(listener);
                recursiveListenerUpdate(sg);
            } else {
                listenerMap.put(tmp.getKey(), listener);
            }
        }
    }

    public void addOptionAt(Option option, int i) {
        updateOptionsMap(option, listener);
        options.add(i, option);
    }

    public void setSkipForPluginSave(boolean b) {
        skip = b;
    }

    public boolean getSkipForPluginSave() {
        return skip;
    }

    /*public interface BlandNewOption {

    }*/

}
