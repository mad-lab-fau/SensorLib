/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Gyro;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;

import java.util.List;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EegDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;

/**
 * Implementation for the Muse Headband.
 */
public class MuseSensor extends AbstractSensor {

    static {
        System.loadLibrary("muse_android");
    }

    private static final String TAG = MuseSensor.class.getSimpleName();

    /**
     * MuseManager is to detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid mMuseManager;

    /**
     * Refers to Muse headband. Used to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse mMuse;

    /**
     * ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     */
    private ConnectionListener mConnectionListener;

    /**
     * DataListener to receive EEG (and other) data from the
     * headband.
     */
    private DataListener mDataListener;

    private Handler mListenHandler = new Handler();
    private Runnable mStopListenRunnable = new Runnable() {
        @Override
        public void run() {
            mMuseManager.stopListening();
        }
    };

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    //private boolean dataTransmission = true;

    private int mBatteryLevel;

    public static class MuseEegDataFrame extends SensorDataFrame implements EegDataFrame {

        private MuseDataPacketType packetType;

        private double[] eegRaw;
        private double[] eegBand;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MuseEegDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public MuseDataPacketType getPacketType() {
            return packetType;
        }

        @Override
        public double[] getRawEeg() {
            return (packetType == MuseDataPacketType.EEG) ? eegBand : null;
        }

        public double[] getEegBand() {
            return eegBand;
        }

        @Override
        public double[] getAlphaBand() {
            return (packetType == MuseDataPacketType.ALPHA_RELATIVE) ? eegBand : null;
        }

        @Override
        public double[] getBetaBand() {
            return (packetType == MuseDataPacketType.BETA_RELATIVE) ? eegBand : null;
        }

        @Override
        public double[] getGammaBand() {
            return (packetType == MuseDataPacketType.GAMMA_RELATIVE) ? eegBand : null;
        }

        @Override
        public double[] getThetaBand() {
            return (packetType == MuseDataPacketType.THETA_RELATIVE) ? eegBand : null;
        }

        @Override
        public double[] getDeltaBand() {
            return (packetType == MuseDataPacketType.DELTA_RELATIVE) ? eegBand : null;
        }
    }

    public static class MuseAccelDataFrame extends SensorDataFrame implements AccelDataFrame {

        private double ax, ay, az;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MuseAccelDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getAccelX() {
            return ax;
        }

        @Override
        public double getAccelY() {
            return ay;
        }

        @Override
        public double getAccelZ() {
            return az;
        }
    }

    public static class MuseGyroDataFrame extends SensorDataFrame implements GyroDataFrame {

        private double gx, gy, gz;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MuseGyroDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getGyroX() {
            return gx;
        }

        @Override
        public double getGyroY() {
            return gy;
        }

        @Override
        public double getGyroZ() {
            return gz;
        }
    }

    public static class MuseArtifactDataFrame extends SensorDataFrame {

        private boolean blink;
        private boolean yawClench;
        private boolean bandOnOff;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public MuseArtifactDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public boolean getBlink() {
            return blink;
        }

        public boolean getYawClench() {
            return yawClench;
        }

        public boolean getBandOnOff() {
            return bandOnOff;
        }
    }


