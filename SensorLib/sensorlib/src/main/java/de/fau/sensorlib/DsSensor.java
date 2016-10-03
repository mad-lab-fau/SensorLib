/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.util.Log;

import java.util.ArrayList;
import java.util.EnumSet;

import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * The base class for all sensor-implementations in the SensorLib.
 */
public abstract class DsSensor extends SensorInfo {

    private static final String TAG = "DsSensor";

    protected static final int MESSAGE_NEW_DATA = 1010;
    protected static final int MESSAGE_NOTIFICATION = 1011;
    protected static final int MESSAGE_SENSOR_CREATED = 1012;
    protected static final int MESSAGE_CONNECTING = 1013;
    protected static final int MESSAGE_CONNECTED = 1014;
    protected static final int MESSAGE_DISCONNECTED = 1015;
    protected static final int MESSAGE_CONNECTION_LOST = 1016;
    protected static final int MESSAGE_START_STREAMING = 1017;
    protected static final int MESSAGE_STOP_STREAMING = 1018;
    protected static final int MESSAGE_SAMPLING_RATE_CHANGED = 1019;

    public static final String DATA_BROADCAST_INTENT_ACTION = "de.fau.sensorlib.DataBroadcast";



    /**
     * The sampling rate in Hz that is used to acquire data samples from the sensor.
     */
    protected double mSamplingRate;

    /**
     * Context this sensor is used in.
     */
    protected Context mContext;

    /**
     * The internal message handler for this sensor.
     */
    protected InternalHandler mInternalHandler;

    /**
     * A list of external handlers outside the SensorLib that can be assigned by the calling application.
     */
    protected ArrayList<SensorDataProcessor> mExternalHandlers = new ArrayList<>(2);

    /**
     * A list of HardwareSensors that were selected by the calling application. Only these sensors report their data to the application.
     */
    protected EnumSet<HardwareSensor> mSelectedHwSensors = EnumSet.noneOf(HardwareSensor.class);

    /**
     * The state the sensor is currently in.
     */
    private SensorState mSensorState = SensorState.UNDEFINED;

    /**
     * Whether to keep the process alive using a background service that holds a reference to the sensor class.
     */
    private boolean mKeepAliveViaService = true;

    /**
     * For network streaming of data. Not yet implemented!
     */
    private DsNetworkStream mNetworkStream = null;

    /**
     * Whether to send each data sample received from the sensor as a system broadcast to the Android device.
     */
    private boolean mSendDataBroadcast = false;

    /**
     * Possible supported hardware sensors.
     */
    public enum HardwareSensor {
        ACCELEROMETER,
        ECG,
        EMG,
        GYROSCOPE,
        MAGNETOMETER,
        LIGHT,
        PRESSURE,
        TEMPERATURE,
        RESPIRATION,
        HEART_RATE,
        BLOOD_PRESSURE,
        BLOOD_VOLUME_PRESSURE,
        GALVANIC_SKIN_RESPONSE,
        GESTURE,
        ORIENTATION,
        NOISE,
        HUMIDITY;

        public static boolean isInertial(HardwareSensor s) {
            return s == ACCELEROMETER || s == GYROSCOPE || s == MAGNETOMETER;
        }

        public static boolean isAmbient(HardwareSensor s) {
            return s == LIGHT || s == PRESSURE || s == TEMPERATURE || s == NOISE || s == HUMIDITY;
        }
    }


    /**
     * The states a DsSensor can be in.
     */
    public enum SensorState {
        UNDEFINED,
        INITIALIZED,
        DISCONNECTED,
        CONNECTION_LOST,
        CONNECTING,
        CONNECTED,
        STREAMING
    }

    /**
     * The default internal handler class used if no custom class is implemented.
     */
    protected static class InternalHandler extends Handler {
        private DsSensor mSensor;

        public InternalHandler(DsSensor sensor) {
            mSensor = sensor;
        }

