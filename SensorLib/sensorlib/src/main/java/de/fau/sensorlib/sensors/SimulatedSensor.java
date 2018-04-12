/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Base implementation of a simulated sensor. It receives predefined data in the constructor that is then
 * streamed back to the caller while simulating the behavior of other sensor implementations.
 */
public abstract class SimulatedSensor extends AbstractSensor {

    protected SensorDataFrame[] mSimulationData;
    protected BufferedReader mBufferedReader;
    private boolean mLiveMode;
    private boolean mUsesFile;
    private Thread mSimThread;

    /**
     * Default constructor.
     *
     * @param context       context.
     * @param deviceName    name for the sensor.
     * @param dataHandler   data handler.
     * @param simulatedData array of data frames that should be simulated.
     * @param samplingRate  sampling rate at which the samples should be sent.
     * @param liveMode      live mode (real time sampling) or continuous mode.
     */
    public SimulatedSensor(Context context, String deviceName, SensorDataFrame[] simulatedData, SensorDataProcessor dataHandler, double samplingRate, boolean liveMode) {
        super(context, deviceName, "SensorLib::SimulatedSensor::" + deviceName, dataHandler, samplingRate);
        mSimulationData = simulatedData;
        mLiveMode = liveMode;
        mUsesFile = false;
    }

    /**
     * Default constructor when reading from external storage.
     *
     * @param context      context.
     * @param deviceName   name for the sensor.
     * @param dataHandler  data handler.
     * @param fileName     file name of the file to be simulated.
     * @param samplingRate sampling rate at which the samples should be sent.
     * @param liveMode     live mode (real time sampling) or continuous mode.
     */
    public SimulatedSensor(Context context, String deviceName, String fileName, SensorDataProcessor dataHandler, double samplingRate, boolean liveMode) {
        super(context, deviceName, "SensorLib::SimulatedSensor::" + deviceName, dataHandler, samplingRate);

        try {
            mBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            mBufferedReader.mark(Integer.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
        mLiveMode = liveMode;
        mUsesFile = true;
    }

    protected boolean usesSimulationFile() {
        return mUsesFile;
    }

    @Override
    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return 0;
        }
        int batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int batteryScale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return (int) (((float) batteryLevel / batteryScale) * 100.0f);
    }

    @Override
    public boolean connect() throws Exception {
        return super.connect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        mSimThread.interrupt();
        mSimThread = null;
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        if (mSimThread == null) {
            mSimThread = new Thread(
                    new Runnable() {
                        public void run() {
                            transmitData();
                        }
                    });
        }
        if (!mSimThread.isAlive()) {
            mSimThread.start();
        }
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

    protected abstract void transmitData();

}
