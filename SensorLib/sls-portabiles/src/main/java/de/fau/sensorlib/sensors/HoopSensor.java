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
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.enums.SensorState;


/**
 * Represents a Hoop Sensor device.
 */
public class HoopSensor extends AbstractNilsPodSensor {


    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    private int globalCounter = 0;


    public HoopSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
        try {
            setSamplingRate(200.0);
        } catch (SensorException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStateChange(SensorState oldState, SensorState newState) {
        super.onStateChange(oldState, newState);
        try {
            switch (newState) {
                case STREAMING:
                    setOperationState(NilsPodOperationState.STREAMING);
                    break;
                default:
                    setOperationState(NilsPodOperationState.IDLE);
                    break;
            }
        } catch (SensorException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
        super.onNewCharacteristicWrite(characteristic, status);
        if (NILS_POD_COMMANDS.equals(characteristic.getUuid())) {
            // check if the command sent to the sensor was STOP_STREAMING
            byte[] values = characteristic.getValue();
            if (values.length > 0) {
                if (Arrays.equals(values, NilsPodSensorCommand.STOP_STREAMING.getByteCmd())) {
                    sendStopStreaming();
                } else if (Arrays.equals(values, NilsPodSensorCommand.START_STREAMING.getByteCmd())) {
                    if (getOperationState().ordinal() < NilsPodOperationState.STREAMING.ordinal()) {
                        sendStartStreaming();
                    }
                }
            }
        }
    }

    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    @Override
    protected void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        // one data packet always has size mSampleSize
        if (values.length % mSampleSize != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mSampleSize) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
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

            // extract packet counter (only 15 bit, therefore getIntValue() method not applicable)
            localCounter = (values[i + mSampleSize - 1] & 0xFF) | ((values[i + mSampleSize - 2] & 0x7F) << 8);


            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 14)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }


            HoopDataFrame df = new HoopDataFrame(this, globalCounter * (2 << 14) + localCounter, accel, gyro);
            // send new data to the SensorDataProcessor
            //Log.d(TAG, df.toString());
            sendNewData(df);

            lastCounter = localCounter;
            if (mRecordingEnabled) {
                mDataRecorder.writeData(df);
            }
        }
    }

    @Override
    public void startStreaming() {
        super.startStreaming();
        lastCounter = 0;
        globalCounter = 0;
    }

    public static class HoopDataFrame extends GenericNilsPodDataFrame {

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public HoopDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro) {
            super(sensor, timestamp, accel, gyro);
        }
    }
}
