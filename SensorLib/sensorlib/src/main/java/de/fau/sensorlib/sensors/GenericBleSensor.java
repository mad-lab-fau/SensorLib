/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
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
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;

import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

/**
 * Implementation of a generic BLE sensor device.
 */
public class GenericBleSensor extends AbstractSensor {

    protected final static String TAG = GenericBleSensor.class.getSimpleName();

    /**
     * Sensors available on the BLE device after discovery. Not available on class instantiation.
     */
    protected EnumSet<HardwareSensor> mAvailableSensors = mDeviceClass.getAvailableSensors();

    /**
     * Bluetooth device.
     */
    private BluetoothDevice mBtDevice;

    private enum BluetoothProfileState {
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_DISCONNECTING,
        STATE_UNKNOWN;

        public static BluetoothProfileState lookup(int state) {
            return (state >= values().length - 1) ? STATE_UNKNOWN : values()[state];
        }
    }

    private enum BluetoothGattStatus {
        GATT_SUCCESS(0x00),
        GATT_READ_NOT_PERMITTED(0x02),
        GATT_WRITE_NOT_PERMITTED(0x03),
        GATT_INSUFFICIENT_AUTHENTICATION(0x05),
        GATT_REQUEST_NOT_SUPPORTED(0x06),
        GATT_INVALID_OFFSET(0x07),
        GATT_INVALID_ATTRIBUTE_LENGTH(0x0D),
        GATT_INSUFFICIENT_ENCRYPTION(0x0F),
        GATT_FAILURE(0x101);

        private int status;

        BluetoothGattStatus(int status) {
            this.status = status;
        }

        public static BluetoothGattStatus lookup(int status) {
            for (BluetoothGattStatus gattStatus : BluetoothGattStatus.values()) {
                if (gattStatus.status == status) {
                    return gattStatus;
                }
            }
            return GATT_FAILURE;
        }
    }

    protected enum BleConnectionMode {
        MODE_DEFAULT,
        MODE_NILSPOD
    }

    private static final int MAX_MTU_SIZE = 247;


    /**
     * GATT client instance for the BLE connection.
     */
    protected BluetoothGatt mGatt;

    private BleGattAttributes.BodySenorLocation mBodyLocation;

    private BleConnectionMode mStateMachineMode = BleConnectionMode.MODE_DEFAULT;

    private boolean mHasBatteryMeasurement = false;


    @Override
    public boolean hasBatteryMeasurement() {
        // Overrides hardcoded value from KnownSensor
        return mHasBatteryMeasurement;
    }

    /**
     * Returns the Body Sensor Location.
     *
     * @return Body Sensor Location as String
     */
    public String getBodyLocation() {
        return BleGattAttributes.BodySenorLocation.getLocation(mBodyLocation);
    }

