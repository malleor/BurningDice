package com.agapep.burndiceplus;

import android.app.Application;
import us.dicepl.android.sdk.Die;

import java.util.ArrayList;

/**
 * Created by slovic on 01.03.14.
 */
public class App extends Application {
    ArrayList<Die> dies = new ArrayList<Die>();
    public boolean addDice(Die die) {
        for(int i=0;i<dies.size();++i) {
            if (dies.get(i).getAddress().equals(die.getAddress())) {
                dies.set(i,die);
                return false;
            }
        }
        dies.add(die);
        return true;
    }
}
