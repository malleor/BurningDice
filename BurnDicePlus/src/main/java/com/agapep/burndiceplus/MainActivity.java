package com.agapep.burndiceplus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.widget.Toast;
import us.dicepl.android.sdk.BluetoothManipulator;
import us.dicepl.android.sdk.DiceConnectionListener;
import us.dicepl.android.sdk.DiceController;
import us.dicepl.android.sdk.DiceResponseAdapter;
import us.dicepl.android.sdk.DiceResponseListener;
import us.dicepl.android.sdk.DiceScanningListener;
import us.dicepl.android.sdk.Die;
import us.dicepl.android.sdk.protocol.constants.Constants;
import us.dicepl.android.sdk.responsedata.*;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int[] developerKey = new int[] {0x83, 0xed, 0x60, 0x0e, 0x5d, 0x31, 0x8f, 0xe7};
    private static final String TAG = "DICEPlus";
    private SharedPreferences sp;
    private SharedPreferences.Editor spe;
    private String preferedDice = null;
    private Handler handler;

    private Die dicePlus;
    TextView TVResult;
    private long time;

    private Runnable drawScene = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "gameLoop");

            //Tutaj główna pętla aplikacji

            int loopTime = 300; //ten czas może się zmieniać. szybkość pętli.
            handler.postDelayed(drawScene, loopTime);
        }
    };

    private Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "gameLoop");

            //Tutaj główna pętla aplikacji

            int loopTime = 300; //ten czas może się zmieniać. szybkość pętli.
            time += loopTime;  //ustalanie nowego czasu
            if (time > 100000) { //warunek zakończenia gry
                handler.removeCallbacks(drawScene);
                handler.removeCallbacks(gameLoop);
            }
            else handler.postDelayed(gameLoop, loopTime);
        }
    };

    DiceScanningListener scanningListener = new DiceScanningListener() {
        @Override
        public void onNewDie(Die die) {
            Log.d(TAG, "New DICE+ found:" + die.getAddress());
            ((App)getApplication()).dies.add(die);
//            dicePlus = die;
             //selectDie(die);
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
        public void onConnectionEstablished(final Die die) {
            Log.d(TAG, "DICE+ Connected");

            DiceController.setMode(dicePlus, Constants.DieMode.DIE_MODE_NO_ROLL_ANIMATIONS);
//            DiceController.runStandardAnimation(die, Constants.LedFace.LED_1, 1, Constants.LedAnimationType.ANIMATION_ROLL_FAILED);
//            DiceController.runFadeAnimation(die, Constants.LedFace.LED_1, 1, 255, 0 ,190, 100, 100);
            DiceController.runBlinkAnimation(die, Constants.LedFace.LED_1, 1, 255, 0 ,190, 1000, 100, 10);
            // Signing up for roll events
            DiceController.subscribeRolls(dicePlus);
            DiceController.subscribeTouchReadouts(dicePlus);
            DiceController.subscribeTapReadouts(dicePlus);
            DiceController.subscribeFaceReadouts(dicePlus);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Ustanowiono Połączenie: " + die.getAddress(), Toast.LENGTH_SHORT).show();
                }
            });
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

            //Log.d(TAG, "Roll: " + rollData.face);
//            Log.d(TAG, "Roll map~: " + Integer.toBinaryString(((1 << (rollData.face-1))&(32))));

            final int face = rollData.face;
//            DiceController.runBlinkAnimation(dicePlus, (~(1 << (rollData.face-1)))&(63) , 1, 0, 255, 0, 200, 10, 0);
//            //DiceController.runBlinkAnimation(die, Constants.LedFace.LED_1, 100, 0, 255 ,100, 100, 100, 10);
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
            //Log.d(TAG, "Tap: " + tapData.x + " " + tapData.y + " " + tapData.z);

        }

        @Override
        public void onTouchReadout(Die die, TouchData data, Exception exception) {
            super.onTouchReadout(die, data, exception);
            DiceController.runBlinkAnimation(dicePlus, data.change_mask, 1, 255, 0, 0, 100, 1, 0);
//            Log.d(TAG, "Touch: " + data.current_state_mask + " " + data.change_mask + " " + data.timestamp);
//            StringBuilder b = new StringBuilder("Touch state:");
//            for(boolean i : TouchMaskAnalizer.getFaces(data)) {
//                if (i) b.append(1); else b.append(0); b.append(" ");
//            }
//            Log.d(TAG, b.toString());

        }

        @Override
        public void onFaceReadout(Die die, FaceData faceData, Exception exception) {
            super.onFaceReadout(die, faceData, exception);
            //Log.d(TAG, "Face: " + faceData.face);
            //DiceController.runBlinkAnimation(dicePlus, 1 << (faceData.face-1), 1, 255, 0, 0, 100, 1, 0);
        }
    };

    public void selectDice(View v) {
        ListDialog.show(MainActivity.this, ((App)getApplication()).dies, new ListDialog.OnDieSelected() {
            @Override
            public void onDieSelected(Die dia) {
                selectDie(dia);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
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

    public void selectDie(Die die) {
        DiceController.disconnectAllDice();
        dicePlus = die;
        spe.putString("prefered_dice", die.getAddress());
        spe.commit();
        DiceController.connect(dicePlus);
    }

    public void startGame(View v) {
        Toast.makeText(getBaseContext(), "startGame", Toast.LENGTH_LONG).show();
        time = 0;
        handler.post(gameLoop);
        handler.post(drawScene);
    }

}
