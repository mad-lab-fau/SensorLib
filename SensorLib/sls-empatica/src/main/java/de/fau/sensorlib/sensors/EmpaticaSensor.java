/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.BloodVolumePulseDataFrame;
import de.fau.sensorlib.dataframe.EdaDataFrame;
import de.fau.sensorlib.dataframe.HeartRateDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.dataframe.SimpleDataFrame;
import de.fau.sensorlib.enums.SensorState;

/**
 * Empatica sensor (https://www.empatica.com/)
 * <p>
 * This sensor is a little more complex to operate:
 * (1) you need an API key you get after registering a developer account on their site.
 * (2) You dev-account/API-key needs to be assigned/registered to the sensors you want to use (in the dev-area on their dev-site).
 * (3) During the connection process (Android->Empatica sensor) an I-Net connection is required so the driver can fetch the authorization from the empatica-site that you are allowed to use the given sensor.
 * (4) Connections can only be made after the EmpaDeviceManager has been initialized, i.e. after receiving the EmpaStatus.READY status update. Any call to anything in the Empatica-API will fail with an exception before that!!
 * <p>
 * Our connection scheme works as follows:
 * (1) the sensor is created, sensor state is UNDEFINED
 * (2) status update sends: READY, sensor state is INITIALIZED, or if it was CONNECTING we immediately start search the desired sensor
 * (3) if a connect call comes in and Empatica API is not READY yet, set sensor state to CONNECTING, otherwise, start search for the sensor
 * (4) sensor found notification: connect to the sensor
 */
public class EmpaticaSensor extends AbstractSensor {
    private static final String API_KEY = "2e588653185f41e2b59c26868e04dd53";
    private EmpaDeviceManager mDeviceManager = null;

    private static final double EMPA_TIMESTAMP_TO_MILLISECONDS = 1000d;

    private EmpaStatus mEmpaStatus;

    public static class EmpaticaAccelDataFrame extends SensorDataFrame implements AccelDataFrame {
        double ax, ay, az;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public EmpaticaAccelDataFrame(AbstractSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            ax = x;
            ay = y;
            az = z;
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

    public static class EmpaticaBvpDataFrame extends SensorDataFrame implements BloodVolumePulseDataFrame {
        double bvp;

        public EmpaticaBvpDataFrame(AbstractSensor fromSensor, double timestamp, double bvp) {
            super(fromSensor, timestamp);
            this.bvp = bvp;
        }

        @Override
        public double getBloodVolumePulse() {
            return bvp;
        }
    }

    public static class EmpaticaIbiDataFrame extends SensorDataFrame implements HeartRateDataFrame {
        double ibi;

        public EmpaticaIbiDataFrame(AbstractSensor fromSensor, double timestamp, double ibi) {
            super(fromSensor, timestamp);
            this.ibi = ibi;
        }

        @Override
        public double getHeartRate() {
            return 60d / ibi;
        }

        @Override
        public double getInterbeatInterval() {
            return ibi;
        }
    }

    public static class EmpaticaEdaDataFrame extends SensorDataFrame implements EdaDataFrame {
        double gsr;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public EmpaticaEdaDataFrame(AbstractSensor fromSensor, double timestamp, double gsr) {
            super(fromSensor, timestamp);
            this.gsr = gsr;
        }

        @Override
        public double getElectrodermalActivity() {
            return gsr;
        }
    }


    private class EmpaticaInternalHandler extends InternalHandler implements EmpaDataDelegate, EmpaStatusDelegate {

        public EmpaticaInternalHandler(AbstractSensor sensor) {
            super(sensor);
        }

        @Override
        public void didUpdateStatus(EmpaStatus empaStatus) {
            Log.d(this.getClass().getSimpleName(), "didUpdateStatus: " + empaStatus.name());
            mEmpaStatus = empaStatus;
            // ready signalized that the Empatica-API can be used
            if (empaStatus == EmpaStatus.READY) {
                sendSensorCreated();

                // if we have a connection request pending, we immediately start searching for the sensor.
                /*if (getState() == SensorState.CONNECTING) {
                    Log.e(TAG, "connecting...");
                } else {
                    // if no connection request was made, set sensor to initialized so we wait for a call to connect before searching for the sensor
                    setState(SensorState.INITIALIZED);
                }*/
            } else if (empaStatus == EmpaStatus.CONNECTED) {
                Log.d(getDeviceName(), "connected.");
                sendConnected();
            } else if (empaStatus == EmpaStatus.DISCONNECTED) {
                if (getState() == SensorState.CONNECTED) {
                    sendDisconnected();
                }
            }

            if (empaStatus == EmpaStatus.CONNECTED || empaStatus == EmpaStatus.DISCONNECTED || empaStatus == EmpaStatus.READY)
                sendNotification(getDeviceName() + " status: " + empaStatus.toString());
        }

        @Override
        public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus, EmpaSensorType empaSensorType) {
            sendNotification(getDeviceName() + " status: " + empaSensorStatus.toString());
        }

        @Override
        public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
            Log.d(getDeviceName(), "didDiscoverDevice " + bluetoothDevice.getName() + "; " + allowed);

            if (allowed && bluetoothDevice.getAddress().equals(getSensor().getDeviceAddress())) {
                // Stop scanning. We found the right device.
                mDeviceManager.stopScanning();
                try {
                    // Connect to the device
                    Log.d(this.getClass().getSimpleName(), "connecting to  " + bluetoothDevice.getName());
                    sendConnecting();
                    mDeviceManager.connectDevice(bluetoothDevice);
                } catch (ConnectionNotAllowedException e) {
                    // This should happen only if you try to connect when allowed == false.
                    //Toast.makeText(this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            sendNotification("Connecting: " + deviceName);
        }

        @Override
        public void didRequestEnableBluetooth() {
            sendNotification("Bt enable requested.");
        }

        @Override
        public void didReceiveGSR(float gsr, double timestamp) {
            // galvanic skin response
            if (getState() == SensorState.STREAMING) {
                sendNewData(new EmpaticaEdaDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, gsr));
            }
        }

        @Override
        public void didReceiveBVP(float bvp, double timestamp) {
            // blood volume pressure
            if (getState() == SensorState.STREAMING) {
                sendNewData(new EmpaticaBvpDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, bvp));
            }
        }

