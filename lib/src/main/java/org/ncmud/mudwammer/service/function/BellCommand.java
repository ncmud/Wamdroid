package org.ncmud.mudwammer.service.function;

import org.ncmud.mudwammer.service.Connection;
import org.ncmud.mudwammer.service.ConnectionCommand;

public class BellCommand extends SpecialCommand {
    public BellCommand() {
        this.commandName = "dobell";
    }

    public Object execute(Object o, Connection c) {

        c.sendCommand(new ConnectionCommand.BellReceived());

        return null;
    }
}