    protected BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : mServiceList) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }

        return null;
    }

    /**
     * Since BLE characteristics should be read sequentially, this thread-safe queue stores all the characteristic-read-requests.
     */
    protected ConcurrentLinkedQueue<BluetoothGattCharacteristic> mCharacteristicsReadRequests;

    /**
     * Since BLE descriptors should be written sequentially, this thread-safe queue stores all the descriptor-write-requests.
     */
    private ConcurrentLinkedQueue<BluetoothGattDescriptor> mDescriptorWriteRequests;

    /**
     * Since BLE characteristics should be written sequentially, this thread-safe queue stores all the characteristic-write-requests.
     */
    private ConcurrentLinkedQueue<BluetoothGattCharacteristic> mCharacteristicsWriteRequests;


    private ConcurrentLinkedQueue<BluetoothGattCharacteristic> mNotificationsList;

    /**
     * Store references to the sensor's services
     */
    private ArrayList<BluetoothGattService> mServiceList = new ArrayList<>();

    private boolean mWasDiscovered = false;

    private int mReconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 2;

    /**
     * GATT callback instance.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.d(TAG, "<" + getDeviceName() + "> BleStateChange: " + gatt + " < status: " + BluetoothGattStatus.lookup(status) + " – new state: " + BluetoothProfileState.lookup(newState) + " >");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // assign the custom name from the device
                    mDeviceName = gatt.getDevice().getName();
                    // discover provided services/sensors for this BLE device
                    mGatt.discoverServices();
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    sendDisconnected();
                    mGatt.close();
                    mGatt = null;
                }
            } else {
                if (getState() == SensorState.CONNECTING && mReconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    mReconnectAttempts++;
                    Log.e(TAG, BluetoothGattStatus.lookup(status) + ", attempting to reconnect (" + mReconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")...");
                    mGatt.connect();
                } else {
                    mGatt.close();
                    mGatt = null;
                    sendConnectionLost();
                }
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "<" + getDeviceName() + "> Gatt: " + gatt + "; " + mGatt);
                // find out what sensor flags can be set based on the provided services
                discoverSensor();
            } else {
                Log.d(TAG, "<" + getDeviceName() + "> onServicesDiscovered received: " + status);
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

            // request MTU update as soon as all read requests have been completed (sensor is considered 'connected' when receiving MTU callback)
            if (!isConnected() && mCharacteristicsReadRequests.isEmpty()) {
                mGatt.requestMtu(MAX_MTU_SIZE);
                return;
            }


            // Poll the request queue until it is empty or we find the next readable characteristic. Unreadable characteristics are removed and ignored.
            boolean ret = false;
            while (!ret) {
                boolean read = false;
                qc = mCharacteristicsReadRequests.poll();

                if (qc != null) {
                    read = mGatt.readCharacteristic(qc);

                }
                // request MTU update if queue is empty and there is no more characteristic to be read
                // otherwise request MTU update when last characteristic has been read (sensor is considered 'connected' when receiving MTU callback)
                if (!isConnected() && mCharacteristicsReadRequests.isEmpty() && !read) {
                    mGatt.requestMtu(MAX_MTU_SIZE);
                    break;
                }

                ret = (qc == null) || read;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            // Peek at the request queue and check if this was our request (which it always should be), then remove it from the queue.
            BluetoothGattCharacteristic qc = mCharacteristicsWriteRequests.peek();
            if (qc != null && qc.equals(characteristic)) {
                mCharacteristicsWriteRequests.poll();
            }

            onNewCharacteristicWrite(characteristic, status);

            if (mCharacteristicsWriteRequests.isEmpty()) {
                return;
            }

            // Poll the request queue until it is empty or we find the next writable characteristic. Unwriteable characteristics are removed and ignored.
            /*boolean ret = false;
            while (!ret) {
                boolean write = false;
                qc = mCharacteristicsWriteRequests.poll();

                if (qc != null) {
                    write = mGatt.writeCharacteristic(qc);
                }

                ret = (qc == null) || write;
            }*/

            boolean ret = false;
            while (!ret) {
                qc = mCharacteristicsWriteRequests.peek();

                if (qc == null) {
                    mCharacteristicsWriteRequests.poll();
                    ret = false;
                } else {
                    ret = mGatt.writeCharacteristic(qc);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            onNewCharacteristicValue(characteristic, true);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "<" + getDeviceName() + "> onDescriptorRead: " + BleGattAttributes.lookupDescriptor(descriptor.getUuid()) + " :: " + Arrays.toString(descriptor.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // Peek at the request queue and check if this was our request (which it always should be), then remove it from the queue.
            BluetoothGattDescriptor qd = mDescriptorWriteRequests.peek();
            if (qd != null && qd.equals(descriptor)) {
                mDescriptorWriteRequests.poll();
            }

            Log.d(TAG, "<" + getDeviceName() + "> onDescriptorWrite: " + BleGattAttributes.lookupCharacteristic(descriptor.getCharacteristic().getUuid()) + " -> " + BleGattAttributes.lookupDescriptor(descriptor.getUuid()) + " :: " + Arrays.toString(descriptor.getValue()));

            // All descriptors were written and removed from the queue
            if (mDescriptorWriteRequests.isEmpty() && descriptor.getValue().length > 0) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
                    onAllGattNotificationsEnabled();
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, descriptor.getValue())) {
                    onAllGattNotificationsDisabled();
                }
                return;
            }

            /*boolean ret = false;
            while (!ret) {
                qd = mDescriptorWriteRequests.peek();
                ret = qd == null || mGatt.writeDescriptor(qd);
            }*/

            // Peek at the request queue until it is empty or we find the next writable descriptor. Unwritable descriptors are removed and ignored.
            boolean ret = false;
            while (!ret) {
                qd = mDescriptorWriteRequests.peek();
                if (qd == null) {
                    mDescriptorWriteRequests.poll();
                    ret = false;
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
            // request higher connection priority
            // TODO commented out for testing purposes
            //mGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            if (mStateMachineMode == BleConnectionMode.MODE_DEFAULT) {
                // Sensor is now connected
                sendConnected();
            } else {
                // enable Gatt notifications
                enableGattNotifications();
            }
        }
    };


    public GenericBleSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        this(context, info.getDeviceName(), info.getDeviceAddress(), dataHandler);
    }

    public GenericBleSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        this(context, deviceName, deviceAddress, dataHandler, -1);
    }

    public GenericBleSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        super(context, deviceName, deviceAddress, dataHandler, desiredSamplingRate);
        mNotificationsList = new ConcurrentLinkedQueue<>();
    }

    public GenericBleSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate, BleConnectionMode stateMachineMode) {
        this(context, deviceName, deviceAddress, dataHandler, desiredSamplingRate);
        mStateMachineMode = stateMachineMode;
    }

    @Override
    public boolean connect() throws Exception {
        if (!super.connect()) {
            Log.e(TAG, "<" + getDeviceName() + "> BleSensor connect failed.");
            return false;
        }

        // connection already existed?
        if (mGatt != null) {
            if (!mGatt.connect()) {
                setState(SensorState.DISCONNECTED);
                Log.d(TAG, "<" + getDeviceName() + "> GATT not connected, returning...");
                return false;
            }
        }

        mBtDevice = BleSensorManager.findBtDevice(mDeviceAddress);
        if (mBtDevice == null) {
            setState(SensorState.INITIALIZED);
            return false;
        }

        // connect GATT
        mGatt = mBtDevice.connectGatt(mContext, false, mGattCallback);

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        mServiceList.clear();

        if (mGatt != null) {
            mGatt.disconnect();
        }
    }

    @Override
    public void startStreaming() {
        if (mNotificationsList == null) {
            return;
        }

        if (mStateMachineMode == BleConnectionMode.MODE_DEFAULT) {
            enableGattNotifications();
        }
    }

    public void enableGattNotifications() {
        for (BluetoothGattCharacteristic charac : mNotificationsList) {
            enableGattNotification(charac);
        }
    }


    public void disableGattNotifications() {
        for (BluetoothGattCharacteristic charac : mNotificationsList) {
            disableGattNotification(charac);
        }
    }

    @Override
    public void stopStreaming() {
        if (mStateMachineMode == BleConnectionMode.MODE_DEFAULT) {
            disableGattNotifications();
        }
    }


    /**
     * Called whenever the sensor is connected and not yet discovered previously.
     */
    private void discoverSensor() {
        if (mWasDiscovered) {
            return;
        }

        mWasDiscovered = true;
        mCharacteristicsReadRequests = new ConcurrentLinkedQueue<>();
        mCharacteristicsWriteRequests = new ConcurrentLinkedQueue<>();
        mDescriptorWriteRequests = new ConcurrentLinkedQueue<>();
        mNotificationsList = new ConcurrentLinkedQueue<>();
        mServiceList = new ArrayList<>();

        for (BluetoothGattService gattService : mGatt.getServices()) {
            // report the discovered service, maybe a child class want to implement its own check.
            onDiscoveredService(gattService);

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                onDiscoveredCharacteristic(gattService, gattCharacteristic);
            }
        }
    }

    /**
     * Checks whether the given characteristic should be enabled for notifications. Can be overriden by extended classes.
     *
     * @param c the characteristic which should be checked.
     * @return true if notifications should be enabled for this Characteristic.
     */
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        /*if (shouldUseHardwareSensor(HardwareSensor.HEART_RATE_SERVICE) && BleGattAttributes.HEART_RATE_MEASUREMENT.equals(c.getUuid())) {
            return true;
        } else if (shouldUseHardwareSensor(HardwareSensor.BLOOD_PRESSURE_SERVICE) && BleGattAttributes.BLOOD_PRESSURE_SERVICE.equals(c.getUuid())) {
            return true;
        } else*/

        return BleGattAttributes.BATTERY_LEVEL.equals(c.getUuid());
    }

    /**
     * Enables on-change notifications for the given characteristic.
     *
     * @param characteristic the characteristic for which to enable notifications.
     */
    private synchronized boolean enableGattNotification(BluetoothGattCharacteristic characteristic) {
        mGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor desc = characteristic.getDescriptor(BleGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);

        if (mGatt != null && desc != null) {
            desc.setValue(ENABLE_NOTIFICATION_VALUE);
            mDescriptorWriteRequests.add(desc);
            return mGatt.writeDescriptor(mDescriptorWriteRequests.peek());
            /*if (mDescriptorWriteRequests.size() == 1) {
                return mGatt.writeDescriptor(desc);
            }*/
        }
        return false;
    }

    /**
     * Disables on-change notifications for the given characteristic.
     *
     * @param characteristic the characteristic for which to disable notifications.
     */
    private synchronized boolean disableGattNotification(BluetoothGattCharacteristic characteristic) {
        if (mGatt != null) {
            mGatt.setCharacteristicNotification(characteristic, false);
            BluetoothGattDescriptor desc = characteristic.getDescriptor(BleGattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION);

            if (mGatt != null && desc != null) {
                desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mDescriptorWriteRequests.add(desc);
                return mGatt.writeDescriptor(mDescriptorWriteRequests.peek());
                /*if (mDescriptorWriteRequests.size() == 1) {
                    return mGatt.writeDescriptor(desc);
                }*/
            }
        }
        return false;
    }

    /**
     * Called for each service that is discovered.
     *
     * @param service the service that was discovered for this sensor.
     */
    protected void onDiscoveredService(BluetoothGattService service) {
        String name = BleGattAttributes.lookupService(service.getUuid());

        Log.d(TAG, getDeviceName() + " >> Service discovered: " + name);

        mServiceList.add(service);

        /*if (BleGattAttributes.HEART_RATE_SERVICE.equals(service.getUuid()) && shouldUseHardwareSensor(HardwareSensor.HEART_RATE_SERVICE)) {
            mAvailableSensors.add(HardwareSensor.HEART_RATE_SERVICE);
        } else if (BleGattAttributes.BLOOD_PRESSURE_SERVICE.equals(service.getUuid()) && shouldUseHardwareSensor(HardwareSensor.BLOOD_PRESSURE_SERVICE)) {
            mAvailableSensors.add(HardwareSensor.BLOOD_PRESSURE_SERVICE);
        }*/
    }

    /**
     * Called for each characteristic that is discovered.
     *
     * @param service        the service to which the reported characteristic belongs.
     * @param characteristic the characteristic that was discovered.
     */
    private void onDiscoveredCharacteristic(BluetoothGattService service, BluetoothGattCharacteristic characteristic) {
        String name = BleGattAttributes.lookupCharacteristic(characteristic.getUuid());

        // add characteristic to read-requests
        readCharacteristic(characteristic);

        if (shouldEnableNotification(characteristic)) {
            mNotificationsList.add(characteristic);
        }

        Log.d(TAG, getDeviceName() + " >>>>> Characteristic discovered: " + name + " [ " + Arrays.toString(characteristic.getValue()) + " ]");
    }


    protected void readCharacteristic(BluetoothGattCharacteristic c) {
        mCharacteristicsReadRequests.add(c);
        if (mGatt != null) {
            mGatt.readCharacteristic(mCharacteristicsReadRequests.peek());
        }
        /*if (mCharacteristicsReadRequests.size() == 1) {
            mGatt.readCharacteristic(c);
        }*/
    }

    protected boolean writeCharacteristic(BluetoothGattCharacteristic c, byte[] value) {
        c.setValue(value);
        c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mCharacteristicsWriteRequests.add(c);

        if (mGatt != null) {
            mGatt.writeCharacteristic(mCharacteristicsWriteRequests.peek());
        }
        /*if (mCharacteristicsWriteRequests.size() == 1) {
            return mGatt.writeCharacteristic(c);
        }*/

        return true;
    }

    /**
     * Called whenever a  characteristic has changed.
     *
     * @param characteristic the new GATT characteristic instance.
     * @param isChange       true if the value changed or false if it was read for the first/a single time.
     * @return true if the value was processed, false otherwise.
     */
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        // First we check conditions that are regularly encountered (whenever values change)
        if (BleGattAttributes.BATTERY_LEVEL.equals(characteristic.getUuid())) {
            mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            // Once battery level was read it means that battery measurement is available
            mHasBatteryMeasurement = true;
            sendNotification(SensorMessage.BATTERY_LEVEL_CHANGED);
            Log.d(TAG, "<" + getDeviceName() + "> Battery level: " + mBatteryLevel);
        }

        // the following are more or less one-time reads
        else if (BleGattAttributes.DEVICE_NAME.equals(characteristic.getUuid())) {
            mDeviceName = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Name: " + mDeviceName);
        } else if (BleGattAttributes.SERIAL_NUMBER_STRING.equals(characteristic.getUuid())) {
            mSerialNumber = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Serial number: " + mSerialNumber);
        } else if (BleGattAttributes.FIRMWARE_REVISION_STRING.equals(characteristic.getUuid())) {
            mFirmwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Firmware revision: " + mFirmwareRevision);
        } else if (BleGattAttributes.SOFTWARE_REVISION_STRING.equals(characteristic.getUuid())) {
            mSoftwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Software revision: " + mSoftwareRevision);
        } else if (BleGattAttributes.MANUFACTURER_NAME_STRING.equals(characteristic.getUuid())) {
            mManufacturer = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Manufacturer: " + mManufacturer);
        } else if (BleGattAttributes.MODEL_NUMBER_STRING.equals(characteristic.getUuid())) {
            mModelNumber = characteristic.getStringValue(0);
            Log.d(TAG, "<" + getDeviceName() + "> Model Number: " + mModelNumber);
        } else if (BleGattAttributes.SYSTEM_ID.equals(characteristic.getUuid())) {
            mSensorSystemID = BleGattAttributes.valueToInt64(characteristic);
            Log.d(TAG, "<" + getDeviceName() + "> Sensor System ID: " + mSensorSystemID);
        } else if (BleGattAttributes.BODY_SENSOR_LOCATION.equals(characteristic.getUuid())) {
            mBodyLocation = BleGattAttributes.BodySenorLocation.inferBodySensorLocation(characteristic.getValue()[0]);
            Log.d(TAG, "<" + getDeviceName() + "> Body location: " + BleGattAttributes.BodySenorLocation.getLocation(mBodyLocation));
        } else {
            return false;
        }
        return true;
    }

    protected void onNewCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "<" + getDeviceName() + "> onCharacteristicWrite: " + BleGattAttributes.lookupCharacteristic(characteristic.getUuid()) + " :: " + Arrays.toString(characteristic.getValue()) + " - success: " + (status == BluetoothGatt.GATT_SUCCESS));
    }

    protected void onAllGattNotificationsEnabled() {
        Log.d(TAG, "<" + getDeviceName() + "> onAllGattNotificationsEnabled");
        if (mStateMachineMode == BleConnectionMode.MODE_DEFAULT) {
            sendStartStreaming();
        } else {
            sendConnected();
        }
    }

    protected void onAllGattNotificationsDisabled() {
        Log.d(TAG, "<" + getDeviceName() + "> onAllGattNotificationsDisabled");
        if (mStateMachineMode == BleConnectionMode.MODE_DEFAULT) {
            sendStopStreaming();
        }
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        // Overrides hardcoded value from KnownSensor
        return mAvailableSensors;
    }
}
