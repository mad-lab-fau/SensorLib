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
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import de.fau.sensorlib.HwSensorNotAvailableException;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AnalogDataFrame;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.MagnetometerDataFrame;
import de.fau.sensorlib.dataframe.PpgDataFrame;
import de.fau.sensorlib.dataframe.TemperatureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.enums.NilsPodAccRange;
import de.fau.sensorlib.sensors.enums.NilsPodAlarmMode;
import de.fau.sensorlib.sensors.enums.NilsPodGyroRange;
import de.fau.sensorlib.sensors.enums.NilsPodIndicationLed;
import de.fau.sensorlib.sensors.enums.NilsPodMotionInterrupt;
import de.fau.sensorlib.sensors.enums.NilsPodOperationMode;
import de.fau.sensorlib.sensors.enums.NilsPodSensorPosition;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.logging.NilsPodLoggable;
import de.fau.sensorlib.sensors.logging.Session;
import de.fau.sensorlib.sensors.logging.SessionDownloader;
import de.fau.sensorlib.sensors.logging.SessionHandler;
import de.fau.sensorlib.widgets.config.ConfigItem;


/**
 * Represents a NilsPod Sensor device.
 */
public class NilsPodSensor extends AbstractNilsPodSensor implements NilsPodLoggable, Configurable {

    protected static class NilsPodInternalHandler extends BasicNilsPodInternalHandler {

        public NilsPodInternalHandler(NilsPodSensor sensor) {
            super(sensor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (getSensor() instanceof NilsPodSensor) {
                NilsPodSensor sensor = (NilsPodSensor) getSensor();
                switch (msg.what) {
                    case MESSAGE_SESSION_LIST_READ:
                        sensor.dispatchSessionListRead((List<Session>) msg.obj);
                        break;
                    case MESSAGE_SESSIONS_CLEARED:
                        sensor.dispatchSessionsCleared();
                        break;
                    case MESSAGE_SESSION_DOWNLOAD_STARTED:
                        sensor.dispatchSessionDownloadStarted((SessionDownloader) msg.obj);
                        break;
                    case MESSAGE_SESSION_DOWNLOAD_PROGRESS:
                        sensor.dispatchSessionDownloadProgress((SessionDownloader) msg.obj);
                        break;
                    case MESSAGE_SESSION_DOWNLOAD_FINISHED:
                        sensor.dispatchSessionDownloadFinished((SessionDownloader) msg.obj);
                        break;
                    case MESSAGE_SENSOR_CONFIG_CHANGED:
                        sensor.dispatchSensorConfigChanged();
                        break;
                }
            }
        }
    }


    private static final String TAG = NilsPodSensor.class.getSimpleName();

    /**
     * Global counter for incoming packages (local counter only has 15 bit)
     */
    protected int globalCounter = 0;

    private double mTotalFlashSize = 0;
    private double mRemainingFlashSize = 0;
    private double mRemainingCapacity = 0;
    private String mRemainingRuntime = "n/a";


    private SessionHandler mSessionHandler;
    private SessionDownloader mSessionDownloader;
    private boolean mCsvExportEnabled = false;

    private ConcurrentLinkedQueue<BluetoothGattCharacteristic> mConfigWriteRequests = new ConcurrentLinkedQueue<>();


    public NilsPodSensor(Context context, SensorInfo info, SensorDataProcessor dataHandler) {
        super(context, info, dataHandler);
        mConfigWriteRequests.clear();
        mInternalHandler = new NilsPodInternalHandler(this);
    }

