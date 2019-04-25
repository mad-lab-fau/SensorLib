//Rev_1.7
/*
 * Copyright (c) 2010 - 2014, Shimmer Research, Ltd.
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
 * @author Jong Chern Lim, Ruaidhri Molloy
 * @date   October, 2013
 *
 * Changes since 1.6
 * - cancel timers for log and stream upon disconnect
 *
 * Changes since 1.5.1
 * - move response time out to ShimmerBluetooth
 *
 * Changes since 1.5 (12 May 2014, RM first revision)
 * - Addition of Strain Gauge for Shimmer3
 *
 *  Changes since beta 1.4
 *  - updated lowbattindicator for Shimmer2r
 *
 *  Changes since beta 1.3.1 (17 Oct 2013)
 *  - added get method for mPressureResolution 
 *  - updated the smart accel mode, which was over complicating things, right now there are two modes, smart accel mode will include 'Accelerometer X'. 'Accelerometer X' is picked based on which accelerometer meets the defined range and has the lowest noise. For backward compatibility users are advised to use Smart Mode. Normal_Mode will not add 'Accelerometer X'.
 *  - fixed boolean indicator for gyro on the fly method  
 *  - fixed getListofEnabledSensors method
 *  - fixed decimalformat which was returning commas for decimal points for certain regions/locale
 *  - fixed a bug when using max number of sensors on Shimmer3
 *  - updated initialize2r method which was not reading the magrange
 *  - fixed a bug with the pressure sensor calibration parameters
 *  - rename NORMAL_MODE to ACCEL_NORMAL_MODE
 *  - added support for Shimmer3 GSR
 *  - minor fix to ACCEL_NORMAL_MODE
 *  - added rawdata and systemtimestamp to objectcluster
 *  - add option to disable the calibration of data, see function enableCalibration()
 *  - removed mgetdatainstructions, not used for anything
 *  - added support for saving and retrieving rawcalibrationparams, should only be used after connected to a device
 *  - updated various variables to protected so it can be inherited
 *  - switch logfile id to mClassName from "Shimmer"
 *  - fixed initialize Shimmer3, maggain and gyrorange being set wrongly
 *  - Shimmer3 GSR support
 *  - Updated the structure to allow future addons (.eg. ShimmerFile , ShimmerBTLE, Shimmer802_15_4) which will all inherit ShimmerObject, also will allow exporting the code to non andoid use, users will just have to remove Shimmer.java which has the Android related code
 *  - Support for internal exp power (Shimmer3) added
 *  
 *  Changes since beta 1.2 (14 Oct 2013)
 *  - added support read data for Shimmer3 pressure sensor
 *  - added support to set pressure resolution
 * 
 *  Changes since beta 1.1.3 (10 Oct 2013)
 *  - updated to work with Boilerplate and to fix a bug with version control as there is BTStream 0.1.0 and Boilerplate 0.1.0 
 *  - minor change to comments
 *  - fixed twos complement function
 *   
 *  Changes since beta 1.1.2 (1 Oct 2013)
 *  - mag gain command implemented for Shimmer2
 *  
 *  Changes since beta 1.1.1 (17 July 2013)
 *  - 
 *  - added support for dual accelerometer mode for Shimmer 3
 *  - updated wide range accel from i12> to i16 and updated default calibration values
 *  
 *  Changes since beta 1.0.2 (17 July 2013)
 *  - started integration with Shimmer 3, major changes include the use of i16* now. This indicates array of bytes where MSB is on the far left/smallest index number of the array.
 *  - minor fix to the stop streaming command, causing it to block the inputstream, and not being able to clear the bytes from minstream
 *  - added default calibration parameters for Shimmer 3
 *  - added functionality for internal and external adc
 *  - added new constructor to support setup device on connect (Shimmer 3)
 *  
 * Changes since beta 1.0.1 (20 June 2013)
 * - Fix the no response bug, through the use of the function dummyreadSamplingRate()
 * - Updates to allow operation with Boilerplate
 * - add get functions for lower power mag and gyro cal on the fly
 * 
 * Changes since beta 1.0 (21 May 2013)
 * - Added support for on the fly gyro offset calibration
 * - Added quartenions
 * - Convert to an instruction stack format, no longer supports Boilerplate
 * 
 * Changes since beta 0.9 (1 January 2013)
 * 
 * - Packet Reception Rate is now provided, whenever a packet loss detected a message is sent via handler, see MESSAGE_PACKET_LOSS_DETECTED
 * - Changed Accel cal parameters
 * - Added default cal parameters for the other accel ranges, if default accel range is being used, changing the accel range will change the defaults automatically as well
 * - Revised the GSR calibration method, now uses a linear fit
 * - Batt Voltage Monitoring
 * - Sensor Conflict checks, have to wire the handler in order to see the msgs
 * - Bug fix, timer wasnt triggering when waiting for response which was not received, causing the driver to get stuck in a loop
 * - Added retrieve all,ecg & emg calibration parameters, only works with Boilerplate >= 1.0
 * - Rearranged the data reception section, to accommodate for in streaming ack detection
 * - Added uncalibrated heart rate, which is a pulse now, with the value being the time difference between the last pulse and the current one
 * - Updated the name of the formats, units and property names, so as to stay consistent with the rest of the instrument drivers
 * - Low Battery Voltage warning at 3.4V where LED turns yellow
 * - Added Packet Reception Rate monitoring
 * - Added MESSAGE_NOT_SYNC for MSS support
 * - Updated the initialization process, if a connection fails during the initialization process, disconnect is done immediately
 * - Update Toggle LED
 * - Switched to the use of createInsecureRfcommSocketToServiceRecord 
 * - ECG and EMG units have a * (mVolts*) as an indicator when default parameters are used
 * - SR 30 support
 * - Orientation 
 * - Support for low and high power Mag (high power == high sampling rate Mag)
 * - Support for different mag range
 * - Updated the execution model when transmitting commands, now uses a thread, and will improve Main UI thread latency 
 
 * */

