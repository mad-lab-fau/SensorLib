/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.enums;

/**
 * The states a Sensor can be in.
 */
public enum SensorState {

    /**
     * Sensor instance not created (yet)
     */
    UNDEFINED,
    /**
     * Sensor instance created
     */
    INITIALIZED,
    /**
     * Sensor disconnected
     */
    DISCONNECTED,
    /**
     * Connection to sensor lost
     */
    CONNECTION_LOST,
    /**
     * Sensor currently connecting
     */
    CONNECTING,
    /**
     * Sensor connected
     */
    CONNECTED,
    /**
     * Sensor streaming
     */
    STREAMING,
    /**
     * Sensor logging
     */
    LOGGING,
    /**
     * Virtual sensor simulating a real sensor
     */
    SIMULATING
}
