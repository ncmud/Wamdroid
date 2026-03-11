/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.offsetnull.bt.alias.AliasData;
import com.offsetnull.bt.button.SlickButtonData;
import com.offsetnull.bt.script.ScriptData;
import com.offsetnull.bt.service.function.BellCommand;
import com.offsetnull.bt.service.function.ClearButtonCommand;
import com.offsetnull.bt.service.function.ColorDebugCommand;
import com.offsetnull.bt.service.function.DirtyExitCommand;
import com.offsetnull.bt.service.function.DisconnectCommand;
import com.offsetnull.bt.service.function.FullScreenCommand;
import com.offsetnull.bt.service.function.FunctionCallbackCommand;
import com.offsetnull.bt.service.function.KeyboardCommand;
import com.offsetnull.bt.service.function.LoadButtonsCommand;
import com.offsetnull.bt.service.function.ReconnectCommand;
import com.offsetnull.bt.service.function.SpecialCommand;
import com.offsetnull.bt.service.function.SpeedwalkCommand;
import com.offsetnull.bt.service.function.SwitchWindowCommand;
import com.offsetnull.bt.service.plugin.ConnectionSettingsPlugin;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.service.plugin.settings.BaseOption;
import com.offsetnull.bt.service.plugin.settings.BooleanOption;
import com.offsetnull.bt.service.plugin.settings.ConnectionSetttingsParser;
import com.offsetnull.bt.service.plugin.settings.PluginParser;
import com.offsetnull.bt.service.plugin.settings.SettingsGroup;
import com.offsetnull.bt.service.plugin.settings.VersionProbeParser;
import com.offsetnull.bt.settings.ColorSetSettings;
import com.offsetnull.bt.settings.HyperSAXParser;
import com.offsetnull.bt.settings.HyperSettings;
import com.offsetnull.bt.speedwalk.DirectionData;
import com.offsetnull.bt.timer.TimerData;
import com.offsetnull.bt.trigger.TriggerData;
import com.offsetnull.bt.window.TextTree;

