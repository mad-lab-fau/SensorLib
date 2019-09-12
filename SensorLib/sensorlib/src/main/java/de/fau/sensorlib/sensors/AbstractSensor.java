/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.CallSuper;

import java.util.ArrayList;
import java.util.EnumSet;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;

/**
 * Base class for all sensor-implementations in the sensor lib.
 */
public abstract class AbstractSensor extends SensorInfo {

    protected static final String TAG = AbstractSensor.class.getSimpleName();

    private static final int MESSAGE_NEW_DATA = 1010;
    private static final int MESSAGE_NOTIFICATION = 1011;
    private static final int MESSAGE_SENSOR_CREATED = 1012;
    private static final int MESSAGE_CONNECTING = 1013;
    private static final int MESSAGE_CONNECTED = 1014;
    private static final int MESSAGE_DISCONNECTED = 1015;
    private static final int MESSAGE_CONNECTION_LOST = 1016;
    private static final int MESSAGE_START_STREAMING = 1017;
    private static final int MESSAGE_STOP_STREAMING = 1018;
    private static final int MESSAGE_START_LOGGING = 1019;
    private static final int MESSAGE_STOP_LOGGING = 1020;
    private static final int MESSAGE_SAMPLING_RATE_CHANGED = 1021;

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
     * 0-100 (%).
     */
    protected int mBatteryLevel;

    protected String mSerialNumberString = "";
    protected String mManufacturerString = "";
    protected String mModelNumberString = "";
    protected String mHardwareRevisionString = "";
    protected String mFirmwareRevisionString = "";
    protected String mSoftwareRevisionString = "";
    protected FirmwareRevision mFirmwareRevision = new FirmwareRevision();
    protected long mSensorSystemID;

    /**
     * The default internal handler class used if no custom class is implemented.
     */
    protected static class InternalHandler extends Handler {
        private AbstractSensor mSensor;

        public InternalHandler(AbstractSensor sensor) {
            mSensor = sensor;
        }

        public AbstractSensor getSensor() {
            return mSensor;
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                //handlers have a what identifier which is used to identify the type of msg
                switch (msg.what) {
                    case AbstractSensor.MESSAGE_NEW_DATA:
                        getSensor().dispatchNewData((SensorDataFrame) msg.obj);
                        break;

                    case AbstractSensor.MESSAGE_NOTIFICATION:
                        getSensor().dispatchNotification(msg.obj);
                        break;

                    case AbstractSensor.MESSAGE_CONNECTING:
                        if (getSensor().getState().ordinal() < SensorState.CONNECTING.ordinal()) {
                            getSensor().setState(SensorState.CONNECTING);
                        }
                        getSensor().dispatchConnecting();
                        break;

                    case AbstractSensor.MESSAGE_CONNECTED:
                        if (getSensor().getState().ordinal() < SensorState.CONNECTED.ordinal()) {
                            getSensor().setState(SensorState.CONNECTED);
                        }
                        getSensor().dispatchConnected();
                        break;

                    case AbstractSensor.MESSAGE_SENSOR_CREATED:
                        getSensor().setState(SensorState.INITIALIZED);
                        getSensor().dispatchSensorCreated();
                        break;

                    case AbstractSensor.MESSAGE_START_STREAMING:
                        getSensor().setState(SensorState.STREAMING);
                        getSensor().dispatchStartStreaming();
                        break;

                    case AbstractSensor.MESSAGE_STOP_STREAMING:
                        getSensor().setState(SensorState.CONNECTED);
                        getSensor().dispatchStopStreaming();
                        break;

                    case AbstractSensor.MESSAGE_START_LOGGING:
                        getSensor().setState(SensorState.LOGGING);
                        getSensor().dispatchStartLogging();
                        break;

                    case AbstractSensor.MESSAGE_STOP_LOGGING:
                        getSensor().setState(SensorState.CONNECTED);
                        getSensor().dispatchStopLogging();
                        break;

                    case AbstractSensor.MESSAGE_DISCONNECTED:
                        getSensor().setState(SensorState.DISCONNECTED);
                        getSensor().dispatchDisconnected();
                        break;

                    case AbstractSensor.MESSAGE_CONNECTION_LOST:
                        getSensor().setState(SensorState.CONNECTION_LOST);
                        getSensor().dispatchConnectionLost();
                        break;

                    case AbstractSensor.MESSAGE_SAMPLING_RATE_CHANGED:
                        getSensor().dispatchSamplingRateChanged();
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
    public AbstractSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        this(context, deviceName, deviceAddress, dataHandler, -1);
    }

    /**
     * @param context             the context.
     * @param deviceName          the name of the device/sensor.
     * @param deviceAddress       the address (bluetooth, IP, etc.) that is used to contact the sensor.
     * @param dataHandler         a default/initial data handler for sensor data and notifications.
     * @param desiredSamplingRate the desired sampling rate for this sensor. See {@link #requestSamplingRateChange(double)} for further information.
     */
    public AbstractSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        super(deviceName, deviceAddress, desiredSamplingRate);
        mDeviceName = deviceName;
        mDeviceAddress = deviceAddress;
        mContext = context;
        mInternalHandler = new InternalHandler(this);
        addDataHandler(dataHandler);
        sendSensorCreated();
    }


    /**
     * @param context       the context.
     * @param deviceAddress the device address.
     * @param dataHandler   the data handler.
     */
    public AbstractSensor(Context context, String deviceAddress, SensorDataProcessor dataHandler) {
        this(context, "", deviceAddress, dataHandler);
    }

