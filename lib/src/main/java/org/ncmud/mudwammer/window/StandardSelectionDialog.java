package org.ncmud.mudwammer.window;

import android.content.Context;

import org.ncmud.mudwammer.service.StellarService;

public class StandardSelectionDialog extends BaseSelectionDialog {

    protected StellarService service;

    public StandardSelectionDialog(Context context, StellarService service) {
        super(context);
        this.service = service;
    }
}
