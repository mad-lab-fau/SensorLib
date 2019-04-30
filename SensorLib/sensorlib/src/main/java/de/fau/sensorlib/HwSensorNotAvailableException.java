package de.fau.sensorlib;

import de.fau.sensorlib.enums.HardwareSensor;

public class HwSensorNotAvailableException extends RuntimeException {

    private HardwareSensor mHwSensor;

    public HwSensorNotAvailableException(HardwareSensor hwSensor) {
        super(hwSensor + " not available!");
    }
}
