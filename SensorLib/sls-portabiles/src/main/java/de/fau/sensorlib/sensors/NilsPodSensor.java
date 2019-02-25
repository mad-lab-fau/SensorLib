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

import java.util.Calendar;
import java.util.Date;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;


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

    @Override
    public void startStreaming() {
        super.startStreaming();
        lastCounter = 0;
        globalCounter = 0;
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
            double baro = 0;
            int localCounter;

            // extract gyroscope data
            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
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
            }

            // extract packet counter (16 bit)
            localCounter = (values[i + mPacketSize - 1] & 0xFF) | ((values[i + mPacketSize - 2] & 0xFF) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 15)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            NilsPodDataFrame df = new NilsPodDataFrame(this, globalCounter * (2 << 15) + localCounter, accel, gyro, baro);
            // send new data to the SensorDataProcessor
            Log.d(TAG, "localCounter: [" + values[i + mPacketSize - 2] + "," + values[i + mPacketSize - 1] + "] -> " + localCounter + "  " + ((int) df.getTimestamp()));


            sendNewData(df);
            lastCounter = localCounter;
            if (mRecordingEnabled) {
                mDataRecorder.writeData(df);
            }
        }
    }


    @Override
    protected void onStateChange(SensorState oldState, SensorState newState) {
        super.onStateChange(oldState, newState);
        if (oldState == SensorState.CONNECTING && newState == SensorState.CONNECTED) {
            setTimeDate();
        }
    }

    /**
     * Updates the current date time stamp of the external temperature compensated RTC module.
     */
    public void setTimeDate() {
        Date currentTime = Calendar.getInstance().getTime();

        Log.d(TAG, "Updating RTC time on " + getName() + " - new time: " + currentTime);

        long unixTime = currentTime.getTime() / 1000; //convert from milliseconds to seconds
        byte[] data = new byte[4];
        data[0] = (byte) ((unixTime >> 24) & 0xFF);
        data[1] = (byte) ((unixTime >> 16) & 0xFF);
        data[2] = (byte) ((unixTime >> 8) & 0xFF);
        data[3] = (byte) ((unixTime) & 0xFF);

        BluetoothGattCharacteristic dateTimeCharacteristic = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_DATE_TIME_CONFIG);
        writeCharacteristic(dateTimeCharacteristic, data);

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
