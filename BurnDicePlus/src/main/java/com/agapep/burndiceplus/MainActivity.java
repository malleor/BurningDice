package com.agapep.burndiceplus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import us.dicepl.android.sdk.BluetoothManipulator;
import us.dicepl.android.sdk.DiceConnectionListener;
import us.dicepl.android.sdk.DiceController;
import us.dicepl.android.sdk.DiceResponseAdapter;
import us.dicepl.android.sdk.DiceResponseListener;
import us.dicepl.android.sdk.DiceScanningListener;
import us.dicepl.android.sdk.Die;
import us.dicepl.android.sdk.responsedata.*;

public class MainActivity extends Activity {
    private static final int[] developerKey = new int[] {0x83, 0xed, 0x60, 0x0e, 0x5d, 0x31, 0x8f, 0xe7};
    private static final String TAG = "DICEPlus";
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private String preferedDice = null;
    private Die dicePlus;
    TextView TVResult;

    private java.util.Set<String> all_connected_things = new java.util.HashSet<String>();

    DiceScanningListener scanningListener = new DiceScanningListener() {
        @Override
        public void onNewDie(Die die) {
            String this_addr = die.getAddress();

            Log.d(TAG, "New DICE+ found: " + this_addr);
            dicePlus = die;

            all_connected_things.add(this_addr);
            String str = "";
            for(String addr : all_connected_things)
                str += addr + '\n';
            final String strf = str;
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TVResult.setText(strf);
                }
            });

            preferedDice = "88:78:9C:69:E9:8A";
            Log.d(TAG, "Preferred device is: " + preferedDice);

            if (preferedDice == null) {
                Log.d(TAG, "New DICE+ found: there is no prefered dice: " + this_addr);
                spe.putString("prefered_dice", die.getAddress());
                spe.commit();
                DiceController.connect(dicePlus);
            } else if (die.getAddress().equals(preferedDice)) {
                Log.d(TAG, "New DICE+ found: connect to prefered dice");
                DiceController.connect(dicePlus);
            }
        }

        @Override
        public void onScanStarted() {
            Log.d(TAG, "Scan Started");
        }

        @Override
        public void onScanFailed() {
            Log.d(TAG, "Scan Failed");
            BluetoothManipulator.startScan();
        }

        @Override
        public void onScanFinished() {
            Log.d(TAG, "Scan Finished");

            if(dicePlus == null) {
                BluetoothManipulator.startScan();
            }
        }
    };

    DiceConnectionListener connectionListener = new DiceConnectionListener() {
        @Override
        public void onConnectionEstablished(Die die) {
            Log.d(TAG, "DICE+ Connected");

            // Signing up for roll events
            DiceController.subscribeRolls(dicePlus);
            DiceController.subscribeTouchReadouts(dicePlus);
            DiceController.subscribeTapReadouts(dicePlus);
            DiceController.subscribeFaceReadouts(dicePlus);
        }

        @Override
        public void onConnectionFailed(Die die, Exception e) {
            Log.d(TAG, "Connection failed", e);

            dicePlus = null;

            BluetoothManipulator.startScan();
        }

        @Override
        public void onConnectionLost(Die die) {
            Log.d(TAG, "Connection lost");

            DiceController.unsubscribeTapReadouts(dicePlus);
            DiceController.unsubscribeTouchReadouts(dicePlus);
            DiceController.unsubscribeFaceReadouts(dicePlus);
            dicePlus = null;

            BluetoothManipulator.startScan();
        }
    };

    DiceResponseListener responseListener = new DiceResponseAdapter() {
        @Override
        public void onRoll(Die die, RollData rollData, Exception e) {
            super.onRoll(die, rollData, e);

            Log.d(TAG, "Roll: " + rollData.face);

            final int face = rollData.face;

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TVResult.setText(""+face);
                }
            });
        }

        @Override
        public void onTapReadout(Die die, TapData tapData, Exception exception) {
            super.onTapReadout(die, tapData, exception);
            Log.d(TAG, "Tap: " + tapData.x + " " + tapData.y + " " + tapData.z);

        }

        @Override
        public void onTouchReadout(Die die, TouchData data, Exception exception) {
            super.onTouchReadout(die, data, exception);
            Log.d(TAG, "Touch: " + data.current_state_mask + " " + data.change_mask + " " + data.timestamp);
            StringBuilder b = new StringBuilder("Touch state:");
            for(boolean i : TouchMaskAnalizer.getFaces(data)) {
                if (i) b.append(1); else b.append(0); b.append(" ");
            }
            Log.d(TAG, b.toString());

        }

        @Override
        public void onFaceReadout(Die die, FaceData faceData, Exception exception) {
            super.onFaceReadout(die, faceData, exception);
            Log.d(TAG, "Face: " + faceData.face);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getPreferences(MODE_PRIVATE);
        spe = sp.edit();
        preferedDice = sp.getString("prefered_dice", null);
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        TVResult = (TextView) findViewById(R.id.TVResult);

        // Initiating
        BluetoothManipulator.initiate(this);
        DiceController.initiate(developerKey);
        // Listen to all the state occurring during the discovering process of DICE+
        BluetoothManipulator.registerDiceScanningListener(scanningListener);

        // When connecting to DICE+ you get two responses: a good one and a bad one ;)
        DiceController.registerDiceConnectionListener(connectionListener);

        // Attaching to DICE+ events that we subscribed to.
        DiceController.registerDiceResponseListener(responseListener);

        // Scan for a DICE+
        BluetoothManipulator.startScan();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");

        // Unregister all the listeners
        DiceController.unregisterDiceConnectionListener(connectionListener);
        BluetoothManipulator.unregisterDiceScanningListener(scanningListener);
        BluetoothManipulator.cancelScan();
        DiceController.unregisterDiceResponseListener(responseListener);
        DiceController.disconnectDie(dicePlus);
        dicePlus = null;
    }


    static class TouchMaskAnalizer {
        public static boolean[] getFaces(TouchData data) {
            boolean[] bits = new boolean[6];
            for (int i = 5; i >= 0; i--) {
                if(((data.current_state_mask & ( 1 << i )) >> i) > 0)
                    bits[i] = true; else bits[i] = false;
            }
            return bits;
        }
    }

}
