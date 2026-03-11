package com.offsetnull.bt.service;

import android.util.SparseArray;

import com.offsetnull.bt.responder.IteratorModifiedException;
import com.offsetnull.bt.responder.TriggerResponder;
import com.offsetnull.bt.responder.gag.GagAction;
import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.trigger.TriggerData;
import com.offsetnull.bt.window.TextTree;
import com.offsetnull.bt.window.TextTree.Line;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Manages trigger building, matching, and dispatch for a Connection. */
public class TriggerManager {

    private static final int TEN_MILLION = 10000000;
    private static final int TEN_THOUSAND = 10000;

    private static final String MAIN_WINDOW = "mainDisplay";

    /** ANSI Color code pattern. */
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\x1B\\x5B.+?m");

    /** ANSI Color code matcher. */
    private static final Matcher COLOR_MATCHER = COLOR_PATTERN.matcher("");

    /** Generic "match a line" pattern. */
    private static final Pattern LINE_PATTERN = Pattern.compile("^.*$", Pattern.MULTILINE);

    /** Line matching matcher. */
    private static final Matcher LINE_MATCHER = LINE_PATTERN.matcher("");

    private final TriggerContext context;

    private boolean mDirty = false;

    private final TreeSet<Range> mLineMap = new TreeSet<Range>(new RangeComparator());
    private final StringBuilder mTriggerBuilder = new StringBuilder();
    private final SparseArray<TriggerData> mSortedTriggerMap = new SparseArray<TriggerData>(0);
    private final SparseArray<Plugin> mTriggerPluginMap = new SparseArray<Plugin>(0);

    private String mMassiveTriggerString = null;
    private Pattern mMassivePattern = null;
    private Matcher mMassiveMatcher = null;

    private TextTree mWorking = null;
    private TextTree mFinished = null;

    public TriggerManager(TriggerContext context) {
        this.context = context;

        mWorking = new TextTree();
        mWorking.setLinkify(false);
        mWorking.setLineBreakAt(TEN_MILLION);
        mWorking.setMaxLines(TEN_THOUSAND);

        mFinished = new TextTree();
        mFinished.setLinkify(false);
        mFinished.setLineBreakAt(TEN_MILLION);
        mFinished.setMaxLines(TEN_THOUSAND);
    }

    /** Marks the trigger system as needing a rebuild on the next dispatch pass. */
    public final void setDirty() {
        mDirty = true;
    }

    /** Rebuilds the amalgamated trigger pattern from all plugins and connection settings. */
    public final void buildTriggerSystem() {
        if (context.getConnectionSettings() == null) {
            return;
        }
        mSortedTriggerMap.clear();
        mTriggerPluginMap.clear();
        int currentgroup = 1;
        mTriggerBuilder.setLength(0);
        boolean addseparator = false;
        ArrayList<TriggerData> tmp = context.getConnectionSettings().getSortedTriggers();
        if (tmp == null) {
            context.getConnectionSettings().sortTriggers();
            tmp = context.getConnectionSettings().getSortedTriggers();
        }
        if (tmp != null && tmp.size() > 0) {
            for (int i = 0; i < tmp.size(); i++) {
                TriggerData t = tmp.get(i);
                if (!(!t.isInterpretAsRegex() && t.getPattern().startsWith("%"))) {
                    if (t.isEnabled()) {
                        if (!addseparator) {
                            mTriggerBuilder.append("(");
                            if (!t.isInterpretAsRegex()) {
                                mTriggerBuilder.append("\\Q");
                            }
                            mTriggerBuilder.append(t.getPattern());
                            if (!t.isInterpretAsRegex()) {
                                mTriggerBuilder.append("\\E");
                            }
                            mTriggerBuilder.append(")");
                            addseparator = true;
                        } else {
                            mTriggerBuilder.append("|(");
                            if (!t.isInterpretAsRegex()) {
                                mTriggerBuilder.append("\\Q");
                            }
                            mTriggerBuilder.append(t.getPattern());
                            if (!t.isInterpretAsRegex()) {
                                mTriggerBuilder.append("\\E");
                            }
                            mTriggerBuilder.append(")");
                        }
                        mSortedTriggerMap.put(currentgroup, t);
                        mTriggerPluginMap.put(currentgroup, context.getConnectionSettings());
                        currentgroup += t.getMatcher().groupCount() + 1;
                    }
                }
            }
        }

        for (Plugin p : context.getPlugins()) {
            tmp = p.getSortedTriggers();
            if (tmp == null) {
                p.sortTriggers();
                tmp = p.getSortedTriggers();
            }
            if (tmp != null && tmp.size() > 0) {
                for (int i = 0; i < tmp.size(); i++) {
                    TriggerData t = tmp.get(i);
                    if (!(!t.isInterpretAsRegex() && t.getPattern().startsWith("%"))) {
                        if (t.isEnabled()) {
                            if (i == 0 && !addseparator) {
                                mTriggerBuilder.append("(");
                                if (!t.isInterpretAsRegex()) {
                                    mTriggerBuilder.append("\\Q");
                                }
                                mTriggerBuilder.append(t.getPattern());
                                if (!t.isInterpretAsRegex()) {
                                    mTriggerBuilder.append("\\E");
                                }
                                mTriggerBuilder.append(")");
                                addseparator = true;
                            } else {
                                mTriggerBuilder.append("|(");
                                if (!t.isInterpretAsRegex()) {
                                    mTriggerBuilder.append("\\Q");
                                }
                                mTriggerBuilder.append(t.getPattern());
                                if (!t.isInterpretAsRegex()) {
                                    mTriggerBuilder.append("\\E");
                                }
                                mTriggerBuilder.append(")");
                            }
                            mSortedTriggerMap.put(currentgroup, t);
                            mTriggerPluginMap.put(currentgroup, p);
                            currentgroup += t.getMatcher().groupCount() + 1;
                        }
                    }
                }
            }
        }
        mMassiveTriggerString = mTriggerBuilder.toString();
        mMassivePattern = Pattern.compile(mMassiveTriggerString, Pattern.MULTILINE);
        mMassiveMatcher = mMassivePattern.matcher("");
        mDirty = false;
    }

