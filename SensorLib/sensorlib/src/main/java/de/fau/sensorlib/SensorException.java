/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

/**
 * Exception class for all Exceptions thrown by the sensor lib.
 */
public class SensorException extends Exception {

    private SensorExceptionType mType;
    private int mErrorCode = 0;

    public enum SensorExceptionType {
        sensorStateError("Sensor state error."),
        readStateError("Error reading sensor state."),
        readConfigError("Error reading sensor config."),
        readHeaderError("Error reading session header."),
        hardwareSensorError("Error during hardware sensor initialization."),
        powerLossWarning("Sensor was terminated by power loss. Consider resetting the sensor before using."),
        permissionsMissing("The app does not have sufficient Android permissions to list available BLE devices."),
        locationDisabled("Location disabled. Please enable location services when scanning for BLE devices."),
        bleScannerError("BLE scanner unavailable."),
        btNotSupported("Bluetooth not supported on this device."),
        bleNotSupported("Bluetooth LE not supported on this device."),
        btNotActivated("Bluetooth disabled."),
        sensorNotResponding("Sensor not responding."),
        noSensorsSelected("No hardware sensors selected."),
        configError("Error configuring the sensor."),
        samplingRateError("Attempted to set negative or zero sampling rate."),
        sessionDownloadError("Error during session download."),
        noMemory("Sensor Memory full!"),
        maxNumSessions("Maximum number of sessions reached."),
        unknown("");

        private String msg;

        SensorExceptionType(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }
    }

    public SensorException(SensorExceptionType type) {
        super(type.getMessage());
        mType = type;
    }

    public SensorException(SensorExceptionType type, int errorCode) {
        super(type.getMessage());
        mType = type;
        mErrorCode = errorCode;
    }

    public SensorException(SensorExceptionType type, String message) {
        super(type.getMessage() + "\n" + message);
        mType = type;
    }

    public SensorExceptionType getExceptionType() {
        return mType;
    }

    public int getErrorCode() {
        return mErrorCode;
    }


}
