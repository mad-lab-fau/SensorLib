/**
 * Copyright (C) 2016 Digital Sports Group, Friedrich-Alexander University Erlangen-Nuremberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.SystemClock;

import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Blood pressure measurement from the BT LE standard.
 */
public class BleBloodPressureMeasurement extends SensorDataFrame implements BloodPressureDataFrame {

    public static final int UNIT_MMHG = 0;
    public static final int UNIT_KPA = 1;

    private byte mFlags;
    private double mSystolicPressure;
    private double mDiastolicPressure;
    private double mMeanArterialPressure;
    private double mPulseRate;
    private int mMeasurementStatus;


    public BleBloodPressureMeasurement(BluetoothGattCharacteristic c, AbstractSensor sensor) {
        super(sensor, SystemClock.elapsedRealtime());
        mFlags = c.getValue()[0];
        int offset = 1;

        mSystolicPressure = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
        offset += 2;
        mDiastolicPressure = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
        offset += 2;
        mMeanArterialPressure = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);

        // skip time bits
        offset += 7;

        if ((mFlags & (0x01 << 2)) != 0) {
            mPulseRate = c.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, offset);
        }
        offset += 2;
        mMeasurementStatus = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
    }

    public int getUnit() {
        return ((mFlags & 0x01) == 0) ? UNIT_MMHG : UNIT_KPA;
    }

    @Override
    public double getSystolicPressure() {
        return mSystolicPressure;
    }

    @Override
    public double getDiastolicPressure() {
        return mDiastolicPressure;
    }

    @Override
    public double getMeanArterialPressure() {
        return mMeanArterialPressure;
    }

    public double getPulseRate() {
        return mPulseRate;
    }
}
