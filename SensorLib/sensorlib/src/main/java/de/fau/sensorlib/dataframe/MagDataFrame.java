/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Magnetometer data frame.
 */
public interface MagDataFrame {

    String[] COLUMNS = new String[]{"mag_x", "mag_y", "mag_z"};

    /**
     * Returns the magnetometer x value.
     *
     * @return x value
     */
    double getMagX();

    /**
     * Returns the magnetometer y value.
     *
     * @return y value
     */
    double getMagY();

    /**
     * Returns the magnetometer z value.
     *
     * @return z value
     */
    double getMagZ();
}