/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Photoplethysmography (PPG) data frame
 */
public interface PpgMultiDataFrame {

    String[] COLUMNS = new String[]{"ppg_red", "ppg_ir", "ppg_green"};

    /**
     * Returns the red channel of the PPG signal.
     *
     * @return Red channel of PPG signal
     */
    double getPpgRedSample();

    /**
     * Returns the infra-red channel of the PPG signal.
     *
     * @return IR channel of PPG signal
     */
    double getPpgIrSample();

    /**
     * Returns the green channel of the PPG signal.
     *
     * @return Green channel of PPG signal
     */
    double getPpgGreenSample();
}
