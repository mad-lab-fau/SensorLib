/*
 * Copyright (C) 2019 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. If you reuse
 * this code you have to keep or cite this comment.
 */

package de.fau.sensorlib.sensors.logging;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.fau.sensorlib.SensorException;
import de.fau.sensorlib.sensors.AbstractSensor;

public class SessionDownloader {

    private static final String TAG = SessionDownloader.class.getSimpleName();

    private AbstractSensor mSensor;

    private Session mSession;

    private SessionByteWriter mSessionWriter;
    private boolean mCsvExportEnabled = false;
    private SessionCsvConverter mSessionCsvConverter;

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

    public SessionDownloader(AbstractSensor sensor, Session session) throws SensorException {
        mSensor = sensor;
        mSession = session;
        mProgress = 0;
        mSessionSize = session.getSessionSize();
        mStartTime = System.currentTimeMillis();
        mLastTime = System.currentTimeMillis();

        setSessionWriter();
    }

    public Session getSession() {
        return mSession;
    }

    public void setSessionWriter() throws SensorException {
        mSessionWriter = new SessionByteWriter(mSensor, mSession, mSensor.getContext());
    }

    public void setCsvExportEnabled(boolean enable) {
        mCsvExportEnabled = enable;
        if (mCsvExportEnabled) {
            mSessionCsvConverter = new SessionCsvConverter(mSensor, mSession);
        }
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
        if (mCsvExportEnabled) {
            mSessionCsvConverter.nextPacket(values);
        }
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

    public String getEstimatedRemainingTimeString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalTime.ofSecondOfDay(getEstimatedRemainingTimeSeconds()).toString();
        } else {
            long hours = TimeUnit.SECONDS.toHours(getEstimatedRemainingTimeSeconds());
            long minutes = TimeUnit.SECONDS.toMinutes(getEstimatedRemainingTimeSeconds()) - TimeUnit.HOURS.toMinutes(hours);
            long seconds = getEstimatedRemainingTimeSeconds() - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);

            return String.format(Locale.getDefault(), "%02dh:%02dm:%02ds", hours, minutes, seconds);
        }
    }

    public void completeDownload() throws SensorException {
        mProgress = mSession.getSessionSize();

        mSessionWriter.completeWriter();
        mSessionWriter.checkFileSize();

        if (mCsvExportEnabled) {
            mSessionCsvConverter.completeBuilder();
        }
        mElapsedTime = System.currentTimeMillis() - mStartTime;
        mDownloadRate = ((double) mSessionSize) / mElapsedTime;
    }

    public static double toKiloByte(double valueByte) {
        return valueByte / 1024.0;
    }

    @NonNull
    @Override
    public String toString() {
        return "DOWNLOADING <Session #" + mSession.getSessionId() + ">: " + mDf.format(toKiloByte(getProgress())) + "/" +
                mDf.format(toKiloByte(mSessionSize)) + " kByte (" + mDf.format(getProgressPercent()) + "%), download rate: " +
                getDownloadRatekB() + " kByte/s, ETA: " + getEstimatedRemainingTimeSeconds() + " s";
    }
}
