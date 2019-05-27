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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.fau.sensorlib.HwSensorNotAvailableException;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AnalogDataFrame;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.MagnetometerDataFrame;
import de.fau.sensorlib.dataframe.TemperatureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;
import de.fau.sensorlib.sensors.enums.NilsPodMotionInterrupt;
import de.fau.sensorlib.sensors.enums.NilsPodOperationMode;
import de.fau.sensorlib.sensors.enums.NilsPodSensorPosition;
import de.fau.sensorlib.sensors.enums.NilsPodSyncGroup;
import de.fau.sensorlib.sensors.enums.NilsPodSyncRole;
import de.fau.sensorlib.sensors.logging.NilsPodLoggable;
import de.fau.sensorlib.sensors.logging.NilsPodLoggingCallback;
import de.fau.sensorlib.sensors.logging.Session;
import de.fau.sensorlib.sensors.logging.SessionDownloader;
import de.fau.sensorlib.sensors.logging.SessionHandler;
import de.fau.sensorlib.widgets.config.ConfigItem;


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
            double[] gyro = null;
            double[] accel = null;
            double[] mag = null;
            double[] analog = null;
            double baro = Double.MIN_VALUE;
            double temp = Double.MIN_VALUE;
            int localCounter;

            // extract gyroscope data
            if (isSensorEnabled(HardwareSensor.GYROSCOPE)) {
                gyro = new double[3];
                for (int j = 0; j < 3; j++) {
                    gyro[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                    offset += 2;
                }
            }
            // extract accelerometer data
            if (isSensorEnabled(HardwareSensor.ACCELEROMETER)) {
                accel = new double[3];
                for (int j = 0; j < 3; j++) {
                    accel[j] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                    offset += 2;
                }
            }

            // extract magnetometer data
            // TODO check!
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

            if (isSensorEnabled(HardwareSensor.TEMPERATURE)) {
                temp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
                temp = temp * (1.0 / 512) + 23;
                offset += 2;
            }

            // extract packet counter (16 bit)
            localCounter = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i + mPacketSize - 2);

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
            } else if (isSensorEnabled(HardwareSensor.MAGNETOMETER)) {
                df = new NilsPodMagDataFrame(this, timestamp, accel, gyro, baro, temp, mag);
            } else if (isSensorEnabled(HardwareSensor.TEMPERATURE)) {
                df = new NilsPodTempDataFrame(this, timestamp, accel, gyro, baro, temp);
            } else {
                df = new NilsPodDataFrame(this, timestamp, accel, gyro, baro);
            }

            // send new data to the SensorDataProcessor
            sendNewData(df);

            Log.d(TAG, df.toString());

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
    public void downloadSession(int sessionId) throws SensorException {
        mSessionDownloader = new SessionDownloader(this, mSessionHandler.getSessionById(sessionId));
        mSessionDownloader.setCsvExportEnabled(true);
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
        NilsPodOperationMode operationMode = (NilsPodOperationMode) mCurrentConfigMap.get(KEY_OPERATION_MODE);
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
                case KEY_OPERATION_MODE:
                    operationMode = (NilsPodOperationMode) configMap.get(key);
                    break;
                case KEY_MOTION_INTERRUPT:
                    interrupt = (NilsPodMotionInterrupt) configMap.get(key);
                    break;
            }
        }

        try {
            writeSamplingRateConfig(samplingRate);
            writeSyncConfig(syncRole, syncGroup);
            writeSensorConfig(sensors);
            writeSystemSettingsConfig(sensorPosition, operationMode, interrupt);
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

    protected void writeSamplingRateConfig(double samplingRate) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SAMPLING_RATE_CONFIG);
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
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SYNC_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int offset = 0;

        value[offset++] = (byte) syncRole.ordinal();
        value[offset++] = (byte) syncGroup.getSyncChannel();
        System.arraycopy(syncGroup.getSyncAddress(), 0, value, offset, 5);

        writeNilsPodConfig(config, oldValue, value);
    }

    protected void writeSensorConfig(ArrayList<HardwareSensor> sensors) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SENSOR_CONFIG);
        byte[] oldValue = config.getValue();

        byte[] value = new byte[2];
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

        writeNilsPodConfig(config, new byte[]{oldValue[0], oldValue[1]}, value);
    }

    protected void writeSystemSettingsConfig(NilsPodSensorPosition sensorPosition, NilsPodOperationMode operationMode, NilsPodMotionInterrupt motionInterrupt) throws SensorException {
        BluetoothGattCharacteristic config = getConfigurationService().getCharacteristic(AbstractNilsPodSensor.NILS_POD_SYSTEM_SETTINGS_CONFIG);
        byte[] oldValue = config.getValue();
        byte[] value = oldValue.clone();

        int offset = 0;

        value[offset++] = (byte) sensorPosition.ordinal();

        int byteVal = 0;
        if (operationMode == NilsPodOperationMode.HOME_MONITORING_MODE) {
            byteVal = byteVal | 0x40;
        }
        if (motionInterrupt == NilsPodMotionInterrupt.MOTION_INTERRUPT_ENABLED) {
            byteVal = byteVal | 0x80;
        }

        value[offset] = (byte) byteVal;

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
        protected boolean hasBaro;

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

        protected double temp;
        protected boolean hasTemp;

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

        protected double[] mag;
        protected boolean hasMag;


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

        protected double[] analog;
        protected boolean hasAnalog;

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

        @Override
        public String toString() {
            String str = super.toString();

            if (hasAnalog) {
                str += ", analog: " + Arrays.toString(analog);
            }

            return str;
        }
    }
}