        @Override
        public void didReceiveIBI(float ibi, double timestamp) {
            // Inter beat interval
            if (getState() == SensorState.STREAMING) {
                sendNewData(new EmpaticaIbiDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, ibi));
            }
        }

        @Override
        public void didReceiveTemperature(float temp, double timestamp) {
            Log.d(this.getClass().getSimpleName(), "Temp: " + temp);
            if (getState() == SensorState.STREAMING) {
                sendNewData(new SimpleDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, 3, temp));
            }
        }

        @Override
        public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
            //Log.d( this.getClass().getSimpleName(), "Accel: " + ax + " " + ay + " " + az + ";  " + mExternalHandlers.size() );
            if (getState() == SensorState.STREAMING) {
                sendNewData(new EmpaticaAccelDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, x, y, z));
            }
        }

        @Override
        public void didReceiveBatteryLevel(float level, double timestamp) {
            // moved to getBatteryLevel()
            //sendNewData(new SimpleDataFrame(this.getSensor(), timestamp * EMPA_TIMESTAMP_TO_MILLISECONDS, 4, level));
            mBatteryLevel = (int) level;
        }
    }

    public EmpaticaSensor(Context context, SensorInfo knownSensor, SensorDataProcessor dataHandler) {
        this(context, knownSensor.getDeviceName(), knownSensor.getDeviceAddress(), dataHandler);
    }

    public EmpaticaSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
        init(context);
    }

    public EmpaticaSensor(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler, double desiredSamplingRate) {
        super(context, deviceName, deviceAddress, dataHandler, desiredSamplingRate);
        init(context);
    }

    private void init(Context context) {
        mInternalHandler = new EmpaticaInternalHandler(this);
        mDeviceManager = new EmpaDeviceManager(context, (EmpaDataDelegate) mInternalHandler, (EmpaStatusDelegate) mInternalHandler);
        mDeviceManager.authenticateWithAPIKey(API_KEY);
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        mDeviceManager.startScanning();

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (mDeviceManager != null) {
            mDeviceManager.disconnect();
            sendDisconnected();
        }
    }

    @Override
    public void startStreaming() {
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

    private int mBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public EmpaStatus getEmpaStatus() {
        return mEmpaStatus;
    }

}
