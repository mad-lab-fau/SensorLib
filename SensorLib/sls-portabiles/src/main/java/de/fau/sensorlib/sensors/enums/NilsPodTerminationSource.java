package de.fau.sensorlib.sensors.enums;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;

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

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace('_', ' '));
    }
}
