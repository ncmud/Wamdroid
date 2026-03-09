package com.offsetnull.bt.service;

import com.offsetnull.bt.service.function.SpecialCommand;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.timer.TimerData;

import android.content.Context;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerManager {

    public enum TimerAction {
        PLAY, PAUSE, RESET, INFO, STOP, NONE
    }

    private static final double ONE_THOUSAND_MILLIS = 1000.0;
    private static final double TOAST_MESSAGE_TOP_OFFSET = 50.0;

    private final TimerContext context;

    public TimerManager(TimerContext context) {
        this.context = context;
    }

    public void handleAction(final String name, final int silentFlag, final TimerAction action) {
        boolean found = false;
        Plugin host = null;
        if (context.getConnectionSettings().getSettings().getTimers().containsKey(name)) {
            host = context.getConnectionSettings();
            found = true;
        } else {
            for (Plugin p : context.getPlugins()) {
                if (p.getSettings().getTimers().containsKey(name)) {
                    host = p;
                    found = true;
                }
            }
        }
        boolean silent = false;
        if (silentFlag == 0) {
            silent = true;
        }

        if (!found) {
            context.dispatchNoProcess(
                SpecialCommand.getErrorMessage("Timer command error",
                    "No timer with name " + name + " found.").getBytes());
        } else {
            switch (action) {
            case PLAY:
                host.startTimer(name);
                if (!silent) {
                    toast("Timer " + name + " started.");
                }
                break;
            case PAUSE:
                host.pauseTimer(name);
                if (!silent) {
                    toast("Timer " + name + " paused.");
                }
                break;
            case RESET:
                host.resetTimer(name);
                if (!silent) {
                    toast("Timer " + name + " reset.");
                }
                break;
            case STOP:
                host.pauseTimer(name);
                host.resetTimer(name);
                if (!silent) {
                    toast("Timer " + name + " stopped.");
                }
                break;
            case INFO:
                TimerData t = host.getSettings().getTimers().get(name);
                if (t.isPlaying()) {
                    long now = SystemClock.elapsedRealtime();
                    long dur = now - t.getStartTime();
                    int sec = t.getSeconds() - (int) (dur / ONE_THOUSAND_MILLIS);
                    toast(name + ": " + sec + "s");
                } else {
                    if (t.getRemainingTime() != t.getSeconds()) {
                        int sec = t.getSeconds() - t.getRemainingTime();
                        toast("Timer " + name + " is paused, " + sec + " remain.");
                    } else {
                        toast("Timer " + name + " is not running.");
                    }
                }
                break;
            case NONE:
                break;
            default:
                break;
            }
        }
    }

    private void toast(final String str) {
        Context c = context.getContext();
        Toast t = Toast.makeText(c, str, Toast.LENGTH_SHORT);
        float density = c.getResources().getDisplayMetrics().density;
        t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, (int) (TOAST_MESSAGE_TOP_OFFSET * density));
        t.show();
    }

    /** The .timer special command. Parses ".timer action name [silent]"
     * and dispatches via Handler messages back to Connection. */
    public static class TimerCommand extends SpecialCommand {
        private final int mOrdinalGroupIndex = 3;
        private final int mSilent = 50;
        private final ArrayList<String> mTimerActions = new ArrayList<String>();

        public TimerCommand() {
            this.commandName = "timer";
            mTimerActions.add("play");
            mTimerActions.add("pause");
            mTimerActions.add("info");
            mTimerActions.add("reset");
            mTimerActions.add("stop");
        }

        @Override
        public Object execute(final Object o, final Connection c) {
            Pattern p = Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s*(\\S*)");
            Matcher m = p.matcher((String) o);

            if (m.matches()) {
                String action = m.group(1).toLowerCase(Locale.US);
                String ordinal = m.group(2);
                String silent = "";
                if (m.groupCount() > 2) {
                    silent = m.group(mOrdinalGroupIndex);
                }
                if (!mTimerActions.contains(action)) {
                    c.dispatchNoProcess(getErrorMessage("Timer action arguemnt " + action + " is invalid.", "Acceptable arguments are \"play\",\"pause\",\"reset\",\"stop\" and \"info\".").getBytes());
                    return null;
                }
                int domsg = mSilent;
                if (!silent.equals("")) {
                    domsg = 0;
                }

                if (action.equals("info")) {
                    c.getHandler().sendMessage(c.getHandler().obtainMessage(Connection.MESSAGE_TIMERINFO, ordinal));
                    return null;
                }
                if (action.equals("reset")) {
                    c.getHandler().sendMessage(c.getHandler().obtainMessage(Connection.MESSAGE_TIMERRESET, 0, domsg, ordinal));
                    return null;
                }
                if (action.equals("play")) {
                    c.getHandler().sendMessage(c.getHandler().obtainMessage(Connection.MESSAGE_TIMERSTART, 0, domsg, ordinal));
                    return null;
                }
                if (action.equals("pause")) {
                    c.getHandler().sendMessage(c.getHandler().obtainMessage(Connection.MESSAGE_TIMERPAUSE, 0, domsg, ordinal));
                    return null;
                }
                if (action.equals("stop")) {
                    c.getHandler().sendMessage(c.getHandler().obtainMessage(Connection.MESSAGE_TIMERSTOP, 0, domsg, ordinal));
                    return null;
                }
            } else {
                c.dispatchNoProcess(getErrorMessage("Timer command: \".timer " + (String) o + "\" is invalid.", "Timer function format \".timer action index [silent]\"\n"
                            + "Where action is \"play\",\"pause\",\"reset\" or \"info\".\nIndex is the timer index displayed in the timer selection list.").getBytes());
            }

            return null;
        }
    }
}
