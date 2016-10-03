/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.content.Context;
import android.os.Handler;

import de.fau.sensorlib.DsBleSensor;
import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.KnownSensor;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;

/**
 * Implementation for the Sony Smartband 2.
 */
public class Smartband2Sensor extends DsBleSensor {

    private static final String TAG = Smartband2Sensor.class.getSimpleName();

    private static final String DEVICE_NAME = "SWR12";

    public Smartband2Sensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor, dataHandler);
    }

    public Smartband2Sensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        useHardwareSensor(DsSensor.HardwareSensor.HEART_RATE);
        return super.connect();
    }

    @Override
    public void startStreaming() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Smartband2Sensor.super.startStreaming();
            }
        }, 5000);
    }
}
