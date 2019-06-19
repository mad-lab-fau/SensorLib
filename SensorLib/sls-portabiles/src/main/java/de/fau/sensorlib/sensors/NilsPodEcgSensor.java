/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;


/**
 * Represents a NilsPod Sensor device for ECG measurement.
 */
public class NilsPodEcgSensor extends NilsPodSensor {

    private static final String TAG = NilsPodEcgSensor.class.getSimpleName();

    public NilsPodEcgSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
    }


    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    @Override
    protected void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        // one data packet always has size mPacketSize
        if (values.length % mPacketSize != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mPacketSize) {
            int offset = i;
            double[] gyro = null;
            double[] accel = null;
            double baro = Double.MIN_VALUE;
            double ecg = Double.MIN_VALUE;
            //double rr;
            int localCounter;

            // extract gyroscope data
            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
                gyro = new double[3];
                for (int j = 0; j < 3; j++) {
                    gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                    offset += 2;
                }
            }
            // extract accelerometer data
            if (isSensorEnabled(HardwareSensor.ACCELEROMETER)) {
                accel = new double[3];
                for (int j = 0; j < 3; j++) {
                    accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                    offset += 2;
                }
            }

            if (isSensorEnabled(HardwareSensor.BAROMETER)) {
                baro = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                baro = (baro + 101325.0) / 100.0;
                offset += 2;
            }

            if (isSensorEnabled(HardwareSensor.ECG)) {
                ecg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, offset);
                offset += 4;
                /*rr = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset) * 7.8125;
                double hr = (60 / (rr / 1000));
                Log.d(TAG, "rr: " + rr + ", hr: " + hr);
                */
            }

            // extract packet counter (16 bit)
            localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + mPacketSize - 2);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 15)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            long timestamp = globalCounter * (2 << 15) + localCounter;
            NilsPodDataFrame df;
            if (isSensorEnabled(HardwareSensor.ECG)) {
                df = new NilsPodEcgDataFrame(this, timestamp, accel, gyro, baro, ecg);
            } else {
                df = new NilsPodDataFrame(this, timestamp, accel, gyro, baro);
            }
            //Log.d(TAG, df.toString());

            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mRecordingEnabled) {
                mDataRecorder.writeData(df);
            }
        }
    }

    /**
     * Data frame to store data received from the NilsPod Sensor
     */
    public static class NilsPodEcgDataFrame extends NilsPodDataFrame implements EcgDataFrame {

        protected double ecg;
        private char label;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param ecg       array storing ECG values
         */
        public NilsPodEcgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double ecg) {
            super(sensor, timestamp, accel, gyro, baro);
            this.ecg = ecg;
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param ecg       array storing ECG values
         */
        public NilsPodEcgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double ecg, char label) {
            super(sensor, timestamp, accel, gyro, baro);
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
            return ecg;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", accel: " + Arrays.toString(accel) + ", ecg: " + ecg + ", label: " + (int) label;
        }
    }
}
