/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Blood pressure data frame.
 */
public interface BloodPressureDataFrame {

    String[] COLUMNS = new String[]{"sp", "dp", "map"};

    /**
     * Returns the systolic pressure value.
     *
     * @return Systolic pressure value
     */
    double getSystolicPressure();

    /**
     * Returns the diastolic pressure value.
     *
     * @return Diastolic pressure value
     */
    double getDiastolicPressure();

    /**
     * Returns the mean arterial pressure value.
     *
     * @return Mean arterial pressure value
     */
    double getMeanArterialPressure();

}
