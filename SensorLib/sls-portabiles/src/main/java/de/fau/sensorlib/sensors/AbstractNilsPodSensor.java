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
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

public abstract class AbstractNilsPodSensor extends GenericBleSensor {


    /**
     * UUID for Data Streaming Service of NilsPod sensor
     */
    protected static final UUID NILS_POD_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Config Characteristic (write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_CONFIG = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (read) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");


    /**
     * Default packet size: 12 Byte IMU + 2 Byte Counter
     */
    protected static int PACKET_SIZE = 14;


    /**
     * Local counter for incoming packages
     */
    protected long lastCounter = 0;

    /**
     * Flag indicating whether data should be logged
     */
    protected boolean mLoggingEnabled;

    /**
     * Data logger
     */
    protected SensorDataLogger mDataLogger;

    /**
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mStreamingService;


    /**
     * Sensor commands for communication with NilsPod Sensor. Used with the Sensor Config Characteristic
     */
    protected enum NilsPodSensorCommand {
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

        NilsPodSensorCommand(byte[] cmd) {
            this.cmd = cmd;
        }
    }


    // Add custom NilsPod UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(NILS_POD_STREAMING_SERVICE, "NilsPod Sensor Streaming");
        BleGattAttributes.addCharacteristic(NILS_POD_CONFIG, "NilsPod Sensor Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_STREAMING, "NilsPod Data Stream");
    }


    public AbstractNilsPodSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
    }

    @Override
    public void startStreaming() {
        if (send(NilsPodSensorCommand.START_STREAMING)) {
            super.startStreaming();
            try {
                if (mLoggingEnabled) {
                    mDataLogger = new SensorDataLogger(this, mContext);
                }
            } catch (SensorException e) {
                switch (e.getExceptionType()) {
                    case permissionsMissing:
                        Toast.makeText(mContext, "Permissions to write external storage needed!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        if (send(NilsPodSensorCommand.STOP_STREAMING)) {
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
            if (NILS_POD_STREAMING.equals(characteristic.getUuid())) {
                extractSensorData(characteristic);
                return true;
            }
            return false;
        }
    }


    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    private boolean send(NilsPodSensorCommand cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getName());
        return send(cmd.cmd);
    }

    private boolean send(byte[] data) {
        if (mStreamingService == null) {
            Log.w(TAG, "Service not found");
            return false;
        }
        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(NILS_POD_CONFIG);
        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mGatt.writeCharacteristic(characteristic);
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (NILS_POD_STREAMING.equals(c.getUuid())) {
            return true;
        }

        return false;
    }


    @Override
    protected void onDiscoveredService(BluetoothGattService service) {
        super.onDiscoveredService(service);
        if (NILS_POD_STREAMING_SERVICE.equals(service.getUuid())) {
            mStreamingService = service;
        }
    }


    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    protected abstract void extractSensorData(BluetoothGattCharacteristic characteristic);


    public boolean reset() {
        if (!send(NilsPodSensorCommand.RESET)) {
            Log.e(TAG, "resetting failed!");
            return false;
        }
        return true;
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
    public static class GenericNilsPodDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame {

        protected long timestamp;
        protected double[] accel;
        protected double[] gyro;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public GenericNilsPodDataFrame(GenericBleSensor sensor, long timestamp, double[] accel, double[] gyro) {
            super(sensor, timestamp);
            if (accel.length != 3 || gyro.length != 3) {
                throw new IllegalArgumentException("Illegal array size for " + ((accel.length != 3) ? "acceleration" : "gyroscope") + " values! ");
            }
            this.timestamp = timestamp;
            this.accel = accel;
            this.gyro = gyro;
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
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + timestamp + ", accel: " + Arrays.toString(accel) + ", gyro: " + Arrays.toString(gyro);
        }
    }
}