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
 * Enum indicating the operation mode of NilsPod.
 */
public enum NilsPodOperationMode {
    NORMAL_MODE,
    HOME_MONITORING_MODE;

    public static NilsPodOperationMode inferOperationMode(int operationMode) {
        return (operationMode & 0x40) == 0 ? NORMAL_MODE : HOME_MONITORING_MODE;
    }

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace('_', ' '));
    }
}
