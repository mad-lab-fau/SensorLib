/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p/>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;

import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AnnotatedDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.EmgDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.RespirationDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Implementation of a simulated sensor. It receives predefined data in the constructor that is then
 * streamed back to the caller while simulating the behavior of other sensor implementations.
 */
public class SimulatedSensor extends DsSensor {

    SimulatedDataFrame[] simData;
    protected BufferedReader mBufferedReader;
    protected boolean liveMode;
    protected boolean usesFile;
    protected Thread simThread;

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
    public SimulatedSensor(Context context, String deviceName, SensorDataProcessor dataHandler, SimulatedDataFrame[] simulatedData, double samplingRate, boolean liveMode) {
        super(context, deviceName, "SensorLib::SimulatedSensor::" + deviceName, dataHandler);
        simData = simulatedData;
        setSamplingRate(samplingRate);
        this.liveMode = liveMode;
        this.usesFile = false;
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
    public SimulatedSensor(Context context, String deviceName, SensorDataProcessor dataHandler, String fileName, double samplingRate, boolean liveMode) {
        super(context, deviceName, "SensorLib::SimulatedSensor::" + deviceName, dataHandler);
        try {
            mBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            mBufferedReader.mark(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setSamplingRate(samplingRate);
        this.liveMode = liveMode;
        this.usesFile = true;
    }

    public static class SimulatedDataFrame extends SensorDataFrame implements AccelDataFrame, AnnotatedDataFrame, EcgDataFrame, EmgDataFrame, GyroDataFrame, RespirationDataFrame {
        public SimulatedDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public SimulatedDataFrame() {
            super(null, 0);
        }

        protected void setSensor(DsSensor fromSensor) {
            originatingSensor = fromSensor;
        }

        protected void setTimestamp(double timestamp) {
            this.timestamp = timestamp;
        }

        public double accelX;
        public double accelY;
        public double accelZ;
        public double gyroX;
        public double gyroY;
        public double gyroZ;
        public double ecg;
        public double ecgLA;
        public double ecgRA;
        public double respiration;
        public double emg;
        public char label;

        @Override
        public double getAccelX() {
            return accelX;
        }

        @Override
        public double getAccelY() {
            return accelY;
        }

        @Override
        public double getAccelZ() {
            return accelZ;
        }

        @Override
        public char getAnnotationChar() {
            return label;
        }

        @Override
        public String getAnnotationString() {
            return null;
        }

        @Override
        public Object getAnnotation() {
            return label;
        }

        @Override
        public double getEcgSample() {
            return ecg;
        }

        @Override
        public double getSecondaryEcgSample() {
            return ecgLA;
        }

        @Override
        public double getEmgSample() {
            return emg;
        }

        @Override
        public double getGyroX() {
            return gyroX;
        }

        @Override
        public double getGyroY() {
            return gyroY;
        }

        @Override
        public double getGyroZ() {
            return gyroZ;
        }

        @Override
        public double getRespirationSample() {
            return respiration;
        }

        @Override
        public double getRespirationRate() {
            return 0;
        }
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.allOf(HardwareSensor.class);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (getState() != SensorState.CONNECTED) {
            return;
        }
        stopStreaming();
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        if (getState() != SensorState.CONNECTED) {
            return;
        }
        simThread = new Thread(
                new Runnable() {
                    public void run() {
                        transmitData();
                    }
                });
        simThread.start();
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        if (getState() != SensorState.STREAMING) {
            return;
        }
        simThread.interrupt();
        sendStopStreaming();
        setState(SensorState.CONNECTED);
    }

    protected void transmitData() {
        //init variables for live mode
        double startTime = System.nanoTime();
        double samplingInterval = 1000d / this.getSamplingRate();
        double samplingIntervalNano = 1.0e9d / mSamplingRate;

        //loop over samples
        for (int i = 0; i < simData.length; i++) {
            //get current time
            double curTime = System.nanoTime();
            //transmitting state
            //if (m_state == State.TRANSMITTING)
            {
                //send message (data available)
                simData[i].setSensor(this);
                simData[i].setTimestamp(i * samplingInterval);
                mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, simData[i]).sendToTarget();
            }
            //live mode
            if (liveMode) {
                //sleep until next sampling event
                while ((System.nanoTime() - curTime) < samplingIntervalNano) {
                    Thread.yield();
                }
            }
            if (Thread.interrupted()) {
                Log.e("SimulatedSensor", "Thread interrupted!");
                sendStopStreaming();
                return;
            }
        }

        sendStopStreaming();
    }
}
