/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import de.fau.sensorlib.SensorDataLogger;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.AccelDataFrame;
import de.fau.sensorlib.dataframe.AmbientDataFrame;
import de.fau.sensorlib.dataframe.BarometricPressureDataFrame;
import de.fau.sensorlib.dataframe.GyroDataFrame;
import de.fau.sensorlib.dataframe.HumidityDataFrame;
import de.fau.sensorlib.dataframe.LightDataFrame;
import de.fau.sensorlib.dataframe.MagDataFrame;
import de.fau.sensorlib.dataframe.OrientationDataFrame;
import de.fau.sensorlib.dataframe.RealTimeTimestampDataFrame;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.dataframe.TemperatureDataFrame;
import de.fau.sensorlib.enums.HardwareSensor;
import de.fau.sensorlib.enums.KnownSensor;
import de.fau.sensorlib.enums.SensorMessage;

/**
 * Implementation of the internal/hardware sensors of the Android device.
 */
public class InternalSensor extends AbstractSensor implements SensorEventListener, Loggable {

    public static final SensorInfo ANDROID_DEVICE_SENSORS = new SensorInfo("Internal", "n/a", KnownSensor.INTERNAL);

    private SensorManager mSensorManager;

    /**
     * Flag indicating whether data should be logged
     */
    protected boolean mLoggingEnabled;

    /**
     * Data logger
     */
    protected ArrayList<SensorDataLogger> mDataLogger = new ArrayList<>();

    private ArrayList<Sensor> mSelectedSensors = new ArrayList<>();
    private ArrayList<Integer> mSensorCounter = new ArrayList<>();

    /**
     * Sampling period in microseconds
     */
    private int mSamplingPeriodUs;


    /**
     * Data frame to store accelerometer data received from the Internal Sensor
     */
    public static class InternalAccelDataFrame extends SensorDataFrame implements AccelDataFrame, RealTimeTimestampDataFrame {

        private double[] accel;
        private double realTimeTimestamp;

        public InternalAccelDataFrame(AbstractSensor sensor, double timestamp) {
            this(sensor, timestamp, new double[3]);
        }

        public InternalAccelDataFrame(AbstractSensor sensor, double timestamp, double[] accel) {
            this(sensor, timestamp, 0, accel);
        }

        public InternalAccelDataFrame(AbstractSensor sensor, double timestamp, double realtimeTimestamp, double[] accel) {
            super(sensor, timestamp);
            this.accel = accel;
            this.realTimeTimestamp = realtimeTimestamp;
        }

        @Override
        public double getAccelX() {
            return accel[0];
        }

        @Override
        public double getAccelY() {
            return accel[1];
        }

        @Override
        public double getAccelZ() {
            return accel[2];
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", accel: " + Arrays.toString(accel);
        }
    }

    /**
     * Data frame to store gyroscope data received from the Internal Sensor
     */
    public static class InternalGyroDataFrame extends SensorDataFrame implements GyroDataFrame, RealTimeTimestampDataFrame {

        private double[] gyro;
        private double realTimeTimestamp;

        public InternalGyroDataFrame(AbstractSensor sensor, double timestamp) {
            this(sensor, timestamp, new double[3]);
        }

        public InternalGyroDataFrame(AbstractSensor sensor, double timestamp, double[] gyro) {
            this(sensor, timestamp, 0, gyro);
        }

        public InternalGyroDataFrame(AbstractSensor sensor, double timestamp, double realTimeTimestamp, double[] gyro) {
            super(sensor, timestamp);
            this.gyro = gyro;
            this.realTimeTimestamp = realTimeTimestamp;
        }

        @Override
        public double getGyroX() {
            return gyro[0];
        }

        @Override
        public double getGyroY() {
            return gyro[1];
        }

        @Override
        public double getGyroZ() {
            return gyro[2];
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", gyro: " + Arrays.toString(gyro);
        }
    }

    /**
     * Data frame to store magnetometer data received from the Internal Sensor
     */
    public static class InternalMagDataFrame extends SensorDataFrame implements MagDataFrame, RealTimeTimestampDataFrame {

        private double[] mag;
        private double realTimeTimestamp;

        public InternalMagDataFrame(AbstractSensor sensor, double timestamp) {
            this(sensor, timestamp, new double[3]);
        }

