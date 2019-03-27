/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.InsoleSensor;
import de.fau.sensorlib.sensors.NilsPodSensor;
import de.fau.sensorlib.sensors.enums.NilsPodRfGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class SessionBuilder {

    private static final String TAG = SessionBuilder.class.getSimpleName();


    private boolean mFirstPacketRead;

    private int mHeaderSize;
    private int mSampleSize;
    private HashMap<HardwareSensor, Boolean> mEnabledSensorsMap = new HashMap<>();

    private ByteBuffer mByteBuffer;

    private AbstractSensor mSensor;
    private Session mSession;

    public SessionBuilder(AbstractSensor sensor, Session session) {
        mSensor = sensor;
        mSession = session;
    }

    public void nextPacket(byte[] values) {
        if (!mFirstPacketRead) {
            Log.e(TAG, Arrays.toString(values));
            mFirstPacketRead = true;
            mHeaderSize = values[0];
            byte[] header = new byte[mHeaderSize];
            byte[] data = new byte[values.length - mHeaderSize];
            System.arraycopy(values, 0, header, 0, header.length);
            System.arraycopy(values, mHeaderSize, data, 0, data.length);
            extractHeader(header);
            onNewData(data);
        } else {
            onNewData(values);
        }
    }

    // TODO TEST!!!
    private synchronized void extractHeader(byte[] values) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(null, 0, 0);
        characteristic.setValue(values);

        Log.e(TAG, "header: " + Arrays.toString(values));
        int offset = 1;
        mSampleSize = values[offset++];

        mByteBuffer = ByteBuffer.allocate(mSampleSize * 1000);

        int sensors = values[offset++];
        mEnabledSensorsMap.put(HardwareSensor.ACCELEROMETER, ((sensors & 0x01) != 0));
        mEnabledSensorsMap.put(HardwareSensor.GYROSCOPE, ((sensors & 0x01) != 0));
        mEnabledSensorsMap.put(HardwareSensor.FSR, ((sensors & 0x02) != 0));
        mEnabledSensorsMap.put(HardwareSensor.BAROMETER, ((sensors & 0x04) != 0));


        double samplingRate = NilsPodSensor.inferSamplingRate(values[offset] & 0x0F);
        NilsPodTerminationSource terminationSource = NilsPodTerminationSource.inferTerminationSource(values[offset++] & 0xF0);

        NilsPodSyncRole syncRole = NilsPodSyncRole.values()[values[offset++]];
        int syncDistance = values[offset++] * 100;
        NilsPodRfGroup rfGroup = NilsPodRfGroup.values()[values[offset++]];

        int accRange = values[offset++];
        int gyroRange = values[offset++];

        // metadata
        int sensorPosition = values[offset++];
        int specialFunction = values[offset++];
        offset += 3;


        int tmpTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        Date startTime = new Date(((long) tmpTime) * 1000);
        offset += 4;

        tmpTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        // little endian
        //tmpTime = (values[offset++] & 0xFF) | ((values[offset++] & 0xFF) << 8) | ((values[offset++] & 0xFF) << 16) | ((values[offset++] & 0xFF) << 24);
        Date endTime = new Date(((long) tmpTime) * 1000);
        offset += 4;

        //int sessionSize = ((values[offset++] & 0xFF) << 24) | ((values[offset++] & 0xFF) << 16) | ((values[offset++] & 0xFF) << 8) | (values[offset++] & 0xFF);
        int sessionSize = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        offset += 4;
        String firmwareVersion = values[offset++] + "." + values[offset++] + "." + values[offset];

        String sb = "sample size: " + mSampleSize + "\n" +
                "enabled sensors: " + mEnabledSensorsMap + "\n" +
                "sampling rate: " + samplingRate + "\n" +
                "termination source: " + terminationSource + "\n" +
                "sync role: " + syncRole + "\n" +
                "sync distance: " + syncDistance + "\n" +
                "rf group: " + rfGroup + "\n" +
                "acc range: " + accRange + "\n" +
                "gyro range: " + gyroRange + "\n" +
                "sensor position: " + sensorPosition + "\n" +
                "special function: " + specialFunction + "\n" +
                "start Time: " + startTime + "\n" +
                "end Time: " + endTime + "\n" +
                "session size: " + sessionSize + "\n" +
                "firmware version: " + firmwareVersion + "\n";
        Log.e(TAG, sb);
    }


    private synchronized void onNewData(byte[] values) {
        try {
            mByteBuffer.put(values);
        } catch (BufferOverflowException e) {
            e.printStackTrace();
        }

        byte[] sample;
        // flip buffer to start reading
        mByteBuffer.flip();
        while (mByteBuffer.remaining() / mSampleSize > 0) {
            sample = new byte[mSampleSize];
            // get one data sample
            mByteBuffer.get(sample);
            extractDataFrame(sample);
        }
        // compact buffer to shift remaining samples to beginning
        mByteBuffer.compact();
    }


    private void extractDataFrame(byte[] values) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(null, 0, 0);
        characteristic.setValue(values);
        int offset = 0;
        double[] gyro = new double[3];
        double[] accel = new double[3];
        double baro = 0;
        double[] pressure = new double[3];
        int timestamp;

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

        timestamp = ((values[offset++] & 0xFF) << 24) | ((values[offset++] & 0xFF) << 16) | ((values[offset++] & 0xFF) << 8) | values[offset] & 0xFF;

        InsoleSensor.InsoleDataFrame df = new InsoleSensor.InsoleDataFrame(mSensor, timestamp, accel, gyro, baro, pressure);

        Log.d(TAG, df.toString());
        //mDataRecorder.writeData(df);
    }

    public boolean isSensorEnabled(HardwareSensor sensor) {
        return (mEnabledSensorsMap.get(sensor) != null) && mEnabledSensorsMap.get(sensor);
    }

}
