/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.enums;

import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AnalogDataFrame;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.BloodPressureDataFrame;
import de.fau.sensorlib.dataframe.BloodVolumePulseDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.EdaDataFrame;
import de.fau.sensorlib.dataframe.EmgDataFrame;
import de.fau.sensorlib.dataframe.GestureDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.HeartRateDataFrame;
import de.fau.sensorlib.dataframe.HumidityDataFrame;
import de.fau.sensorlib.dataframe.LightDataFrame;
import de.fau.sensorlib.dataframe.MagnetometerDataFrame;
import de.fau.sensorlib.dataframe.NoiseDataFrame;
import de.fau.sensorlib.dataframe.OrientationDataFrame;
import de.fau.sensorlib.dataframe.PpgDataFrame;
import de.fau.sensorlib.dataframe.PressureDataFrame;
import de.fau.sensorlib.dataframe.QuaternionDataFrame;
import de.fau.sensorlib.dataframe.RespirationDataFrame;
import de.fau.sensorlib.dataframe.TemperatureDataFrame;

/**
 * Possible supported hardware sensors.
 */
public enum HardwareSensor {

    ACCELEROMETER("ACC", AccelDataFrame.class),
    GYROSCOPE("GYR", GyroDataFrame.class),
    MAGNETOMETER("MAG", MagnetometerDataFrame.class),
    LIGHT("LUX", LightDataFrame.class),
    PRESSURE("PRES", PressureDataFrame.class),
    BAROMETER("BARO", BarometricPressureDataFrame.class),
    ANALOG("ANA", AnalogDataFrame.class),
    TEMPERATURE("TEMP", TemperatureDataFrame.class),
    ECG("ECG", EcgDataFrame.class),
    EMG("EMG", EmgDataFrame.class),
    EEG_RAW("EEG"),
    EEG_FREQ_BANDS("EEG"),
    HEART_RATE("HR", HeartRateDataFrame.class),
    RESPIRATION("RESP", RespirationDataFrame.class),
    BLOOD_PRESSURE("BP", BloodPressureDataFrame.class),
    BLOOD_VOLUME_PRESSURE("BVP", BloodVolumePulseDataFrame.class),
    PPG("PPG", PpgDataFrame.class),
    GALVANIC_SKIN_RESPONSE("GSR", EdaDataFrame.class),
    ORIENTATION("ORI", OrientationDataFrame.class),
    QUATERNION("QUA", QuaternionDataFrame.class),
    GESTURE("GES", GestureDataFrame.class),
    NOISE("NOIS", NoiseDataFrame.class),
    HUMIDITY("HUM", HumidityDataFrame.class);

    private String mShortDescription;
    private Class<?> mDf;

    HardwareSensor(String shortDescription, Class<?> df) {
        mShortDescription = shortDescription;
        mDf = df;
    }

    HardwareSensor(String shortDescription) {
        this(shortDescription, null);
    }

    public String getShortDescription() {
        return mShortDescription;
    }

    public Class<?> getDataFrameClass() {
        return mDf;
    }

    public static boolean isInertial(HardwareSensor s) {
        return s == ACCELEROMETER || s == GYROSCOPE || s == MAGNETOMETER;
    }

    public static boolean isAmbient(HardwareSensor s) {
        return s == LIGHT || s == PRESSURE || s == TEMPERATURE || s == NOISE || s == HUMIDITY;
    }

}
