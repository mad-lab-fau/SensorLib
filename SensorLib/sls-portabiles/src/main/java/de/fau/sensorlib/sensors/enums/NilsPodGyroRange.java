/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors.enums;

/**
 * Enum describing the possible gyroscope range of the NilsPod.
 */
public enum NilsPodGyroRange {

    GYRO_RANGE_2000_DPS(0x00),
    GYRO_RANGE_1000_DPS(0x10),
    GYRO_RANGE_500_DPS(0x20),
    GYRO_RANGE_250_DPS(0x30),
    GYRO_RANGE_125_DPS(0x40);

    private int gyroRange;

    NilsPodGyroRange(int gyroRange) {
        this.gyroRange = gyroRange;
    }

    public static NilsPodGyroRange inferGyroRange(int range) {
        for (NilsPodGyroRange gyroRange : values()) {
            if (gyroRange.gyroRange == range) {
                return gyroRange;
            }
        }
        return GYRO_RANGE_2000_DPS;
    }

    public int getRangeDps() {
        switch (this) {
            case GYRO_RANGE_2000_DPS:
                return 2000;
            case GYRO_RANGE_1000_DPS:
                return 1000;
            case GYRO_RANGE_500_DPS:
                return 500;
            case GYRO_RANGE_250_DPS:
                return 250;
            case GYRO_RANGE_125_DPS:
                return 125;
        }
        return 2000;
    }

    public int getRangeVal() {
        return gyroRange;
    }
}
