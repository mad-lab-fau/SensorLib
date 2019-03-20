/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
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
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Callback implementation for the BLE scan callback.
 */
public class BleScanCallback extends ScanCallback {

    private static final String TAG = BleScanCallback.class.getSimpleName();

    private SensorFoundCallback mSensorCallback;

    /**
     * List of device-addresses that were already reported in the callback. Used to avoid double or triple reports of the same device.
     */
    private List<String> mScannedAddresses = new ArrayList<>(20);

    public BleScanCallback(SensorFoundCallback sensorCallback) {
        mSensorCallback = sensorCallback;
    }

    @Override
    @CallSuper
    public synchronized void onScanResult(int callbackType, ScanResult result) {
        // check if we already reported this device in this scan iteration
        if (mScannedAddresses.contains(result.getDevice().getAddress())) {
            //Log.d(TAG, "Skipping BLE device: already discovered.");
            return;
        }
        mScannedAddresses.add(result.getDevice().getAddress());

        SparseArray<byte[]> manuData = null;
        //Log.d(TAG, "New BLE device: " + result.getDevice().getName() + "@" + result.getRssi());
        SensorInfo s;
        if (result.getScanRecord() != null) {
            manuData = result.getScanRecord().getManufacturerSpecificData();
            s = new SensorInfo(result.getDevice().getName(), result.getDevice().getAddress(), manuData);
            //Log.d(TAG, "Manufacturer Data: " + s.getDeviceClass() + ", " + Arrays.toString(manuData.valueAt(0)));
        } else {
            s = new SensorInfo(result.getDevice().getName(), result.getDevice().getAddress());
        }

        boolean ret;
        if (s.getDeviceClass() != null) {
            Log.d(TAG, "New BLE device: " + s.getDeviceName() + " @ " + result.getRssi());
            // call this method for the DsSensorPickerFragment,
            // because we need some information about the Bluetooth device
            ret = mSensorCallback.onKnownSensorFound(s, result.getRssi());
            // Logical OR because onKnownSensorFound(KnownSensor, BluetoothDevice)
            // always returns true. If we should stop scanning, then this method returns
            // false => ret == false.
            ret &= mSensorCallback.onKnownSensorFound(s);
        } else {
            ret = mSensorCallback.onUnknownSensorFound(result.getDevice().getName(), result.getDevice().getAddress());
        }

        if (!ret) {
            BleSensorManager.cancelRunningScans();
        }
    }
}
