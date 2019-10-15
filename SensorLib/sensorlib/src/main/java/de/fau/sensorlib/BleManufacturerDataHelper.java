/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;


import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.enums.SensorState;

public class BleManufacturerDataHelper {

    private static final String TAG = BleManufacturerDataHelper.class.getSimpleName();

    public static int getBatteryLevel(KnownSensor sensor, byte[] manuData) {
        if (manuData == null || sensor == null) {
            return 0;
        }

        switch (sensor) {
            case NILSPOD:
                return manuData[2];
        }

        return 0;
    }

    public static boolean getChargingState(KnownSensor sensor, byte[] manuData) {
        if (manuData == null || sensor == null) {
            return false;
        }

        switch (sensor) {
            case NILSPOD:
                return manuData[1] != 0x00;
        }

        return false;
    }


    public static SensorState getSensorState(KnownSensor sensor, byte[] manuData) {
        if (manuData == null || sensor == null) {
            return SensorState.UNDEFINED;
        }

        switch (sensor) {
            case NILSPOD:
                if (manuData[0] == 2) {
                    return SensorState.LOGGING;
                }
                return SensorState.UNDEFINED;
        }

        return SensorState.UNDEFINED;
    }
}
