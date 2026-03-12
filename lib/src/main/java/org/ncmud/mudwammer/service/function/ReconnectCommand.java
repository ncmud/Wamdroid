package org.ncmud.mudwammer.service.function;

import org.ncmud.mudwammer.service.Colorizer;
import org.ncmud.mudwammer.service.Connection;

public class ReconnectCommand extends SpecialCommand {
    public ReconnectCommand() {
        this.commandName = "reconnect";
    }

    public Object execute(Object o, Connection c) {

        // myhandler.sendEmptyMessage(MESSAGE_RECONNECT);
        String msg =
                "\n"
                        + Colorizer.getRedColor()
                        + "Reconnecting . . ."
                        + Colorizer.getWhiteColor()
                        + "\n";
        c.sendDataToWindow(msg);
        return null;
    }
}
