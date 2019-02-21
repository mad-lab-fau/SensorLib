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
        readStateError("Error reading sensor state."),
        hardwareSensorError("Error during hardware sensor initialization."),
        permissionsMissing("The app does not have sufficient Android permissions to list available BLE devices."),
        bleScannerError("BLE scanner unavailable."),
        btNotSupported("Bluetooth not supported on this device."),
        bleNotSupported("Bluetooth LE not supported on this device."),
        btNotActivated("Bluetooth disabled."),
        sensorNotResponding("Sensor not responding."),
        noSensorsSelected("No hardware sensors selected."),
        configError("Error configuring the sensor."),
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
        super(message);
        mType = type;
    }

    public SensorExceptionType getExceptionType() {
        return mType;
    }

    public int getErrorCode() {
        return mErrorCode;
    }


}
