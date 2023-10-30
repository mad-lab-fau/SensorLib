/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.fau.sensorlib.SensorException.SensorExceptionType;
import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.sensors.InternalSensor;


/**
 * Comprises several static methods that can be used for convenience tasks related
 * to sensor management, e.g. iterating available sensors or resolving sensor-addresses to names.
 */
public class BleSensorManager {

    private static final String TAG = BleSensorManager.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 0xBAAD;
    public static final int REQUEST_BT_PERMISSIONS = 0xF00D;
    public static final int REQUEST_STORAGE = 0x14FA;

    public static final int BT_ENABLED = 0;

    public static final int PERMISSIONS_GRANTED = 0;
    public static final int PERMISSIONS_MISSING = -1;

    private static Handler sBleScanHandler = new Handler();
    public static final long DEFAULT_SCAN_DURATION = 10000;
    private static BluetoothLeScanner sBleScanner;
    private static BleScanCallback sScanCallback;

    private static boolean sIsScanning = false;

    public static boolean isScanning() {
        return sIsScanning;
    }

    /**
     * method to list all available and connectable sensor within this framework.
     *
     * @return list of available sensor or null
     */
    public static List<SensorInfo> getConnectableSensors() {
        // Create sensor list
        ArrayList<SensorInfo> sensorList = new ArrayList<>();

        // Add internal sensor
        sensorList.add(new SensorInfo(InternalSensor.INTERNAL_SENSOR_NAME, InternalSensor.INTERNAL_SENSOR_ADDRESS));

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
            if (sensor.getDeviceClass() != null) {
                // check if it is already in our list
                /*boolean in = false;
                for (SensorInfo s : sensorList) {
                    if (s.getDeviceAddress() != null && sensor.getDeviceAddress() != null && s.getDeviceAddress().equals(sensor.getDeviceAddress())) {
                        in = true;
                        break;
                    }
                }
                if (!in)*/
                if (!sensorList.contains(sensor)) {
                    sensorList.add(sensor);
                }
            }
        }

