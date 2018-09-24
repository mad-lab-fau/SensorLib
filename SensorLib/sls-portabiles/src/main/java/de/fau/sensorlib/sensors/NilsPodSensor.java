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

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;


/**
 * Represents a NilsPod Sensor device.
 */
public class NilsPodSensor extends AbstractNilsPodSensor {


    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    private int globalCounter = 0;


    public NilsPodSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
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
            double[] gyro = new double[3];
            double[] accel = new double[3];
            double baro;
            int localCounter;

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

            // extract packet counter (only 15 bit, therefore getIntValue() method not applicable)
            localCounter = (values[offset + 1] & 0xFF) | ((values[offset] & 0x7F) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 14)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            NilsPodDataFrame df = new NilsPodDataFrame(this, globalCounter * (2 << 14) + localCounter, accel, gyro, baro);
            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }

    public static class NilsPodDataFrame extends GenericNilsPodDataFrame implements BarometricPressureDataFrame {

        protected double baro;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro) {
            this(sensor, timestamp, accel, gyro, 0);
        }


        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro, double baro) {
            super(sensor, timestamp, accel, gyro);
            this.baro = baro;
        }

        @Override
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public String toString() {
            return super.toString() + ", baro: " + baro;
        }
    }
}
