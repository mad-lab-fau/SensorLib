/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Data frame storing ambient sensor data.
 */

public interface AmbientDataFrame extends BarometricPressureDataFrame, HumidityDataFrame, LightDataFrame, NoiseDataFrame, TemperatureDataFrame {

    // empty interface, interface just for convenient summary of multiple single data frames

}