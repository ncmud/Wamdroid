package com.offsetnull.bt.window;

import android.content.Context;

import com.offsetnull.bt.service.StellarService;

public class StandardSelectionDialog extends BaseSelectionDialog {

    protected StellarService service;

    public StandardSelectionDialog(Context context, StellarService service) {
        super(context);
        this.service = service;
    }
}
