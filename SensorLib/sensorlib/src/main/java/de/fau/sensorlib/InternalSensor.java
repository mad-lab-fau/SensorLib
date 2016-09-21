/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.EnumSet;

import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.OrientationDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;

/**
 * Implementation of the internal/hardware sensors of the Android device.
 */
public class InternalSensor extends DsSensor implements SensorEventListener {

    private SensorManager mSensorManager;


    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalAccelDataFrame extends SensorDataFrame implements AccelDataFrame {

        double ax, ay, az;

        public InternalAccelDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public InternalAccelDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
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

    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalGyroDataFrame extends SensorDataFrame implements GyroDataFrame {

        public double gx, gy, gz;

        public InternalGyroDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public InternalGyroDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            gx = x;
            gy = y;
            gz = z;
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

    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalMagDataFrame extends SensorDataFrame implements MagDataFrame {

        double mx, my, mz;

        public InternalMagDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public InternalMagDataFrame(DsSensor fromSensor, double timestamp, double x, double y, double z) {
            super(fromSensor, timestamp);
            mx = x;
            my = y;
            mz = z;
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

    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalOrientationDataFrame extends SensorDataFrame implements OrientationDataFrame {

        double roll, pitch, yaw;

        public InternalOrientationDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        public InternalOrientationDataFrame(DsSensor fromSensor, double timestamp, double roll, double pitch, double yaw) {
            super(fromSensor, timestamp);
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
        }

        @Override
        public double getRoll() {
            return roll;
        }

        @Override
        public double getPitch() {
            return pitch;
        }

        @Override
        public double getYaw() {
            return yaw;
        }
    }

    /**
     * Combine all possible single data frames into one sensor specific data frame
     */
    public static class InternalAmbientDataFrame extends SensorDataFrame implements AmbientDataFrame {

        double l, p, t;

        public InternalAmbientDataFrame(DsSensor fromSensor, double timestamp) {
            super(fromSensor, timestamp);
        }

        @Override
        public double getLight() {
            return l;
        }

        @Override
        public double getPressure() {
            return p;
        }

        @Override
        public double getTemperature() {
            return t;
        }

        @Override
        public double getHumidity() {
            return 0;
        }

        @Override
        public double getNoise() {
            return 0;
        }
    }


    /**
     * Constructor for standard internal sensor devices.
     *
     * @param dataHandler method to provide unified data handling
     */
    public InternalSensor(Context context, SensorDataProcessor dataHandler) {
        super(context, "Internal", "n/a", dataHandler);
    }

    @Override
    protected EnumSet<HardwareSensor> providedSensors() {
        return EnumSet.of(
                HardwareSensor.ACCELEROMETER,
                HardwareSensor.GYROSCOPE,
                HardwareSensor.MAGNETOMETER,
                HardwareSensor.ORIENTATION,
                HardwareSensor.LIGHT,
                HardwareSensor.PRESSURE,
                HardwareSensor.TEMPERATURE);
    }

    @Override
    public boolean connect() {
        try {
            super.connect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // use default sampling rate if none is given
        double samplingRateDouble = getSamplingRate();
        if (samplingRateDouble == 0) {
            samplingRateDouble = 100;
        }

        int samplingRate = 1000000 / (int) samplingRateDouble;

        mSensorManager = (SensorManager) super.mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSelectedHwSensors.contains(HardwareSensor.ACCELEROMETER)) {
            Sensor sensorAcc = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, sensorAcc, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.ORIENTATION)) {
            Sensor sensorGrav = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            Sensor sensorMag = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, sensorGrav, samplingRate);
            mSensorManager.registerListener(this, sensorMag, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.GYROSCOPE)) {
            Sensor sensorGyro = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorManager.registerListener(this, sensorGyro, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.MAGNETOMETER)) {
            Sensor sensorMag = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, sensorMag, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.LIGHT)) {
            Sensor sensorLight = this.mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener(this, sensorLight, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.PRESSURE)) {
            Sensor sensorPress = this.mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mSensorManager.registerListener(this, sensorPress, samplingRate);
        }
        if (mSelectedHwSensors.contains(HardwareSensor.TEMPERATURE)) {
            Sensor sensorTemp = this.mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            mSensorManager.registerListener(this, sensorTemp, samplingRate);
        }

        sendConnected();

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        mSensorManager.unregisterListener(this);
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        sendStopStreaming();
    }

    private float[] valuesMagnetic = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorDataFrame frame = null;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            frame = new InternalAccelDataFrame(this, event.timestamp, event.values[0], event.values[1], event.values[2]);
            //Log.i("Internal Accelerometer", Double.toString(frame.ax) + "   " + Double.toString(frame.ay) + "   " + Double.toString(frame.az));
        }
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            float[] I = new float[9];
            float[] R = new float[9];
            SensorManager.getRotationMatrix(R, I, event.values, valuesMagnetic);
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            frame = new InternalOrientationDataFrame(this, event.timestamp, Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]));
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            frame = new InternalGyroDataFrame(this, event.timestamp, event.values[0], event.values[1], event.values[2]);
            //Log.i("Internal Gyroscope", Double.toString(frame.gx) + "   " + Double.toString(frame.gy) + "   " + Double.toString(frame.gz));
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            valuesMagnetic[0] = event.values[0];
            valuesMagnetic[1] = event.values[1];
            valuesMagnetic[2] = event.values[2];
            frame = new InternalMagDataFrame(this, event.timestamp, event.values[0], event.values[1], event.values[2]);
            //Log.i("Internal Magnetometer", Double.toString(frame.mx) + "   " + Double.toString(frame.my) + "   " + Double.toString(frame.mz));
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            frame = new InternalAmbientDataFrame(this, event.timestamp);
            ((InternalAmbientDataFrame) frame).l = event.values[0];
            //Log.i("Internal Lightsensor", Double.toString(frame.l));
        }
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            frame = new InternalAmbientDataFrame(this, event.timestamp);
            ((InternalAmbientDataFrame) frame).p = event.values[0];
            //Log.i("Internal Pressuresensor", Double.toString(frame.p));
        }
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            frame = new InternalAmbientDataFrame(this, event.timestamp);
            ((InternalAmbientDataFrame) frame).t = event.values[0];
            //Log.i("Internal Tempsensor", Double.toString(frame.t));
        }

        sendNewData(frame);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /*Bundle bundle = new Bundle();
        bundle.putInt("accuracy", accuracy);
        sendNotification(bundle);*/
    }
}
