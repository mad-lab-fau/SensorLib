/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.fau.sensorlib.dataframe.BleBloodPressureMeasurement;
import de.fau.sensorlib.dataframe.BleHeartRateMeasurement;

/**
 * Implementation of a generic BLE device communication class.
 */
public class DsBleSensor extends DsSensor {

    protected final static String TAG = DsBleSensor.class.getSimpleName();

    /**
     * Sensors available on the BLE device after discovery. Not available on class instantiation.
     */
    private EnumSet<HardwareSensor> mAvailableSensors = mDeviceClass.getAvailableSensors();

    /**
     * Bluetooth device.
     */
    protected BluetoothDevice mBtDevice;

    /**
     * GATT client instance for the BLE connection.
     */
    protected BluetoothGatt mGatt;

    private String mSerialNumber;
    private String mManufacturer;
    private String mFirmwareRevision;
    private String mSoftwareRevision;
    private long mSensorSystemID;
    private DsGattAttributes.BodySenorLocation mBodyLocation;

    /**
     * 0-100 (%).
     */
    private int mBatteryLevel;
    private boolean mHasBatteryMeasurement = false;


    @Override
    public boolean hasBatteryMeasurement() {
        // Overrides hardcoded value from KnownSensor
        return mHasBatteryMeasurement;
    }

    @Override
    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    /**
     * Returns the Body Sensor Location.
     *
     * @return Body Sensor Location as String
     */
    public String getBodyLocation() {
        return DsGattAttributes.BodySenorLocation.getLocation(mBodyLocation);
    }

    public String getSerialNumber() {
        return mSerialNumber;
    }

    public String getManufacturer() {
        return mManufacturer;
    }

    /**
     * Since BLE characteristics should be read sequentially, this thread-safe queue stores all the characteristic-read-requests.
     */
    protected ConcurrentLinkedQueue<BluetoothGattCharacteristic> mCharacteristicsReadRequests;

    /**
     * Since BLE descriptors should be written sequentially, this thread-safe queue stores all the descriptor-write-requests.
     */
    protected ConcurrentLinkedQueue<BluetoothGattDescriptor> mDescriptorWriteRequests;

    protected ConcurrentLinkedQueue<BluetoothGattCharacteristic> mNotificationsList;

