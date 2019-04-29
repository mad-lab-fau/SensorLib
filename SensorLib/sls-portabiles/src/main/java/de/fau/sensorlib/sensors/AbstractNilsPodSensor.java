package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import de.fau.sensorlib.BleGattAttributes;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorDataRecorder;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorMessage;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.configs.ConfigItem;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;

public abstract class AbstractNilsPodSensor extends GenericBleSensor implements Recordable, Resettable {

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
     * UUID for Secure DFU (Device Firmware Update) Service of NilsPod Sensor
     */
    protected static final UUID NILS_POD_SECURE_DFU_SERVICE = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb");
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
     * UUID for System Settings Characteristic (read/write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_SYSTEM_SETTINGS_CONFIG = UUID.fromString("98ff0303-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Date Time Characteristic (write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_DATE_TIME_CONFIG = UUID.fromString("98ff0404-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Firmware Version Characteristic (read) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_FIRMWARE_VERSION = UUID.fromString("98ff0f0f-770d-4a83-9e9b-ce6bbd75e472");
    /**
     * UUID for Buttonless DFU Characteristic (write) of NilsPod Sensor
     */
    protected static final UUID NILS_POD_BUTTONLESS_DFU = UUID.fromString("8ec90003-f315-4f60-9fb8-838830daea50");

    /**
     * Default packet size: 12 Byte IMU + 2 Byte Counter
     */
    protected int mPacketSize = 14;

    /**
     * Local counter for incoming packages
     */
    protected long lastCounter = 0;

    /**
     * Flag indicating whether data should be logged
     */
    protected boolean mRecordingEnabled;

    private boolean mShouldDisconnect;

    /**
     * Data logger
     */
    protected SensorDataRecorder mDataRecorder;

    /**
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mStreamingService;

    /**
     * Keep a local reference to the Streaming Service
     */
    private BluetoothGattService mConfigurationService;

    private NilsPodSensorPosition mSensorPosition;

    private ArrayList<HardwareSensor> mEnabledSensorList = new ArrayList<>();

    private NilsPodOperationState mOperationState = NilsPodOperationState.IDLE;

    private boolean mMotionInterruptEnabled = false;

    private NilsPodOperationMode mOperationMode = NilsPodOperationMode.NORMAL_MODE;

    private SensorRecorderListener mSensorRecorderListener;


    public static final String KEY_SAMPLING_RATE = "sampling_rate";
    public static final String KEY_HARDWARE_SENSORS = "hardware_sensors";
    public static final String KEY_MOTION_INTERRUPT = "motion_interrupt";
    public static final String KEY_SYNC_ROLE = "sync_role";
    public static final String KEY_SYNC_GROUP = "sync_group";
    public static final String KEY_SYNC_DISTANCE = "sync_distance";
    public static final String KEY_SENSOR_POSITION = "sensor_position";
    public static final String KEY_OPERATION_MODE = "operation_mode";

    protected static HashMap<String, ConfigItem> mConfigMap = new LinkedHashMap<>();

    static {
        sAvailableSamplingRates.put("102.4 Hz", 102.4);
        sAvailableSamplingRates.put("204.8 Hz", 204.8);
        sAvailableSamplingRates.put("256.0 Hz", 256.0);
        sAvailableSamplingRates.put("512.0 Hz", 512.0);
        sAvailableSamplingRates.put("1024.0 Hz", 1024.0);
    }

    protected static SparseArray<Double> sSamplingRateCommands = new SparseArray<>();

    static {
        sSamplingRateCommands.put(1, 1024.0);
        sSamplingRateCommands.put(2, 512.0);
        sSamplingRateCommands.put(4, 256.0);
        sSamplingRateCommands.put(5, 204.8);
        sSamplingRateCommands.put(10, 102.4);
    }

    protected static HashMap<Integer, String> sSyncDistanceCommands = new LinkedHashMap<>();

    static {
        sSyncDistanceCommands.put(1, "100 ms");
        sSyncDistanceCommands.put(2, "200 ms");
        sSyncDistanceCommands.put(3, "300 ms");
        sSyncDistanceCommands.put(10, "1 s");
        sSyncDistanceCommands.put(20, "2 s");
        sSyncDistanceCommands.put(50, "5 s");
        sSyncDistanceCommands.put(100, "10 s");
        sSyncDistanceCommands.put(200, "20 s");
    }


    protected static ConfigItem sSamplingRateConfig = new ConfigItem(
            "Sampling Rate",
            new ArrayList<Object>(sAvailableSamplingRates.keySet()),
            ConfigItem.UiType.TYPE_DROPDOWN
    );
    protected static ConfigItem sSensorConfig = new ConfigItem(
            "Sensors",
            new ArrayList<Object>(EnumSet.of(HardwareSensor.ACCELEROMETER, HardwareSensor.GYROSCOPE, HardwareSensor.BAROMETER)),
            ConfigItem.UiType.TYPE_MULTI_SELECT
    );
    protected static ConfigItem sMotionInterruptConfig = new ConfigItem(
            "Motion Interrupt",
            new ArrayList<Object>(Arrays.asList(NilsPodMotionInterrupt.values())),
            ConfigItem.UiType.TYPE_SELECT
    );
    protected static ConfigItem sSyncRoleConfig = new ConfigItem(
            "Sync Role",
            new ArrayList<Object>(Arrays.asList(NilsPodSyncRole.values())),
            ConfigItem.UiType.TYPE_SELECT
    );
    protected static ConfigItem sSyncGroupConfig = new ConfigItem(
            "Sync Group",
            new ArrayList<Object>(Arrays.asList(NilsPodSyncGroup.values())),
            ConfigItem.UiType.TYPE_DROPDOWN
    );
    protected static ConfigItem sSyncDistanceConfig = new ConfigItem(
            "Sync Distance",
            new ArrayList<Object>(sSyncDistanceCommands.values()),
            ConfigItem.UiType.TYPE_DROPDOWN
    );
    protected static ConfigItem sOperationModeConfig = new ConfigItem(
            "Operation Mode",
            new ArrayList<Object>(Arrays.asList(NilsPodOperationMode.values())),
            ConfigItem.UiType.TYPE_SELECT
    );
    protected static ConfigItem sSensorPositionConfig = new ConfigItem(
            "SensorPosition",
            new ArrayList<Object>(Arrays.asList(NilsPodSensorPosition.values())),
            ConfigItem.UiType.TYPE_DROPDOWN
    );

    static {
        mConfigMap.put(KEY_SAMPLING_RATE, sSamplingRateConfig);
        mConfigMap.put(KEY_HARDWARE_SENSORS, sSensorConfig);
        mConfigMap.put(KEY_MOTION_INTERRUPT, sMotionInterruptConfig);
        mConfigMap.put(KEY_SENSOR_POSITION, sSensorPositionConfig);
        mConfigMap.put(KEY_SYNC_ROLE, sSyncRoleConfig);
        mConfigMap.put(KEY_SYNC_GROUP, sSyncGroupConfig);
        mConfigMap.put(KEY_SYNC_DISTANCE, sSyncDistanceConfig);
        mConfigMap.put(KEY_OPERATION_MODE, sOperationModeConfig);
    }

    protected HashMap<String, Object> mCurrentConfigMap = new LinkedHashMap<>();


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
         * Flash Clear Sessions Command
         */
        FLASH_CLEAR_SESSIONS(new byte[]{(byte) 0xF1}),
        /**
         * Flash Read Session List Command
         */
        FLASH_READ_SESSION_LIST(new byte[]{(byte) 0xF2}),
        /**
         * Flash Transmit Session Command
         */
        FLASH_TRANSMIT_SESSION(new byte[]{(byte) 0xF3, 0x00}),
        /**
         * Flash Transmit Pages Command (only for debugging)
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

        public byte[] getByteCmd() {
            return cmd;
        }
    }


    /**
     * Enum describing the operation state of the NilsPod sensor
     */
    public enum NilsPodOperationState {
        IDLE,
        STREAMING,
        LOGGING,
        DOWNLOADING_SESSION,
        READING_SESSION_LIST,
        FLASH_ERASE,
        SAVING_CONFIG
    }

    /**
     * Enum describing the power states of the NilsPod sensor
     */
    protected enum NilsPodPowerState {
        NO_POWER(0x00),
        WP_CHR_COMPLETE(0x01),
        CHR_ACTIVE(0x10),
        WP_CHR_ACTIVE(0x11);

        private int powerState;

        NilsPodPowerState(int powerState) {
            this.powerState = powerState;
        }

        public static NilsPodPowerState inferPowerState(int powerState) {

            if (powerState == NO_POWER.getPowerState()) {
                return NO_POWER;
            } else if (powerState == WP_CHR_COMPLETE.getPowerState()) {
                return WP_CHR_COMPLETE;
            } else if (powerState == CHR_ACTIVE.getPowerState()) {
                return CHR_ACTIVE;
            } else if (powerState == WP_CHR_ACTIVE.getPowerState()) {
                return WP_CHR_ACTIVE;
            } else {
                return null;
            }
        }

        public int getPowerState() {
            return powerState;
        }
    }


    /**
     * Enum describing the sensor position
     */
    protected enum NilsPodSensorPosition {
        NO_POSITION_DEFINED,
        LEFT_FOOT,
        RIGHT_FOOT,
        HIP,
        LEFT_WRIST,
        RIGHT_WRIST,
        CHEST;
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

    protected enum NilsPodMotionInterrupt {
        MOTION_INTERRUPT_DISABLED,
        MOTION_INTERRUPT_ENABLED,
    }

    protected enum NilsPodOperationMode {
        NORMAL_MODE,
        HOME_MONITORING_MODE,
    }

    // Add custom NilsPod UUIDs to known UUID pool
    static {
        BleGattAttributes.addService(NILS_POD_STREAMING_SERVICE, "NilsPod Streaming Service");
        BleGattAttributes.addService(NILS_POD_CONFIGURATION_SERVICE, "NilsPod Configuration Service");
        BleGattAttributes.addService(NILS_POD_SECURE_DFU_SERVICE, "NilsPod Secure DFU Service");
        BleGattAttributes.addCharacteristic(NILS_POD_COMMANDS, "NilsPod Sensor Commands");
        BleGattAttributes.addCharacteristic(NILS_POD_STREAMING, "NilsPod Streaming");
        BleGattAttributes.addCharacteristic(NILS_POD_SYSTEM_STATE, "NilsPod System State");
        BleGattAttributes.addCharacteristic(NILS_POD_TS_CONFIG, "NilsPod Timer Sampling Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_SENSOR_CONFIG, "NilsPod Sensor Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_SYSTEM_SETTINGS_CONFIG, "NilsPod System Settings Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_DATE_TIME_CONFIG, "NilsPod Date Time Configuration");
        BleGattAttributes.addCharacteristic(NILS_POD_FIRMWARE_VERSION, "NilsPod Firmware Version");
        BleGattAttributes.addCharacteristic(NILS_POD_BUTTONLESS_DFU, "NilsPod Buttonless DFU");
    }


    public AbstractNilsPodSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        // set sampling rate to default value
        super(context, info.getDeviceName(), info.getDeviceAddress(), dataHandler, BleConnectionMode.MODE_NILSPOD);
    }

    @Override
    public void startStreaming() {
        super.startStreaming();
        // send START_STREAMING command to NilsPod
        if (send(NilsPodSensorCommand.START_STREAMING)) {
            enableRecorder();
        } else {
            Log.e(TAG, "startStreaming failed!");
        }
    }

    @Override
    public void stopStreaming() {
        super.stopStreaming();
        // send STOP_STREAMING command to sensor
        if (send(NilsPodSensorCommand.STOP_STREAMING)) {
            disableRecorder();
        } else {
            Log.e(TAG, "stopStreaming failed!");
        }
    }

    @Override
    public void disconnect() {
        if (getOperationState() == NilsPodOperationState.STREAMING) {
            mShouldDisconnect = true;
            stopStreaming();
        } else {
            super.disconnect();
        }
    }

    @Override
    protected void onStateChange(SensorState oldState, SensorState newState) {
        super.onStateChange(oldState, newState);

        if (newState == SensorState.CONNECTED) {
            if (getOperationState() == NilsPodOperationState.LOGGING) {
                setState(SensorState.LOGGING);
            }

            if (oldState == SensorState.STREAMING && mShouldDisconnect) {
                mShouldDisconnect = false;
                disconnect();
            }
        }
    }

    protected void onOperationStateChanged(NilsPodOperationState oldState, NilsPodOperationState newState) throws SensorException {
        Log.d(TAG, "<" + getDeviceName() + "> onOperationStateChanged: <" + oldState + "> -> <" + newState + ">");

        switch (oldState) {
            case IDLE:
                switch (newState) {
                    case STREAMING:
                        sendStartStreaming();
                        break;
                }
                break;
            case STREAMING:
                switch (newState) {
                    case IDLE:
                        sendStopStreaming();
                        break;
                }
                break;
        }

        sendNotification(SensorMessage.OPERATION_STATE_CHANGED);
    }

    public NilsPodOperationState getOperationState() {
        return mOperationState;
    }

    protected void setOperationState(NilsPodOperationState operationState) throws SensorException {
        NilsPodOperationState oldState = mOperationState;
        mOperationState = operationState;
        onOperationStateChanged(oldState, mOperationState);
    }

    /**
     * Send command to sensor via Config Characteristic
     *
     * @param cmd Sensor Command
     * @return true if data has been successfully sent, false otherwise
     */
    protected boolean send(NilsPodSensorCommand cmd) {
        Log.d(TAG, "Sending " + cmd + " command to " + getDeviceName());
        if (!send(cmd.cmd)) {
            Log.e(TAG, cmd + " failed!");
            return false;
        }
        return true;
    }

    protected boolean send(byte[] data) {
        if (getStreamingService() == null) {
            Log.w(TAG, "Service not found");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getStreamingService().getCharacteristic(NILS_POD_COMMANDS);
        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        while (getOperationState() == NilsPodOperationState.SAVING_CONFIG) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return writeCharacteristic(characteristic, data);
    }

    public void setSensorRecorderListener(SensorRecorderListener listener) {
        mSensorRecorderListener = listener;
    }

    @Override
    public void setRecorderEnabled() {
        mRecordingEnabled = true;
    }

    @Override
    public void setRecorderDisabled() {
        mRecordingEnabled = false;
    }

    @Override
    public void reset() {
        send(NilsPodSensorCommand.RESET);
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (super.shouldEnableNotification(c)) {
            return true;
        } else if (NILS_POD_STREAMING.equals(c.getUuid())) {
            return true;
        } else if (NILS_POD_SYSTEM_STATE.equals(c.getUuid())) {
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
                if (getOperationState() == NilsPodOperationState.STREAMING || isStreaming()) {
                    extractSensorData(characteristic);
                    return true;
                }
            } else if (NILS_POD_SYSTEM_STATE.equals(characteristic.getUuid())) {
                try {
                    extractSystemState(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
                return true;
            } else if (NILS_POD_TS_CONFIG.equals(characteristic.getUuid())) {
                try {
                    extractTsConfig(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
                return true;
            } else if (NILS_POD_SENSOR_CONFIG.equals(characteristic.getUuid())) {
                try {
                    extractSensorConfig(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
                return true;
            } else if (NILS_POD_SYSTEM_SETTINGS_CONFIG.equals(characteristic.getUuid())) {
                try {
                    extractSystemSettings(characteristic);
                } catch (SensorException e) {
                    handleSensorException(e);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onNewCharacteristicWrite(BluetoothGattCharacteristic characteristic, int status) {
        super.onNewCharacteristicWrite(characteristic, status);
        if (NILS_POD_SENSOR_CONFIG.equals(characteristic.getUuid())) {
            // sensor config was changed from app side => read characteristic to update
            readSensorConfig();
        } else if (NILS_POD_TS_CONFIG.equals(characteristic.getUuid())) {
            readTsConfig();
        } else if (NILS_POD_SYSTEM_SETTINGS_CONFIG.equals(characteristic.getUuid())) {
            readSystemSettings();
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


    protected synchronized void extractSystemState(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        boolean connectionState;
        NilsPodOperationState operationState;
        NilsPodPowerState powerState;
        int errorFlags;
        // no longer in latest firmware version
        //int activityLabel;

        try {
            connectionState = values[offset++] == 1;
            operationState = NilsPodOperationState.values()[values[offset++]];
            NilsPodOperationState oldState = mOperationState;
            // set new state
            mOperationState = operationState;
            // call callback
            onOperationStateChanged(oldState, mOperationState);
            powerState = NilsPodPowerState.inferPowerState(values[offset++]);
            errorFlags = values[offset++];
            mBatteryLevel = values[offset];
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
        Log.d(TAG, "\tBattery Level: " + mBatteryLevel);
        //Log.d(TAG, "\tActivity Label: " + activityLabel);
    }


    protected synchronized void extractTsConfig(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        NilsPodSyncRole syncRole;
        int syncDistance;
        NilsPodSyncGroup syncGroup;

        try {
            double samplingRate = inferSamplingRate(values[offset++]);
            requestSamplingRateChange(samplingRate);
            syncRole = NilsPodSyncRole.values()[values[offset++]];
            syncDistance = values[offset++];
            syncGroup = NilsPodSyncGroup.values()[values[offset]];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readConfigError);
        }

        Log.d(TAG, ">>>> Timer Sampling State:");
        Log.d(TAG, "\tSampling Rate: " + mSamplingRate);
        Log.d(TAG, "\tSync Role: " + syncRole);
        Log.d(TAG, "\tSync Distance: " + syncDistance * 100);
        Log.d(TAG, "\tSync Group: " + syncGroup);

        mCurrentConfigMap.put(KEY_SAMPLING_RATE, getSamplingRateString(mSamplingRate));
        mCurrentConfigMap.put(KEY_SYNC_ROLE, syncRole);
        mCurrentConfigMap.put(KEY_SYNC_GROUP, syncGroup);
        mCurrentConfigMap.put(KEY_SYNC_DISTANCE, sSyncDistanceCommands.get(syncDistance));
    }

    protected synchronized void extractSensorConfig(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        byte[] values = characteristic.getValue();
        int sensors;
        int sampleSize;
        mEnabledSensorList = new ArrayList<>();

        try {
            sensors = values[offset++];
            if ((sensors & 0x01) != 0) {
                mEnabledSensorList.add(HardwareSensor.ACCELEROMETER);
                mEnabledSensorList.add(HardwareSensor.GYROSCOPE);
            }
            if ((sensors & 0x02) != 0) {
                mEnabledSensorList.add(HardwareSensor.FSR);
            }
            if ((sensors & 0x04) != 0) {
                mEnabledSensorList.add(HardwareSensor.BAROMETER);
            }
            if ((sensors & 0x20) != 0) {
                mEnabledSensorList.add(HardwareSensor.ECG);
            }
            sampleSize = values[offset];
        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readConfigError);
        }

        Log.d(TAG, ">>>> Sensor Config:");
        Log.d(TAG, "\tEnabled Sensors: " + mEnabledSensorList);
        Log.d(TAG, "\tSample Size: " + sampleSize);
        mPacketSize = sampleSize;
        mCurrentConfigMap.put(KEY_HARDWARE_SENSORS, mEnabledSensorList);
    }

    protected synchronized void extractSystemSettings(BluetoothGattCharacteristic characteristic) throws SensorException {
        int offset = 0;
        try {
            int sensorPosition = characteristic.getValue()[offset++];
            if (sensorPosition < NilsPodSensorPosition.values().length) {
                mSensorPosition = NilsPodSensorPosition.values()[sensorPosition];
            } else {
                mSensorPosition = NilsPodSensorPosition.NO_POSITION_DEFINED;
            }
            int operationMode = characteristic.getValue()[offset];
            mOperationMode = (operationMode & 0x40) == 0 ? NilsPodOperationMode.NORMAL_MODE : NilsPodOperationMode.HOME_MONITORING_MODE;
            mMotionInterruptEnabled = (operationMode & 0x80) != 0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new SensorException(SensorException.SensorExceptionType.readStateError);
        }

        Log.d(TAG, ">>>> System Settings:");
        Log.d(TAG, "\tSensor Position: " + mSensorPosition);
        Log.d(TAG, "\tOperation Mode: " + mOperationMode);
        Log.d(TAG, "\tMotion Interrupt Enabled: " + mMotionInterruptEnabled);

        mCurrentConfigMap.put(KEY_MOTION_INTERRUPT, mMotionInterruptEnabled ? NilsPodMotionInterrupt.MOTION_INTERRUPT_ENABLED : NilsPodMotionInterrupt.MOTION_INTERRUPT_DISABLED);
        mCurrentConfigMap.put(KEY_OPERATION_MODE, mOperationMode);
        mCurrentConfigMap.put(KEY_SENSOR_POSITION, mSensorPosition);
    }


    public boolean isSensorEnabled(HardwareSensor sensor) {
        return mEnabledSensorList.contains(sensor);
    }

    /**
     * Sends a read request to the sensor to check which HardwareSensors are currently enabled.
     */
    public void readSensorConfig() {
        if (getConfigurationService() != null) {
            BluetoothGattCharacteristic configChara = getConfigurationService().getCharacteristic(NILS_POD_SENSOR_CONFIG);
            readCharacteristic(configChara);
        }
    }

    public void readTsConfig() {
        if (getConfigurationService() != null) {
            BluetoothGattCharacteristic configChara = getConfigurationService().getCharacteristic(NILS_POD_TS_CONFIG);
            readCharacteristic(configChara);
        }
    }

    public void readSystemSettings() {
        if (getConfigurationService() != null) {
            BluetoothGattCharacteristic configChara = getConfigurationService().getCharacteristic(NILS_POD_SYSTEM_SETTINGS_CONFIG);
            readCharacteristic(configChara);
        }
    }

    /**
     * Sends a read request to the sensor to read the current system state.
     */
    public void readSystemState() {
        if (getConfigurationService() != null) {
            BluetoothGattCharacteristic systemStateChara = getConfigurationService().getCharacteristic(NILS_POD_SYSTEM_STATE);
            readCharacteristic(systemStateChara);
        }
    }

    public static double inferSamplingRate(int value) {
        return sSamplingRateCommands.get(value, 0.0);
    }

    public static String getSamplingRateString(double samplingRate) {
        for (Map.Entry<String, Double> entry : sAvailableSamplingRates.entrySet()) {
            if (entry.getValue() == samplingRate) {
                return entry.getKey();
            }
        }
        return "";
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

    private void enableRecorder() {
        try {
            if (mRecordingEnabled) {
                mDataRecorder = new SensorDataRecorder(this, mContext);
                if (mSensorRecorderListener != null) {
                    mSensorRecorderListener.onSensorRecordingStarted(mDataRecorder);
                }
            }
        } catch (SensorException e) {
            switch (e.getExceptionType()) {
                case permissionsMissing:
                    Toast.makeText(mContext, "Permissions to write external storage needed!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void disableRecorder() {
        if (mDataRecorder != null) {
            mDataRecorder.completeRecorder();
            if (mSensorRecorderListener != null) {
                mSensorRecorderListener.onSensorRecordingFinished(mDataRecorder);
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
        public GenericNilsPodDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro) {
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
