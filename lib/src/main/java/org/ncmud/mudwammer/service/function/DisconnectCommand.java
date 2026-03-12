package org.ncmud.mudwammer.service.function;

import org.ncmud.mudwammer.service.Colorizer;
import org.ncmud.mudwammer.service.Connection;

public class DisconnectCommand extends SpecialCommand {

    public DisconnectCommand() {
        this.commandName = "disconnect";
    }

    public Object execute(Object o, Connection c) {

        // myhandler.sendEmptyMessage(MESSAGE_DODISCONNECT);
        String msg =
                "\n" + Colorizer.getRedColor() + "Disconnected." + Colorizer.getWhiteColor() + "\n";
        c.sendDataToWindow(msg);
        return null;
    }
}
