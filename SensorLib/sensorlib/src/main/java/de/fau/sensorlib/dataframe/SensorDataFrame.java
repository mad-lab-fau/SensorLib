/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

import android.content.Intent;
import android.support.annotation.CallSuper;

import de.fau.sensorlib.DsSensor;

/**
 * Base class for all data frames coming from a sensor.
 *
 * @author gradl
 */
public class SensorDataFrame {
    public static final String EXTRA_TIMESTAMP = "tstamp";
    /**
     * The sensor on which this data frame was generated.
     */
    protected DsSensor originatingSensor;

    /**
     * Timestamp in milliseconds when this data frame was generated on the sensor.
     */
    protected double timestamp;

    /**
     * Creates a sensor data frame.
     *
     * @param fromSensor the sensor from which this data frame originated.
     * @param timestamp  the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public SensorDataFrame(DsSensor fromSensor, double timestamp) {
        originatingSensor = fromSensor;
        this.timestamp = timestamp;
    }

    /**
     * @return reference to the Sensor which generated this data frame.
     */
    public DsSensor getOriginatingSensor() {
        return originatingSensor;
    }

    /**
     * @return the timestamp in milliseconds when this data frame was generated on the sensor.
     */
    public double getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("DF::");
        str.append(originatingSensor.getClass().getSimpleName());
        str.append("@T").append(getTimestamp());
        if (this instanceof AccelDataFrame) {
            AccelDataFrame adf = (AccelDataFrame) this;
            str.append("Accel(").append(
                    adf.getAccelX()).append(";").append(
                    adf.getAccelY()).append(";").append(
                    adf.getAccelZ()).append(") ");
        }
        if (this instanceof GyroDataFrame) {
            GyroDataFrame adf = (GyroDataFrame) this;
            str.append("Gyro(").append(
                    adf.getGyroX()).append(";").append(
                    adf.getGyroY()).append(";").append(
                    adf.getGyroZ()).append(") ");
        }
        if (this instanceof MagDataFrame) {
            MagDataFrame adf = (MagDataFrame) this;
            str.append("Mag(").append(
                    adf.getMagX()).append(";").append(
                    adf.getMagY()).append(";").append(
                    adf.getMagZ()).append(") ");
        }
        if (this instanceof AmbientDataFrame) {
            AmbientDataFrame adf = (AmbientDataFrame) this;
            str.append("Ambient(").append(
                    adf.getHumidity()).append(";").append(
                    adf.getLight()).append(";").append(
                    adf.getNoise()).append(";").append(
                    adf.getPressure()).append(";").append(
                    adf.getTemperature()).append(") ");
        }
        if (this instanceof EcgDataFrame) {
            EcgDataFrame edf = (EcgDataFrame) this;
            str.append("Ecg(").append(
                    edf.getEcgSample()).append(";").append(
                    edf.getSecondaryEcgSample()).append(") ");
        }
        if (this instanceof HeartRateDataFrame) {
            HeartRateDataFrame df = (HeartRateDataFrame) this;
            str.append("HR(").append(
                    df.getHeartRate()).append(") ");
        }
        if (this instanceof RespirationDataFrame) {
            RespirationDataFrame df = (RespirationDataFrame) this;
            str.append("Resp(").append(
                    df.getRespirationSample()).append(";").append(
                    df.getRespirationRate()).append(") ");
        }

        //str.append(super.toString());

        return str.toString();
    }

    /**
     * Fills the data of this class into the given intent for parcel operations.
     *
     * @param i The intent which is given to this method where the data should be put to via extras.
     */
    @CallSuper
    public void putToIntent(Intent i) {
        i.putExtra(EXTRA_TIMESTAMP, timestamp);

        if (this instanceof EcgDataFrame) {
            EcgDataFrame df = (EcgDataFrame) this;
            i.putExtra(EcgDataFrame.EXTRA_ECG_SAMPLE_1, df.getEcgSample());
            i.putExtra(EcgDataFrame.EXTRA_ECG_SAMPLE_2, df.getSecondaryEcgSample());
        } else if (this instanceof HeartRateDataFrame) {
            HeartRateDataFrame df = (HeartRateDataFrame) this;
            i.putExtra(HeartRateDataFrame.EXTRA_HEART_RATE, df.getHeartRate());
        } else if (this instanceof RespirationDataFrame) {
            RespirationDataFrame df = (RespirationDataFrame) this;
            i.putExtra(RespirationDataFrame.EXTRA_RESP_RATE, df.getRespirationRate());
            i.putExtra(RespirationDataFrame.EXTRA_RESP_SAMPLE, df.getRespirationSample());
        } else if (this instanceof AccelDataFrame) {
            AccelDataFrame df = (AccelDataFrame) this;
            i.putExtra(AccelDataFrame.EXTRA_ACCEL_X, df.getAccelX());
            i.putExtra(AccelDataFrame.EXTRA_ACCEL_Y, df.getAccelY());
            i.putExtra(AccelDataFrame.EXTRA_ACCEL_Z, df.getAccelZ());
        }
    }
}
