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
import de.fau.sensorlib.dataframe.InsolePressureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;


/**
 * Represents a Sensor Insole.
 */
public class InsoleSensor extends NilsPodSensor {

    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    private int globalCounter = 0;

    public InsoleSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
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
            Log.e(TAG, "Wrong BLE Packet Size! " + values.length + ", " + mPacketSize);
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mPacketSize) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
            double baro = 0;
            double[] pressure = new double[3];
            int localCounter;

            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
                // extract gyroscope data
                for (int j = 0; j < 3; j++) {
                    gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                    offset += 2;
                }
            }
            // extract accelerometer data
            if (isSensorEnabled(HardwareSensor.ACCELEROMETER)) {
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

            if (isSensorEnabled(HardwareSensor.FSR)) {
                for (int j = 0; j < 3; j++) {
                    pressure[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
                    offset++;
                }
            }

            // extract packet counter (only 15 bit, therefore getIntValue() method not applicable)
            localCounter = (values[mPacketSize - 1] & 0xFF) | ((values[mPacketSize - 2] & 0x7F) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 14)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            InsoleDataFrame df = new InsoleDataFrame(this, globalCounter * (2 << 14) + localCounter, accel, gyro, baro, pressure);

            //Log.d(TAG, df.toString());
            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }

    /**
     * Data frame to store data received from the Insole
     */
    public static class InsoleDataFrame extends NilsPodDataFrame implements InsolePressureDataFrame {

        protected double[] pressure;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     Array storing acceleration values
         * @param gyro      Array storing gyroscope values
         * @param baro      Atmospheric pressure from barometer
         * @param pressure  Array storing FSR pressure values
         */
        public InsoleDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] pressure) {
            super(sensor, timestamp, accel, gyro, baro);
            this.pressure = pressure;
        }

        @Override
        public double getFirstPressureSample() {
            return pressure[0];

        }

        @Override
        public double getSecondPressureSample() {
            return pressure[1];
        }

        @Override
        public double getThirdPressureSample() {
            return pressure[2];
        }

        @Override
        public String toString() {
            return super.toString() + ", pressure: " + Arrays.toString(pressure);
        }
    }
}
