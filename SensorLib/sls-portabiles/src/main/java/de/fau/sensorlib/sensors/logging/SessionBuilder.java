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
import java.util.Date;
import java.util.HashMap;

import de.fau.sensorlib.SensorDataRecorder;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.InsoleSensor;
import de.fau.sensorlib.sensors.NilsPodSensor;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class SessionBuilder {

    private static final String TAG = SessionBuilder.class.getSimpleName();


    private boolean mFirstPacketRead;

    private SessionHeader mHeader;

    private ByteBuffer mByteBuffer;

    private AbstractSensor mSensor;
    private Session mSession;

    private SensorDataRecorder mRecorder;

    public SessionBuilder(AbstractSensor sensor, Session session) {
        mSensor = sensor;
        mSession = session;
    }

    public void nextPacket(byte[] values) {
        if (!mFirstPacketRead) {
            mFirstPacketRead = true;
            int headerSize = values[0];
            byte[] header = new byte[headerSize];
            byte[] data = new byte[values.length - headerSize];
            System.arraycopy(values, 0, header, 0, header.length);
            System.arraycopy(values, headerSize, data, 0, data.length);
            try {
                extractHeader(header);
            } catch (SensorException e) {
                e.printStackTrace();
            }
            onNewData(data);
        } else {
            onNewData(values);
        }
    }

    private synchronized void extractHeader(byte[] values) throws SensorException {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(null, 0, 0);
        characteristic.setValue(values);

        mHeader = new SessionHeader();

        int offset = 1;
        int sampleSize = values[offset++];

        mByteBuffer = ByteBuffer.allocate(sampleSize * 1000);

        int sensors = values[offset++];
        HashMap<HardwareSensor, Boolean> enabledSensorsMap = new HashMap<>();
        enabledSensorsMap.put(HardwareSensor.ACCELEROMETER, ((sensors & 0x01) != 0));
        enabledSensorsMap.put(HardwareSensor.GYROSCOPE, ((sensors & 0x01) != 0));
        enabledSensorsMap.put(HardwareSensor.FSR, ((sensors & 0x02) != 0));
        enabledSensorsMap.put(HardwareSensor.BAROMETER, ((sensors & 0x04) != 0));


        double samplingRate = NilsPodSensor.inferSamplingRate(values[offset] & 0x0F);
        NilsPodTerminationSource terminationSource = NilsPodTerminationSource.inferTerminationSource(values[offset++] & 0xF0);

        NilsPodSyncRole syncRole = NilsPodSyncRole.values()[values[offset++]];
        int syncDistance = values[offset++] * 100;
        NilsPodSyncGroup syncGroup = NilsPodSyncGroup.values()[values[offset++]];

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
        Date endTime = new Date(((long) tmpTime) * 1000);
        offset += 4;

        int sessionSize = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        offset += 4;
        String firmwareVersion = values[offset++] + "." + values[offset++] + "." + values[offset];

        mHeader.setSensorName(mSensor.getDeviceName());
        mHeader.setFirmwareVersion(mSensor.getFirmwareRevision());
        mHeader.setModelNumber(mSensor.getModelNumber());
        mHeader.setSampleSize(sampleSize);
        mHeader.setSamplingRate(samplingRate);
        mHeader.setEnabledSensors(enabledSensorsMap);
        mHeader.setTerminationSource(terminationSource);
        mHeader.setSyncRole(syncRole);
        mHeader.setSyncDistance(syncDistance);
        mHeader.setSyncGroup(syncGroup);
        mHeader.setAccRange(accRange);
        mHeader.setGyroRange(gyroRange);
        mHeader.setSensorPosition(sensorPosition);
        mHeader.setSpecialFunction(specialFunction);
        mHeader.setStartDate(startTime.toString());
        mHeader.setEndDate(endTime.toString());
        mHeader.setSessionSize(sessionSize);
        mHeader.setFirmwareVersion(firmwareVersion);

        Log.d(TAG, mHeader.toString());

        String subDir = "NilsPodSessionDownloads";
        mRecorder = new SensorDataRecorder(mSensor, mSensor.getContext(), mHeader.toJson(), subDir, mSession.getStartDate());
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
        while (mByteBuffer.remaining() / mHeader.getSampleSize() > 0) {
            sample = new byte[mHeader.getSampleSize()];
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
        long timestamp;

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

        SensorDataFrame df;
        if (mSensor instanceof InsoleSensor) {
            df = new InsoleSensor.InsoleDataFrame(mSensor, timestamp, accel, gyro, baro, pressure);
        } else {
            df = new NilsPodSensor.NilsPodDataFrame(mSensor, timestamp, accel, gyro, baro);
        }

        Log.d(TAG, df.toString());
        mRecorder.writeData(df);
    }

    public boolean isSensorEnabled(HardwareSensor sensor) {
        return (mHeader.getEnabledSensors().get(sensor) != null) && mHeader.getEnabledSensors().get(sensor);
    }

    public void completeBuilder() {
        mRecorder.completeRecorder();
    }
}
