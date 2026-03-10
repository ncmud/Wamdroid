package com.offsetnull.bt.service;

public interface WindowCallback {
    boolean isWindowShowing();

    void rawDataIncoming(byte[] raw);

    void resetWithRawDataIncoming(byte[] raw);

    void redraw();

    String getName();

    void shutdown();

    void xcallS(String function, String str);

    void xcallB(String function, byte[] raw);

    void clearText();

    void updateSetting(String key, String value);

    void setEncoding(String value);
}
