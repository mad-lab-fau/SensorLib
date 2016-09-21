/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Heart rate data frame.
 */
public interface HeartRateDataFrame {

    String EXTRA_HEART_RATE = "hr";

    /**
     * @return the heart rate in beats per minute.
     */
    double getHeartRate();

    /**
     * @return the RR interval in seconds.
     */
    double getInterbeatInterval();
}
