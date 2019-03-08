package de.fau.sensorlib.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorDataRecorder;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.LabelDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

public class SimulatedEcgSensor extends SimulatedSensor {

    public static final String SIMULATED_ECG_SENSOR_NAME = "SimulatedEcg";
    public static final String SIMULATED_ECG_SENSOR_ADDRESS = "n/a";


    private SensorDataFrame[] mSimulatedData;

    public SimulatedEcgSensor(Context context, String deviceName, String fileName, SensorDataProcessor dataHandler, double samplingRate, boolean liveMode) {
        super(context, deviceName, fileName, dataHandler, samplingRate, liveMode);
        createSensorDataFrame(super.mBufferedReader);
    }

    private static final String splitBy = ",";

    public void createSensorDataFrame(BufferedReader bufferedReader) {
        int timestampCounter = 0;
        ArrayList<SensorDataFrame> mSimulatedDataList = new ArrayList<SensorDataFrame>();
        try {
            String line = "";
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                String[] sampleData = line.split(splitBy);
                double[] accel = {Double.parseDouble(sampleData[1]), Double.parseDouble(sampleData[2]), Double.parseDouble(sampleData[3])};
                double[] ecg = {Double.parseDouble(sampleData[4]), Double.parseDouble(sampleData[5])};
                SensorDataFrame dataFrame = new SimulatedEcgDataFrame(this, timestampCounter, accel, ecg);
                mSimulatedDataList.add(dataFrame);
                //mSimulatedData[timestampCounter] = dataFrame;
                timestampCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSimulatedData = new SensorDataFrame[mSimulatedDataList.size()];
        for (int i = 0; i < mSimulatedData.length; i++) {
            mSimulatedData[i] = mSimulatedDataList.get(i);
        }

    }

    public static class SimulatedEcgDataFrame extends SensorDataFrame implements AccelDataFrame, EcgDataFrame, LabelDataFrame {

        private double[] accel;
        private double[] ecg;
        private char label;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param ecg       array storing ECG values
         */
        public SimulatedEcgDataFrame(SimulatedSensor sensor, long timestamp, double[] accel, double[] ecg) {
            this(sensor, timestamp, accel, ecg, (char) 0);
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param ecg       array storing ECG values
         */
        public SimulatedEcgDataFrame(SimulatedSensor sensor, long timestamp, double[] accel, double[] ecg, char label) {
            super(sensor, timestamp);
            if (accel.length != 3) {
                throw new IllegalArgumentException("Illegal array size for acceleration values! ");
            }
            this.accel = accel;
            this.ecg = ecg;
            this.label = label;
        }

        @Override
        public double getAccelX() {
            return accel[0];
        }

        @Override
        public double getAccelY() {
            return accel[1];
        }

        @Override
        public double getAccelZ() {
            return accel[2];
        }

        @Override
        public double getEcgSample() {
            // TODO dynamically change based on ECG sensor configuration
            return ecg[1];
        }

        @Override
        public double getSecondaryEcgSample() {
            return ecg[1];
        }

        @Override
        public char getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", accel: " + Arrays.toString(accel) + ", ecg: " + Arrays.toString(ecg) + ", label: " + (int) label;
        }
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
        //return super.connect();
        super.connect();
        sendConnected();
        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
    }

    @Override
    public void startStreaming() {
        super.startStreaming();
    }

    @Override
    public void stopStreaming() {
        super.stopStreaming();
    }

    public void transmitData() {

        int i = 0;
        while (i < mSimulatedData.length) {
            sendNewData(mSimulatedData[i]);
            if (i == ((mSimulatedData.length) - 1)) {
                stopStreaming();
            }
            i++;
            try {
                Thread.sleep((long) (1000 / this.getSamplingRate()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }
}
