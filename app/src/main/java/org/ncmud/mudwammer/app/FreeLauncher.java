package org.ncmud.mudwammer.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.ncmud.mudwammer.R;

public class FreeLauncher extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_launcher_layout);

        Intent launch = new Intent(this, org.ncmud.mudwammer.launcher.Launcher.class);
        launch.putExtra("LAUNCH_MODE", "org.ncmud.mudwammer");
        this.startActivity(launch);

        this.finish();
    }
}
