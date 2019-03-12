package de.fau.sensorlib.sensors.enums;

public enum NilsPodTerminationSource {

    NO_MEMORY,
    BLE,
    DOCK,
    LOW_VOLTAGE;

    public static NilsPodTerminationSource inferTerminationSource(int termState) {
        switch (termState) {
            case 0x10:
                return NO_MEMORY;
            case 0x20:
                return BLE;
            case 0x40:
                return DOCK;
            case 0x80:
                return LOW_VOLTAGE;
            default:
                return null;
        }
    }
}
