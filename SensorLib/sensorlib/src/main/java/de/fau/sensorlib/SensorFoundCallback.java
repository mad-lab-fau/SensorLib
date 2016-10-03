/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import java.util.EnumSet;

/**
 * Base class for all sensor discovery callback instances.
 */
public abstract class SensorFoundCallback {
    /**
     * @param sensor the KnownSensor that was found.
     * @return true to continue scanning, false to cancel the scan.
     */
    public abstract boolean onKnownSensorFound(SensorInfo sensor);

    public boolean onKnownSensorFound(KnownSensor sensor, int rssi) {
        return true;
    }

    public void onSensorsSelected(EnumSet<DsSensor.HardwareSensor> selectedSensors) {
    }

    /**
     * @param name    Name of the found sensor
     * @param address (MAC) address of the found sensor
     * @return true to continue scanning, false to cancel the scan.
     */
    public boolean onUnknownSensorFound(String name, String address) {
        return true;
    }
}
