package com.offsetnull.bt.settings;

import android.annotation.SuppressLint;
import android.content.Context;

public class ConfigurationLoader {

    // Resource name is determined at runtime by callers, so getIdentifier is required.
    @SuppressLint("DiscouragedApi")
    public static String getConfigurationValue(String key, Context context) {
        int id = context.getResources().getIdentifier(key, "string", context.getPackageName());
        return context.getResources().getString(id);
    }

    // Resource is in the app module, not this library, so getIdentifier is required.
    @SuppressLint("DiscouragedApi")
    public static int getAboutDialogResource(Context context) {
        int id =
                context.getResources()
                        .getIdentifier("about_dialog", "layout", context.getPackageName());
        return id;
    }

    public static boolean isTestMode(Context context) {
        if (getConfigurationValue("testMode", context).equals("true")) {
            return true;
        } else {
            return false;
        }
    }
}
