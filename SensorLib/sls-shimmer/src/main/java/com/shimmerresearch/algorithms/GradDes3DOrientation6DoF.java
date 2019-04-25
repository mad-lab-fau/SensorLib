package com.shimmerresearch.algorithms;
/* Rev 0.1
 * 
 * Madgwick, Sebastian OH, Andrew JL Harrison, and Ravi Vaidyanathan. "Estimation of imu and marg orientation using a gradient descent algorithm." Rehabilitation Robotics (ICORR), 2011 IEEE International Conference on. IEEE, 2011.
 *
 * 3D orientation code taken from https://code.google.com/p/labview-quaternion-ahrs/ which is licensed under GNU_Lesser_GPL
 *
 * Copyright (c) 2010, Shimmer Research, Ltd.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Shimmer Research, Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Jong Chern Lim
 * @date   October, 2013
 * 
 * 
 * 
 */



/*
 * Copyright (c) 2010, Shimmer Research, Ltd.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Shimmer Research, Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Jong Chern Lim
 * @date   October, 2013
 */

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4d;
import javax.vecmath.Vector4f;

import com.shimmerresearch.algorithms.GradDes3DOrientation.Quaternion;

public class GradDes3DOrientation6DoF {

	double mBeta = 1;
	double mSamplingPeriod = 1;
	double q0, q1, q2, q3;
	public GradDes3DOrientation6DoF(double beta, double samplingPeriod, double q1, double q2, double q3, double q4)
	{
		mBeta = beta;
		this.q0 = q1;
		this.q1 = q2;
		this.q2 = q3;
		this.q3 = q4;
		mSamplingPeriod = samplingPeriod;
	}



	/**
	 * @param ax Accelerometer X in m/(sec^2)
	 * @param ay Accelerometer Y in m/(sec^2)
	 * @param az Accelerometer Z in m/(sec^2)
	 * @param gx Gyroscope X in rad/sec
	 * @param gy Gyroscope X in rad/sec
	 * @param gz Gyroscope X in rad/sec
	 * @return Calculated Quaternion value
	 */
	public Quaternion update(double ax,double ay,double az,double gx, double gy, double gz)
	{
		double norm;
		double s0, s1, s2, s3;
		double qDot1, qDot2, qDot3, qDot4;
		double _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
		qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
		qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
		qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

		// Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
		if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

			// Normalise accelerometer measurement
			 norm = Math.sqrt(ax * ax + ay * ay + az * az);
			    if (norm > 0.0){
			       norm = 1.0 / norm; 
			       ax *= norm;
			       ay *= norm;
			       az *= norm;
			    }
			    else{
			
			    }

			// Auxiliary variables to avoid repeated arithmetic
			_2q0 = 2.0f * q0;
			_2q1 = 2.0f * q1;
			_2q2 = 2.0f * q2;
			_2q3 = 2.0f * q3;
			_4q0 = 4.0f * q0;
			_4q1 = 4.0f * q1;
			_4q2 = 4.0f * q2;
			_8q1 = 8.0f * q1;
			_8q2 = 8.0f * q2;
			q0q0 = q0 * q0;
			q1q1 = q1 * q1;
			q2q2 = q2 * q2;
			q3q3 = q3 * q3;

			// Gradient decent algorithm corrective step
			s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
			s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * q1 - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
			s2 = 4.0f * q0q0 * q2 + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
			s3 = 4.0f * q1q1 * q3 - _2q1 * ax + 4.0f * q2q2 * q3 - _2q2 * ay;

		    norm = 1.0 / Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s0 * s0);    // normalise
		    s0 *= norm;
		    s1 *= norm;
		    s2 *= norm;
		    s3 *= norm;
			

			// Apply feedback step
			qDot1 -= mBeta * s0;
			qDot2 -= mBeta * s1;
			qDot3 -= mBeta * s2;
			qDot4 -= mBeta * s3;
		}

		
		
		 // Integrate to yield quaternion
        q0 += qDot1 * mSamplingPeriod;
        q1 += qDot2 * mSamplingPeriod;
        q2 += qDot3 * mSamplingPeriod;
        q3 += qDot4 * mSamplingPeriod;
	    norm = 1.0 / Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q0 * q0);    // normalise quaternion
	
	    q1 = q1 * norm;
	    q2 = q2 * norm;
	    q3 = q3 * norm;
	    q0 = q0 * norm;

	    
	    return new Quaternion(q0,q1,q2,q3);
		}


	public class Quaternion{

		public double q1, q2, q3, q4;

		public Quaternion(double q1, double q2, double q3, double q4)
		{
			this.q1 = q1;
			this.q2 = q2;
			this.q3 = q3;
			this.q4 = q4;
		}
	}




}
