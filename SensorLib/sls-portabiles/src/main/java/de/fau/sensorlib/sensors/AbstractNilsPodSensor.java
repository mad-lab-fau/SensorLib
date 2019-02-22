package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataLogger;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.sensors.configs.BaseConfigItem;

public abstract class AbstractNilsPodSensor extends GenericBleSensor implements Loggable, Resettable {

    public static final String TAG = AbstractNilsPodSensor.class.getSimpleName();

    /**
     * UUID for Data Streaming Service of NilsPod sensor
     */
    protected static final UUID NILS_POD_STREAMING_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Configuration Service of NilsPod Sensor
     */
    protected static final UUID NILS_POD_CONFIGURATION_SERVICE = UUID.fromString("98ff0000-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Config Characteristic (write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_COMMANDS = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for Streaming Characteristic (notification) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_STREAMING = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    /**
     * UUID for System State Characteristic (read) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_SYSTEM_STATE = UUID.fromString("98ff0a0a-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Timer Sampling Config Characteristic (read/write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_TS_CONFIG = UUID.fromString("98ff0101-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Sensor Config Characteristic (read/write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_SENSOR_CONFIG = UUID.fromString("98ff0202-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Metadata Characteristic (read/write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_METADATA_CONFIG = UUID.fromString("98ff0303-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Date Time Characteristic (write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_DATE_TIME_CONFIG = UUID.fromString("98ff0404-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Firmware Version Characteristic (read) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_FIRMWARE_VERSION = UUID.fromString("98ff0f0f-770d-4a83-9e9b-ce6bbd75e472");


    /**
     * Default packet size: 12 Byte IMU + 2 Byte Counter
     */
    protected int mPacketSize = 14;

    /**
     * Byte containing sensor enabled flags
     */
    protected int mEnabledSensors = 0;


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
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mConfigurationService;

    private NilsPodSensorPosition mSensorPosition;

    private HashMap<HardwareSensor, Boolean> mEnabledSensorsMap = new HashMap<>();

    private HashMap<String, BaseConfigItem> mConfigMap = new HashMap<>();

    public static final String KEY_SENSOR_ENABLE = "sensors_enable";


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
        IDLE,
        STREAMING,
        LOGGING,
        FLASH_PAGE_TRANSMISSION,
        FLASH_SESSION_LIST_TRANSMISSION,
        NAND_FLASH_ERASE
    }

    /**
     * Enum describing the power states of the NilsPod sensor
     */
    protected enum NilsPodPowerState {
        NO_POWER,
        WP_CHR_COMPLETE,
        CHR_ACTIVE,
        WP_CHR_ACTIVE
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

    /**
     * Enum describing the sensor error codes read in the NILS_POD_SYSTEM_STATE characteristic.
     */
    protected enum NilsPodSensorError {
        ERROR_BMI_160(0x01),
        ERROR_BMP_280(0x02),
        ERROR_NAND_FLASH(0x04),
        ERROR_RTC(0x08);

        private int errorCode;

        NilsPodSensorError(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
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
        // set sampling rate to default value
        super(context, info.getName(), info.getDeviceAddress(), dataHandler, 200);
    }

    @Override
    public void startStreaming() {
        // enable notifications to subscribe to characteristics (i.e. write descriptors)
        // START_STREAMING command is sent to sensor as soon as all descriptors are written
        // (in onAllGattNotificationsEnabled)
        super.startStreaming();
    }

    @Override
    protected void onAllGattNotificationsEnabled() {
        super.onAllGattNotificationsEnabled();
        if (send(NilsPodSensorCommand.START_STREAMING)) {
            enableLogger();
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        // send STOP_STREAMING command to sensor
        // super.stopStreaming() (where characteristics are unsubscribed) is called in
        // onNewCharacteristicWrite (callback indicating that STOP_STREAMING command was successfully
        // sent to sensor)
        if (send(NilsPodSensorCommand.STOP_STREAMING)) {
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

        return writeCharacteristic(characteristic, data);
    }

    @Override
    public void setLoggerEnabled() {
        mLoggingEnabled = true;
    }

    @Override
    public void setLoggerDisabled() {
        mLoggingEnabled = false;
    }

    @Override
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
        } else if (NILS_POD_CONFIGURATION_SERVICE.equals(service.getUuid())) {
            mConfigurationService = service;
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
                try {
                    readSystemState(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
            } else if (NILS_POD_TS_CONFIG.equals(characteristic.getUuid())) {
                try {
                    readTsConfig(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
            } else if (NILS_POD_SENSOR_CONFIG.equals(characteristic.getUuid())) {
                try {
                    readSensorConfig(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
            } else if (NILS_POD_METADATA_CONFIG.equals(characteristic.getUuid())) {
                try {
                    readSensorPosition(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
            }
            return false;
        }
    }

    @Override
    protected void onNewCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
        super.onNewCharacteristicWrite(characteristic, status);
        if (NILS_POD_SENSOR_CONFIG.equals(characteristic.getUuid())) {
            // sensor config was changed from app side => read characteristic to update
            readEnabledSensors();
        } else if (NILS_POD_COMMANDS.equals(characteristic.getUuid())) {
            // check if the command sent to the sensor was STOP_STREAMING
            byte[] values = characteristic.getValue();
            if (values.length > 0 && (values[0] == NilsPodSensorCommand.STOP_STREAMING.cmd[0])) {
                // call stopStreaming() to unsubscribe from characteristic notifications
                super.stopStreaming();
            }
        }
    }


    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    protected abstract void extractSensorData(BluetoothGattCharacteristic characteristic);

    /**
     * Returns a reference to the BluetoothGattService of the NilsPodStreamingService.
     *
     * @return a reference to the NilsPodStreamingService
     */
    protected BluetoothGattService getStreamingService() {
        return mStreamingService;
    }

    /**
     * Returns a reference to the BluetoothGattService of the NilsPodConfigurationService.
     *
     * @return a reference to the NilsPodConfigurationService
     */
    protected BluetoothGattService getConfigurationService() {
        return mConfigurationService;
    }


    protected void readSystemState(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        boolean connectionState;
        NilsPodOperationState operationState;
        NilsPodPowerState powerState;
        int errorFlags;
        int batteryLevel;
        // no longer in latest firmware version
        //int activityLabel;

        try {
            connectionState = values[offset++] == 1;
            operationState = NilsPodOperationState.values()[values[offset++]];
            powerState = NilsPodPowerState.values()[values[offset++]];
            errorFlags = values[offset++];
            batteryLevel = values[offset];
            //activityLabel = values[offset];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readStateError);
        }
        if (errorFlags > 0) {
            throw new SensorException(SensorException.SensorExceptionType.hardwareSensorError, errorFlags);
        }
        Log.d(TAG, ">>>> System State:");
        Log.d(TAG, "\tConnection State: " + connectionState);
        Log.d(TAG, "\tOperation State: " + operationState);
        Log.d(TAG, "\tWireless Power State: " + powerState);
        Log.d(TAG, "\tError Flags: " + Integer.toBinaryString(errorFlags));
        Log.d(TAG, "\tBattery Level: " + batteryLevel);
        //Log.d(TAG, "\tActivity Label: " + activityLabel);
    }


    protected void readTsConfig(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        NilsPodSyncRole syncRole;
        int syncDistance;
        int syncGroup;

        try {

            requestSamplingRateChange(convertSamplingRate(values[offset++]));
            syncRole = NilsPodSyncRole.values()[values[offset++]];
            syncDistance = values[offset++] * 100;
            syncGroup = values[offset];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readStateError);
        }

        Log.d(TAG, ">>>> Timer Sampling State:");
        Log.d(TAG, "\tSampling Rate: " + mSamplingRate);
        Log.d(TAG, "\tSync Role: " + syncRole);
        Log.d(TAG, "\tSync Distance: " + syncDistance);
        Log.d(TAG, "\tSync Group: " + syncGroup);
    }

    protected void readSensorConfig(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        int sensors;
        int sampleSize;

        try {
            sensors = values[offset++];
            mEnabledSensorsMap.put(HardwareSensor.ACCELEROMETER, ((sensors & 0x01) != 0));
            mEnabledSensorsMap.put(HardwareSensor.GYROSCOPE, ((sensors & 0x01) != 0));
            mEnabledSensorsMap.put(HardwareSensor.FSR, ((sensors & 0x02) != 0));
            mEnabledSensorsMap.put(HardwareSensor.BAROMETER, ((sensors & 0x04) != 0));
            sampleSize = values[offset];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readStateError);
        }

        Log.d(TAG, ">>>> Sensor Config:");
        Log.d(TAG, "\tSensors: IMU: " + isSensorEnabled(HardwareSensor.ACCELEROMETER) + ", Pressure: " + isSensorEnabled(HardwareSensor.FSR) + ", Barometer: " + isSensorEnabled(HardwareSensor.BAROMETER));
        Log.d(TAG, "\tSample Size: " + sampleSize);
        mEnabledSensors = sensors;
        mPacketSize = sampleSize;

        // TODO just for testing
        ArrayList<String> sensorList = (ArrayList<String>) Arrays.asList("ACC", "GYRO", "BARO");
        BaseConfigItem item = new BaseConfigItem("Sensor Config");
        mConfigMap.put(KEY_SENSOR_ENABLE, item);

        if (!isSensorEnabled(HardwareSensor.BAROMETER)) {
            setSensorsEnabled(EnumSet.of(HardwareSensor.BAROMETER), true);
        }
    }

    protected void readSensorPosition(BluetoothGattCharacteristic characteristic) throws SensorException {
        try {
            mSensorPosition = NilsPodSensorPosition.values()[characteristic.getValue()[0]];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readStateError);
        }

        Log.d(TAG, ">>>> Meta Data:");
        Log.d(TAG, "\tSensor Position: " + mSensorPosition);
    }


    public boolean isSensorEnabled(HardwareSensor sensor) {
        return (mEnabledSensorsMap.get(sensor) != null) && mEnabledSensorsMap.get(sensor);
    }


    public boolean setSensorsEnabled(EnumSet<HardwareSensor> sensors, boolean enable) {
        byte value = (byte) mEnabledSensors;

        for (HardwareSensor sensor : sensors) {
            int offset = 0;
            switch (sensor) {
                case FSR:
                    offset = 1;
                    break;
                case BAROMETER:
                    offset = 2;
            }

            if (enable) {
                value = (byte) (value | (1 << offset));
            } else {
                value = (byte) (value & ~(1 << offset));
            }
        }


        BluetoothGattCharacteristic configChara = mGatt.getService(NILS_POD_CONFIGURATION_SERVICE).getCharacteristic(NILS_POD_SENSOR_CONFIG);
        return writeCharacteristic(configChara, new byte[]{value});
    }

    /**
     * Sends a read request to the sensor to check which HardwareSensors are currently enabled.
     */
    public void readEnabledSensors() {
        BluetoothGattCharacteristic configChara = mGatt.getService(NILS_POD_CONFIGURATION_SERVICE).getCharacteristic(NILS_POD_SENSOR_CONFIG);
        readCharacteristic(configChara);
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


    private void handleSensorException(SensorException e) {
        String msg = e.getExceptionType().getMessage();
        switch (e.getExceptionType()) {
            case hardwareSensorError:
                msg += "\n";
                int errCode = e.getErrorCode();
                if ((errCode & NilsPodSensorError.ERROR_BMI_160.errorCode) != 0) {
                    msg += "BMI160 initialization failed.\n";
                }
                if ((errCode & NilsPodSensorError.ERROR_BMP_280.errorCode) != 0) {
                    msg += "BMP280 initialization failed.\n";
                }
                if ((errCode & NilsPodSensorError.ERROR_NAND_FLASH.errorCode) != 0) {
                    msg += "NAND Flash initialization failed.\n";
                }
                if ((errCode & NilsPodSensorError.ERROR_RTC.errorCode) != 0) {
                    msg += "RTC initialization failed.\n";
                }
                e = new SensorException(SensorException.SensorExceptionType.hardwareSensorError, msg);
                break;
        }

        sendNotification(e);
    }

    private void enableLogger() {
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
    }

    public NilsPodSensorPosition getSensorPosition() {
        return mSensorPosition;
    }

    /**
     * Data frame to store data received from the Hoop Sensor
     */
    public static class GenericNilsPodDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame {

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
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", accel: " + Arrays.toString(accel) + ", gyro: " + Arrays.toString(gyro);
        }
    }
}
