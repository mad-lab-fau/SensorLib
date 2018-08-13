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

    public enum SensorExceptionType {
        permissionsMissing("The app does not have sufficient Android permissions to list available BLE devices."),
        bleScannerError("BLE scanner unavailable."),
        btNotSupported("Bluetooth not supported on this device."),
        bleNotSupported("Bluetooth LE not supported on this device."),
        btNotActivated("Bluetooth disabled."),
        noSensorsSelected("No hardware sensors selected."),
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

    public SensorExceptionType getExceptionType() {
        return mType;
    }


}
