/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Electroencephalography (EEG) data frame.
 */
public interface EegDataFrame {

    // TODO change

    /**
     * @return Returns the raw EEG samples
     */
    double[] getRawEeg();

    /**
     * @return Returns the Alpha band samples
     */
    double[] getAlphaBand();

    /**
     * @return Returns the Beta band samples
     */
    double[] getBetaBand();

    /**
     * @return Returns the Gamma band samples
     */
    double[] getGammaBand();

    /**
     * @return Returns the Theta band samples
     */
    double[] getThetaBand();

    /**
     * @return Returns the Delta band samples
     */
    double[] getDeltaBand();

}
