/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.configs.ConfigItem;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.logging.NilsPodLoggable;
import de.fau.sensorlib.sensors.logging.NilsPodLoggingCallback;
import de.fau.sensorlib.sensors.logging.Session;
import de.fau.sensorlib.sensors.logging.SessionDownloader;
import de.fau.sensorlib.sensors.logging.SessionHandler;


/**
 * Represents a NilsPod Sensor device.
 */
public class NilsPodSensor extends AbstractNilsPodSensor implements NilsPodLoggable, Configurable {

    private static final String TAG = NilsPodSensor.class.getSimpleName();

    protected ArrayList<NilsPodLoggingCallback> mCallbacks;

    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    protected int globalCounter = 0;


    private SessionHandler mSessionHandler;
    private SessionDownloader mSessionDownloader;

    private ConcurrentLinkedQueue<BluetoothGattCharacteristic> mConfigWriteRequests = new ConcurrentLinkedQueue<>();


    public NilsPodSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
        mCallbacks = new ArrayList<>();
        mConfigWriteRequests.clear();
    }

    @Override
    public void startStreaming() {
        super.startStreaming();
        lastCounter = 0;
        globalCounter = 0;
    }


    public void addNilsPodLoggingCallback(NilsPodLoggingCallback callback) {
        mCallbacks.add(callback);
    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (NILS_POD_STREAMING.equals(characteristic.getUuid())) {
                switch (getOperationState()) {
                    case READING_SESSION_LIST:
                        extractSessionListData(characteristic);
                        return true;
                    case DOWNLOADING_SESSION:
                        extractSessionData(characteristic);
                        return true;
                }
            }
        }
        return false;
    }

    protected void extractSessionListData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        if (!mSessionHandler.firstPacketRead()) {
            mSessionHandler.setSessionCount(values[0]);
        } else {
            Session session = new Session(values);
            mSessionHandler.addSession(session);
            Log.d(TAG, session.toDebugString());
        }

        if (mSessionHandler.allSessionsRead()) {
            Log.d(TAG, "All Sessions read!");
            if (mCallbacks != null) {
                for (NilsPodLoggingCallback callback : mCallbacks) {
                    callback.onSessionListRead(this, mSessionHandler.getSessionList());
                }
            }
        }
    }

    protected void extractSessionData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        mSessionDownloader.onNewData(values);
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onSessionDownloadProgress(this, mSessionDownloader);
        }
    }

    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    @Override
    protected void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        // one data packet always has size mPacketSize
        if (values.length % mPacketSize != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mPacketSize) {
            int offset = i;
            double[] gyro = new double[3];
            double[] accel = new double[3];
            double baro = 0;
            int localCounter;

            // extract gyroscope data
            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
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
            }

            // extract packet counter (16 bit)
            localCounter = (values[i + mPacketSize - 1] & 0xFF) | ((values[i + mPacketSize - 2] & 0xFF) << 8);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 15)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }

            NilsPodDataFrame df = new NilsPodDataFrame(this, globalCounter * (2 << 15) + localCounter, accel, gyro, baro);

            // send new data to the SensorDataProcessor
            sendNewData(df);
            lastCounter = localCounter;
            if (mRecordingEnabled) {
                mDataRecorder.writeData(df);
            }
        }
    }

    @Override
    protected void onAllGattNotificationsEnabled() {
        super.onAllGattNotificationsEnabled();
    }

    @Override
    protected void onOperationStateChanged(NilsPodOperationState oldState, NilsPodOperationState newState) throws SensorException {
        super.onOperationStateChanged(oldState, newState);
        switch (newState) {
            case IDLE:
                switch (oldState) {
                    case LOGGING:
                        //readSessionList();
                        setState(SensorState.CONNECTED);
                        for (NilsPodLoggingCallback callback : mCallbacks) {
                            callback.onStopLogging(this);
                        }
                        break;
                    case FLASH_ERASE:
                        //readSessionList();
                        for (NilsPodLoggingCallback callback : mCallbacks) {
                            callback.onClearSessions(this);
                        }
                        break;
                    case DOWNLOADING_SESSION:
                        mSessionDownloader.completeDownload();
                        for (NilsPodLoggingCallback callback : mCallbacks) {
                            callback.onSessionDownloaded(this, mSessionDownloader.getSession());
                        }
                        break;
                    case SAVING_CONFIG:
                        // remove last written item
                        mConfigWriteRequests.poll();
                        if (!mConfigWriteRequests.isEmpty()) {
                            BluetoothGattCharacteristic chara = mConfigWriteRequests.peek();
                            if (!writeCharacteristic(chara, chara.getValue())) {
                                throw new SensorException(SensorException.SensorExceptionType.configError);
                            }
                        }
                        break;
                }
                break;
            case LOGGING:
                switch (oldState) {
                    case IDLE:
                        if (isConnected()) {
                            setState(SensorState.LOGGING);
                        }
                        for (NilsPodLoggingCallback callback : mCallbacks) {
                            callback.onStartLogging(this);
                        }
                        break;
                }
                break;
            case DOWNLOADING_SESSION:
                switch (oldState) {
                    case IDLE:
                        for (NilsPodLoggingCallback callback : mCallbacks) {
                            callback.onSessionDownloadStarted(this, mSessionDownloader.getSession());
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public void readSessionList() {
        // clear session list (if there was some before)
        mSessionHandler = new SessionHandler();
        send(NilsPodSensorCommand.FLASH_READ_SESSION_LIST);
    }

    @Override
    public void startLogging() {
        send(NilsPodSensorCommand.START_LOGGING);
    }

    @Override
    public void stopLogging() {
        send(NilsPodSensorCommand.STOP_LOGGING);
    }

    @Override
    public void downloadSession(Session session) throws SensorException {
        mSessionDownloader = new SessionDownloader(this, session);
        mSessionDownloader.enableCsvExport();
        int sessionId = session.getSessionNumber();
        byte[] cmd = NilsPodSensorCommand.FLASH_TRANSMIT_SESSION.getByteCmd();
        cmd[1] = (byte) sessionId;
        send(cmd);
    }

    @Override
    public void setConfigMap(HashMap<String, Object> configMap) {
        double samplingRate = 0.0;
        ArrayList<HardwareSensor> sensors = (ArrayList<HardwareSensor>) mCurrentConfigMap.get(KEY_HARDWARE_SENSORS);
        NilsPodSensorPosition sensorPosition = (NilsPodSensorPosition) mCurrentConfigMap.get(KEY_SENSOR_POSITION);
        NilsPodSyncGroup syncGroup = (NilsPodSyncGroup) mCurrentConfigMap.get(KEY_SYNC_GROUP);
        NilsPodSyncRole syncRole = (NilsPodSyncRole) mCurrentConfigMap.get(KEY_SYNC_ROLE);
        String syncDistance = (String) mCurrentConfigMap.get(KEY_SYNC_DISTANCE);
        NilsPodSpecialFunction specialFunction = (NilsPodSpecialFunction) mCurrentConfigMap.get(KEY_SPECIAL_FUNCTION);
        NilsPodMotionInterrupt interrupt = (NilsPodMotionInterrupt) mCurrentConfigMap.get(KEY_MOTION_INTERRUPT);

        for (String key : configMap.keySet()) {
            Log.d(TAG, "config map: " + key + ", " + configMap.get(key) + ", " + configMap.get(key).getClass());
            switch (key) {
                case KEY_SAMPLING_RATE:
                    String sr = (String) configMap.get(key);
                    samplingRate = sAvailableSamplingRates.get(sr);
                    break;
                case KEY_HARDWARE_SENSORS:
                    sensors = (ArrayList<HardwareSensor>) configMap.get(key);
                    break;
                case KEY_SENSOR_POSITION:
                    sensorPosition = (NilsPodSensorPosition) configMap.get(key);
                    break;
                case KEY_SYNC_GROUP:
                    syncGroup = (NilsPodSyncGroup) configMap.get(key);
                    break;
                case KEY_SYNC_ROLE:
                    syncRole = (NilsPodSyncRole) configMap.get(key);
                    break;
                case KEY_SYNC_DISTANCE:
                    syncDistance = (String) configMap.get(key);
                    break;
                case KEY_SPECIAL_FUNCTION:
                    specialFunction = (NilsPodSpecialFunction) configMap.get(key);
                    break;
                case KEY_MOTION_INTERRUPT:
                    interrupt = (NilsPodMotionInterrupt) configMap.get(key);
                    break;
            }
        }

        try {
            writeTsConfig(samplingRate, syncRole, syncGroup, syncDistance);
            writeSensorConfig(sensors);
            writeSystemSettingsConfig(sensorPosition, specialFunction, interrupt);
        } catch (SensorException e) {
            e.printStackTrace();
        }
    }

    @Override
    public HashMap<String, ConfigItem> getConfigMap() {
        return mConfigMap;
    }

    @Override
    public HashMap<String, Object> getCurrentConfigMap() {
        return mCurrentConfigMap;
    }


    private void createDummySessions() {
        send(new byte[]{(byte) 0xAB});
    }


    protected void writeNilsPodConfig(BluetoothGattCharacteristic configChara, byte[] oldValue, byte[] value) throws SensorException {
        if (Arrays.equals(oldValue, value)) {
            return;
        }

        configChara.setValue(value);
        configChara.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mConfigWriteRequests.add(configChara);

        if (mConfigWriteRequests.size() == 1) {
            BluetoothGattCharacteristic chara = mConfigWriteRequests.peek();
            if (!writeCharacteristic(chara, chara.getValue())) {
                throw new SensorException(SensorException.SensorExceptionType.configError);
            }
        }
    }


    protected void writeTsConfig(double samplingRate, NilsPodSyncRole syncRole, NilsPodSyncGroup syncGroup, String syncDistance) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_TS_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int command = -1;

        for (int i = 0; i < mSamplingRateCommands.size(); i++) {
            if (mSamplingRateCommands.valueAt(i) == samplingRate) {
                command = mSamplingRateCommands.keyAt(i);
                break;
            }
        }

        int syncDistanceCommand = 0;
        for (Map.Entry<Integer, String> entry : sSyncDistanceCommands.entrySet()) {
            if (entry.getValue().equals(syncDistance)) {
                syncDistanceCommand = entry.getKey();
                break;
            }
        }

        if (command == -1) {
            throw new SensorException(SensorException.SensorExceptionType.configError);
        }

        int offset = 0;
        value[offset++] = (byte) command;
        value[offset++] = (byte) syncRole.ordinal();
        value[offset++] = (byte) syncDistanceCommand;
        value[offset] = (byte) syncGroup.ordinal();

        writeNilsPodConfig(config, oldValue, value);
    }

    protected void writeSensorConfig(ArrayList<HardwareSensor> sensors) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SENSOR_CONFIG);
        byte[] oldValue = config.getValue();

        byte[] value = new byte[1];
        int sensorField = 0;
        for (HardwareSensor sensor : sensors) {
            switch (sensor) {
                case GYROSCOPE:
                    sensorField = (sensorField | (0x01));
                    break;
                case BAROMETER:
                    sensorField = (sensorField | (0x04));
                    break;
            }
        }

        value[0] = (byte) sensorField;

        writeNilsPodConfig(config, new byte[]{oldValue[0]}, value);
    }

    protected void writeSystemSettingsConfig(NilsPodSensorPosition sensorPosition, NilsPodSpecialFunction specialFunction, NilsPodMotionInterrupt motionInterrupt) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SYSTEM_SETTINGS_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int offset = 0;

        value[offset++] = (byte) sensorPosition.ordinal();
        value[offset++] = (byte) specialFunction.ordinal();
        offset += 2;
        value[offset] = (byte) motionInterrupt.ordinal();

        writeNilsPodConfig(config, oldValue, value);
    }


    /**
     * Updates the current date time stamp of the external temperature compensated RTC module.
     */
    public void updateRtc() {
        Date currentTime = Calendar.getInstance().getTime();

        Log.d(TAG, "Updating RTC time on " + getDeviceName() + " - new time: " + currentTime);

        long unixTime = currentTime.getTime() / 1000; //convert from milliseconds to seconds
        byte[] data = new byte[4];
        data[0] = (byte) ((unixTime >> 24) & 0xFF);
        data[1] = (byte) ((unixTime >> 16) & 0xFF);
        data[2] = (byte) ((unixTime >> 8) & 0xFF);
        data[3] = (byte) ((unixTime) & 0xFF);

        BluetoothGattCharacteristic dateTimeCharacteristic = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_DATE_TIME_CONFIG);
        writeCharacteristic(dateTimeCharacteristic, data);
    }

    @Override
    public void clearData() {
        send(NilsPodSensorCommand.FLASH_CLEAR_SESSIONS);
    }

    @Override
    public void fullErase() {
        send(NilsPodSensorCommand.FLASH_FULL_ERASE);
    }

    @Override
    public void setDefaultConfig() {
        send(NilsPodSensorCommand.SET_DEFAULT_CONFIG);
    }


    public static class NilsPodDataFrame extends GenericNilsPodDataFrame implements BarometricPressureDataFrame {

        protected double baro;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro) {
            this(sensor, timestamp, accel, gyro, 0);
        }


        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro) {
            super(sensor, timestamp, accel, gyro);
            this.baro = baro;
        }

        @Override
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public String toString() {
            return super.toString() + ", baro: " + baro;
        }
    }
}