    public MuseSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);

        mMuseManager = MuseManagerAndroid.getInstance();
        mMuseManager.setContext(mContext);

        mConnectionListener = new ConnectionListener(this);
        mDataListener = new DataListener(this);
        mMuseManager.setMuseListener(new MuseScanListener(this));
    }

    public MuseSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getDeviceName(), knownSensor.getDeviceAddress(), dataHandler);
    }

    @Override
    public boolean connect() throws Exception {
        // start listening for available Muses
        mMuseManager.startListening();
        // stop listening after 10 seconds
        mListenHandler.postDelayed(mStopListenRunnable, 10000);

        return super.connect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        mMuse.disconnect();
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        for (HardwareSensor sensor : mSelectedHwSensors) {
            registerListener(sensor);
        }
        mMuse.enableDataTransmission(true);
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        mMuse.enableDataTransmission(false);
        for (HardwareSensor sensor : mSelectedHwSensors) {
            unregisterListener(sensor);
        }
        sendStopStreaming();
    }

    private void onMusesFound() {
        List<Muse> muses = mMuseManager.getMuses();
        for (Muse muse : muses) {
            if (getDeviceAddress().equals(muse.getMacAddress())) {
                Log.d(TAG, "connect Muse");
                mMuse = muse;
                mMuse.registerConnectionListener(mConnectionListener);
                for (HardwareSensor sensor : mSelectedHwSensors) {
                    useHardwareSensor(sensor);
                }
                mMuse.runAsynchronously();
            }
        }
    }

    private void registerListener(HardwareSensor sensor) {
        switch (sensor) {
            case EEG_RAW:
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.EEG);
                break;
            case EEG_FREQ_BANDS:
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.ALPHA_RELATIVE);
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.BETA_RELATIVE);
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.GAMMA_RELATIVE);
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.DELTA_RELATIVE);
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.THETA_RELATIVE);
                break;
            case ACCELEROMETER:
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.ACCELEROMETER);
                break;
            case GYROSCOPE:
                mMuse.registerDataListener(mDataListener, MuseDataPacketType.GYRO);
                break;
        }
    }

    private void unregisterListener(HardwareSensor sensor) {
        switch (sensor) {
            case EEG_RAW:
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.EEG);
                break;
            case EEG_FREQ_BANDS:
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.ALPHA_RELATIVE);
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.BETA_RELATIVE);
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.GAMMA_RELATIVE);
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.DELTA_RELATIVE);
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.THETA_RELATIVE);
                break;
            case ACCELEROMETER:
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.ACCELEROMETER);
                break;
            case GYROSCOPE:
                mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.GYRO);
                break;
        }
    }

    public boolean shouldUseArtifactSensor(boolean enable) {
        if (enable) {
            mMuse.registerDataListener(mDataListener, MuseDataPacketType.ARTIFACTS);
            return true;
        } else {
            mMuse.unregisterDataListener(mDataListener, MuseDataPacketType.ARTIFACTS);
            return false;
        }
    }

    /**
     * Receive callback to this method each time there is a change to the
     * connection state of one of the headbands.
     *
     * @param packet A packet containing the current and prior connection states
     * @param muse   The headband whose state changed.
     */
    private void receiveMuseConnectionPacket(final MuseConnectionPacket packet, final Muse muse) {
        final ConnectionState currentState = packet.getCurrentConnectionState();
        Log.d(TAG, packet.getPreviousConnectionState() + " -> " + currentState);

        MuseVersion museVersion = muse.getMuseVersion();

        if (museVersion != null) {
            Log.d(TAG, "Muse version --- firmware type: " + museVersion.getFirmwareType() +
                    "\nfirmware version: " + museVersion.getFirmwareVersion() +
                    "\nprotocol version: " + museVersion.getProtocolVersion());

            mFirmwareRevisionString = museVersion.getFirmwareVersion();
            mFirmwareRevision = new FirmwareRevision(mFirmwareRevisionString);
        }


        if (currentState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Muse connected: " + muse.getName());
            Log.d(TAG, "Muse connected: " + muse.getMuseConfiguration().getMicrocontrollerId());
            sendConnected();
        }

        if (currentState == ConnectionState.DISCONNECTED) {
            Log.d(TAG, "Muse disconnected: " + muse.getName());
            // We have disconnected from the headband, so set our cached copy to null.
            this.mMuse = null;
            sendDisconnected();
        }
    }

    /**
     * Receive callback to this method each time the headband sends a MuseDataPacket.
     *
     * @param packet The data packet containing the data from the headband (eg. EEG data)
     */
    private void receiveMuseDataPacket(final MuseDataPacket packet) {
        SensorDataFrame df;

        switch (packet.packetType()) {
            case EEG:
            case ALPHA_RELATIVE:
            case BETA_RELATIVE:
            case DELTA_RELATIVE:
            case THETA_RELATIVE:
            case GAMMA_RELATIVE:
                df = extractEegChannels(packet);
                break;
            case ACCELEROMETER:
                df = extractAccelChannels(packet);
                break;
            case GYRO:
                df = extractGyroChannels(packet);
                break;
            case BATTERY:
                mBatteryLevel = (int) packet.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
                return;
            default:
                return;

        }

        sendNewData(df);
    }

    /**
     * Callback for artifacts if the ARTIFACTS data type is registered.
     * Artifacts are generated when eye blinks are detected, the jaw is clenched and
     * when the headband is put on or removed.
     *
     * @param p The artifact packet with the data from the headband.
     */
    private void receiveMuseArtifactPacket(final MuseArtifactPacket p) {
        sendNewData(extractArtifactChannels(p));
    }

    @Override
    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    private SensorDataFrame extractEegChannels(MuseDataPacket packet) {
        MuseEegDataFrame df = new MuseEegDataFrame(this, packet.timestamp());
        df.packetType = packet.packetType();

        if (packet.packetType() == MuseDataPacketType.EEG) {
            df.eegRaw = new double[Eeg.values().length];
            for (Eeg eeg : Eeg.values()) {
                df.eegRaw[eeg.ordinal()] = packet.getEegChannelValue(eeg);
            }
        } else {
            df.eegBand = new double[Eeg.values().length];
            for (Eeg eeg : Eeg.values()) {
                df.eegBand[eeg.ordinal()] = packet.getEegChannelValue(eeg);
            }
        }

        return df;
    }

    private SensorDataFrame extractAccelChannels(MuseDataPacket packet) {
        MuseAccelDataFrame df = new MuseAccelDataFrame(this, packet.timestamp());

        df.ax = packet.getAccelerometerValue(Accelerometer.X);
        df.ay = packet.getAccelerometerValue(Accelerometer.Y);
        df.az = packet.getAccelerometerValue(Accelerometer.Z);

        return df;
    }

    private SensorDataFrame extractGyroChannels(MuseDataPacket packet) {
        MuseGyroDataFrame df = new MuseGyroDataFrame(this, packet.timestamp());

        df.gx = packet.getGyroValue(Gyro.X);
        df.gy = packet.getGyroValue(Gyro.Y);
        df.gz = packet.getGyroValue(Gyro.Z);

        return df;
    }

    private SensorDataFrame extractArtifactChannels(MuseArtifactPacket packet) {
        MuseArtifactDataFrame df = new MuseArtifactDataFrame(this, System.currentTimeMillis());

        df.blink = packet.getBlink();
        df.bandOnOff = packet.getHeadbandOn();
        df.yawClench = packet.getJawClench();

        return df;
    }


    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener.
    // Each class simply forwards the messages it receives back to the Activity.
    private class MuseScanListener extends MuseListener {
        final MuseSensor mSensor;

        MuseScanListener(final MuseSensor sensor) {
            mSensor = sensor;
        }

        @Override
        public void museListChanged() {
            mSensor.onMusesFound();
        }


    }

    private class ConnectionListener extends MuseConnectionListener {
        final MuseSensor mSensor;

        ConnectionListener(final MuseSensor sensor) {
            mSensor = sensor;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            mSensor.receiveMuseConnectionPacket(p, muse);
        }
    }

    private class DataListener extends MuseDataListener {
        final MuseSensor mSensor;

        DataListener(final MuseSensor sensor) {
            mSensor = sensor;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            mSensor.receiveMuseDataPacket(p);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            mSensor.receiveMuseArtifactPacket(p);
        }
    }
}
