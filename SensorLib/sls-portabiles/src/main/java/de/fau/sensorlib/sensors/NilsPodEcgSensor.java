/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataLogger;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.LabelDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;


/**
 * Represents a NilsPod Sensor device for ECG measurement.
 */
public class NilsPodEcgSensor extends GenericBleSensor {

    /**
     * UUID for Data Streaming Service of Hoop sensor
     */
    private static final UUID NILSPOD_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Config Characteristic (write) of Hoop Sensor
     */
    private static final UUID NILSPOD_CONFIG = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (read) of Hoop Sensor
     */
    private static final UUID NILSPOD_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int PACKET_SIZE = 4 + 7 + 6;


    // Adding custom NilsPod BLE UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(NILSPOD_STREAMING_SERVICE, "NilsPod Sensor Streaming");
        BleGattAttributes.addCharacteristic(NILSPOD_CONFIG, "NilsPod Sensor Configuration");
        BleGattAttributes.addCharacteristic(NILSPOD_STREAMING, "NilsPod Data Stream");
    }

    /**
     * Sensor commands for communication with NilsPod Sensor. Used with the Sensor Config Characteristic
     */
    private enum NilsPodEcgSensorCommands {
        /**
         * Start Streaming Command
         */
        START_STREAMING((byte) 0xC2),
        /**
         * Stop Streaming Command
         */
        STOP_STREAMING((byte) 0xC1);

        private byte cmd;

        NilsPodEcgSensorCommands(byte cmd) {
            this.cmd = cmd;
        }
    }

    /**
     * Local counter for incoming packages
     */
    private long lastCounter = 0;

    /**
     * Flag indicating whether data should be logged
     */
    private boolean mLoggingEnabled;

    /**
     * Data logger
     */
    private SensorDataLogger mDataLogger;

    /**
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mStreamingService;


    /**
     * Create a new instance of one NilsPod ECG Sensor
     *
     * @param context     Application context
     * @param knownSensor The Sensor reference retrieved from the BLE scan
     */
    public NilsPodEcgSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler, 200);
    }

    @Override
    public void startStreaming() {
        if (send(NilsPodEcgSensorCommands.START_STREAMING)) {
            super.startStreaming();
            if (mLoggingEnabled) {
                try {
                    mDataLogger = new SensorDataLogger(this, mContext);
                } catch (SensorException e) {
                    switch (e.getExceptionType()) {
                        case permissionsMissing:
                            Toast.makeText(mContext, "Permissions to write external storage needed!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        if (send(NilsPodEcgSensorCommands.STOP_STREAMING)) {
            super.stopStreaming();
            if (mDataLogger != null) {
                mDataLogger.completeLogger();
            }
        } else {
            Log.e(TAG, "stopStreaming failed!");
        }
    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (NILSPOD_STREAMING.equals(characteristic.getUuid())) {
                extractSensorData(characteristic);
                return true;
            }
            return false;
        }
    }


    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    private void extractSensorData(BluetoothGattCharacteristic characteristic) {
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
            // TODO fix strange bug of corrupted sample
            if (label == 1) {
                return;
            }
            sendNewData(df);
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (NILSPOD_STREAMING.equals(c.getUuid())) {
            return true;
        }

        return false;
    }


    @Override
    protected void onDiscoveredService(BluetoothGattService service) {
        super.onDiscoveredService(service);
        if (NILSPOD_STREAMING_SERVICE.equals(service.getUuid())) {
            mStreamingService = service;
        }
    }


    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    private boolean send(NilsPodEcgSensorCommands cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getName());
        return send(new byte[]{cmd.cmd});
    }


    private boolean send(byte[] data) {
        if (mStreamingService == null) {
            Log.w(TAG, "Service not found");
            return false;
        }
        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(NILSPOD_CONFIG);
        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mGatt.writeCharacteristic(characteristic);
    }


    /**
     * Enables data logging for this sensor
     */
    public void enableDataLogger() {
        mLoggingEnabled = true;
    }

    /**
     * Disables data logging for this sensor
     */
    public void disableDataLogger() {
        mLoggingEnabled = false;
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
            return ecg[0];
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
