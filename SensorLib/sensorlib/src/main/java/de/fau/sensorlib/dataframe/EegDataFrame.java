package de.fau.sensorlib.dataframe;

/**
 * EEG data frame.
 */
public interface EegDataFrame {

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