package com.shimmerresearch.android;

import it.gerdavax.easybluetooth.BtSocket;
import it.gerdavax.easybluetooth.LocalDevice;
import it.gerdavax.easybluetooth.RemoteDevice;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;








import com.shimmerresearch.algorithms.GradDes3DOrientation;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerMsg;
import com.shimmerresearch.driver.ShimmerObject;
import com.shimmerresearch.driver.Configuration.Shimmer3;
import com.shimmerresearch.driver.Configuration.Shimmer3.SensorBitmap;









import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
//import java.io.FileOutputStream;

public class Shimmer extends ShimmerBluetooth{
	//generic UUID for serial port protocol
	private UUID mSPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Message types sent from the Shimmer Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_ACK_RECEIVED = 4;
	public static final int MESSAGE_DEVICE_NAME = 5;
	public static final int MESSAGE_TOAST = 6;
	public static final int MESSAGE_SAMPLING_RATE_RECEIVED = 7;
	public static final int MESSAGE_INQUIRY_RESPONSE = 8;
	public static final int MESSAGE_STOP_STREAMING_COMPLETE = 9;
	public static final int MESSAGE_PACKET_LOSS_DETECTED = 11;
	public static final int MESSAGE_NOT_SYNC = 12;
	public static final int MESSAGE_LOG_AND_STREAM_STATUS_CHANGED = 13;
	
	// Key names received from the Shimmer Handler 
	public static final String TOAST = "toast";
	private final BluetoothAdapter mAdapter;
	public final Handler mHandler;

	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private boolean mDummy=false;
	private LocalDevice localDevice;
	//private InputStream mInputStream=null;
	//private DataInputStream mInStream=null;
	private DataInputStream mInStream;
	//private BufferedInputStream mInStream=null;
	private OutputStream mmOutStream=null;

	public static final int MSG_STATE_FULLY_INITIALIZED = 3;  // This is the connected state, indicating the device has establish a connection + tx/rx commands and reponses (Initialized)
	public static final int MSG_STATE_STREAMING = 4;
	public static final int MSG_STATE_STOP_STREAMING = 5;

	protected String mClassName="Shimmer";
	
