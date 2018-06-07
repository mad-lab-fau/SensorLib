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
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.Shimmer;

import java.util.Collection;

import de.fau.sensorlib.BleSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.EcgDataFrame;
import de.fau.sensorlib.dataframe.EmgDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.SensorState;

/**
 * Implementation of the SHIMMER BT sensors.
 */
public class ShimmerSensor extends AbstractSensor {
    Shimmer shimmer;
    int accelRange = 0;
    CalibratedTimestamp calTimestamp;
    ShimmerMessageHandler mShimmerHandler;

    public static class ShimmerDataFrame extends SensorDataFrame implements EcgDataFrame, GyroDataFrame, AccelDataFrame, EmgDataFrame {
        public String name;
        //public int 		id;
        double ax;
        double ay;
        double az;
        double gx;
        double gy;
        double gz;
        double ecg;
        double ecgLA;
        double ecgRA;
        boolean isTwoChannelEcg;
        double emg;
        char label;

        public ShimmerDataFrame(AbstractSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getEcgSample() {
            if (isTwoChannelEcg) {
                return ecgRA;
            }
            return ecg;
        }

        @Override
        public double getSecondaryEcgSample() {
            if (isTwoChannelEcg) {
                return ecgLA;
            }
            return 0;
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
        public double getEmgSample() {
            return emg;
        }
    }

    private class ShimmerMessageHandler extends Handler {

        /////////////////////////////////////////////////
        // Do not perform expensive computations here! //
        // Do not modify the GUI from here!			   //
        /////////////////////////////////////////////////
        @Override
        public void handleMessage(Message msg) {
            try {
                //local data structure

                //handlers have a what identifier which is used to identify the type of msg
                switch (msg.what) {
                    //within each message an object can be include, object clusters are used to represent the data structure of the SHIMMER device
                    case Shimmer.MESSAGE_READ:
                        ShimmerDataFrame tempData = parseDataMessage(msg);

                        if (tempData != null) {
                            sendNewData(tempData);
                        }
                        break;
                    case Shimmer.MESSAGE_TOAST:
                        String notification = msg.getData().getString(Shimmer.TOAST);
                        //sendNotification(notification);
                        if (notification != null && notification.equals("Device connection was lost")) {
                            sendConnectionLost();
                        }
                        Log.i("toast", notification);
                        break;
                    case Shimmer.MESSAGE_ACK_RECEIVED:
                        Log.i("SensorDeviceManager", "SHIMMER ack message received.");
                        break;
                    case Shimmer.MESSAGE_DEVICE_NAME:
                        sendNotification("Device name received.");
                        Log.i("SensorDeviceManager", "SHIMMER device name received.");
                        break;
                    case Shimmer.MESSAGE_INQUIRY_RESPONSE:
                        Log.i("SensorDeviceManager", "SHIMMER response inquiried.");
                        break;
                    case Shimmer.MESSAGE_SAMPLING_RATE_RECEIVED:
                        Log.i("SensorDeviceManager", "SHIMMER sampling rate received: " + msg + "  " + msg.arg1 + "  " + msg.getData());
                        break;
                    case Shimmer.MESSAGE_STATE_CHANGE:
                        Log.i("SensorDeviceManager", "SHIMMER state changed -> " + msg.arg1);
                        //differentiate between state changes
                        switch (msg.arg1) {
                            case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                                //////////////////////////////////////////////////////////////////////
                                // Do not perform expensive computations here! 						//
                                // If one these states get lost, the SHIMMER can not work properly! //
                                //////////////////////////////////////////////////////////////////////
                                Log.i("SensorDeviceManager", "SHIMMER state fully initialized.");
                                //connect shimmer consecutively

                                setSamplingRate(shimmer.getSamplingRate());
                                sendNotification("Shimmer sampling rate: " + getSamplingRate());

                                Log.i("SensorDeviceManager", "SHIMMER connected.");
                                //check if SHIMMER are connected and not streaming
                                /*if ((shimmer.getShimmerState() == Shimmer.STATE_CONNECTED) && (!shimmer.getStreamingStatus())) {
                                    //mSensorConnectedFlag = true;
                                }*/

                                if (shimmer.getShimmerState() != Shimmer.STATE_CONNECTED) {
                                    //connect the unconnected SHIMMER
                                    connect();
                                    Log.i("SensorDeviceManager", "Connect SHIMMER " + shimmer.getDeviceName() + " .");
                                } else {
                                    startStreaming();
                                }
                                break;
                            case Shimmer.STATE_CONNECTING:
                                Log.i("SensorDeviceManager", "SHIMMER state is connecting.");
                                sendConnecting();
                                break;
                            case Shimmer.STATE_NONE:
                                Log.i("SensorDeviceManager", "SHIMMER state is none.");
                                break;
                            case Shimmer.STATE_CONNECTED:
                                Log.i("SensorDeviceManager", "SHIMMER state is connected.");
                                sendNotification("Shimmer " + getDeviceName() + " is connected.");
                                sendConnected();
                                break;
                            case Shimmer.MSG_STATE_STREAMING:
                                Log.i("SensorDeviceManager", "SHIMMER state is started streaming.");
                                sendStartStreaming();
                                break;
                            case Shimmer.MSG_STATE_STOP_STREAMING:
                                Log.i("SensorDeviceManager", "SHIMMER state is stopped streaming.");
                                sendStopStreaming();
                                break;
                        }
                        //from "case Shimmer.MESSAGE_STATE_CHANGE"
                        break;
                    case Shimmer.MESSAGE_WRITE:
                        Log.i("SensorDeviceManager", "SHIMMER write message received.");
                        break;
                    case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
                        Log.i("SensorDeviceManager", "SHIMMER stop streaming complete.");
                        break;
                    case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                        Log.i("SensorDeviceManager", "SHIMMER packet lost.");
                        break;
                    default:
                        Log.e("SensorDeviceManager", "Unknown SHIMMER message received.");
                        break;
                }
            } catch (Exception e) {
                //some unknown error occurred
                Log.e("SensorDeviceManager", "An error occured on SHIMMER sensor data processing!");
                e.printStackTrace();
            }
        }
    }

    public ShimmerSensor(Context context, String macAddress, SensorDataProcessor dataHandler) throws Exception {
        this(context, BleSensorManager.findBtDevice(macAddress), dataHandler);
    }

    public ShimmerSensor(Context context, @NonNull BluetoothDevice btDevice, SensorDataProcessor dataHandler) {
        super(context, btDevice.getName(), btDevice.getAddress(), dataHandler);
        mShimmerHandler = new ShimmerMessageHandler();
        sendSensorCreated();
    }

    /*public ShimmerSensor( Context context, SensorDataProcessor handler, String myName, String mDeviceAddress, double mSamplingRate, int accelRange, int gsrRange, int setEnabledSensors, boolean continousSync )
    {
        super( myName, mDeviceAddress, mSamplingRate );
        mExternalHandler = handler;
        mInternalHandler = new ShimmerMessageHandler();
        shimmer = new Shimmer( context, mInternalHandler, myName, mSamplingRate, accelRange, gsrRange, setEnabledSensors, continousSync );
    }*/

    public int getShimmerState() {
        return shimmer.getShimmerState();
    }

    public boolean getStreamingStatus() {
        return shimmer.getStreamingStatus();
    }

    public void setAccelRange(int accelRange) {
        // TODO: This should also be realized via e.g. a SupportsAccelerometer interface that is implemented by this Sensor class
        this.accelRange = accelRange;
    }

    /**
     * Maps the SensorLib hardware sensors to the Shimmer driver sensors.
     *
     * @return bitmask containing selected sensors.
     */
    private int getShimmerSelectedSensorsInt() {
        int sensors = 0;
        if (mSelectedHwSensors.contains(HardwareSensor.ACCELEROMETER))
            sensors |= Shimmer.SENSOR_ACCEL;
        if (mSelectedHwSensors.contains(HardwareSensor.ECG))
            sensors |= Shimmer.SENSOR_ECG;
        if (mSelectedHwSensors.contains(HardwareSensor.EMG))
            sensors |= Shimmer.SENSOR_EMG;
        if (mSelectedHwSensors.contains(HardwareSensor.GYROSCOPE))
            sensors |= Shimmer.SENSOR_GYRO;
        if (mSelectedHwSensors.contains(HardwareSensor.MAGNETOMETER))
            sensors |= Shimmer.SENSOR_MAG;
        return sensors;
    }

    @Override
    public boolean connect() throws Exception {
        super.connect();
        // check requested sensors
        if (mSelectedHwSensors.isEmpty())
            throw new SensorException(SensorException.SensorExceptionType.noSensorsSelected);

        calTimestamp = new CalibratedTimestamp(mSamplingRate);
        if (shimmer == null) {
            if (mShimmerHandler == null) {
                mShimmerHandler = new ShimmerMessageHandler();
            }
            shimmer = new Shimmer(mContext, mShimmerHandler, mName, mSamplingRate, accelRange, 4, getShimmerSelectedSensorsInt(), false);
        }
        shimmer.connect(mDeviceAddress, "default");
        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        if (shimmer.getStreamingStatus())
            shimmer.stopStreaming();
        shimmer.stop();
        sendDisconnected();
    }

    @Override
    public void stopStreaming() {
        shimmer.stopStreaming();
        setState(SensorState.CONNECTED);
    }

    @Override
    public void startStreaming() {
        shimmer.startStreaming();
        //setState(SensorState.STREAMING);
    }

    @Override
    public String getDeviceName() {
        if (shimmer == null)
            return mName;
        return shimmer.getDeviceName();
    }

    private ShimmerDataFrame parseDataMessage(Message msg) {
        if (msg.obj instanceof ObjectCluster) {
            ShimmerDataFrame df = null;
            ObjectCluster objectCluster = (ObjectCluster) msg.obj;
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> timestamp = objectCluster.mPropertyCluster.get("Timestamp");
            if (timestamp.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(timestamp, "RAW");
                df = new ShimmerDataFrame(this, calTimestamp.calibrateTimestamp(formatCluster.mData));
            }
            if (df == null)
                return null;
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> gyroXFormats = objectCluster.mPropertyCluster.get("Gyroscope X");
            if (gyroXFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(gyroXFormats, "CAL");
                df.gx = formatCluster.mData;
            }
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> gyroYFormats = objectCluster.mPropertyCluster.get("Gyroscope Y");
            if (gyroYFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(gyroYFormats, "CAL");
                df.gy = formatCluster.mData;
            }
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> gyroZFormats = objectCluster.mPropertyCluster.get("Gyroscope Z");
            if (gyroZFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(gyroZFormats, "CAL");
                df.gz = formatCluster.mData;
            }
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> accelXFormats = objectCluster.mPropertyCluster.get("Accelerometer X");
            if (accelXFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(accelXFormats, "CAL");
                df.ax = formatCluster.mData;
            }
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> accelYFormats = objectCluster.mPropertyCluster.get("Accelerometer Y");
            if (accelYFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(accelYFormats, "CAL");
                df.ay = formatCluster.mData;
            }
            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> accelZFormats = objectCluster.mPropertyCluster.get("Accelerometer Z");
            if (accelZFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(accelZFormats, "CAL");
                df.az = formatCluster.mData;
            }

            // Check for the old "ECG" channel, or try the new two separate channels
            Collection<FormatCluster> ecgFormats = objectCluster.mPropertyCluster.get("ECG");
            if (ecgFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(ecgFormats, "CAL");
                df.ecg = formatCluster.mData;
            } else {
                // Try the "new" two separate channels "LA" and "RA"
                ecgFormats = objectCluster.mPropertyCluster.get("ECG LA-LL");
                if (ecgFormats.size() != 0) {
                    //retrieve the calibrated data
                    FormatCluster formatCluster = ObjectCluster.returnFormatCluster(ecgFormats, "CAL");
                    df.ecgLA = formatCluster.mData;
                    // set ecg as ecgLA for backward compatibility
                    df.ecg = df.ecgLA;
                    df.isTwoChannelEcg = true;
                }

                ecgFormats = objectCluster.mPropertyCluster.get("ECG RA-LL");
                if (ecgFormats.size() != 0) {
                    //retrieve the calibrated data
                    FormatCluster formatCluster = ObjectCluster.returnFormatCluster(ecgFormats, "CAL");
                    df.ecgRA = formatCluster.mData;
                    // set ecg as ecgRA for backward compatibility, this is checked/done after checking for LA, so the RA value would overwrite the LA value in the "ecg" variable, since we assume that the RA-LL value (Einthoven Lead II) is more likely to be expected in an "ecg" variable.
                    df.ecg = df.ecgRA;
                } else {
                    df.isTwoChannelEcg = false;
                }
            }

            //first retrieve all the possible formats for the current sensor device
            Collection<FormatCluster> emgFormats = objectCluster.mPropertyCluster.get("EMG");
            if (emgFormats.size() != 0) {
                //retrieve the calibrated data
                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(emgFormats, "CAL");
                df.emg = formatCluster.mData;
            }
            //save name of the SHIMMER which sends data
            df.name = objectCluster.mMyName;

            return df;
        } else {
            Log.e("ShimmerSensor", "Object of message is not an ObjectCluster instance.");
        }

        return null;
    }

    @Override
    public boolean requestSamplingRateChange(double toSamplingRate) {
        // if we are streaming we can't change the sampling rate
        if (getState().ordinal() >= SensorState.STREAMING.ordinal())
            return false;
        // if we are connected we can immediately write/request the new sampling rate
        if (getState() == SensorState.CONNECTED)
            shimmer.writeSamplingRate(toSamplingRate);
        else
            // if we aren't even connected we just change our internal value
            setSamplingRate(toSamplingRate);
        return true;
    }
}
