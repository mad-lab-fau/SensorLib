/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.enums;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;

/**
 * Enum describing the sensor position
 */
public enum NilsPodSensorPosition {
    NO_POSITION_DEFINED,
    LEFT_FOOT,
    RIGHT_FOOT,
    HIP,
    LEFT_WRIST,
    RIGHT_WRIST,
    CHEST;

    public static NilsPodSensorPosition inferSensorPosition(int position) {
        if (position < values().length) {
            return values()[position];
        }
        return NO_POSITION_DEFINED;
    }

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace('_', ' '));
    }
}
