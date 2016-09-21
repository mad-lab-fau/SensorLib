/**
 * Copyright (C) 2015 Digital Sports Group, Pattern Recognition Lab, Friedrich-Alexander University Erlangen-Nürnberg (FAU).
 * <p>
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package de.fau.sensorlib.sensors;

/**
 * Created by gradl on 07.10.2015.
 */
public class CalibratedTimestamp {
    private double mDeltaTimeStamp;
    private double mNewTimeStamp;
    private double mOldTimeStamp;
    private float mAccTimeStamp;
    private boolean mFirstTimeStamp;
    private double mMaxSamplingRate;

    public CalibratedTimestamp(double samplingRate) {
        mMaxSamplingRate = samplingRate;
        mDeltaTimeStamp = (32768 / mMaxSamplingRate);
        mOldTimeStamp = 0.0;
        mAccTimeStamp = 0;
    }

    public float calibrateTimestamp(double timestamp) {
        //compute time stamp
        if (mFirstTimeStamp) {
            //first time stamp can be every known positive timer counter value
            mAccTimeStamp += (float) (1000.0 / mMaxSamplingRate);
            mOldTimeStamp = timestamp;
            mFirstTimeStamp = false;
        } else {
            if (timestamp > mOldTimeStamp) {
                //normally timer counter value increases
                mAccTimeStamp += (float) ((((timestamp - mOldTimeStamp) / mDeltaTimeStamp) * 1000.0) / mMaxSamplingRate);
                mOldTimeStamp = timestamp;
            } else {
                //handle timer counter overflow
                mAccTimeStamp += (float) (1000 / mMaxSamplingRate);
                mOldTimeStamp = timestamp;
            }
        }
        return mAccTimeStamp;
    }
}
