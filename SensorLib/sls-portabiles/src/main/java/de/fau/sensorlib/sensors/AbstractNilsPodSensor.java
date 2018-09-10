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
    private static final UUID NILS_POD_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Configuration Service of NilsPod Sensor
     */
    protected static final UUID NILS_POD_CONFIGURATION_SERVICE = UUID.fromString("98ff0000-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Config Characteristic (write) of NilsPod Sensor
     */
    private static final UUID NILS_POD_COMMANDS = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (notification) of NilsPod Sensor
     */
    private static final UUID NILS_POD_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for System State Characteristic (read) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_SYSTEM_STATE = UUID.fromString("98ff0a0a-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Timer Sampling Config Characteristic (read/write) of NilsPod Sensor
     */
    private static final UUID NILS_POD_TS_CONFIG = UUID.fromString("98ff0101-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Sensor Config Characteristic (read/write) of NilsPod Sensor
     */
    private static final UUID NILS_POD_SENSOR_CONFIG = UUID.fromString("98ff0202-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Metadata Characteristic (read/write) of NilsPod Sensor
     */
    private static final UUID NILS_POD_METADATA_CONFIG = UUID.fromString("98ff0303-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Date Time Characteristic (write) of NilsPod Sensor
     */
    private static final UUID NILS_POD_DATE_TIME_CONFIG = UUID.fromString("98ff0404-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Firmware Version Characteristic (read) of NilsPod Sensor
     */
    private static final UUID NILS_POD_FIRMWARE_VERSION = UUID.fromString("98ff0f0f-770d-4a83-9e9b-ce6bbd75e472");


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

    private NilsPodSensorPosition mSensorPosition;


    /**
     * Sensor commands for communication with NilsPod Sensor. Used with the Sensor Config Characteristic
     */
    protected enum NilsPodSensorCommand {
        // CONTROL COMMANDS
        /**
         * Clear all events
         */
        CLEAR_ALL_EVENTS(new byte[]{(byte) 0xC0}),
        /**
         * Stop Streaming Command
         */
        STOP_STREAMING(new byte[]{(byte) 0xC1}),
        /**
         * Start Streaming Command
         */
        START_STREAMING(new byte[]{(byte) 0xC2}),
        /**
         * Stop Logging Command
         */
        STOP_LOGGING(new byte[]{(byte) 0xC3}),
        /**
         * Start Logging Command
         */
        START_LOGGING(new byte[]{(byte) 0xC4}),
        // FLASH COMMANDS
        /**
         * Flash Full Erase Command
         */
        FLASH_FULL_ERASE(new byte[]{(byte) 0xF0}),
        /**
         * Flash Read Config Command
         */
        FLASH_READ_CONFIG(new byte[]{(byte) 0xF2}),
        /**
         * Flash Transmit Session Command
         */
        FLASH_TRANSMIT_SESSION(new byte[]{(byte) 0xF3}),
        /**
         * Flash Transmit Pages Command
         */
        FLASH_TRANSMIT_PAGES(new byte[]{(byte) 0xF4}),
        // CONFIG SET COMMANDS
        /**
         * Set Default Config Command
         */
        SET_DEFAULT_CONFIG(new byte[]{(byte) 0xA0}),
        // RESET SENSOR COMMAND
        /**
         * Reset Command
         */
        RESET(new byte[]{(byte) 0xCF, (byte) 0xFF});

        private byte[] cmd;

        NilsPodSensorCommand(byte[] cmd) {
            this.cmd = cmd;
        }
    }


    /**
     * Enum describing the operation state of the NilsPod sensor
     */
    protected enum NilsPodOperationState {
        OPERATION_STATE_IDLE,
        OPERATION_STATE_STREAMING,
        OPERATION_STATE_LOGGING,
        OPERATION_STATE_FLASH_TRANSMISSION
    }


    /**
     * Enum describing the synchronization role of the NilsPod sensor
     */
    protected enum NilsPodSyncRole {
        SYNC_ROLE_DISABLED,
        SYNC_ROLE_SLAVE,
        SYNC_ROLE_MASTER
    }

    /**
     * Enum describing the sensor position
     */
    protected enum NilsPodSensorPosition {
        NO_POSITION_DEFINED,
        LEFT_FOOT,
        RIGHT_FOOT
    }


    // Add custom NilsPod UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(NILS_POD_STREAMING_SERVICE, "NilsPod Streaming Service");
        BleGattAttributes.addService(NILS_POD_CONFIGURATION_SERVICE, "NilsPod Configuration Service");
        BleGattAttributes.addCharacteristic(NILS_POD_COMMANDS, "NilsPod Sensor Commands");
        BleGattAttributes.addCharacteristic(NILS_POD_STREAMING, "NilsPod Streaming");
        BleGattAttributes.addCharacteristic(NILS_POD_SYSTEM_STATE, "NilsPod System State");
        BleGattAttributes.addCharacteristic(NILS_POD_TS_CONFIG, "NilsPod Timer Sampling Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_SENSOR_CONFIG, "NilsPod Sensor Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_METADATA_CONFIG, "NilsPod Metadata Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_DATE_TIME_CONFIG, "NilsPod Date Time Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_FIRMWARE_VERSION, "NilsPod Firmware Version");
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
                    Log.e(TAG, "create logger");
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
        BluetoothGattCharacteristic characteristic = mStreamingService.getCharacteristic(NILS_POD_COMMANDS);
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


    public boolean reset() {
        if (!send(NilsPodSensorCommand.RESET)) {
            Log.e(TAG, "resetting failed!");
            return false;
        }
        return true;
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

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (NILS_POD_STREAMING.equals(characteristic.getUuid())) {
                extractSensorData(characteristic);
                return true;
            } else if (NILS_POD_SYSTEM_STATE.equals(characteristic.getUuid())) {
                readSystemState(characteristic);
            } else if (NILS_POD_TS_CONFIG.equals(characteristic.getUuid())) {
                readTsConfig(characteristic);
            } else if (NILS_POD_SENSOR_CONFIG.equals(characteristic.getUuid())) {
                readSensorConfig(characteristic);
            } else if (NILS_POD_METADATA_CONFIG.equals(characteristic.getUuid())) {
                readSensorPosition(characteristic);
            }
            return false;
        }
    }

    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    protected abstract void extractSensorData(BluetoothGattCharacteristic characteristic);


    protected void readSystemState(BluetoothGattCharacteristic characteristic) {
        int offset = 0;
        byte[] values = characteristic.getValue();
        boolean connectionState = values[offset++] == 1;
        int operationState = values[offset++];
        boolean wirelessPowerState = values[offset++] == 0;
        boolean chargingState = values[offset] == 0;
        // TODO determine error states of sensors
        Log.d(TAG, ">>>> System State:");
        Log.d(TAG, "\tConnection State: " + connectionState);
        Log.d(TAG, "\tOperation State: " + NilsPodOperationState.values()[operationState]);
        Log.d(TAG, "\tWireless Power State: " + wirelessPowerState);
        Log.d(TAG, "\tCharging State: " + chargingState);
    }

    protected void readTsConfig(BluetoothGattCharacteristic characteristic) {
        int offset = 0;
        byte[] values = characteristic.getValue();
        mSamplingRate = convertSamplingRate(values[offset++]);
        NilsPodSyncRole syncRole = NilsPodSyncRole.values()[values[offset++]];
        int syncDistance = values[offset++] * 100;
        int rfGroup = values[offset];

        Log.d(TAG, ">>>> Timer Sampling State:");
        Log.d(TAG, "\tSampling Rate: " + mSamplingRate);
        Log.d(TAG, "\tSync Role: " + syncRole);
        Log.d(TAG, "\tSync Distance: " + syncDistance);
        Log.d(TAG, "\tRF Group: " + rfGroup);
    }

    protected void readSensorConfig(BluetoothGattCharacteristic characteristic) {
        int offset = 0;
        byte[] values = characteristic.getValue();
        int sensors = values[offset++];
        int sampleSize = values[offset];
        Log.d(TAG, ">>>> Sensor Config:");
        Log.d(TAG, "\tSensors: " + sensors);
        Log.d(TAG, "\tSample Size: " + sampleSize);
        PACKET_SIZE = sampleSize;
    }

    protected void readSensorPosition(BluetoothGattCharacteristic characteristic) {
        mSensorPosition = NilsPodSensorPosition.values()[characteristic.getValue()[0]];
        Log.d(TAG, ">>>> Meta Data:");
        Log.d(TAG, "\tSensor Position: " + mSensorPosition);
    }

    private double convertSamplingRate(byte value) {
        switch (value) {
            case 20:
                return 61.0;
            case 10:
                return 100.0;
            case 5:
                return 200.0;
            case 4:
                return 250.0;
            case 3:
                return 333.3;
            case 2:
                return 500.0;
            case 1:
                return 1000.0;
        }
        return 0.0;
    }

    public NilsPodSensorPosition getSensorPosition() {
        return mSensorPosition;
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
