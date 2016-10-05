package de.fau.sensorlib;

/**
 * Exception class for all Exceptions thrown by the sensorlib.
 */

public class DsException extends Exception {

    public enum DsExceptionType {
        permissionsMissing("The app does not have sufficient Android permissions to list available BLE devices."),
        bleScannerError("BLE scanner unavailable."),
        btNotSupported("Bluetooth not supported on this device."),
        bleNotSupported("Bluetooth LE not supported on this device."),
        btNotActivated("Bluetooth disabled."),
        noSensorsSelected("No hardware sensors selected."),
        unknown("");

        private String msg;

        DsExceptionType(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }
    }

    private DsExceptionType mType;

    public DsException(DsExceptionType type) {
        super(type.getMessage());
        mType = type;
    }

    public DsException(String msg) {
        super(msg);
        mType = DsExceptionType.unknown;
        mType.msg = msg;
    }


}
