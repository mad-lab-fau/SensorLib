/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors.enums;

/**
 * Enum describing the possible accelerometer range of the NilsPod.
 */
public enum NilsPodAccRange {

    ACC_RANGE_2_G(0x03),
    ACC_RANGE_4_G(0x05),
    ACC_RANGE_8_G(0x08),
    ACC_RANGE_16_G(0x0C);

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
        return ACC_RANGE_16_G;
    }

    public int getRangeG() {
        return (ordinal() + 1) * 4;
    }

    public int getRangeVal() {
        return accRange;
    }
}
