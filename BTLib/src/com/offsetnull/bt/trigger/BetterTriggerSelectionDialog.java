package com.offsetnull.bt.trigger;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.offsetnull.bt.R;
import com.offsetnull.bt.service.StellarService;
import com.offsetnull.bt.window.PluginFilterSelectionDialog;
import com.offsetnull.bt.window.BaseSelectionDialog;

public class BetterTriggerSelectionDialog extends PluginFilterSelectionDialog implements BaseSelectionDialog.UtilityToolbarListener {

	HashMap<String,TriggerData> dataMap;
	String[] sortedKeys;
	private boolean mShowWarning = true;
	
	public BetterTriggerSelectionDialog(Context context,
			StellarService service,boolean showWarning) {
		super(context, service);
		buildList();
		this.setToolbarListener(this);
		this.setTitle("TRIGGERS");
		mShowWarning = showWarning;
	}

	@Override
	public void onButtonPressed(View v, int row, int index) {
		TriggerData d = dataMap.get(sortedKeys[row]);
		Log.e("Trigger","trigger item selected for modification: "+d.getName());

		TriggerEditorDialog editor = new TriggerEditorDialog(BetterTriggerSelectionDialog.this.getContext(),d,service,triggerEditorDoneHandler,currentPlugin,mShowWarning);
		editor.show();
	}

	@Override
	public void onButtonStateChanged(ImageButton v, int row, int index, boolean statea) {
		TriggerData d = dataMap.get(sortedKeys[row]);
		boolean state = !d.isEnabled();
		d.setEnabled(state);

		if(currentPlugin.equals(MAIN_SETTINGS)) {
			service.setTriggerEnabled(state, d.getName());
		} else {
			service.setPluginTriggerEnabled(currentPlugin, state, d.getName());
		}
		
		if(state) {
			v.setImageResource(R.drawable.toolbar_toggleon_button);
			this.setItemMiniIcon(row, R.drawable.toolbar_mini_enabled);
		} else {
			v.setImageResource(R.drawable.toolbar_toggleoff_button);
			this.setItemMiniIcon(row, R.drawable.toolbar_mini_disabled);
		}
		Log.e("Trigger","trigger item selected for enable/disable: "+d.getName());
	}

	@Override
	public void onItemDeleted(int row) {
		TriggerData d = dataMap.get(sortedKeys[row]);
		

		if(currentPlugin.equals(MAIN_SETTINGS)) {
			service.deleteTrigger(d.getName());
		} else {
			service.deletePluginTrigger(currentPlugin, d.getName());
		}
		
		Log.e("Trigger","trigger item selected for delete: "+d.getName());
	}

	@Override
	public void onNewPressed(View v) {
		TriggerEditorDialog editor = new TriggerEditorDialog(BetterTriggerSelectionDialog.this.getContext(),null,service,triggerEditorDoneHandler,currentPlugin,mShowWarning);
		editor.show();
	}

	@Override
	public void onDonePressed(View v) {

		service.saveSettings();
		
	}
	
	@Override
	public void onHelp() {
		Intent web_help = new Intent(Intent.ACTION_VIEW,Uri.parse("http://bt.happygoatstudios.com/?view=triggers"));
		this.getContext().startActivity(web_help);
		//Log.e("Error","Help pressed.");
	}
	
	@Override
	public void onEnableAll() {
		Log.e("Error","Enable All pressed.");
	}
	
	private void buildList() {
		//HashMap<String,TriggerData> list = null;
		//pull the list down, clear out the items list, populate it, and call the superclass to reload the table.

		if(currentPlugin.equals(MAIN_SETTINGS)) {
			dataMap = (HashMap<String, TriggerData>) service.getTriggerData();
		} else {
			dataMap = (HashMap<String, TriggerData>) service.getPluginTriggerData(currentPlugin);
		}
		
		
		sortedKeys = new String[dataMap.size()];
		sortedKeys = dataMap.keySet().toArray(sortedKeys);
		//Arrays.sort(sortedKeys);
		Arrays.sort(sortedKeys,String.CASE_INSENSITIVE_ORDER);
		clearListItems();
		for(int i=0;i<sortedKeys.length;i++) {
			TriggerData data = dataMap.get(sortedKeys[i]);
			int resource = 0;
			if(data.isEnabled()) {
				resource = R.drawable.toolbar_mini_enabled;
			} else {
				resource = R.drawable.toolbar_mini_disabled;
			}
			this.addListItem(data.getName(), data.getPattern(),resource, data.isEnabled());
		}
		
		invalidateList();
		
	}
	
	@Override 
	public List<String> getPluginList() {
		List<String> foo = (List<String>)service.getPluginsWithTriggers();
		return foo;
	}
	
	@Override
	public void onOptionItemClicked(int row) {
		super.onOptionItemClicked(row);
		this.hideOptionsMenu();
		if(row < 2) return;
		buildList();	
		
	}

	@Override
	public void willShowToolbar(LinearLayout toolbar, int row) {
		//this will be called before the toolbar is shown, it will give the implementer the option to set up on/off lock/unlock etc.
		ImageButton b = (ImageButton)toolbar.getChildAt(1);
		toolbar.getChildAt(1);
		TriggerData data = dataMap.get(sortedKeys[row]);
		if(data.isEnabled()) {
			b.setImageResource(R.drawable.toolbar_toggleon_button);
		} else {
			b.setImageResource(R.drawable.toolbar_toggleoff_button);
		}
	}
	
private static class TriggerEditorDoneHandler extends Handler {
		private final WeakReference<BetterTriggerSelectionDialog> ref;
		TriggerEditorDoneHandler(BetterTriggerSelectionDialog outer) {
			super(Looper.getMainLooper());
			ref = new WeakReference<>(outer);
		}
		@Override
		public void handleMessage(Message msg) {
			BetterTriggerSelectionDialog outer = ref.get();
			if (outer == null) return;
			switch(msg.what) {
			case 100:
				TriggerData d = (TriggerData)msg.obj;
				outer.buildList();
				outer.scrollToSelection(d.getName());
				break;
			}
		}
	}
private final Handler triggerEditorDoneHandler = new TriggerEditorDoneHandler(this);

@Override
public void willHideToolbar(LinearLayout v, int row) {
	// TODO Auto-generated method stub
	
}
	
	

}
