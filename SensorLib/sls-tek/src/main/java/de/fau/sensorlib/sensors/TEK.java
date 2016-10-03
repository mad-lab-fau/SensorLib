/**
 * Copyright (C) 2015-2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.UUID;

import de.fau.sensorlib.DsBleSensor;
import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Implementation of the Bosch TEK sensor system.
 */
public class TEK extends DsBleSensor {

    protected static final UUID TEK_CHARACTERISTIC_TOP75_PRIVATE_SERVICE = UUID.fromString("00007500-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_TOP75_HCTM_SERVICE = UUID.fromString("00007700-0000-1000-8000-00805f9b34fb");

    protected static final UUID TEK_CHARACTERISTIC_SINGLE_ACCESS = UUID.fromString("00007501-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_INERTIAL_SENSOR = UUID.fromString("00007502-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_FUSION_SENSOR = UUID.fromString("00007503-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_ENVIRONMENTAL_SENSOR = UUID.fromString("00007504-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_3D_FUSION = UUID.fromString("00007505-0000-1000-8000-00805f9b34fb");
    protected static final UUID TEK_CHARACTERISTIC_HCTM_INERTIAL = UUID.fromString("00007701-0000-1000-8000-00805f9b34fb");

    /**
     * The basic dataframe for the TEK sensor.
     */
    public class TekDataFrame extends SensorDataFrame {

        protected int mCounter;

        public int getCounter() {
            return mCounter;
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public TekDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }
    }

    /**
     * The TEK data frame containing IMU data.
     */
    public class TekImuDataFrame extends TekDataFrame implements AccelDataFrame, GyroDataFrame, MagDataFrame {

        protected double mAx, mAy, mAz;
        protected double mGx, mGy, mGz;
        protected double mMx, mMy, mMz;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public TekImuDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getAccelX() {
            return mAx;
        }

        @Override
        public double getAccelY() {
            return mAy;
        }

        @Override
        public double getAccelZ() {
            return mAz;
        }

        @Override
        public double getGyroX() {
            return mGx;
        }

        @Override
        public double getGyroY() {
            return mGy;
        }

        @Override
        public double getGyroZ() {
            return mGz;
        }

        @Override
        public double getMagX() {
            return mMx;
        }

        @Override
        public double getMagY() {
            return mMy;
        }

        @Override
        public double getMagZ() {
            return mMz;
        }
    }

    /**
     * The TEK data frame containing fusion (Quaternion) data.
     */
    public class TekFusionDataFrame extends TekDataFrame implements AccelDataFrame {
        protected double mQw, mQx, mQy, mQz;
        protected double mAx, mAy, mAz;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public TekFusionDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getAccelX() {
            return mAx;
        }

        @Override
        public double getAccelY() {
            return mAy;
        }

        @Override
        public double getAccelZ() {
            return mAz;
        }

        public double getQuaternionW() {
            return mQw;
        }

        public double getQuaternionX() {
            return mQx;
        }

        public double getQuaternionY() {
            return mQy;
        }

        public double getQuaternionZ() {
            return mQz;
        }
    }

    /**
     * The TEK data frame containing ambient/environment data.
     */
    public class TekAmbientDataFrame extends TekDataFrame implements AmbientDataFrame {

        protected double mTemp, mHumidity, mPressure, mNoise, mLight;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public TekAmbientDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getLight() {
            return mLight;
        }

        @Override
        public double getPressure() {
            return mPressure;
        }

        @Override
        public double getTemperature() {
            return mTemp;
        }

        @Override
        public double getHumidity() {
            return mHumidity;
        }

        @Override
        public double getNoise() {
            return mNoise;
        }
    }


    /**
     * Creates a new instance of the TEK sensor.
     *
     * @param context       the app's context.
     * @param deviceName    the given device name.
     * @param deviceAddress the device's MAC address.
     * @param dataHandler   a SensorDataProcessor instance that receives notifcations of new data, etc.
     */
    public TEK(Context context, String deviceName, String deviceAddress, SensorDataProcessor dataHandler) {
        super(context, deviceName, deviceAddress, dataHandler);
    }

    /**
     * Creates a new instance of the TEK sensor.
     *
     * @param context     the app's context.
     * @param sensor      KnownSensor to connect to, is reported back by BLE scan iteration
     * @param dataHandler a SensorDataProcessor instance that receives notifcations of new data, etc.
     */
    public TEK(Context context, SensorInfo sensor, SensorDataProcessor dataHandler) {
        this(context, sensor.getName(), sensor.getDeviceAddress(), dataHandler);
    }

    /**
     * Creates a new instance of the TEK sensor.
     *
     * @param context       the app's context.
     * @param deviceAddress the device's MAC address.
     * @param dataHandler   a SensorDataProcessor instance that receives notifcations of new data, etc.
     */
    public TEK(Context context, String deviceAddress, SensorDataProcessor dataHandler) {
        this(context, null, deviceAddress, dataHandler);
    }

    @Override
    public void startStreaming() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TEK.super.startStreaming();
            }
        }, 2500);
    }

    @Override
    public void stopStreaming() {

    }

    @Override
    protected boolean onNewCharacteristicValue(BluetoothGattCharacteristic characteristic, boolean isChange) {
        SensorDataFrame df = null;
        byte[] value = characteristic.getValue();

        //Log.d(TAG, "char value: " + characteristic.getUuid().toString() + " :: " + value);

        // Extract the dataframe from the raw byte data
        if (characteristic.getUuid().equals(TEK_CHARACTERISTIC_INERTIAL_SENSOR)) {
            df = extractDataFrameInertial(value);
        }
        if (characteristic.getUuid().equals(TEK_CHARACTERISTIC_FUSION_SENSOR)) {
            df = extractDataFrameFusion(value);
        }
        if (characteristic.getUuid().equals(TEK_CHARACTERISTIC_ENVIRONMENTAL_SENSOR)) {
            df = extractDataFrameEnvironmental(value);
        }
        if (characteristic.getUuid().equals(TEK_CHARACTERISTIC_3D_FUSION)) {
            df = extractDataFrame3dFusion(value);
        }

        // If we could extract a DF, send it out and return from this method.
        if (df != null) {
            sendNewData(df);
            return true;
        }

        // No known dataframe format, call super implementation
        return super.onNewCharacteristicValue(characteristic, isChange);
    }

    @Override
    protected boolean shouldEnableNotification(BluetoothGattCharacteristic c) {
        if (c.getUuid().equals(TEK_CHARACTERISTIC_INERTIAL_SENSOR)) {
            return shouldUseInertialSensor();
        }
        if (c.getUuid().equals(TEK_CHARACTERISTIC_ENVIRONMENTAL_SENSOR)) {
            return shouldUseAmbientSensor();
        }
        if (c.getUuid().equals(TEK_CHARACTERISTIC_FUSION_SENSOR)) {
            return shouldUseInertialSensor();
        }
        /*if (c.getUuid().equals(TEK_CHARACTERISTIC_3D_FUSION)) {
            return true;
        }*/
        /*if (c.getUuid().equals(TEK_CHARACTERISTIC_HCTM_INERTIAL)) {
            return true;
        }*/
        /*if (c.getUuid().equals(TEK_CHARACTERISTIC_SINGLE_ACCESS)) {
            return true;
        }*/
        return super.shouldEnableNotification(c);
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.noneOf(HardwareSensor.class);
    }

    private double convertAccelerometerValue(short value) {
        return value * 0.0000625d;
    }

    private double convertGyroValue(short value) {
        return value / 16.4d;
    }

    private double convertMagValue(short value) {
        return value * 0.0625d;
    }


    private SensorDataFrame extractDataFrameInertial(byte[] data) {
        if (data.length == 0)
            return null;

        TekImuDataFrame df = new TekImuDataFrame(this, System.currentTimeMillis());

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);

        byte in = data[0]; // whatever goes here
        df.mCounter = in & 0xFF;

        df.mAx = convertAccelerometerValue(bb.getShort(2));
        df.mAy = convertAccelerometerValue(bb.getShort(4));
        df.mAz = convertAccelerometerValue(bb.getShort(6));

        df.mGx = convertGyroValue(bb.getShort(8));
        df.mGy = convertGyroValue(bb.getShort(10));
        df.mGz = convertGyroValue(bb.getShort(12));

        df.mMx = convertMagValue(bb.getShort(14));
        df.mMy = convertMagValue(bb.getShort(16));
        df.mMz = convertMagValue(bb.getShort(18));

        return df;
    }

    private SensorDataFrame extractDataFrameFusion(byte[] data) {
        if (data.length == 0)
            return null;

        TekFusionDataFrame df = new TekFusionDataFrame(this, System.currentTimeMillis());

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);

        byte in = data[0]; // whatever goes here
        df.mCounter = in & 0xFF;

        df.mQw = bb.getShort(2);
        df.mQx = bb.getShort(4);
        df.mQy = bb.getShort(6);
        df.mQz = bb.getShort(8);

        df.mAx = convertAccelerometerValue(bb.getShort(11));
        df.mAy = convertAccelerometerValue(bb.getShort(13));
        df.mAz = convertAccelerometerValue(bb.getShort(15));

        return df;
    }

    private SensorDataFrame extractDataFrameEnvironmental(byte[] data) {
        if (data.length == 0)
            return null;

        TekAmbientDataFrame df = new TekAmbientDataFrame(this, System.currentTimeMillis());

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);

        byte in = data[0]; // whatever goes here
        df.mCounter = in & 0xFF;

        df.mTemp = bb.getShort(2) / 100f;
        df.mHumidity = bb.getShort(5) / 100f;
        df.mPressure = bb.getInt(8) / 100f;
        df.mNoise = bb.getShort(13) & 0xffff;
        df.mLight = bb.getInt(16);

        return df;
    }

    private SensorDataFrame extractDataFrame3dFusion(byte[] data) {
        if (data.length == 0)
            return null;

        TekImuDataFrame df = new TekImuDataFrame(this, System.currentTimeMillis());

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.BIG_ENDIAN);

        byte in = data[0]; // whatever goes here
        df.mCounter = in & 0xFF;

        Log.d(TAG, "3dFusion (" + df.mCounter + "): " + data[10] + "; " + data[11] + "; " + data[12] + "; " + data[13]);

        return df;
    }
}