    @Override
    public void startStreaming() {
        super.startStreaming();
        lastCounter = 0;
        globalCounter = 0;
    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        if (super.onNewCharacteristicValue(characteristic, isChange)) {
            return true;
        } else {
            if (NILS_POD_STREAMING.equals(characteristic.getUuid())) {
                switch (getOperationState()) {
                    case SESSION_LIST:
                        extractSessionListData(characteristic);
                        return true;
                    case SESSION_DOWNLOAD:
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
            Session session = new Session(characteristic);
            mSessionHandler.addSession(session);
            Log.d(TAG, session.toDebugString());
        }

        if (mSessionHandler.allSessionsRead()) {
            Log.d(TAG, "All Sessions read!");
            computeRemainingCapacity();
            computeRemainingRuntime();

            sendSessionListRead(mSessionHandler.getSessionList());
        }
    }

    protected void extractSessionData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();
        mSessionDownloader.onNewData(values);
        sendSessionDownloadProgress(mSessionDownloader);
    }

    /**
     * Extracts sensor data into data frames from the given characteristic.
     *
     * @param characteristic Received characteristic from the BLE API
     */
    @Override
    protected void extractSensorData(BluetoothGattCharacteristic characteristic) {
        byte[] values = characteristic.getValue();

        // one data packet always has size mSampleSize
        if (values.length % mSampleSize != 0) {
            Log.e(TAG, "Wrong BLE Packet Size!");
            return;
        }

        // iterate over data packets
        for (int i = 0; i < values.length; i += mSampleSize) {
            int offset = i;
            double[] gyro = null;
            double[] accel = null;
            double[] mag = null;
            double[] analog = null;
            double baro = Double.MIN_VALUE;
            double temp = Double.MIN_VALUE;
            double ecg = Double.MIN_VALUE;
            double ppg = Double.MIN_VALUE;
            int localCounter;

            // extract gyroscope data
            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
                gyro = new double[3];
                for (int j = 0; j < 3; j++) {
                    gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset) / getGyroScalingFactor();
                    offset += 2;
                }
            }

            // extract accelerometer data
            if (isSensorEnabled(HardwareSensor.ACCELEROMETER)) {
                accel = new double[3];
                for (int j = 0; j < 3; j++) {
                    accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset) / getAccScalingFactor();
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
                    analog[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++);
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

            // extract packet counter (16 bit)
            localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + mSampleSize - 2);

            // check if packets have been lost
            if (((localCounter - lastCounter) % (2 << 15)) > 1) {
                Log.w(TAG, this + ": BLE Packet Loss!");
            }
            // increment global counter if local counter overflows
            if (localCounter < lastCounter) {
                globalCounter++;
            }


            long timestamp = globalCounter * (2 << 15) + localCounter;
            NilsPodDataFrame df;
            if (isSensorEnabled(HardwareSensor.ANALOG)) {
                df = new NilsPodAnalogDataFrame(this, timestamp, accel, gyro, baro, temp, mag, analog);
            } else if (isSensorEnabled(HardwareSensor.ECG)) {
                df = new NilsPodEcgDataFrame(this, timestamp, accel, gyro, baro, temp, mag, ecg);
            } else if (isSensorEnabled(HardwareSensor.PPG)) {
                df = new NilsPodPpgDataFrame(this, timestamp, accel, gyro, baro, temp, mag, ppg);
            } else if (isSensorEnabled(HardwareSensor.MAGNETOMETER)) {
                df = new NilsPodMagDataFrame(this, timestamp, accel, gyro, baro, temp, mag);
            } else if (isSensorEnabled(HardwareSensor.TEMPERATURE)) {
                df = new NilsPodTempDataFrame(this, timestamp, accel, gyro, baro, temp);
            } else {
                df = new NilsPodDataFrame(this, timestamp, accel, gyro, baro);
            }

            // send new data to the SensorDataProcessor
            sendNewData(df);

            //Log.d(TAG, df.toString());

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
    protected void onStateChange(SensorState oldState, SensorState newState) {
        super.onStateChange(oldState, newState);
        if (newState == SensorState.CONNECTED) {
            if (getOperationState() == NilsPodOperationState.LOGGING) {
                // operation state is read before sensor state is connected => check again
                // (and notify listeners) when sensor is finally connected
                sendOperationStateChanged(NilsPodOperationState.LOGGING);
                sendStartLogging();
            }
        }
    }

    @Override
    protected void onOperationStateChanged(NilsPodOperationState oldState, NilsPodOperationState newState) throws SensorException {
        super.onOperationStateChanged(oldState, newState);
        switch (newState) {
            case IDLE:
                if (mModelNumberString.length() > 0) {
                    // extract flash size from model number
                    int flashType = mModelNumberString.charAt(mModelNumberString.length() - 1) == '4' ? 4 : 2;
                    // convert from Gigabit to Byte
                    mTotalFlashSize = ((double) flashType * Math.pow(1024, 3)) / 8;
                    computeRemainingCapacity();
                    computeRemainingRuntime();
                }

                switch (oldState) {
                    case LOGGING:
                        setState(SensorState.CONNECTED);
                        sendStopLogging();
                        break;
                    case FLASH_ERASE:
                        sendSessionsCleared();
                        break;
                    case SESSION_DOWNLOAD:
                        mSessionDownloader.completeDownload();
                        sendSessionDownloadFinished(mSessionDownloader);
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
                        sendSensorConfigChanged();
                        break;
                }
                break;
            case LOGGING:
                switch (oldState) {
                    case IDLE:
                        if (isConnected()) {
                            sendStartLogging();
                        }
                        break;
                }
                break;
            case SESSION_DOWNLOAD:
                switch (oldState) {
                    case IDLE:
                        sendSessionDownloadStarted(mSessionDownloader);
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
    public void downloadSession(int sessionId) throws SensorException {
        mSessionDownloader = new SessionDownloader(this, mSessionHandler.getSessionById(sessionId));
        mSessionDownloader.setCsvExportEnabled(mCsvExportEnabled);

        byte[] cmd = NilsPodSensorCommand.FLASH_TRANSMIT_SESSION.getByteCmd();
        cmd[1] = (byte) sessionId;
        send(cmd);
    }

    @Override
    public void setCurrentConfig(HashMap<String, Object> configMap) {
        for (String key : configMap.keySet()) {
            Log.d(TAG, "config map: " + key + ", " + configMap.get(key) + ", " + configMap.get(key).getClass());
            mCurrentConfigMap.put(key, configMap.get(key));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeConfig() {
        double samplingRate = 0.0;
        ArrayList<HardwareSensor> sensors = (ArrayList<HardwareSensor>) mCurrentConfigMap.get(KEY_HARDWARE_SENSORS);
        NilsPodAccRange accRange = (NilsPodAccRange) mCurrentConfigMap.get(KEY_ACC_RANGE);
        NilsPodGyroRange gyroRange = (NilsPodGyroRange) mCurrentConfigMap.get(KEY_GYRO_RANGE);
        NilsPodSensorPosition sensorPosition = (NilsPodSensorPosition) mCurrentConfigMap.get(KEY_SENSOR_POSITION);
        NilsPodSyncGroup syncGroup = (NilsPodSyncGroup) mCurrentConfigMap.get(KEY_SYNC_GROUP);
        NilsPodSyncRole syncRole = (NilsPodSyncRole) mCurrentConfigMap.get(KEY_SYNC_ROLE);
        NilsPodOperationMode operationMode = (NilsPodOperationMode) mCurrentConfigMap.get(KEY_OPERATION_MODE);
        NilsPodMotionInterrupt interrupt = (NilsPodMotionInterrupt) mCurrentConfigMap.get(KEY_MOTION_INTERRUPT);
        NilsPodIndicationLed indicationLed = (NilsPodIndicationLed) mCurrentConfigMap.get(KEY_INDICATION_LED);

        boolean alarmEnabled = mCurrentConfigMap.get(KEY_ALARM_ENABLED) == NilsPodAlarmMode.ALARM_ENABLED;
        Calendar alarmStart = (Calendar) mCurrentConfigMap.get(KEY_ALARM_START_TIME);
        Calendar alarmStop = (Calendar) mCurrentConfigMap.get(KEY_ALARM_STOP_TIME);
        NilsPodAlarm alarmConfig = new NilsPodAlarm(alarmStart, alarmStop, alarmEnabled);

        String sr = (String) mCurrentConfigMap.get(KEY_SAMPLING_RATE);
        if (sr != null) {
            samplingRate = sAvailableSamplingRates.get(sr);
        }

        try {
            writeSamplingRateConfig(samplingRate);
            writeSyncConfig(syncRole, syncGroup);
            writeSensorConfig(sensors, accRange, gyroRange);
            writeSystemSettingsConfig(sensorPosition, operationMode, interrupt, indicationLed);
            writeAlarmConfig(alarmConfig);
        } catch (SensorException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public HashMap<String, ConfigItem> getConfigItemMap() {
        return (HashMap<String, ConfigItem>) sConfigMap.clone();
    }

    @Override
    public HashMap<String, Object> getCurrentConfig() {
        return mCurrentConfigMap;
    }

    private void sendSessionListRead(List<Session> sessionList) {
        mInternalHandler.obtainMessage(MESSAGE_SESSION_LIST_READ, sessionList).sendToTarget();
    }


    private void dispatchSessionListRead(List<Session> sessionList) {
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onSessionListRead(this, sessionList);
        }
    }

    private void sendSessionsCleared() {
        mInternalHandler.obtainMessage(MESSAGE_SESSIONS_CLEARED).sendToTarget();
    }


    private void dispatchSessionsCleared() {
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onClearSessions(this);
        }
    }

    private void sendSessionDownloadStarted(SessionDownloader sessionDownloader) {
        mInternalHandler.obtainMessage(MESSAGE_SESSION_DOWNLOAD_STARTED, sessionDownloader).sendToTarget();
    }

    private void dispatchSessionDownloadStarted(SessionDownloader sessionDownloader) {
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onSessionDownloadStarted(this, sessionDownloader);
        }
    }

    private void sendSessionDownloadProgress(SessionDownloader sessionDownloader) {
        mInternalHandler.obtainMessage(MESSAGE_SESSION_DOWNLOAD_PROGRESS, sessionDownloader).sendToTarget();
    }

    private void dispatchSessionDownloadProgress(SessionDownloader sessionDownloader) {
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onSessionDownloadProgress(this, sessionDownloader);
        }
    }

    private void sendSessionDownloadFinished(SessionDownloader sessionDownloader) {
        mInternalHandler.obtainMessage(MESSAGE_SESSION_DOWNLOAD_FINISHED, sessionDownloader).sendToTarget();
    }

    private void dispatchSessionDownloadFinished(SessionDownloader sessionDownloader) {
        for (NilsPodLoggingCallback callback : mCallbacks) {
            callback.onSessionDownloadFinished(this, sessionDownloader);
        }
    }

    private void sendSensorConfigChanged() {
        mInternalHandler.obtainMessage(MESSAGE_SENSOR_CONFIG_CHANGED).sendToTarget();
    }

    private void dispatchSensorConfigChanged() {
        for (NilsPodConfigCallback callback : mCallbacks) {
            callback.onSensorConfigChanged(this);
        }
    }


    /**
     * Returns the remaining storage capacity.
     *
     * @return Remaining storage capacity in %
     */
    public double getRemainingCapacity() {
        return mRemainingCapacity;
    }

    private void computeRemainingCapacity() {
        mRemainingFlashSize = mTotalFlashSize;

        int occupiedStorage = 0;

        if (mSessionHandler == null) {
            return;
        }

        for (Session session : mSessionHandler.getSessionList()) {
            // round up to integer page sizes because new sessions are always started on a new page
            occupiedStorage += (Math.ceil(((double) session.getSessionSize()) / Session.PAGE_SIZE)) * Session.PAGE_SIZE;
        }

        mRemainingFlashSize -= occupiedStorage;

        if (mTotalFlashSize != 0) {
            mRemainingCapacity = (mRemainingFlashSize / mTotalFlashSize) * 100.0;
        } else {
            mRemainingCapacity = 100.0;
        }
    }

    /**
     * Returns the (estimated) remaining logging runtime.
     *
     * @return Remaining runtime in format hh:mm
     */
    public String getRemainingRuntime() {
        return mRemainingRuntime;
    }

    private void computeRemainingRuntime() {
        long runtimeSeconds = (long) ((mRemainingFlashSize / mSampleSize) / getSamplingRate());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Duration duration = Duration.ofSeconds(runtimeSeconds);
            long days = duration.toDays();
            long hours = duration.minusDays(days).toHours();
            long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
            mRemainingRuntime = days + "d : " + hours + "h : " + minutes + "min";
        } else {
            long hours = TimeUnit.SECONDS.toHours(runtimeSeconds);
            long minutes = TimeUnit.SECONDS.toMinutes(runtimeSeconds) - TimeUnit.HOURS.toMinutes(hours);

            mRemainingRuntime = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
        }
    }

    @Override
    public void setCsvExportEnabled(boolean enabled) {
        mCsvExportEnabled = enabled;
        if (mSessionDownloader != null) {
            mSessionDownloader.setCsvExportEnabled(enabled);
        }
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
            if (chara != null && !writeCharacteristic(chara, chara.getValue())) {
                throw new SensorException(SensorException.SensorExceptionType.configError);
            }
        }
    }

    protected void writeSamplingRateConfig(double samplingRate) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(NILS_POD_SAMPLING_RATE_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int command = -1;

        for (int i = 0; i < sSamplingRateCommands.size(); i++) {
            if (sSamplingRateCommands.valueAt(i) == samplingRate) {
                command = sSamplingRateCommands.keyAt(i);
                break;
            }
        }

        if (command == -1) {
            throw new SensorException(SensorException.SensorExceptionType.configError);
        }

        value[0] = (byte) command;
        writeNilsPodConfig(config, oldValue, value);
    }


    protected void writeSyncConfig(NilsPodSyncRole syncRole, NilsPodSyncGroup syncGroup) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(NILS_POD_SYNC_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int offset = 0;
        if (syncRole != null) {
            value[offset++] = (byte) syncRole.ordinal();
        }
        if (syncGroup != null) {
            value[offset++] = (byte) syncGroup.getSyncChannel();
            System.arraycopy(syncGroup.getSyncAddress(), 0, value, offset, 5);
        }

        writeNilsPodConfig(config, oldValue, value);
    }

    protected void writeSensorConfig(ArrayList<HardwareSensor> sensors, NilsPodAccRange accRange, NilsPodGyroRange gyroRange) throws SensorException {
        if (sensors == null) {
            return;
        }

        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(NILS_POD_SENSOR_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = new byte[oldValue.length - 1];
        // Read-modify-write
        System.arraycopy(oldValue, 0, value, 0, value.length);

        int sensorField = 0;
        for (HardwareSensor sensor : sensors) {
            switch (sensor) {
                case ACCELEROMETER:
                    sensorField = (sensorField | (0x0001));
                    break;
                case GYROSCOPE:
                    sensorField = (sensorField | (0x0002));
                    break;
                case MAGNETOMETER:
                    sensorField = (sensorField | (0x0004));
                    break;
                case BAROMETER:
                    sensorField = (sensorField | (0x0008));
                    break;
                case ANALOG:
                    sensorField = (sensorField | (0x0010));
                    break;
                case ECG:
                    sensorField = (sensorField | (0x0020));
                    break;
                case PPG:
                    sensorField = (sensorField | (0x0040));
                    break;
                case TEMPERATURE:
                    sensorField = (sensorField | (0x0080));
                    break;
            }
        }

        value[0] = (byte) (sensorField & 0xFF);
        value[1] = (byte) ((sensorField >> 8) & 0xFF);

        if (accRange != null || gyroRange != null) {
            value[2] = 0;
            if (accRange != null) {
                value[2] |= (byte) accRange.getRangeVal() & 0xFF;
            }

            if (gyroRange != null) {
                value[2] |= (byte) gyroRange.getRangeVal() & 0xFF;
            }
        }

        if (getFirmwareRevision().isAtLeast(NilsPodFirmwareRevisions.FW_0_16_0)) {
            writeNilsPodConfig(config, new byte[]{oldValue[0], oldValue[1], oldValue[2]}, value);
        } else {
            writeNilsPodConfig(config, new byte[]{oldValue[0], oldValue[1]}, value);
        }
    }

    protected void writeSystemSettingsConfig(NilsPodSensorPosition sensorPosition, NilsPodOperationMode operationMode, NilsPodMotionInterrupt motionInterrupt, NilsPodIndicationLed indicationLed) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(NILS_POD_SYSTEM_SETTINGS_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int offset = 0;

        if (sensorPosition != null) {
            value[offset++] = (byte) sensorPosition.ordinal();
        }

        int byteVal = 0;
        if (operationMode == NilsPodOperationMode.HOME_MONITORING_MODE) {
            byteVal |= 0x40;
        }
        if (motionInterrupt == NilsPodMotionInterrupt.MOTION_INTERRUPT_ENABLED) {
            byteVal |= 0x80;
        }
        if (indicationLed == null || indicationLed == NilsPodIndicationLed.INDICATION_LED_ENABLED) {
            byteVal |= 0x01;
        }

        value[offset] = (byte) byteVal;

        writeNilsPodConfig(config, oldValue, value);
    }

    protected void writeAlarmConfig(NilsPodAlarm alarmConfig) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(NILS_POD_SYSTEM_SETTINGS_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();
        int offset = 0;

        value[offset++] = (byte) (alarmConfig.isAlarmEnabled() ? 0x01 : 0x00);
        value[offset++] = (byte) alarmConfig.getStartHour();
        value[offset++] = (byte) alarmConfig.getStartMinute();
        value[offset++] = (byte) alarmConfig.getStopHour();
        value[offset] = (byte) alarmConfig.getStopMinute();

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

        private double baro;
        private boolean hasBaro;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro) {
            super(sensor, timestamp, accel, gyro);
            hasBaro = false;
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
            this(sensor, timestamp, accel, gyro);
            if (baro != Double.MIN_VALUE) {
                this.baro = baro;
                hasBaro = true;
            }
        }

        @Override
        public double getBarometricPressure() {
            if (hasBaro) {
                return baro;
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.BAROMETER);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasBaro) {
                str += ", baro: " + getBarometricPressure() + " mBar";
            }
            return str;
        }
    }

    public static class NilsPodTempDataFrame extends NilsPodDataFrame implements TemperatureDataFrame {

        private double temp;
        private boolean hasTemp;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodTempDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double temp) {
            this(sensor, timestamp, accel, gyro, baro);
            if (temp != Double.MIN_VALUE) {
                this.temp = temp;
                hasTemp = true;
            }
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         */
        public NilsPodTempDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro) {
            super(sensor, timestamp, accel, gyro, baro);
            hasTemp = false;
        }

        @Override
        public double getTemperature() {
            if (hasTemp) {
                return temp;
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.TEMPERATURE);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasTemp) {
                str += ", temp: " + new DecimalFormat("#.00").format(getTemperature()) + " °C";
            }
            return str;
        }
    }


