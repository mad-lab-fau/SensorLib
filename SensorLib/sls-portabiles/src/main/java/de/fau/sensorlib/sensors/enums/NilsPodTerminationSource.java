package de.fau.sensorlib.sensors.enums;

public enum NilsPodTerminationSource {

    NO_MEMORY,
    BLE,
    DOCK,
    LOW_VOLTAGE,
    UNKNOWN;

    public static NilsPodTerminationSource inferTerminationSource(int termSource) {
        switch (termSource) {
            case 0x10:
                return NO_MEMORY;
            case 0x20:
                return BLE;
            case 0x40:
                return DOCK;
            case 0x80:
                return LOW_VOLTAGE;
            default:
                return UNKNOWN;
        }
    }
}
