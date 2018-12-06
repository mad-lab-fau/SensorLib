/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Class that creates and delivers sensor objects users can then work with (connect, stream, etc.).
 */
public class SensorFactory {

    /**
     * Creates a new sensor object based on the provided sensor information.
     *
     * @param info Sensor info (containing device name, address, etc.)
     * @param context Android context
     * @param sensorDataProcessor Callback for sensor events
     * @return the sensor instance
     */
    public static AbstractSensor getSensorInstance(SensorInfo info, Context context, SensorDataProcessor sensorDataProcessor) {

        AbstractSensor sensor = null;
        try {
            // get constructors from the class instance based on KnownSensor type
            Constructor[] constructors = info.getDeviceClass().getClassType().getConstructors();
            for (Constructor constructor : constructors) {
                // find the constructor with 3 parameters
                if (constructor.getParameterTypes() != null && constructor.getParameterTypes().length == 3) {
                    // we found the right constructor, so create a new
                    sensor = (AbstractSensor) constructor.newInstance(context, info, sensorDataProcessor);
                    break;
                }
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            e.printStackTrace();
        }
        return sensor;
    }
}
