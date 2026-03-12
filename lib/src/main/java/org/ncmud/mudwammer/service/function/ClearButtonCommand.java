package org.ncmud.mudwammer.service.function;

import org.ncmud.mudwammer.service.Connection;

public class ClearButtonCommand extends SpecialCommand {
    public ClearButtonCommand() {
        this.commandName = "clearbuttons";
    }

    public Object execute(Object o, Connection c) {
        c.getService().doClearAllButtons();
        return null;
    }
}
