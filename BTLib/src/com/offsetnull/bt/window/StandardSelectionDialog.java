package com.offsetnull.bt.window;

import java.util.List;

import com.offsetnull.bt.service.StellarService;

import android.content.Context;
import android.view.View;

public class StandardSelectionDialog extends BaseSelectionDialog {
	
	protected StellarService service;
	
	
	public StandardSelectionDialog(Context context,StellarService service)  {
		super(context);
		this.service = service;
	}
	
	


}
