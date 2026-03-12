package org.ncmud.mudwammer.responder.ack;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import org.ncmud.mudwammer.responder.TriggerResponder;
import org.ncmud.mudwammer.service.Colorizer;
import org.ncmud.mudwammer.service.Connection;
import org.ncmud.mudwammer.timer.TimerData;
import org.ncmud.mudwammer.trigger.TriggerData;
import org.ncmud.mudwammer.window.TextTree;

import org.keplerproject.luajava.LuaState;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.ListIterator;

public class AckResponder extends TriggerResponder {

    private String ackWith;

    public AckResponder() {
        super(RESPONDER_TYPE.ACK);
        ackWith = "";
        this.setFireType(FIRE_WHEN.WINDOW_BOTH);
    }

    public AckResponder(RESPONDER_TYPE pType) {
        super(pType);
    }

    public AckResponder copy() {
        AckResponder tmp = new AckResponder();
        tmp.ackWith = this.ackWith;
        tmp.setFireType(this.getFireType());
        return tmp;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AckResponder)) return false;

        AckResponder test = (AckResponder) o;

        if (!test.getAckWith().equals(this.getAckWith())) return false;
        if (test.getFireType() != this.getFireType()) return false;
        return true;
    }

    Character cr = Character.valueOf((char) 13);
    Character lf = Character.valueOf((char) 10);
    String crlf = cr.toString() + lf.toString();

    @Override
    public boolean doResponse(
            Context c,
            TextTree tree,
            int lineNumber,
            ListIterator<TextTree.Line> iterator,
            TextTree.Line line,
            int start,
            int end,
            String matched,
            Object source,
            String displayname,
            String host,
            int port,
            int triggernumber,
            boolean windowIsOpen,
            Handler dispatcher,
            HashMap<String, String> captureMap,
            LuaState L,
            String name,
            String encoding) {
        if (windowIsOpen) {
            if (this.getFireType() == FIRE_WHEN.WINDOW_CLOSED
                    || this.getFireType() == FIRE_WHEN.WINDOW_NEVER) return false;
        } else {
            if (this.getFireType() == FIRE_WHEN.WINDOW_OPEN
                    || this.getFireType() == FIRE_WHEN.WINDOW_NEVER) return false;
        }

        Message msg = null;
        // Log.e("ACKRESPONDER","RESPONDING WITH: " + this.getAckWith());
        String xformed = AckResponder.this.translate(this.getAckWith(), captureMap);
        // msg = dispatcher.obtainMessage(StellarService.MESSAGE_SENDDATA,(this.getAckWith() +
        // crlf).getBytes("ISO-8859-1"));
        // TODO: make ack responder actually ack

        L.getGlobal("debug");
        L.getField(-1, "traceback");
        L.remove(-2);

        int ret = L.LloadString(xformed);
        if (ret != 0) {
            msg = dispatcher.obtainMessage(Connection.MESSAGE_SENDDATA_STRING, (xformed + crlf));
            dispatcher.sendMessage(msg);
            L.pop(2);
        } else {
            // successful compilation
            ret = L.pcall(0, 1, -2);
            if (ret != 0) {
                String str = null;
                if (source instanceof TimerData) {
                    str =
                            "Error in timer("
                                    + ((TimerData) source).getName()
                                    + "): "
                                    + L.getLuaObject(-1).getString();
                } else if (source instanceof TriggerData) {
                    str =
                            "Error in trigger("
                                    + ((TriggerData) source).getName()
                                    + "): "
                                    + L.getLuaObject(-1).getString();
                }

                dispatcher.sendMessage(
                        dispatcher.obtainMessage(
                                Connection.MESSAGE_PLUGINLUAERROR,
                                "\n"
                                        + Colorizer.getRedColor()
                                        + str
                                        + Colorizer.getWhiteColor()
                                        + "\n"));
                L.pop(1);
            }
            L.pop(1);
        }

        return false;
    }

    public void setAckWith(String ackWith) {
        if (ackWith == null) ackWith = "";
        this.ackWith = ackWith;
    }

    public String getAckWith() {
        return ackWith;
    }

    @Override
    public void saveResponderToXML(XmlSerializer out)
            throws IllegalArgumentException, IllegalStateException, IOException {
        AckResponderParser.saveResponderToXML(out, this);
    }

    /*@Override
    public void doResponse(Context c, String displayname, int triggernumber,
    		boolean windowIsOpen, Handler dispatcher,
    		HashMap<String, String> captureMap, LAUNCH_MODE mode) {
    	// TODO Auto-generated method stub

    }*/

}
