package com.offsetnull.bt.service.function;

import com.offsetnull.bt.service.Connection;
import com.offsetnull.bt.service.ConnectionCommand;

public class BellCommand extends SpecialCommand {
    public BellCommand() {
        this.commandName = "dobell";
    }

    public Object execute(Object o, Connection c) {

        c.sendCommand(new ConnectionCommand.BellReceived());

        return null;
    }
}
