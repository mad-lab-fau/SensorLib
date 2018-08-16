/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.util.Log;
import android.util.SparseArray;

import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.enums.SensorState;

/**
 * Basic sensor information that is used to describe all sensors and handle sensors not constructed (yet).
 */
public class SensorInfo {

    private static final String TAG = SensorInfo.class.getSimpleName();

    /**
     * The state the sensor is currently in.
     */
    private SensorState mSensorState = SensorState.UNDEFINED;

    /**
     * The descriptive name of this sensor. Should be Human-readable and Human-understandable.
     */
    protected String mName = "Unknown";

    /**
     * The address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    protected String mDeviceAddress = "n/a";

    /**
     * The class of the device, if it is known and implemented in the sensorlib.
     */
    protected KnownSensor mDeviceClass = null;


    protected SparseArray<byte[]> mManufacturerData = null;


    /**
     * @return the device name.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the address of a given implementation identified by this enum instance.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    /**
     * @return the sensor class for this sensor.
     */
    public KnownSensor getDeviceClass() {
        return mDeviceClass;
    }

    public SparseArray<byte[]> getManufacturerData() {
        return mManufacturerData;
    }

    /**
     * Changes the sensor state.
     *
     * @param newState the new state for the sensor.
     */
    protected void setState(SensorState newState) {
        Log.d(TAG, "<" + mName + "> " + mSensorState + " --> " + newState);
        mSensorState = newState;
    }

    /**
     * @return the current sensor state.
     */
    public SensorState getState() {
        return mSensorState;
    }


    /**
     * @return true if the sensor is connected, false otherwise.
     */
    public boolean isInitialized() {
        return (mSensorState.ordinal() >= SensorState.INITIALIZED.ordinal());
    }

    /**
     * @return true if the sensor is connected, false otherwise.
     */
    public boolean isConnected() {
        return (mSensorState.ordinal() >= SensorState.CONNECTED.ordinal());
    }

    /**
     * @return true if the sensor is streaming, false otherwise.
     */
    public boolean isStreaming() {
        return (mSensorState.ordinal() >= SensorState.STREAMING.ordinal());
    }

    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName), null);
    }

    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress, SparseArray<byte[]> manufacturerData) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName), manufacturerData);
    }

    /**
     * Default constructor.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     * @param deviceClass   class of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress, KnownSensor deviceClass) {
        this(deviceName, deviceAddress, deviceClass, null);
    }

    public SensorInfo(String deviceName, String deviceAddress, KnownSensor deviceClass, SparseArray<byte[]> manufacturerData) {
        mName = deviceName;
        mDeviceAddress = deviceAddress;
        mDeviceClass = deviceClass;
        mManufacturerData = manufacturerData;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SensorInfo && mDeviceAddress.equals(((SensorInfo) obj).getDeviceAddress());
    }

    @Override
    public String toString() {
        return getName() + ": <" + getState() + ">";
    }
}
