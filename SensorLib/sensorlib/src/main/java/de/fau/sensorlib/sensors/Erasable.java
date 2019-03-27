/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors;

public interface Erasable {

    /**
     * Deletes the data currently stored on the sensor, but does not perform a full erase (see {@link Erasable#fullErase()} for that).
     */
    void clearData();

    /**
     * Performs a <b>full</b> erase of the sensor storage.
     */
    void fullErase();

}
