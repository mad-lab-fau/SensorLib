/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Data frame for analog data samples, e.g. data read from FSR sensors via ADC.
 */
public interface AnalogDataFrame {

    String[] COLUMNS = new String[]{"ana_1", "ana_2", "ana_3"};

    double getFirstAnalogSample();

    double getSecondAnalogSample();

    double getThirdAnalogSample();
}
