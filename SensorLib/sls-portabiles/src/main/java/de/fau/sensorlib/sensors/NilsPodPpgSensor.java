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
import de.fau.sensorlib.dataframe.PpgDataFrame;


/**
 * Represents a NilsPod Sensor device for PPG measurement.
 */
public class NilsPodPpgSensor extends NilsPodSensor {

    public NilsPodPpgSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
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
        if (values.length == 0 || values.length % mPacketSize != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mPacketSize) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
            double baro;
            double[] ppg = new double[2];
            long localCounter;

            // extract gyroscope data
            for (int j = 0; j < 3; j++) {
                gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }
            // extract accelerometer data
            for (int j = 0; j < 3; j++) {
                accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }

            baro = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
            baro = (baro + 101325.0) / 100.0;
            offset += 2;

            for (int j = 0; j < 2; j++) {
                ppg[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }

            localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);

            // check if packets have been lost
            if ((localCounter - lastCounter) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }

            NilsPodPpgDataFrame df = new NilsPodPpgDataFrame(this, localCounter, accel, gyro, baro, ppg);

            Log.d(TAG, df.toString());
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
    public static class NilsPodPpgDataFrame extends NilsPodDataFrame implements PpgDataFrame {

        private double[] ppg;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     Array storing acceleration values
         * @param gyro      Array storing gyroscope values
         * @param baro      Atmospheric pressure from barometer
         */
        public NilsPodPpgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro, double baro) {
            this(sensor, timestamp, accel, gyro, baro, new double[2]);
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         * @param ppg       array storing PPG values
         */
        public NilsPodPpgDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] ppg) {
            super(sensor, timestamp, accel, gyro, baro);
            this.ppg = ppg;
        }

        @Override
        public double getPpgIrSample() {
            return ppg[0];

        }

        @Override
        public double getPpgRedSample() {
            return ppg[1];
        }


        @Override
        public String toString() {
            return super.toString() + ", ppg: " + Arrays.toString(ppg);
        }

    }

}