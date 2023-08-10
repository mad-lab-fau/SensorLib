/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.enums.SensorState;

/**
 * Basic sensor information that is used to describe all sensors and handle sensors not constructed (yet).
 */
public class SensorInfo implements Serializable {

    private static final String TAG = SensorInfo.class.getSimpleName();

    /**
     * The state the sensor is currently in.
     */
    private SensorState mSensorState = SensorState.UNDEFINED;


    protected static HashMap<String, Double> sAvailableSamplingRates = new LinkedHashMap<>();

    /**
     * The descriptive name of this sensor. Should be Human-readable and Human-understandable.
     */
    protected String mDeviceName;

    /**
     * The address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    protected String mDeviceAddress;

    /**
     * The class of the device, if it is known and implemented in the sensorlib.
     */
    protected KnownSensor mDeviceClass;

    protected double mSamplingRate;


    protected byte[] mManufacturerData;

    /**
     * Battery level in % (0-100).
     */
    protected int mBatteryLevel;

    protected boolean mIsCharging;

    protected int mNumRecordings;


    /**
     * @return a not necessarily unique, human readable name for this sensor.
     */
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * @return a not necessarily unique, human readable name for this sensor.
     * @deprecated use {@link SensorInfo#getDeviceName()} instead.
     */
    @Deprecated
    public String getName() {
        return getDeviceName();
    }

    /**
     * @return the address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
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

    public byte[] getManufacturerData() {
        return mManufacturerData;
    }

    /**
     * Returns the battery level.
     *
     * @return Battery level of the sensor in %.
     */
    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public boolean getChargingState() {
        return mIsCharging;
    }

    public void setChargingState(boolean isCharging) {
        mIsCharging = isCharging;
    }

    public int getNumRecordings() {
        return mNumRecordings;
    }

    /**
     * Changes the sensor state.
     *
     * @param newState the new state for the sensor.
     */
    protected void setState(SensorState newState) {
        if (newState != mSensorState) {
            SensorState tmp = mSensorState;
            mSensorState = newState;
            onStateChange(tmp, mSensorState);
        }
    }

    /**
     * @return the current sensor state.
     */
    public SensorState getState() {
        return mSensorState;
    }

    protected void onStateChange(SensorState oldState, SensorState newState) {
        Log.d(TAG, "<" + mDeviceName + "> " + oldState + " --> " + newState);
    }

    public static HashMap<String, Double> getAvailableSamplingRates() {
        return sAvailableSamplingRates;
    }

    public static boolean checkSamplingRateAvailable(double samplingRate) {
        return sAvailableSamplingRates.containsValue(samplingRate);
    }

    /**
     * @return the sampling rate for the current sensor connection in Hz.
     */
    public double getSamplingRate() {
        return mSamplingRate;
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

    public boolean isLogging() {
        return mSensorState == SensorState.LOGGING;
    }

    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName), -1, null);
    }


    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     * @param samplingRate  sampling rate of the sensor
     */
    public SensorInfo(String deviceName, String deviceAddress, double samplingRate) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName), samplingRate, null);
    }

    /**
     * Default constructor. The sensor class is inferred from the name or device address.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress, byte[] manufacturerData) {
        this(deviceName, deviceAddress, KnownSensor.inferSensorClass(deviceName), -1, manufacturerData);
    }

    /**
     * Default constructor.
     *
     * @param deviceName    name of the device.
     * @param deviceAddress address of the device.
     * @param deviceClass   class of the device.
     */
    public SensorInfo(String deviceName, String deviceAddress, KnownSensor deviceClass) {
        this(deviceName, deviceAddress, deviceClass, -1, null);
    }

    public SensorInfo(String deviceName, String deviceAddress, KnownSensor deviceClass, double samplingRate, byte[] manufacturerData) {
        mDeviceName = deviceName;
        mDeviceAddress = deviceAddress;
        mDeviceClass = deviceClass;
        mManufacturerData = manufacturerData;
        mSamplingRate = samplingRate;
        mBatteryLevel = BleManufacturerDataHelper.getBatteryLevel(deviceClass, mManufacturerData);
        mIsCharging = BleManufacturerDataHelper.getChargingState(deviceClass, mManufacturerData);
        mNumRecordings = BleManufacturerDataHelper.getNumberOfRecordings(deviceClass, mManufacturerData);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SensorInfo && mDeviceAddress.equals(((SensorInfo) obj).getDeviceAddress());
    }

    @NonNull
    @Override
    public String toString() {
        return getDeviceName() + ": <" + getState() + ">";
    }
}
