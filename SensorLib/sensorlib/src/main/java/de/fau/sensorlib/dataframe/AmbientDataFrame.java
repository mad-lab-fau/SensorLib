/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

/**
 * Ambient data frame.
 */
<<<<<<< HEAD
public interface AmbientDataFrame extends BarometricPressureDataFrame, HumidityDataFrame, LightDataFrame, NoiseDataFrame, TemperatureDataFrame {

    @Override
    double getBarometricPressure();
=======
public interface AmbientDataFrame extends BarometerDataFrame {

    double getLight();

    @Override
    double getBarometerPressure();

    double getTemperature();
>>>>>>> 854906a15bd4bd1223c77dd988df0aa21f8de1c4

    @Override
    double getHumidity();

    @Override
    double getLight();

    @Override
    double getNoise();

    @Override
    double getTemperature();

}