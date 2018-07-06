/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Respiration data frame.
 */
public interface RespirationDataFrame {

    String[] COLUMNS = new String[]{"resp_sample", "resp_rate"};

    /**
     * Returns the current respiration sample.
     *
     * @return current respiration sample
     */
    double getRespirationSample();

    /**
     * Returns the current respiration rate.
     *
     * @return current respiration rate
     */
    double getRespirationRate();
}
