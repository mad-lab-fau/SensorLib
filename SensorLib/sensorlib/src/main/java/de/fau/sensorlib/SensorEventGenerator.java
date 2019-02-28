/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import java.util.ArrayList;

import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Sensor event generator that channels state changes or sensor messages and broadcasts it to UI elements or other classes that add listeners to this class.
 */
public class SensorEventGenerator {

    private ArrayList<SensorEventListener> mEventListener;

    private SensorState mState;

    /**
     * Creates a new instance.
     */
    public SensorEventGenerator() {
        mEventListener = new ArrayList<>();
    }

    /**
     * Adds a new sensor event listener.
     *
     * @param listener The listener to be added
     */
    public void addSensorEventListener(SensorEventListener listener) {
        if (!mEventListener.contains(listener)) {
            mEventListener.add(listener);
        }
    }

    /**
     * Removes the sensor event listener.
     *
     * @param listener The listener to be removed
     */
    public void removeSensorEventListener(SensorEventListener listener) {
        mEventListener.remove(listener);
    }

    public boolean isSensorEventListenerRegistered(SensorEventListener listener) {
        return mEventListener.contains(listener);
    }

    /**
     * Sets a new global state and notifies all attached listeners about the state change.
     *
     * @param state
     */
    public void setState(SensorState state) {
        setState(state, true);
    }

    /**
     * Sets a new global state.
     *
     * @param state     the new state
     * @param broadcast true to notify attached listeners about the state change, false otherwise
     */
    public void setState(SensorState state, boolean broadcast) {
        mState = state;
        if (broadcast) {
            broadcastStateEvent(state);
        }
    }

    /**
     * Returns the global state.
     *
     * @return the global state
     */
    public SensorState getState() {
        return mState;
    }


    /**
     * Broadcasts a global sensor state change.
     *
     * @param state the sensor state
     */
    public void broadcastStateEvent(SensorState state) {
        broadcastStateEvent(null, state);
    }

    /**
     * Broadcasts a sensor state change of a specific sensor.
     *
     * @param sensor the sensor whose state changed
     * @param state  the sensor state
     */
    public void broadcastStateEvent(AbstractSensor sensor, SensorState state) {
        for (SensorEventListener listener : mEventListener) {
            listener.onSensorStateChange(sensor, state);
        }
    }

    /**
     * Broadcasts a global sensor message.
     *
     * @param message the sensor message
     */
    public void broadcastMessage(SensorMessage message) {
        broadcastMessage(null, message);
    }

    /**
     * Broadcasts a sensor message of a specific sensor
     *
     * @param sensor      the sensor where the message originated from
     * @param messageType Type of sensor message
     */
    public void broadcastMessage(AbstractSensor sensor, SensorMessage messageType) {
        broadcastMessage(sensor, messageType, null);
    }

    /**
     * Broadcasts a sensor message of a specific sensor
     *
     * @param sensor      the sensor where the message originated from
     * @param messageType Type of sensor message
     * @param message     An optional sensor message
     */
    public void broadcastMessage(AbstractSensor sensor, SensorMessage messageType, String message) {
        for (SensorEventListener listener : mEventListener) {
            listener.onSensorMessage(sensor, messageType, message);
        }
    }


}
