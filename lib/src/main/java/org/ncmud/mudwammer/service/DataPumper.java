/*
 * Copyright (C) Dan Block 2013
 */
package org.ncmud.mudwammer.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.ncmud.mudwammer.service.net.DataPumperBridge;
import org.ncmud.mudwammer.service.net.PumpEvent;
import org.ncmud.mudwammer.service.net.RealSocketIO;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Data pumper implementation. Delegates network I/O to coroutine-based
 * DataPumperLoop via {@link DataPumperBridge}, while preserving the public
 * API that {@link Connection} and {@link GMCPHandler} depend on.
 */
public class DataPumper {
    // Keep legacy message constants for any remaining references.
    public static final int MESSAGE_RETRIEVE = 100;
    public static final int MESSAGE_END = 102;
    public static final int MESSAGE_INITXFER = 103;
    public static final int MESSAGE_ENDXFER = 104;
    public static final int MESSAGE_COMPRESS = 105;
    public static final int MESSAGE_NOCOMPRESS = 106;
    public static final int MESSAGE_THROTTLE = 108;
    public static final int MESSAGE_NOTHROTTLE = 109;

    private static final int SOCKET_TIMEOUT = 14000;

    private final Handler mReportTo;
    private final String mHost;
    private final int mPort;

    private RealSocketIO mSocket;
    private DataPumperBridge mBridge;
    private volatile boolean mConnected = false;

    public DataPumper(final String host, final int port, final Handler useme) {
        this.mHost = host;
        this.mPort = port;
        this.mReportTo = useme;
    }

    /**
     * Starts the DataPumper. Replaces Thread.start().
     * Connects on a background thread, then launches the coroutine loop.
     */
    public final void start() {
        new Thread(this::init, "DataPumper-init").start();
    }

    /** Startup and initialization routine. */
    public final void init() {
        sendWarning(
                Colorizer.getBrightCyanColor()
                        + "Attempting connection to: "
                        + Colorizer.getBrightYellowColor()
                        + mHost + ":" + mPort + "\n"
                        + Colorizer.getBrightCyanColor()
                        + "Timeout set to 14 seconds."
                        + Colorizer.getWhiteColor() + "\n");

        InetAddress addr;
        try {
            addr = InetAddress.getByName(mHost);
        } catch (UnknownHostException e) {
            dispatchDialog("Unknown Host: " + e.getMessage());
            return;
        }

        String ip = addr.getHostAddress();
        if (!ip.equals(mHost)) {
            sendWarning(
                    Colorizer.getBrightCyanColor() + "Looked up: "
                            + Colorizer.getBrightYellowColor() + ip
                            + Colorizer.getBrightCyanColor() + " for "
                            + Colorizer.getBrightYellowColor() + mHost
                            + Colorizer.getWhiteColor() + "\n");
        }

        mSocket = new RealSocketIO(mHost, mPort);
        try {
            mSocket.connect(SOCKET_TIMEOUT);

            sendWarning(
                    Colorizer.getBrightCyanColor() + "Connected to: "
                            + Colorizer.getBrightYellowColor() + mHost
                            + Colorizer.getBrightCyanColor() + "!"
                            + Colorizer.getWhiteColor() + "\n");

            mConnected = true;

            mBridge = new DataPumperBridge(mSocket, this::dispatchEvent);
            mBridge.start();

            mReportTo.sendEmptyMessage(Connection.MESSAGE_CONNECTED);

        } catch (Exception e) {
            dispatchDialog("Connection error: " + e.getMessage());
        }
    }

    private void dispatchEvent(PumpEvent event) {
        if (event instanceof PumpEvent.DataReceived) {
            byte[] data = ((PumpEvent.DataReceived) event).getData();
            Message msg = mReportTo.obtainMessage(Connection.MESSAGE_PROCESS, data);
            synchronized (mReportTo) {
                mReportTo.sendMessage(msg);
            }
        } else if (event instanceof PumpEvent.DisconnectedByPeer) {
            sendWarning("\n" + Colorizer.getRedColor()
                    + "Connection terminated by peer."
                    + Colorizer.getWhiteColor() + "\n");
            mConnected = false;
            mReportTo.sendEmptyMessage(Connection.MESSAGE_TERMINATED_BY_PEER);
        } else if (event instanceof PumpEvent.Disconnected) {
            mConnected = false;
            mReportTo.sendEmptyMessage(Connection.MESSAGE_DISCONNECTED);
        } else if (event instanceof PumpEvent.MccpFatalError) {
            mConnected = false;
            mReportTo.sendEmptyMessage(Connection.MESSAGE_MCCPFATALERROR);
        } else if (event instanceof PumpEvent.Warning) {
            sendWarning(((PumpEvent.Warning) event).getText());
        } else if (event instanceof PumpEvent.DialogError) {
            dispatchDialog(((PumpEvent.DialogError) event).getMessage());
        }
    }

    public final void sendData(final byte[] data) {
        if (mBridge != null) {
            mBridge.getLoop().send(data);
        }
    }

    public final void sendWarning(final String str) {
        mReportTo.sendMessage(mReportTo.obtainMessage(Connection.MESSAGE_PROCESSORWARNING, str));
    }

    public final void startCompression(final byte[] trailingData) {
        if (mBridge != null) {
            mBridge.getLoop().startCompression(trailingData);
        }
    }

    public final void stopCompression() {
        if (mBridge != null) {
            mBridge.getLoop().stopCompression();
        }
    }

    public final boolean isConnected() {
        return mConnected;
    }

    public final void closeSocket() {
        mConnected = false;
        if (mSocket != null) {
            mSocket.close();
        }
    }

    /** Shuts down the coroutine scope and closes the socket. */
    public final void shutdown() {
        mConnected = false;
        if (mBridge != null) {
            mBridge.shutdown();
            mBridge = null;
        }
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * Waits for the pumper to finish. Replaces Thread.join().
     * With coroutines, shutdown() is sufficient — this is a no-op for compatibility.
     */
    public final void join() throws InterruptedException {
        // Coroutine scope cancellation is synchronous enough for our purposes.
    }

    /** @deprecated Use {@link #startCompression} directly. */
    public final Handler getHandler() {
        return null;
    }

    public final void corruptMe() {
        Log.w("DataPumper", "corruptMe() called but not supported in coroutine-based DataPumper");
    }

    public final void interruptSocket() {
        closeSocket();
    }

    private void dispatchDialog(final String str) {
        mReportTo.sendMessage(mReportTo.obtainMessage(Connection.MESSAGE_DODIALOG, str));
    }

    public static String toHex(final byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }
}