import org.keplerproject.luajava.LuaState;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import android.util.Log;
/** Connection class implementation. */
public class Connection
        implements SettingsChangedListener,
                ConnectionPluginCallback,
                TimerContext,
                GMCPContext,
                TriggerContext,
                AliasContext,
                SettingsContext,
                ConnectionHandle {

    /** Initiates the connection with the server. */
    public static final int MESSAGE_STARTUP = 1;

    /** Bridge between Processor and DataPumper to initiate MCCP compression. */
    public static final int MESSAGE_STARTCOMPRESS = 2;

    /** Sent by various objects to throw text to the window bypassing the trigger parse routine. */
    public static final int MESSAGE_PROCESSORWARNING = 3;

    /** Sent from the Processor to send data to the DataPumper's output thread. */
    public static final int MESSAGE_SENDOPTIONDATA = 4;

    /** Sent from the Processor indicating the bell character has been recieved. */
    public static final int MESSAGE_BELLINC = 5;

    /**
     * Not sure where this is sent from, I think the Xml parser or the datapumper. * Used to put up
     * an alert dialog with some text to the foreground window if connected.
     */
    public static final int MESSAGE_DODIALOG = 6;

    /** Sent from Processor, contains the non-telnet related data in the incoming transmission. */
    public static final int MESSAGE_PROCESS = 7;

    /** Sent from the DataPumper when the connection has been lost. */
    public static final int MESSAGE_DISCONNECTED = 8;

    /** Sent from the DataPumper indicating a fatal mccp error. */
    public static final int MESSAGE_MCCPFATALERROR = 9;

    /** Sent from the foreground window with data to be sent to the server. */
    public static final int MESSAGE_SENDDATA_BYTES = 9;

    /**
     * Sent from Plugin.LineToWindowFunction contains a * TextTree.Line object to send to a specific
     * window.
     */
    public static final int MESSAGE_LINETOWINDOW = 10;

    /** Sent from Plugin.NoteFunction with text to send to the output window. */
    public static final int MESSAGE_LUANOTE = 11;

    /** Sent from Plugin.DrawWindowFunction, I think this is no longer used and deprecated. */
    public static final int MESSAGE_DRAWINDOW = 12;

    /**
     * Sent from Plugin.NewWindowFucntion used to create a new miniwindow with then given
     * configuration.
     */
    public static final int MESSAGE_NEWWINDOW = 13;

    /**
     * Sent from Plugin.WindowBufferFunction and sets the buffering option of a window. I think this
     * is deprecated.
     */
    public static final int MESSAGE_WINDOWBUFFER = 15;

    /** Sent from Plugin.RegisterSpecialCommandFunction and registers a .command callback. */
    public static final int MESSAGE_ADDFUNCTIONCALLBACK = 16;

    /**
     * Sent from Plugin.WindowXCallSFunction, calls an anonymous global function in the target
     * window with data.
     */
    public static final int MESSAGE_WINDOWXCALLS = 17;

    /** Sent from plugin functions, used to redraw a target window. */
    public static final int MESSAGE_INVALIDATEWINDOWTEXT = 18;

    /** Sent from Processor indicating that gmcp has triggered. */
    public static final int MESSAGE_GMCPTRIGGERED = 19;

    /**
     * Sent from various sources, containing a string to be sent to the server in the selected
     * encoding.
     */
    public static final int MESSAGE_SENDDATA_STRING = 20;

    /** Sent from various sources, initates the settings loader routine. */
    public static final int MESSAGE_SAVESETTINGS = 21;

    /**
     * Sent from either the foreground window or the save settings routine, exports settings to a
     * given path.
     */
    public static final int MESSAGE_EXPORTFILE = 22;

    /**
     * Sent from either the foreground window or the settings loader routing, imports settings from
     * a given location into this active connection.
     */
    public static final int MESSAGE_IMPORTFILE = 23;

    /** Sent from Plugin.SendGMCPDataFunction, sends the given data to the server using gmcp. */
    public static final int MESSAGE_SENDGMCPDATA = 24;

    /**
     * Sent from Plugin.WindowXCallB, same as WindowXCallS only great care is taken to preserve the
     * "bytes".
     */
    public static final int MESSAGE_WINDOWXCALLB = 25;

    /** Sent from Plugin, indicating an error condition from a script entry point. */
    public static final int MESSAGE_PLUGINLUAERROR = 26;

    /** Sent from DataPumper indicating that the tcp connection to the server has started. */
    public static final int MESSAGE_CONNECTED = 30;

    /** Sent from AckWithResponder when a trigger executes script code that results in error. */
    public static final int MESSAGE_TRIGGER_LUA_ERROR = 32;

    /** Sent from the foreground window, initiates the settings reloading process. */
    public static final int MESSAGE_RELOADSETTINGS = 33;

    /**
     * Sent from various sources, indicates that triggers need to be rebuilt, I believe this is
     * deprecated.
     */
    public static final int MESSAGE_SETTRIGGERSDIRTY = 34;

    /** Sent from the DataPumper indicating the orderly shutdown of the tcp connection. */
    public static final int MESSAGE_TERMINATED_BY_PEER = 41;

    /**
     * Sent from the foreground window indicating that the DataPumper should re-establish the tcp
     * connection to the server.
     */
    static final int MESSAGE_RECONNECT = 31;

    /** Sent from the foreground window, initates a settings reset. */
    static final int MESSAGE_DORESETSETTINGS = 27;

    /** Sent from the foreground window, adds an external plugin at the given path. */
    static final int MESSAGE_ADDLINK = 28;

    /** Sent from the foreground window, deletes and removes a plugin. */
    static final int MESSAGE_DELETEPLUGIN = 29;

    /**
     * Sent from Plugin.CallPlugin calls an anonymous global function in the target plugin with
     * arguments.
     */
    static final int MESSAGE_CALLPLUGIN = 35;

    /** Sent from the timer command. */
    static final int MESSAGE_TIMERINFO = 36;

    /** Sent from the timer command. */
    static final int MESSAGE_TIMERSTART = 37;

    /** Sent from the timer command. */
    static final int MESSAGE_TIMERPAUSE = 38;

    /** Sent from the timer command. */
    static final int MESSAGE_TIMERRESET = 39;

    /** Sent from the timer command. */
    static final int MESSAGE_TIMERSTOP = 40;

    /** Server negotiated WILL ECHO — disable client local echo. */
    static final int MESSAGE_DISABLE_LOCAL_ECHO = 42;

    /** Server negotiated WONT ECHO — re-enable client local echo. */
    static final int MESSAGE_ENABLE_LOCAL_ECHO = 43;

    /** The value of 4. */
    private static final int FOUR = 4;

    /** 500 ms timeout, generic timeout or other. */
    private static final int FIVE_HUNDRED_MILLIS = 500;

    /** Very large value. */
    private static final int TEN_MILLION = 10000000;

    /** Medium large value. */
    private static final int TEN_THOUSAND = 10000;

    /** 3 seconds. */
    private static final int THREE_THOUSAND_MILLIS = 3000;

    /** 20 seconds. */
    private static final int TWENTY_THOUSAND_MILLIS = 20000;

    /** Minimum starting font size for the fit routine. */
    private static final float MIN_FONT_SIZE = 8.0f;

    /** Target char width for fit routine. */
    private static final float TARGET_FIT_WIDTH = 80.0f;

    /** Status bar height holder. */
    private static final int STATUS_BAR_DEFAULT_SIZE = 25;

    /** Value of -3. */
    private static final int NEGATIVE_THREE = -3;

    /** Value of -2. */
    private static final int NEGATIVE_TWO = -2;

    TriggerManager mTriggerManager;
    AliasManager mAliasManager;
    private SettingsPersistence mSettingsPersistence;

    /** String name of the default output window. */
    private static final String MAIN_WINDOW = "mainDisplay";

    /** Manages window tokens, callbacks, and window-related operations. */
    ConnectionWindowManager mWindowManager;

    /** The auto reconnect limit helper varialbe. */
    private Integer mAutoReconnectLimit;

    /** The current auto reconnect attempt. */
    Integer mAutoReconnectAttempt = 0;

    /** Weather or not we should auto reconnect on connection failure. */
    private Boolean mAutoReconnect;

    /** Coroutine-based event loop replacing the legacy Handler dispatch. */
    ConnectionEventLoop mEventLoop;

    /** Backward-compatible Handler shim for callers still sending Message objects. */
    private Handler mHandlerShim = null;

    /** Global handler for the speedwalk command, useful for changing the settings. */
    private SpeedwalkCommand mSpeedwalkCommand = null;

    /** Main tracker for plugins, generic ordered list of plugins in the order they were loaded. */
    ArrayList<Plugin> mPlugins = null;

    /** Global map for handling the capture transformation for triggers and aliases. */
    private HashMap<String, String> mCaptureMap = new HashMap<String, String>();

    /** The DataPumper instance for this connection. */
    DataPumper mPump = null;

    /** The Processor instance for this connection. */
    private Processor mProcessor = null;

    // TextTree buffer = null;

    /** Mapping of link paths to plugin names. */
    private HashMap<String, ArrayList<String>> mLinkMap = new HashMap<String, ArrayList<String>>();

    /** Mapping of plugin names to plugin objects. */
    private HashMap<String, Plugin> mPluginMap = new HashMap<String, Plugin>(0);

    /** Not really sure what this is. */
    private boolean mLoaded = false;

    /** Launcher display name for this Connection. */
    String mDisplay;

    /** Host name for this connection. */
    String mHost;

    /** Port indication for this connection. */
    int mPort;

    /** Instance of our parent service. This is bad. */
    StellarService mService = null;

    /** A simple holder for if we are connected or not. */
    boolean mIsConnected = false;

    /** The main settings wad/plugin. */
    ConnectionSettingsPlugin mSettings = null;

    TimerManager mTimerManager;
    GMCPHandler mGMCPHandler;

    /** The keyboard command instance, not sure why this is here. */
    private KeyboardCommand mKeyboardCommand;

    private ConnectionDispatcher mDispatcher;

    /** Cancellable job for delayed reconnect. */
    private kotlinx.coroutines.Job mReconnectJob = null;

    /** Value of CRLF. */
    private String mCRLF = "\r\n";

    /** Constant for the status bar height, useful for plugins, hard to get. */
    private int mStatusBarHeight;

    /** Constant for the title bar height, useful for plugins, hard to get. */
    private int mTitleBarHeight;

    /**
     * Public constructor for the Connection.
     *
     * @param display The display name.
     * @param host The host name.
     * @param port The port number.
     * @param service Parent that initated this connection.
     */
    public Connection(
            final String display, final String host, final int port, final StellarService service) {

        ColorDebugCommand colordebug = new ColorDebugCommand();
        DirtyExitCommand dirtyexit = new DirtyExitCommand();
        mTimerManager = new TimerManager(this);
        mGMCPHandler = new GMCPHandler(this);
        TimerManager.TimerCommand timercmd = new TimerManager.TimerCommand();
        BellCommand bellcmd = new BellCommand();
        FullScreenCommand fscmd = new FullScreenCommand();
        mKeyboardCommand = new KeyboardCommand();
        DisconnectCommand dccmd = new DisconnectCommand();
        ReconnectCommand rccmd = new ReconnectCommand();
        mAliasManager = new AliasManager(this);
        mSpeedwalkCommand = new SpeedwalkCommand(null, new AliasManager.Data());
        LoadButtonsCommand lbcmd = new LoadButtonsCommand();
        ClearButtonCommand cbcmd = new ClearButtonCommand();
        HashMap<String, SpecialCommand> specialCommands = mAliasManager.getSpecialCommands();
        specialCommands.put(colordebug.commandName, colordebug);
        specialCommands.put(dirtyexit.commandName, dirtyexit);
        specialCommands.put(timercmd.commandName, timercmd);
        specialCommands.put(bellcmd.commandName, bellcmd);
        specialCommands.put(fscmd.commandName, fscmd);
        specialCommands.put(mKeyboardCommand.commandName, mKeyboardCommand);
        specialCommands.put("kb", mKeyboardCommand);
        specialCommands.put(dccmd.commandName, dccmd);
        specialCommands.put(rccmd.commandName, rccmd);
        specialCommands.put(mSpeedwalkCommand.commandName, mSpeedwalkCommand);
        specialCommands.put(lbcmd.commandName, lbcmd);
        specialCommands.put(cbcmd.commandName, cbcmd);
        SwitchWindowCommand swdcmd = new SwitchWindowCommand();
        specialCommands.put(swdcmd.commandName, swdcmd);

        this.mDisplay = display;
        this.mHost = host;
        this.mPort = port;
        this.mService = service;

        mPlugins = new ArrayList<Plugin>();
        mTriggerManager = new TriggerManager(this);

        mWindowManager = new ConnectionWindowManager();
        mSettingsPersistence = new SettingsPersistence(this);

        mDispatcher = new ConnectionDispatcher(
                new TriggerManagerAdapter(this),
                new PumpAdapter(this),
                new BellCallbacksAdapter(this),
                new DisplayAdapter(this),
                new LifecycleAdapter(this),
                new WindowManagerAdapter(this),
                new PluginManagerAdapter(this),
                new TimerManagerAdapter(this),
                new GmcpAdapter(this),
                new AliasManagerAdapter(this),
                "UTF-8");

        mEventLoop = new ConnectionEventLoop(mDispatcher);
        mEventLoop.start();

        final Connection self = this;
        mHandlerShim = new ConnectionHandlerShim(
                mEventLoop,
                bytes -> self.sendToServer(bytes),
                str -> {
                    try {
                        self.sendToServer(str.getBytes(self.mSettings.getEncoding()));
                    } catch (java.io.UnsupportedEncodingException ignored) { }
                },
                () -> self.mPump != null && self.mPump.isConnected());

        SharedPreferences sprefs = this.getContext().getSharedPreferences("STATUS_BAR_HEIGHT", 0);
        mStatusBarHeight =
                sprefs.getInt(
                        "STATUS_BAR_HEIGHT",
                        (int)
                                (STATUS_BAR_DEFAULT_SIZE
                                        * this.getContext()
                                                .getResources()
                                                .getDisplayMetrics()
                                                .density));
        mTitleBarHeight = sprefs.getInt("TITLE_BAR_HEIGHT", 0);

        mLoaded = true;

        // fish out the window.

    }

    /**
     * Quick frontend for dispatchNoProcess(...) for sending a lua error message.
     *
     * @param message The message to show.
     */
    protected final void dispatchLuaError(final String message) {
        try {
            dispatchNoProcess(message.getBytes(mSettings.getEncoding()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves dirty plugins or the main settings wad.
     *
     * @param changedplugin The name of the plugin, "" will save the main settings.
     * @note This doesn't work as far as I know.
     */
    protected final void saveDirtyPlugin(final String changedplugin) {
        if (changedplugin.equals("")) {
            saveMainSettings();
        } else {
            Plugin p = mPluginMap.get(changedplugin);
            if (p != null) {
                if (p.getStorageType().equals("INTERNAL")) {
                    saveMainSettings();
                } else {
                    if (p.getSettings().isDirty()) {
                        saveMainSettings(); // ugly, need to be able to save plugins individually.
                    }
                }
            }
        }
    }

    /**
     * Work horse method for plugins to invalidate a target window's text.
     *
     * @param name Name of the window that should invalidate it's text.
     */
    protected final void doInvalidateWindowText(final String name) {
        mWindowManager.doInvalidateWindowText(name);
    }

    /**
     * Work horse method for WindowXCallS Lua function.
     *
     * @param name Name of the target window.
     * @param function Name of the anonymous global function to call
     * @param o String argument to provide to @param function
     */
    public final void windowXCallS(final String name, final String function, final Object o) {
        mWindowManager.windowXCallS(name, function, o);
    }

    /**
     * Work horse method for WindowXCallB Lua function.
     *
     * @param name Name of the target window.
     * @param functions Name of the anonymous global function to call.
     * @param bytes Bytes to provide as an argument to @param function
     */
    protected final void windowXCallB(
            final String name, final String functions, final byte[] bytes) {
        mWindowManager.windowXCallB(name, functions, bytes);
    }

    /**
     * Work horse method for the CallPlugin Lua function.
     *
     * @param plugin Name of the plugin to call.
     * @param function Name of the anonymous global function to call.
     * @param data String argument to provide to @param function.
     */
    void doCallPlugin(final String plugin, final String function, final String data) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.callFunction(function, data);
        } else {
            this.dispatchLuaText(
                    "\n"
                            + Colorizer.getRedColor()
                            + "No plugin named: "
                            + plugin
                            + Colorizer.getRedColor()
                            + "\n");
        }
    }

    /** Calling this method will reload the connection settings and all plugins. */
    public final void reloadSettings() {

        for (WindowCallback c : mWindowManager.getCallbackMap().values()) {
            c.shutdown();
        }

        mWindowManager.getCallbackMap().clear();
        mService.markWindowsDirty();
        loadInternalSettings();
    }

    /** Shuts down all running plugins and clears associated structures. */
    private void shutdownPlugins() {
        for (Plugin p : mPlugins) {
            p.shutdown();
            p = null;
        }
        mPlugins.clear();
    }

    /**
     * Loads plugins and sets up internal data structures.
     *
     * @param tmpPlugs The array of already loaded plugins.
     * @param summary A holder string fo what happened during the internal loading process.
     */
    private void loadPlugins(final ArrayList<Plugin> tmpPlugs, final String summary) {

        HashMap<String, TextTree> bufferSaves = new HashMap<String, TextTree>();

        TextTree buffer = null;
        if (mWindowManager.getWindows().size() > 0) {
            buffer = mWindowManager.getWindows().get(0).getBuffer();
            while (mWindowManager.getWindows().size() > 0) {
                WindowToken t =
                        mWindowManager.getWindows().remove(mWindowManager.getWindows().size() - 1);
                bufferSaves.put(t.getName(), t.getBuffer());
            }
        }
        if (mSettings != null) {
            mSettings.shutdown();
        }
        mSettings = null;

        mSettings = (ConnectionSettingsPlugin) tmpPlugs.get(0);
        mSettings.sortTriggers();
        mSettings.initTimers();
        for (WindowToken tmpw : mSettings.getSettings().getWindows().values()) {
            tmpw.setDisplayHost(mDisplay);
        }

        mWindowManager.getWindows().add(0, mSettings.getSettings().getWindows().get(MAIN_WINDOW));
        if (buffer == null) {
            buffer = mWindowManager.getWindows().get(0).getBuffer();
        } else {
            buffer.addString("\n\n");
        }

        buffer.addString(summary);
        tmpPlugs.remove(0);

        mPluginMap.clear();
        mLinkMap.clear();

        mPlugins.addAll(tmpPlugs);

        for (Plugin p : mPlugins) {
            for (WindowToken tmpw : p.getSettings().getWindows().values()) {
                tmpw.setDisplayHost(mDisplay);
            }
            p.initTimers();
            mPluginMap.put(p.getName(), p);
            p.sortTriggers();
            if (p.getSettings().getWindows().size() > 0) {
                mWindowManager.getWindows().addAll(p.getSettings().getWindows().values());
            }

            p.pushOptionsToLua();
        }

        if (mSettings.getDirections().size() == 0) {
            HashMap<String, DirectionData> tmp = new HashMap<String, DirectionData>();
            tmp.put("n", new DirectionData("n", "n"));
            tmp.put("e", new DirectionData("e", "e"));
            tmp.put("s", new DirectionData("s", "s"));
            tmp.put("w", new DirectionData("w", "w"));
            tmp.put("h", new DirectionData("h", "nw"));
            tmp.put("j", new DirectionData("j", "ne"));
            tmp.put("k", new DirectionData("k", "sw"));
            tmp.put("l", new DirectionData("l", "se"));
            mSettings.setDirections(tmp);
            mSpeedwalkCommand.setDirections(tmp);
        } else {
            mSpeedwalkCommand.setDirections(mSettings.getDirections());
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            for (String link : mSettings.getLinks()) {
                buffer.addString(Colorizer.getWhiteColor() + "Loading plugin file: " + link);
                String filename = Environment.getExternalStorageDirectory() + "/BlowTorch/" + link;
                ArrayList<Plugin> tmplist = new ArrayList<Plugin>();
                PluginParser parse =
                        new PluginParser(
                                filename,
                                link,
                                mService.getApplicationContext(),
                                tmplist,
                                mHandlerShim,
                                this);

                try {
                    ArrayList<Plugin> group = parse.load();
                    for (Plugin p : group) {
                        mPluginMap.put(p.getName(), p);
                        if (mLinkMap.get(link) == null) {
                            ArrayList<String> vals = new ArrayList<String>();
                            vals.add(p.getName());
                            mLinkMap.put(link, vals);
                        } else {
                            ArrayList<String> vals = mLinkMap.get(link);
                            vals.add(p.getName());
                        }

                        for (WindowToken tmpw : p.getSettings().getWindows().values()) {
                            tmpw.setDisplayHost(mDisplay);
                        }

                        if (p.getSettings().getWindows().size() > 0) {
                            mWindowManager
                                    .getWindows()
                                    .addAll(p.getSettings().getWindows().values());
                        }

                        p.pushOptionsToLua();
                    }

                    mPlugins.addAll(group);

                    buffer.addString(
                            Colorizer.getWhiteColor()
                                    + ", success."
                                    + Colorizer.getWhiteColor()
                                    + "\n");
                } catch (FileNotFoundException e) {
                    buffer.addString(
                            Colorizer.getRedColor()
                                    + " file not found."
                                    + Colorizer.getWhiteColor()
                                    + "\n");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    buffer.addString(
                            Colorizer.getRedColor()
                                    + " XML Parse error.\n"
                                    + e.getLocalizedMessage()
                                    + Colorizer.getWhiteColor()
                                    + "\n");
                }
            }
        }

        // so now that we have all the plugins, we need to build up the processor's
        // gmcpTriggerTables.
        // loop through all the plugins, looking for literal triggers starting
        // with the gmcpTriggerChar.

        if (bufferSaves != null) {
            for (WindowToken w : mWindowManager.getWindows()) {
                if (w != null) {

                    if (bufferSaves.get(w.getName()) != null) {
                        w.setBuffer(bufferSaves.get(w.getName()));
                    }
                }
            }
        }

        buildSettingsPage();
        mService.reloadWindows();
    }

    /** Delegates to TriggerManager. */
    public final void buildTriggerSystem() {
        mTriggerManager.buildTriggerSystem();
    }

    /**
     * end of the line of the DrawWindow function. I don't think this is used.
     *
     * @param win Name of the window to redraw.
     */
    protected final void redrawWindow(final String win) {
        mWindowManager.redrawWindow(win);
    }

    /**
     * Actual working method for the LineToWindow Lua function.
     *
     * @param target Name of the window to recieve the line.
     * @param line The TextTree.Line to send to @param target
     */
    protected final void lineToWindow(final String target, final Object line) {
        mWindowManager.lineToWindow(target, line, mSettings.getEncoding());
    }

    /**
     * Called from StellarService when the foreground window has started a new window and needs to
     * let the Connection know that a new window is open for it.
     *
     * @param name The name of the new window.
     * @param callback The WindowCallback associated with the window.
     */
    public final void registerWindowCallback(final String name, final WindowCallback callback) {
        mWindowManager.registerWindowCallback(name, callback);
    }

    /**
     * Called from StellarService when the foreground window has stopped and destroyed a window and
     * needs to let the Connection know that the WindowCallback is invalid.
     *
     * @param callback The WindowCallback of the destroyed window.
     */
    public final void unregisterWindowCallback(final WindowCallback callback) {
        mWindowManager.unregisterWindowCallback(callback);
    }

    /**
     * Called from the DataPumper when the net threads have been shut down.
     *
     * @param override Indicates weather the auto reconnect should be overridden.
     */
    protected final void doDisconnect(final boolean override) {
        if (mEventLoop == null) {
            return;
        }
        if (mAutoReconnect && !override) {
            if (mAutoReconnectAttempt < mAutoReconnectLimit) {
                mAutoReconnectAttempt++;
                String message =
                        "\n"
                                + Colorizer.getRedColor()
                                + "Network connection disconnected.\n"
                                + "Attmempting reconnect in 3 seconds. "
                                + (mAutoReconnectLimit - mAutoReconnectAttempt)
                                + " tries remaining."
                                + Colorizer.getWhiteColor()
                                + "\n";
                mEventLoop.send(new ConnectionCommand.ProcessorWarning(message));
                mReconnectJob = mEventLoop.sendDelayed(
                        ConnectionCommand.Reconnect.INSTANCE, THREE_THOUSAND_MILLIS);
                return;
            }
        }

        mService.doDisconnect(this);
    }

    /**
     * Called from various sources to kill the DataPumper and all of its threads.
     *
     * @param noreconnect true if there should be no reconnect attempt made.
     */
    protected final void killNetThreads(final boolean noreconnect) {

        if (mPump == null) {
            return;
        }

        mPump.shutdown();

        mProcessor = null;

        if (noreconnect) {
            if (mReconnectJob != null) {
                mReconnectJob.cancel(null);
                mReconnectJob = null;
            }
        }

        mPump = null;
    }

    /**
     * Sends a byte array to the default output window. Does not invoke trigger processing.
     *
     * @param data The data to send.
     */
    public void dispatchNoProcess(final byte[] data) {
        try {
            mWindowManager.getWindows().get(0).getBuffer().addBytesImpl(data);
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sendBytesToWindow(data);
    }

    /** Setter for triggersDirty. */
    public final void setTriggersDirty() {
        mTriggerManager.setDirty();
    }

    final void addFunctionCallbackImpl(
            final String id, final String command, final String callback) {
        int pid = -1;
        for (int i = 0; i < mPlugins.size(); i++) {
            Plugin p = mPlugins.get(i);
            if (p.getName().equals(id)) {
                pid = i;
            }
        }
        if (pid != -1) {
            FunctionCallbackCommand fcc =
                    new FunctionCallbackCommand(pid, command, callback);
            mAliasManager.getSpecialCommands().put(fcc.commandName, fcc);
        }
    }

    /**
     * Called from a few places I think. Triggers the network disconnected dialog in the foreground
     * window. Unless the auto reconnect is set.
     *
     * @param str The message fro the dialog.
     */
    protected final void dispatchDialog(final String str) {
        if (mEventLoop == null || str == null) {
            return;
        }
        if (mAutoReconnect) {
            if (mAutoReconnectAttempt < mAutoReconnectLimit) {
                mAutoReconnectAttempt++;
                killNetThreads(true);
                String message =
                        "\n"
                                + Colorizer.getRedColor()
                                + "Network Error: "
                                + str
                                + "\n"
                                + "Attmempting reconnect in 20 seconds. "
                                + (mAutoReconnectLimit - mAutoReconnectAttempt)
                                + " tries remaining."
                                + Colorizer.getWhiteColor()
                                + "\n";
                mEventLoop.send(new ConnectionCommand.ProcessorWarning(message));
                mReconnectJob = mEventLoop.sendDelayed(
                        ConnectionCommand.Reconnect.INSTANCE, TWENTY_THOUSAND_MILLIS);
                return;
            }
        }
        mService.dispatchDialog(str);
    }

    /**
     * Sends a string to the main output window.
     *
     * @param message The string to send.
     */
    public final void sendDataToWindow(final String message) {

        try {
            sendBytesToWindow(message.getBytes(mSettings.getEncoding()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends bytes to the main output window.
     *
     * @param data The bytes to send.
     */
    public final void sendBytesToWindow(final byte[] data) {
        mWindowManager.sendBytesToWindow(data);
    }

    @Override
    public void startup() {
        if (mPump == null) {
            doStartup();
        }
    }

    /** Meat of the startup sequence. Starts the net threads after the settings have been loaded. */
    private void doStartup() {

        killNetThreads(true);

        mPump = new DataPumper(mHost, mPort, mHandlerShim);

        mProcessor =
                new Processor(mHandlerShim, mSettings.getEncoding(), mService.getApplicationContext());

        initSettings();
        mPump.start();
        mGMCPHandler.loadTriggers();
        mIsConnected = true;

        mService.showConnectionNotification(mDisplay, mHost, mPort);
    }

    /**
     * Switches to another open connection.
     *
     * @param connection Name of the connection to switch to.
     */
    public final void switchTo(final String connection) {
        mService.switchTo(connection);
    }

    /**
     * Gets the current window token list in loaded order as an array.
     *
     * @return The array of window tokens in loaded order.
     */
    public final WindowToken[] getWindows() {
        if (mLoaded) {
            return mWindowManager.getWindowsArray();
        } else {
            return null;
        }
    }

    /**
     * Called from the foreground window. This method fetches a named script body from a plugin.
     *
     * @param plugin The plugin to look in.
     * @param name The name of the script to fetch.
     * @return The script body.
     */
    public final String getScript(final String plugin, final String name) {
        for (Plugin p : mPlugins) {
            if (p.getSettings().getName().equals(plugin)) {
                if (p.getSettings().getScripts().containsKey(name)) {
                    ScriptData d = p.getSettings().getScripts().get(name);
                    return d.getData();
                } else {
                    return "";
                }
            }
        }

        if (mSettings.getSettings().getScripts().containsKey(name)) {
            ScriptData d = mSettings.getSettings().getScripts().get(name);
            return d.getData();
        } else {
            return "";
        }
    }

    /**
     * Calls an anonymous global function in the target plugin with arguments.
     *
     * @param id The ID of the plugin to target.
     * @param callback The name of the desired function to execute.
     * @param args The data to supply to @param callback.
     */
    public final void executeFunctionCallback(
            final int id, final String callback, final String args) {
        Plugin p = mPlugins.get(id);
        p.execute(callback, args);
    }

    /**
     * The reciever of the foreground window PluginXCallS Lua function.
     *
     * @param plugin The name of the plugin to look in.
     * @param function The name of the anonymous global function to call.
     * @param str The argument to pass to <b>function</b>.
     */
    public final void pluginXcallS(final String plugin, final String function, final String str) {
        for (Plugin p : mPlugins) {
            if (p.getName().equals(plugin)) {
                p.xcallS(function, str);
            }
        }
    }

    /**
     * Helper method for reverse mapping R.java constants from name to id.
     *
     * @param variableName The desired field name e.g. "alias_dialog".
     * @param context THe current application context to use.
     * @param c The class to search, this is usually R.layout or R.drawable.
     * @return The integer id of <b>variableName</b> or -1 if the class does not have a field named
     *     <b>variableName</b>.
     */
    public static int getResId(final String variableName, final Context context, final Class<?> c) {
        try {
            Field idField = c.getDeclaredField(variableName);
            return idField.getInt(null);
        } catch (NoSuchFieldException e) {
            Log.e("Connection", "No resource field: " + variableName + " in " + c.getName());
            return -1;
        } catch (IllegalAccessException e) {
            Log.e("Connection", "Cannot access field: " + variableName + " in " + c.getName());
            return -1;
        }
    }

    /**
     * Helper function to get a window by name.
     *
     * @param desired The name of the window to look up.
     * @return The WindowToken for the corresponding window name.
     */
    public final WindowToken getWindowByName(final String desired) {
        return mWindowManager.getWindowByName(desired);
    }

    /**
     * Helper function to get the triggers for the main conenction settings.
     *
     * @return the triggers for the main connection settings.
     */
    public final HashMap<String, TriggerData> getTriggers() {
        return mSettings.getSettings().getTriggers();
    }

    /**
     * Helper function to get the triggers for a given plugin.
     *
     * @param name The name of the plugin to interrogate.
     * @return The triggers of the given plugin, null if <b>name</b> does not correspond to a loaded
     *     plugin.
     */
    public final HashMap<String, TriggerData> getPluginTriggers(final String name) {
        Plugin p = mPluginMap.get(name);
        if (p != null) {
            return p.getSettings().getTriggers();
        } else {
            return null;
        }
    }

    /**
     * Adds a trigger into the main settings plugin.
     *
     * @param data The trigger to add.
     */
    public final void addTrigger(final TriggerData data) {
        mSettings.addTrigger(data);
    }

    /**
     * Updates a trigger in the main settings plugin.
     *
     * @param from Old trigger.
     * @param to New trigger.
     */
    public final void updateTrigger(final TriggerData from, final TriggerData to) {
        mSettings.updateTrigger(from, to);
    }

    /**
     * Updates a trigger in the target plugin.
     *
     * @param selectedPlugin Name of the plugin to work in.
     * @param from Old plugin.
     * @param to New plugin.
     */
    public final void updatePluginTrigger(
            final String selectedPlugin, final TriggerData from, final TriggerData to) {
        Plugin p = mPluginMap.get(selectedPlugin);
        if (p != null) {
            p.updateTrigger(from, to);
        }
    }

    /**
     * Adds a new trigger in the target plugin.
     *
     * @param selectedPlugin Target plugin for the new trigger.
     * @param data The new trigger.
     */
    public final void newPluginTrigger(final String selectedPlugin, final TriggerData data) {
        Plugin p = mPluginMap.get(selectedPlugin);
        if (p != null) {
            p.addTrigger(data);
        }
    }

    /**
     * Gets a trigger in the target plugin.
     *
     * @param selectedPlugin Name of the plugin to look in.
     * @param pattern Name of the desired trigger.
     * @return The trigger, <b>null</b> if it does not exist.
     */
    public final TriggerData getPluginTrigger(final String selectedPlugin, final String pattern) {
        Plugin p = mPluginMap.get(selectedPlugin);
        if (p != null) {
            return p.getSettings().getTriggers().get(pattern);
        } else {
            return null;
        }
    }

    /**
     * Gets a trigger from the main settings plugin.
     *
     * @param pattern Name of the trigger to get.
     * @return The trigger, <b>null</b> if it does not exist.
     */
    public final TriggerData getTrigger(final String pattern) {
        return mSettings.getSettings().getTriggers().get(pattern);
    }

    /**
     * Sets the enabled state of a trigger in the target plugin.
     *
     * @param selectedPlugin Name of the target plugin to affect.
     * @param enabled Desired state of the trigger.
     * @param key The name of the trigger to affect.
     */
    public final void setPluginTriggerEnabled(
            final String selectedPlugin, final boolean enabled, final String key) {
        Plugin p = mPluginMap.get(selectedPlugin);
        if (p != null) {
            TriggerData data = p.getSettings().getTriggers().get(key);
            if (data != null) {
                data.setEnabled(enabled);
                p.getSettings().setDirty(true);
                buildTriggerSystem();
            }
        }
    }

    /**
     * Sets the enabled state of a trigger in the main settings plugin.
     *
     * @param enabled Desired state of the target trigger.
     * @param key Name of the trigger to affect.
     */
    public final void setTriggerEnabled(final boolean enabled, final String key) {
        TriggerData data = mSettings.getSettings().getTriggers().get(key);
        if (data != null) {
            data.setEnabled(enabled);
            buildTriggerSystem();
        }
    }

    /**
     * Removes a trigger from the target plugin.
     *
     * @param selectedPlugin Name of the plugin to search in.
     * @param which Name of the trigger to remove.
     */
    public final void deletePluginTrigger(final String selectedPlugin, final String which) {
        Plugin p = mPluginMap.get(selectedPlugin);
        if (p != null) {
            p.getSettings().getTriggers().remove(which);
            p.getSettings().setDirty(true);
            p.sortTriggers();
        }
        buildTriggerSystem();
    }

    /**
     * Removes a trigger from the main settings plugin.
     *
     * @param which Name of the trigger to remove.
     */
    public final void deleteTrigger(final String which) {
        mSettings.getSettings().getTriggers().remove(which);
        mSettings.sortTriggers();
        buildTriggerSystem();
    }

    /**
     * Sets the aliases for the main settings plugin. This comes from the foreground window in one
     * glob.
     *
     * @param map The new alias map (HashMap<String, AliasData>).
     */
    public final void setAliases(final HashMap<String, AliasData> map) {
        mSettings.getSettings().setAliases(map);
        mSettings.buildAliases();
    }

    /**
     * Sets the aliases for a given plugin. This comes from the foreground window in one glob.
     *
     * @param plugin Name of the target plugin to affect.
     * @param map The new alias map (HashMap<String, AliasData>)
     */
    public final void setPluginAliases(final String plugin, final HashMap<String, AliasData> map) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.getSettings().setAliases(map);
            p.getSettings().setDirty(true);
            p.buildAliases();
        }
    }

    /**
     * Gets an alias for a target plugin.
     *
     * @param plugin Name of the plugin to search.
     * @param key The pre part of the alias.
     * @return The AliasData associated with <b>key</b>.
     */
    public final AliasData getPluginAlias(final String plugin, final String key) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            return p.getSettings().getAliases().get(key);
        }
        return null;
    }

    /**
     * Gets an alias from the main settings plugin.
     *
     * @param key The pre part of the alias.
     * @return The AliasData associated with <b>key</b>
     */
    public final AliasData getAlias(final String key) {
        return mSettings.getSettings().getAliases().get(key);
    }

    /**
     * Removes an alias form the main settings plugin.
     *
     * @param key The pre part of the alias to delete.
     */
    public final void deleteAlias(final String key) {
        mSettings.getSettings().getAliases().remove(key);
    }

    /**
     * Removes an alias from the target plugin.
     *
     * @param plugin The name of the plugin to affect.
     * @param key The pre part of the alias to remove.
     */
    public final void deletePluginAlias(final String plugin, final String key) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.getSettings().getAliases().remove(key);
        }
    }

    /**
     * Gets the alias map for the main settings plugin.
     *
     * @return The alais map for the main settings plugin.
     */
    public final HashMap<String, AliasData> getAliases() {
        return mSettings.getSettings().getAliases();
    }

    /**
     * Gets the alias map for a target plugin.
     *
     * @param plugin The desired plugin to interrogate.
     * @return The alias map for <b>plugin</b>.
     */
    public final HashMap<String, AliasData> getPluginAliases(final String plugin) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            return p.getSettings().getAliases();
        } else {
            return null;
        }
    }

    /**
     * Gets the list of all the installed system commands.
     *
     * @return The system command list.
     */
    public final ArrayList<String> getSystemCommands() {
        ArrayList<String> list = new ArrayList<String>();
        Set<String> keys = mAliasManager.getSpecialCommands().keySet();
        for (String key : keys) {
            list.add(key);
        }
        return list;
    }

    /**
     * Sets the enabled state of an alias in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param enabled Desired state of the alias.
     * @param key The pre part of the alias to affect.
     */
    public final void setPluginAliasEnabled(
            final String plugin, final boolean enabled, final String key) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            AliasData data = p.getSettings().getAliases().get(key);
            if (data != null) {
                data.setEnabled(enabled);
                p.getSettings().setDirty(true);
                p.buildAliases();
            }
        }
    }

    /**
     * Sets the enabled state of an alias in the main settings plugin.
     *
     * @param enabled Desired state of the alias.
     * @param key The pre part of the alias to affect.
     */
    public final void setAliasEnabled(final boolean enabled, final String key) {
        AliasData data = mSettings.getSettings().getAliases().get(key);
        if (data != null) {
            data.setEnabled(enabled);
            mSettings.buildAliases();
        }
    }

    /**
     * Helper function for the keyboard command. Does an alias replacment in a special kind of way.
     *
     * @param bytes Bytes to process.
     * @param reprocess Weather to do recursive alias replacement.
     * @return The processed command bytes.
     */
    public final byte[] doKeyboardAliasReplace(final byte[] bytes, final Boolean reprocess) {
        return mAliasManager.doKeyboardAliasReplace(bytes, reprocess);
    }

    /** Helper method that kicks off the reconnection sequence. */
    public final void startReconnect() {
        mEventLoop.send(ConnectionCommand.Reconnect.INSTANCE);
    }

    /** Helper method to initiate a reconnect right now. */
    public final void doReconnect() {
        if (mPump != null) {
            mPump.shutdown();
            mPump = null;
        }

        doStartup();
    }

    /**
     * Removes a timer from the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param name Name of the timer to remove.
     */
    public final void deletePluginTimer(final String plugin, final String name) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.getSettings().getTimers().remove(name);
        }
    }

    /**
     * Gets a timer from the main settings plugin.
     *
     * @param name Name of the timer to get.
     * @return The timer associated with <b>name</b>.
     */
    public final TimerData getTimer(final String name) {
        return mSettings.getSettings().getTimers().get(name);
    }

    /**
     * Removes a timer from the main settings plugin.
     *
     * @param name Name of the trigger to remove.
     */
    public final void deleteTimer(final String name) {
        mSettings.getSettings().getTimers().remove(name);
    }

    /**
     * Gets a timer from the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param name Name of the trigger to get.
     * @return The trigger associated with <b>name</b>.
     */
    public final TimerData getPluginTimer(final String plugin, final String name) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            return p.getSettings().getTimers().get(name);
        } else {
            return null;
        }
    }

    /**
     * Adds a timer to the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param newtimer New timer data.
     */
    public final void addPluginTimer(final String plugin, final TimerData newtimer) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            newtimer.setRemainingTime(newtimer.getSeconds());
            p.getSettings().getTimers().put(newtimer.getName(), newtimer);
            p.getSettings().setDirty(true);
        }
    }

    /**
     * Updates a timer in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param old Old timer data.
     * @param newtimer New timer data.
     */
    public final void updatePluginTimer(
            final String plugin, final TimerData old, final TimerData newtimer) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.getSettings().getTimers().remove(old.getName());
            p.getSettings().getTimers().put(newtimer.getName(), newtimer);
            p.getSettings().setDirty(true);
        }
    }

    /**
     * Updates a timer in the main settings plugin.
     *
     * @param old Old timer data.
     * @param newtimer New timer data.
     */
    public final void updateTimer(final TimerData old, final TimerData newtimer) {
        mSettings.getSettings().getTimers().remove(old.getName());
        mSettings.getSettings().getTimers().put(newtimer.getName(), newtimer);
    }

    /**
     * Gets the timer map for the main settings plugin.
     *
     * @return The timer map.
     */
    public final HashMap<String, TimerData> getTimers() {
        mSettings.updateTimerProgress();
        return mSettings.getSettings().getTimers();
    }

    /**
     * Gets the timer map for a target plugin.
     *
     * @param plugin Name of the target plugin.
     * @return The tier map.
     */
    public final HashMap<String, TimerData> getPluginTimers(final String plugin) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.updateTimerProgress();
            return p.getSettings().getTimers();
        } else {
            return null;
        }
    }

    /**
     * Adds a new timer into the main settings plugin.
     *
     * @param newtimer New timer to add.
     */
    public final void addTimer(final TimerData newtimer) {
        newtimer.setRemainingTime(newtimer.getSeconds());
        mSettings.getSettings().getTimers().put(newtimer.getName(), newtimer);
        mSettings.getSettings().setDirty(true);
    }

    /**
     * Helper method to see if the window is currently being shown.
     *
     * @return visibility state of the foreground window.
     */
    public final boolean isWindowShowing() {
        return mService.isWindowConnected();
    }

    /**
     * Getter method for mDisplay.
     *
     * @return the display name for this connection.
     */
    public final String getDisplayName() {
        return mDisplay;
    }

    /**
     * Helper method for getting the application context out here in the desert of the Service.
     *
     * @return The application context.
     */
    public final Context getContext() {
        return mService.getApplicationContext();
    }

    /**
     * Starts a timer in the main settings plugin with the target name.
     *
     * @param key Name of the timer to start.
     */
    public final void playTimer(final String key) {
        mSettings.startTimer(key);
    }

    /**
     * Starts a timer in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param timer Name of the timer to start.
     */
    public final void playPluginTimer(final String plugin, final String timer) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.startTimer(timer);
        }
    }

    /**
     * Pauses a timer in the main settings plugin.
     *
     * @param key Name of the plugin to pause.
     */
    public final void pauseTimer(final String key) {
        mSettings.pauseTimer(key);
    }

    /**
     * Pauses a timer in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param timer Name of the timer to pause.
     */
    public final void pausePluginTimer(final String plugin, final String timer) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.pauseTimer(timer);
        }
    }

    /**
     * Stops a timer in the main settings plugin.
     *
     * @param key Name of the timer to stop.
     */
    public final void stopTimer(final String key) {
        mSettings.stopTimer(key);
    }

    /**
     * Stops a timer in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param key Name of the timer to stop.
     */
    public final void stopPluginTimer(final String plugin, final String key) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.stopTimer(key);
        }
    }

    /**
     * Gets the settings object for the main settings plugin.
     *
     * @return The settings for the main settings plugin.
     */
    public final SettingsGroup getSettings() {
        if (mSettings == null) {
            return new SettingsGroup();
        }
        return mSettings.getSettings().getOptions();
    }

    /**
     * Gets the settings object for a target plugin.
     *
     * @param plugin Name of the target plugin.
     * @return The settings for the target plugin. Returns null if <b>name</b> is not a loaded
     *     plugin.
     */
    public final SettingsGroup getPluginSettings(final String plugin) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            return p.getSettings().getOptions();
        } else {
            return null;
        }
    }

    /**
     * Updates a boolean setting in the main settings plugin.
     *
     * @param key id of the setting to affect.
     * @param value new value for setting <b>key</b>
     */
    public final void updateBooleanSetting(final String key, final boolean value) {
        mSettings.updateBooleanSetting(key, value);
    }

    /**
     * Updates a boolean setting in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updatePluginBooleanSetting(
            final String plugin, final String key, final boolean value) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.updateBooleanSetting(key, value);
        }
    }

    /**
     * Updates a string setting in the main settings plugin.
     *
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updateStringSetting(final String key, final String value) {
        mSettings.updateStringSetting(key, value);
    }

    /**
     * Updates a string setting in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updatePluginStringSetting(
            final String plugin, final String key, final String value) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.updateStringSetting(key, value);
        }
    }

    /**
     * Udpates an integer setting in the main settings plugin.
     *
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updateIntegerSetting(final String key, final int value) {
        mSettings.updateIntegerSetting(key, value);
    }

    /**
     * Updates an integer setting in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updatePluginIntegerSetting(
            final String plugin, final String key, final int value) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.updateIntegerSetting(key, value);
        }
    }

    /**
     * Updates a float setting in the main settings plugin.
     *
     * @param key key id of the setting to update.
     * @param value the value to use.
     */
    public final void updateFloatSetting(final String key, final float value) {
        mSettings.updateFloatSetting(key, value);
    }

    /**
     * Updates a float setting in the target plugin.
     *
     * @param plugin Name of the target plugin.
     * @param key key id of the setting to affect.
     * @param value the value to use.
     */
    public final void updatePluginFloatSetting(
            final String plugin, final String key, final float value) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.updateFloatSetting(key, value);
        }
    }

    /** Utility class for tracking changes ot the main window settings. */
    private class WindowSettingsChangedListener implements SettingsChangedListener {
        /** Name of the window that this listener is watching. */
        private String mWindow;

        /**
         * Constructor.
         *
         * @param window Name of the window to watch.
         */
        public WindowSettingsChangedListener(final String window) {
            this.mWindow = window;
        }

        @Override
        public void updateSetting(final String key, final String value) {
            Connection.this.handleWindowSettingsChanged(mWindow, key, value);
        }
    }

    /**
     * Work horse of the main window settings change listener.
     *
     * @param window Name of the window that was affected.
     * @param key Name of the key that changed.
     * @param value The value that it was changed to.
     */
    public final void handleWindowSettingsChanged(
            final String window, final String key, final String value) {

        WindowCallback callback = mWindowManager.getCallbackMap().get(window);
        if (callback == null) {
            return;
        }
        callback.updateSetting(key, value);
    }

    @Override
    public final void updateSetting(final String key, final String value) {
        if (mSettings == null) {
            return; // this is for when the settings are first being loaded.
        }
        BaseOption o = (BaseOption) mSettings.getSettings().getOptions().findOptionByKey(key);
        try {
            KEYS tmp = KEYS.valueOf(key);
            switch (tmp) {
                case process_semicolon:
                    mSettings.setSemiIsNewLine((Boolean) o.getValue());
                    break;
                case debug_telnet:
                    if (mProcessor != null) {
                        mProcessor.setDebugTelnet((Boolean) o.getValue());
                    }
                    break;
                case encoding:
                    this.doUpdateEncoding((String) o.getValue());
                    break;
                case orientation:
                    mService.doExecuteSetOrientation((Integer) o.getValue());
                    break;
                case screen_on:
                    mService.doExecuteKeepScreenOn((Boolean) o.getValue());
                    break;
                case fullscreen:
                    mService.doExecuteFullscreen((Boolean) o.getValue());
                    break;
                case fullscreen_editor:
                    mService.doExecuteFullscreenEditor((Boolean) o.getValue());
                    break;
                case use_suggestions:
                    mService.doExecuteUseSuggestions((Boolean) o.getValue());
                    break;
                case keep_last:
                    mService.dispatchKeepLast((Boolean) o.getValue());
                    break;
                case compatibility_mode:
                    mService.doExecuteCompatibilityMode((Boolean) o.getValue());
                    break;
                case local_echo:
                    mSettings.setLocalEcho((Boolean) o.getValue());
                    break;
                case process_system_commands:
                    mSettings.setProcessPeriod((Boolean) o.getValue());
                    break;
                case echo_alias_updates:
                    mSettings.setEchoAliasUpdates((Boolean) o.getValue());
                    break;
                case keep_wifi_alive:
                    this.doSetKeepWifiAlive((Boolean) o.getValue());
                    break;
                case auto_reconnect:
                    mAutoReconnect = (Boolean) o.getValue();
                    break;
                case auto_reconnect_limit:
                    mAutoReconnectLimit = (Integer) o.getValue();
                    break;
                case cull_extraneous_color:
                    this.doSetCullExtraneousColor((Boolean) o.getValue());
                    break;
                case debug_telent:
                    this.doSetDebugTelnet((Boolean) o.getValue());
                    break;
                case bell_vibrate:
                    mSettings.setVibrateOnBell((Boolean) o.getValue());
                    break;
                case bell_notification:
                    mSettings.setNotifyOnBell((Boolean) o.getValue());
                    break;
                case bell_display:
                    mSettings.setDisplayOnBell((Boolean) o.getValue());
                    break;
                case show_regex_warning:
                    mService.dispatchShowRegexWarning((Boolean) o.getValue());
                    break;
                case use_gmcp:
                    mProcessor.setUseGMCP((Boolean) o.getValue());
                    break;
                case gmcp_supports:
                    mProcessor.setGMCPSupports((String) o.getValue());
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Impelemntation of the set debug telnet settings handler.
     *
     * @param value New value to use.
     */
    private void doSetDebugTelnet(final Boolean value) {
        mSettings.setDebugTelnet(value);
        if (mProcessor != null) {
            mProcessor.setDebugTelnet(value);
        }
    }

    /**
     * Impelemntation of the cull extraneous colors settings handler.
     *
     * @param value New value to use.
     */
    private void doSetCullExtraneousColor(final Boolean value) {
        mSettings.setRemoveExtraColor(value);
        mWindowManager.getWindows().get(0).getBuffer().setCullExtraneous(value);
    }

    /**
     * Impelemntation of the keep wifi alive settings handler.
     *
     * @param value New value to use.
     */
    private void doSetKeepWifiAlive(final Boolean value) {
        mSettings.setKeepWifiActive(value);
        if (value) {
            mService.enableWifiKeepAlive();
        } else {
            mService.disableWifiKeepAlive();
        }
    }

    /**
     * Impelemntation of the system encoding settings handler.
     *
     * @param value New value to use.
     */
    private void doUpdateEncoding(final String value) {
        if (mProcessor == null) {
            return;
        }
        mProcessor.setEncoding(value);
        // this.encoding = value;
        mSettings.setEncoding(value);
        mTriggerManager.setEncoding(value);
        if (mProcessor != null) {
            this.mProcessor.setEncoding(value);
        }
        for (int i = 0; i < mWindowManager.getWindows().size(); i++) {
            WindowToken w = mWindowManager.getWindows().get(i);
            w.getBuffer().setEncoding(value);
        }

        for (WindowCallback w : mWindowManager.getCallbackMap().values()) {
            w.setEncoding(value);
        }

        for (int i = 0; i < mPlugins.size(); i++) {
            Plugin p = mPlugins.get(i);
            p.setEncoding(value);
        }

        // handle the keyboard command callback.
        mKeyboardCommand.setEncoding(value);

        // may want to go through and activate the settings changed handler for plugins.
        // the chat window would want to re-construct it's buffers. But for proper operation
        // it may not be out of the question to make encoding change requrie a restart.
        // everything that doesn't use TextTree's directly to make multi-buffers, will work fine.
    }

    /** Helper enum to map the main settings plugin's settings keys to strings. */
    private enum KEYS {
        /** Semicolon processing. */
        process_semicolon,
        /** Debug telnet. */
        debug_telnet,
        /** System encoding. */
        encoding,
        /** Window orientation. */
        orientation,
        /** Keep screen on. */
        screen_on,
        /** Hide notification bar. */
        fullscreen,
        /** Use fullscreen editor. */
        fullscreen_editor,
        /** Make editor use suggestions. */
        use_suggestions,
        /** Keep last entered. */
        keep_last,
        /** Input compatibility mode. */
        compatibility_mode,
        /** Local echo. */
        local_echo,
        /** Process period commands. */
        process_system_commands,
        /** Echo alias updates. */
        echo_alias_updates,
        /** Keep wifi alive. */
        keep_wifi_alive,
        /** Cull extraneous color codes. */
        cull_extraneous_color,
        /** Debug telnet data. */
        debug_telent,
        /** Bell vibrates. */
        bell_vibrate,
        /** Bell notifies. */
        bell_notification,
        /** Bell toasts. */
        bell_display,
        /** Auto reconnect. */
        auto_reconnect,
        /** Auto reconnect limit. */
        auto_reconnect_limit,
        /** Use GMCP. */
        use_gmcp,
        /** GMCP Supports string. */
        gmcp_supports,
        /** Show Regex Warning. */
        show_regex_warning
    }

    /**
     * Work horse function of sending data to the server, this initiates all levels of processing.
     *
     * @param bytes Input to process.
     */
    private void sendToServer(final byte[] bytes) {
        if (bytes == null || mSettings == null) {
            return;
        }
        AliasManager.Data d = null;
        try {
            d = mAliasManager.processOutputData(new String(bytes, mSettings.getEncoding()), this);
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }

        if (d == null) {
            return;
        }

        if (d.mCmdString.equals("")
                && (d.mVisString != null && d.mVisString.replaceAll("\\s", "").equals(""))) {
            return;
        }

        String nosemidata = null;
        try {

            if (d.mCmdString != null && !d.mCmdString.equals("")) {
                nosemidata = d.mCmdString;
                byte[] sendtest = nosemidata.getBytes(mSettings.getEncoding());
                ByteBuffer buf =
                        ByteBuffer.allocate(
                                sendtest.length * 2); // just in case EVERY byte is the IAC
                int count = 0;
                for (int i = 0; i < sendtest.length; i++) {
                    if (sendtest[i] == TC.IAC) {
                        buf.put(TC.IAC);
                        buf.put(TC.IAC);
                        count += 2;
                    } else {
                        buf.put(sendtest[i]);
                        count++;
                    }
                }

                byte[] tosend = new byte[count];
                buf.rewind();
                buf.get(tosend, 0, count);

                if (mPump != null && mPump.isConnected()) {
                    mPump.sendData(tosend);
                } else {
                    sendBytesToWindow(
                            new String(
                                            Colorizer.getRedColor()
                                                    + "\nDisconnected.\n"
                                                    + Colorizer.getWhiteColor())
                                    .getBytes("UTF-8"));
                }
            } else {
                if (d.mCmdString.equals("") && d.mVisString == null) {
                    mPump.sendData(mCRLF.getBytes(mSettings.getEncoding()));
                    d.mVisString = "\n";
                }
            }
            if (d.mVisString != null && !d.mVisString.equals("")) {
                if (mSettings.isLocalEcho()) {
                    mWindowManager
                            .getWindows()
                            .get(0)
                            .getBuffer()
                            .addBytesImpl(d.mVisString.getBytes(mSettings.getEncoding()));
                    sendBytesToWindow(d.mVisString.getBytes(mSettings.getEncoding()));
                }
            }
        } catch (IOException e) {
            mEventLoop.send(ConnectionCommand.Disconnected.INSTANCE);
        }
    }

    /**
     * Possibly Deprecated. Sets the buffer size for a target window in a target plugin.
     *
     * @param plugin The target plugin.
     * @param window The target window.
     * @param amount The new buffer size value.
     */
    public final void updateWindowBufferMaxValue(
            final String plugin, final String window, final int amount) {
        for (WindowToken w : mWindowManager.getWindows()) {
            if (w.getName().equals(window)) {
                // WindowToken w = mWindowManager.getWindows().get(0);
                w.setBufferSize(amount);
            }
        }
    }

    /**
     * The main starting point for the save settings routine. This is called for a few different
     * locations.
     */
    public final void saveMainSettings() {
        mSettingsPersistence.saveMainSettings();
    }

    /**
     * Export settings routine. Called from either the main settings save routine or the export
     * settings dialog.
     *
     * @param path File name to save to. Must be absolute from the OS root directory.
     */
    public final void exportSettings(final String path) {
        mSettingsPersistence.exportSettings(path);
    }

    /**
     * Access point for the foreground window to initate a custom export action with the provided
     * path.
     *
     * @param path Path to save settings to, this must be absolute from the root directory (?)
     */
    public final void startExportSequence(final String path) {
        mEventLoop.send(new ConnectionCommand.ExportFile(path));
    }

    /**
     * The real workhorse method for importing settings.
     *
     * @param path The path of the settings to load.
     * @param save flag to save the settings after loading.
     * @param loadmessage verb for loading(true) or importing(false)
     */
    void importSettings(final String path, final boolean save, final boolean loadmessage) {
        shutdownPlugins();

        String verb = null;
        if (loadmessage) {
            verb = "Loading";
        } else {
            verb = "Importing";
        }

        VersionProbeParser vpp = new VersionProbeParser(path, mService.getApplicationContext());

        try {
            boolean isLegacy = vpp.isLegacy();
            if (isLegacy) {
                Log.e("XMLPARSE", "LOADING V1 SETTINGS FROM PATH: " + path);

                if (!path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    // internal legacy file being loaded, export the settings to the external
                    // blowtorch directory.
                    // convert path to external settings directory.
                    // get the package path.
                    // mService.getApplicationContext().getFilesDir();
                    File f = new File(mService.getApplicationContext().getFilesDir(), path);
                    String file = f.getName();
                    // File p = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    // + "/BlowTorch/recovered/");
                    File p = new File(mService.getExternalFilesDir(null), "/recovered/");
                    if (!p.exists()) {
                        p.mkdirs();
                    }
                    File newfile = new File(p, file);

                    if (!newfile.exists()) {
                        newfile.createNewFile();
                    }

                    InputStream in = new FileInputStream(f);
                    OutputStream out = new FileOutputStream(newfile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }

                HyperSAXParser p = new HyperSAXParser(path, mService.getApplicationContext());
                HyperSettings s = p.load();

                ApplicationInfo ai = null;
                try {
                    ai =
                            mService.getApplicationContext()
                                    .getPackageManager()
                                    .getApplicationInfo(
                                            mService.getPackageName(),
                                            PackageManager.GET_META_DATA);
                } catch (NameNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                String dataDir = ai.dataDir;

                // load up the default settings and then merge the old settings into the new
                // settings.
                ArrayList<Plugin> tmpplugs = new ArrayList<Plugin>();
                ConnectionSetttingsParser newsettings =
                        new ConnectionSetttingsParser(
                                null, mService.getApplicationContext(), tmpplugs, mHandlerShim, this);
                tmpplugs = newsettings.load(this, dataDir);

                Plugin buttonwindow = tmpplugs.get(1);
                ConnectionSettingsPlugin root_settings = (ConnectionSettingsPlugin) tmpplugs.get(0);

                if (path != null) { // import old buttons

                    // slag out the old settings and RAM them into the new ones.
                    LuaState pL = buttonwindow.getLuaState();
                    if (pL == null) {
                        return;
                    }

                    pL.newTable();
                    for (String key : s.getButtonSets().keySet()) {
                        ColorSetSettings defaults = s.getSetSettings().get(key);
                        // Vector<SlickButtonData> data = s.getButtonSets().get(key);
                        pL.newTable();

                        if (defaults.getPrimaryColor() != SlickButtonData.DEFAULT_COLOR) {
                            pL.pushString("primaryColor");
                            pL.pushNumber(defaults.getPrimaryColor());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getSelectedColor() != SlickButtonData.DEFAULT_SELECTED_COLOR) {
                            pL.pushString("selectedColor");
                            pL.pushNumber(defaults.getSelectedColor());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getFlipColor() != SlickButtonData.DEFAULT_FLIP_COLOR) {
                            pL.pushString("flipColor");
                            pL.pushNumber(defaults.getFlipColor());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getLabelColor() != SlickButtonData.DEFAULT_LABEL_COLOR) {
                            pL.pushString("labelColor");
                            pL.pushNumber(defaults.getLabelColor());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getFlipLabelColor()
                                != SlickButtonData.DEFAULT_FLIPLABEL_COLOR) {
                            pL.pushString("flipLabelColor");
                            pL.pushNumber(defaults.getFlipLabelColor());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getButtonWidth() != SlickButtonData.DEFAULT_BUTTON_WDITH) {
                            pL.pushString("width");
                            pL.pushNumber(defaults.getButtonWidth());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getButtonHeight() != SlickButtonData.DEFAULT_BUTTON_HEIGHT) {
                            pL.pushString("height");
                            pL.pushNumber(defaults.getButtonHeight());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        if (defaults.getLabelSize() != SlickButtonData.DEFAULT_LABEL_SIZE) {
                            pL.pushString("labelSize");
                            pL.pushNumber(defaults.getLabelSize());
                            pL.setTable(NEGATIVE_THREE);
                        }

                        pL.setField(NEGATIVE_TWO, key);
                    }

                    pL.setGlobal("buttonset_defaults");

                    pL.newTable();

                    for (String name : s.getButtonSets().keySet()) {
                        // String name = key;
                        ColorSetSettings defaults = s.getSetSettings().get(name);
                        Vector<SlickButtonData> data = s.getButtonSets().get(name);
                        pL.newTable();
                        int counter = 1;
                        for (SlickButtonData button : data) {
                            pL.newTable();
                            if (defaults.getPrimaryColor() != button.getPrimaryColor()) {
                                pL.pushString("primaryColor");
                                pL.pushNumber(button.getPrimaryColor());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getSelectedColor() != button.getSelectedColor()) {
                                pL.pushString("selectedColor");
                                pL.pushNumber(button.getSelectedColor());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getFlipColor() != button.getFlipColor()) {
                                pL.pushString("flipColor");
                                pL.pushNumber(button.getFlipColor());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getLabelColor() != button.getLabelColor()) {
                                pL.pushString("labelColor");
                                pL.pushNumber(button.getLabelColor());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getFlipLabelColor() != button.getFlipLabelColor()) {
                                pL.pushString("flipLabelColor");
                                pL.pushNumber(button.getFlipLabelColor());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getButtonWidth() != button.getWidth()) {
                                pL.pushString("width");
                                pL.pushNumber(button.getWidth());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getButtonHeight() != button.getHeight()) {
                                pL.pushString("height");
                                pL.pushNumber(button.getHeight());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (defaults.getLabelSize() != button.getLabelSize()) {
                                pL.pushString("labelSize");
                                pL.pushNumber(button.getLabelSize());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            if (button.getTargetSet() != null
                                    && !button.getTargetSet().equals("")) {
                                pL.pushString("switchTo");
                                pL.pushString(button.getTargetSet());
                                pL.setTable(NEGATIVE_THREE);
                            }

                            pL.pushString("x");
                            pL.pushNumber(button.getX());
                            pL.setTable(NEGATIVE_THREE);

                            pL.pushString("y");
                            pL.pushNumber(button.getY());
                            pL.setTable(NEGATIVE_THREE);

                            pL.pushString("label");
                            pL.pushString(button.getLabel());
                            pL.setTable(NEGATIVE_THREE);

                            pL.pushString("command");
                            pL.pushString(button.getText());
                            pL.setTable(NEGATIVE_THREE);

                            pL.pushString("flipLabel");
                            pL.pushString(button.getFlipLabel());
                            pL.setTable(NEGATIVE_THREE);

                            pL.pushString("flipCommand");
                            pL.pushString(button.getFlipCommand());
                            pL.setTable(NEGATIVE_THREE);

                            pL.rawSetI(NEGATIVE_TWO, counter);
                            counter++;
                        }

                        pL.setField(NEGATIVE_TWO, name);
                    }

                    pL.setGlobal("buttonsets");

                    pL.pushString(s.getLastSelected());
                    pL.setGlobal("current_set");

                    pL.getGlobal("legacyButtonsImported");
                    if (pL.getLuaObject(-1).isFunction()) {
                        pL.call(0, 0);
                    }

                } else {
                    // default settings are being loaded.
                    // run the adjustment for the new buttons
                    LuaState pL = buttonwindow.getLuaState();
                    if (pL == null) {
                        return;
                    }
                    pL.getGlobal("debug");
                    pL.getField(-1, "traceback");
                    pL.getGlobal("alignDefaultButtons");
                    if (pL.isFunction(-1)) {
                        int ret = pL.pcall(0, 1, NEGATIVE_TWO);
                        if (ret != 0) {
                            this.dispatchLuaError(pL.getLuaObject(-1).getString());
                        }
                    } else {
                        pL.pop(1);
                    }
                }
                // s.getSetSettings();

                WindowToken tmp = root_settings.getSettings().getWindows().get("mainDisplay");
                tmp.importV1Settings(s);

                // handle button settings.
                SettingsGroup buttonops = buttonwindow.getSettings().getOptions();
                String hfedit = s.getHapticFeedbackMode();
                if (hfedit.equals("auto")) {
                    buttonops.setOption("haptic_edit", Integer.toString(0));
                } else if (hfedit.equals("always")) {
                    buttonops.setOption("haptic_edit", Integer.toString(1));
                } else if (hfedit.equals("none")) {
                    buttonops.setOption("haptic_edit", Integer.toString(2));
                }

                String hfpress = s.getHapticFeedbackOnPress();
                if (hfpress.equals("auto")) {
                    buttonops.setOption("haptic_press", Integer.toString(0));
                } else if (hfpress.equals("always")) {
                    buttonops.setOption("haptic_press", Integer.toString(1));
                } else if (hfpress.equals("none")) {
                    buttonops.setOption("haptic_press", Integer.toString(2));
                }

                String hfflip = s.getHapticFeedbackOnFlip();
                if (hfflip.equals("auto")) {
                    buttonops.setOption("haptic_flip", Integer.toString(0));
                } else if (hfflip.equals("always")) {
                    buttonops.setOption("haptic_flip", Integer.toString(1));
                } else if (hfflip.equals("none")) {
                    buttonops.setOption("haptic_flip", Integer.toString(2));
                }
                String summary = Colorizer.getWhiteColor() + verb + " legacy settings file.\n";
                loadPlugins(tmpplugs, summary);
                root_settings.importV1Settings(s);
                if (!s.isRoundButtons()) {
                    buttonops.setOption("button_roundness", Integer.toString(0));
                }
            } else {
                int version = vpp.getVersionNumber();
                if (version == 2) {
                    Log.e("XMLPARSE", "LOADING V2 SETTINGS FROM PATH: " + path);
                    ArrayList<Plugin> tmpplugs = new ArrayList<Plugin>();
                    ConnectionSetttingsParser csp =
                            new ConnectionSetttingsParser(
                                    path,
                                    mService.getApplicationContext(),
                                    tmpplugs,
                                    mHandlerShim,
                                    this);
                    ApplicationInfo ai = null;
                    try {
                        ai =
                                mService.getApplicationContext()
                                        .getPackageManager()
                                        .getApplicationInfo(
                                                mService.getPackageName(),
                                                PackageManager.GET_META_DATA);
                    } catch (NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    String dataDir = ai.dataDir;
                    tmpplugs = csp.load(this, dataDir);

                    if (path == null) {
                        Plugin buttonwindow = tmpplugs.get(1);
                        // LuaState L = buttonwindow.getLuaState();
                        LuaState pL = buttonwindow.getLuaState();
                        if (pL == null) {
                            return;
                        }
                        pL.getGlobal("debug");
                        pL.getField(-1, "traceback");
                        pL.getGlobal("alignDefaultButtons");
                        if (pL.isFunction(-1)) {
                            int ret = pL.pcall(0, 1, NEGATIVE_TWO);
                            if (ret != 0) {
                                Connection.this.dispatchLuaError(
                                        "ERROR IN DEFAULT BUTTONS:"
                                                + (pL.getLuaObject(-1).getString()));
                            }
                        } else {
                            pL.pop(1);
                        }
                    }
                    String summary = Colorizer.getWhiteColor() + verb + " settings file.\n";
                    loadPlugins(tmpplugs, summary);
                } else {
                    Log.e(
                            "XMLPARSE",
                            "ERROR IN LOADING V2 SETTINGS, DID NOT FIND PROPER XMLVERSION NUMBER");
                    mService.dispatchXMLError(
                            "Error "
                                    + verb.toLowerCase(Locale.US)
                                    + " settings, invalid or missing version attribute.\n");
                    return;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
            mService.dispatchXMLError(e.getLocalizedMessage());
            return;
        }

        if (path == null) {
            mSettings
                    .getSettings()
                    .getOptions()
                    .setOption("font_size", Integer.toString(calculate80CharFontSize()));
        }

        buildTriggerSystem();
        if (save) {
            this.saveMainSettings();
        }
    }

    /** Entry point to load the internal settings. */
    private void loadInternalSettings() {
        Pattern invalidchars = Pattern.compile("\\W");
        Matcher replacebadchars = invalidchars.matcher(this.mDisplay);
        String prefsname = replacebadchars.replaceAll("");
        prefsname = prefsname.replaceAll("/", "");
        String rootPath = prefsname + ".xml";
        String internal = mService.getApplicationContext().getApplicationInfo().dataDir + "/files/";
        File oldp = new File(internal + rootPath);
        if (!oldp.exists()) {
            importSettings(null, false, true);
        } else {
            importSettings(rootPath, false, true);
        }
    }

    /** Build settings page routine. This is used by the settings loading routine. */
    private void buildSettingsPage() {
        mSettingsPersistence.buildSettingsPage();
        buildTriggerSystem();
    }

    /**
     * Attatches a WindowSettingsChangedListener to the given WindowToken.
     *
     * @param w The window to attatch a new settings changed listener to.
     */
    public final void attatchWindowSettingsChangedListener(final WindowToken w) {
        w.getSettings().setListener(new WindowSettingsChangedListener(w.getName()));
    }

    /**
     * Target for the foreground window to check if the keep last setting is set.
     *
     * @return value of the keep list settings.
     */
    public final boolean isKeepLast() {
        return (Boolean)
                ((BooleanOption) mSettings.getSettings().getOptions().findOptionByKey("keep_last"))
                        .getValue();
    }

    /**
     * Target for the foreground window to check if the full screen settings is set.
     *
     * @return the value of the full screen option.
     */
    public final boolean isFullScren() {
        return (Boolean)
                ((BooleanOption) mSettings.getSettings().getOptions().findOptionByKey("fullscreen"))
                        .getValue();
    }

    /**
     * Getter for mHost.
     *
     * @return mHost;
     */
    public final String getHostName() {
        return mHost;
    }

    /**
     * Getter for this connection's port value.
     *
     * @return the port number this connection is using.
     */
    public final int getPort() {
        return mPort;
    }

    /**
     * Utility method that generates the font size necessary to fit 80 chars to the window width.
     *
     * @return the font size that will produce nearest to 80 chars as possible.
     */
    private int calculate80CharFontSize() {
        return mSettingsPersistence.calculate80CharFontSize();
    }

    /**
     * Starts the recursive settings initialization routine to set all the settings loaded from the
     * serialized settings file.
     */
    private void initSettings() {
        mSettingsPersistence.initSettings();
    }

    /** Entry point for the foreground window to reset the settings for this connection. */
    public final void resetSettings() {
        mEventLoop.send(ConnectionCommand.ResetSettings.INSTANCE);
    }

    /** Work horse routine that actually resets the settings. */
    public final void doResetSettings() {
        for (WindowCallback c : mWindowManager.getCallbackMap().values()) {
            c.shutdown();
        }
        mService.markWindowsDirty();
        importSettings(null, true, true);
    }

    /**
     * Entry point for the foreground window to import a custom settings file at the given location.
     *
     * @param path Path of the settings to load.
     */
    public final void startLoadSettingsSequence(final String path) {
        mEventLoop.send(new ConnectionCommand.ImportFile(path));
    }

    /**
     * Work horse for the foreground window to add an external plugin and reload the settings.
     *
     * @param path The location of the external settings file.
     */
    public final void doAddLink(final String path) {
        mSettings.getLinks().add(path);
        saveMainSettings();
        reloadSettings();
    }

    /**
     * Entry point for the foreground window to add an external plugin and reload the settings.
     *
     * @param path The location of the external settings file.
     */
    public final void addLink(final String path) {
        mEventLoop.send(new ConnectionCommand.AddLink(path));
    }

    /**
     * Work horse routine for removing a plugin.
     *
     * @param plugin The name of the plugin to remove.
     */
    void doDeletePlugin(final String plugin) {
        Plugin p = mPluginMap.remove(plugin);

        String remove = null;
        if (p.getStorageType().equals("EXTERNAL")) {
            for (String path : mSettings.getLinks()) {
                if (p.getFullPath().contains(path)) {
                    remove = path;
                }
            }
        }
        if (remove != null) {
            mSettings.getLinks().remove(remove);
            mLinkMap.remove(remove);
        }

        mPlugins.remove(p);
        saveMainSettings();
        reloadSettings();
    }

    /**
     * Entry poit routine for removing a plugin.
     *
     * @param plugin The name of the plugin to remove.
     */
    public final void deletePlugin(final String plugin) {
        mEventLoop.send(new ConnectionCommand.DeletePlugin(plugin));
    }

    /**
     * Sets a plugin enabled.
     *
     * @param plugin Name of the plugin to affect.
     * @param enabled Desired state of the plugin.
     */
    public final void setPluginEnabled(final String plugin, final boolean enabled) {
        Plugin p = mPluginMap.get(plugin);
        p.setEnabled(enabled);
        saveMainSettings();
        reloadSettings();
    }

    /**
     * Gets the direction data from the main settings plugin.
     *
     * @return The direction map.
     */
    public final HashMap<String, DirectionData> getDirectionData() {
        return mSettings.getDirections();
    }

    /**
     * Sets the directio data for the main settings plugin. This is supplied from the foreground
     * window.
     *
     * @param data Diretion data wad from to use for the main settings wad.
     */
    public final void setDirectionData(final HashMap<String, DirectionData> data) {
        mSettings.setDirections(data);
        mSpeedwalkCommand.setDirections(data);
    }

    /**
     * Getter for the title bar height.
     *
     * @return the title bar height.
     */
    public final int getTitleBarHeight() {
        return mTitleBarHeight;
    }

    /**
     * Getter for the status bar height.
     *
     * @return the status bar height.
     */
    public final int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    /**
     * Utility method to test to see if a link has been loaded.
     *
     * @param link The path of the link to test. Relative to the BlowTorch sd card root.
     * @return The state of the target plugin. true = loaded, false = unloaded.
     */
    public final boolean isLinkLoaded(final String link) {
        String foo = Environment.getExternalStorageDirectory() + "/BlowTorch/";
        String bar = link.replace(foo, "");

        boolean ret = mLinkMap.containsKey(bar);
        return ret;
    }

    /** Kicks off the loadInternalSettings() routine. */
    public final void initWindows() {
        loadInternalSettings();
    }

    /** Immediatly shuts down this connection and all associated data structures. */
    public final void shutdown() {
        this.saveMainSettings();
        this.killNetThreads(true);
        for (Plugin p : mPlugins) {
            p.shutdown();
            p = null;
        }
        mSettings.shutdown();
        mSettings = null;
        if (mReconnectJob != null) {
            mReconnectJob.cancel(null);
            mReconnectJob = null;
        }
        mEventLoop.shutdown();
        mEventLoop = null;
        mHandlerShim = null;
        mService.removeConnectionNotification(mDisplay);
    }

    /**
     * Gets the path for a plugin.
     *
     * @param plugin Name of the plugin to interrogate.
     * @return The full path of the plugin.
     */
    public final String getPluginPath(final String plugin) {
        Plugin p = mPluginMap.get(plugin);
        return p.getFullPath();
    }

    /**
     * Entry point for Plugins to send data to the foreground window without trigger parsing.
     *
     * @param str The string to send.
     */
    public final void dispatchLuaText(final String str) {
        if (str != null) {
            mEventLoop.send(new ConnectionCommand.LuaNote(str));
        }
    }

    /**
     * Calls an anonymous global function in the target plugin. Does not provide an arugment.
     *
     * @param plugin Name of the target plugin.
     * @param function Name of the function to call.
     */
    public final void callPluginFunction(final String plugin, final String function) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            p.callFunction(function);
        } else {
            this.dispatchLuaText(
                    "\n"
                            + Colorizer.getRedColor()
                            + "No plugin named: "
                            + plugin
                            + Colorizer.getRedColor()
                            + "\n");
        }
    }

    @Override
    public final SettingsChangedListener getSettingsListener() {
        return (SettingsChangedListener) this;
    }

    @Override
    public final void callPlugin(final String plugin, final String function, final String data) {
        mEventLoop.send(new ConnectionCommand.CallPlugin(plugin, function, data));
    }

    @Override
    public final boolean pluginSupports(final String plugin, final String function) {
        Plugin p = mPluginMap.get(plugin);
        if (p != null) {
            return p.checkPluginSupports(function);
        }
        return false;
    }

    /**
     * Test to see of a plugin is installed.
     *
     * @param desired Name of the desired plugin.
     * @return is it loaded or not.
     */
    public final boolean isPluginInstalled(final String desired) {
        return mPluginMap.containsKey(desired);
    }

    /**
     * Returns a backward-compatible Handler shim for callers that still use Message-based dispatch.
     *
     * @return The handler shim associated with this connection.
     * @deprecated Use {@link #sendCommand(ConnectionCommand)} instead.
     */
    @Deprecated
    public final Handler getHandler() {
        return mHandlerShim;
    }

    /**
     * Sends a command through the event loop for dispatch.
     *
     * @param command The command to dispatch.
     */
    public final void sendCommand(final ConnectionCommand command) {
        if (mEventLoop != null) {
            mEventLoop.send(command);
        }
    }

    /**
     * Getter for the plugin list.
     *
     * @return The plugin list in loaded order.
     */
    public final ArrayList<Plugin> getPlugins() {
        return mPlugins;
    }

    @Override
    public ConnectionSettingsPlugin getConnectionSettings() {
        return mSettings;
    }

    @Override
    public Map<String, Plugin> getPluginMap() {
        return mPluginMap;
    }

    /**
     * Getter for mPump.
     *
     * @return the data pump for this connection.
     */
    public final DataPumper getPump() {
        return mPump;
    }

    /**
     * Getter for mProcessor.
     *
     * @return the processor associated with this connection.
     */
    public final Processor getProcessor() {
        return mProcessor;
    }

    /**
     * Getter for the display name.
     *
     * @return the launcher display name for this connection.
     */
    public final String getDisplay() {
        return mDisplay;
    }

    /**
     * Getter for the host name.
     *
     * @return the host name this connection uses.
     */
    public final String getHost() {
        return mHost;
    }

    /**
     * getter for mIsConnected.
     *
     * @return the connected state of this connection.
     */
    public final boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Getter for mService. This is really ugly and should be fixed immediatly.
     *
     * @return the service that initated this connection.
     */
    public final StellarService getService() {
        return mService;
    }

    public String getPluginOptionValue(String plugin, String key) {
        Plugin p = mPluginMap.get(plugin);
        if (p == null) return "Plugin " + plugin + " does not exist.";
        return p.getOptionValue(key);
    }

    @Override
    public HashMap<String, String> getCaptureMap() {
        return mCaptureMap;
    }

    @Override
    public String getEncoding() {
        return mSettings.getEncoding();
    }

    @Override
    public ConnectionWindowManager getWindowManager() {
        return mWindowManager;
    }

    @Override
    public Map<String, SpecialCommand> getSpecialCommands() {
        return mAliasManager.getSpecialCommands();
    }

    @Override
    public String getCRLF() {
        return mCRLF;
    }

    @Override
    public Map<String, ArrayList<String>> getLinkMap() {
        return mLinkMap;
    }

    @Override
    public SettingsChangedListener createWindowSettingsChangedListener(final String windowName) {
        return new WindowSettingsChangedListener(windowName);
    }
}
