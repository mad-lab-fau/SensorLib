/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlibutils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Implementation of a broadcast receiver that can be used in external applications.
 */
public class DataReceiver extends BroadcastReceiver {

    private static final String TAG = "SensorlibDataReceiver";

    private static DataReceiver mDataReceiver;

    public static double heartRate;
    public static double rTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        heartRate = intent.getDoubleExtra("hr", 0);
        rTime = intent.getDoubleExtra("tstamp", 0);
        Log.d(TAG, "received data broadcast: " + heartRate + "; " + rTime);
    }

    public static void createInstance() {
        if (mDataReceiver == null) {
            mDataReceiver = new DataReceiver();
        }
    }
}
