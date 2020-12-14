/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.bluetooth.BluetoothGattCharacteristic;
import android.hardware.SensorManager;
import android.util.Log;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.fau.sensorlib.SensorDataRecorder;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.AbstractNilsPodSensor;
import de.fau.sensorlib.sensors.AbstractSensor;
import de.fau.sensorlib.sensors.NilsPodSensor;
import de.fau.sensorlib.sensors.enums.NilsPodGyroRange;
import de.fau.sensorlib.sensors.enums.NilsPodSensorPosition;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class SessionCsvConverter {

    private static final String TAG = SessionCsvConverter.class.getSimpleName();


    private boolean mFirstPacketRead;

    private SessionHeader mHeader;

    private ByteBuffer mByteBuffer;

    private AbstractSensor mSensor;
    private Session mSession;

    private SensorDataRecorder mRecorder;

    private double gyroScalingFactor = 1.0;
    private double accScalingFactor = 1.0;

    public SessionCsvConverter(AbstractSensor sensor, Session session) {
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
                return;
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

        try {
            int offset = 1;
            // Byte 1
            int sampleSize = values[offset++];

            mByteBuffer = ByteBuffer.allocate(sampleSize * 1000);

            // Byte 2-3
            ArrayList<HardwareSensor> enabledSensorList = new ArrayList<>();
            int sensors = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
            offset += 2;
            if ((sensors & 0x0001) != 0) {
                enabledSensorList.add(HardwareSensor.ACCELEROMETER);
            }
            if ((sensors & 0x0002) != 0) {
                enabledSensorList.add(HardwareSensor.GYROSCOPE);
            }
            if ((sensors & 0x0004) != 0) {
                enabledSensorList.add(HardwareSensor.MAGNETOMETER);
            }
            if ((sensors & 0x0008) != 0) {
                enabledSensorList.add(HardwareSensor.BAROMETER);
            }
            if ((sensors & 0x0010) != 0) {
                enabledSensorList.add(HardwareSensor.ANALOG);
            }
            if ((sensors & 0x0020) != 0) {
                enabledSensorList.add(HardwareSensor.ECG);
            }
            if ((sensors & 0x0040) != 0) {
                enabledSensorList.add(HardwareSensor.PPG);
            }
            if ((sensors & 0x0080) != 0) {
                enabledSensorList.add(HardwareSensor.TEMPERATURE);
            }

            // Byte 4
            double samplingRate = NilsPodSensor.inferSamplingRate(values[offset++]);

            // Byte 5
            NilsPodTerminationSource terminationSource = NilsPodTerminationSource.inferTerminationSource(values[offset++]);

            // Byte 6
            NilsPodSyncRole syncRole = NilsPodSyncRole.values()[values[offset++]];

            // Byte 7
            int syncDistance = values[offset++] * 100; // in ms

            // Byte 8
            int accRange = values[offset++]; // in g

            // Byte 9
            int gyroRange = values[offset++] * 125; // in dps

            // Bytes 10-14: System Settings
            // Byte 10: Sensor Position
            NilsPodSensorPosition sensorPosition = NilsPodSensorPosition.values()[values[offset++]];

            // Byte 11: Operation Modes (Motion Interrupt, Home Monitoring, etc.)
            boolean dockMode = (values[offset] & 0x40) != 0;
            boolean motionInterrupt = (values[offset++] & 0x80) != 0;

            // Bytes 12-14: Custom Meta Data
            byte[] customMetaData = new byte[3];
            System.arraycopy(values, offset, customMetaData, 0, 3);
            offset += 3;

            // Bytes 15-18
            int startTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
            offset += 4;

            // Bytes 19-22
            int endTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
            offset += 4;

            // Bytes 23-26: Session Size (number of samples)
            int sessionSize = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
            offset += 4;

            // Bytes 27-30
            int syncIndexStart = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
            offset += 4;
            // Bytes 31-34
            int syncIndexEnd = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
            offset += 4;

            // Bytes 35-40: 6 Byte MAC Address
            StringBuilder sb = new StringBuilder();
            for (int i = 5; i >= 0; i--) {
                byte val = values[offset + i];
                sb.append(String.format("%02x", val).toUpperCase());
                if (i != 0) {
                    sb.append(":");
                }
            }
            String macAddress = sb.toString();
            offset += 6;

            // Bytes 41-45: 5 Byte RF Address used for synchronization packages
            sb = new StringBuilder();
            for (int i = 4; i >= 0; i--) {
                byte val = values[offset + i];
                sb.append("0x").append(String.format("%02x", val).toUpperCase());
                if (i != 0) {
                    sb.append(" ");
                }
            }
            String syncAddress = sb.toString();
            offset += 5;

            // Byte 46
            int syncChannel = values[offset++];

            // Bytes 47-48
            String hardwareVersion = Integer.toString(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset));
            offset += 2;

            // Bytes 49-51
            String firmwareVersion = "v" + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++) + "." + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++) + "." + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

            mHeader.setSampleSize(sampleSize);
            mHeader.setSamplingRate(samplingRate);
            mHeader.setEnabledSensors(enabledSensorList);
            mHeader.setTerminationSource(terminationSource);
            mHeader.setSyncRole(syncRole);
            mHeader.setSyncDistance(syncDistance);
            mHeader.setSyncIndex(syncIndexStart, syncIndexEnd);
            mHeader.setSyncAddress(syncAddress, syncChannel);
            mHeader.setAccRange(accRange);
            mHeader.setGyroRange(gyroRange);
            mHeader.setSensorPosition(sensorPosition);
            mHeader.setDockModeEnabled(dockMode);
            mHeader.setMotionInterruptEnabled(motionInterrupt);
            mHeader.setCustomMetaData(customMetaData);
            mHeader.setStartTime(startTime);
            mHeader.setEndTime(endTime);
            mHeader.setSessionSize(sessionSize);
            mHeader.setHardwareVersion(hardwareVersion);
            mHeader.setMacAddress(macAddress);
            mHeader.setFirmwareVersion(firmwareVersion);

            // acc scaling factor for conversion from raw values to m/s^2
            accScalingFactor = (AbstractNilsPodSensor.BASE_SCALING_FACTOR_ACC / mHeader.getAccRange()) / SensorManager.GRAVITY_EARTH;
            // gyro scaling factor for conversion from raw values to dps
            gyroScalingFactor = (AbstractNilsPodSensor.BASE_SCALING_FACTOR_GYRO * NilsPodGyroRange.GYRO_RANGE_2000_DPS.getRangeDps()) / mHeader.getGyroRange();

        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readHeaderError);
        }

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

    protected void extractDataFrame(byte[] values) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(null, 0, 0);
        characteristic.setValue(values);
        int offset = 0;

        double[] gyro = null;
        double[] acc = null;
        double[] mag = null;
        double[] analog = null;
        double baro = Double.MIN_VALUE;
        double ecg = Double.MIN_VALUE;
        double ppg = Double.MIN_VALUE;
        double temp = Double.MIN_VALUE;

        // extract gyroscope data
        if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
            gyro = new double[3];

            for (int j = 0; j < 3; j++) {
                gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset) / gyroScalingFactor;
                offset += 2;
            }
        }

        // extract accelerometer data
        if (isSensorEnabled(HardwareSensor.ACCELEROMETER)) {
            acc = new double[3];

            for (int j = 0; j < 3; j++) {
                acc[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset) / accScalingFactor;
                offset += 2;
            }
        }

        // extract magnetometer data
        if (isSensorEnabled(HardwareSensor.MAGNETOMETER)) {
            mag = new double[3];
            for (int j = 0; j < 3; j++) {
                mag[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                offset += 2;
            }
        }

        if (isSensorEnabled(HardwareSensor.BAROMETER)) {
            baro = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
            baro = (baro + 101325.0) / 100.0;
            offset += 2;
        }

        if (isSensorEnabled(HardwareSensor.ANALOG)) {
            analog = new double[3];
            for (int j = 0; j < 3; j++) {
                analog[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;
            }
        }

        if (isSensorEnabled(HardwareSensor.ECG)) {
            ecg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, offset);
            offset += 4;
        }

        if (isSensorEnabled(HardwareSensor.PPG)) {
            ppg = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, offset);
            offset += 4;
        }

        if (isSensorEnabled(HardwareSensor.TEMPERATURE)) {
            temp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
            temp = temp * (1.0 / 512) + 23;
            offset += 2;
        }

        long timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, mHeader.getSampleSize() - 4);

        NilsPodSensor.NilsPodDataFrame df;
        if (isSensorEnabled(HardwareSensor.ANALOG)) {
            df = new NilsPodSensor.NilsPodAnalogDataFrame(mSensor, timestamp, acc, gyro, baro, temp, mag, analog);
        } else if (isSensorEnabled(HardwareSensor.ECG)) {
            df = new NilsPodSensor.NilsPodEcgDataFrame(mSensor, timestamp, acc, gyro, baro, temp, mag, ecg);
        } else if (isSensorEnabled(HardwareSensor.PPG)) {
            df = new NilsPodSensor.NilsPodPpgDataFrame(mSensor, timestamp, acc, gyro, baro, temp, mag, ppg);
        } else if (isSensorEnabled(HardwareSensor.MAGNETOMETER)) {
            df = new NilsPodSensor.NilsPodMagDataFrame(mSensor, timestamp, acc, gyro, baro, temp, mag);
        } else if (isSensorEnabled(HardwareSensor.TEMPERATURE)) {
            df = new NilsPodSensor.NilsPodTempDataFrame(mSensor, timestamp, acc, gyro, baro, temp);
        } else {
            df = new NilsPodSensor.NilsPodDataFrame(mSensor, timestamp, acc, gyro, baro);
        }

        //Log.d(TAG, df.toString());
        mRecorder.writeData(df);

    }

    public boolean isSensorEnabled(HardwareSensor sensor) {
        return mHeader.getEnabledSensors().contains(sensor);
    }

    public void completeBuilder() {
        mRecorder.completeRecorder();
    }
}