        public InternalMagDataFrame(AbstractSensor sensor, double timestamp, double[] mag) {
            this(sensor, timestamp, 0, mag);
        }

        public InternalMagDataFrame(AbstractSensor sensor, double timestamp, double realTimeTimestamp, double[] mag) {
            super(sensor, timestamp);
            this.mag = mag;
            this.realTimeTimestamp = realTimeTimestamp;
        }

        @Override
        public double getMagX() {
            return mag[0];
        }

        @Override
        public double getMagY() {
            return mag[1];
        }

        @Override
        public double getMagZ() {
            return mag[2];
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", mag: " + Arrays.toString(mag);
        }
    }

    /**
     * Data frame to store orientation data received from the Internal Sensor
     */
    public static class InternalOrientationDataFrame extends SensorDataFrame implements OrientationDataFrame, RealTimeTimestampDataFrame {

        private double roll, pitch, yaw;
        private double realTimeTimestamp;

        public InternalOrientationDataFrame(AbstractSensor sensor, double timestamp) {
            this(sensor, timestamp, 0, 0, 0);
        }

        public InternalOrientationDataFrame(AbstractSensor sensor, double timestamp, double roll, double pitch, double yaw) {
            this(sensor, timestamp, 0, roll, pitch, yaw);
        }

        public InternalOrientationDataFrame(AbstractSensor sensor, double timestamp, double realTimeTimestamp, double roll, double pitch, double yaw) {
            super(sensor, timestamp);
            this.roll = roll;
            this.pitch = pitch;
            this.yaw = yaw;
            this.realTimeTimestamp = realTimeTimestamp;
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

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", roll: " + roll + ", pitch: " + pitch + ", yaw: " + yaw;
        }
    }

    /**
     * Data frame to store combined ambient data received from the Internal Sensor
     */
    public static class InternalAmbientDataFrame extends SensorDataFrame implements AmbientDataFrame {

        private double light;
        private double baro;
        private double temp;
        private double humidity;

        public InternalAmbientDataFrame(AbstractSensor fromSensor, double timestamp) {
            this(fromSensor, timestamp, 0, 0, 0, 0);
        }

        public InternalAmbientDataFrame(AbstractSensor fromSensor, double timestamp, double light, double baro, double temp, double humidity) {
            super(fromSensor, timestamp);
            this.light = light;
            this.baro = baro;
            this.temp = temp;
            this.humidity = humidity;
        }

        @Override
        public double getLight() {
            return light;
        }

        @Override
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public double getTemperature() {
            return temp;
        }

        @Override
        public double getHumidity() {
            return humidity;
        }

        @Override
        public double getNoise() {
            return 0;
        }
    }

    /**
     * Data frame to store light data received from the Internal Sensor
     */
    public static class InternalLightDataFrame extends SensorDataFrame implements LightDataFrame, RealTimeTimestampDataFrame {

