/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.util.Log;

import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Callback for communication Service -> Activity
 */
public interface SensorCallback {

    String TAG = SensorCallback.class.getSimpleName();

    void onScanResult(boolean sensorFound);

    /**
     * Is called when the Service sent a message that the sensor
     * started to stream.
     *
     * @param sensor The corresponding sensor
     */
    void onStartStreaming(AbstractSensor sensor);

    /**
     * Is called when the Service sent a message that the sensor
     * stopped to stream.
     *
     * @param sensor The corresponding sensor device
     */
    void onStopStreaming(AbstractSensor sensor);


    /**
     * Is called when the Service received a message (e.g. status message, etc. – not data!)
     * from the sensor.
     *
     * @param sensor  The corresponding sensor device
     * @param message The message, usually encoded as String
     */
    void onMessageReceived(AbstractSensor sensor, Object... message);

    /**
     * Is called when the Service sent a message that the sensor
     * is connected.
     *
     * @param sensor The corresponding sensor device
     */
    void onSensorConnected(AbstractSensor sensor);

    /**
     * Is called when the Service sent a message that the sensor
     * is disconnected.
     *
     * @param sensor The corresponding sensor device
     */
    void onSensorDisconnected(AbstractSensor sensor);

    /**
     * Is called when the Service sent a message that is has lost connection to the sensor.
     *
     * @param sensor The corresponding sensor device
     */
    void onSensorConnectionLost(AbstractSensor sensor);

    /**
     * Is called when the Service received data from the sensor.
     *
     * @param data Data received from the Service
     */
    void onDataReceived(SensorDataFrame data);


    default void onStartLogging(AbstractSensor sensor) {

    }

    default void onStopLogging(AbstractSensor sensor) {

    }

    /**
     * Called when all attached sensors are disconnected.
     */
    default void onAllSensorsDisconnected() {
        Log.d(TAG, "All sensors disconnected!");
    }

    /**
     * Called when all attached sensors are connected.
     */
    default void onAllSensorsConnected() {
        Log.d(TAG, "All sensors connected!");
    }

    /**
     * Called when all attached sensors are streaming.
     */
    default void onAllSensorsStreaming() {
        Log.d(TAG, "All sensors streaming!");
    }

    default void onAllSensorsLogging() {
        Log.d(TAG, "All sensors logging!");
    }

}