	private int mBluetoothLib=0;												// 0 = default lib, 1 = arduino lib
	private BluetoothAdapter mBluetoothAdapter = null;
		
	

	
	/**
	 * Constructor. Prepares a new Bluetooth session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myName  To allow the user to set a unique identifier for each Shimmer device
	 * @param continuousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public Shimmer(Handler handler, String myName, Boolean continuousSync) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mMyName=myName;
		mContinousSync=continuousSync;
		mSetupDevice=false;
	}
	
	public Shimmer(Context context, Handler handler, String myName, Boolean continousSync) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mMyName=myName;
		mContinousSync=continousSync;
		mSetupDevice=false;
	}
	
	
	/**
	 * Constructor. Prepares a new Bluetooth session. Additional fields allows the device to be set up immediately.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myName  To allow the user to set a unique identifier for each Shimmer device
	 * @param samplingRate Defines the sampling rate
	 * @param accelRange Defines the Acceleration range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 * @param gsrRange Numeric value defining the desired gsr range. Valid range settings are 0 (10kOhm to 56kOhm),  1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 * @param setEnabledSensors Defines the sensors to be enabled (e.g. 'Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO' enables the Accelerometer and Gyroscope)
	 * @param continousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public Shimmer(Context context, Handler handler, String myName, double samplingRate, int accelRange, int gsrRange, long setEnabledSensors, boolean continousSync) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mShimmerSamplingRate = samplingRate;
		mAccelRange = accelRange;
		mGSRRange = gsrRange;
		mSetEnabledSensors=setEnabledSensors;
		mMyName = myName;
		mSetupDevice = true;
		mContinousSync = continousSync;
	}

	/**
	 * Constructor. Prepares a new Bluetooth session. Additional fields allows the device to be set up immediately.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myName  To allow the user to set a unique identifier for each Shimmer device
	 * @param samplingRate Defines the sampling rate
	 * @param accelRange Defines the Acceleration range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 * @param gsrRange Numeric value defining the desired gsr range. Valid range settings are 0 (10kOhm to 56kOhm),  1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 * @param setEnabledSensors Defines the sensors to be enabled (e.g. 'Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO' enables the Accelerometer and Gyroscope)
	 * @param continousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public Shimmer(Context context, Handler handler, String myName, double samplingRate, int accelRange, int gsrRange, long setEnabledSensors, boolean continousSync, int magGain) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mShimmerSamplingRate = samplingRate;
		mAccelRange = accelRange;
		mMagRange = magGain;
		mGSRRange = gsrRange;
		mSetEnabledSensors=setEnabledSensors;
		mMyName = myName;
		mSetupDevice = true;
		mContinousSync = continousSync;
	}

	
	/**
	 * Constructor. Prepares a new Bluetooth session. Additional fields allows the device to be set up immediately.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myName  To allow the user to set a unique identifier for each Shimmer device
	 * @param samplingRate Defines the sampling rate
	 * @param accelRange Defines the Acceleration range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 * @param gsrRange Numeric value defining the desired gsr range. Valid range settings are 0 (10kOhm to 56kOhm),  1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 * @param setEnabledSensors Defines the sensors to be enabled (e.g. 'Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO' enables the Accelerometer and Gyroscope)
	 * @param continousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public Shimmer(Context context, Handler handler, String myName, double samplingRate, int accelRange, int gsrRange, long setEnabledSensors, boolean continousSync, boolean enableLowPowerAccel, boolean enableLowPowerGyro, boolean enableLowPowerMag, int gyroRange, int magRange) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mShimmerSamplingRate = samplingRate;
		mAccelRange = accelRange;
		mGSRRange = gsrRange;
		mSetEnabledSensors=setEnabledSensors;
		mMyName = myName;
		mSetupDevice = true;
		mContinousSync = continousSync;
		mLowPowerMag = enableLowPowerMag;
		mLowPowerAccelWR = enableLowPowerAccel;
		mLowPowerGyro = enableLowPowerGyro;
		mGyroRange = gyroRange;
		mMagRange = magRange;
	}


	/**
	 * Constructor. Prepares a new Bluetooth session. Additional fields allows the device to be set up immediately.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 * @param myName  To allow the user to set a unique identifier for each Shimmer device
	 * @param samplingRate Defines the sampling rate
	 * @param accelRange Defines the Acceleration range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 * @param gsrRange Numeric value defining the desired gsr range. Valid range settings are 0 (10kOhm to 56kOhm),  1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 * @param setEnabledSensors Defines the sensors to be enabled (e.g. 'Shimmer.SENSOR_ACCEL|Shimmer.SENSOR_GYRO' enables the Accelerometer and Gyroscope)
	 * @param continousSync A boolean value defining whether received packets should be checked continuously for the correct start and end of packet.
	 */
	public Shimmer(Context context, Handler handler, String myName, double samplingRate, int accelRange, int gsrRange, long setEnabledSensors, boolean continousSync, boolean enableLowPowerAccel, boolean enableLowPowerGyro, boolean enableLowPowerMag, int gyroRange, int magRange,byte[] exg1,byte[] exg2) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		mShimmerSamplingRate = samplingRate;
		mAccelRange = accelRange;
		mGSRRange = gsrRange;
		mSetEnabledSensors=setEnabledSensors;
		mMyName = myName;
		mSetupDevice = true;
		mContinousSync = continousSync;
		mLowPowerMag = enableLowPowerMag;
		mLowPowerAccelWR = enableLowPowerAccel;
		mLowPowerGyro = enableLowPowerGyro;
		mGyroRange = gyroRange;
		mMagRange = magRange;
		mSetupEXG = true;
		mEXG1RegisterArray = exg1;
		mEXG2RegisterArray = exg2;
	}
	


	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	protected synchronized void setState(int state) {
		mState = state;
		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE, state, -1, new ObjectCluster(mMyName,getBluetoothAddress())).sendToTarget();
	}

	/**
	 * Return the current connection state. */
	public synchronized int getShimmerState() {
		return mState;
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device. The purpose of having two libraries is because some Stock firmware do not implement the full Bluetooth Stack. In such cases use 'gerdavax'. If problems persist consider installing an aftermarket firmware, with a mature Bluetooth stack.
	 * @param address Bluetooth Address of Device to connect too
	 * @param bluetoothLibrary Supported libraries are 'default' and 'gerdavax'  
	 */
	public synchronized void connect(final String address, String bluetoothLibrary) {
		mIamAlive = false;
		mListofInstructions.clear();
		mFirstTime=true;
		if (bluetoothLibrary=="default"){
			mMyBluetoothAddress=address;
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

			// Cancel any thread attempting to make a connection
			if (mState == STATE_CONNECTING) {
				if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
			}
			// Cancel any thread currently running a connection
			if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

			// Start the thread to connect with the given device
			mConnectThread = new ConnectThread(device);
			mConnectThread.start();
			setState(STATE_CONNECTING);
		} else if (bluetoothLibrary=="gerdavax"){
			mMyBluetoothAddress=address;
			// Cancel any thread attempting to make a connection
			if (mState == STATE_CONNECTING) {
				if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
			}
			// Cancel any thread currently running a connection
			if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

			if (address == null) return;
			Log.d("ConnectionStatus","Get Local Device  " + address);

			localDevice = LocalDevice.getInstance();
			RemoteDevice device = localDevice.getRemoteForAddr(address);
			new ConnectThreadArduino(device).start();
			setState(STATE_CONNECTING);
			/*localDevice.init(this, new ReadyListener() {
   			@Override
   			public synchronized void ready() {


   				//localDevice.destroy();


   			}
   		});*/


		}
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mIOThread = new IOThread();
		mIOThread.start();
		mPThread = new ProcessingThread();
		mPThread.start();
				
		mMyBluetoothAddress = device.getAddress();
		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(Shimmer.MESSAGE_DEVICE_NAME);
		mHandler.sendMessage(msg);
		setState(STATE_CONNECTED);
		initialize();
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		
		if (mTimerToReadStatus!=null) {
			mTimerToReadStatus.cancel();
			mTimerToReadStatus.purge();
		}
		
		if (mAliveTimer!=null){
			mAliveTimer.cancel();
			mAliveTimer.purge();
			mAliveTimer = null;
		}
		
		if (mTimer!=null){
			mTimer.cancel();
			mTimer.purge();
		}
		setState(STATE_NONE);
		mStreaming = false;
		mInitialized = false;
		if (mIOThread != null) {
			mIOThread.stop = true;
			mIOThread = null;
			mPThread.stop =true;
			mPThread = null;
			
		}
		if (mConnectThread != null) {
			mConnectThread.cancel(); 
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel(); 
			mConnectedThread = null;
		}

	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * @param out The bytes to write
	 * @see ConnectedThread write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED) return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_NONE);
		mInitialized = false;
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	protected void connectionLost() {
		if (mIOThread != null) {
			mIOThread.stop = true;
			mIOThread = null;
			mPThread.stop =true;
			mPThread = null;
			
		}
		setState(STATE_NONE);
		mInitialized = false;
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			Log.d(mClassName,"Start of Default ConnectThread");
			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createInsecureRfcommSocketToServiceRecord(mSPP_UUID); // If your device fails to pair try: device.createInsecureRfcommSocketToServiceRecord(mSPP_UUID)
			} catch (IOException e) {
				connectionLost();
				
			}
			mmSocket = tmp;
		}

		public void run() {
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				connectionFailed();
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) { }
				return;
			}
			// Reset the ConnectThread because we're done
			synchronized (Shimmer.this) {
				mConnectThread = null;
			}
			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}
	//arduino
	private class ConnectThreadArduino extends Thread {

		//private static final String TAG = "ConnectThread";
		private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		private final RemoteDevice mDevice;
		private BtSocket mSocket;

		public ConnectThreadArduino(RemoteDevice device) {
			mDevice = device;
			Log.d(mClassName," Start of ArduinoConnectThread");
		}

		public void run() {
			try {
				boolean isPaired = false;

				try {
					isPaired = mDevice.ensurePaired();


				}
				catch (RuntimeException re){
					re.printStackTrace();
				}

				//add a timer to wait for the user to pair the device otherwise quit
				if (!isPaired){
					Thread.sleep(10000);
					isPaired = mDevice.ensurePaired();
				}

				if (!isPaired){
					Log.d(mClassName, "not paired!");
					connectionFailed();


				}
				else {
					Log.d(mClassName, "is paired!");
					// Let main thread do some stuff to render UI immediately
					//Thread.yield();
					// Get a BluetoothSocket to connect with the given BluetoothDevice
					try {
						mSocket = mDevice.openSocket(SPP_UUID);
					} catch (Exception e) {
						Log.d(mClassName, "Connection via SDP unsuccessful, try to connect via port directly");
						// 1.x Android devices only work this way since SDP was not part of their firmware then
						mSocket = mDevice.openSocket(1);
						//connectionFailed();
						Log.d(mClassName, "I am here");
					}

					// Do work to manage the connection (in a separate thread)
					Log.d(mClassName, "Going to Manage Socket");
					if (getShimmerState() != STATE_NONE){
						Log.d(mClassName, "ManagingSocket");
						manageConnectedSocket(mSocket);
					}
				}
			}

			catch (Exception e) {
				Log.d(mClassName,"Connection Failed");
				//sendConnectionFailed(mDevice.getAddress());
				connectionFailed();
				e.printStackTrace();
				if (mSocket != null)
					try {
						mSocket.close();
						Log.d(mClassName,"Arduinothreadclose");
					} catch (IOException e1) {}

				return;
			}
		}

		/** Will cancel an in-progress connection, and close the socket */
		@SuppressWarnings("unused")
		public void cancel() {
			try {
				if (mSocket != null) mSocket.close();
				//sendConnectionDisconnected(mDevice.getAddress());
			} 
			catch (IOException e) { Log.e("Shimmer", "cannot close socket to " + mDevice.getAddress()); }
		}

		private void manageConnectedSocket(BtSocket socket){
			//	    	Logger.d(TAG, "connection established.");
			// pass the socket to a worker thread
			String address = mDevice.getAddress();
			mConnectedThread = new ConnectedThread(socket, address);
			Log.d(mClassName, "ConnectedThread is about to start");
			mIOThread = new IOThread();
			mIOThread.start();
			mPThread = new ProcessingThread();
			mPThread.start();
			// Send the name of the connected device back to the UI Activity
			mMyBluetoothAddress = mDevice.getAddress();
			Message msg = mHandler.obtainMessage(Shimmer.MESSAGE_DEVICE_NAME);
			mHandler.sendMessage(msg);
			// Send the name of the connected device back to the UI Activity
			while(!mIOThread.isAlive()){}; 
			Log.d(mClassName, "alive!!");
			setState(STATE_CONNECTED);
			//startStreaming();
			initialize();
		}
	}




	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread{
		private BluetoothSocket mmSocket=null;

		private BtSocket mSocket=null;
		
		public ConnectedThread(BluetoothSocket socket) {

			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				connectionLost();
			}

			//mInStream = new BufferedInputStream(tmpIn);
			mInStream = new DataInputStream(tmpIn);
			mmOutStream = tmpOut;
		}

		public ConnectedThread(BtSocket socket, String address) {
			mSocket = socket;
			//this.mAddress = address;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (Exception e) { Log.d(mClassName,"Connected Thread Error");
			connectionLost();}

			//mInStream = new BufferedInputStream(tmpIn);
			mInStream = new DataInputStream(tmpIn);
			mmOutStream = tmpOut;

		}

		/**
		 *The received packets are processed here 
		 */
	
		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
		private void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				Log.d(mClassName, "Command transmitted: " + mMyBluetoothAddress + "; Command Issued: " + mCurrentCommand );

			} catch (IOException e) {
				Log.d(mClassName, "Command NOT transmitted: " + mMyBluetoothAddress + "; Command Issued: " + mCurrentCommand );
			}
		}

		public void cancel() {
			if(mInStream != null) {
				try {
					mInStream.close();
				} catch (IOException e) {}
			}
			if(mmOutStream != null) {
				try {
					mmOutStream.close();
				} catch (IOException e) {}
			}
			if(mmSocket != null) {
				try {
					if (mBluetoothLib==0){
						mmSocket.close();
					}	else {
						mSocket.close();
					}
				} catch (IOException e) {}
			}
		}
	}






	protected void inquiryDone() {
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Inquiry done for device-> " + mMyBluetoothAddress);
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		isReadyForStreaming();
	}   

	protected void isReadyForStreaming(){
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Device " + mMyBluetoothAddress +" is ready for Streaming");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		if (mInitialized == false){
			//only do this during the initialization process to indicate that it is fully initialized, dont do this for a normal inqiuiry
			mInitialized = true;
		}
		mHandler.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE, MSG_STATE_FULLY_INITIALIZED, -1, new ObjectCluster(mMyName,getBluetoothAddress())).sendToTarget();
		Log.d(mClassName,"Shimmer " + mMyBluetoothAddress +" Initialization completed and is ready for Streaming");
	}

	protected void isNowStreaming() {

		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Device " + mMyBluetoothAddress + " is now Streaming");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		Log.d(mClassName,"Shimmer " + mMyBluetoothAddress +" is now Streaming");
		mHandler.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE, MSG_STATE_STREAMING, -1, new ObjectCluster(mMyName,getBluetoothAddress())).sendToTarget();
		
	}

	/*
	 * Set and Get Methods
	 * */    
	public void setContinuousSync(boolean continousSync){
		mContinousSync=continousSync;
	}

	public boolean getStreamingStatus(){
		return mStreaming;
	}

	


	/**
	 * This returns the variable mTransactionCompleted which indicates whether the Shimmer device is in the midst of a command transaction. True when no transaction is taking place. This is deprecated since the update to a thread model for executing commands
	 * @return mTransactionCompleted
	 */
	public boolean getInstructionStatus()
	{	
		boolean instructionStatus=false;
		if (mTransactionCompleted == true) {
			instructionStatus=true;
		} else {
			instructionStatus=false;
		}
		return instructionStatus;
	}

	public double getSamplingRate(){
		return mShimmerSamplingRate;
	}

	/**
	 * Purpose of disabling calibration are for logging applications, to reduce the amount of computations needed to compute the data thus reducing risk of a performance bottleneck occuring and maximizing logging performance
	 * @param enable enables or disables calibration, note that this need to be enabled when using the ID as to calculate 3D orientation, see enable3DOrientation 
	 */
	public void enableCalibration(boolean enable){
		mEnableCalibration = enable;
	}

	



	public void writeInstruction(){
		if (getShimmerState() == STATE_CONNECTED) {
			byte[] instruction = (byte[]) mListofInstructions.get(0);
			write(instruction);
		}
	}



	
	
	@Override
	protected void sendStatusMsgPacketLossDetected() {
		// TODO Auto-generated method stub
		mHandler.obtainMessage(Shimmer.MESSAGE_PACKET_LOSS_DETECTED,  new ObjectCluster(mMyName,getBluetoothAddress())).sendToTarget();
	}
	



	@Override
	protected boolean bytesToBeRead() {
		// TODO Auto-generated method stub
		try {
			if (mInStream.available()!=0){
				return true;
			} else {
			 return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			connectionLost();
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected int availableBytes() {
		try {
			return mInStream.available();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			connectionLost();
			e.printStackTrace();
			return 0;
		}
	}

	

	@Override
	protected void writeBytes(byte[] data) {
		// TODO Auto-generated method stub
		write(data);
	}

	/*
	public byte[] readBytes(int numberofBytes){
		  byte[] b = new byte[numberofBytes];  
		  try{

			   int timeoutMillis = 500;
			   int bufferOffset = 0;
			   long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
			   while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length && mState!=STATE_NONE) {
			    int readLength = java.lang.Math.min(mInStream.available(),b.length-bufferOffset);
			    // can alternatively use bufferedReader, guarded by isReady():
			    int readResult = mInStream.read(b, bufferOffset, readLength);
			    if (readResult == -1) break;
			    bufferOffset += readResult;
		   }
			   return b;
		  } catch (IOException e) {
		   // TODO Auto-generated catch block
			   connectionLost();
			   e.printStackTrace();
			   return b;
		  }
	}*/
	
	@Override
	protected byte[] readBytes(int numberofBytes) {
		// TODO Auto-generated method stub
		byte[] b = new byte[numberofBytes];
		try {
			//mIN.read(b,0,numberofBytes);
			mInStream.readFully(b,0,numberofBytes);
			return(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Connection Lost");
			e.printStackTrace();
		}
			
			
		return null;
	}

	@Override
	protected byte readByte() {
		byte[] tb = new byte[1];
		try {
			//mInStream.read(tb,0,1);
			mInStream.readFully(tb,0,1);
			return tb[0];
		} catch (IOException e) {
			// TODO Auto-generated catch block
			connectionLost();
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	protected void dataHandler(ObjectCluster ojc) {
		// TODO Auto-generated method stub
		mHandler.obtainMessage(MESSAGE_READ, ojc).sendToTarget();
	}

	@Override
	protected void sendStatusMSGtoUI(String smsg) {
		// TODO Auto-generated method stub
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, smsg);
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	@Override
	protected void printLogDataForDebugging(String msg) {
		// TODO Auto-generated method stub
		Log.d(mClassName,msg);
	}

	@Override
	protected void hasStopStreaming() {
		// TODO Auto-generated method stub
		Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(TOAST, "Device " + mMyBluetoothAddress +" stopped streaming");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		mHandler.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE, MSG_STATE_STOP_STREAMING, -1, new ObjectCluster(mMyName,getBluetoothAddress())).sendToTarget();
		
	}

	@Override
	protected void logAndStreamStatusChanged() {
		// TODO Auto-generated method stub
		
		int docked, sensing;
		
		if(isDocked())
			docked=1;
		else
			docked=0;
		
		if(isSensing())
			sensing=1;
		else
			sensing=0;
		
		mHandler.obtainMessage(Shimmer.MESSAGE_LOG_AND_STREAM_STATUS_CHANGED, docked, sensing).sendToTarget();
		Log.d(mClassName,"Shimmer " + mMyBluetoothAddress +" Status has changed. Docked: "+docked+" Sensing: "+sensing);
	}

	@Override
	protected void processMsgFromCallback(ShimmerMsg shimmerMSG) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	protected void checkBatteryShimmer2r() {
//		if (!mWaitForAck) {	
//			if (mVSenseBattMA.getMean()<mLowBattLimit*1000) {
//				if (mCurrentLEDStatus!=1) {
//					writeLEDCommand(1);
//				}
//			} else if(mVSenseBattMA.getMean()>mLowBattLimit*1000+100) { //+100 is to make sure the limits are different to prevent excessive switching when the batt value is at the threshold
//				if (mCurrentLEDStatus!=0) {
//					writeLEDCommand(0);
//				}
//			}
//
//		}
//	}
//	
//	@Override
//	protected void checkBatteryShimmer3(){
//		if (!mWaitForAck){
//			if(mVSenseBattMA.getMean()<mLowBattLimit*1000){
//				if (mCurrentLEDStatus!=1) {
//					writeLEDCommand(1);
//				}
//			}
//			else if(mVSenseBattMA.getMean()>mLowBattLimit*1000+100){ //+100 is to make sure the limits are different to prevent excessive switching when the batt value is at the threshold
//				if (mCurrentLEDStatus!=0) {
//					writeLEDCommand(0);
//				}
//			}
//		}
//	}


	
}
