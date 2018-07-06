/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.dataframe;

/**
 * Quaternion data frame.
 */
public interface QuaternionDataFrame {

    String[] COLUMNS = new String[]{"quat_w", "quat_x", "quat_y", "quat_z"};

    /**
     * Returns the quaternion w coordinate.
     *
     * @return quaternion w coordinate
     */
    double getQuaternionW();

    /**
     * Returns the quaternion x coordinate.
     *
     * @return quaternion x coordinate
     */
    double getQuaternionX();

    /**
     * Returns the quaternion y coordinate.
     *
     * @return quaternion y coordinate
     */
    double getQuaternionY();

    /**
     * Returns the quaternion z coordinate.
     *
     * @return quaternion z coordinate
     */
    double getQuaternionZ();

}
