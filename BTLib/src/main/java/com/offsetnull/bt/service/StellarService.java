/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import dalvik.system.PathClassLoader;

import com.offsetnull.bt.alias.AliasData;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.service.plugin.settings.EncodingOption;
import com.offsetnull.bt.service.plugin.settings.SettingsGroup;
import com.offsetnull.bt.settings.ConfigurationLoader;
import com.offsetnull.bt.speedwalk.DirectionData;
import com.offsetnull.bt.timer.TimerData;
import com.offsetnull.bt.trigger.TriggerData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of the background Service handler.
 *
 * <p>This class mostly just holds Connection objects and proxys commands made from the foreground
 * window to the appropriate connection.
 */
public class StellarService extends Service {

    /** Message constant indicating system startup. */
    protected static final int MESSAGE_STARTUP = 0;

    /** Message constant indicating a new connection launch. */
    protected static final int MESSAGE_NEWCONENCTION = 1;

    /**
     * Message constant indicating that the active connection should switch to a different
     * connection.
     */
    protected static final int MESSAGE_SWITCH = 2;

    /** Message constant indicating that the active connection should reload its settings. */
    protected static final int MESSAGE_RELOADSETTINGS = 3;

    /**
     * Not really sure what this is for but I think it had something to do with debugging an ANR in
     * the service.
     */
    protected static final int MESSAGE_STOPANR = 4;

    /** Duration of a short interval of time. */
    private static final int SHORT_DURATION = 300;

    /** The starting value for the notification id counter. */
    private static final int NOTIFICATION_START_VALUE = 100;

    /** File copy buffer size. */
    private static final int FILE_COPY_BUFFER_SIZE = 1024;

    /** The tracker variable for monotonically increasing notification ids. */
    private static int notificationCount = NOTIFICATION_START_VALUE;

    /** Tracker for if the foreground window is showing or hidden. */
    private boolean mWindowShowing = true;

    /**
     * The handler object used to coordinate multi-threaded efforts from the aidl callback onto the
     * main thread.
     */
    private Handler mHandler = null;

    /** The WifiLock object. */
    private WifiManager.WifiLock mWifiLock = null;

    /** The WifiManager object. */
    private WifiManager mWifiManager = null;

    /**
     * Tracker for if there is a notification in use that is used by the startForeground(...)
     * method.
     *
     * @see Service.startForeground(...)
     */
    private boolean mHasForegroundNotification = false;

    /** If there is a foreground notification being used, the id will be stored in this variable. */
    private int mForegroundNotificationId = -1;

    /** A map that keeps track of what notification id corresponds to which connection. */
    private HashMap<String, Integer> mConnectionNotificationIdMap = new HashMap<String, Integer>();

    /** A map that keeps track of what notification corresponds to which connection. */
    private HashMap<String, Notification> mConnectionNotificationMap =
            new HashMap<String, Notification>();

    /** The notification manager. */
    private NotificationManager mNotificationManager = null;

    /** A map of connection display names and their associated connection objects. */
    private HashMap<String, Connection> mConnections = null;

    /** The currently "Selected" connection. */
    private String mConnectionClutch = "";

    /** Dispatcher that handles service-level commands. */
    private ServiceDispatcher mDispatcher = null;

    /** The callback list of MainWindow activities that have bound to the Service. */
    private final List<ConnectionCallback> mCallbacks = new ArrayList<ConnectionCallback>();

    /** The callback list of Launcher activities that have bound to the Service. */
    private final List<LauncherCallback> mLauncherCallbacks = new ArrayList<LauncherCallback>();

    /** The remote callback target. */
    /** The local binder for in-process binding. */
    private final LocalBinder mLocalBinder = new LocalBinder();

    static {
        try {
            System.loadLibrary("sqlite3");
            System.loadLibrary("lua");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MUDWammer", "Native Lua libraries not available", e);
        }
    }

    /** Binder for in-process (local) binding, replacing AIDL IPC. */
    public class LocalBinder extends android.os.Binder {
        public StellarService getService() {
            return StellarService.this;
        }
    }

    @Override
    public void onLowMemory() {
        // Log.e("SERVICE","The service has been requested to shore up memory usage, potentially
        // going to be killed.");
    }

