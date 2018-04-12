/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Gyroscope data frame.
 */
public interface GyroDataFrame {

    /**
     * Returns the gyroscope x value.
     *
     * @return x value
     */
    double getGyroX();

    /**
     * Returns the gyroscope y value.
     *
     * @return y value
     */
    double getGyroY();

    /**
     * Returns the gyroscope z value.
     *
     * @return z value
     */
    double getGyroZ();
}
