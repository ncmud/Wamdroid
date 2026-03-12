package org.ncmud.mudwammer.window;

import android.content.Context;

import org.ncmud.mudwammer.service.StellarService;

import java.util.List;

public class PluginFilterSelectionDialog extends BaseSelectionDialog
        implements BaseSelectionDialog.OptionItemClickListener {

    protected StellarService service;
    public static final String MAIN_SETTINGS = "bt_main_settings";
    protected String currentPlugin = MAIN_SETTINGS;

    String[] pluginList;

    public PluginFilterSelectionDialog(Context context, StellarService service) {
        super(context);
        this.service = service;
        setOptionItemClickListener(this);
        List<String> rawList = this.getPluginList();
        if (rawList == null) return;
        pluginList = new String[rawList.size()];
        pluginList = rawList.toArray(pluginList);
        java.util.Arrays.sort(pluginList);
        // java.util.Arrays.sort(plugins);

        this.clearOptionItems();

        this.addOptionItem("Help", true);
        // this.addOptionItem("Enable All", true);

        this.addPluginFilterOptions();
    }

    protected void addPluginFilterOptions() {
        if (pluginList.length < 1) {
            // we only have 1 plugin (the standard one), so don't make anything.
            // but promote the help.
            this.promoteHelp();
            return;
        }
        this.addOptionDivider("Filter by plugin", false);
        this.addOptionItem("Main", false);
        for (int i = 0; i < pluginList.length; i++) {

            this.addOptionItem(pluginList[i], false);
        }
    }

    @Override
    public void onOptionItemClicked(int row) {
        // TODO Auto-generated method stub
        switch (row) {
            case 0:
                // Toast t = Toast.makeText(this.getContext(), "Help not implemented.",
                // Toast.LENGTH_LONG);
                // t.show();
                onHelp();
                break;
            // case 1:
            //	onEnableAll();
            //	Toast h = Toast.makeText(this.getContext(), "Enable all toggle not implemented.",
            // Toast.LENGTH_LONG);
            //	h.show();
            //	break;
            case 1:
                // divier
                break;
            case 2:
                currentPlugin = MAIN_SETTINGS;
                break;
            default:
                currentPlugin = pluginList[row - 3];
                break;
        }
    }

    public void onHelp() {}

    public void onEnableAll() {}

    public List<String> getPluginList() {
        // List<String> foo = (List<String>)service.getPluginsWithTriggers();
        return null;
    }
}
