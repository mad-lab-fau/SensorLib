/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;


import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * A simple implementation of the SensorDataFrame that contains an int identifier and a double value.
 */
public class SimpleDataFrame extends SensorDataFrame {
    int mIdentifier;
    double mValue;

    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SimpleDataFrame(AbstractSensor fromSensor, double timestamp, int identifier, double value) {
        super(fromSensor, timestamp);
        mIdentifier = identifier;
        mValue = value;
    }

    public int getIdentifier() {
        return mIdentifier;
    }

    public double getValue() {
        return mValue;
    }

    @Override
    public String toString() {
        return "SimpleDataFrame(" + mIdentifier + "; " + mValue + ")";
    }
}
