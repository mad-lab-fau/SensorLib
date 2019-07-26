/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import java.util.EnumSet;

import de.fau.sensorlib.enums.HardwareSensor;

/**
 * Base class for all sensor discovery callback instances.
 */
public abstract class SensorFoundCallback {

    /**
     * Called when a sensor known to the sensorlib was found.
     *
     * @param sensor the KnownSensor that was found
     * @return true to continue scanning, false to cancel the scan
     */
    public abstract boolean onKnownSensorFound(SensorInfo sensor);

    /**
     * Called when a sensor known to the sensorlib was found.
     *
     * @param sensor the KnownSensor that was found
     * @param rssi   the RSSI of the sensor
     * @return true to continue scanning, false to cancel the scan
     */
    public boolean onKnownSensorFound(SensorInfo sensor, int rssi) {
        return onKnownSensorFound(sensor);
    }

    /**
     * Called when a subset of Hardware sensors of the sensor device was selected.
     *
     * @param selectedSensors the set of selected Hardware sensors
     */
    public void onSensorsSelected(EnumSet<HardwareSensor> selectedSensors) {
    }

    /**
     * Called when a Bluetooth device unknown to the sensorlib was found.
     *
     * @param name    Name of the found sensor
     * @param address (MAC) address of the found sensor
     * @return true to continue scanning, false to cancel the scan.
     */
    public boolean onUnknownSensorFound(String name, String address) {
        return true;
    }
}
