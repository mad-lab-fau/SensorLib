/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
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

import java.util.Arrays;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataLogger;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.enums.SensorState;

/**
 * Implementation of the Simblee sensor.
 */
public class SimbleeSensor extends GenericBleSensor {

    private static final String TAG = SimbleeSensor.class.getSimpleName();

    // ECG SERVICE
    public final static UUID SIMBLEE_SERVICE = UUID.fromString("00000fe84-0000-1000-8000-00805f9b34fb");
    public final static UUID SIMBLEE_RECEIVE = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
    public final static UUID SIMBLEE_CONFIG = UUID.fromString("2d30c083-f39f-4ce6-923f-3484ea480596");
    //public final static UUID SIMBLEE_DISCONNECT = UUID.fromString("2d30c084-f39f-4ce6-923f-3484ea480596");

    static {
        BleGattAttributes.addService(SIMBLEE_SERVICE, "Simblee Service");
        BleGattAttributes.addCharacteristic(SIMBLEE_RECEIVE, "Simblee Receive");
        BleGattAttributes.addCharacteristic(SIMBLEE_CONFIG, "Simblee Configuration");
    }

    /**
     * Sensor commands for communication with SimbleeSensor. Used with the Sensor Config Characteristic
     */
    private enum SimbleeSensorCommands {
        /**
         * Start Streaming Command
         */
        START_STREAMING((byte) 0xC2),
        /**
         * Stop Streaming Command
         */
        STOP_STREAMING((byte) 0xC1);

        private byte cmd;

        SimbleeSensorCommands(byte cmd) {
            this.cmd = cmd;
        }
    }


    /**
     * Global counter for incoming packages (local counter only has 16 bit)
     */
    private int globalCounter = 0;

    /**
     * Local counter for incoming packages
     */
    private int lastCounter = 0;

    /**
     * Keep local reference to streaming service
     */
    private BluetoothGattService mStreamingService;

    /**
     * Flag indicating whether data should be logged
     */
    private boolean mLoggingEnabled;

    /**
     * Data logger
     */
    private SensorDataLogger mDataLogger;


    public static class SimbleeDataFrame extends SensorDataFrame implements AccelDataFrame, EcgDataFrame {

        private long timestamp;
        private double[] accel;
        private double[] ecg;

        public SimbleeDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] ecg) {
            super(sensor, timestamp);
            this.timestamp = timestamp;
            this.accel = accel;
            this.ecg = ecg;
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
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + timestamp + ", accel: " + Arrays.toString(accel) + ", ecg: " + Arrays.toString(ecg);
        }

        @Override
        public double getEcgSample() {
            return ecg[0];
        }

        @Override
        public double getSecondaryEcgSample() {
            return ecg[1];
        }
    }

    public SimbleeSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler);
    }

    public SimbleeSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler, 250);
    }

    @Override
    public void startStreaming() {
        if (send(SimbleeSensorCommands.START_STREAMING)) {
            super.startStreaming();
            if (mLoggingEnabled) {
                mDataLogger = new SensorDataLogger(this, mContext);
            }
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        if (send(SimbleeSensorCommands.STOP_STREAMING)) {
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
            if (SIMBLEE_RECEIVE.equals(characteristic.getUuid()) && getState().ordinal() > SensorState.CONNECTING.ordinal()) {
                extractSensorData(characteristic);
                return true;
            }
            return false;
        }
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (SIMBLEE_RECEIVE.equals(c.getUuid())) {
            return true;
        }

        return false;
    }

    @Override
    protected void onDiscoveredService(BluetoothGattService service) {
        super.onDiscoveredService(service);
        if (SIMBLEE_SERVICE.equals(service.getUuid())) {
            mStreamingService = service;
            mAvailableSensors.addAll(KnownSensor.SIMBLEE.getAvailableSensors());
        }
    }

    private void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        if (values.length == 0) {
            return;
        }
        int offset = 0;
        double[] accel = new double[3];
        double[] ecg = new double[2];
        int localCounter;
        for (int i = 0; i < 3; i++) {
            accel[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
            offset += 2;
        }

        for (int i = 0; i < 2; i++) {
            int tmp = ((values[offset] & 0xFF) | ((values[offset + 1] & 0xFF) << 8) | ((values[offset + 2] & 0xFF) << 16));
            tmp = tmp << 8;
            // shift to fill 32 bit integer with sign bit
            ecg[i] = tmp >> 16;
            offset += 3;
        }

        localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);

        if (((localCounter - lastCounter) % (2 << 15)) > 1) {
            Log.w(TAG, this + ": BLE Packet Loss!");
        }
        // increment global counter if local counter overflows
        if (localCounter < lastCounter) {
            globalCounter++;
        }

        SimbleeDataFrame df = new SimbleeDataFrame(this, globalCounter * (2 << 15) + localCounter, accel, ecg);
        Log.d(TAG, df.toString());
        sendNewData(df);
        lastCounter = localCounter;
        if (mLoggingEnabled) {
            mDataLogger.writeData(df);
        }

    }

    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    private boolean send(SimbleeSensorCommands cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getName());
        return send(new byte[]{cmd.cmd});
    }

    public boolean send(byte[] data) {
        if (mStreamingService == null) {
            Log.w(TAG, "Service not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(SIMBLEE_CONFIG);
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
}
