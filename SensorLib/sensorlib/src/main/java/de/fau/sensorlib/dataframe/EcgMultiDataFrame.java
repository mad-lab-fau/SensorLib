/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Electrocardiography (ECG) data frame for multiple channels.
 */
public interface EcgMultiDataFrame {

    String[] COLUMNS = new String[]{"ecg_1", "ecg_2", "ecg_3"};

    /**
     * Returns the ECG sample from the first channel.
     *
     * @return ECG value from first channel
     */
    default double getFirstEcgSample() {
        return 0.0;
    }

    /**
     * Returns the ECG sample from the second channel.
     *
     * @return ECG value from second channel
     */
    default double getSecondEcgSample() {
        return 0.0;
    }

    /**
     * Returns the ECG sample from the third channel.
     *
     * @return ECG value from third channel
     */
    default double getThirdEcgSample() {
        return 0.0;
    }
}
