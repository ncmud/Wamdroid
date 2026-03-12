package org.ncmud.mudwammer.service.function;

import org.ncmud.mudwammer.service.Connection;

public class DirtyExitCommand extends SpecialCommand {
    public DirtyExitCommand() {
        this.commandName = "closewindow";
    }

    public Object execute(Object o, Connection c) {

        c.getService().doDirtyExit();
        return null;
    }
}
