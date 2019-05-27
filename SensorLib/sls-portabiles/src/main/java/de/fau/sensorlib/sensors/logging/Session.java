/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.bluetooth.BluetoothGattCharacteristic;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.fau.sensorlib.sensors.AbstractNilsPodSensor;
import de.fau.sensorlib.sensors.enums.NilsPodTerminationSource;

public class Session {

    private static final String TAG = Session.class.getSimpleName();

    /**
     * Flash page size in Byte
     */
    private static final int PAGE_SIZE = 2048;

    private int mSessionId;

    private int mSessionSize;

    private long mDuration;

    private int mStartPage;

    private double mSamplingRate;
    private NilsPodTerminationSource mTerminationSource;

    private Date mStartTime;
    private Date mStopTime;

    private SimpleDateFormat mStartTimeFormat = new SimpleDateFormat("EEE, dd.MM.yyyy HH:mm", Locale.getDefault());
    private SimpleDateFormat mSessionTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());


    public Session(byte[] sessionPacket) {
        int offset = 0;
        BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(null, 0, 0);
        chara.setValue(sessionPacket);

        // Bytes 0-3
        mStartPage = chara.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        offset += 4;

        // Bytes 4-7
        int tmpTime = chara.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        mStartTime = new Date(((long) tmpTime) * 1000);
        offset += 4;

        // Bytes 8-11
        tmpTime = chara.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        offset += 4;
        mStopTime = new Date(((long) tmpTime) * 1000);

        // Bytes 12-15
        mSessionSize = chara.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
        offset += 4;

        mSamplingRate = AbstractNilsPodSensor.inferSamplingRate(sessionPacket[offset++]);
        mTerminationSource = NilsPodTerminationSource.inferTerminationSource(sessionPacket[offset]);

        mDuration = mStopTime.getTime() - mStartTime.getTime();
    }

    public void setSessionId(int sessionId) {
        mSessionId = sessionId;
    }

    public int getSessionId() {
        return mSessionId;
    }

    public Date getStartDate() {
        return mStartTime;
    }

    public String getSessionStartString() {
        return mSessionTimeFormat.format(getStartDate());
    }

    public String getStartTime() {
        return mStartTimeFormat.format(getStartDate());
    }

    public String getDurationString() {
        long hours = TimeUnit.MILLISECONDS.toHours(mDuration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(mDuration) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(mDuration) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public NilsPodTerminationSource getTerminationSource() {
        return mTerminationSource;
    }

    public double getSamplingRate() {
        return mSamplingRate;
    }

    public int getSessionSize() {
        return mSessionSize;
    }

    public static double toKiloByte(double valueByte) {
        return valueByte / 1024.0;
    }

    @Override
    public String toString() {
        return "<Session #" + getSessionId() + ">: " + getStartTime() + " [" + getDurationString() + "]";
    }

    public String toDebugString() {
        return "<Session #" + getSessionId() + "> [" + mStartPage + "-" + (mStartPage + (mSessionSize / PAGE_SIZE)) + "] @ " +
                getStartTime() + " | fs=" + getSamplingRate() + " Hz, duration: " + getDurationString() +
                " (" + getSessionSize() + " Byte)";
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Session) && (getSessionId() == ((Session) obj).getSessionId());
    }
}
