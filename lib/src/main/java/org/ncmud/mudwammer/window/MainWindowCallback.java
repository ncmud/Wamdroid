package org.ncmud.mudwammer.window;

import android.app.Activity;

public interface MainWindowCallback {
    double getTitleBarHeight();

    double getStatusBarHeight();

    boolean isStatusBarHidden();

    String getPathForPlugin(String plugin);

    void dispatchLuaText(String text);

    Activity getActivity();

    boolean isPluginInstalled(String desired);

    boolean checkWindowSupports(String desired, String function);

    void windowCall(String window, String function, String data);

    void windowBroadcast(String function, String data);

    String getPluginOption(String plugin, String value);
}
