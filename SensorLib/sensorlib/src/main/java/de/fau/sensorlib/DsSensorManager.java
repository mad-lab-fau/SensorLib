/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * The SensorManager comprises several static methods that can be used for convenience tasks related
 * to sensor management, e.g. iterating available sensors or resolving sensor-addresses to names.
 */
public class DsSensorManager {

    private static final String TAG = DsSensorManager.class.getSimpleName();

    /**
     * method to list all available and connectable sensor within this framework.
     *
     * @return list of available sensor or null
     */
    public static List<SensorInfo> getConnectableSensors() {
        // Create sensor list
        ArrayList<SensorInfo> sensorList = new ArrayList<>();

        // Add internal sensor
        sensorList.add(SensorInfo.ANDROID_DEVICE_SENSORS);

        // Search for Bluetooth sensors
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            return sensorList;
        }
        Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
        // Get paired devices iterator
        // Loop over all paired devices
        for (BluetoothDevice device : pairedDevices) {
            // Get next device
            SensorInfo sensor = new SensorInfo(device.getName(), device.getAddress());
            if (sensor != null) {
                // check if it is already in our list
                /*boolean in = false;
                for (SensorInfo s : sensorList) {
                    if (s.getDeviceAddress() != null && sensor.getDeviceAddress() != null && s.getDeviceAddress().equals(sensor.getDeviceAddress())) {
                        in = true;
                        break;
                    }
                }
                if (!in)*/
                sensorList.add(sensor);
            }
        }