        private double light;
        private double realTimeTimestamp;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public InternalLightDataFrame(AbstractSensor fromSensor, double timestamp) {
            this(fromSensor, timestamp, 0);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param light      light value
         */
        public InternalLightDataFrame(AbstractSensor fromSensor, double timestamp, double light) {
            this(fromSensor, timestamp, 0, light);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param light      light value
         */
        public InternalLightDataFrame(AbstractSensor fromSensor, double timestamp, double realTimeTimestamp, double light) {
            super(fromSensor, timestamp);
            this.light = light;
            this.realTimeTimestamp = realTimeTimestamp;
        }

        @Override
        public double getLight() {
            return light;
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", light: " + light;
        }

    }

    /**
     * Data frame to store barometer data received from the Internal Sensor
     */
    public static class InternalBarometricPressureDataFrame extends SensorDataFrame implements BarometricPressureDataFrame, RealTimeTimestampDataFrame {

        private double baro;
        private double realTimeTimestamp;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public InternalBarometricPressureDataFrame(AbstractSensor fromSensor, double timestamp) {
            this(fromSensor, timestamp, 0);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param baro       barometric pressure value
         */
        public InternalBarometricPressureDataFrame(AbstractSensor fromSensor, double timestamp, double baro) {
            this(fromSensor, timestamp, 0, baro);
        }


        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param baro       barometric pressure value
         */
        public InternalBarometricPressureDataFrame(AbstractSensor fromSensor, double timestamp, double realTimeTimestamp, double baro) {
            super(fromSensor, timestamp);
            this.baro = baro;
            this.realTimeTimestamp = realTimeTimestamp;
        }

        @Override
        public double getBarometricPressure() {
            return baro;
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", baro: " + baro;
        }
    }


    /**
     * Data frame to store ambient temperature data received from the Internal Sensor
     */
    public static class InternalTemperatureDataFrame extends SensorDataFrame implements TemperatureDataFrame, RealTimeTimestampDataFrame {

        private double temp;
        private double realTimeTimestamp;


        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public InternalTemperatureDataFrame(AbstractSensor fromSensor, double timestamp) {
            this(fromSensor, timestamp, 0);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param temp       ambient temperature value
         */
        public InternalTemperatureDataFrame(AbstractSensor fromSensor, double timestamp, double temp) {
            this(fromSensor, timestamp, 0, temp);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param temp       ambient temperature value
         */
        public InternalTemperatureDataFrame(AbstractSensor fromSensor, double timestamp, double realTimeTimestamp, double temp) {
            super(fromSensor, timestamp);
            this.temp = temp;
            this.realTimeTimestamp = temp;
        }


        @Override
        public double getTemperature() {
            return temp;
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", temp: " + temp;
        }

    }


    /**
     * Data frame to store relative humidity data received from the Internal Sensor
     */
    public static class InternalHumidityDataFrame extends SensorDataFrame implements HumidityDataFrame, RealTimeTimestampDataFrame {

        private double humidity;
        private double realTimeTimestamp;

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         */
        public InternalHumidityDataFrame(AbstractSensor fromSensor, double timestamp) {
            this(fromSensor, timestamp, 0);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param humidity   relative humidity value
         */
        public InternalHumidityDataFrame(AbstractSensor fromSensor, double timestamp, double humidity) {
            this(fromSensor, timestamp, 0, humidity);
        }

        /**
         * Creates a sensor data frame.
         *
         * @param fromSensor the sensor from which this data frame originated.
         * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
         * @param humidity   relative humidity value
         */
        public InternalHumidityDataFrame(AbstractSensor fromSensor, double timestamp, double realTimeTimestamp, double humidity) {
            super(fromSensor, timestamp);
            this.humidity = humidity;
            this.realTimeTimestamp = realTimeTimestamp;
        }


        @Override
        public double getHumidity() {
            return humidity;
        }

        @Override
        public double getRealTimeTimestamp() {
            return realTimeTimestamp;
        }

        @Override
        public String toString() {
            return "<" + originatingSensor.getDeviceName() + ">\tctr=" + ((long) getTimestamp()) + ", humidity: " + humidity;
        }
    }


    /**
     * Constructor for standard internal sensor devices.
     *
     * @param dataHandler method to provide unified data handling
     */
    public InternalSensor(Context context, SensorDataProcessor dataHandler) {
        super(context, ANDROID_DEVICE_SENSORS.getName(), ANDROID_DEVICE_SENSORS.getDeviceAddress(), dataHandler, 10);
    }

    @Override
    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return 0;
        }
        int batteryLevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int batteryScale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return (int) (((float) batteryLevel / batteryScale) * 100.0f);
    }

    @Override
    public boolean connect() throws Exception {
        if (!super.connect()) {
            return false;
        }

        // use default sampling rate (100 Hz) if none is given
        if (getSamplingRate() <= 0) {
            setSamplingRate(100);
        }

        mDataLogger = new ArrayList<>();
        mSamplingPeriodUs = 1000000 / (int) getSamplingRate();
        mSensorManager = (SensorManager) super.mContext.getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager == null) {
            throw new SensorException(SensorException.SensorExceptionType.sensorNotResponding);
        }

        for (HardwareSensor hwSensor : getSelectedSensors()) {
            int sensorType = -1;

            switch (hwSensor) {
                case ACCELEROMETER:
                    sensorType = Sensor.TYPE_ACCELEROMETER;
                    break;
                case ORIENTATION:
                    sensorType = Sensor.TYPE_ROTATION_VECTOR;
                    //Sensor sensorMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    // add magnetometer here (not after switch-case-statement),
                    // because for orientation we need input from two sensors
                    /*if (!mSelectedSensors.contains(sensorMag)) {
                        mSelectedSensors.add(sensorMag);
                    }*/
                    break;
                case GYROSCOPE:
                    sensorType = Sensor.TYPE_GYROSCOPE;
                    break;
                case MAGNETOMETER:
                    sensorType = Sensor.TYPE_MAGNETIC_FIELD;
                    break;
                case LIGHT:
                    sensorType = Sensor.TYPE_LIGHT;
                    break;
                case BAROMETER:
                    sensorType = Sensor.TYPE_PRESSURE;
                    break;
                case TEMPERATURE:
                    sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE;
                    break;
                case HUMIDITY:
                    sensorType = Sensor.TYPE_RELATIVE_HUMIDITY;
                    break;
            }

            if (sensorType != -1) {
                Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
                if (sensor == null) {
                    mSelectedHwSensors.remove(hwSensor);
                } else {
                    mSelectedSensors.add(sensor);
                }
            }
        }

        Log.e(TAG, mSelectedHwSensors.toString());
        Log.e(TAG, mSelectedSensors.toString());

        mSensorCounter = new ArrayList<>();
        for (int i = 0; i < mSelectedSensors.size(); i++) {
            mSensorCounter.add(0);
        }

        sendConnected();

        return true;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        mSensorManager = null;
        sendDisconnected();
    }

    @Override
    public void startStreaming() {
        try {
            if (mLoggingEnabled) {
                for (HardwareSensor hwSensor : getSelectedSensors()) {
                    mDataLogger.add(new SensorDataLogger(this, hwSensor, mContext));
                }
            }
        } catch (SensorException e) {
            switch (e.getExceptionType()) {
                case permissionsMissing:
                    Toast.makeText(mContext, "Permissions to write external storage needed!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        //Log.e(TAG, mSelectedSensors.toString());
        for (Sensor sensor : mSelectedSensors) {
            mSensorManager.registerListener(this, sensor, mSamplingPeriodUs);
        }

        sendStartStreaming();
    }

    @Override
    public void stopStreaming() {
        mSensorManager.unregisterListener(this);
        for (SensorDataLogger logger : mDataLogger) {
            logger.completeLogger();
        }
        sendStopStreaming();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorDataFrame df = null;
        int localCounter = mSensorCounter.get(mSelectedSensors.indexOf(event.sensor));
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                df = new InternalAccelDataFrame(this, localCounter, event.timestamp, new double[]{event.values[0], event.values[1], event.values[2]});
                break;
            case Sensor.TYPE_GYROSCOPE:
                df = new InternalGyroDataFrame(this, localCounter, event.timestamp, new double[]{event.values[0], event.values[1], event.values[2]});
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                df = new InternalMagDataFrame(this, localCounter, event.timestamp, new double[]{event.values[0], event.values[1], event.values[2]});
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                float[] R = new float[9];
                SensorManager.getRotationMatrixFromVector(R, event.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                df = new InternalOrientationDataFrame(this, localCounter, event.timestamp, Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]));
                break;
            case Sensor.TYPE_LIGHT:
                df = new InternalLightDataFrame(this, localCounter, event.timestamp, event.values[0]);
                break;
            case Sensor.TYPE_PRESSURE:
                df = new InternalBarometricPressureDataFrame(this, localCounter, event.timestamp, event.values[0]);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                df = new InternalTemperatureDataFrame(this, localCounter, event.timestamp, event.values[0]);
                break;
        }

        if (df == null) {
            return;
        }
        Log.d(TAG, df.toString());
        //Log.d(TAG, "sensor: " + event.sensor.getStringType() + ", timestamp: " + ((long) ((RealTimeTimestampDataFrame) df).getRealTimeTimestamp()));

        sendNewData(df);
        if (mLoggingEnabled) {
            mDataLogger.get(mSelectedSensors.indexOf(event.sensor)).writeData(df);
        }
        mSensorCounter.set(mSelectedSensors.indexOf(event.sensor), ++localCounter);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        sendNotification(SensorMessage.ACCURACY_CHANGED);
        /*Bundle bundle = new Bundle();
        bundle.putInt("accuracy", accuracy);
        sendNotification(bundle);*/
    }


    @Override
    public void enableDataLogger() {
        mLoggingEnabled = true;
    }

    @Override
    public void disableDataLogger() {
        mLoggingEnabled = false;
    }
}
