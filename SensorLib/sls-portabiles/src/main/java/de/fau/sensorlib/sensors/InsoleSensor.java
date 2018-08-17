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
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.InsolePressureDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;


/**
 * Represents a Sensor Insole.
 */
public class InsoleSensor extends GenericBleSensor {

    /**
     * UUID for Data Streaming Service of Insole
     */
    private static final UUID INSOLE_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Config Characteristic (write) of Insole
     */
    private static final UUID INSOLE_CONFIG = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (read) of Insole
     */
    private static final UUID INSOLE_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int PACKET_SIZE = 20;


    // Adding custom Insole BLE UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(INSOLE_STREAMING_SERVICE, "Insole Streaming");
        BleGattAttributes.addCharacteristic(INSOLE_CONFIG, "Insole Configuration");
        BleGattAttributes.addCharacteristic(INSOLE_STREAMING, "Insole Data Stream");
    }

    /**
     * Sensor commands for communication with NilsPod Sensor. Used with the Sensor Config Characteristic
     */
    private enum InsoleSensorCommands {
        /**
         * Start Streaming Command
         */
        START_STREAMING(new byte[]{(byte) 0xC2}),
        /**
         * Stop Streaming Command
         */
        STOP_STREAMING(new byte[]{(byte) 0xC1}),
        /**
         * Reset Command
         */
        RESET(new byte[]{(byte) 0xCF, (byte) 0xFF});

        private byte[] cmd;

        InsoleSensorCommands(byte[] cmd) {
            this.cmd = cmd;
        }
    }

    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    private int globalCounter = 0;

    /**
     * Local counter for incoming packages
     */
    private int lastCounter = 0;

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
     * Create a new instance of one Insole
     *
     * @param context     Application context
     * @param knownSensor The Sensor reference retrieved from the BLE scan
     */
    public InsoleSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        super(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler, 200);
    }

    @Override
    public void startStreaming() {
        if (send(InsoleSensorCommands.START_STREAMING)) {
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
        if (send(InsoleSensorCommands.STOP_STREAMING)) {
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
            if (INSOLE_STREAMING.equals(characteristic.getUuid())) {
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

        // one data packet always has size PACKET_SIZE
        if (values.length % PACKET_SIZE != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += PACKET_SIZE) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
            double baro;
            double[] pressure = new double[3];
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

            for (int j = 0; j < 3; j++) {
                pressure[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
                offset++;
            }

            // extract packet counter (only 15 bit, therefore getIntValue() method not applicable)
            localCounter = (values[PACKET_SIZE - 1] & 0xFF) | ((values[PACKET_SIZE - 2] & 0x7F) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 14)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            InsoleDataFrame df = new InsoleDataFrame(this, globalCounter * (2 << 14) + localCounter, accel, gyro, baro, pressure);
            Log.d(TAG, df.toString());
            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mLoggingEnabled) {
                mDataLogger.writeData(df);
            }
        }
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (INSOLE_STREAMING.equals(c.getUuid())) {
            return true;
        }

        return false;
    }


    @Override
    protected void onDiscoveredService(BluetoothGattService service) {
        super.onDiscoveredService(service);
        if (INSOLE_STREAMING_SERVICE.equals(service.getUuid())) {
            mAvailableSensors.add(HardwareSensor.ACCELEROMETER);
            mAvailableSensors.add(HardwareSensor.GYROSCOPE);
            mStreamingService = service;
        }
    }


    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    private boolean send(InsoleSensorCommands cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getName());
        return send(cmd.cmd);
    }


    private boolean send(byte[] data) {
        if (mStreamingService == null) {
            Log.w(TAG, "Service not found");
            return false;
        }
        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(INSOLE_CONFIG);
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
     * Data frame to store data received from the Insole
     */
    public static class InsoleDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame, BarometricPressureDataFrame, InsolePressureDataFrame {

        private long timestamp;
        private double[] accel;
        private double[] gyro;
        private double[] pressure;
        private double baro;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     Array storing acceleration values
         * @param gyro      Array storing gyroscope values
         */
        public InsoleDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro) {
            this(sensor, timestamp, accel, gyro, 0.0, null);
        }

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
            super(sensor, timestamp);
            if (accel.length != 3 || gyro.length != 3) {
                throw new IllegalArgumentException("Illegal array size for " + ((accel.length != 3) ? "acceleration" : "gyroscope") + " values! ");
            }
            this.timestamp = timestamp;
            this.accel = accel;
            this.gyro = gyro;
            this.baro = baro;
            this.pressure = pressure;
        }

        @Override
        public double getGyroX() {
            return gyro[0];
        }

        @Override
        public double getGyroY() {
            return gyro[1];
        }

        @Override
        public double getGyroZ() {
            return gyro[2];
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
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public double[] getPressureArray() {
            if (pressure != null) {
                return pressure;
            } else {
                return new double[]{0.0, 0.0, 0.0};
            }
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + timestamp + ", accel: " + Arrays.toString(accel) + ", gyro: " + Arrays.toString(gyro);
        }
    }
}
