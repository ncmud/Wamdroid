package org.ncmud.mudwammer.button;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import org.ncmud.mudwammer.R;
import org.ncmud.mudwammer.service.StellarService;
import org.ncmud.mudwammer.validator.Validator;
import org.ncmud.mudwammer.window.MainWindow;

import java.util.List;

public class NewButtonSetEntryDialog extends Dialog {

    Handler dispatcher = null;
    StellarService service = null;

    public NewButtonSetEntryDialog(Context context, Handler reportto, StellarService theService) {
        super(context);
        dispatcher = reportto;
        service = theService;
    }

    public void onCreate(Bundle b) {

        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setBackgroundDrawableResource(R.drawable.dialog_window_crawler1);

        setContentView(R.layout.new_buttonset_entry);

        Button done = (Button) findViewById(R.id.newbuttonset_done);

        done.setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View v) {

                        EditText ed = (EditText) findViewById(R.id.newbuttonset_entry);

                        Validator checker = new Validator();
                        checker.add(ed, Validator.VALIDATE_NOT_BLANK, "Set name");

                        String result = checker.validate();
                        if (result != null) {
                            checker.showMessage(NewButtonSetEntryDialog.this.getContext(), result);
                            return;
                        }

                        // step 2 validation
                        List<String> list = null;
                        //
                        // list = service.getButtonSetNames();
                        //

                        for (String str : list) {
                            if (ed.getText().toString().equals(str)) {
                                checker.showMessage(
                                        NewButtonSetEntryDialog.this.getContext(),
                                        str + " is an existing button set.");
                                return;
                            }
                        }

                        // send a message to add and start working on the new button set
                        Message newset =
                                dispatcher.obtainMessage(
                                        MainWindow.MESSAGE_NEWBUTTONSET, ed.getText().toString());
                        dispatcher.sendMessage(newset);

                        NewButtonSetEntryDialog.this.dismiss();
                    }
                });
    }
}