    /**
     * @param context     the context.
     * @param knownSensor the KnownSensor to associate with.
     * @param dataHandler the data handler.
     */
    public AbstractSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getDeviceName(), knownSensor.getDeviceAddress(), dataHandler);
    }

    /**
     * Opens a connection to the sensor.
     * <p>
     * If no hardware sensor was selected by calling one of the useHardwareSensor(s) methods, all available hardware sensors will automatically be used.
     *
     * @return true if the connection has been established successfully. False otherwise.
     * @throws Exception if an error occurs during connection attempt
     */
    @CallSuper
    public boolean connect() throws Exception {
        sendConnecting();

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
    }

    /**
     * Requests the data streaming to begin. Depending on the sensor, it might not be necessary to call this, since streaming begins automatically after a connection has been established.
     */
    public abstract void startStreaming();

    /**
     * Requests the data streaming to stop. Depending on the sensor, this might induce a disconnect.
     */
    public abstract void stopStreaming();

    public Context getContext() {
        return mContext;
    }

    /**
     * Sets the sampling rate for this sensor.
     *
     * @param samplingRate the sampling rate to set.
     */
    protected void setSamplingRate(double samplingRate) throws SensorException {
        if (samplingRate >= 0.0) {
            if (samplingRate != mSamplingRate) {
                this.mSamplingRate = samplingRate;
                sendSamplingRateChanged();
            }
            this.mSamplingRate = samplingRate;
        }
    }

    /**
     * Requests a change of the sampling rate for this sensor. This may or may not succeed, depending on the sensor and its internal state. If successful, the new sampling rate will be reported via the SensorDataProcessor.onSamplingRateChanged notification.
     *
     * @param toSamplingRate the sampling rate to which the sensor should switch.
     * @return true if the sampling rate can change, false if it is definitely not possible
     */
    public boolean requestSamplingRateChange(double toSamplingRate) throws SensorException {
        setSamplingRate(toSamplingRate);
        return true;
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
            if (sdp == handler) {
                return;
            }
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
     * This method checks whether the given kind of hardware sensor was selected for use in a previous call to {@link #useHardwareSensor(HardwareSensor)}.
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
    protected EnumSet<HardwareSensor> providedSensors() {
        return mDeviceClass.getAvailableSensors();
    }

    public EnumSet<HardwareSensor> getSelectedSensors() {
        return mSelectedHwSensors;
    }

    public boolean hasBatteryMeasurement() {
        return mDeviceClass.hasBatteryMeasurement();
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getSerialNumberString() {
        return mSerialNumberString;
    }

    public String getManufacturerString() {
        return mManufacturerString;
    }

    public String getModelNumberString() {
        return mModelNumberString;
    }

    public String getHardwareRevisionString() {
        return mHardwareRevisionString;
    }

    public String getFirmwareRevisionString() {
        return mFirmwareRevisionString;
    }

    public FirmwareRevision getFirmwareRevision() {
        return mFirmwareRevision;
    }

    public String getSoftwareRevisionString() {
        return mSoftwareRevisionString;
    }

    private long getSensorSystemID() {
        return mSensorSystemID;
    }

    /**
     * Sends an onNotify to all external handlers.
     *
     * @param notification notification object to send.
     */
    protected void sendNotification(Object notification) {
        mInternalHandler.obtainMessage(MESSAGE_NOTIFICATION, notification).sendToTarget();
    }

    private void dispatchNotification(Object notification) {
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
        mInternalHandler.obtainMessage(MESSAGE_NEW_DATA, data).sendToTarget();
    }

    private void dispatchNewData(SensorDataFrame data) {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onNewData(data);
        }
    }

    /**
     * Sends the sensor created event.
     */
    protected void sendSensorCreated() {
        mInternalHandler.sendEmptyMessage(MESSAGE_SENSOR_CREATED);
    }

    private void dispatchSensorCreated() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSensorCreated(this);
        }
    }

    protected void sendConnecting() {
        mInternalHandler.sendEmptyMessage(MESSAGE_CONNECTING);
    }

    private void dispatchConnecting() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnecting(this);
        }
    }

    protected void sendConnected() {
        mInternalHandler.sendEmptyMessage(MESSAGE_CONNECTED);
    }

    private void dispatchConnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnected(this);
        }
    }

    /**
     * Sends the disconnected event to all handlers.
     */
    protected void sendDisconnected() {
        mInternalHandler.sendEmptyMessage(MESSAGE_DISCONNECTED);
    }

    private void dispatchDisconnected() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onDisconnected(this);
        }
    }

    protected void sendConnectionLost() {
        mInternalHandler.sendEmptyMessage(MESSAGE_CONNECTION_LOST);
    }

    private void dispatchConnectionLost() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onConnectionLost(this);
        }
    }

    protected void sendStartStreaming() {
        mInternalHandler.obtainMessage(MESSAGE_START_STREAMING).sendToTarget();
    }

    private void dispatchStartStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStartStreaming(this);
        }
    }

    protected void sendStopStreaming() {
        mInternalHandler.obtainMessage(MESSAGE_STOP_STREAMING).sendToTarget();
    }

    private void dispatchStopStreaming() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStopStreaming(this);
        }
    }

    protected void sendStartLogging() {
        mInternalHandler.obtainMessage(MESSAGE_START_LOGGING).sendToTarget();
    }

    private void dispatchStartLogging() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStartLogging(this);
        }
    }

    protected void sendStopLogging() {
        mInternalHandler.obtainMessage(MESSAGE_STOP_LOGGING).sendToTarget();
    }

    private void dispatchStopLogging() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onStopLogging(this);
        }
    }

    protected void sendSamplingRateChanged() {
        mInternalHandler.obtainMessage(MESSAGE_SAMPLING_RATE_CHANGED).sendToTarget();
    }

    private void dispatchSamplingRateChanged() {
        for (SensorDataProcessor sdp : mExternalHandlers) {
            sdp.onSamplingRateChanged(this, getSamplingRate());
        }
    }
}
