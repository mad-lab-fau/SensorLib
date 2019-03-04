/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Session {

    private static final String TAG = Session.class.getSimpleName();

    /**
     * Flash page size in Byte
     */
    private static final int PAGE_SIZE = 2048;

    private int mSessionNumber;

    private int mSessionSize;

    private long mDuration;

    private int mStartPage;
    private int mEndPage;

    private double mSamplingRate;
    private boolean mVoltageTerminated;

    private Date mStartTime;
    private int mSampleSize;

    private SimpleDateFormat mStartTimeFormat = new SimpleDateFormat("EEE, dd.MM.yyyy HH:mm", Locale.getDefault());


    public Session(byte[] sessionPacket) {
        int offset = 0;
        mStartPage = ((sessionPacket[offset++] & 0xFF) << 16) | ((sessionPacket[offset++] & 0xFF) << 8) | (sessionPacket[offset++] & 0xFF);
        mEndPage = ((sessionPacket[offset++] & 0xFF) << 16) | ((sessionPacket[offset++] & 0xFF) << 8) | (sessionPacket[offset++] & 0xFF);

        mSamplingRate = sessionPacket[offset] & 0x7F;
        mVoltageTerminated = (sessionPacket[offset++] & 0x80) != 0;

        int tmpTime = ((sessionPacket[offset++] & 0xFF) << 24) | ((sessionPacket[offset++] & 0xFF) << 16) | ((sessionPacket[offset++] & 0xFF) << 8) | (sessionPacket[offset++] & 0xFF);
        mStartTime = new Date(((long) tmpTime) * 1000);

        mSampleSize = (sessionPacket[offset] & 0xFF);

        mSessionSize = (mEndPage - mStartPage + 1) * PAGE_SIZE;
        // TODO check for bug
        mDuration = (long) (((double) mSessionSize) / mSampleSize / 102.4);
    }

    public void setSessionNumber(int sessionNumber) {
        mSessionNumber = sessionNumber;
    }

    public int getSessionNumber() {
        return mSessionNumber;
    }

    public Date getStartDate() {
        return mStartTime;
    }

    public String getStartTime() {
        // TODO check for bug
        return mStartTimeFormat.format(getStartDate());
    }

    public String getDurationString() {
        long hours = TimeUnit.SECONDS.toHours(mDuration);
        long minutes = TimeUnit.SECONDS.toMinutes(mDuration) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.SECONDS.toSeconds(mDuration) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    public int getSampleSize() {
        return mSampleSize;
    }

    public boolean wasSessionVoltageTerminated() {
        return mVoltageTerminated;
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
        return "<Session #" + mSessionNumber + "> [" + mStartPage + "-" + mEndPage + "] @ " +
                getStartTime() + " | fs=" + mSamplingRate + " Hz, duration: " + getDurationString() +
                " (" + mSessionSize + " Byte)";
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Session) && (getSessionNumber() == ((Session) obj).getSessionNumber());
    }
}