        public DsSensor getSensor() {
            return mSensor;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                //handlers have a what identifier which is used to identify the type of msg
                switch (msg.what) {
                    case DsSensor.MESSAGE_NEW_DATA:
                        //Log.e( this.getClass().getSimpleName(), "Received new data " + getSensor().getDeviceName() + " :: " + msg.obj );
                        getSensor().dispatchNewData((SensorDataFrame) msg.obj);
                        break;

                    case DsSensor.MESSAGE_NOTIFICATION:
                        getSensor().dispatchNotification(msg.obj);
                        break;

                    case DsSensor.MESSAGE_CONNECTING:
                        getSensor().dispatchConnecting();
                        break;

                    case DsSensor.MESSAGE_CONNECTED:
                        getSensor().dispatchConnected();
                        break;

                    case DsSensor.MESSAGE_SENSOR_CREATED:
                        getSensor().dispatchSensorCreated();
                        break;

                    case DsSensor.MESSAGE_START_STREAMING:
                        getSensor().dispatchStartStreaming();
                        break;

                    case DsSensor.MESSAGE_STOP_STREAMING:
                        getSensor().dispatchStopStreaming();
                        break;

                    case DsSensor.MESSAGE_DISCONNECTED:
                        getSensor().dispatchDisconnected();
                        break;

                    case DsSensor.MESSAGE_CONNECTION_LOST:
                        getSensor().dispatchConnectionLost();
                        break;

                    case DsSensor.MESSAGE_SAMPLING_RATE_CHANGED:
                        getSensor().dispatchSamplingRateChanged();
                        break;

                    default:
                        Log.e(this.getClass().getSimpleName(), "Unknown message received.");
                        break;
                }
            } catch (Exception e) {
                //some unknown error occurred
                Log.e(this.getClass().getSimpleName(), "An error occured on sensor data processing!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Default constructor. IMPORTANT: do not initialize any static or sensor-side-specific components here.
     * If you need to initialize something on the sensor-side etc do this in the connect method.
     *
     * @param context       the context.
     * @param deviceName    the name of the device/sensor.
     * @param deviceAddress the addess (bluetooth, IP, etc.) that is used to contact the sensor.
     * @param dataHandler   a default/initial data handler for sensor data and notifications.
     */
    public DsSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(deviceName, deviceAddress);
        mName = deviceName;
        mDeviceAddress = deviceAddress;
        mContext = context;
        mInternalHandler = new InternalHandler(this);
        addDataHandler(dataHandler);
        sendSensorCreated();
    }

    /**
     * @param context             the context.
     * @param deviceName          the name of the device/sensor.
     * @param deviceAddress       the addess (bluetooth, IP, etc.) that is used to contact the sensor.
     * @param dataHandler         a default/initial data handler for sensor data and notifications.
     * @param desiredSamplingRate the desired sampling rate for this sensor. See {@link #requestSamplingRateChange(double)} for further information.
     */
    public DsSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        this(context, deviceName, deviceAddress, dataHandler);
        mSamplingRate = desiredSamplingRate;
    }


    /**
     * @param context       the context.
     * @param deviceAddress the device address.
     * @param dataHandler   the data handler.
     */
    public DsSensor(Context context, String deviceAddress, SensorDataProcessor dataHandler) {
        this(context, DsSensorManager.getNameForDeviceAddress(deviceAddress), deviceAddress, dataHandler);
    }

