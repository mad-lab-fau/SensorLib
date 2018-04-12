/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;


import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Base class for all data frames coming from a sensor.
 */
public class SensorDataFrame {

    /**
     * The sensor on which this data frame was generated.
     */
    protected AbstractSensor originatingSensor;

    /**
     * Timestamp in milliseconds when this data frame was generated on the sensor.
     */
    private double timestamp;

    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SensorDataFrame(AbstractSensor fromSensor, double timestamp) {
        originatingSensor = fromSensor;
        this.timestamp = timestamp;
    }

    /**
     * @return reference to the Sensor which generated this data frame.
     */
    public AbstractSensor getOriginatingSensor() {
        return originatingSensor;
    }

    /**
     * @return the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public double getTimestamp() {
        return timestamp;
    }

}
