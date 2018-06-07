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
import android.hardware.Sensor;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;


/**
 * Implementation for the Motorola Smartwatch.
 */
public class SmartWatch extends AbstractSensor {


    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class SmartWatchSensorDataFrame extends SensorDataFrame implements AccelDataFrame, GyroDataFrame, MagDataFrame {

        public double ax, ay, az;
        public double gx, gy, gz;
        public double mx, my, mz;

        public SmartWatchSensorDataFrame(AbstractSensor fromSensor, double timestamp) {
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

        @Override
        public double getMagX() {
            return mx;
        }

        @Override
        public double getMagY() {
            return my;
        }

        @Override
        public double getMagZ() {
            return mz;
        }
    }


    // Log tag
    private final String TAG = SmartWatch.class.getSimpleName();

    // Google Wearable API
    private GoogleApiClient mGoogleApiClient;

    // Own instance
    public static SmartWatch instance;

    public static SmartWatch getInstance() {
        return instance;
    }

    // Connection status
    private boolean mConnected;

    // Data per sensor:
    // 1) sensorType 2) accuracy, 3) timestamp 4) - 6) 1D to 3D data
    private final int mDataEntriesPerSensor = 6;


    /**
     * Constructor.
     */
    public SmartWatch(Context context, BluetoothDevice device, SensorDataProcessor dataHandler) {
        super(context, device.getName(), device.getAddress(), dataHandler);
        mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        instance = this;
        mConnected = false;
        sendSensorCreated();
    }

    /**
     * Method to connect the DsSensor interface to the physical sensor.
     */
    @Override
    public boolean connect() throws Exception {
        super.connect();
        Log.i(TAG, "connect");
        mConnected = true;
        return true;
    }


    /**
     * Method to disconnect the DsSensor interface from the physical sensor.
     */
    @Override
    public void disconnect() {
        super.disconnect();
        Log.i(TAG, "disconnect");
        mConnected = false;
    }


    /**
     * Method to start streaming sensor data.
     */
    @Override
    public void startStreaming() {
        sendStartStreaming();
    }


    /**
     * Method to stop streaming sensor data.
     */
    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

    protected void sendConnectedWear() {
        sendConnected();
    }

    protected void sendDisconnectedWear() {
        sendDisconnected();
    }


    /**
     * Method to extract sensor data
     *
     * @param dataMap map of decrypted data
     */
    public void unpackSensorData(DataMap dataMap) {

        if (mInternalHandler == null) {
            return;
        }

        if (!mConnected) {
            return;
        }

        Log.d(TAG, "unpackSensorData()");

        int sensorType;
        int accuracy;
        int timestamp;

        float[] data = dataMap.getFloatArray("sensors");

        int max = (int) (data.length / mDataEntriesPerSensor);

        for (int i = 0; i < max; i++) {

            sensorType = (int) data[i * mDataEntriesPerSensor];
            accuracy = (int) data[i * mDataEntriesPerSensor + 1];
            timestamp = (int) data[i * mDataEntriesPerSensor + 2];

            SmartWatchSensorDataFrame frame = new SmartWatchSensorDataFrame(this, timestamp);

            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                frame.ax = data[i * mDataEntriesPerSensor + 3];
                frame.ay = data[i * mDataEntriesPerSensor + 4];
                frame.az = data[i * mDataEntriesPerSensor + 5];
                Log.i(TAG, Double.toString(frame.ax) + " " + Double.toString(frame.ay) + " " + Double.toString(frame.az) + " " + Integer.toString(timestamp));
                sendNewData(frame);
            }
            if (sensorType == Sensor.TYPE_GYROSCOPE) {
                frame.gx = data[i * mDataEntriesPerSensor + 3];
                frame.gy = data[i * mDataEntriesPerSensor + 4];
                frame.gz = data[i * mDataEntriesPerSensor + 5];
                sendNewData(frame);
            }
            if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                frame.mx = data[i * mDataEntriesPerSensor + 3];
                frame.my = data[i * mDataEntriesPerSensor + 4];
                frame.mz = data[i * mDataEntriesPerSensor + 5];
                sendNewData(frame);
            }


        }
    }

}
