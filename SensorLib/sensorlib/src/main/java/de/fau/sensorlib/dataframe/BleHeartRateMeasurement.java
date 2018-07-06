/*
 * Copyright (C) 2018 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */
package de.fau.sensorlib.dataframe;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Represents a heart rate measurement specified in the BLE standard.
 */
public class BleHeartRateMeasurement extends SensorDataFrame implements HeartRateDataFrame {
    byte mFlags;
    int mBeatsPerMinute;
    int mEnergyExpendedInJoule;
    boolean mHasSensorContact;
    List<Double> mRrIntervals = new ArrayList<>();

    public BleHeartRateMeasurement(BluetoothGattCharacteristic c, AbstractSensor sensor) {
        super(sensor, SystemClock.elapsedRealtime());

        int offset = 0;

        /*ByteBuffer bb = ByteBuffer.wrap(c.getValue());
        bb.order(ByteOrder.LITTLE_ENDIAN);*/
        mFlags = c.getValue()[0];
        offset++;
        //mFlags = bb.get();

        // read HRM value
        if (isHrm8()) {
            mBeatsPerMinute = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset++);
        } else {
            mBeatsPerMinute = c.getIntValue((BluetoothGattCharacteristic.FORMAT_UINT16), offset);
            offset += 2;
        }

        if (hasSensorContactStatus()) {
            if ((mFlags & (0x01 << 1)) != 0) {
                mHasSensorContact = true;
            }
        }

        if ((mFlags & (0x01 << 3)) != 0) {
            mEnergyExpendedInJoule = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
            offset += 2;
        }

        if ((mFlags & (0x01 << 4)) != 0) {
            //while (bb.hasRemaining()) {
            while (offset <= c.getValue().length - 2) {
                mRrIntervals.add(c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset) / 1024d);
                offset += 2;
                //mRrIntervals.add(DsGattAttributes.getUInt16(bb) / 1024d);
            }
        }
    }

    boolean isHrm8() {
        return (mFlags & 0x01) == 0;
    }

    boolean hasSensorContactStatus() {
        return (mFlags & (0x01 << 2)) != 0;
    }

    @Override
    public double getHeartRate() {
        return mBeatsPerMinute;
    }

    @Override
    public double getInterbeatInterval() {
        if (mRrIntervals.size() > 0) {
            return mRrIntervals.get(mRrIntervals.size() - 1);
        }
        return 0;
    }
}
