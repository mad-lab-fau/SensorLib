/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.util.Log;

import java.text.DecimalFormat;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionDownloader {

    private static final String TAG = SessionDownloader.class.getSimpleName();

    private AbstractSensor mSensor;

    private Session mSession;

    private SessionByteWriter mSessionWriter;
    private SessionBuilder mSessionBuilder;

    // in Byte
    private int mProgress;
    private int mSessionSize;

    private long mStartTime;

    // in Byte / s
    private double mDownloadRate;

    // in ms
    private long mElapsedTime;
    private long mLastTime;

    // in ms (I hope)
    private long mEstimatedRemainingTime;

    private DecimalFormat mDf = new DecimalFormat("##.##");

    public SessionDownloader(AbstractSensor sensor, Session session) {
        mSensor = sensor;
        mSession = session;
        mProgress = 0;
        mSessionSize = session.getSessionSize();
        mStartTime = System.currentTimeMillis();
        mLastTime = System.currentTimeMillis();
    }

    public Session getSession() {
        return mSession;
    }

    public void setSessionWriter() throws SensorException {
        mSessionWriter = new SessionByteWriter(mSensor, mSession, mSensor.getContext());
    }

    public void setSessionBuilder() {
        mSessionBuilder = new SessionBuilder(mSensor, mSession);
    }

    public void onNewData(byte[] values) {
        mProgress += values.length;
        mElapsedTime = System.currentTimeMillis() - mStartTime;
        mDownloadRate = (((double) mProgress) / mElapsedTime) * 1000;

        if (System.currentTimeMillis() - mLastTime > 1000) {
            mLastTime = System.currentTimeMillis();
            Log.d(TAG, toString());
        }

        int remainingBytes = mSessionSize - mProgress;
        mEstimatedRemainingTime = (long) (remainingBytes / mDownloadRate) * 1000;

        mSessionWriter.writeData(values);
        mSessionBuilder.nextPacket(values);
    }

    public int getProgress() {
        return mProgress;
    }

    public double getProgressPercent() {
        return ((double) mProgress / mSessionSize) * 100.0;
    }

    public double getDownloadRate() {
        return mDownloadRate;
    }

    public String getDownloadRatekB() {
        return mDf.format(toKiloByte(mDownloadRate));
    }

    public long getEstimatedRemainingTime() {
        return mEstimatedRemainingTime;
    }

    public int getEstimatedRemainingTimeSeconds() {
        return (int) (getEstimatedRemainingTime() / 1000);
    }

    public void completeDownload() {
        mProgress = mSession.getSessionSize();
        mSessionWriter.completeWriter();
        mSessionBuilder.completeBuilder();
        mElapsedTime = System.currentTimeMillis() - mStartTime;
        mDownloadRate = ((double) mSessionSize) / mElapsedTime;
    }

    public static double toKiloByte(double valueByte) {
        return valueByte / 1024.0;
    }

    @Override
    public String toString() {
        return "DOWNLOADING <Session #" + mSession.getSessionNumber() + ">: " + mDf.format(toKiloByte(getProgress())) + "/" +
                mDf.format(toKiloByte(mSessionSize)) + " kByte (" + mDf.format(getProgressPercent()) + "%), download rate: " +
                getDownloadRatekB() + " kByte/s, ETA: " + getEstimatedRemainingTimeSeconds() + " s";
    }
}
