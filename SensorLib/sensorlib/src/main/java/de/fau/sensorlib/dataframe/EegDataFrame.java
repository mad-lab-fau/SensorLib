package de.fau.sensorlib.dataframe;

/**
 * Created by Robert on 10/31/16.
 */

public interface EegDataFrame {

    double getEegSample();

    double getAlphaBandSample();

    double getBetaBandSample();

    double getGammaBandSample();

    double getThetaBandSample();

    double getDeltaBandSample();

}