    /**
     * THE INCOMING DATA DISPATCH ROUTINE! Unicorns and puppies and all kinds of good things live
     * here.
     *
     * @param data The data to process.
     * @throws UnsupportedEncodingException Thrown when a string<==>byte[] conversion has a bad
     *     encoding provided.
     */
    public void dispatch(final byte[] data) throws UnsupportedEncodingException {
        byte[] raw = context.getProcessor().rawProcess(data);
        if (raw == null) {
            return;
        }

        TextTree buffer = null;
        for (WindowToken w : context.getWindowManager().getWindows()) {
            if (w.getName().equals(MAIN_WINDOW)) {
                buffer = w.getBuffer();
            }
        }

        TextTree.Color tmpcolor = buffer.getBleedColor();
        mWorking.setBleedColor(tmpcolor);
        mFinished.setBleedColor(tmpcolor);

        mWorking.addBytesImpl(raw);

        mWorking.setModCount(0);

        // strip the color out.
        COLOR_MATCHER.reset(new String(raw, context.getEncoding()));
        String stripped = COLOR_MATCHER.replaceAll("");

        if (mDirty) {
            buildTriggerSystem();
        }

        ListIterator<TextTree.Line> it =
                mWorking.getLines().listIterator(mWorking.getLines().size());
        mLineMap.clear();
        LINE_MATCHER.reset(stripped);
        boolean found = false;
        int lineNumber = mWorking.getLines().size() - 1;
        while (LINE_MATCHER.find()) {
            found = true;
            mLineMap.add(new Range(LINE_MATCHER.start(), LINE_MATCHER.end(), lineNumber));
            lineNumber = lineNumber - 1;
        }
        boolean keepEvaluating = true;
        lineNumber = mWorking.getLines().size() - 1;
        Line l = null;
        if (it.hasPrevious()) {
            l = it.previous();
        } else {
            return;
        }
        if (found) {
            boolean done = false;
            while (!done) {
                done = true;
                boolean rebuildTriggers = false;
                boolean replaceGagged = false;
                int gagloc = -1;
                mMassiveMatcher.reset(stripped);
                while (keepEvaluating && mMassiveMatcher.find()) {
                    int s = mMassiveMatcher.start();
                    int e = mMassiveMatcher.end() - 1;
                    String matched = mMassiveMatcher.group();
                    Range r = new Range(s, e, 0);
                    SortedSet<Range> tmp = mLineMap.tailSet(r);

                    int tmpline = tmp.first().getLine();
                    int tmpstart = s - tmp.first().getStart();
                    int tmpend = (e - 1) - tmp.first().getStart();
                    gagloc = tmp.first().getEnd();

                    int index = -1;
                    for (int i = 1; i <= mMassiveMatcher.groupCount(); i++) {
                        if (mMassiveMatcher.group(i) != null) {
                            index = i;
                            i = mMassiveMatcher.groupCount();
                        }
                    }

                    if (index > 0) {
                        // we have found a trigger. advance the line number to

                        TriggerData t = mSortedTriggerMap.get(index);
                        Plugin p = mTriggerPluginMap.get(index);

                        boolean gagged = false;
                        if (lineNumber > tmpline) {
                            int amount = lineNumber - tmpline;

                            for (int i = 0; i < amount; i++) {
                                if (it.hasPrevious()) {
                                    l = it.previous();
                                }
                            }
                            mWorking.setModCount(0);
                            lineNumber = tmpline;
                            if (it.hasNext()) {
                                lineNumber = tmpline;
                            }
                        } else if (tmpline > lineNumber) {
                            gagged = true;
                        }
                        if (t != null && t.isEnabled() && !gagged) {
                            context.getCaptureMap().clear();
                            for (int i = index; i <= (t.getMatcher().groupCount() + index); i++) {

                                context.getCaptureMap()
                                        .put(Integer.toString(i - index), mMassiveMatcher.group(i));
                            }
                            for (TriggerResponder responder : t.getResponders()) {
                                if (responder instanceof GagAction) {
                                    replaceGagged = true;
                                }
                                try {
                                    responder.doResponse(
                                            context.getService().getApplicationContext(),
                                            mWorking,
                                            lineNumber,
                                            it,
                                            l,
                                            tmpstart,
                                            tmpend,
                                            matched,
                                            t,
                                            context.getDisplay(),
                                            context.getHostName(),
                                            context.getPort(),
                                            StellarService.getNotificationId(),
                                            context.getService().isWindowConnected(),
                                            context.getHandler(),
                                            context.getCaptureMap(),
                                            p.getLuaState(),
                                            t.getName(),
                                            context.getEncoding());

                                    if (mDirty) {
                                        keepEvaluating = false;
                                        rebuildTriggers = true;
                                    }
                                } catch (IteratorModifiedException e1) {
                                    it = e1.getIterator();
                                    mWorking.setModCount(0);
                                    lineNumber = it.previousIndex();
                                    if (it.hasPrevious()) {
                                        l = it.previous();
                                    } else {
                                        keepEvaluating = false;
                                    }
                                }
                                if (mWorking.getLines().size() == 0) {
                                    keepEvaluating = false;
                                }
                            }
                        }
                    }
                    if (rebuildTriggers) {
                        break;
                    }
                }
                if (rebuildTriggers) {
                    mWorking.setModCount(0);
                    done = false;
                    keepEvaluating = true;
                    int e = mMassiveMatcher.end();

                    if (e != stripped.length()) {
                        if (replaceGagged) {
                            stripped = stripped.substring(gagloc + 1, stripped.length());
                        } else {
                            stripped = stripped.substring(e + 1, stripped.length());
                        }
                    }

                    if (lineNumber <= mWorking.getLines().size() - 1) {
                        while (mWorking.getLines().size() - 1 > lineNumber) {

                            Line tmp = mWorking.getLines().get(mWorking.getLines().size() - 1);
                            mWorking.getLines().remove(mWorking.getLines().size() - 1);
                            mFinished.appendLine(tmp);
                        }
                    }

                    buildTriggerSystem();

                    mLineMap.clear();
                    LINE_MATCHER.reset(stripped);
                    found = false;

                    lineNumber = mWorking.getLines().size() - 1;
                    while (LINE_MATCHER.find()) {
                        found = true;
                        mLineMap.add(
                                new Range(LINE_MATCHER.start(), LINE_MATCHER.end(), lineNumber));
                        lineNumber = lineNumber - 1;
                    }

                    lineNumber = mWorking.getLines().size() - 1;
                    if (lineNumber == -1) {
                        keepEvaluating = false;
                        done = true;
                    } else {
                        it = mWorking.getLines().listIterator(lineNumber + 1);
                        l = it.previous();
                    }
                }
            }
        }

        ListIterator<TextTree.Line> finisher =
                mWorking.getLines().listIterator(mWorking.getLines().size());
        while (finisher.hasPrevious()) {
            mFinished.appendLine(finisher.previous());
        }

        mWorking.empty();
        mFinished.updateMetrics();

        byte[] proc = mFinished.dumpToBytes(false);

        buffer.addBytesImpl(proc);
        context.sendBytesToWindow(proc);
    }

    /**
     * Updates the encoding on the internal working and finished TextTree instances.
     *
     * @param encoding The new encoding to set.
     */
    public void setEncoding(final String encoding) {
        mWorking.setEncoding(encoding);
        mFinished.setEncoding(encoding);
    }

    /** Utility class used for trigger processing. Maps a start and end value to a line number. */
    static class Range {
        private int mStart;
        private int mEnd;
        private int mLine;

        public Range(final int start, final int end, final int line) {
            this.mStart = start;
            this.mEnd = end;
            this.mLine = line;
        }

        public int getLine() {
            return mLine;
        }

        public int getStart() {
            return mStart;
        }

        public int getEnd() {
            return mEnd;
        }
    }

    /** Range class comparator. */
    static class RangeComparator implements Comparator<Range> {
        @Override
        public int compare(final Range a, final Range b) {
            if (b.mStart > a.mEnd && b.mEnd > a.mEnd) {
                return -1;
            }

            if (b.mStart < a.mStart && b.mEnd < a.mEnd) {
                return 1;
            }

            return 0;
        }
    }
}