        // Return list of available sensor
        return sensorList;
    }

    /**
     * @param sensorClass the type/class of sensor that should be looked for.
     * @return the first found sensor of the given class that can be connected to.
     */
    public static SensorInfo getFirstConnectableSensor(KnownSensor sensorClass) {
        for (SensorInfo s : getConnectableSensors()) {
            if (s.getDeviceClass() == sensorClass) {
                return s;
            }
        }
        return null;
    }


    /**
     * @param address the address to look for.
     * @return a human readable name (if available) for a given device address or the address string if a name is not available.
     */
    public static String getNameForDeviceAddress(String address) {
        List<SensorInfo> sensors = getConnectableSensors();
        for (SensorInfo s : sensors) {
            if (s.getDeviceAddress().equals(address))
                return s.getName();
        }
        return address;
    }

    /**
     * @param name name of the sensor to look for.
     * @return the address of the first sensor where the given name matches the sensor name exactly. Returns <code>null</code> if no matching sensor was found.
     */
    public static String findFirstMatchingAddressForName(String name) {
        List<SensorInfo> sensors = getConnectableSensors();
        for (SensorInfo s : sensors) {
            if (s.getName().equals(name))
                return s.getDeviceAddress();
        }
        return null;
    }


    /**
     * Finds a BluetoothDevice based on an given address.
     *
     * @param deviceAddress the address for the device for which a BluetoothDevice should be returned.
     * @return the found BluetoothDevice, or null on error.
     */
    public static BluetoothDevice findBtDevice(String deviceAddress) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if (ba == null) {
            Log.d(TAG, "Failed to get default BT adapter.");
            return null;
        }

        if (!ba.isEnabled()) {
            Log.d(TAG, "Can't find device. Bluetooth is not enabled.");
            return null; // TODO: maybe we should throw some sensible exceptions here?
        }

        ba.cancelDiscovery();

        return ba.getRemoteDevice(deviceAddress);
    }


    /**
     * @param context the context.
     * @return true if BLE is available on this device.
     */
    public static boolean isBleSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    /**
     * Tries to enable Bluetooth radio on this device.
     *
     * @param activity the current activity.
     * @return the request code that can be used in onActivityResult for asynchronous feedback, or 0 if Bluetooth could not be enabled.
     * @throws Exception
     */
    public static int enableBluetooth(Activity activity) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null)
            throw new Exception("Bluetooth is not supported on this device.");
        if (adapter.isEnabled())
            return 0;
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        int requestCode = 20541;
        activity.startActivityForResult(enableBtIntent, requestCode);
        return requestCode;
    }

    public static final int PERMISSIONS_MISSING = -1;

    /**
     * Checks whether the permissions required for BT-LE access were previously granted to the calling App (Android >=6). If not, they are requested.
     *
     * @param activity some App activity. Receives the user response callback (onRequestPermissionsResult).
     * @return the ID that can be used to check for user response on the permission request (onRequestPermissionsResult).
     */
    public static int checkBtLePermissions(Activity activity, boolean requestPermissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }

        ArrayList<String> perms = new ArrayList<>(4);

        int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH);
        Log.d(TAG, "BLUETOOTH: " + (permissionCheck == PackageManager.PERMISSION_GRANTED));
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.BLUETOOTH);
            if (!requestPermissions)
                return PERMISSIONS_MISSING;
        }

        permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN);
        Log.d(TAG, "BLUETOOTH_ADMIN: " + (permissionCheck == PackageManager.PERMISSION_GRANTED));
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.BLUETOOTH_ADMIN);
            if (!requestPermissions)
                return PERMISSIONS_MISSING;
        }

        permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        Log.d(TAG, "ACCESS_COARSE_LOCATION: " + (permissionCheck == PackageManager.PERMISSION_GRANTED));
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (!requestPermissions)
                return PERMISSIONS_MISSING;
        }

        permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        Log.d(TAG, "ACCESS_FINE_LOCATION: " + (permissionCheck == PackageManager.PERMISSION_GRANTED));
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (!requestPermissions)
                return PERMISSIONS_MISSING;
        }

        int permissionCallbackId = 10562;
        if (requestPermissions && perms.size() > 0) {
            String[] sa = new String[perms.size()];
            for (int i = 0; i < perms.size(); i++) {
                sa[i] = perms.get(i);
            }
            ActivityCompat.requestPermissions(activity, sa, permissionCallbackId);
            return permissionCallbackId;
        }
        return 0;
    }

    private static Handler mBleScanHandler = new Handler();
    private static final long SCAN_PERIOD = 10500;
    private static BluetoothLeScanner mBleScanner;
    private static DsScanCallback mScanCallback;

    /**
     * Cancels all running BLE discovery scans.
     */
    public static synchronized void cancelRunningScans() {
        if (mScanCallback != null && mBleScanner != null) {
            mBleScanner.stopScan(mScanCallback);
            mScanCallback = null;
        }
    }

    /**
     * Search for BLE devices.
     *
     * @param callback a callback implementation that receives notifications for found sensors.
     */
    public static void searchBleDevices(Activity activity, final SensorFoundCallback callback) throws Exception {
        if (checkBtLePermissions(activity, false) == PERMISSIONS_MISSING) {
            throw new Exception("The app does not have sufficient Android permissions to list available BLE devices.");
        }
        mBleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        if (mBleScanner == null) {
            throw new Exception("BLE scanner unavailable.");
        }

        // set scan settings and filters
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        List<ScanFilter> filters = new ArrayList<>();

        // is an old scan still running? cancel it.
        cancelRunningScans();
        // create new scan callback
        mScanCallback = new DsScanCallback(callback);

        // post a delayed runnable to stop the BLE scan after SCAN_PERIOD.
        mBleScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "...Stopping BLE scan.");
                if (mScanCallback != null) {
                    mBleScanner.stopScan(mScanCallback);
                }
            }
        }, SCAN_PERIOD);

        // start the BLE scan
        Log.d(TAG, "Starting BLE scan for " + SCAN_PERIOD / 1000 + "s ...");
        mBleScanner.startScan(filters, settings, mScanCallback);
    }

    public static void searchBleDeviceByNames(ScanCallback callback, String[] deviceNames) {
        //Log.e(TAG, "Searching for BLE device...");
        List<ScanFilter> filterList = new ArrayList<>();
        for (String name : deviceNames) {
            filterList.add(new ScanFilter.Builder().setDeviceName(name).build());
        }

        BluetoothLeScanner bleScanner;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (bleScanner != null) {
                bleScanner.startScan(filterList, new ScanSettings.Builder().build(), callback);
            }
        }
    }

    public static void searchBleDeviceByUUIDs(ScanCallback callback, UUID[] uuids) {
        //Log.e(TAG, "Searching for BLE device...");
        List<ScanFilter> filterList = new ArrayList<>();
        for (UUID uuid : uuids) {
            filterList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build());
        }

        BluetoothLeScanner bleScanner;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (bleScanner != null) {
                bleScanner.startScan(filterList, new ScanSettings.Builder().build(), callback);
            }
        }
    }

    public static void cancelBleSearch(ScanCallback callback) {
        BluetoothLeScanner bleScanner;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            bleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (bleScanner != null) {
                bleScanner.stopScan(callback);
            }
        }
    }
}
