package de.fau.sensorlib;/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

/**
 * Implements callback methods that can be used when multiple sensors are connected.
 */
public interface AllSensorCallback {

    /**
     * Called when all attached sensors are disconnected.
     */
    void onAllSensorsDisconnected();

    /**
     * Called when all attached sensors are connected.
     */
    void onAllSensorsConnected();

    /**
     * Called when all attached sensors are streaming.
     */
    void onAllSensorsStreaming();

}