    public static class NilsPodMagDataFrame extends NilsPodTempDataFrame implements MagnetometerDataFrame {

        private double[] mag;
        private boolean hasMag;


        public NilsPodMagDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double temp, double[] mag) {
            super(sensor, timestamp, accel, gyro, baro, temp);

            if (mag != null) {
                if (mag.length != 3) {
                    throw new IllegalArgumentException("Illegal array size for magnetometer values!");
                }
                this.mag = mag;
                hasMag = true;
            }
        }

        public NilsPodMagDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double[] mag) {
            this(sensor, timestamp, accel, gyro, Double.MIN_VALUE, Double.MIN_VALUE, mag);
        }

        @Override
        public double getMagX() {
            if (hasMag) {
                return mag[0];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.MAGNETOMETER);
            }
        }

        @Override
        public double getMagY() {
            if (hasMag) {
                return mag[1];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.MAGNETOMETER);
            }
        }

        @Override
        public double getMagZ() {
            if (hasMag) {
                return mag[2];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.MAGNETOMETER);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasMag) {
                str += ", mag: " + Arrays.toString(mag);
            }

            return str;
        }
    }


    public static class NilsPodAnalogDataFrame extends NilsPodMagDataFrame implements AnalogDataFrame {

        private double[] analog;
        private boolean hasAnalog;

        public NilsPodAnalogDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double temp, double[] mag, double[] analog) {
            super(sensor, timestamp, accel, gyro, baro, temp, mag);

            if (analog != null) {
                if (analog.length != 3) {
                    throw new IllegalArgumentException("Illegal array size for analog channel values!");
                }
                this.analog = analog;
                hasAnalog = true;
            }
        }

        public NilsPodAnalogDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] analog) {
            this(sensor, timestamp, accel, gyro, baro, Double.MIN_VALUE, null, analog);
        }

        public NilsPodAnalogDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double[] mag, double[] analog) {
            this(sensor, timestamp, accel, gyro, baro, Double.MIN_VALUE, mag, analog);
        }

        public NilsPodAnalogDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double[] analog) {
            this(sensor, timestamp, accel, gyro, Double.MIN_VALUE, Double.MIN_VALUE, null, analog);
        }

        @Override
        public double getFirstAnalogSample() {
            if (hasAnalog) {
                return analog[0];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.ANALOG);
            }
        }

        @Override
        public double getSecondAnalogSample() {
            if (hasAnalog) {
                return analog[1];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.ANALOG);
            }
        }

        @Override
        public double getThirdAnalogSample() {
            if (hasAnalog) {
                return analog[2];
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.ANALOG);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasAnalog) {
                str += ", analog: " + Arrays.toString(analog);
            }

            return str;
        }
    }


    /**
     * Data frame to store data received from the NilsPod Sensor
     */
    public static class NilsPodEcgDataFrame extends NilsPodMagDataFrame implements EcgDataFrame {

        private double ecg;
        private boolean hasEcg;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         * @param baro      barometer value
         * @param temp      temperature value
         * @param mag       array storing magnetometer values
         * @param ecg       ECG value
         */
        public NilsPodEcgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double temp, double[] mag, double ecg) {
            super(sensor, timestamp, accel, gyro, baro, temp, mag);
            if (ecg != Double.MIN_VALUE) {
                this.ecg = ecg;
                hasEcg = true;
            }
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         * @param baro      barometer value
         * @param ecg       ECG value
         */
        public NilsPodEcgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double ecg) {
            this(sensor, timestamp, accel, gyro, baro, Double.MIN_VALUE, null, ecg);
        }

        @Override
        public double getEcgSample() {
            if (hasEcg) {
                return ecg;
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.ECG);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasEcg) {
                str += ", ecg: " + ecg;
            }

            return str;
        }
    }


    /**
     * Data frame to store data received from the NilsPod Sensor
     */
    public static class NilsPodPpgDataFrame extends NilsPodMagDataFrame implements PpgDataFrame {

        private double ppg;
        private boolean hasPpg;

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param gyro      array storing gyroscope values
         * @param baro      barometer value
         * @param temp      temperature value
         * @param mag       array storing magnetometer values
         * @param ppg       PPG value
         */
        public NilsPodPpgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double temp, double[] mag, double ppg) {
            super(sensor, timestamp, accel, gyro, baro, temp, mag);
            if (ppg != Double.MIN_VALUE) {
                this.ppg = ppg;
                hasPpg = true;
            }
        }

        /**
         * Creates a new data frame for sensor data
         *
         * @param sensor    Originating sensor
         * @param timestamp Incremental counter for each data frame
         * @param accel     array storing acceleration values
         * @param ppg       PPG value
         */
        public NilsPodPpgDataFrame(AbstractSensor sensor, long timestamp, double[] accel, double[] gyro, double baro, double ppg) {
            this(sensor, timestamp, accel, gyro, baro, Double.MIN_VALUE, null, ppg);
        }

        @Override
        public double getPpgSample() {
            if (hasPpg) {
                return ppg;
            } else {
                throw new HwSensorNotAvailableException(HardwareSensor.PPG);
            }
        }

        @NonNull
        @Override
        public String toString() {
            String str = super.toString();

            if (hasPpg) {
                str += ", ppg: " + ppg;
            }

            return str;
        }
    }
}
