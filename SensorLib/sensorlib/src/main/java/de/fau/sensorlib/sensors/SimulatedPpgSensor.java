package de.fau.sensorlib.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.LabelDataFrame;
import de.fau.sensorlib.dataframe.PpgDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

public class SimulatedPpgSensor extends SimulatedSensor {

    public static final String SIMULATED_PPG_SENSOR_NAME = "SimulatedPpg";
    public static final String SIMULATED_PPG_SENSOR_ADDRESS = "n/a";


    private SensorDataFrame[] mSimulatedData;

    public SimulatedPpgSensor(Context context, String deviceName, String fileName, SensorDataProcessor dataHandler, double samplingRate, boolean liveMode) {
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
                double[] gyro = {Double.parseDouble(sampleData[4]), Double.parseDouble(sampleData[5]), Double.parseDouble(sampleData[6])};
                double baro = Double.parseDouble(sampleData[7]);
                double[] ppg = {Double.parseDouble(sampleData[8]) * (-1), Double.parseDouble(sampleData[9]) * (-1)};
                SensorDataFrame dataFrame = new SimulatedPpgSensor.SimulatedPpgDataFrame(this, timestampCounter, accel, gyro, baro, ppg);
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

    public static class SimulatedPpgDataFrame extends SensorDataFrame implements AccelDataFrame, BarometricPressureDataFrame, GyroDataFrame, PpgDataFrame, LabelDataFrame {

        private double[] accel;
        private double[] gyro;
        private double baro;
        private double[] ppg;
        private char label;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration value
         * @param ppg       array storing ECG values
         */
        public SimulatedPpgDataFrame(SimulatedSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] ppg) {
            this(sensor, timestamp, accel, gyro, baro, ppg, (char) 0);
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param gyro
         * @param accel     array storing acceleration values
         * @param baro
         * @param ppg       array storing ECG values
         */
        public SimulatedPpgDataFrame(SimulatedSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] ppg, char label) {
            super(sensor, timestamp);
            if (accel.length != 3) {
                throw new IllegalArgumentException("Illegal array size for acceleration values! ");
            }
            this.accel = accel;
            this.gyro = gyro;
            this.baro = baro;
            this.ppg = ppg;
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
        public double getGyroX() {
            return gyro[0];
        }

        @Override
        public double getGyroY() {
            return gyro[1];
        }

        @Override
        public double getGyroZ() {
            return gyro[2];
        }

        @Override
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public double getPpgRedSample() {
            // TODO dynamically change based on ECG sensor configuration
            return ppg[1];
        }

        @Override
        public double getPpgIrSample() {
            return ppg[1];
        }

        @Override
        public char getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", accel: " + Arrays.toString(accel) + ", gyro: " + Arrays.toString(gyro) + ", baro: " + baro + ", ppg: " + Arrays.toString(ppg) + ", label: " + (int) label;
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
