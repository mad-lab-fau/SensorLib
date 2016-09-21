/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

/**
 * A background service that sensors can register to in order to keep them alive even while the calling App is pushed to the background in Android.
 */
public class DsSensorService extends Service {

    private static final String TAG = DsSensorService.class.getSimpleName();

    /**
     * List of sensors registered to the service.
     */
    private static ArrayList<DsSensor> mConnectedSensors = new ArrayList<>();
    private final Handler mHandler = new Handler();
    private Runnable mRunCheck = new Runnable() {
        @Override
        public void run() {
            // we check all five seconds whether there are still any sensors connected.
            if (mConnectedSensors.isEmpty()) {
                Log.d(TAG, "No more sensors on list... stopping SensorService.");
                stopSelf();
            } else {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, 5000);
            }
        }
    };

    /**
     * Attaches the given sensor to the service, keeping it alive.
     *
     * @param sensor Reference to the sensor that should be kept alive in the service.
     */
    public static void attachSensor(DsSensor sensor) {
        if (mConnectedSensors.contains(sensor))
            return;
        mConnectedSensors.add(sensor);
        Log.d(TAG, "Attached sensor: " + sensor);
    }

    /**
     * Detaches the given sensor from the service.
     *
     * @param sensor Reference to the sensor that should be detached from the service.
     */
    public static void detachSensor(DsSensor sensor) {
        mConnectedSensors.remove(sensor);
        Log.d(TAG, "Detached sensor: " + sensor);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating SensorService.");
        mHandler.removeCallbacks(mRunCheck);
        mHandler.postDelayed(mRunCheck, 5000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
