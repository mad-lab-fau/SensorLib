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
 * Enum describing the possible accelerometer range of the NilsPod.
 */
public enum NilsPodAccRange {

    ACC_RANGE_2G(0x03),
    ACC_RANGE_4G(0x05),
    ACC_RANGE_8G(0x08),
    ACC_RANGE_16G(0x0C);

    private int accRange;

    NilsPodAccRange(int accRange) {
        this.accRange = accRange;
    }

    public static NilsPodAccRange inferAccRange(int range) {
        for (NilsPodAccRange accRange : values()) {
            if (accRange.accRange == range) {
                return accRange;
            }
        }
        return ACC_RANGE_16G;
    }

    public int getRangeG() {
        return (ordinal() + 1) * 4;
    }

    public int getRangeVal() {
        return accRange;
    }

    @NonNull
    @Override
    public String toString() {
        return WordUtils.capitalizeFully(name().replace("ACC_RANGE", "").replace('_', ' '));
    }
}