        // Return list of available sensor
        return sensorList;
    }


    /**
     * @param sensor the type/class of sensor that should be looked for.
     * @return the first found sensor of the given class that can be connected to.
     */
    public static SensorInfo getFirstConnectableSensor(KnownSensor sensor) {
        for (SensorInfo s : getConnectableSensors()) {
            if (s.getDeviceClass() == sensor) {
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
                return s.getDeviceName();
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
            if (s.getDeviceName().equals(name))
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
    public static BluetoothDevice findBtDevice(String deviceAddress) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new SensorException(SensorExceptionType.btNotSupported);
        }

        if (!adapter.isEnabled()) {
            throw new SensorException(SensorExceptionType.btNotActivated);
        }

        adapter.cancelDiscovery();

        return adapter.getRemoteDevice(deviceAddress);
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
     * @return the request code that can be used in onActivityResult for asynchronous feedback, or 0 if Bluetooth is already enabled.
     * @throws SensorException If any error occurs during Bluetooth activation
     */
    public static int enableBluetooth(Activity activity) throws SensorException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new SensorException(SensorExceptionType.btNotSupported);
        }
        if (adapter.isEnabled()) {
            return BT_ENABLED;
        }
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        return REQUEST_ENABLE_BT;
    }

    /**
     * Checks whether the permissions required for BT-LE access were previously granted to the calling App (Android >=6). If not, they are requested.
     *
     * @param activity some App activity. Receives the user response callback (onRequestPermissionsResult).
     * @return the request code that can be used to check for user response on the permission request (onRequestPermissionsResult),
     * 0 if the permission does not need to be requested, or PERMISSIONS_MISSING if the permission should not be requested, but is missing.
     */
    public static int checkBtLePermissions(Activity activity, boolean requestPermissions) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return PERMISSIONS_GRANTED;
        }

        if (!isBleSupported(activity)) {
            throw new SensorException(SensorExceptionType.bleNotSupported);
        }
        final String[] PERMISSIONS_BT_LE;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PERMISSIONS_BT_LE =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    };
        } else {
            PERMISSIONS_BT_LE =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    };
        }

        // Android SDK: An app must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission in order to get results.
        // => one location permission is sufficient, check for that
        boolean locationEnabled = false;
        for (String permission : PERMISSIONS_BT_LE) {
            boolean granted = (ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED);
            Log.d(TAG, permission + ": " + granted);

            if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission) ||
                    Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                locationEnabled |= granted;
            }

            if (!granted) {
                if (!requestPermissions && !locationEnabled) {
                    return PERMISSIONS_MISSING;
                } else if (requestPermissions) {
                    ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_BT_PERMISSIONS);
                    return REQUEST_BT_PERMISSIONS;
                }
            }
        }

        return PERMISSIONS_GRANTED;
    }

    /**
     * Checks whether the specified permissions are already granted by the application and
     * (if desired) requests them.
     *
     * @param activity           Calling activity that receives the response callback (onRequestPermissionsResult)
     * @param permissions        String of permissions to check
     * @param requestPermissions true to request the permissions if not granted
     * @return the request code that can be used to check for user response on the permission request (onRequestPermissionsResult),
     * 0 if the permission does not need to be requested, or PERMISSIONS_MISSING if the permission should not be requested, but is missing.
     */
    public static int checkPermissions(Activity activity, String[] permissions, boolean requestPermissions, int requestCode) {
        boolean granted = true;
        for (String permission : permissions) {
            granted &= (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED);
            Log.d(TAG, permission + ": " + granted);
        }

        if (!granted) {
            if (requestPermissions) {
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
                return requestCode;
            } else {
                return PERMISSIONS_MISSING;
            }
        } else {
            return PERMISSIONS_GRANTED;
        }
    }

    /**
     * Cancels all running BLE discovery scans.
     */
    public static synchronized void cancelRunningScans() {
        if (sScanCallback != null && sBleScanner != null) {
            sBleScanner.stopScan(sScanCallback);
            sScanCallback = null;
        }
    }

    /**
     * Search for BLE devices.
     *
     * @param callback a callback implementation that receives notifications for found sensors.
     */
    public static void searchBleDevices(Context context, final SensorFoundCallback callback) throws SensorException {
        searchBleDevices(context, callback, DEFAULT_SCAN_DURATION);
    }

    /**
     * Search for BLE devices.
     *
     * @param callback a callback implementation that receives notifications for found sensors.
     */
    public static void searchBleDevices(Context context, final SensorFoundCallback callback, long scanDuration) throws SensorException {
        searchBleDevices(context, callback, new ArrayList<>(), scanDuration);
    }

    public static void searchBleDeviceByNames(Context context, final SensorFoundCallback callback, String[] deviceNames) throws SensorException {
        searchBleDeviceByNames(context, callback, deviceNames, DEFAULT_SCAN_DURATION);
    }

    public static void searchBleDeviceByNames(Context context, final SensorFoundCallback callback, String[] deviceNames, long scanDuration) throws SensorException {
        List<ScanFilter> filterList = new ArrayList<>();
        if (deviceNames != null) {
            for (String name : deviceNames) {
                filterList.add(new ScanFilter.Builder().setDeviceName(name).build());
            }
        }

        searchBleDevices(context, callback, filterList, scanDuration);
    }

    public static void searchBleDeviceByUUIDs(Context context, SensorFoundCallback callback, UUID[] uuids) throws SensorException {
        searchBleDeviceByUUIDs(context, callback, uuids, DEFAULT_SCAN_DURATION);
    }

    public static void searchBleDeviceByUUIDs(Context context, SensorFoundCallback callback, UUID[] uuids, long scanDuration) throws SensorException {
        List<ScanFilter> filterList = new ArrayList<>();
        if (uuids != null) {
            for (UUID uuid : uuids) {
                filterList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build());
            }
        }

        searchBleDevices(context, callback, filterList, scanDuration);
    }

    /**
     * Search for BLE devices.
     *
     * @param callback   a callback implementation that receives notifications for found sensors.
     * @param filterList list to filter scanned devices
     */
    private static void searchBleDevices(Context context, final SensorFoundCallback callback, List<ScanFilter> filterList, long scanPeriod) throws SensorException {
        //searchBleDeviceByNames(callback, null);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            sBleScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (sBleScanner == null) {
                throw new SensorException(SensorExceptionType.bleScannerError);
            }
        }

        // set scan settings and filters
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

        // is an old scan still running? cancel it.
        if (sIsScanning) {
            cancelRunningScans();
        }

        if (!checkLocationEnabled(context)) {
            throw new SensorException(SensorExceptionType.locationDisabled);
        }

        // create new scan callback
        sScanCallback = new BleScanCallback(callback);

        // start the BLE scan
        Log.d(TAG, "Starting BLE scan for " + scanPeriod / 1000 + " s ...");
        sIsScanning = true;

        sBleScanner.startScan(filterList, settings, sScanCallback);

        // post a delayed runnable to stop the BLE scan after scanPeriod.
        sBleScanHandler.postDelayed(() -> {
            if (sScanCallback != null) {
                Log.d(TAG, "...Stopping BLE scan.");
                sBleScanner.stopScan(sScanCallback);
                sIsScanning = false;
            }
        }, scanPeriod);
    }

    private static boolean checkLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
