package com.agapep.burndiceplus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import us.dicepl.android.sdk.Die;

import java.util.ArrayList;

/**
 * Created by slovic on 01.03.14.
 */
public class ListDialog {

    public interface OnDieSelected {
        public void onDieSelected(Die dia);
    }

    public static void show(final Context c, final ArrayList<Die> list, final OnDieSelected selected) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                c);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Select One Dice:-");
        final ArrayAdapter<String> arrayAdapter =
            new ArrayAdapter<String>(c, android.R.layout.select_dialog_singlechoice,  dieListToArray(list));
        builderSingle.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         selected.onDieSelected(list.get(which));
                    }
                });
        builderSingle.show();
    }

    private static String[] dieListToArray(ArrayList<Die> dies) {
        String[] result = new String[dies.size()];
        for (int i = 0; i < dies.size(); ++i) {
            result[i] = dies.get(i).getAddress();
        }
        return result;
    }

}
