/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Implementation of the Simblee sensor.
 */
public class SimbleeSensor extends DsSensor {

    private static final String TAG = SimbleeSensor.class.getSimpleName();

    public static final String DEVICE_NAME = "Simblee";

    public final static String ACTION_CONNECTED =
            "com.simblee.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.simblee.ACTION_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.simblee.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.simblee.EXTRA_DATA";


    // ECG SERVICE
    public final static UUID UUID_SERVICE = UUID.fromString("00000FE84-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_RECEIVE = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_SEND = UUID.fromString("2d30c083-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_DISCONNECT = UUID.fromString("2d30c084-f39f-4ce6-923f-3484ea480596");
    public final static UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("000002902-0000-1000-8000-00805F9B34FB");

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;

    /**
     * GATT callback for the communication with the Bluetooth remote device
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();

            Log.e(TAG, "error: " + status + ", " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED) {
                 /*
                 * Once successfully connected, we must next discover all the
				 * services on the device before we can read and write their
				 * characteristics.
				 */
                if (mName.equals(name)) {
                    sendConnected();
                    sendNotification("Discovering services...");
                }

            } else if (status == BluetoothGatt.GATT_SUCCESS
                    && newState == BluetoothProfile.STATE_DISCONNECTED) {

                 /*
                 * If at any point we disconnect, send a message to clear the
				 * values out of the UI
				 */
                if (mName.equals(name)) {
                    sendDisconnected();
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * if there is a failure at any stage, simply disconnect
                 */
                if (mName.equals(name)) {
                    sendConnectionLost();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Log.e(TAG, "Simblee GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                        sendStartStreaming();
                    } else {
                        Log.e(TAG, "Simblee receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "Simblee receive characteristic not found!");
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mName.equals(gatt.getDevice().getName()) && UUID_RECEIVE.equals(characteristic.getUuid())) {
                    byte[] values = characteristic.getValue();
                    Log.d(TAG, "values: " + Arrays.toString(values));
                    sendNewData(new SimbleeDataFrame(SimbleeSensor.this, timestamp++, values[0]));
                }
            }
        }

        private long timestamp;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (gatt.getDevice().getName().equals(mName)) {
                byte[] values = characteristic.getValue();
                Log.d(TAG, "values: " + Arrays.toString(values));
                sendNewData(new SimbleeDataFrame(SimbleeSensor.this, timestamp++, values[0]));
            }
        }
    };


    public static class SimbleeDataFrame extends SensorDataFrame {

        public double value;
        public long timestamp;

        public SimbleeDataFrame(DsSensor sensor, long timestamp, double value) {
            super(sensor, timestamp);
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    public SimbleeSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor, dataHandler);
    }

    public SimbleeSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        // Previously connected device.  Try to reconnect.
        if (mDeviceAddress != null && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing BluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        BluetoothDevice device = DsSensorManager.findBtDevice(mDeviceAddress);
        if (device != null) {
            sendNotification("Connecting to... " + device.getName());
            sendConnecting();
            mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
            return true;
        }

        return false;
    }

    public boolean send(byte[] data) {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                mBluetoothGattService.getCharacteristic(UUID_SEND);

        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    @Override
    public void disconnect() {
        super.disconnect();
        Log.d(TAG, "disconnect");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public void startStreaming() {
        Log.d(TAG, "start streaming");
        mBluetoothGatt.discoverServices();
    }

    @Override
    public void stopStreaming() {
        Log.d(TAG, "stop streaming");
        mBluetoothGatt.disconnect();
        sendStopStreaming();
    }
}
