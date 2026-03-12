/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import android.util.Log;

import com.offsetnull.bt.window.TextTree;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Manages window tokens, window callbacks, and window-related operations for a Connection. */
public class ConnectionWindowManager {

    private static final String MAIN_WINDOW = "mainDisplay";

    private final ArrayList<WindowToken> mWindows = new ArrayList<WindowToken>();
    private final HashMap<String, WindowCallback> mWindowCallbackMap =
            new HashMap<String, WindowCallback>();
    private final Object mWindowSynch = new Object();
    private final List<WindowCallback> mWindowCallbacks = new ArrayList<>();
    private boolean mCallbacksStarted = false;

    /** Gets the list of window tokens. */
    public ArrayList<WindowToken> getWindows() {
        return mWindows;
    }

    /** Gets the window callback map. */
    public HashMap<String, WindowCallback> getCallbackMap() {
        return mWindowCallbackMap;
    }

    /** Gets the synchronization object. */
    public Object getSynch() {
        return mWindowSynch;
    }

    /** Gets the window callbacks list. */
    public List<WindowCallback> getWindowCallbacks() {
        return mWindowCallbacks;
    }

    /** Returns whether callbacks have been started. */
    public boolean isCallbacksStarted() {
        return mCallbacksStarted;
    }

    /** Sets the callbacks started flag. */
    public void setCallbacksStarted(final boolean value) {
        mCallbacksStarted = value;
    }

    /** Returns the windows as an array, or null if empty check fails externally. */
    public WindowToken[] getWindowsArray() {
        WindowToken[] tmp = new WindowToken[mWindows.size()];
        tmp = mWindows.toArray(tmp);
        return tmp;
    }

    /**
     * Invalidate a target window's text.
     *
     * @param name Name of the window that should invalidate its text.
     */
    public void doInvalidateWindowText(final String name) {
        WindowCallback callback = mWindowCallbackMap.get(name);

        if (callback == null) {
            return;
        }

        WindowToken w = null;
        for (int i = 0; i < mWindows.size(); i++) {
            WindowToken tmp = mWindows.get(i);
            if (tmp.getName().equals(name)) {
                w = tmp;
            }
        }

        TextTree buffer = w.getBuffer();

        callback.resetWithRawDataIncoming(buffer.dumpToBytes(true));
    }

    /**
     * Work horse method for WindowXCallS Lua function.
     *
     * @param name Name of the target window.
     * @param function Name of the anonymous global function to call
     * @param o String argument to provide to function
     */
    public void windowXCallS(final String name, final String function, final Object o) {
        WindowCallback c = mWindowCallbackMap.get(name);

        if (c != null) {
            c.xcallS(function, (String) o);
        }
    }

    /**
     * Work horse method for WindowXCallB Lua function.
     *
     * @param name Name of the target window.
     * @param functions Name of the anonymous global function to call.
     * @param bytes Bytes to provide as an argument to function
     */
    public void windowXCallB(final String name, final String functions, final byte[] bytes) {
        WindowCallback c = mWindowCallbackMap.get(name);
        if (c != null) {
            c.xcallB(functions, bytes);
        }
    }

    /**
     * Actual working method for the LineToWindow Lua function.
     *
     * @param target Name of the window to receive the line.
     * @param line The TextTree.Line to send to target
     * @param encoding The character encoding to use.
     */
    public void lineToWindow(final String target, final Object line, final String encoding) {
        for (WindowToken w : mWindows) {
            if (w.getName().equals(target)) {
                TextTree tmp = new TextTree();
                tmp.setEncoding(encoding);
                if (line instanceof TextTree.Line) {
                    tmp.appendLine((TextTree.Line) line);
                } else if (line instanceof String) {
                    try {
                        tmp.addBytesImpl(((String) line).getBytes(encoding));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                tmp.updateMetrics();
                byte[] lol = tmp.dumpToBytes(false);

                WindowCallback c = mWindowCallbackMap.get(target);
                if (c != null) {
                    c.rawDataIncoming(lol);
                } else {
                    try {
                        w.getBuffer().addBytesImpl(lol);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * End of the line of the DrawWindow function.
     *
     * @param win Name of the window to redraw.
     */
    public void redrawWindow(final String win) {
        WindowCallback w = mWindowCallbackMap.get(win);
        if (w == null) {
            return;
        }
        w.redraw();
    }

    /**
     * Called from StellarService when the foreground window has started a new window and needs to
     * let the Connection know that a new window is open for it.
     *
     * @param name The name of the new window.
     * @param callback The WindowCallback associated with the window.
     */
    public void registerWindowCallback(final String name, final WindowCallback callback) {
        synchronized (mWindowSynch) {
            Log.e("LOG", "REGISTERING WINDOW " + name + " mCallbacksStarte=" + mCallbacksStarted);
            Log.e("LOG", "REGISTERING " + name);
            mWindowCallbacks.add(callback);

            mWindowCallbackMap.clear();
            for (WindowCallback w : mWindowCallbacks) {
                mWindowCallbackMap.put(w.getName(), w);
            }
            mCallbacksStarted = true;
        }
    }

    /**
     * Called from StellarService when the foreground window has stopped and destroyed a window and
     * needs to let the Connection know that the WindowCallback is invalid.
     *
     * @param callback The WindowCallback of the destroyed window.
     */
    public void unregisterWindowCallback(final WindowCallback callback) {
        synchronized (mWindowSynch) {
            Log.e("LOG", "UNREGISTERING WINDOW " + " mCallbacksStarted=" + mCallbacksStarted);
            Log.e("LOG", "UNREGISTERING " + callback.getName());
            mWindowCallbacks.remove(callback);

            mWindowCallbackMap.clear();
            for (WindowCallback w : mWindowCallbacks) {
                mWindowCallbackMap.put(w.getName(), w);
            }

            mCallbacksStarted = true;
        }
    }

    /**
     * Helper function to get a window by name.
     *
     * @param desired The name of the window to look up.
     * @return The WindowToken for the corresponding window name.
     */
    public WindowToken getWindowByName(final String desired) {
        for (int i = 0; i < mWindows.size(); i++) {
            WindowToken t = mWindows.get(i);
            if (t.getName().equals(desired)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Sends bytes to the main output window.
     *
     * @param data The bytes to send.
     */
    public void sendBytesToWindow(final byte[] data) {
        WindowCallback c = mWindowCallbackMap.get(MAIN_WINDOW);
        if (c != null) {
            c.rawDataIncoming(data);
        } else {
            // No callback registered yet — write directly to the buffer.
            for (WindowToken w : mWindows) {
                if (w.getName().equals(MAIN_WINDOW)) {
                    try {
                        w.getBuffer().addBytesImpl(data);
                    } catch (java.io.UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
