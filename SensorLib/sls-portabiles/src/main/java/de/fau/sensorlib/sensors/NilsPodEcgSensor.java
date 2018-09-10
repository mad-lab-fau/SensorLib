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
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.LabelDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;


/**
 * Represents a NilsPod Sensor device for ECG measurement.
 */
public class NilsPodEcgSensor extends AbstractNilsPodSensor {

    // Override default counter
    static {
        // 7 Byte ECG + 6 Byte accelerometer + 4 Byte counter
        PACKET_SIZE = 7 + 6 + 4;
    }


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
        //Log.d(TAG, "data: " + Arrays.toString(values) + ", LENGTH: " + values.length);

        // one data packet always has size PACKET_SIZE
        if (values.length % PACKET_SIZE != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }


        // iterate over data packets
        for (int i = 0; i < values.length; i += PACKET_SIZE) {
            int offset = i;
            double[] accel = new double[3];
            double[] ecg = new double[2];
            char label;
            long localCounter;

            // extract ECG data
            for (int j = 0; j < 2; j++) {
                int tmp = ((values[offset] & 0xFF) | ((values[offset + 1] & 0xFF) << 8) | ((values[offset + 2] & 0xFF) << 16));
                tmp = tmp << 8;
                // shift to fill 32 bit integer with sign bit
                ecg[j] = tmp >> 8;
                offset += 3;
            }

            label = (char) values[offset];
            offset++;

            // extract accelerometer data
            for (int j = 0; j < 3; j++) {
                accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }

            // extract packet counter
            localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);

            // check if packets have been lost
            if (((localCounter - lastCounter) % ((long) 2 << 31)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }

            NilsPodEcgDataFrame df = new NilsPodEcgDataFrame(this, localCounter, accel, ecg, label);
            // send new data to the SensorDataProcessor
            Log.d(TAG, df.toString());
            lastCounter = localCounter;
            sendNewData(df);
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }


    /**
     * Data frame to store data received from the Hoop Sensor
     */
    public static class NilsPodEcgDataFrame extends SensorDataFrame implements AccelDataFrame, EcgDataFrame, LabelDataFrame {

        private long timestamp;
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
        public NilsPodEcgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] ecg) {
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
        public NilsPodEcgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] ecg, char label) {
            super(sensor, timestamp);
            if (accel.length != 3) {
                throw new IllegalArgumentException("Illegal array size for acceleration values! ");
            }
            this.timestamp = timestamp;
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
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + timestamp + ", accel: " + Arrays.toString(accel) + ", ecg: " + Arrays.toString(ecg) + ", label: " + (int) label;
        }
    }
}
