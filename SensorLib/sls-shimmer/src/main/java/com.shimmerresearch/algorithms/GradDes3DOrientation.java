package com.shimmerresearch.algorithms;
/*
 * Madgwick, Sebastian OH, Andrew JL Harrison, and Ravi Vaidyanathan. "Estimation of imu and marg orientation using a gradient descent algorithm." Rehabilitation Robotics (ICORR), 2011 IEEE International Conference on. IEEE, 2011.
 *
 * 3D orientation code modified from https://code.google.com/p/labview-quaternion-ahrs/ which is licensed under GNU_Lesser_GPL
 */


public class GradDes3DOrientation {

    double mBeta = 1;
    double mSamplingPeriod = 1;
    double q1, q2, q3, q4;

    public GradDes3DOrientation(double beta, double samplingPeriod, double q1, double q2, double q3, double q4) {
        mBeta = beta;
        this.q1 = q1;
        this.q2 = q2;
        this.q3 = q3;
        this.q4 = q4;
        mSamplingPeriod = samplingPeriod;
    }


    public Quaternion update(double ax, double ay, double az, double gx, double gy, double gz, double mx, double my, double mz) {


        double norm;
        double hx, hy, _2bx, _2bz;
        double s1, s2, s3, s4;
        double qDot1, qDot2, qDot3, qDot4;

        double _4bx, _4bz;
        double q1q1, q1q2, q1q3, q1q4, q2q2, q2q3, q2q4, q3q3, q3q4, q4q4, _2q1q3, _2q3q4;

        _2q1q3 = 2.0 * q1 * q3;
        _2q3q4 = 2.0 * q3 * q4;
        q1q1 = q1 * q1;
        q1q2 = q1 * q2;
        q1q3 = q1 * q3;
        q1q4 = q1 * q4;
        q2q2 = q2 * q2;
        q2q3 = q2 * q3;
        q2q4 = q2 * q4;
        q3q3 = q3 * q3;
        q3q4 = q3 * q4;
        q4q4 = q4 * q4;

        // Normalise accelerometer measurement
        norm = Math.sqrt(ax * ax + ay * ay + az * az);
        if (norm > 0.0) {
            norm = 1.0 / norm;
            ax *= norm;
            ay *= norm;
            az *= norm;
        } else {

        }

        // Normalise magnetometer measurement
        norm = Math.sqrt(mx * mx + my * my + mz * mz);
        if (norm > 0.0) {
            norm = 1.0 / norm;
            mx *= norm;
            my *= norm;
            mz *= norm;
        } else {

        }


        hx = mx * q1q1 - (2 * q1 * my) * q4 + (2 * q1 * mz) * q3 + mx * q2q2 + (2.0 * q2) * my * q3 + (2.0 * q2) * mz * q4 - mx * q3q3 - mx * q4q4;
        hy = (2 * q1 * mx) * q4 + my * q1q1 - (2 * q1 * mz) * q2 + (2 * q2 * mx) * q3 - my * q2q2 + my * q3q3 + (2.0 * q3) * mz * q4 - my * q4q4;
        _2bx = Math.sqrt(hx * hx + hy * hy);
        _2bz = -(2 * q1 * mx) * q3 + (2 * q1 * my) * q2 + mz * q1q1 + (2 * q2 * mx) * q4 - mz * q2q2 + (2.0 * q3) * my * q4 - mz * q3q3 + mz * q4q4;
        _4bx = 2 * _2bx;
        _4bz = 2 * _2bz;

        // Corrective step
        s1 = -(2.0 * q3) * (2.0 * q2q4 - _2q1q3 - ax) + (2.0 * q2) * (2.0 * q1q2 + _2q3q4 - ay) - _2bz * q3 * (_2bx * (0.5 - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (-_2bx * q4 + _2bz * q2) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + _2bx * q3 * (_2bx * (q1q3 + q2q4) + _2bz * (0.5 - q2q2 - q3q3) - mz);
        s2 = (2.0 * q4) * (2.0 * q2q4 - _2q1q3 - ax) + (2.0 * q1) * (2.0 * q1q2 + _2q3q4 - ay) - 4.0 * q2 * (1.0 - 2.0 * q2q2 - 2.0 * q3q3 - az) + _2bz * q4 * (_2bx * (0.5 - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (_2bx * q3 + _2bz * q1) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + (_2bx * q4 - _4bz * q2) * (_2bx * (q1q3 + q2q4) + _2bz * (0.5 - q2q2 - q3q3) - mz);
        s3 = -(2.0 * q1) * (2.0 * q2q4 - _2q1q3 - ax) + (2.0 * q4) * (2.0 * q1q2 + _2q3q4 - ay) - 4.0 * q3 * (1.0 - 2.0 * q2q2 - 2.0 * q3q3 - az) + (-_4bx * q3 - _2bz * q1) * (_2bx * (0.5 - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (_2bx * q2 + _2bz * q4) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + (_2bx * q1 - _4bz * q3) * (_2bx * (q1q3 + q2q4) + _2bz * (0.5 - q2q2 - q3q3) - mz);
        s4 = (2.0 * q2) * (2.0 * q2q4 - _2q1q3 - ax) + (2.0 * q3) * (2.0 * q1q2 + _2q3q4 - ay) + (-_4bx * q4 + _2bz * q2) * (_2bx * (0.5 - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mx) + (-_2bx * q1 + _2bz * q3) * (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) - my) + _2bx * q2 * (_2bx * (q1q3 + q2q4) + _2bz * (0.5 - q2q2 - q3q3) - mz);

        norm = 1.0 / Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4);    // normalise
        s1 *= norm;
        s2 *= norm;
        s3 *= norm;
        s4 *= norm;

        // Compute rate of change of quaternion
        qDot1 = 0.5 * (-q2 * gx - q3 * gy - q4 * gz) - mBeta * s1;
        qDot2 = 0.5 * (q1 * gx + q3 * gz - q4 * gy) - mBeta * s2;
        qDot3 = 0.5 * (q1 * gy - q2 * gz + q4 * gx) - mBeta * s3;
        qDot4 = 0.5 * (q1 * gz + q2 * gy - q3 * gx) - mBeta * s4;

        // Integrate to yield quaternion
        q1 += qDot1 * mSamplingPeriod;
        q2 += qDot2 * mSamplingPeriod;
        q3 += qDot3 * mSamplingPeriod;
        q4 += qDot4 * mSamplingPeriod;
        norm = 1.0 / Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);    // normalise quaternion

        q1 = q1 * norm;
        q2 = q2 * norm;
        q3 = q3 * norm;
        q4 = q4 * norm;


        return new Quaternion(q1, q2, q3, q4);
    }


    public class Quaternion {

        public double q1, q2, q3, q4;

        public Quaternion(double q1, double q2, double q3, double q4) {
            this.q1 = q1;
            this.q2 = q2;
            this.q3 = q3;
            this.q4 = q4;
        }
    }


}