    /**
     * Implementation of onStartCommand(...) Service function.
     *
     * @param intent see docs
     * @param flags see docs
     * @param startId see docs
     * @return see docs
     * @see Android Documentation for Service.onStartCommand()
     */
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null) {
            return Service.START_STICKY_COMPATIBILITY;
        }
        if (ConfigurationLoader.isTestMode(this.getApplicationContext())) {
            // Thread.setDefaultUncaughtExceptionHandler(new
            // com.happygoatstudios.bt.crashreport.CrashReporter(this.getApplicationContext()));
            Log.e("BLOWTORCH", "SHOULD SET THE UNCAUGHT EXCEPTION HANDLER HERE.");
        }
        return Service.START_STICKY_COMPATIBILITY;
    }

    /** The implementation of the onCreate() Service method. */
    public final void onCreate() {

        mConnections = new HashMap<String, Connection>();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        SharedPreferences prefs = this.getSharedPreferences("SERVICE_INFO", 0);

        int libsver = prefs.getInt("CURRENT_LUA_LIBS_VERSION", 0);
        Bundle meta = null;
        try {
            meta =
                    this.getPackageManager()
                            .getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA)
                            .metaData;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        int packagever = meta.getInt("BLOWTORCH_LUA_LIBS_VERSION");
        if (packagever != libsver) {
            // copy new libs.
            try {
                updateLibs();
                // updatelibsver needs to be incremented and saved back into the shared preferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("CURRENT_LUA_LIBS_VERSION", packagever);
                editor.apply();
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mDispatcher = new ServiceDispatcher(
                (display, host, port) -> {
                    Connection c = new Connection(display, host, port, StellarService.this);
                    mConnections.put(display, c);
                    return c;
                }
        );
        mHandler = new Handler(new ServiceHandler());
    }

    /**
     * There are a few things that are needed to be handled on the main thread and the aidl bridge
     * makes that difficult, so this is used to aggregate code onto the main thread rather than the
     * dispatch threads.
     */
    private class ServiceHandler implements Handler.Callback {
        @Override
        public boolean handleMessage(final Message msg) {
            switch (msg.what) {
                case MESSAGE_RELOADSETTINGS:
                    mDispatcher.dispatch(ServiceCommand.ReloadSettings.INSTANCE);
                    mConnectionClutch = mDispatcher.getActiveConnection();
                    reloadWindows();
                    break;
                case MESSAGE_STARTUP:
                    mDispatcher.dispatch(ServiceCommand.Startup.INSTANCE);
                    break;
                case MESSAGE_NEWCONENCTION:
                    Bundle b = msg.getData();
                    String display = b.getString("DISPLAY");
                    String host = b.getString("HOST");
                    int port = b.getInt("PORT");

                    if (!mConnections.containsKey(display)) {
                        mDispatcher.dispatch(
                                new ServiceCommand.NewConnection(display, host, port));
                        mConnectionClutch = mDispatcher.getActiveConnection();
                    }
                    break;
                case MESSAGE_SWITCH:
                    mDispatcher.dispatch(
                            new ServiceCommand.SwitchConnection((String) msg.obj));
                    mConnectionClutch = mDispatcher.getActiveConnection();
                    switchTo((String) msg.obj);
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    /** Implementation of the Service.onDestroy() method. */
    public final void onDestroy() {
        doShutdown();
        super.onDestroy();
    }

    /**
     * The top level disconnect function.
     *
     * <p>This method is initated by a connection when it has disconnected. This method interfaces
     * with the foreground window if appropriate to show the disconnection message.
     *
     * @param c The connection that disconnected.
     */
    public final void doDisconnect(final Connection c) {
        // attempt to display the disconnection dialog.
        if (c.getDisplay().equals(mConnectionClutch)) {

            for (ConnectionCallback cb : mCallbacks) {
                cb.doDisconnectNotice(c.getDisplay());
            }

            if (mCallbacks.isEmpty()) {
                showDisconnectedNotification(c, c.getDisplay(), c.getHost(), c.getPort());
            }
        } else {
            showDisconnectedNotification(c, c.getDisplay(), c.getHost(), c.getPort());
        }
    }

    /**
     * Dispatches an error dialog in the foreground window.
     *
     * @param error The error message to show.
     */
    public final void dispatchXMLError(final String error) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.displayXMLError(error);
        }
    }

    public void dispatchSaveError(String error) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.displaySaveError(error);
        }
    }

    public void dispatchPluginSaveError(String plugin, String error) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.displayPluginSaveError(plugin, error);
        }
    }

    /** Enables Wifi KeepAlive. */
    public final void enableWifiKeepAlive() {
        try {
            if (mWifiManager == null) {
                mWifiManager =
                        (WifiManager)
                                this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            }

            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info.getNetworkId() != -1) {
                mWifiLock = mWifiManager.createWifiLock("BLOWTORCH_WIFI_LOCK");
                boolean held = false;
                while (!held) {
                    mWifiLock.acquire();
                    held = mWifiLock.isHeld();
                }
            }
        } catch (SecurityException e) {
            Log.w("StellarService", "Cannot acquire wifi lock: " + e.getMessage());
        }
    }

    /** The implementation of the bell vibrator. Connections will call this. */
    public final void doVibrateBell() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(SHORT_DURATION);
    }

    /**
     * The implementation ofthe bell notifier. Connections will call this.
     *
     * @param display The display name of the calling connection.
     * @param host The host name of the calling connection.
     * @param port The port number for the calling connection.
     */
    // Resource name for notification icon is loaded from configuration at runtime.
    @SuppressLint("DiscouragedApi")
    @SuppressWarnings("deprecation")
    public final void doNotifyBell(final String display, final String host, final int port) {
        int resId =
                this.getResources()
                        .getIdentifier(
                                ConfigurationLoader.getConfigurationValue(
                                        "notificationIcon", this.getApplicationContext()),
                                "drawable",
                                this.getPackageName());

        // Notification note = new Notification(resId, "Alert!", System.currentTimeMillis());

        Context context = getApplicationContext();
        CharSequence contentTitle = display + " - Alert!";
        CharSequence contentText = "The server is notifying you with the bell character, 0x07.";
        Intent notificationIntent = null;
        String windowAction =
                ConfigurationLoader.getConfigurationValue(
                        "windowAction", this.getApplicationContext());
        notificationIntent = new Intent(windowAction);
        notificationIntent.putExtra("DISPLAY", display);
        notificationIntent.putExtra("HOST", host);
        notificationIntent.putExtra("PORT", Integer.toString(port));
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // note.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification note =
                builder.setContentIntent(contentIntent)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .build();
        // mNM.notify(NOTIFICATION, notification);
        note.icon = resId;
        note.flags = Notification.DEFAULT_ALL;

        mNotificationManager.notify(StellarService.getNotificationId(), note);
    }

    /** Implementation of the visual bell callback. Called from a Connection. */
    public final void doDisplayBell() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.doVisualBell();
        }
    }

    /** Disables the wifi keep alive. */
    public final void disableWifiKeepAlive() {
        // if we have a wifi lock, release it
        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }
    }

    /**
     * Utility method for dispatching a toast message to be displayed by the foreground window.
     *
     * @param message The message to show.
     * @param longtime true for Toast.LONG, false for Toast.SHORT
     */
    public final void dispatchToast(final String message, final boolean longtime) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.showMessage(message, longtime);
        }
    }

    /**
     * Utility method for dispatching a generic error looking dialog on the foreground window.
     *
     * @param message The message to display.
     */
    public final void dispatchDialog(final String message) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.showDialog(message);
        }
    }

    /**
     * Gets a new unique id for notifications. Always increments the value so it will be unique with
     * each call.
     *
     * @return the new unique identifier number.
     */
    public static int getNotificationId() {
        notificationCount += 1;
        return Integer.valueOf(notificationCount);
    }

    /**
     * The end of the chain of methods that are called when a disconnection is encountered.
     *
     * @param c The connection that has disconnected.
     * @param display The display name associated with <b>c</b>
     * @param host The host name associated with <b>c</b>
     * @param port The port name associated with <b>c</b>
     */
    // Resource name for notification icon is loaded from configuration at runtime.
    @SuppressLint("DiscouragedApi")
    @SuppressWarnings("deprecation")
    private void showDisconnectedNotification(
            final Connection c, final String display, final String host, final int port) {
        // if we are here it means that the server has explicitly closed the connection, and nobody
        // was around to see it.
        c.shutdown(); // call this to make sure all net threads are really dead, and to
        // remove the ongoing notification and re-set the foreground
        // notification if need be.
        mConnections.remove(display);

        // mNM.cancel(5545);
        int resId =
                this.getResources()
                        .getIdentifier(
                                ConfigurationLoader.getConfigurationValue(
                                        "notificationIcon", this.getApplicationContext()),
                                "drawable",
                                this.getPackageName());

        CharSequence brandName =
                ConfigurationLoader.getConfigurationValue(
                        "ongoingNotificationLabel", this.getApplicationContext());
        // Notification note = new Notification(resId, brandName + " Disconnected",
        // System.currentTimeMillis());
        // String defaultmsg = "Click to reconnect: "+ host +":"+ port;
        Context context = getApplicationContext();
        CharSequence contentTitle = brandName + " Disconnected";
        // CharSequence contentText = "Hello World!";
        CharSequence contentText = null;
        String message = "Click to reconnect: " + host + ":" + port;
        if (message != null && !message.equals("")) {
            contentText = message;
        }
        Intent notificationIntent = null;
        String windowAction =
                ConfigurationLoader.getConfigurationValue(
                        "windowAction", this.getApplicationContext());
        notificationIntent = new Intent(windowAction);

        String apkName = null;
        try {
            apkName =
                    this.getPackageManager().getApplicationInfo(this.getPackageName(), 0).sourceDir;
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }
        Class<?> w = null;
        PathClassLoader cl =
                new dalvik.system.PathClassLoader(apkName, ClassLoader.getSystemClassLoader());
        try {
            w = Class.forName("com.offsetnull.bt.window.MainWindow", false, cl);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            notificationIntent.setClass(
                    this.createPackageContext(this.getPackageName(), Context.CONTEXT_INCLUDE_CODE),
                    w);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        notificationIntent.putExtra("DISPLAY", display);
        notificationIntent.putExtra("HOST", host);
        notificationIntent.putExtra("PORT", Integer.toString(port));
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int id = getNotificationId();
        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // note.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Notification note =
                builder.setContentIntent(contentIntent)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setSmallIcon(resId)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .build();
        // note.icon = resId;
        // note.flags = note.flags | Notification.FLAG_AUTO_CANCEL |
        // Notification.FLAG_ONLY_ALERT_ONCE;
        mNotificationManager.notify(id, note);

        // now, if the launcher connection list has a listener, we should notify it that a
        // connection has gone
        for (LauncherCallback cb : mLauncherCallbacks) {
            cb.connectionDisconnected();
        }
    }

    /**
     * Method called when a connection has connected successfully.
     *
     * <p>This method coordinates with the multi-connection system to ensure that at least one
     * notification is used for the startForeground(...) method.
     *
     * @param display The display name of the connection.
     * @param host The host name of the connection.
     * @param port The port number of the connection.
     */
    // Resource name for notification icon is loaded from configuration at runtime.
    @SuppressLint("DiscouragedApi")
    @SuppressWarnings("deprecation")
    public final void showConnectionNotification(
            final String display, final String host, final int port) {
        if (mConnectionNotificationMap.containsKey(display)) {
            return;
        }
        int resId =
                this.getResources()
                        .getIdentifier(
                                ConfigurationLoader.getConfigurationValue(
                                        "notificationIcon", this.getApplicationContext()),
                                "drawable",
                                this.getPackageName());

        // Notification note = new Notification(resId, "BlowTorch Connected",
        // System.currentTimeMillis());
        Context context = getApplicationContext();

        CharSequence contentTitle =
                ConfigurationLoader.getConfigurationValue(
                        "ongoingNotificationLabel", this.getApplicationContext());
        CharSequence contentText = "Connected: (" + host + ":" + port + ")";

        Intent notificationIntent = null;
        String windowAction =
                ConfigurationLoader.getConfigurationValue(
                        "windowAction", this.getApplicationContext());
        notificationIntent = new Intent(windowAction);

        String apkName = null;
        try {
            apkName =
                    this.getPackageManager().getApplicationInfo(this.getPackageName(), 0).sourceDir;
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }
        Class<?> w = null;
        PathClassLoader cl =
                new dalvik.system.PathClassLoader(apkName, ClassLoader.getSystemClassLoader());
        try {
            w = Class.forName("com.offsetnull.bt.window.MainWindow", false, cl);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        try {
            notificationIntent.setClass(
                    this.createPackageContext(this.getPackageName(), Context.CONTEXT_INCLUDE_CODE),
                    w);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        notificationIntent.putExtra("DISPLAY", display);
        notificationIntent.putExtra("HOST", host);
        notificationIntent.putExtra("PORT", Integer.toString(port));
        // }
        int notificationID = -1;
        if (mConnectionNotificationIdMap.containsKey(display)) {
            notificationID = mConnectionNotificationIdMap.get(display);
        } else {
            notificationID = getNotificationId();
        }

        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this,
                        notificationID,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = createNotificationChannel(display);
        // note.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        Notification note =
                builder.setContentIntent(contentIntent)
                        .setContentTitle(contentTitle)
                        .setSmallIcon(resId)
                        .setContentText(contentText)
                        .setOngoing(true)
                        .setChannelId(channelId)
                        .build();

        // note.icon = resId;
        // note.flags = Notification.FLAG_ONGOING_EVENT;

        if (!mHasForegroundNotification) {
            mForegroundNotificationId = notificationID;
            this.startForeground(
                    mForegroundNotificationId,
                    note,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            mHasForegroundNotification = true;
        } else {
            // int notificationId = notificationID;
            mNotificationManager.notify(notificationID, note);
        }

        mConnectionNotificationIdMap.put(display, notificationID);
        mConnectionNotificationMap.put(display, note);
    }

    /**
     * Create notification channel. This is require for startForeground starting on Android O
     *
     * @param none
     */
    public final String createNotificationChannel(final String display) {
        String channelId =
                ConfigurationLoader.getConfigurationValue("ongoingNotificationLabel", this)
                        + "_service"
                        + "_"
                        + display;
        String channelName =
                ConfigurationLoader.getConfigurationValue("ongoingNotificationLabel", this);
        NotificationChannel c =
                new NotificationChannel(
                        channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        c.setShowBadge(false);
        c.setSound(null, null);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(c);
        return channelId;
    }

    /**
     * Called by a connection when disconnected to remove the associated notification.
     *
     * @param display The display name of the disconnected connection.
     */
    public final void removeConnectionNotification(final String display) {
        if (!mConnectionNotificationIdMap.containsKey(display)) {
            return;
        }
        int id = mConnectionNotificationIdMap.get(display);
        mConnectionNotificationIdMap.remove(display);
        mConnectionNotificationMap.remove(display);
        if (id != mForegroundNotificationId) {
            // just kill off the notification
            mNotificationManager.cancel(id);
        } else {
            this.stopForeground(true);

            // get the first connection and make it the new foreground notification
            if (mConnectionNotificationMap.size() == 0) {
                mConnectionClutch = "";
                return;
            }
            String[] tmp = new String[mConnectionNotificationMap.size()];
            tmp = mConnectionNotificationMap.keySet().toArray(tmp);
            int tmpID = mConnectionNotificationIdMap.get(tmp[0]);
            mConnectionClutch = tmp[0];
            Notification tmpNote = mConnectionNotificationMap.get(tmp[0]);
            // mNotificationManager.cancel(mForegroundNotificationId);
            mForegroundNotificationId = tmpID;
            mNotificationManager.cancel(tmpID);
            this.startForeground(
                    tmpID, tmpNote, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        }
    }

    /**
     * The generic top level shutdown routine. Attempts to gracefully shut down all active
     * connections.
     */
    public final void doShutdown() {

        for (Connection c : mConnections.values()) {
            c.shutdown();
            c = null;
        }

        this.stopForeground(true);
        this.stopSelf();
    }

    /**
     * The implementation of Service.onBind().
     *
     * @param arg0 the Intent that was used to start this service.
     * @return the IBinder to give to the foreground process that is requesting to bind.
     * @see Android AIDL docs.
     */
    public final IBinder onBind(final Intent arg0) {
        return mLocalBinder;
    }

    /**
     * Setter method for mConnectionClutch.
     *
     * @param connection The new connection to make active.
     */
    public final void setClutch(final String connection) {
        mConnectionClutch = connection;
    }

    /**
     * Utility method to switch the active connection.
     *
     * @param display The display name of the connection to switch to.
     */
    public final void switchTo(final String display) {
        setClutch(display);
        for (ConnectionCallback cb : mCallbacks) {
            cb.markWindowsDirty();
            cb.loadWindowSettings();
            cb.loadSettings();
            cb.reloadBuffer();
        }
    }

    /** Generic method to make the currently active connection reload its windows. */
    public final void reloadWindows() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.loadWindowSettings();
        }
    }

    // ── Public methods for local (in-process) binding ──────────────────

    public void registerCallback(
            final ConnectionCallback c, final String host, final int port, final String display) {
        if (c != null) {
            mCallbacks.add(c);

            if (!mConnections.containsKey(display)) {
                setConnectionData(host, port, display);
            } else {
                mConnectionClutch = display;
                c.loadWindowSettings();
            }
        }
    }

    public void unregisterCallback(final ConnectionCallback c) {
        if (c != null) {
            mCallbacks.remove(c);
        }
    }

    public void registerLauncherCallback(final LauncherCallback c) {
        if (c != null) {
            mLauncherCallbacks.add(c);
        }
    }

    public void unregisterLauncherCallback(final LauncherCallback c) {
        if (c != null) {
            mLauncherCallbacks.remove(c);
        }
    }

    public void initXfer() {
        mHandler.sendEmptyMessage(MESSAGE_STARTUP);
    }

    public void endXfer() {
        Connection c = mConnections.get(mConnectionClutch);
        c.sendDataToWindow(
                "\n"
                        + Colorizer.getRedColor()
                        + "Connection terminated by user."
                        + Colorizer.getWhiteColor()
                        + "\n\n");
        c.killNetThreads(true);
        mConnections.get(mConnectionClutch).doDisconnect(true);
    }

    public boolean isConnected() {
        if (mConnections.size() < 1) {
            return false;
        }
        return mConnections.get(mConnectionClutch).isConnected();
    }

    public void sendData(final byte[] seq) {
        Handler handler = mConnections.get(mConnectionClutch).getHandler();
        handler.sendMessage(handler.obtainMessage(Connection.MESSAGE_SENDDATA_BYTES, seq));
    }

    public void saveSettings() {
        Connection c = mConnections.get(mConnectionClutch);
        if (c == null) {
            return;
        }
        c.saveMainSettings();
    }

    public void setConnectionData(final String host, final int port, final String display) {
        Message msg = mHandler.obtainMessage(MESSAGE_NEWCONENCTION);
        Bundle b = msg.getData();
        b.putString("DISPLAY", display);
        b.putString("HOST", host);
        b.putInt("PORT", port);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    @SuppressWarnings("rawtypes")
    public List getSystemCommands() {
        return mConnections.get(mConnectionClutch).getSystemCommands();
    }

    @SuppressWarnings("rawtypes")
    public Map getAliases() {
        return mConnections.get(mConnectionClutch).getAliases();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setAliases(final Map map) {
        mConnections.get(mConnectionClutch).setAliases((HashMap<String, AliasData>) map);
    }

    public void loadSettingsFromPath(final String path) {
        mConnections.get(mConnectionClutch).startLoadSettingsSequence(path);
    }

    public void exportSettingsToPath(final String path) {
        mConnections.get(mConnectionClutch).exportSettings(path);
    }

    public void resetSettings() {
        mConnections.get(mConnectionClutch).resetSettings();
    }

    @SuppressWarnings("rawtypes")
    public Map getTriggerData() {
        HashMap<String, TriggerData> triggers = mConnections.get(mConnectionClutch).getTriggers();
        return triggers;
    }

    @SuppressWarnings("rawtypes")
    public Map getPluginTriggerData(final String id) {
        return mConnections.get(mConnectionClutch).getPluginTriggers(id);
    }

    @SuppressWarnings("rawtypes")
    public Map getDirectionData() {
        return mConnections.get(mConnectionClutch).getDirectionData();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setDirectionData(final Map data) {
        mConnections.get(mConnectionClutch).setDirectionData((HashMap<String, DirectionData>) data);
    }

    public void newTrigger(final TriggerData data) {
        mConnections.get(mConnectionClutch).addTrigger(data);
    }

    public void updateTrigger(final TriggerData from, final TriggerData to) {
        mConnections.get(mConnectionClutch).updateTrigger(from, to);
    }

    public void deleteTrigger(final String which) {
        mConnections.get(mConnectionClutch).deleteTrigger(which);
    }

    public TriggerData getTrigger(final String pattern) {
        return mConnections.get(mConnectionClutch).getTrigger(pattern);
    }

    public boolean isKeepLast() {
        return mConnections.get(mConnectionClutch).isKeepLast();
    }

    public void setDisplayDimensions(final int rows, final int cols) {
        Connection c = mConnections.get(mConnectionClutch);
        if (c == null) {
            return;
        }
        c.getProcessor().setDisplayDimensions(rows, cols);
    }

    public void reconnect(final String str) {
        String connection = str;
        if (str == null || str.equals("")) {
            connection = mConnectionClutch;
        }
        mConnections.get(connection).doReconnect();
    }

    @SuppressWarnings("rawtypes")
    public Map getTimers() {
        return mConnections.get(mConnectionClutch).getTimers();
    }

    @SuppressWarnings("rawtypes")
    public Map getPluginTimers(final String plugin) {
        return mConnections.get(mConnectionClutch).getPluginTimers(plugin);
    }

    public TimerData getTimer(final String ordinal) {
        return mConnections.get(mConnectionClutch).getTimer(ordinal);
    }

    public void startTimer(final String ordinal) {
        mConnections.get(mConnectionClutch).playTimer(ordinal);
    }

    public void pauseTimer(final String ordinal) {
        mConnections.get(mConnectionClutch).pauseTimer(ordinal);
    }

    public void stopTimer(final String ordinal) {
        mConnections.get(mConnectionClutch).stopTimer(ordinal);
    }

    public void startPluginTimer(final String plugin, final String ordinal) {
        mConnections.get(mConnectionClutch).playPluginTimer(plugin, ordinal);
    }

    public void pausePluginTimer(final String plugin, final String ordinal) {
        mConnections.get(mConnectionClutch).pausePluginTimer(plugin, ordinal);
    }

    public void stopPluginTimer(final String plugin, final String ordinal) {
        mConnections.get(mConnectionClutch).stopPluginTimer(plugin, ordinal);
    }

    public void updateTimer(final TimerData old, final TimerData newtimer) {
        mConnections.get(mConnectionClutch).updateTimer(old, newtimer);
    }

    public void addTimer(final TimerData newtimer) {
        mConnections.get(mConnectionClutch).addTimer(newtimer);
    }

    public void removeTimer(final TimerData deltimer) {
        // TODO: THIS IS BLANK, CAN WE REMOVE TIMERS?!
    }

    public int getNextTimerOrdinal() {
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public Map getTimerProgressWad() {
        return null;
    }

    public String getEncoding() {
        return (String)
                ((EncodingOption)
                                mConnections
                                        .get(mConnectionClutch)
                                        .getSettings()
                                        .findOptionByKey("encoding"))
                        .getValue();
    }

    public String getConnectedTo() {
        return mConnectionClutch;
    }

    public boolean isFullScreen() {
        return mConnections.get(mConnectionClutch).isFullScren();
    }

    public void setTriggerEnabled(final boolean enabled, final String key) {
        mConnections.get(mConnectionClutch).setTriggerEnabled(enabled, key);
    }

    public void setButtonSetLocked(final boolean locked, final String key) {}

    public boolean isButtonSetLocked(final String key) {
        return false;
    }

    public boolean isButtonSetLockedMoveButtons(final String key) {
        return false;
    }

    public boolean isButtonSetLockedNewButtons(final String key) {
        return false;
    }

    public boolean isButtonSetLockedEditButtons(final String key) {
        return false;
    }

    public void startNewConnection(final String host, final int port, final String display) {}

    public void switchToConnection(final String display) {
        mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SWITCH, display));
    }

    public boolean isConnectedTo(final String display) {
        return mConnections.keySet().contains(display);
    }

    @SuppressWarnings("rawtypes")
    public List getConnections() {
        List<String> tmp = new ArrayList<String>();
        for (String key : mConnections.keySet()) {
            tmp.add(key);
        }
        return tmp;
    }

    public WindowToken[] getWindowTokens() {
        if (mConnections == null || mConnections.size() == 0) {
            return null;
        }
        return mConnections.get(mConnectionClutch).getWindows();
    }

    public void registerWindowCallback(
            final String displayName, final String name, final WindowCallback callback) {
        Connection c = mConnections.get(displayName);
        if (c != null) {
            c.registerWindowCallback(name, callback);
        }
    }

    public void unregisterWindowCallback(final String name, final WindowCallback callback) {
        Connection c = mConnections.get(name);
        if (c != null) {
            c.unregisterWindowCallback(callback);
        }
    }

    public String getScript(final String plugin, final String name) {
        return mConnections.get(mConnectionClutch).getScript(plugin, name);
    }

    public void reloadSettings() {
        mHandler.sendEmptyMessage(MESSAGE_RELOADSETTINGS);
    }

    public void pluginXcallS(final String plugin, final String function, final String str) {
        mConnections.get(mConnectionClutch).pluginXcallS(plugin, function, str);
    }

    @SuppressWarnings("rawtypes")
    public Map getPluginList() {
        Connection c = mConnections.get(mConnectionClutch);
        HashMap<String, String> list = new HashMap<String, String>();

        for (Plugin p : c.getPlugins()) {
            String info = "";
            info += p.getTriggerCount() + " T, ";
            info += p.getAliasCount() + " A, ";
            info += p.getTimerCount() + " C, ";
            info += p.getScriptCount() + " S, ";
            info += p.getStorageType();
            list.put(p.getName(), info);
        }

        return list;
    }

    @SuppressWarnings("rawtypes")
    public List getPluginsWithTriggers() {
        ArrayList<String> list = new ArrayList<String>();
        Connection c = mConnections.get(mConnectionClutch);
        for (Plugin p : c.getPlugins()) {
            if (p.getSettings().getTriggers().size() > 0) {
                list.add(p.getName());
            }
        }
        return list;
    }

    public void newPluginTrigger(final String selectedPlugin, final TriggerData data) {
        mConnections.get(mConnectionClutch).newPluginTrigger(selectedPlugin, data);
    }

    public void updatePluginTrigger(
            final String selectedPlugin, final TriggerData from, final TriggerData to) {
        mConnections.get(mConnectionClutch).updatePluginTrigger(selectedPlugin, from, to);
    }

    public TriggerData getPluginTrigger(final String selectedPlugin, final String pattern) {
        return mConnections.get(mConnectionClutch).getPluginTrigger(selectedPlugin, pattern);
    }

    public void setPluginTriggerEnabled(
            final String selectedPlugin, final boolean enabled, final String key) {
        mConnections.get(mConnectionClutch).setPluginTriggerEnabled(selectedPlugin, enabled, key);
    }

    public void deletePluginTrigger(final String selectedPlugin, final String which) {
        mConnections.get(mConnectionClutch).deletePluginTrigger(selectedPlugin, which);
    }

    public AliasData getAlias(final String key) {
        return mConnections.get(mConnectionClutch).getAlias(key);
    }

    public AliasData getPluginAlias(final String plugin, final String key) {
        return mConnections.get(mConnectionClutch).getPluginAlias(plugin, key);
    }

    @SuppressWarnings("rawtypes")
    public Map getPluginAliases(final String currentPlugin) {
        return mConnections.get(mConnectionClutch).getPluginAliases(currentPlugin);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setPluginAliases(final String plugin, final Map map) {
        mConnections
                .get(mConnectionClutch)
                .setPluginAliases(plugin, (HashMap<String, AliasData>) map);
    }

    public void deleteAlias(final String key) {
        mConnections.get(mConnectionClutch).deleteAlias(key);
    }

    public void deletePluginAlias(final String plugin, final String key) {
        mConnections.get(mConnectionClutch).deletePluginAlias(plugin, key);
    }

    public void setAliasEnabled(final boolean enabled, final String key) {
        mConnections.get(mConnectionClutch).setAliasEnabled(enabled, key);
    }

    public void setPluginAliasEnabled(
            final String plugin, final boolean enabled, final String key) {
        mConnections.get(mConnectionClutch).setPluginAliasEnabled(plugin, enabled, key);
    }

    public TimerData getPluginTimer(final String plugin, final String name) {
        return mConnections.get(mConnectionClutch).getPluginTimer(plugin, name);
    }

    public void deleteTimer(final String name) {
        mConnections.get(mConnectionClutch).deleteTimer(name);
    }

    public void deletePluginTimer(final String plugin, final String name) {
        mConnections.get(mConnectionClutch).deletePluginTimer(plugin, name);
    }

    public void updatePluginTimer(
            final String plugin, final TimerData old, final TimerData newtimer) {
        mConnections.get(mConnectionClutch).updatePluginTimer(plugin, old, newtimer);
    }

    public void addPluginTimer(final String plugin, final TimerData newtimer) {
        mConnections.get(mConnectionClutch).addPluginTimer(plugin, newtimer);
    }

    public SettingsGroup getSettings() {
        if (mConnections.size() == 0) {
            return null;
        }
        Connection c = mConnections.get(mConnectionClutch);
        if (c == null) {
            return null;
        }
        return c.getSettings();
    }

    public SettingsGroup getPluginSettings(final String plugin) {
        return mConnections.get(mConnectionClutch).getPluginSettings(plugin);
    }

    public void updateBooleanSetting(final String key, final boolean value) {
        mConnections.get(mConnectionClutch).updateBooleanSetting(key, value);
    }

    public void updatePluginBooleanSetting(
            final String plugin, final String key, final boolean value) {
        mConnections.get(mConnectionClutch).updatePluginBooleanSetting(plugin, key, value);
    }

    public void updateIntegerSetting(final String key, final int value) {
        mConnections.get(mConnectionClutch).updateIntegerSetting(key, value);
    }

    public void updatePluginIntegerSetting(final String plugin, final String key, final int value) {
        mConnections.get(mConnectionClutch).updatePluginIntegerSetting(plugin, key, value);
    }

    public void updateFloatSetting(final String key, final float value) {
        mConnections.get(mConnectionClutch).updateFloatSetting(key, value);
    }

    public void updatePluginFloatSetting(final String plugin, final String key, final float value) {
        mConnections.get(mConnectionClutch).updatePluginFloatSetting(plugin, key, value);
    }

    public void updateStringSetting(final String key, final String value) {
        mConnections.get(mConnectionClutch).updateStringSetting(key, value);
    }

    public void updatePluginStringSetting(
            final String plugin, final String key, final String value) {
        mConnections.get(mConnectionClutch).updatePluginStringSetting(plugin, key, value);
    }

    public void updateWindowBufferMaxValue(
            final String plugin, final String window, final int amount) {
        mConnections.get(mConnectionClutch).updateWindowBufferMaxValue(plugin, window, amount);
    }

    public void closeConnection(final String display) {
        Connection c = mConnections.get(display);
        if (c != null) {
            c.shutdown();
            mConnections.remove(display);
        }
    }

    public void windowShowing(final boolean show) {
        mWindowShowing = show;
    }

    public void dispatchLuaError(final String message) {
        mConnections.get(mConnectionClutch).dispatchLuaError(message);
    }

    public void addLink(final String path) {
        mConnections.get(mConnectionClutch).addLink(path);
    }

    public void deletePlugin(final String plugin) {
        mConnections.get(mConnectionClutch).deletePlugin(plugin);
    }

    public void setPluginEnabled(final String plugin, final boolean enabled) {
        mConnections.get(mConnectionClutch).setPluginEnabled(plugin, enabled);
    }

    @SuppressWarnings("rawtypes")
    public List getPluginsWithAliases() {
        ArrayList<String> list = new ArrayList<String>();
        Connection c = mConnections.get(mConnectionClutch);
        for (Plugin p : c.getPlugins()) {
            if (p.getSettings().getAliases().size() > 0) {
                list.add(p.getName());
            }
        }
        return list;
    }

    @SuppressWarnings("rawtypes")
    public List getPluginsWithTimers() {
        ArrayList<String> list = new ArrayList<String>();
        Connection c = mConnections.get(mConnectionClutch);
        for (Plugin p : c.getPlugins()) {
            if (p.getSettings().getTimers().size() > 0) {
                list.add(p.getName());
            }
        }
        return list;
    }

    public boolean isLinkLoaded(final String link) {
        boolean retval = mConnections.get(mConnectionClutch).isLinkLoaded(link);
        return retval;
    }

    public String getPluginPath(final String plugin) {
        String path = mConnections.get(mConnectionClutch).getPluginPath(plugin);
        if (path == null) {
            path = "";
        }
        return path;
    }

    public void dispatchLuaText(final String str) {
        mConnections.get(mConnectionClutch).dispatchLuaText(str);
    }

    public void callPluginFunction(final String plugin, final String function) {
        mConnections.get(mConnectionClutch).callPluginFunction(plugin, function);
    }

    public boolean isPluginInstalled(final String desired) {
        return mConnections.get(mConnectionClutch).isPluginInstalled(desired);
    }

    public void setShowRegexWarning(boolean state) {
        mConnections.get(mConnectionClutch).updateBooleanSetting("show_regex_warning", state);
    }

    public String getPluginOption(String plugin, String key) {
        return mConnections.get(mConnectionClutch).getPluginOptionValue(plugin, key);
    }

    /**
     * Dispatches data to the foreground window.
     *
     * @param data the data to send.
     */
    public final void sendRawDataToWindow(final byte[] data) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.rawDataIncoming(data);
        }
    }

    /**
     * Utility method for checking weather or not the window is showing.
     *
     * @return true if the window is showing, false if the window is not showing.
     */
    public final boolean isWindowConnected() {
        return mWindowShowing;
    }

    /**
     * The utility method to clear all buttons. I don't think that this is actually used, as this
     * code has been folded into the plugin.
     */
    public final void doClearAllButtons() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.clearAllButtons();
        }
    }

    /**
     * Implementation of the working code to set the current color debug mode for the foreground
     * window.
     *
     * @param iarg The color debug mode to enter.
     */
    public final void doExecuteColorDebug(final Integer iarg) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.executeColorDebug(iarg);
        }
    }

    /**
     * Working implementation of the dirty exit. That is to close the app without closing the
     * connections first.
     */
    public final void doDirtyExit() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.invokeDirtyExit();
        }
    }

    /**
     * Working implementation of the method that sets the fullscreen option in the foreground
     * window.
     *
     * @param set True for fullscreen, false for not fullscreen.
     */
    public final void doExecuteFullscreen(final boolean set) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setScreenMode(set);
        }
    }

    /**
     * Working implementation of the method that pops up the keyboard in the foreground window.
     *
     * @param text The text to add to the input bar.
     * @param dopopup True to pop up the keyboard.
     * @param doadd True to apppend text to the input bar.
     * @param doflush True to flush the input bar.
     * @param doclear True to clear the input bar.
     * @param doclose True to close the keyboard.
     */
    public final void doShowKeyboard(
            final String text,
            final boolean dopopup,
            final boolean doadd,
            final boolean doflush,
            final boolean doclear,
            final boolean doclose) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.showKeyBoard(text, dopopup, doadd, doflush, doclear, doclose);
        }
    }

    /**
     * Utility method to mark the foreground window settings as dirty, so they are reloaded at next
     * opportunity.
     */
    public final void markWindowsDirty() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.markWindowsDirty();
        }
    }

    /**
     * Utility method to mark the foreground window (non-window) settings as dirty so they are
     * reloaded.
     */
    public final void markSettingsDirty() {
        for (ConnectionCallback cb : mCallbacks) {
            cb.markSettingsDirty();
        }
    }

    /**
     * Implementation of the working method that sets the foreground window input bar's keep last
     * setting.
     *
     * @param value True for keeplast, false for clear when command is sent.
     */
    public final void dispatchKeepLast(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setKeepLast((boolean) value);
        }
    }

    /**
     * Implementation of the working method that sets the foreground window trigger editor regex
     * warning message state.
     *
     * @param value True for show warning, false for no warning.
     */
    public final void dispatchShowRegexWarning(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setRegexWarning((boolean) value);
        }
    }

    /**
     * Utility method that updates the internal lua libraries and files.
     *
     * @throws NameNotFoundException Thrown when the package manager doesn't find the right package
     *     (never happens).
     * @throws IOException Thrown when there is a disk i/o error.
     */
    private void updateLibs() throws NameNotFoundException, IOException {
        ApplicationInfo ai =
                this.getPackageManager()
                        .getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
        String dataDir = ai.dataDir;
        File libs = new File(dataDir + "/lua/lib");
        deleteRecursive(libs);
        File share = new File(dataDir + "/lua/share");
        deleteRecursive(share);

        File lualib = new File(dataDir + "/lua/lib/5.1/");
        if (!lualib.exists()) {
            lualib.mkdirs();
        }

        File luashare = new File(dataDir + "/lua/share/5.1/");
        if (!luashare.exists()) {
            luashare.mkdirs();
        }

        File luares = new File(dataDir + "/lua/share/5.1/res");
        if (!luares.exists()) {
            luares.mkdirs();
        }

        File luareshdpi = new File(dataDir + "/lua/share/5.1/res/hdpi");
        if (!luareshdpi.exists()) {
            luareshdpi.mkdirs();
        }
        File luaresmdpi = new File(dataDir + "/lua/share/5.1/res/mdpi");
        if (!luaresmdpi.exists()) {
            luaresmdpi.mkdirs();
        }
        File luaresldpi = new File(dataDir + "/lua/share/5.1/res/ldpi");
        if (!luaresldpi.exists()) {
            luaresldpi.mkdirs();
        }

        // copy new file.
        AssetManager assetManager = this.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("lib/lua/5.1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String filename : files) {
            // Log.e("asset name:","name:"+filename);
            InputStream in = assetManager.open("lib/lua/5.1/" + filename);
            File tmp = new File(lualib, filename);
            if (!tmp.exists()) {
                tmp.createNewFile();
            }
            OutputStream out = new FileOutputStream(tmp);
            copyfile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        }

        files = assetManager.list("share/lua/5.1");
        for (String filename : files) {
            if (!filename.equals("res")) {
                InputStream in = assetManager.open("share/lua/5.1/" + filename);
                File tmp = new File(luashare, filename);
                if (!tmp.exists()) {
                    tmp.createNewFile();
                }
                OutputStream out = new FileOutputStream(tmp);
                copyfile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            }
        }

        files = assetManager.list("share/lua/5.1/res/hdpi");
        for (String filename : files) {
            InputStream in = assetManager.open("share/lua/5.1/res/hdpi/" + filename);
            File tmp = new File(luareshdpi, filename);
            if (!tmp.exists()) {
                tmp.createNewFile();
            }
            OutputStream out = new FileOutputStream(tmp);
            copyfile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        }

        files = assetManager.list("share/lua/5.1/res/mdpi");
        for (String filename : files) {
            InputStream in = assetManager.open("share/lua/5.1/res/mdpi/" + filename);
            File tmp = new File(luaresmdpi, filename);
            if (!tmp.exists()) {
                tmp.createNewFile();
            }
            OutputStream out = new FileOutputStream(tmp);
            copyfile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        }

        files = assetManager.list("share/lua/5.1/res/ldpi");
        for (String filename : files) {
            InputStream in = assetManager.open("share/lua/5.1/res/ldpi/" + filename);
            File tmp = new File(luaresldpi, filename);
            if (!tmp.exists()) {
                tmp.createNewFile();
            }
            OutputStream out = new FileOutputStream(tmp);
            copyfile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        }
    }

    /**
     * Recursive folder deletion routine. Thanks android API for not having one.
     *
     * @param file The path to recursively delete.
     */
    private void deleteRecursive(final File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        } else {
            file.delete();
        }
    }

    /**
     * Copy file routine.
     *
     * @param in Input stream to read from.
     * @param out Output stream to write to.
     * @throws IOException Thrown when there is a problem.
     */
    private void copyfile(final InputStream in, final OutputStream out) throws IOException {
        byte[] buffer = new byte[FILE_COPY_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Implementation of the method that sets the preferred orientation for the foreground window.
     *
     * @param value Integer value, 1= portrait, 2=landscape, 3=auto
     */
    public final void doExecuteSetOrientation(final Integer value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setOrientation(value);
        }
    }

    /**
     * Implementation of the method that sets the keep screen on in the foreground window.
     *
     * @param value True, screen stays on, false screen does not stay on.
     */
    public final void doExecuteKeepScreenOn(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setKeepScreenOn(value);
        }
    }

    /**
     * Implementation of the method that sets the option for a fullscreen editor in the foreground
     * window.
     *
     * @param value True to use fullscreen editor, false to not.
     */
    public final void doExecuteFullscreenEditor(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setUseFullscreenEditor(value);
        }
    }

    /**
     * Implementation of the method that sets the preference for weather or not to use suggestions
     * in the foreground window editor.
     *
     * @param value True to use suggestions, false to not.
     */
    public final void doExecuteUseSuggestions(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setUseSuggestions(value);
        }
    }

    /**
     * Implementation of the method that sets the compatibility mode preference for the foreground
     * window's editor.
     *
     * @param value True to use compatibility mode, false to not.
     */
    public final void doExecuteCompatibilityMode(final Boolean value) {
        for (ConnectionCallback cb : mCallbacks) {
            cb.setCompatibilityMode(value);
        }
    }
}
