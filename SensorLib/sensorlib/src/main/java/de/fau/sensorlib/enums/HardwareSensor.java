/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.enums;

/**
 * Possible supported hardware sensors.
 */
public enum HardwareSensor {

    ACCELEROMETER("ACC"),
    GYROSCOPE("GYR"),
    MAGNETOMETER("MAG"),
    LIGHT("LUX"),
    PRESSURE("PRES"),
    FSR("FSR"),
    TEMPERATURE("TEMP"),
    ECG("ECG"),
    EMG("EMG"),
    EEG_RAW("EEG"),
    EEG_FREQ_BANDS("EEG"),
    HEART_RATE("HR"),
    RESPIRATION("RESP"),
    BLOOD_PRESSURE("BP"),
    BLOOD_VOLUME_PRESSURE("BVP"),
    GALVANIC_SKIN_RESPONSE("GSR"),
    ORIENTATION("ORI"),
    QUATERNION("QUA"),
    GESTURE("GES"),
    NOISE("NOIS"),
    HUMIDITY("HUM");

    private String mShortDescription;

    HardwareSensor(String shortDescription) {
        mShortDescription = shortDescription;
    }

    public String getShortDescription() {
        return mShortDescription;
    }

    public static boolean isInertial(HardwareSensor s) {
        return s == ACCELEROMETER || s == GYROSCOPE || s == MAGNETOMETER;
    }

    public static boolean isAmbient(HardwareSensor s) {
        return s == LIGHT || s == PRESSURE || s == TEMPERATURE || s == NOISE || s == HUMIDITY;
    }

}
