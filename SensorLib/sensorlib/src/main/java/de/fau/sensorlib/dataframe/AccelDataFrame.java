/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Acceleration data frame.
 */
public interface AccelDataFrame {

    String[] COLUMNS = new String[]{"acc_x", "acc_y", "acc_z"};

    /**
     * Returns the acceleration x value.
     *
     * @return x value
     */
    double getAccelX();

    /**
     * Returns the acceleration y value.
     *
     * @return y value
     */
    double getAccelY();

    /**
     * Returns the acceleration z value.
     *
     * @return z value
     */
    double getAccelZ();
}
