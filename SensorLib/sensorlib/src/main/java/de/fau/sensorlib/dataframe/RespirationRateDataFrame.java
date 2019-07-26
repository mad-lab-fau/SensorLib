/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Respiration rate data frame.
 */
public interface RespirationRateDataFrame {

    String[] COLUMNS = new String[]{"resp_rate"};

    /**
     * Returns the current respiration rate in bpm (breaths per minute).
     *
     * @return current respiration rate
     */
    double getRespirationRate();
}
