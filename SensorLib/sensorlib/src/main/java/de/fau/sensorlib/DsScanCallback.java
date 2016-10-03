/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.support.annotation.CallSuper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Callback implementation for the BLE scan callback.
 */
public class DsScanCallback extends ScanCallback {

    private static final String TAG = DsScanCallback.class.getSimpleName();

    private SensorFoundCallback mSensorCallback;

    /**
     * List of device-addresses that were already reported in the callback. Used to avoid double or triple reports of the same device.
     */
    private List<String> mScannedAddresses = new ArrayList<>(20);

    public DsScanCallback(SensorFoundCallback sensorCallback) {
        mSensorCallback = sensorCallback;
    }

    @Override
    @CallSuper
    public synchronized void onScanResult(int callbackType, ScanResult result) {
        // check if we already reported this device in this scan iteration
        if (mScannedAddresses.contains(result.getDevice().getAddress())) {
            Log.d(TAG, "Skipping BLE device: already discovered.");
            return;
        }
        mScannedAddresses.add(result.getDevice().getAddress());

        Log.d(TAG, "New BLE device: " + result.getDevice().getName() + "@" + result.getRssi());


        SensorInfo s = new SensorInfo(result.getDevice().getName(), result.getDevice().getAddress());

        boolean ret;
        if (s.getDeviceClass() != null)
            ret = mSensorCallback.onKnownSensorFound(s);
        else
            ret = mSensorCallback.onUnknownSensorFound(result.getDevice().getName(), result.getDevice().getAddress());
        if (!ret) {
            DsSensorManager.cancelRunningScans();
        }
    }
}