    /**
     * GATT callback instance.
     */
    protected BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.d(TAG, "BleStateChange: " + gatt + "; " + status + "; " + newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // assign the custom name from the device
                    mName = gatt.getDevice().getName();
                    sendConnected();

                    // discover provided services/sensors for this BLE device
                    mGatt.discoverServices();
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    sendDisconnected();
                }
            } else {
                sendConnectionLost();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Gatt: " + gatt + "; " + mGatt);
                // find out what sensor flags can be set based on the provided services
                discoverSensor();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            // Peek at the request queue and check if this was our request (which it always should be), then remove it from the queue.
            BluetoothGattCharacteristic qc = mCharacteristicsReadRequests.peek();
            if (qc != null && qc.equals(characteristic)) {
                mCharacteristicsReadRequests.poll();
            }

            onNewCharacteristicValue(characteristic, false);

            //Log.d(TAG, "chara read: " + DsGattAttributes.lookup(characteristic.getUuid()) + " :: " + Arrays.toString(characteristic.getValue()));

            // Poll the request queue until it is empty or we find the next readable characteristic. Unreadable characteristics are removed and ignored.
            boolean ret = false;
            while (!ret) {
                qc = mCharacteristicsReadRequests.poll();
                ret = (qc == null) || mGatt.readCharacteristic(qc);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "chara write: " + DsGattAttributes.lookup(characteristic.getUuid()) + " :: " + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            onNewCharacteristicValue(characteristic, true);
            //Log.d(TAG, "chara changed: " + DsGattAttributes.lookup(characteristic.getUuid()) + " :: " + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "desc read: " + DsGattAttributes.lookup(descriptor.getUuid()) + " :: " + Arrays.toString(descriptor.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // Peek at the request queue and check if this was our request (which it always should be), then remove it from the queue.
            BluetoothGattDescriptor qd = mDescriptorWriteRequests.peek();
            if (qd != null && qd.equals(descriptor)) {
                mDescriptorWriteRequests.poll();
            }

            Log.d(TAG, "desc write: " + DsGattAttributes.lookup(descriptor.getUuid()) + " :: " + Arrays.toString(descriptor.getValue()));

            // Poll the request queue until it is empty or we find the next writable descriptor. Unwritable descriptors are removed and ignored.
            boolean ret = false;
            while (!ret) {
                qd = mDescriptorWriteRequests.poll();
                if (qd == null) {
                    ret = true;
                } else {
                    ret = mGatt.writeDescriptor(qd);
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    public DsBleSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
    }

    public DsBleSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler);
    }

    /**
     * @param characteristic the new GATT characteristic instance.
     * @param isChange       true if the value changed or false if it was read for the first/a single time.
     * @return true if the value was processed, false otherwise.
     */
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        // First we check conditions that are regularly encountered (whenever values change)
        if (DsGattAttributes.BATTERY_LEVEL.equals(characteristic.getUuid())) {
            mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            // Once battery level was read it means that battery measurement is available
            mHasBatteryMeasurement = true;
            Log.d(TAG, "Battery level: " + mBatteryLevel);
        } else if (DsGattAttributes.HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BleHeartRateMeasurement hrm = new BleHeartRateMeasurement(characteristic, this);
            Log.d(TAG, "HR value: " + hrm.getHeartRate());
            sendNewData(hrm);
        } else if (DsGattAttributes.BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
            BleBloodPressureMeasurement bpm = new BleBloodPressureMeasurement(characteristic, this);
            Log.d(TAG, "Blood pressure value: " + bpm.getMeanArterialPressure());
        }

        // the following are more or less one-time reads
        else if (DsGattAttributes.DEVICE_NAME.equals(characteristic.getUuid())) {
            mName = characteristic.getStringValue(0);
            Log.d(TAG, "Name: " + mName);
        } else if (DsGattAttributes.SERIAL_NUMBER_STRING.equals(characteristic.getUuid())) {
            mSerialNumber = characteristic.getStringValue(0);
            Log.d(TAG, "Serial number: " + mSerialNumber);
        } else if (DsGattAttributes.FIRMWARE_REVISION_STRING.equals(characteristic.getUuid())) {
            mFirmwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, "Firmware revision: " + mFirmwareRevision);
        } else if (DsGattAttributes.SOFTWARE_REVISION_STRING.equals(characteristic.getUuid())) {
            mSoftwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, "Software revision: " + mSoftwareRevision);
        } else if (DsGattAttributes.MANUFACTURER_NAME_STRING.equals(characteristic.getUuid())) {
            mManufacturer = characteristic.getStringValue(0);
            Log.d(TAG, "Manufacturer: " + mManufacturer);
        } else if (DsGattAttributes.SYSTEM_ID.equals(characteristic.getUuid())) {
            mSensorSystemID = DsGattAttributes.valueToInt64(characteristic);
            Log.d(TAG, "Sensor System ID: " + mSensorSystemID);
        } else if (DsGattAttributes.BODY_SENSOR_LOCATION.equals(characteristic.getUuid())) {
            mBodyLocation = DsGattAttributes.BodySenorLocation.inferBodySensorLocation(characteristic.getValue()[0]);
            Log.d(TAG, "Body location: " + DsGattAttributes.BodySenorLocation.getLocation(mBodyLocation));
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean connect() throws Exception {
        if (!super.connect()) {
            Log.e(TAG, "DsSensor connect failed.");
            return false;
        }

        // Already covered by DsSensorManager.findBtDevice()
        /*mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Log.e(TAG, "No BT adapter.");
            setState(SensorState.INITIALIZED);
            return false;
        }*/

        // connection already existed?
        if (mGatt != null) {
            if (!mGatt.connect()) {
                setState(SensorState.DISCONNECTED);
                Log.d(TAG, "GATT not connected, returning...");
                return false;
            }
        }

        mBtDevice = DsSensorManager.findBtDevice(mDeviceAddress);
        if (mBtDevice == null) {
            setState(SensorState.INITIALIZED);
            return false;
        }

        sendConnecting();
        // connect GATT
        mGatt = mBtDevice.connectGatt(mContext, false, mGattCallback);

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (mGatt == null)
            return;
        mGatt.disconnect();
        mGatt.close();
        mGatt = null;
    }

    @Override
    public void startStreaming() {
        if (mNotificationsList == null)
            return;

        for (BluetoothGattCharacteristic charac : mNotificationsList) {
            enableGattNotifications(charac);
        }
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        for (BluetoothGattCharacteristic charac : mNotificationsList) {
            disableGattNotifications(charac);
        }
        mGatt.disconnect();
        sendStopStreaming();
    }

    /**
     * Enables on-change notifications for the given characteristic.
     *
     * @param characteristic the characteristic for which to enalbe notifications.
     */
    private boolean enableGattNotifications(BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor desc = characteristic.getDescriptor(DsGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);

        if (desc != null) {
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mDescriptorWriteRequests.add(desc);
            if (mDescriptorWriteRequests.size() == 1) {
                return mGatt.writeDescriptor(desc);
            }
        }
        return false;
    }

    /**
     * Disables on-change notifications for the given characteristic.
     *
     * @param characteristic the characteristic for which to disable notifications.
     */
    private boolean disableGattNotifications(BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(characteristic, false);
        BluetoothGattDescriptor desc = characteristic.getDescriptor(DsGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);

        if (desc != null) {
            desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mDescriptorWriteRequests.add(desc);
            if (mDescriptorWriteRequests.size() == 1) {
                return mGatt.writeDescriptor(desc);
            }
        }
        return false;
    }

    /**
     * Called for each service that is discovered.
     *
     * @param service the service that was discovered for this sensor.
     */
    private void onDiscoveredService(BluetoothGattService service) {
        String name = DsGattAttributes.lookup(service.getUuid());

        Log.d(TAG, mBtDevice.getName() + ">> Service discovered: " + name);

        if (DsGattAttributes.HEART_RATE.equals(service.getUuid()) && shouldUseHardwareSensor(HardwareSensor.HEART_RATE)) {
            mAvailableSensors.add(HardwareSensor.HEART_RATE);
        } else if (DsGattAttributes.BLOOD_PRESSURE.equals(service.getUuid()) && shouldUseHardwareSensor(HardwareSensor.BLOOD_PRESSURE)) {
            mAvailableSensors.add(HardwareSensor.BLOOD_PRESSURE);
        }
    }

    /**
     * Called for each characteristic that is discovered.
     *
     * @param service        the service to which the reported characteristic belongs.
     * @param characteristic the characteristic that was discovered.
     */
    private void onDiscoveredCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        String name = DsGattAttributes.lookup(characteristic.getUuid());

        // add characteristic to read-requests
        mCharacteristicsReadRequests.add(characteristic);
        if (mCharacteristicsReadRequests.size() == 1) {
            mGatt.readCharacteristic(characteristic);
        }

        if (shouldEnableNotification(characteristic)) {
            mNotificationsList.add(characteristic);
        }

        Log.d(TAG, mBtDevice.getName() + ">>>>> Characteristic discovered: " + name + " [ " + Arrays.toString(characteristic.getValue()) + " ]");
    }

    /**
     * Checks whether the given characteristic should be enabled for notifications. Can be overriden by extended classes.
     *
     * @param c the characteristic which should be checked.
     * @return true if notifications should be enabled for this Characteristic.
     */
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (shouldUseHardwareSensor(HardwareSensor.HEART_RATE) && DsGattAttributes.HEART_RATE_MEASUREMENT.equals(c.getUuid())) {
            return true;
        } else if (shouldUseHardwareSensor(HardwareSensor.BLOOD_PRESSURE) && DsGattAttributes.BLOOD_PRESSURE.equals(c.getUuid())) {
            return true;
        } else if (DsGattAttributes.BATTERY_LEVEL.equals(c.getUuid())) {
            return true;
        }
        return false;
    }

    private boolean mWasDiscovered = false;

    /**
     * Called whenever the sensor is connected and not yet discovered previously.
     */
    private void discoverSensor() {
        if (mWasDiscovered) {
            return;
        }

        mWasDiscovered = true;
        mCharacteristicsReadRequests = new ConcurrentLinkedQueue<>();
        mDescriptorWriteRequests = new ConcurrentLinkedQueue<>();
        mNotificationsList = new ConcurrentLinkedQueue<>();

        for (BluetoothGattService gattService : mGatt.getServices()) {
            // report the discovered service, maybe a child class want to implement its own check.
            onDiscoveredService(gattService);

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                onDiscoveredCharacteristic(gattService, gattCharacteristic);
            }
        }
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        // Overrides hardcoded value from KnownSensor
        return mAvailableSensors;
    }
}