    /**
     * @param context     the context.
     * @param knownSensor the KnownSensor to associate with.
     * @param dataHandler the data handler.
     */
    public DsSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getName(), knownSensor.getDeviceAddress(), dataHandler);
    }


    /**
     * Connect this sensor to the service, possibly starting the service if it is not running yet.
     *
     * @return true if successfully connected to the service, false otherwise.
     */
    private boolean connectToService() {
        if (mContext == null) {
            return false;
        }
        if (mContext.startService(new Intent(mContext, DsSensorService.class)) == null) {
            return false;
        }
        DsSensorService.attachSensor(this);
        return true;
    }

    /**
     * Disconnects this sensor from the service.
     */
    private void disconnectFromService() {
        DsSensorService.detachSensor(this);
    }

    /**
     * Whether this sensor should also report data while its calling App is in the background.
     *
     * @param keepAlive true to keep this sensor alive using the sensor service.
     */
    public void keepAliveViaService(boolean keepAlive) {
        if (!mKeepAliveViaService && keepAlive) {
            connectToService();
        }
        if (!keepAlive) {
            disconnectFromService();
        }
        mKeepAliveViaService = keepAlive;
    }

    /**
     * Activate or deactivate broadcasting of the new data frames from this sensor via a broadcast intent.
     *
     * @param activateBroadcasts whether to enable broadcasting of data frames or not.
     */
    public void broadcastData(boolean activateBroadcasts) {
        mSendDataBroadcast = activateBroadcasts;
    }

    /**
     * Opens a connection to the sensor.
     * <p>
     * If no hardware sensor was selected by calling one of the useHardwareSensor(s) methods, all available hardware sensors will automatically be used.
     *
     * @return true if the connection has been established successfully. False otherwise.
     * @throws Exception
     */
    @CallSuper
    public boolean connect() throws Exception {
        sendConnecting();

        if (mKeepAliveViaService) {
            if (!connectToService())
                return false;
        }

        if (mSelectedHwSensors.isEmpty()) {
            mSelectedHwSensors.addAll(providedSensors());
        }
        return true;
    }

    /**
     * Closes the connection to the sensor.
     */
    @CallSuper
    public void disconnect() {
        if (getState().ordinal() > SensorState.CONNECTED.ordinal()) {
            stopStreaming();
        }
        disconnectFromService();
    }

    /**
     * @return true if the sensor is connected, false otherwise.
     */
    public boolean isConnected() {
        return (mSensorState.ordinal() >= SensorState.CONNECTED.ordinal());
    }

    /**
     * Requests the data streaming to begin. Depending on the sensor, it might not be necessary to call this, since streaming begins automatically after a connection has been established.
     */
    public abstract void startStreaming();

    /**
     * Requests the data streaming to stop. Depending on the sensor, this might induce a disconnect.
     */
    public abstract void stopStreaming();

    /**
     * @return the address under which this device can be found, e.g. this can be the Bluetooth MAC-address, or the IP-address for WLAN-connected sensors.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    /**
     * @return a not necessarily unique, human readable name for this sensor.
     */
    public String getDeviceName() {
        return mName;
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * @return the sampling rate for the current sensor connection in Hz.
     */
    public double getSamplingRate() {
        return mSamplingRate;
    }

    /**
     * Sets the sampling rate for this sensor.
     *
     * @param samplingRate the sampling rate to set.
     */
    protected void setSamplingRate(double samplingRate) {
        if (samplingRate != this.mSamplingRate) {
            this.mSamplingRate = samplingRate;
            sendSamplingRateChanged();
        }
        this.mSamplingRate = samplingRate;
    }

    /**
     * Requests a change of the sampling rate for this sensor. This may or may not succeed, depending on the sensor and its internal state. If successful, the new sampling rate will be reported via the SensorDataProcessor.onSamplingRateChanged notification.
     *
     * @param toSamplingRate the sampling rate to which the sensor should switch.
     * @return true if the sampling rate can change, false if it is definitely not possible
     */
    public boolean requestSamplingRateChange(double toSamplingRate) {
        setSamplingRate(toSamplingRate);
        return true;
    }

    /**
     * Changes the sensor state.
     *
     * @param newState the new state for the sensor.
     */
    protected void setState(SensorState newState) {
        Log.d(TAG, "<" + mName + "> " + mSensorState + " --> " + newState);
        mSensorState = newState;
    }

    /**
     * @return the current sensor state.
     */
    public SensorState getState() {
        return mSensorState;
    }

    /**
     * @param context the context.
     * @deprecated The context has to be set in the constructor.
     */
    public void setContext(Context context) {
        this.mContext = context;
    }

    /**
     * Adds an additional SensorDataProcessor to this sensor.
     *
     * @param handler the additional data handler that will also receive all sensor callbacks.
     */
    public void addDataHandler(SensorDataProcessor handler) {
        if (handler == null) {
            return;
        }

        for (SensorDataProcessor sdp : mExternalHandlers) {
            if (sdp == handler)
                return;
        }

        mExternalHandlers.add(handler);
    }

    /**
     * Selects the specified hardware sensors to be used and data from them reported back for this DsSensor. If the selected hardware sensors are not available the returned values are undefined for these types.
     *
     * @param hwSensors the requested (internal) hardware sensors from which data is needed.
     */
    public boolean useHardwareSensors(EnumSet<HardwareSensor> hwSensors) {
        mSelectedHwSensors = hwSensors;
        return true;
    }

    /**
     * Requests the specified hardware sensor to be used and data from it reported back for this DsSensor. If the requested hardware is not available the returned values are undefined.
     *
     * @param sensor the requested (internal) hardware sensor from which data is needed.
     */
    public boolean useHardwareSensor(HardwareSensor sensor) {
        mSelectedHwSensors.add(sensor);
        return providedSensors().contains(sensor);
    }

    /**
     * This method checkes whether the given kind of hardware sensor was selected for use in a previous call to {@link #useHardwareSensor(HardwareSensor)}.
     *
     * @param sensor the kind of hardware sensor that should be checked.
     * @return whether the given sensor was selected for use.
     */
    public boolean shouldUseHardwareSensor(HardwareSensor sensor) {
        return mSelectedHwSensors.contains(sensor);
    }

    public boolean shouldUseInertialSensor() {
        for (HardwareSensor s : mSelectedHwSensors) {
            if (HardwareSensor.isInertial(s))
                return true;
        }
        return false;
    }

    public boolean shouldUseAmbientSensor() {
        for (HardwareSensor s : mSelectedHwSensors) {
            if (HardwareSensor.isAmbient(s))
                return true;
        }
        return false;
    }


    /**
     * Checks for availability of a (hardware) sensor unit.
     *
     * @param hwSensor the sensor/capability to check for.
     * @return true if the sensor/capability is present, false if not.
     */
    public boolean hasHardwareSensor(HardwareSensor hwSensor) {
        return providedSensors().contains(hwSensor);
    }

    /**
     * Informs about all (internal) existing/provided hardware sensors for this DsSensor. This has to be implemented by all sensor implementations.
     *
     * @return an enum containing all the available/provided hardware sensors.
     */
    protected abstract EnumSet<HardwareSensor> providedSensors();

    /**
     * Sends an onNotify to all external handlers.
     *
     * @param notification notification object to send.
     */
    protected void sendNotification(Object notification) {
        mInternalHandler.obtainMessage(MESSAGE_NOTIFICATION, notification).sendToTarget();
    }

    protected void dispatchNotification(Object notification) {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onNotify(this, notification);
        }
    }

    /**
     * Sends a new SensorDataFrame to all external handlers.
     *
     * @param data The SensorDataFrame to send to the external handlers.
     */
    protected void sendNewData(SensorDataFrame data) {
        //Log.d( this.getClass().getSimpleName(), "new data " + data.getOriginatingSensor() );
        mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, data).sendToTarget();
    }

    protected void dispatchNewData(SensorDataFrame data) {
        //Log.d( this.getClass().getSimpleName(), "dispatch " + mExternalHandlers.size() );
        for (SensorDataProcessor sdp : mExternalHandlers) {
            //Log.d( this.getClass().getSimpleName(), "dispatch to " + sdp.getClass().getSimpleName() );
            sdp.onNewData(data);
        }

        // check if we should broadcast the data to Android env
        if (mSendDataBroadcast && mContext != null) {
            Intent i = new Intent(DATA_BROADCAST_INTENT_ACTION);
            data.putToIntent(i);
            mContext.sendBroadcast(i);
        }

        // check if we should send the data to the network
        if (mNetworkStream != null) {
            // TODO
        }
    }

    /**
     * Sends the sensor created event.
     */
    protected void sendSensorCreated() {
        setState(SensorState.INITIALIZED);
        mInternalHandler.obtainMessage(MESSAGE_SENSOR_CREATED).sendToTarget();
    }

    protected void dispatchSensorCreated() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSensorCreated(this);
        }
    }

    protected void sendConnecting() {
        if (mSensorState.ordinal() < SensorState.CONNECTING.ordinal()) {
            setState(SensorState.CONNECTING);
        }
        mInternalHandler.obtainMessage(MESSAGE_CONNECTING).sendToTarget();
    }

    protected void dispatchConnecting() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnecting(this);
        }
    }

    protected void sendConnected() {
        if (mSensorState.ordinal() < SensorState.CONNECTED.ordinal()) {
            setState(SensorState.CONNECTED);
        }
        mInternalHandler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
    }

    protected void dispatchConnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnected(this);
        }
    }

    /**
     * Sends the disconnected event to all handlers.
     */
    protected void sendDisconnected() {
        setState(SensorState.DISCONNECTED);
        mInternalHandler.obtainMessage(MESSAGE_DISCONNECTED).sendToTarget();
        disconnectFromService();
    }

    protected void dispatchDisconnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onDisconnected(this);
        }
    }

    protected void sendConnectionLost() {
        setState(SensorState.CONNECTION_LOST);
        mInternalHandler.obtainMessage(MESSAGE_CONNECTION_LOST).sendToTarget();
    }

    protected void dispatchConnectionLost() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnectionLost(this);
        }
    }

    protected void sendStartStreaming() {
        setState(SensorState.STREAMING);
        mInternalHandler.obtainMessage(MESSAGE_START_STREAMING).sendToTarget();
    }

    protected void dispatchStartStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStartStreaming(this);
        }
    }

    protected void sendStopStreaming() {
        mInternalHandler.obtainMessage(MESSAGE_STOP_STREAMING).sendToTarget();
    }

    protected void dispatchStopStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStopStreaming(this);
        }
    }

    protected void sendSamplingRateChanged() {
        mInternalHandler.obtainMessage(MESSAGE_SAMPLING_RATE_CHANGED).sendToTarget();
    }

    protected void dispatchSamplingRateChanged() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSamplingRateChanged(this, getSamplingRate());
        }
    }
}
