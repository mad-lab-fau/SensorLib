/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.enums;

import androidx.annotation.NonNull;

import org.apache.commons.text.WordUtils;


public enum SensorAction {
    CONFIGURE,
    DEFAULT_CONFIG,
    RESET,
    START_LOGGING,
    STOP_LOGGING,
    CLEAR_SESSIONS,
    FULL_ERASE,
    SENSOR_INFO;

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace('_', ' '));
    }
}
