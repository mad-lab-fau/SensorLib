//Rev_1.9
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
 * @author Jong Chern Lim, Ruaidhri Molloy, Mark Nolan
 * @date  September, 2014
 * 
 * Changes since 1.8
 * - set mInstructionStackLock in initialize(), this fix a bug when upon a ack timeout disconnect shimmer is unable to reconnect
 * - add mtimer cancel and purge to intialize for precaution
 * - added reset, to prevent API thinking it is the wrong fwidentifier (e.g. using btstream after logandstream) causing the get_status timer to be called
 * - added readBlinkLED to initializeShimmer3, remove mCurrentLEDStatus from startStreaming
 * - added a check for get_dir and get_status timeout, device won't disconnect if there is packet loss detected
 * 
 * Changes since since 1.7 
 * - updated logandstream support, now supports push button, start-stop streaming
 * 
 * Changes since 1.6
 * - updated to support LogAndStream
 * - updated checkBatt()
 * 
 * Changes since 1.5
 * - updated comments
 * - Baud rate setting support
 *
 * Changes since 1.4.04
 * - Reduce timeout for get_shimmer_version_command_new, to speed up connection for Shimmer2r 
 * - Move timeout response task to here, removed from Shimmer and ShimmerPCBT
 * - Added 
 *
 * Changes since 1.4.03
 * - support for Shimmer3 bridge amplifier, sensor conflict handling for Shimmer3
 * - Added isEXGUsingTestSignal24Configuration() isEXGUsingTestSignal16Configuration() isEXGUsingECG24Configuration() isEXGUsingECG16Configuration() isEXGUsingEMG24Configuration() isEXGUsingEMG16Configuration()
 * 
 * Changes since 1.4.02
 * - moved setting of writeexg setting to after the ack, otherwise readexg and writeexg in the instruction stack will yield wrong results
 * 
 *  Changes since 1.4.01
 *  - added exg set configuration to initialize shimmer3 exg from constructor
 * 
 *  Changes since 1.4
 *  - removed mShimmerSamplingRate decimal formatter, decimal formatter should be done on the UI
 *  - remove null characters from mListofInstructions, after a stop streaming command, this was causing a race condition error
 *  
 */



package com.shimmerresearch.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;














import java.util.concurrent.ArrayBlockingQueue;


import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.ShimmerVerDetails.FW_ID;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerVerDetails.HW_ID;
import com.shimmerresearch.driver.ShimmerObject;
import com.shimmerresearch.tools.ArrayUtils;

public abstract class ShimmerBluetooth extends ShimmerObject implements Serializable{
	
	//region --------- CLASS VARIABLES AND ABSTRACT METHODS ---------
	
	protected long mSetEnabledSensors = SENSOR_ACCEL;								// Only used during the initialization process, see initialize();
	// Constants that indicate the current connection state
	
	public static final int STATE_NONE = 0;       // The class is doing nothing
	public static final int STATE_CONNECTING = 1; // The class is now initiating an outgoing connection
	public static final int STATE_CONNECTED = 2;  // The class is now connected to a remote device
	protected boolean mInstructionStackLock = false;
	protected int mState;
	protected byte mCurrentCommand;	
	protected boolean mWaitForAck=false;                                          // This indicates whether the device is waiting for an acknowledge packet from the Shimmer Device  
	protected boolean mWaitForResponse=false; 									// This indicates whether the device is waiting for a response packet from the Shimmer Device 
	protected boolean mTransactionCompleted=true;									// Variable is used to ensure a command has finished execution prior to executing the next command (see initialize())
	transient protected IOThread mIOThread;
	transient protected ProcessingThread mPThread;
	protected boolean mContinousSync=false;                                       // This is to select whether to continuously check the data packets 
	protected boolean mSetupDevice=false;		
	protected Stack<Byte> byteStack = new Stack<Byte>();
	protected double mLowBattLimit=3.4;
	protected int numBytesToReadFromExpBoard=0;
	ArrayBlockingQueue<byte[]> mABQ = new ArrayBlockingQueue<byte[]>(10000);
	protected boolean mIamAlive = false;
	protected abstract void connect(String address,String bluetoothLibrary);
	protected abstract void dataHandler(ObjectCluster ojc);
	protected abstract boolean bytesToBeRead();
	protected abstract int availableBytes();
	
	protected abstract void writeBytes(byte[] data);
	protected abstract void stop();
	protected abstract void isNowStreaming();
	protected abstract void hasStopStreaming();
	protected abstract void sendStatusMsgPacketLossDetected();
	protected abstract void inquiryDone();
	protected abstract void sendStatusMSGtoUI(String msg);
	protected abstract void printLogDataForDebugging(String msg);
	protected abstract void isReadyForStreaming();
	protected abstract void connectionLost();
	protected abstract void setState(int state);
	protected abstract void logAndStreamStatusChanged();
	
	protected boolean mInitialized = false;
	protected abstract byte[] readBytes(int numberofBytes);
	protected abstract byte readByte();
	protected List<byte []> mListofInstructions = new  ArrayList<byte[]>();
	private final int ACK_TIMER_DURATION = 2; 									// Duration to wait for an ack packet (seconds)
	transient protected Timer mTimer;														// Timer variable used when waiting for an ack or response packet
	protected boolean mDummy=false;
	protected boolean mFirstTime=true;
	private byte mTempByteValue;												// A temporary variable used to store Byte value	
	protected int mTempIntValue;													// A temporary variable used to store Integer value, used mainly to store a value while waiting for an acknowledge packet (e.g. when writeGRange() is called, the range is stored temporarily and used to update GSRRange when the acknowledge packet is received.
	protected long tempEnabledSensors;												// This stores the enabled sensors
	private int mTempChipID;
	protected boolean mSync=true;													// Variable to keep track of sync
	protected boolean mSetupEXG = false;
	private byte[] cmdcalibrationParameters = new byte [22];  
	private int mReadStatusPeriod=5000;
	private int mAliveStatusPeriod=2000;
	transient protected Timer mTimerToReadStatus;
	transient protected Timer mAliveTimer;
	private int mCountDeadConnection = 0;
	private boolean mCheckIfConnectionisAlive = false;
	
	transient ByteArrayOutputStream mByteArrayOutputStream = new ByteArrayOutputStream();
	
	//endregion
	
	/**
	 * Provides an interface directly to the method BuildMSG. This can be used to implement algorithm/filters/etc. Two methods are provided, processdata to implement your methods, and InitializeProcessData which is called everytime you startstreaming, in the event you need to reinitialize your method/algorithm everytime a Shimmer starts streaming
	 *
	 */
	public interface DataProcessing {

		/**
		 * Initialise your method/algorithm here, this callback is called when startstreaming is called
		 */
		
		/** Initialise Process Data here. This is called whenever the startStreaming command is called and can be used to initialise algorithms
		 * 
		 */
		public void InitializeProcessData();
		
		/** Process data here, algorithms can access the object cluster built by the buildMsg method here
		 * @param ojc the objectCluster built by the buildMsg method
		 * @return the processed objectCluster
		 */
		public ObjectCluster ProcessData(ObjectCluster ojc);

	}
	DataProcessing mDataProcessing;

	
	public class ProcessingThread extends Thread {
		byte[] tb ={0};
		byte[] newPacket=new byte[mPacketSize+1];
		public boolean stop = false;
		int count=0;
		public synchronized void run() {
			while (!stop) {
				if (!mABQ.isEmpty()){
					count++;
					if (count%1000==0){
						System.out.print("Queue Size: " + mABQ.size() + "\n");
						printLogDataForDebugging("Queue Size: " + mABQ.size() + "\n");
					}
					byte[] packet = mABQ.remove();
					ObjectCluster objectCluster=buildMsg(packet, FW_TYPE_BT, 0);
					if (mDataProcessing!=null){
						objectCluster = mDataProcessing.ProcessData(objectCluster);
					}
					dataHandler(objectCluster);
				}
			}
		}
	}
	
	//region --------- BLUETOOH STACK --------- 
	
	public class IOThread extends Thread {
		byte[] tb ={0};
		byte[] newPacket=new byte[mPacketSize+1];
		public boolean stop = false;
		public synchronized void run() {
			while (!stop) {
				/////////////////////////
				// is an instruction running ? if not proceed
				if (mInstructionStackLock==false){
					// check instruction stack, are there any other instructions left to be executed?
					if (!mListofInstructions.isEmpty()) {
						if (mListofInstructions.get(0)==null) {
							mListofInstructions.remove(0);
							String msg = "Null Removed ";
							printLogDataForDebugging(msg);
						}
					}
					if (!mListofInstructions.isEmpty()){

						byte[] insBytes = (byte[]) mListofInstructions.get(0);
						mCurrentCommand=insBytes[0];
						mInstructionStackLock=true;
						mWaitForAck=true;
						
						String msg = "Command Transmitted: " + Arrays.toString(insBytes);
						printLogDataForDebugging(msg);

						if(!mStreaming){
							while(availableBytes()>0){ //this is to clear the buffer 
								tb=readBytes(availableBytes());
							}
						}
						if(mCurrentCommand==SET_RWC_COMMAND){
							byte[] bytearray=ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
							ArrayUtils.reverse(bytearray);
							byte[] bytearraycommand= new byte[9];
							bytearraycommand[0]=SET_RWC_COMMAND;
							System.arraycopy(bytearray, 0, bytearraycommand, 1, 8);
							insBytes=bytearraycommand;
						}
						writeBytes(insBytes);

						
						if (mCurrentCommand==STOP_STREAMING_COMMAND){
							mStreaming=false;
							mListofInstructions.removeAll(Collections.singleton(null));
						} else {
							if (mCurrentCommand==GET_FW_VERSION_COMMAND){
								startResponseTimer(ACK_TIMER_DURATION);
							} else if (mCurrentCommand==GET_SAMPLING_RATE_COMMAND){
								startResponseTimer(ACK_TIMER_DURATION);
							} else if (mCurrentCommand==GET_SHIMMER_VERSION_COMMAND_NEW){
								startResponseTimer(ACK_TIMER_DURATION);
							} else {
								if(mStreaming){
									startResponseTimer(ACK_TIMER_DURATION);
								} else {
									startResponseTimer(ACK_TIMER_DURATION+10);
								}
							}
						}
						
							mTransactionCompleted=false;
						
					}


				}
				
				
				if (mWaitForAck==true && mStreaming ==false) {

					if (bytesToBeRead()){
						tb=readBytes(1);
						mIamAlive = true;
						String msg="";
						//	msg = "rxb resp : " + Arrays.toString(tb);
						//	printLogDataForDebugging(msg);

						if (mCurrentCommand==STOP_STREAMING_COMMAND) { //due to not receiving the ack from stop streaming command we will skip looking for it.
							mTimer.cancel();
							mTimer.purge();
							mStreaming=false;
							mTransactionCompleted=true;
							mWaitForAck=false;
							try {
								Thread.sleep(200);	// Wait to ensure that we dont missed any bytes which need to be cleared
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byteStack.clear();

							while(availableBytes()>0){ //this is to clear the buffer 

								tb=readBytes(availableBytes());

							}
							hasStopStreaming();					
							mListofInstructions.remove(0);
							mListofInstructions.removeAll(Collections.singleton(null));
							mInstructionStackLock=false;
						}

						if ((byte)tb[0]==ACK_COMMAND_PROCESSED)
						{	
							msg = "Ack Received for Command: " + Byte.toString(mCurrentCommand);
							printLogDataForDebugging(msg);
							if (mCurrentCommand==START_STREAMING_COMMAND || mCurrentCommand==START_SDBT_COMMAND) {
								mTimer.cancel();
								mTimer.purge();
								mStreaming=true;
								mTransactionCompleted=true;
								byteStack.clear();
								isNowStreaming();
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							
							else if (mCurrentCommand==SET_SAMPLING_RATE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted=true;
								mWaitForAck=false;
								byte[] instruction=mListofInstructions.get(0);
								double tempdouble=-1;
								if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){
									tempdouble=(double)1024/instruction[1];
								} else {
									tempdouble = 32768/(double)((int)(instruction[1] & 0xFF) + ((int)(instruction[2] & 0xFF) << 8));
								}
								mShimmerSamplingRate = tempdouble;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
								if (mHardwareVersion == HW_ID.SHIMMER_3){ // has to be here because to ensure the current exgregister settings have been read back
									//check sampling rate and adjust accordingly;
									/*if (mShimmerSamplingRate<=128){
										writeEXGRateSetting(1,0);
										writeEXGRateSetting(2,0);
									} else if (mShimmerSamplingRate<=256){
										writeEXGRateSetting(1,1);
										writeEXGRateSetting(2,1);
									}
									else if (mShimmerSamplingRate<=512){
										writeEXGRateSetting(1,2);
										writeEXGRateSetting(2,2);
									}*/
								}
								
							}
							else if (mCurrentCommand==SET_BUFFER_SIZE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mWaitForAck=false;
								mBufferSize=(int)((byte[])mListofInstructions.get(0))[1];
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==INQUIRY_COMMAND) {
								mWaitForResponse=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_LSM303DLHC_ACCEL_LPMODE_COMMAND) {
								mWaitForResponse=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_LSM303DLHC_ACCEL_HRMODE_COMMAND) {
								mWaitForResponse=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_BUFFER_SIZE_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_RWC_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_BLINK_LED) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_MAG_SAMPLING_RATE_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_MAG_GAIN_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_ACCEL_SENSITIVITY_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
							}
							else if (mCurrentCommand==GET_MPU9150_GYRO_RANGE_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
							}
							else if (mCurrentCommand==GET_GSR_RANGE_COMMAND) {
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_FW_VERSION_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
							}
							else if (mCurrentCommand==GET_ECG_CALIBRATION_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_EMG_CALIBRATION_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==SET_BLINK_LED) {
								mCurrentLEDStatus=(int)((byte[])mListofInstructions.get(0))[1];
								mTransactionCompleted = true;
								//mWaitForAck=false;
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_GSR_RANGE_COMMAND) {

								mTransactionCompleted = true;
								mWaitForAck=false;
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mGSRRange=(int)((byte [])mListofInstructions.get(0))[1];
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==GET_SAMPLING_RATE_COMMAND) {
								mWaitForResponse=true;
								mWaitForAck=false;

							}
							else if (mCurrentCommand==GET_CONFIG_BYTE0_COMMAND) {
								mWaitForResponse=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==SET_CONFIG_BYTE0_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mConfigByte0=(int)((byte [])mListofInstructions.get(0))[1];
								mWaitForAck=false;
								mTransactionCompleted=true;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_LSM303DLHC_ACCEL_LPMODE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mWaitForAck=false;
								mTransactionCompleted=true;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_LSM303DLHC_ACCEL_HRMODE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mWaitForAck=false;
								mTransactionCompleted=true;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_PMUX_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								if (((byte[])mListofInstructions.get(0))[1]==1) {
									mConfigByte0=(byte) ((byte) (mConfigByte0|64)&(0xFF)); 
								}
								else if (((byte[])mListofInstructions.get(0))[1]==0) {
									mConfigByte0=(byte) ((byte)(mConfigByte0 & 191)&(0xFF));
								}
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if(mCurrentCommand==SET_BMP180_PRES_RESOLUTION_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mPressureResolution=(int)((byte [])mListofInstructions.get(0))[1];
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_GYRO_TEMP_VREF_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted=true;
								mConfigByte0=mTempByteValue;
								mWaitForAck=false;
							}
							else if (mCurrentCommand==SET_5V_REGULATOR_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								if (((byte[])mListofInstructions.get(0))[1]==1) {
									mConfigByte0=(byte) (mConfigByte0|128); 
								}
								else if (((byte[])mListofInstructions.get(0))[1]==0) {
									mConfigByte0=(byte)(mConfigByte0 & 127);
								}
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_INTERNAL_EXP_POWER_ENABLE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								if (((byte[])mListofInstructions.get(0))[1]==1) {
									mConfigByte0 = (mConfigByte0|16777216); 
									mInternalExpPower = 1;
								}
								else if (((byte[])mListofInstructions.get(0))[1]==0) {
									mConfigByte0 = mConfigByte0 & 4278190079l;
									mInternalExpPower = 0;
								}
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_VBATT_FREQ_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_RWC_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_ACCEL_SENSITIVITY_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mAccelRange=(int)(((byte[])mListofInstructions.get(0))[1]);
								if (mDefaultCalibrationParametersAccel == true){
									if (mHardwareVersion != HW_ID.SHIMMER_3){
										if (getAccelRange()==0){
											mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel1p5gShimmer2; 
										} else if (getAccelRange()==1){
											mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel2gShimmer2; 
										} else if (getAccelRange()==2){
											mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel4gShimmer2; 
										} else if (getAccelRange()==3){
											mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel6gShimmer2; 
										}
									} else if(mHardwareVersion == HW_ID.SHIMMER_3){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixLowNoiseAccel2gShimmer3;
										mAlignmentMatrixAnalogAccel = AlignmentMatrixLowNoiseAccelShimmer3;
										mOffsetVectorAnalogAccel = OffsetVectorLowNoiseAccelShimmer3;
									}
								}

								if (mDefaultCalibrationParametersDigitalAccel){
									if (mHardwareVersion == HW_ID.SHIMMER_3){
										if (getAccelRange()==1){
											mSensitivityMatrixWRAccel = SensitivityMatrixWideRangeAccel4gShimmer3;
											mAlignmentMatrixWRAccel = AlignmentMatrixWideRangeAccelShimmer3;
											mOffsetVectorWRAccel = OffsetVectorWideRangeAccelShimmer3;
										} else if (getAccelRange()==2){
											mSensitivityMatrixWRAccel = SensitivityMatrixWideRangeAccel8gShimmer3;
											mAlignmentMatrixWRAccel = AlignmentMatrixWideRangeAccelShimmer3;
											mOffsetVectorWRAccel = OffsetVectorWideRangeAccelShimmer3;
										} else if (getAccelRange()==3){
											mSensitivityMatrixWRAccel = SensitivityMatrixWideRangeAccel16gShimmer3;
											mAlignmentMatrixWRAccel = AlignmentMatrixWideRangeAccelShimmer3;
											mOffsetVectorWRAccel = OffsetVectorWideRangeAccelShimmer3;
										} else if (getAccelRange()==0){
											mSensitivityMatrixWRAccel = SensitivityMatrixWideRangeAccel2gShimmer3;
											mAlignmentMatrixWRAccel = AlignmentMatrixWideRangeAccelShimmer3;
											mOffsetVectorWRAccel = OffsetVectorWideRangeAccelShimmer3;
										}
									}
								}
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} 
							
							else if (mCurrentCommand==SET_ACCEL_CALIBRATION_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								retrievekinematiccalibrationparametersfrompacket(Arrays.copyOfRange(mListofInstructions.get(0), 1, mListofInstructions.get(0).length), ACCEL_CALIBRATION_RESPONSE);	
								mTransactionCompleted = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
								}
							else if (mCurrentCommand==SET_GYRO_CALIBRATION_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();	
								retrievekinematiccalibrationparametersfrompacket(Arrays.copyOfRange(mListofInstructions.get(0), 1, mListofInstructions.get(0).length), GYRO_CALIBRATION_RESPONSE);	
								mTransactionCompleted = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_MAG_CALIBRATION_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								retrievekinematiccalibrationparametersfrompacket(Arrays.copyOfRange(mListofInstructions.get(0), 1, mListofInstructions.get(0).length), MAG_CALIBRATION_RESPONSE);	
								mTransactionCompleted = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_LSM303DLHC_ACCEL_CALIBRATION_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								retrievekinematiccalibrationparametersfrompacket(Arrays.copyOfRange(mListofInstructions.get(0), 1, mListofInstructions.get(0).length), LSM303DLHC_ACCEL_CALIBRATION_RESPONSE);	
								mTransactionCompleted = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							
							else if (mCurrentCommand==SET_MPU9150_GYRO_RANGE_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mGyroRange=(int)(((byte[])mListofInstructions.get(0))[1]);
								if (mDefaultCalibrationParametersGyro == true){
									if(mHardwareVersion == HW_ID.SHIMMER_3){
										mAlignmentMatrixGyroscope = AlignmentMatrixGyroShimmer3;
										mOffsetVectorGyroscope = OffsetVectorGyroShimmer3;
										if (mGyroRange==0){
											mSensitivityMatrixGyroscope = SensitivityMatrixGyro250dpsShimmer3;

										} else if (mGyroRange==1){
											mSensitivityMatrixGyroscope = SensitivityMatrixGyro500dpsShimmer3;

										} else if (mGyroRange==2){
											mSensitivityMatrixGyroscope = SensitivityMatrixGyro1000dpsShimmer3;

										} else if (mGyroRange==3){
											mSensitivityMatrixGyroscope = SensitivityMatrixGyro2000dpsShimmer3;

										}
									}
								}
								mTransactionCompleted=true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} 
							else if (mCurrentCommand==SET_MAG_SAMPLING_RATE_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mLSM303MagRate = mTempIntValue;
								mWaitForAck = false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==GET_ACCEL_SAMPLING_RATE_COMMAND){
								mWaitForAck=false;
								mWaitForResponse=true;
								mListofInstructions.remove(0);
							} else if (mCurrentCommand==SET_ACCEL_SAMPLING_RATE_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mLSM303DigitalAccelRate = mTempIntValue;
								mWaitForAck = false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_MPU9150_SAMPLING_RATE_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mMPU9150GyroAccelRate = mTempIntValue;
								mWaitForAck = false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_EXG_REGS_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								byte[] bytearray = mListofInstructions.get(0);
								if (bytearray[1]==EXG_CHIP1){  //0 = CHIP 1
									System.arraycopy(bytearray, 4, mEXG1RegisterArray, 0, 10);
									mEXG1RateSetting = mEXG1RegisterArray[0] & 7;
									mEXG1CH1GainSetting = (mEXG1RegisterArray[3] >> 4) & 7;
									mEXG1CH1GainValue = convertEXGGainSettingToValue(mEXG1CH1GainSetting);
									mEXG1CH2GainSetting = (mEXG1RegisterArray[4] >> 4) & 7;
									mEXG1CH2GainValue = convertEXGGainSettingToValue(mEXG1CH2GainSetting);
									mEXGReferenceElectrode = mEXG1RegisterArray[5] & 0x0f;
								
								} else if (bytearray[1]==EXG_CHIP2){ //1 = CHIP 2
									System.arraycopy(bytearray, 4, mEXG2RegisterArray, 0, 10);
									mEXG2RateSetting = mEXG2RegisterArray[0] & 7;
									mEXG2CH1GainSetting = (mEXG2RegisterArray[3] >> 4) & 7;
									mEXG2CH1GainValue = convertEXGGainSettingToValue(mEXG2CH1GainSetting);
									mEXG2CH2GainSetting = (mEXG2RegisterArray[4] >> 4) & 7;
									mEXG2CH2GainValue = convertEXGGainSettingToValue(mEXG2CH2GainSetting);
								}
								mTransactionCompleted = true;
								mWaitForAck = false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							} else if (mCurrentCommand==SET_SENSORS_COMMAND) {
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mWaitForAck=false;
								mEnabledSensors=tempEnabledSensors;
								byteStack.clear(); // Always clear the packetStack after setting the sensors, this is to ensure a fresh start
								mTransactionCompleted=true;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_MAG_GAIN_COMMAND){
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mWaitForAck = false;
								mMagRange=(int)((byte [])mListofInstructions.get(0))[1];
								if (mDefaultCalibrationParametersMag == true){
									if(mHardwareVersion == HW_ID.SHIMMER_3){
										mAlignmentMatrixMagnetometer = AlignmentMatrixMagShimmer3;
										mOffsetVectorMagnetometer = OffsetVectorMagShimmer3;
										if (mMagRange==1){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag1p3GaShimmer3;
										} else if (mMagRange==2){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag1p9GaShimmer3;
										} else if (mMagRange==3){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag2p5GaShimmer3;
										} else if (mMagRange==4){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag4GaShimmer3;
										} else if (mMagRange==5){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag4p7GaShimmer3;
										} else if (mMagRange==6){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag5p6GaShimmer3;
										} else if (mMagRange==7){
											mSensitivityMatrixMagnetometer = SensitivityMatrixMag8p1GaShimmer3;
										}
									}
								}
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==GET_ACCEL_CALIBRATION_COMMAND || mCurrentCommand==GET_GYRO_CALIBRATION_COMMAND || mCurrentCommand==GET_MAG_CALIBRATION_COMMAND || mCurrentCommand==GET_ALL_CALIBRATION_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_SHIMMER_VERSION_COMMAND_NEW) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==GET_BMP180_CALIBRATION_COEFFICIENTS_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							} else if (mCurrentCommand==GET_SHIMMER_VERSION_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							} else if (mCurrentCommand==GET_EXG_REGS_COMMAND){
								byte[] bytearray = mListofInstructions.get(0);
								mTempChipID = bytearray[1];
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==SET_ECG_CALIBRATION_COMMAND){
								//mGSRRange=mTempIntValue;
								mDefaultCalibrationParametersECG = false;
								OffsetECGLALL=(double)((((byte[])mListofInstructions.get(0))[0]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[1]&0xFF);
								GainECGLALL=(double)((((byte[])mListofInstructions.get(0))[2]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[3]&0xFF);
								OffsetECGRALL=(double)((((byte[])mListofInstructions.get(0))[4]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[5]&0xFF);
								GainECGRALL=(double)((((byte[])mListofInstructions.get(0))[6]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[7]&0xFF);
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mTransactionCompleted = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==SET_EMG_CALIBRATION_COMMAND){
								//mGSRRange=mTempIntValue;
								mDefaultCalibrationParametersEMG = false;
								OffsetEMG=(double)((((byte[])mListofInstructions.get(0))[0]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[1]&0xFF);
								GainEMG=(double)((((byte[])mListofInstructions.get(0))[2]&0xFF)<<8)+(((byte[])mListofInstructions.get(0))[3]&0xFF);
								mTransactionCompleted = true;
								mWaitForAck=false;
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==TOGGLE_LED_COMMAND){
								//mGSRRange=mTempIntValue;
								mTransactionCompleted = true;
								mWaitForAck=false;
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
							}
							else if (mCurrentCommand==GET_BAUD_RATE_COMMAND) {
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if (mCurrentCommand==SET_BAUD_RATE_COMMAND) {
								mTransactionCompleted = true;
								mWaitForAck=false;
								mTimer.cancel(); //cancel the ack timer
								mTimer.purge();
								mBluetoothBaudRate=(int)((byte [])mListofInstructions.get(0))[1];
								mListofInstructions.remove(0);
								mInstructionStackLock=false;
//								reconnect();
							}
							else if(mCurrentCommand==GET_DAUGHTER_CARD_ID_COMMAND){
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if(mCurrentCommand==GET_DIR_COMMAND){
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}
							else if(mCurrentCommand==GET_STATUS_COMMAND){
								mWaitForResponse = true;
								mWaitForAck=false;
								mListofInstructions.remove(0);
							}

						}


					}
				} else if (mWaitForResponse==true && !mStreaming) {
					if (mFirstTime){
						while (availableBytes()!=0){
								int avaible = availableBytes();
								if (bytesToBeRead()){
									tb=readBytes(1);
									String msg = "First Time : " + Arrays.toString(tb);
									printLogDataForDebugging(msg);
								}
							
						}
						
					} else if (availableBytes()!=0){

						tb=readBytes(1);
						mIamAlive = true;
						String msg="";
						//msg = "rxb : " + Arrays.toString(tb);
						//printLogDataForDebugging(msg);
						
						if (tb[0]==FW_VERSION_RESPONSE){
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();

							try {
								Thread.sleep(200);	// Wait to ensure the packet has been fully received
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferInquiry = new byte[6]; 
							bufferInquiry = readBytes(6);
							mFirmwareIdentifier=(int)((bufferInquiry[1]&0xFF)<<8)+(int)(bufferInquiry[0]&0xFF);
//							mFWVersion=(double)((bufferInquiry[3]&0xFF)<<8)+(double)(bufferInquiry[2]&0xFF)+((double)((bufferInquiry[4]&0xFF))/10);
							mFirmwareVersionMajor = (int)((bufferInquiry[3]&0xFF)<<8)+(int)(bufferInquiry[2]&0xFF);
							mFirmwareVersionMinor = ((int)((bufferInquiry[4]&0xFF)));
							mFirmwareVersionInternal=(int)(bufferInquiry[5]&0xFF);
							
//							if (((double)((bufferInquiry[4]&0xFF))/10)==0){
//								mFirmwareVersionParsed = "BtStream " + Double.toString(mFWVersion) + "."+ Integer.toString(mFirmwareVersionRelease);
//							} else {
//								mFirmwareVersionParsed = "BtStream " + Double.toString(mFWVersion) + "."+ Integer.toString(mFirmwareVersionRelease);
//							}
							if(mFirmwareIdentifier==1){ //BTStream
								if((mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1) || (mFirmwareVersionMajor==1 && mFirmwareVersionMinor==2 && mHardwareVersion==HW_ID.SHIMMER_2R))
									mFirmwareVersionCode = 1;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==2)
									mFirmwareVersionCode = 2;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==3)
									mFirmwareVersionCode = 3;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==4)
									mFirmwareVersionCode = 4;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor>=5)
									mFirmwareVersionCode = 5;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==7 && mFirmwareVersionInternal>=3)//need to change to 8
									mFirmwareVersionCode = 7;
								if (mFirmwareVersionMajor==0 && mFirmwareVersionMinor>=8)
									mFirmwareVersionCode = 7;
								
								mFirmwareVersionParsed = "BtStream " + mFirmwareVersionMajor + "." + mFirmwareVersionMinor + "."+ mFirmwareVersionInternal;
							}
							else if(mFirmwareIdentifier==3){ //LogAndStream
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1)
									mFirmwareVersionCode = 3;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==2)
									mFirmwareVersionCode = 4;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor>=3)
									mFirmwareVersionCode = 5;
								if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor>=6)
									mFirmwareVersionCode = 6;
																
								mFirmwareVersionParsed = "LogAndStream " + mFirmwareVersionMajor + "." + mFirmwareVersionMinor + "."+ mFirmwareVersionInternal;
							}

							//Once the version is known update settings accordingly 
							if (mFirmwareVersionCode>=6){
								mTimeStampPacketByteSize =3;
								mTimeStampPacketRawMaxValue = 16777216;
							} 
							else if (mFirmwareVersionCode<6){
								mTimeStampPacketByteSize =2;
								mTimeStampPacketRawMaxValue = 65536;
							}
							
							
							printLogDataForDebugging("FW Version Response Received. FW Code: " + mFirmwareVersionCode);
							msg = "FW Version Response Received: " +mFirmwareVersionParsed;
							printLogDataForDebugging(msg);
							mListofInstructions.remove(0);
							mInstructionStackLock=false;
							mTransactionCompleted=true;
							if (mHardwareVersion == HW_ID.SHIMMER_2R){
								initializeShimmer2R();
							} else if (mHardwareVersion == HW_ID.SHIMMER_3) {
								initializeShimmer3();
							}
							
							if (mCheckIfConnectionisAlive){
								if(mAliveTimer==null){ 
									mAliveTimer = new Timer();
								}
								mAliveTimer.schedule(new checkIfAliveTask(), mAliveStatusPeriod, mAliveStatusPeriod);
							
							}
							
//							readShimmerVersion();
						} else if (tb[0]==BMP180_CALIBRATION_COEFFICIENTS_RESPONSE){
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							
							//get pressure
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							byte[] pressureResoRes = new byte[22]; 
						
							pressureResoRes = readBytes(22);
							mPressureCalRawParams = new byte[23];
							System.arraycopy(pressureResoRes, 0, mPressureCalRawParams, 1, 22);
							mPressureCalRawParams[0] = tb[0];
							retrievepressurecalibrationparametersfrompacket(pressureResoRes,tb[0]);
							msg = "BMP180 Response Received";
							printLogDataForDebugging(msg);
							mInstructionStackLock=false;
						} else if (tb[0]==INQUIRY_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							try {
								Thread.sleep(500);	// Wait to ensure the packet has been fully received
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							List<Byte> buffer = new  ArrayList<Byte>();
							if (!(mHardwareVersion==HW_ID.SHIMMER_3))
							{
								 for (int i = 0; i < 5; i++)
	                                {
	                                    // get Sampling rate, accel range, config setup byte0, num chans and buffer size
	                                    buffer.add(readByte());
	                                }
								 
	                                for (int i = 0; i < (int)buffer.get(3); i++)
	                                {
	                                    // read each channel type for the num channels
	                                	buffer.add(readByte());
	                                }
							}
							else
							{
								  for (int i = 0; i < 8; i++)
	                                {
	                                    // get Sampling rate, accel range, config setup byte0, num chans and buffer size
									  buffer.add(readByte());
	                                }
	                                for (int i = 0; i < (int)buffer.get(6); i++)
	                                {
	                                    // read each channel type for the num channels
	                                	buffer.add(readByte());
	                                }
							}
							byte[] bufferInquiry = new byte[buffer.size()];
							for (int i = 0; i < bufferInquiry.length; i++) {
								bufferInquiry[i] = (byte) buffer.get(i);
							}
								
							msg = "Inquiry Response Received: " + Arrays.toString(bufferInquiry);
							printLogDataForDebugging(msg);
							interpretInqResponse(bufferInquiry);
							inquiryDone();
							mWaitForResponse = false;
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						} else if(tb[0] == GSR_RANGE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferGSRRange = readBytes(1); 
							mGSRRange=bufferGSRRange[0];
							msg = "GSR Range Response Received: " + Arrays.toString(bufferGSRRange);
							printLogDataForDebugging(msg);
							mInstructionStackLock=false;
						} else if(tb[0] == MAG_SAMPLING_RATE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(1); 
							mLSM303MagRate=bufferAns[0];
							msg = "Mag Sampling Rate Response Received: " + Arrays.toString(bufferAns);
							printLogDataForDebugging(msg);
							mInstructionStackLock=false;
						} else if(tb[0] == ACCEL_SAMPLING_RATE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(1); 
							mLSM303DigitalAccelRate=bufferAns[0];
							mInstructionStackLock=false;
						} else if(tb[0] == RWC_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(8); 
							mInstructionStackLock=false;
						} else if(tb[0] == 	EXG_REGS_RESPONSE){
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							try {
								Thread.sleep(300);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferAns = readBytes(11);
							if (mTempChipID==0){
								System.arraycopy(bufferAns, 1, mEXG1RegisterArray, 0, 10);
								// retrieve the gain and rate from the the registers
								mEXG1RateSetting = mEXG1RegisterArray[0] & 7;
								mEXG1CH1GainSetting = (mEXG1RegisterArray[3] >> 4) & 7;
								mEXG1CH1GainValue = convertEXGGainSettingToValue(mEXG1CH1GainSetting);
								mEXG1CH2GainSetting = (mEXG1RegisterArray[4] >> 4) & 7;
								mEXG1CH2GainValue = convertEXGGainSettingToValue(mEXG1CH2GainSetting);
								mEXGReferenceElectrode = mEXG1RegisterArray[5] & 0x0F;
								mEXG1LeadOffCurrentMode = mEXG1RegisterArray[2] & 1;
								mEXG1Comparators = mEXG1RegisterArray[1] & 0x40;								
								mEXGRLDSense = mEXG1RegisterArray[5] & 0x10;
								mEXG1LeadOffSenseSelection = mEXG1RegisterArray[6] & 0x0f;
								mEXGLeadOffDetectionCurrent = (mEXG1RegisterArray[2] >> 2) & 3;
								mEXGLeadOffComparatorTreshold = (mEXG1RegisterArray[2] >> 5) & 7;
							} else if (mTempChipID==1){
								System.arraycopy(bufferAns, 1, mEXG2RegisterArray, 0, 10);						
								mEXG2RateSetting = mEXG2RegisterArray[0] & 7;
								mEXG2CH1GainSetting = (mEXG2RegisterArray[3] >> 4) & 7;
								mEXG2CH1GainValue = convertEXGGainSettingToValue(mEXG2CH1GainSetting);
								mEXG2CH2GainSetting = (mEXG2RegisterArray[4] >> 4) & 7;
								mEXG2CH2GainValue = convertEXGGainSettingToValue(mEXG2CH2GainSetting);
								mEXG2LeadOffCurrentMode = mEXG2RegisterArray[2] & 1;
								mEXG2Comparators = mEXG2RegisterArray[1] & 0x40;
								mEXG2LeadOffSenseSelection = mEXG2RegisterArray[6] & 0x0f;
							}
							if(mEXG1Comparators == 0 && mEXG2Comparators == 0 && mEXG1LeadOffSenseSelection == 0 && mEXG2LeadOffSenseSelection == 0){
								mLeadOffDetectionMode = 0; // Off
							}
							else if(mEXG1LeadOffCurrentMode == mEXG2LeadOffCurrentMode && mEXG1LeadOffCurrentMode == 0){
								mLeadOffDetectionMode = 1; // DC Current
							}
							else if(mEXG1LeadOffCurrentMode == mEXG2LeadOffCurrentMode && mEXG1LeadOffCurrentMode == 1){
								mLeadOffDetectionMode = 2; // AC Current. Not supported yet
							}
							mInstructionStackLock=false;
						} else if(tb[0] == MAG_GAIN_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(1); 
							mMagRange=bufferAns[0];
							mInstructionStackLock=false;
						} else if(tb[0] == LSM303DLHC_ACCEL_HRMODE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(1);
							mInstructionStackLock=false;
						} else if(tb[0] == LSM303DLHC_ACCEL_LPMODE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAns = readBytes(1);
							mInstructionStackLock=false;
						} else if(tb[0]==BUFFER_SIZE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] byteled = readBytes(1);
							mBufferSize = byteled[0] & 0xFF;
							mInstructionStackLock=false;
						} else if(tb[0]==BLINK_LED_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] byteled = readBytes(1);
							mCurrentLEDStatus = byteled[0]&0xFF;
							mInstructionStackLock=false;
						} else if(tb[0]==ACCEL_SENSITIVITY_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferAccelSensitivity = readBytes(1);
							mAccelRange=bufferAccelSensitivity[0];
							if (mDefaultCalibrationParametersAccel == true){
								if (mHardwareVersion != HW_ID.SHIMMER_3){
									if (getAccelRange()==0){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel1p5gShimmer2; 
									} else if (getAccelRange()==1){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel2gShimmer2; 
									} else if (getAccelRange()==2){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel4gShimmer2; 
									} else if (getAccelRange()==3){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel6gShimmer2; 
									}
								} else if(mHardwareVersion == HW_ID.SHIMMER_3){
									if (getAccelRange()==0){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixLowNoiseAccel2gShimmer3;
										mAlignmentMatrixAnalogAccel = AlignmentMatrixLowNoiseAccelShimmer3;
										mOffsetVectorAnalogAccel = OffsetVectorLowNoiseAccelShimmer3;
									} else if (getAccelRange()==1){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixWideRangeAccel4gShimmer3;
										mAlignmentMatrixAnalogAccel = AlignmentMatrixWideRangeAccelShimmer3;
										mOffsetVectorAnalogAccel = OffsetVectorWideRangeAccelShimmer3;
									} else if (getAccelRange()==2){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixWideRangeAccel8gShimmer3;
										mAlignmentMatrixAnalogAccel = AlignmentMatrixWideRangeAccelShimmer3;
										mOffsetVectorAnalogAccel = OffsetVectorWideRangeAccelShimmer3;
									} else if (getAccelRange()==3){
										mSensitivityMatrixAnalogAccel = SensitivityMatrixWideRangeAccel16gShimmer3;
										mAlignmentMatrixAnalogAccel = AlignmentMatrixWideRangeAccelShimmer3;
										mOffsetVectorAnalogAccel = OffsetVectorWideRangeAccelShimmer3;
									}
								}
							}   
							mListofInstructions.remove(0);
							mInstructionStackLock=false;
						} else if(tb[0]==MPU9150_GYRO_RANGE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferGyroSensitivity = readBytes(1);
							mGyroRange=bufferGyroSensitivity[0];
							if (mDefaultCalibrationParametersGyro == true){
								if(mHardwareVersion == HW_ID.SHIMMER_3){
									mAlignmentMatrixGyroscope = AlignmentMatrixGyroShimmer3;
									mOffsetVectorGyroscope = OffsetVectorGyroShimmer3;
									if (mGyroRange==0){
										mSensitivityMatrixGyroscope = SensitivityMatrixGyro250dpsShimmer3;

									} else if (mGyroRange==1){
										mSensitivityMatrixGyroscope = SensitivityMatrixGyro500dpsShimmer3;

									} else if (mGyroRange==2){
										mSensitivityMatrixGyroscope = SensitivityMatrixGyro1000dpsShimmer3;

									} else if (mGyroRange==3){
										mSensitivityMatrixGyroscope = SensitivityMatrixGyro2000dpsShimmer3;
									}
								}
							}   
							mListofInstructions.remove(0);
							mInstructionStackLock=false;
						}else if (tb[0]==SAMPLING_RATE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							if(mStreaming==false) {
								if (mHardwareVersion==HW_ID.SHIMMER_2R || mHardwareVersion==HW_ID.SHIMMER_2){    
									byte[] bufferSR = readBytes(1);
									if (mCurrentCommand==GET_SAMPLING_RATE_COMMAND) { // this is a double check, not necessary 
										double val=(double)(bufferSR[0] & (byte) ACK_COMMAND_PROCESSED);
										mShimmerSamplingRate=1024/val;
									}
								} else if (mHardwareVersion==HW_ID.SHIMMER_3){
									byte[] bufferSR = readBytes(2); //read the sampling rate
									mShimmerSamplingRate = 32768/(double)((int)(bufferSR[0] & 0xFF) + ((int)(bufferSR[1] & 0xFF) << 8));
								}
							}

							msg = "Sampling Rate Response Received: " + Double.toString(mShimmerSamplingRate);
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mListofInstructions.remove(0);
							mInstructionStackLock=false;
						} else if (tb[0]==ACCEL_CALIBRATION_RESPONSE ) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
								try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mWaitForResponse=false;
							byte[] bufferCalibrationParameters = readBytes(21);
							
							mAccelCalRawParams = new byte[22];
							System.arraycopy(bufferCalibrationParameters, 0, mAccelCalRawParams, 1, 21);
							mAccelCalRawParams[0] = tb[0];
							
							int packetType=tb[0];
							retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, packetType);
							msg = "Accel Calibration Response Received";
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						}  else if (tb[0]==ALL_CALIBRATION_RESPONSE ) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
					
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (mHardwareVersion != HW_ID.SHIMMER_3){
								byte[] bufferCalibrationParameters = readBytes(21);
								mAccelCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mAccelCalRawParams, 1, 21);
								mAccelCalRawParams[0] = ACCEL_CALIBRATION_RESPONSE;
								
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, ACCEL_CALIBRATION_RESPONSE);

								//get gyro
								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								bufferCalibrationParameters = readBytes(21);
								mGyroCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mGyroCalRawParams, 1, 21);
								mGyroCalRawParams[0] = GYRO_CALIBRATION_RESPONSE;
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, GYRO_CALIBRATION_RESPONSE);

								//get mag
								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								bufferCalibrationParameters = readBytes(21);
								mMagCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mMagCalRawParams, 1, 21);
								mMagCalRawParams[0] = MAG_CALIBRATION_RESPONSE;
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, MAG_CALIBRATION_RESPONSE);

								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								
								bufferCalibrationParameters = readBytes(4); 
								mEMGCalRawParams = new byte[5];
								System.arraycopy(bufferCalibrationParameters, 0, mEMGCalRawParams, 1, 4);
								mEMGCalRawParams[0] = EMG_CALIBRATION_RESPONSE;
								retrievebiophysicalcalibrationparametersfrompacket( bufferCalibrationParameters,EMG_CALIBRATION_RESPONSE);
								
								bufferCalibrationParameters = readBytes(8);
								
								mECGCalRawParams = new byte[9];
								System.arraycopy(bufferCalibrationParameters, 0, mECGCalRawParams, 1, 8);
								mECGCalRawParams[0] = ECG_CALIBRATION_RESPONSE;
								retrievebiophysicalcalibrationparametersfrompacket( bufferCalibrationParameters,ECG_CALIBRATION_RESPONSE);
								
								mTransactionCompleted=true;
								mInstructionStackLock=false;

							} else {


								byte[] bufferCalibrationParameters =readBytes(21); 
								
								mAccelCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mAccelCalRawParams, 1, 21);
								mAccelCalRawParams[0] = ACCEL_CALIBRATION_RESPONSE;
								
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, ACCEL_CALIBRATION_RESPONSE);

								//get gyro
								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								bufferCalibrationParameters = readBytes(21); 
								
								mGyroCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mGyroCalRawParams, 1, 21);
								mGyroCalRawParams[0] = GYRO_CALIBRATION_RESPONSE;
								
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, GYRO_CALIBRATION_RESPONSE);

								//get mag
								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								bufferCalibrationParameters = readBytes(21); 
								
								mMagCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mMagCalRawParams, 1, 21);
								mMagCalRawParams[0] = MAG_CALIBRATION_RESPONSE;
								
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, MAG_CALIBRATION_RESPONSE);

								//second accel cal params
								try {
									Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								bufferCalibrationParameters = readBytes(21);
								
								mDigiAccelCalRawParams = new byte[22];
								System.arraycopy(bufferCalibrationParameters, 0, mDigiAccelCalRawParams, 1, 21);
								mDigiAccelCalRawParams[0] = LSM303DLHC_ACCEL_CALIBRATION_RESPONSE;
								msg = "All Calibration Response Received";
								printLogDataForDebugging(msg);
								retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, LSM303DLHC_ACCEL_CALIBRATION_RESPONSE);
								mTransactionCompleted=true;
								mInstructionStackLock=false;

							}
						} else if (tb[0]==GYRO_CALIBRATION_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mWaitForResponse=false;
							byte[] bufferCalibrationParameters = readBytes(21);
							mGyroCalRawParams = new byte[22];
							System.arraycopy(bufferCalibrationParameters, 0, mGyroCalRawParams, 1, 21);
							mGyroCalRawParams[0] = tb[0];
							
							int packetType=tb[0];
							retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, packetType);
							msg = "Gyro Calibration Response Received";
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						} else if (tb[0]==MAG_CALIBRATION_RESPONSE ) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferCalibrationParameters = readBytes(21);
							mMagCalRawParams = new byte[22];
							System.arraycopy(bufferCalibrationParameters, 0, mMagCalRawParams, 1, 21);
							mMagCalRawParams[0] = tb[0];
							int packetType=tb[0];
							retrievekinematiccalibrationparametersfrompacket(bufferCalibrationParameters, packetType);
							msg = "Mag Calibration Response Received";
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						} else if(tb[0]==CONFIG_BYTE0_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							
							if (mHardwareVersion==HW_ID.SHIMMER_2R || mHardwareVersion==HW_ID.SHIMMER_2){    
								byte[] bufferConfigByte0 = readBytes(1);
								mConfigByte0 = bufferConfigByte0[0] & 0xFF;
							} else {
								byte[] bufferConfigByte0 = readBytes(4);
								mConfigByte0 = ((long)(bufferConfigByte0[0] & 0xFF) +((long)(bufferConfigByte0[1] & 0xFF) << 8)+((long)(bufferConfigByte0[2] & 0xFF) << 16) +((long)(bufferConfigByte0[3] & 0xFF) << 24));
							}
							msg = "ConfigByte0 response received Response Received";
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						} else if(tb[0]==GET_SHIMMER_VERSION_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferShimmerVersion = new byte[1]; 
							bufferShimmerVersion = readBytes(1);
							mHardwareVersion=(int)bufferShimmerVersion[0];
							mTransactionCompleted=true;
							mInstructionStackLock=false;
//							if (mShimmerVersion == HW_ID.SHIMMER_2R){
//								initializeShimmer2R();
//							} else if (mShimmerVersion == HW_ID.SHIMMER_3) {
//								initializeShimmer3();
//							}
							msg = "Shimmer Version (HW) Response Received: " + Arrays.toString(bufferShimmerVersion);
							printLogDataForDebugging(msg);
							readFWVersion();
						} else if (tb[0]==ECG_CALIBRATION_RESPONSE){
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferCalibrationParameters = new byte[8]; 
							bufferCalibrationParameters = readBytes(4);
															
							mECGCalRawParams = new byte[9];
							System.arraycopy(bufferCalibrationParameters, 0, mECGCalRawParams, 1, 8);
							mECGCalRawParams[0] = ECG_CALIBRATION_RESPONSE;
							//get ecg 
							retrievebiophysicalcalibrationparametersfrompacket( bufferCalibrationParameters,ECG_CALIBRATION_RESPONSE);
							msg = "ECG Calibration Response Received";
							printLogDataForDebugging(msg);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						} else if (tb[0]==EMG_CALIBRATION_RESPONSE){
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							try {
								Thread.sleep(100);	// Due to the nature of the Bluetooth SPP stack a delay has been added to ensure the buffer is filled before it is read
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							byte[] bufferCalibrationParameters = new byte[4]; 
							bufferCalibrationParameters = readBytes(4);
							
							mEMGCalRawParams = new byte[5];
							System.arraycopy(bufferCalibrationParameters, 0, mEMGCalRawParams, 1, 4);
							mEMGCalRawParams[0] = EMG_CALIBRATION_RESPONSE;
							//get EMG
							msg = "EMG Calibration Response Received";
							printLogDataForDebugging(msg);
							retrievebiophysicalcalibrationparametersfrompacket( bufferCalibrationParameters,EMG_CALIBRATION_RESPONSE);
							mTransactionCompleted=true;
							mInstructionStackLock=false;
						}
						else if(tb[0] == BAUD_RATE_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							byte[] bufferBaud = readBytes(1); 
							printLogDataForDebugging(msg);
							mBluetoothBaudRate=bufferBaud[0] & 0xFF;
							mInstructionStackLock=false;
						}
						else if(tb[0] == DAUGHTER_CARD_ID_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							printLogDataForDebugging(msg);
							mExpBoardArray = readBytes(numBytesToReadFromExpBoard+1);
							mInstructionStackLock=false;
						}
						else if(tb[0] == INSTREAM_CMD_RESPONSE) {
							mTimer.cancel(); //cancel the ack timer
							mTimer.purge();
							mWaitForResponse=false;
							mTransactionCompleted=true;
							printLogDataForDebugging(msg);
							byte[] bufferLogCommandType = new byte[2];
							bufferLogCommandType =	readBytes(2);
							System.out.println("Instrucction received (113 = STATUS_RESPONSE) = "+bufferLogCommandType[0]);
							if(bufferLogCommandType[0]==DIR_RESPONSE){
								mDirectoryNameLength = bufferLogCommandType[1];
								byte[] bufferDirectoryName = new byte[mDirectoryNameLength];
								bufferDirectoryName = readBytes(mDirectoryNameLength);
								String tempDirectory = new String(bufferDirectoryName);
								mDirectoryName = tempDirectory;
								System.out.println("Directory Name = "+ mDirectoryName);
							}
							else if(bufferLogCommandType[0]==STATUS_RESPONSE){
								int sensing = bufferLogCommandType[1] & 2;
								if(sensing==2)
									mSensingStatus = true;
								else
									mSensingStatus = false;
								
								int docked = bufferLogCommandType[1] & 1;
								if(docked==1)
									mDockedStatus = true;
								else
									mDockedStatus = false;
								
								mIsSDLogging = ((bufferLogCommandType[1] & 0x08) > 0)? true:false;
					            mIsStreaming = ((bufferLogCommandType[1] & 0x10) > 0)? true:false;

								
								System.out.println("Sensing = "+sensing);
								System.out.println("Sensing status = "+mSensingStatus);
								System.out.println("Docked = "+docked);
								System.out.println("Docked status = "+mDockedStatus);
								
								if (mSensingStatus == false){
									if (mInitialized== false){
										writeRTCCommand();	
									}
								}
								
							}
							mInstructionStackLock=false;
						}
					}
				} 
				
				
				if (mWaitForAck==false && mWaitForResponse == false && mStreaming ==false && availableBytes()!=0 && mFirmwareIdentifier==3) {
					tb=readBytes(1);
					if(tb[0]==ACK_COMMAND_PROCESSED){
						System.out.println("ACK RECEIVED , Connected State!!");
						tb = readBytes(1);
						if (tb[0]==ACK_COMMAND_PROCESSED){
							tb = readBytes(1);
						}
						if(tb[0]==INSTREAM_CMD_RESPONSE){
							System.out.println("INS CMD RESP");
							byte[] command = readBytes(2);
							if(command[0]==DIR_RESPONSE){
								mDirectoryNameLength = command[1];
								byte[] bufferDirectoryName = new byte[mDirectoryNameLength];
								bufferDirectoryName = readBytes(mDirectoryNameLength);
								String tempDirectory = new String(bufferDirectoryName);
								mDirectoryName = tempDirectory;

								System.out.println("DIR RESP : " + mDirectoryName);
							}
							else if(command[0]==STATUS_RESPONSE){
								int sensing = command[1] & 2;
								if(sensing==2)
									mSensingStatus = true;
								else
									mSensingStatus = false;

								int docked = command[1] & 1;
								if(docked==1)
									mDockedStatus = true;
								else
									mDockedStatus = false;

								if (mSensingStatus){
									//flush all the bytes
									while(availableBytes()!=0){
										System.out.println("Throwing away = "+readBytes(1));
									}
									startStreaming();
								}
								
								logAndStreamStatusChanged();
								
								System.out.println("Sensing = "+sensing);
								System.out.println("Sensing status = "+mSensingStatus);
								System.out.println("Docked = "+docked);
								System.out.println("Docked status = "+mDockedStatus);
							}
						}
					}
					
					while(availableBytes()!=0){
						System.out.println("Throwing away = "+readBytes(1));
					}
				}
				
				
				if (mStreaming==true) {
					
					tb = readBytes(1);
					if (tb!=null){
						mByteArrayOutputStream.write(tb[0]);
					} else {
						System.out.print("readbyte null");
					}
					
					

					//If there is a full packet and the subsequent sequence number of following packet
					if (mByteArrayOutputStream.size()>=mPacketSize+2){
						mIamAlive = true;
						byte[] bufferTemp = mByteArrayOutputStream.toByteArray();
						if (bufferTemp[0]==DATA_PACKET && bufferTemp[mPacketSize+1]==DATA_PACKET){
							newPacket = new byte[mPacketSize];
							System.arraycopy(bufferTemp, 1, newPacket, 0, mPacketSize);
							mABQ.add(newPacket);
							//Finally clear the parsed packet from the bytearrayoutputstream, NOTE the last two bytes(seq number of next packet) are added back on after the reset
							//System.out.print("Byte size reset: " + mByteArrayOutputStream.size() + "\n");
							mByteArrayOutputStream.reset();
							mByteArrayOutputStream.write(bufferTemp[mPacketSize+1]);
							//System.out.print(bufferTemp[mPacketSize+1] + "\n");
							
						} else if (bufferTemp[0]==DATA_PACKET && bufferTemp[mPacketSize+1]==ACK_COMMAND_PROCESSED){
							if (mByteArrayOutputStream.size()>mPacketSize+2){
								if (bufferTemp[mPacketSize+2]==DATA_PACKET){
									newPacket = new byte[mPacketSize];
									System.arraycopy(bufferTemp, 1, newPacket, 0, mPacketSize);
									mABQ.add(newPacket);
									//Finally clear the parsed packet from the bytearrayoutputstream, NOTE the last two bytes(seq number of next packet) are added back on after the reset
									mByteArrayOutputStream.reset();
									mByteArrayOutputStream.write(bufferTemp[mPacketSize+2]);
									System.out.print(bufferTemp[mPacketSize+2] + "\n");
									
									if (mCurrentCommand==SET_BLINK_LED){
										System.out.println("LED COMMAND ACK RECEIVED");
										mCurrentLEDStatus=(int)((byte[])mListofInstructions.get(0))[1];
										mListofInstructions.remove(0);
										mTimer.cancel(); //cancel the ack timer
										mTimer.purge();
										mWaitForAck=false;
										mTransactionCompleted = true;
										mInstructionStackLock=false;
									} 
									
									
								} else if (mFirmwareIdentifier == FW_ID.SHIMMER3.LOGANDSTREAM && bufferTemp[mPacketSize+2]==INSTREAM_CMD_RESPONSE){ //this is for logandstream stupport, command is trasmitted and ack received
									System.out.println("COMMAND TXed and ACK RECEIVED IN STREAM");
									System.out.println("INS CMD RESP");
									byte[] command = readBytes(2);
									if(command[0]==DIR_RESPONSE){
										int mDirectoryNameLength = command[1];
										byte[] bufferDirectoryName = new byte[mDirectoryNameLength];
										bufferDirectoryName = readBytes(mDirectoryNameLength);
										String tempDirectory = new String(bufferDirectoryName);
										mDirectoryName = tempDirectory;

										System.out.println("DIR RESP : " + mDirectoryName);
									}
									else if(command[0]==STATUS_RESPONSE){
										int sensing = command[1] & 2;
										if(sensing==2)
											mSensingStatus = true;
										else
											mSensingStatus = false;

										int docked = command[1] & 1;
										if(docked==1)
											mDockedStatus = true;
										else
											mDockedStatus = false;

										System.out.println("Sensing = "+sensing);
										System.out.println("Sensing status = "+mSensingStatus);
										System.out.println("Docked = "+docked);
										System.out.println("Docked status = "+mDockedStatus);
									}
									
									mWaitForAck=false;
									mTransactionCompleted = true;   
									mTimer.cancel(); //cancel the ack timer
									mTimer.purge();
									mListofInstructions.remove(0);
									mInstructionStackLock=false;
									newPacket = new byte[mPacketSize];
									System.arraycopy(bufferTemp, 1, newPacket, 0, mPacketSize);
									mABQ.add(newPacket);
									mByteArrayOutputStream.reset();
								} else {
									System.out.print("?? \n");
								}
							} 
							if (mByteArrayOutputStream.size()>mPacketSize+2){ //throw the first byte away
								byte[] bTemp = mByteArrayOutputStream.toByteArray();
								mByteArrayOutputStream.reset();
								mByteArrayOutputStream.write(bTemp, 1, bTemp.length-1); //this will throw the first byte away
								System.out.print("Throw Byte \n");
							}
							
						} else {//throw the first byte away
							byte[] bTemp = mByteArrayOutputStream.toByteArray();
							mByteArrayOutputStream.reset();
							mByteArrayOutputStream.write(bTemp, 1, bTemp.length-1); //this will throw the first byte away
							System.out.print("Throw Byte \n");
						}
					} 
				}
			}
		}
	}
	
	private byte[] convertstacktobytearray(Stack<Byte> b,int packetSize) {
		byte[] returnByte=new byte[packetSize];
		b.remove(0); //remove the Data Packet identifier 
		for (int i=0;i<packetSize;i++) {
			returnByte[packetSize-1-i]=(byte) b.pop();
		}
		return returnByte;
	}
	
	protected void startResponseTimer(int duration) {
		// TODO Auto-generated method stub
		responseTimer(duration);
	}

	public synchronized void responseTimer(int seconds) {
		if (mTimer!=null) {
			mTimer.cancel();
			mTimer.purge();
		}
		printLogDataForDebugging("Waiting for ack/response for command: " + Integer.toString(mCurrentCommand));
		mTimer = new Timer();
		mTimer.schedule(new responseTask(), seconds*1000);
	}

	class responseTask extends TimerTask {
		public void run() {
			{
				if (mCurrentCommand==GET_FW_VERSION_COMMAND){
					printLogDataForDebugging("FW Response Timeout");
					//					mFWVersion=0.1;
					mFirmwareVersionMajor=0;
					mFirmwareVersionMinor=1;
					mFirmwareVersionInternal=0;
					mFirmwareVersionCode=0;
					mFirmwareVersionParsed="BoilerPlate 0.1.0";
					mHardwareVersion = HW_ID.SHIMMER_2R; // on Shimmer2r has
					/*Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
          	        Bundle bundle = new Bundle();
          	        bundle.putString(TOAST, "Firmware Version: " +mFirmwareVersionParsed);
          	        msg.setData(bundle);*/
					if (!mDummy){
						//mHandler.sendMessage(msg);
					}
					mWaitForAck=false;
					mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment 
					mTimer.cancel(); //Terminate the timer thread
					mTimer.purge();
					mFirstTime=false;
					mListofInstructions.remove(0);
					mInstructionStackLock=false;
					initializeBoilerPlate();
				} else if(mCurrentCommand==GET_SAMPLING_RATE_COMMAND && mInitialized==false){
					printLogDataForDebugging("Get Sampling Rate Timeout");
					mWaitForAck=false;
					mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment 
					mTimer.cancel(); //Terminate the timer thread
					mTimer.purge();
					mFirstTime=false;
					mListofInstructions.remove(0);
					mInstructionStackLock=false;
					mFirstTime=false;
				} else if(mCurrentCommand==GET_SHIMMER_VERSION_COMMAND_NEW){ //in case the new command doesn't work, try the old command
					printLogDataForDebugging("Shimmer Version Response Timeout. Trying the old version command");
					mWaitForAck=false;
					mTransactionCompleted=true; 
					mTimer.cancel(); //Terminate the timer thread
					mTimer.purge();
					mFirstTime=false;
					mListofInstructions.remove(0);
					mInstructionStackLock=false;
					readShimmerVersionDepracated();
				}
				else if(mCurrentCommand==GET_STATUS_COMMAND){
					// If the command fails to get a response, the API should assume that the connection has been lost and close the serial port cleanly.
					System.out.println("Command " + Integer.toString(mCurrentCommand) +" failed");
					mTimer.cancel(); //Terminate the timer thread
					mTimer.purge();
					mWaitForAck=false;
					mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment
					mInstructionStackLock=false;
					
					if (mStreaming && getPacketReceptionRate()<100){
						mListofInstructions.clear();
						printLogDataForDebugging("Response not received for Get_Status_Command. Loss bytes detected.");
					} else if(!mStreaming) {
						//CODE TO BE USED
						printLogDataForDebugging("Command " + Integer.toString(mCurrentCommand) +" failed; Killing Connection. Packet RR:  " + Double.toString(getPacketReceptionRate()));
						if (mWaitForResponse){
							printLogDataForDebugging("Response not received");
							sendStatusMSGtoUI("Connection lost." + mMyBluetoothAddress);
						}
						stop(); //If command fail exit device
					}
				}
				else if(mCurrentCommand==GET_DIR_COMMAND){
					// If the command fails to get a response, the API should assume that the connection has been lost and close the serial port cleanly.

					System.out.println("Command " + Integer.toString(mCurrentCommand) +" failed");
					mTimer.cancel(); //Terminate the timer thread
					mTimer.purge();
					mWaitForAck=false;
					mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment 
					mInstructionStackLock=false;
					if (mStreaming && getPacketReceptionRate()<100){
						printLogDataForDebugging("Response not received for Get_Dir_Command. Loss bytes detected.");
						mListofInstructions.clear();
					} else  if(!mStreaming){
						//CODE TO BE USED
						printLogDataForDebugging("Command " + Integer.toString(mCurrentCommand) +" failed; Killing Connection  ");
						if (mWaitForResponse){
							printLogDataForDebugging("Response not received");
							sendStatusMSGtoUI("Connection lost." + mMyBluetoothAddress);
						}
						stop(); //If command fail exit device
					}
				}
				else {
					
					if(!mStreaming){
						printLogDataForDebugging("Command " + Integer.toString(mCurrentCommand) +" failed; Killing Connection  ");
						if (mWaitForResponse){
							printLogDataForDebugging("Response not received");
							sendStatusMSGtoUI("Response not received, please reset Shimmer Device." + mMyBluetoothAddress);
						}
						mWaitForAck=false;
						mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment 
						stop(); //If command fail exit device 
					} else {
						printLogDataForDebugging("Command " + Integer.toString(mCurrentCommand) +" failed;");
						mWaitForAck=false;
						mTransactionCompleted=true; //should be false, so the driver will know that the command has to be executed again, this is not supported at the moment 
						mInstructionStackLock=false;
					}
				}
			}
		}
	}
	
	//endregion
	
	
	protected void reset(){
		mFirmwareVersionCode=0;
		//		protected double mFWVersion;
		mFirmwareVersionMajor = 0;
		mFirmwareVersionMinor = 0;
		mFirmwareVersionInternal = 0;
		mFirmwareIdentifier = 0;
		mFirmwareVersionParsed="";
		
	}

	//region --------- INITIALIZE SHIMMER FUNCTIONS --------- 
	
	protected synchronized void initialize() {	    	//See two constructors for Shimmer
		//InstructionsThread instructionsThread = new InstructionsThread();
		//instructionsThread.start();
		reset();
		if (mTimerToReadStatus!=null) {
			mTimerToReadStatus.cancel();
			mTimerToReadStatus.purge();
		}
		
		if (mTimer!=null){
			mTimer.cancel();
			mTimer.purge();
		}
		mInstructionStackLock = false;
		dummyreadSamplingRate(); // it actually acts to clear the write buffer
		readShimmerVersion();
		//readFWVersion();
		//mShimmerVersion=4;

	}

	public void initializeBoilerPlate(){
		readSamplingRate();
		readConfigByte0();
		readCalibrationParameters("Accelerometer");
		readCalibrationParameters("Magnetometer");
		readCalibrationParameters("Gyroscope");
		if (mSetupDevice==true && mHardwareVersion!=4){
			writeAccelRange(mAccelRange);
			writeGSRRange(mGSRRange);
			writeSamplingRate(mShimmerSamplingRate);	
			writeEnabledSensors(mSetEnabledSensors);
			setContinuousSync(mContinousSync);
		} else {
			inquiry();
		}
	}
	
	/**
	 * By default once connected no low power modes will be enabled. Low power modes should be enabled post connection once the MSG_STATE_FULLY_INITIALIZED is sent 
	 */
	private void initializeShimmer2R(){ 
		readSamplingRate();
		readMagSamplingRate();
		writeBufferSize(1);
		readBlinkLED();
		readConfigByte0();
		readCalibrationParameters("All");
		if (mSetupDevice==true){
			writeMagRange(mMagRange); //set to default Shimmer mag gain
			writeAccelRange(mAccelRange);
			writeGSRRange(mGSRRange);
			writeSamplingRate(mShimmerSamplingRate);	
			writeEnabledSensors(mSetEnabledSensors);
			setContinuousSync(mContinousSync);
		} else {
			if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
			
			} else {
				readMagRange();
			}
			inquiry();
		}
	}

	private void initializeShimmer3(){
		readSamplingRate();
		readMagRange();
		readAccelRange();
		readGyroRange();
		readAccelSamplingRate();
		readBlinkLED();
		readCalibrationParameters("All");
		readpressurecalibrationcoefficients();
		readEXGConfigurations(1);
		readEXGConfigurations(2);
		if (mFirmwareVersionCode>=7){
			writeDisableBattTXFreq();
		}
		if (mFirmwareVersionCode>=6 && mFirmwareIdentifier == FW_ID.SHIMMER3.LOGANDSTREAM){
			//readRWCCommand();
			//writeRWCCommand();
			readStatusLogAndStream();
		}
		//enableLowPowerMag(mLowPowerMag);
		if (mSetupDevice==true){
			//writeAccelRange(mDigitalAccelRange);
			if (mSetupEXG){
				writeEXGConfiguration(mEXG1RegisterArray,1);
				writeEXGConfiguration(mEXG2RegisterArray,2);
				mSetupEXG = false;
			}
			writeGSRRange(mGSRRange);
			writeAccelRange(mAccelRange);
			writeGyroRange(mGyroRange);
			writeMagRange(mMagRange);
			writeSamplingRate(mShimmerSamplingRate);	
			writeInternalExpPower(1);
//			setContinuousSync(mContinousSync);
			writeEnabledSensors(mSetEnabledSensors); //this should always be the last command
		} else {
			inquiry();
		}
		
		
		if(mFirmwareIdentifier==3){ // if shimmer is using LogAndStream FW, read its status perdiocally
			if (mTimerToReadStatus!=null) {
				mTimerToReadStatus.cancel();
				mTimerToReadStatus.purge();
				
				
			}
			printLogDataForDebugging("Waiting for ack/response for command: " + Integer.toString(mCurrentCommand));
			mTimerToReadStatus = new Timer();
			mTimerToReadStatus.schedule(new readStatusTask(), mReadStatusPeriod, mReadStatusPeriod);
		}
	}
	
	//endregion

	
	//region  --------- START/STOP STREAMING FUNCTIONS --------- 
	
	public void startStreaming() {
		//mCurrentLEDStatus=-1;	
		//provides a callback for users to initialize their algorithms when start streaming is called
		if (mDataProcessing!=null){
			mDataProcessing.InitializeProcessData();
		} 	
		else {
			//do nothing
		}
		
		if(mFirmwareIdentifier==3){ // if shimmer is using LogAndStream FW, stop reading its status perdiocally
			if(mTimerToReadStatus!=null){
				mTimerToReadStatus.cancel();
				mTimerToReadStatus.purge();
				mTimerToReadStatus = null;
			}
		}
		
		mPacketLossCount = 0;
		mPacketReceptionRate = 100;
		mFirstTimeCalTime=true;
		mLastReceivedCalibratedTimeStamp = -1;
		mSync=true; // a backup sync done every time you start streaming
		mByteArrayOutputStream.reset();
		mListofInstructions.add(new byte[]{START_STREAMING_COMMAND});
	}
	
	public void startDataLogAndStreaming(){
		if(mFirmwareIdentifier==3){ // if shimmer is using LogAndStream FW, stop reading its status perdiocally

			if (mDataProcessing!=null){
				mDataProcessing.InitializeProcessData();
			} 	
			else {
				//do nothing
			}


			if(mTimerToReadStatus!=null){
				mTimerToReadStatus.cancel();
				mTimerToReadStatus.purge();
				mTimerToReadStatus = null;
			}

			mPacketLossCount = 0;
			mPacketReceptionRate = 100;
			mFirstTimeCalTime=true;
			mLastReceivedCalibratedTimeStamp = -1;
			mSync=true; // a backup sync done every time you start streaming
			mListofInstructions.add(new byte[]{START_SDBT_COMMAND});
		}
	}
	
	
	public void stopStreaming() {
		mListofInstructions.add(new byte[]{STOP_STREAMING_COMMAND});
		if(mFirmwareIdentifier==3){ // if shimmer is using LogAndStream FW, read its status perdiocally
			if(mTimerToReadStatus==null){ 
				mTimerToReadStatus = new Timer();
			}
			mTimerToReadStatus.schedule(new readStatusTask(), mReadStatusPeriod, mReadStatusPeriod);
			}
		}
	
	
	//endregion
	
	
	//region --------- READ FUNCTIONS --------- 
	
	public void readShimmerVersion() {
		mDummy=false;//false
//		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
//			mShimmerVersion = HW_ID.SHIMMER_2R; // on Shimmer2r has 
			
//		} else if (mFWVersion!=1.2){
			mListofInstructions.add(new byte[]{GET_SHIMMER_VERSION_COMMAND_NEW});
//		} else {
//			mListofInstructions.add(new byte[]{GET_SHIMMER_VERSION_COMMAND});
//		}
	}
	
	@Deprecated
	public void readShimmerVersionDepracated(){
		mListofInstructions.add(new byte[]{GET_SHIMMER_VERSION_COMMAND});
	}
	
	/**
	 * The reason for this is because sometimes the 1st response is not received by the phone
	 */
	protected void dummyreadSamplingRate() {
		mDummy=true;
		mListofInstructions.add(new byte[]{GET_SAMPLING_RATE_COMMAND});
	}

	/**
	 * This reads the configuration of a chip from the EXG board
	 * @param chipID Chip id can either be 1 or 2
	 */
	public void readEXGConfigurations(int chipID){
		if ((mFirmwareVersionInternal >=8 && mFirmwareVersionCode==2) || mFirmwareVersionCode>2){
			if (chipID==1 || chipID==2){
				mListofInstructions.add(new byte[]{GET_EXG_REGS_COMMAND,(byte)(chipID-1),0,10});
			}
		}
	}

	public void readpressurecalibrationcoefficients() {
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			if (mFirmwareVersionCode>1){
				mListofInstructions.add(new byte[]{ GET_BMP180_CALIBRATION_COEFFICIENTS_COMMAND});
			}
		}
	}

	
	/**
	 * @param sensor is a string value that defines the sensor. Accepted sensor values are "Accelerometer","Gyroscope","Magnetometer","ECG","EMG","All"
	 */
	public void readCalibrationParameters(String sensor) {
	
			if (!mInitialized){
				if (mFirmwareVersionCode==1 && mFirmwareVersionInternal==0  && mHardwareVersion!=3) {
					//mFirmwareVersionParsed="BoilerPlate 0.1.0";
					/*Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
          	        Bundle bundle = new Bundle();
          	        bundle.putString(TOAST, "Firmware Version: " +mFirmwareVersionParsed);
          	        msg.setData(bundle);
          	        mHandler.sendMessage(msg);*/
				}	
			}
			if (sensor.equals("Accelerometer")) {
				mListofInstructions.add(new byte[]{GET_ACCEL_CALIBRATION_COMMAND});
			}
			else if (sensor.equals("Gyroscope")) {
				mListofInstructions.add(new byte[]{GET_GYRO_CALIBRATION_COMMAND});
			}
			else if (sensor.equals("Magnetometer")) {
				mListofInstructions.add(new byte[]{GET_MAG_CALIBRATION_COMMAND});
			}
			else if (sensor.equals("All")){
				mListofInstructions.add(new byte[]{GET_ALL_CALIBRATION_COMMAND});
			} 
			else if (sensor.equals("ECG")){
				mListofInstructions.add(new byte[]{GET_ECG_CALIBRATION_COMMAND});
			} 
			else if (sensor.equals("EMG")){
				mListofInstructions.add(new byte[]{GET_EMG_CALIBRATION_COMMAND});
			}
		
	}
	
	public void readSamplingRate() {
		mListofInstructions.add(new byte[]{GET_SAMPLING_RATE_COMMAND});
	}
	
	public void readGSRRange() {
		mListofInstructions.add(new byte[]{GET_GSR_RANGE_COMMAND});
	}

	public void readAccelRange() {
		mListofInstructions.add(new byte[]{GET_ACCEL_SENSITIVITY_COMMAND});
	}

	public void readGyroRange() {
		mListofInstructions.add(new byte[]{GET_MPU9150_GYRO_RANGE_COMMAND});
	}

	public void readBufferSize() {
		mListofInstructions.add(new byte[]{GET_BUFFER_SIZE_COMMAND});
	}

	public void readMagSamplingRate() {
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{GET_MAG_SAMPLING_RATE_COMMAND});
		}
	}

	/**
	 * Used to retrieve the data rate of the Accelerometer on Shimmer 3
	 */
	public void readAccelSamplingRate() {
		if (mHardwareVersion!=3){
		} else {
			mListofInstructions.add(new byte[]{GET_ACCEL_SAMPLING_RATE_COMMAND});
		}
	}

	public void readMagRange() {
		mListofInstructions.add(new byte[]{GET_MAG_GAIN_COMMAND});
	}

	public void readBlinkLED() {
		mListofInstructions.add(new byte[]{GET_BLINK_LED});
	}
	
	public void readECGCalibrationParameters() {
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{GET_ECG_CALIBRATION_COMMAND});
		}
	}

	public void readEMGCalibrationParameters() {
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{GET_EMG_CALIBRATION_COMMAND});
		}
	}
	
	public void readBaudRate(){
		if(mFirmwareVersionCode>=5){ 
			mListofInstructions.add(new byte[]{GET_BAUD_RATE_COMMAND});
		}
	}
	
	/**
	 * Read the number of bytes specified starting in the offset from the expansion board attached to the Shimmer Device
	 * @param numBytes number of bytes to be read. there can be read up to 256 bytes
	 * @param offset point from where the function starts to read
	 */
	public void readExpansionBoardByBytes(int numBytes, int offset){
		if(mFirmwareVersionCode>=5){ 
			if(numBytes+offset<=256){
				numBytesToReadFromExpBoard = numBytes;
				mListofInstructions.add(new byte[]{GET_DAUGHTER_CARD_ID_COMMAND, (byte) numBytes, (byte) offset});
			}
		}
	}

	public void readExpansionBoardID(){
		if(mFirmwareVersionCode>=5){ 
			numBytesToReadFromExpBoard=3;
			int offset=0;
			mListofInstructions.add(new byte[]{GET_DAUGHTER_CARD_ID_COMMAND, (byte) numBytesToReadFromExpBoard, (byte) offset});
		}
	}
	
	public void readDirectoryName(){
		if(mFirmwareIdentifier==3){ // check if Shimmer is using LogAndStream firmware
			mListofInstructions.add(new byte[]{GET_DIR_COMMAND});
		}
	}
	
	public void readStatusLogAndStream(){
		if(mFirmwareIdentifier==3){ // check if Shimmer is using LogAndStream firmware
			mListofInstructions.add(new byte[]{GET_STATUS_COMMAND});
			System.out.println("Instrucction added to the list");
		}
	}

	public void readConfigByte0() {
		mListofInstructions.add(new byte[]{GET_CONFIG_BYTE0_COMMAND});
	}
	
	public void readFWVersion() {
		mDummy=false;//false
		mListofInstructions.add(new byte[]{GET_FW_VERSION_COMMAND});
	}
	
	/**
	 * Class used to read perdiocally the shimmer status when LogAndStream FW is installed
	 */
	public class readStatusTask extends TimerTask{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			mListofInstructions.add(new byte[]{GET_STATUS_COMMAND});
		}
		
	}
	
	/**
	 * @author Lim
	 * Used to check if the connection is alive 
	 */
	private class checkIfAliveTask extends TimerTask{

		@Override
		public void run() {
			if (!mIamAlive){
				mCountDeadConnection++;
				writeLEDCommand(0);
				if (mCountDeadConnection>5){
					setState(STATE_NONE);
				}
			} else {
				mCountDeadConnection = 0;
				mIamAlive=false;
			}
		}

	}
	
	
	
	//endregion
	
	
	//region --------- WRITE FUNCTIONS --------- 
	
	
	/**
	 * writeGyroSamplingRate(range) sets the GyroSamplingRate on the Shimmer (version 3) to the value of the input range.
	 * @param rate it is a value between 0 and 255; 6 = 1152Hz, 77 = 102.56Hz, 255 = 31.25Hz
	 */
	private void writeGyroSamplingRate(int rate) {
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			mTempIntValue=rate;
			mListofInstructions.add(new byte[]{SET_MPU9150_SAMPLING_RATE_COMMAND, (byte)rate});
		}
	}
	
	/**
	 * writeMagSamplingRate(range) sets the MagSamplingRate on the Shimmer to the value of the input range.
	 * @param rate for Shimmer 2 it is a value between 1 and 6; 0 = 0.5 Hz; 1 = 1.0 Hz; 2 = 2.0 Hz; 3 = 5.0 Hz; 4 = 10.0 Hz; 5 = 20.0 Hz; 6 = 50.0 Hz, for Shimmer 3 it is a value between 0-7; 0 = 0.75Hz; 1 = 1.5Hz; 2 = 3Hz; 3 = 7.5Hz; 4 = 15Hz ; 5 = 30 Hz; 6 = 75Hz ; 7 = 220Hz 
	 * 
	 * */
	private void writeMagSamplingRate(int rate) {
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mTempIntValue=rate;
			mListofInstructions.add(new byte[]{SET_MAG_SAMPLING_RATE_COMMAND, (byte)rate});
		}
	}
	
	/**
	 * writeAccelSamplingRate(range) sets the AccelSamplingRate on the Shimmer (version 3) to the value of the input range.
	 * @param rate it is a value between 1 and 7; 1 = 1 Hz; 2 = 10 Hz; 3 = 25 Hz; 4 = 50 Hz; 5 = 100 Hz; 6 = 200 Hz; 7 = 400 Hz
	 */
	private void writeAccelSamplingRate(int rate) {
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			mTempIntValue=rate;
			mListofInstructions.add(new byte[]{SET_ACCEL_SAMPLING_RATE_COMMAND, (byte)rate});
		}
	}
	
	/**
	 * Transmits a command to the Shimmer device to enable the sensors. To enable multiple sensors an or operator should be used (e.g. writeEnabledSensors(SENSOR_ACCEL|SENSOR_GYRO|SENSOR_MAG)). Command should not be used consecutively. Valid values are SENSOR_ACCEL, SENSOR_GYRO, SENSOR_MAG, SENSOR_ECG, SENSOR_EMG, SENSOR_GSR, SENSOR_EXP_BOARD_A7, SENSOR_EXP_BOARD_A0, SENSOR_BRIDGE_AMP and SENSOR_HEART.
    SENSOR_BATT
	 * @param enabledSensors e.g SENSOR_ACCEL|SENSOR_GYRO|SENSOR_MAG
	 */
	public void writeEnabledSensors(long enabledSensors) {
		
		if (!sensorConflictCheck(enabledSensors)){ //sensor conflict check
		
		} else {
			enabledSensors=generateSensorBitmapForHardwareControl(enabledSensors);
			tempEnabledSensors=enabledSensors;

			byte secondByte=(byte)((enabledSensors & 65280)>>8);
			byte firstByte=(byte)(enabledSensors & 0xFF);

			//write(new byte[]{SET_SENSORS_COMMAND,(byte) lowByte, highByte});
			if (mHardwareVersion == HW_ID.SHIMMER_3){
				byte thirdByte=(byte)((enabledSensors & 16711680)>>16);

				mListofInstructions.add(new byte[]{SET_SENSORS_COMMAND,(byte) firstByte,(byte) secondByte,(byte) thirdByte});
			} else {
				mListofInstructions.add(new byte[]{SET_SENSORS_COMMAND,(byte) firstByte,(byte) secondByte});
			}
			inquiry();
			
		}
	}
		
	/**
	 * writePressureResolution(range) sets the resolution of the pressure sensor on the Shimmer3
	 * @param settinge Numeric value defining the desired resolution of the pressure sensor. Valid range settings are 0 (low), 1 (normal), 2 (high), 3 (ultra high)
	 * 
	 * */
	public void writePressureResolution(int setting) {
		if (mHardwareVersion==HW_ID.SHIMMER_3){
			mListofInstructions.add(new byte[]{SET_BMP180_PRES_RESOLUTION_COMMAND, (byte)setting});
		}
	}
	
	/**
	 * 
	 * 
	 */
	private void writeDisableBattTXFreq() {
		if (mFirmwareVersionCode==7){
			byte[] freq={SET_VBATT_FREQ_COMMAND,0,0,0,0};
			mListofInstructions.add(freq);
		}
		
	}
	
	private void readRTCCommand(){
		mListofInstructions.add(new byte[]{GET_ACCEL_SENSITIVITY_COMMAND});
	}
	
	private void writeRTCCommand(){
		byte[] bytearray=ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
		ArrayUtils.reverse(bytearray);
		byte[] bytearraycommand= new byte[9];
		bytearraycommand[0]=SET_RWC_COMMAND;
		System.arraycopy(bytearray, 0, bytearraycommand, 1, 8);
		mListofInstructions.add(bytearraycommand);
	}

	/**
	 * writeAccelRange(range) sets the Accelerometer range on the Shimmer to the value of the input range. When setting/changing the accel range, please ensure you have the correct calibration parameters. Note that the Shimmer device can only carry one set of accel calibration parameters at a single time.
	 * @param range is a numeric value defining the desired accelerometer range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 */
	public void writeAccelRange(int range) {
		mListofInstructions.add(new byte[]{SET_ACCEL_SENSITIVITY_COMMAND, (byte)range});
		mAccelRange=(int)range;
		
	}

	/**
	 * writeGyroRange(range) sets the Gyroscope range on the Shimmer3 to the value of the input range. When setting/changing the range, please ensure you have the correct calibration parameters.
	 * @param range is a numeric value defining the desired gyroscope range. 
	 */
	public void writeGyroRange(int range) {
		if (mHardwareVersion==HW_ID.SHIMMER_3){
			mListofInstructions.add(new byte[]{SET_MPU9150_GYRO_RANGE_COMMAND, (byte)range});
			mGyroRange=(int)range;
		}
	}

	/**
	 * @param rate Defines the sampling rate to be set (e.g.51.2 sets the sampling rate to 51.2Hz). User should refer to the document Sampling Rate Table to see all possible values.
	 */
	public void writeSamplingRate(double rate) {
		if (mInitialized=true) {
			setShimmerSamplingRate(rate);
			if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){

				writeMagSamplingRate(mShimmer2MagRate);
				
				int samplingByteValue = (int) (1024/mShimmerSamplingRate); //the equivalent hex setting
				mListofInstructions.add(new byte[]{SET_SAMPLING_RATE_COMMAND, (byte)Math.rint(samplingByteValue), 0x00});
			} else if (mHardwareVersion==HW_ID.SHIMMER_3) {
	
				writeMagSamplingRate(mLSM303MagRate);
				writeAccelSamplingRate(mLSM303DigitalAccelRate);
				writeGyroSamplingRate(mMPU9150GyroAccelRate);
				
				int samplingByteValue = (int) (32768/mShimmerSamplingRate);
				mListofInstructions.add(new byte[]{SET_SAMPLING_RATE_COMMAND, (byte)(samplingByteValue&0xFF), (byte)((samplingByteValue>>8)&0xFF)});
			}
		}
		
//		if (mInitialized=true) {
//
//			if (mShimmerVersion==HW_ID.SHIMMER_2 || mShimmerVersion==HW_ID.SHIMMER_2R){
//				if (!mLowPowerMag){
//					if (rate<=10) {
//						writeMagSamplingRate(4);
//					} else if (rate<=20) {
//						writeMagSamplingRate(5);
//					} else {
//						writeMagSamplingRate(6);
//					}
//				} else {
//					writeMagSamplingRate(4);
//				}
//				rate=1024/rate; //the equivalent hex setting
//				mListofInstructions.add(new byte[]{SET_SAMPLING_RATE_COMMAND, (byte)Math.rint(rate), 0x00});
//			} else if (mShimmerVersion==HW_ID.SHIMMER_3) {
//				if (!mLowPowerMag){
//					if (rate<=1) {
//						writeMagSamplingRate(1);
//					} else if (rate<=15) {
//						writeMagSamplingRate(4);
//					} else if (rate<=30){
//						writeMagSamplingRate(5);
//					} else if (rate<=75){
//						writeMagSamplingRate(6);
//					} else {
//						writeMagSamplingRate(7);
//					}
//				} else {
//					if (rate >=10){
//						writeMagSamplingRate(4);
//					} else {
//						writeMagSamplingRate(1);
//					}
//				}
//
//				if (!mLowPowerAccelWR){
//					if (rate<=1){
//						writeAccelSamplingRate(1);
//					} else if (rate<=10){
//						writeAccelSamplingRate(2);
//					} else if (rate<=25){
//						writeAccelSamplingRate(3);
//					} else if (rate<=50){
//						writeAccelSamplingRate(4);
//					} else if (rate<=100){
//						writeAccelSamplingRate(5);
//					} else if (rate<=200){
//						writeAccelSamplingRate(6);
//					} else {
//						writeAccelSamplingRate(7);
//					}
//				}
//				else {
//					if (rate>=10){
//						writeAccelSamplingRate(2);
//					} else {
//						writeAccelSamplingRate(1);
//					}
//				}
//
//				if (!mLowPowerGyro){
//					if (rate<=51.28){
//						writeGyroSamplingRate(0x9B);
//					} else if (rate<=102.56){
//						writeGyroSamplingRate(0x4D);
//					} else if (rate<=129.03){
//						writeGyroSamplingRate(0x3D);
//					} else if (rate<=173.91){
//						writeGyroSamplingRate(0x2D);
//					} else if (rate<=205.13){
//						writeGyroSamplingRate(0x26);
//					} else if (rate<=258.06){
//						writeGyroSamplingRate(0x1E);
//					} else if (rate<=533.33){
//						writeGyroSamplingRate(0xE);
//					} else {
//						writeGyroSamplingRate(6);
//					}
//				}
//				else {
//					writeGyroSamplingRate(0xFF);
//				}
//
//				
//
//				int samplingByteValue = (int) (32768/rate);
//				mListofInstructions.add(new byte[]{SET_SAMPLING_RATE_COMMAND, (byte)(samplingByteValue&0xFF), (byte)((samplingByteValue>>8)&0xFF)});
//
//
//
//
//			}
//		}
	}
	
	/**
	 * Only supported on Shimmer3, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device
	 * This function set the treshold of the ExG Lead-Off Comparator. There are 8 possible values:
	 * 1. Pos:95% - Neg:5%, 2. Pos:92.5% - Neg:7.5%, 3. Pos:90% - Neg:10%, 4. Pos:87.5% - Neg:12.5%, 5. Pos:85% - Neg:15%,
	 * 6. Pos:80% - Neg:20%, 7. Pos:75% - Neg:25%, 8. Pos:70% - Neg:30%
	 * @param treshold where 0 = 95-5, 1 = 92.5-7.5, 2 = 90-10, 3 = 87.5-12.5, 4 = 85-15, 5 = 80-20, 6 = 75-25, 7 = 70-30
	 */
	public void writeEXGLeadOffComparatorTreshold(int treshold){
		if(mFirmwareVersionCode>2){
			if(treshold >=0 && treshold<8){ 
				byte[] reg1 = mEXG1RegisterArray;
				byte[] reg2 = mEXG2RegisterArray;
				byte currentLeadOffTresholdChip1 = reg1[2];
				byte currentLeadOffTresholdChip2 = reg2[2];
				currentLeadOffTresholdChip1 = (byte) (currentLeadOffTresholdChip1 & 31);
				currentLeadOffTresholdChip2 = (byte) (currentLeadOffTresholdChip2 & 31);
				currentLeadOffTresholdChip1 = (byte) (currentLeadOffTresholdChip1 | (treshold<<5));
				currentLeadOffTresholdChip2 = (byte) (currentLeadOffTresholdChip2 | (treshold<<5));
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 0,0,10,reg1[0],reg1[1],currentLeadOffTresholdChip1,reg1[3],reg1[4],reg1[5],reg1[6],reg1[7],reg1[8],reg1[9]});
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0],reg2[1],currentLeadOffTresholdChip2,reg2[3],reg2[4],reg2[5],reg2[6],reg2[7],reg2[8],reg2[9]});
			}
		}
	}
	
	
	/**
	 * Only supported on Shimmer3, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device
	 * This function set the ExG Lead-Off Current. There are 4 possible values: 6nA (default), 22nA, 6uA and 22uA.
	 * @param LeadOffCurrent where 0 = 6nA, 1 = 22nA, 2 = 6uA and 3 = 22uA
	 */
	public void writeEXGLeadOffDetectionCurrent(int leadOffCurrent){
		if(mFirmwareVersionCode>2){
			if(leadOffCurrent >=0 && leadOffCurrent<4){
				byte[] reg1 = mEXG1RegisterArray;
				byte[] reg2 = mEXG2RegisterArray;
				byte currentLeadOffDetectionCurrentChip1 = reg1[2];
				byte currentLeadOffDetectionCurrentChip2 = reg2[2];
				currentLeadOffDetectionCurrentChip1 = (byte) (currentLeadOffDetectionCurrentChip1 & 243);
				currentLeadOffDetectionCurrentChip2 = (byte) (currentLeadOffDetectionCurrentChip2 & 243);
				currentLeadOffDetectionCurrentChip1 = (byte) (currentLeadOffDetectionCurrentChip1 | (leadOffCurrent<<2));
				currentLeadOffDetectionCurrentChip2 = (byte) (currentLeadOffDetectionCurrentChip2 | (leadOffCurrent<<2));
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 0,0,10,reg1[0],reg1[1],currentLeadOffDetectionCurrentChip1,reg1[3],reg1[4],reg1[5],reg1[6],reg1[7],reg1[8],reg1[9]});
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0],reg2[1],currentLeadOffDetectionCurrentChip2,reg2[3],reg2[4],reg2[5],reg2[6],reg2[7],reg2[8],reg2[9]});
			}
		}
	}
	
	
	/**
	 * Only supported on Shimmer3
	 * This function set the ExG Lead-Off detection mode. There are 3 possible modes: DC Current, AC Current (not supported yet), and Off.
	 * @param detectionMode where 0 = Off, 1 = DC Current, and 2 = AC Current
	 */
	public void writeEXGLeadOffDetectionMode(int detectionMode){
		
		if(mFirmwareVersionCode>2){
			if(detectionMode == 0){
				mLeadOffDetectionMode = detectionMode;
				byte[] reg1 = mEXG1RegisterArray;
				byte[] reg2 = mEXG2RegisterArray;
				byte currentComparatorChip1 = reg1[1];
				byte currentComparatorChip2 = reg2[1];
				currentComparatorChip1 = (byte) (currentComparatorChip1 & 191);
				currentComparatorChip2 = (byte) (currentComparatorChip2 & 191);
				byte currentRDLSense = reg1[5];
				currentRDLSense = (byte) (currentRDLSense & 239);
				byte current2P1N1P = reg1[6];
				current2P1N1P = (byte) (current2P1N1P & 240);
				byte current2P = reg2[6];
				current2P = (byte) (current2P & 240);
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 0,0,10,reg1[0],currentComparatorChip1,reg1[2],reg1[3],reg1[4],currentRDLSense,current2P1N1P,reg1[7],reg1[8],reg1[9]});
				if(isEXGUsingDefaultEMGConfiguration()){
					byte currentEMGConfiguration = reg2[4];
					currentEMGConfiguration = (byte) (currentEMGConfiguration & 127);
					currentEMGConfiguration = (byte) (currentEMGConfiguration | 128);
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0],currentComparatorChip2,reg2[2],reg2[3],currentEMGConfiguration,reg2[5],current2P,reg2[7],reg2[8],reg2[9]});
				}
				else
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0],currentComparatorChip2,reg2[2],reg2[3],reg2[4],reg2[5],current2P,reg2[7],reg2[8],reg2[9]});
			}
			else if(detectionMode == 1){
				mLeadOffDetectionMode = detectionMode;
				
				byte[] reg1 = mEXG1RegisterArray;
				byte[] reg2 = mEXG2RegisterArray;
				byte currentDetectionModeChip1 = reg1[2];
				byte currentDetectionModeChip2 = reg2[2];
				currentDetectionModeChip1 = (byte) (currentDetectionModeChip1 & 254);	// set detection mode chip1 
				currentDetectionModeChip2 = (byte) (currentDetectionModeChip2 & 254);  // set detection mode chip2
				byte currentComparatorChip1 = reg1[1];
				byte currentComparatorChip2 = reg2[1];
				currentComparatorChip1 = (byte) (currentComparatorChip1 & 191);	
				currentComparatorChip2 = (byte) (currentComparatorChip2 & 191);
				currentComparatorChip1 = (byte) (currentComparatorChip1 | 64); // set comparator chip1 
				currentComparatorChip2 = (byte) (currentComparatorChip2 | 64); // set comparator chip2 
				byte currentRDLSense = reg1[5];
				currentRDLSense = (byte) (currentRDLSense & 239); 
				currentRDLSense = (byte) (currentRDLSense | 16); // set RLD sense
				byte current2P1N1P = reg1[6];
				current2P1N1P = (byte) (current2P1N1P & 240);
				current2P1N1P = (byte) (current2P1N1P | 7); // set 2P, 1N, 1P
				byte current2P = reg2[6];
				current2P = (byte) (current2P & 240);
				current2P = (byte) (current2P | 4); // set 2P
				
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 0,0,10,reg1[0], currentComparatorChip1, currentDetectionModeChip1,reg1[3],reg1[4], currentRDLSense, current2P1N1P,reg1[7],reg1[8],reg1[9]});
				if(isEXGUsingDefaultEMGConfiguration()){ //if the EMG configuration is used, then enable the chanel 2 since it is needed for the Lead-off detection
					byte currentEMGConfiguration = reg2[4];
					currentEMGConfiguration = (byte) (currentEMGConfiguration & 127);
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0], currentComparatorChip2, currentDetectionModeChip2,reg2[3],currentEMGConfiguration,reg2[5],current2P,reg2[7],reg2[8],reg2[9]});
				}
				else
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 1,0,10,reg2[0], currentComparatorChip2, currentDetectionModeChip2,reg2[3],reg2[4],reg2[5],current2P,reg2[7],reg2[8],reg2[9]});
			}
			else if(detectionMode == 2){
				mLeadOffDetectionMode = detectionMode;
				//NOT SUPPORTED YET
			}
		}
	}
	
	/**
	 * Only supported on Shimmer3, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device
	 * This function set the ExG reference electrode. There are 2 possible values when using ECG configuration: Inverse Wilson CT (default) and Fixed Potential
	 * and 2 possible values when using EMG configuration: Fixed Potential (default) and Inverse of Ch 1
	 * @param referenceElectrode reference electrode code where 0 = Fixed Potential and 13 = Inverse Wilson CT (default) for an ECG configuration, and
	 * 													where 0 = Fixed Potential (default) and 3 = Inverse Ch1 for an EMG configuration
	 */
	public void writeEXGReferenceElectrode(int referenceElectrode){
		if (mFirmwareVersionCode>2){
			byte currentByteValue = mEXG1RegisterArray[5];
			byte[] reg = mEXG1RegisterArray;
			currentByteValue = (byte) (currentByteValue & 240);
			currentByteValue = (byte) (currentByteValue | referenceElectrode);
			mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) 0,0,10,reg[0],reg[1],reg[2],reg[3],reg[4],currentByteValue,reg[6],reg[7],reg[8],reg[9]});
		}
	}

	/**
	 * Only supported on Shimmer3, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device
	 * @param chipID Either a 1 or 2 value
	 * @param rateSettingsam , where 0=125SPS ; 1=250SPS; 2=500SPS; 3=1000SPS; 4=2000SPS  
	 */
	public void writeEXGRateSetting(int chipID, int rateSetting){
		if ((mFirmwareVersionInternal >=8 && mFirmwareVersionCode==2) || mFirmwareVersionCode>2){
			if (chipID==1 || chipID==2){
				if (chipID==1){
					byte currentByteValue = mEXG1RegisterArray[0];
					byte[] reg = mEXG1RegisterArray;
					currentByteValue = (byte) (currentByteValue & 248);
					currentByteValue = (byte) (currentByteValue | rateSetting);
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,currentByteValue,reg[1],reg[2],reg[3],reg[4],reg[5],reg[6],reg[7],reg[8],reg[9]});
				} else if (chipID==2){
					byte currentByteValue = mEXG2RegisterArray[0];
					byte[] reg = mEXG2RegisterArray;
					currentByteValue = (byte) (currentByteValue & 248);
					currentByteValue = (byte) (currentByteValue | rateSetting);
					mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,currentByteValue,reg[1],reg[2],reg[3],reg[4],reg[5],reg[6],reg[7],reg[8],reg[9]});
				}
			}
		}
	}
	
	
	/**
	 * This is only supported on SHimmer3,, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device 
	 * @param chipID Either a 1 or 2 value
	 * @param gainSetting , where 0 = 6x Gain, 1 = 1x , 2 = 2x , 3 = 3x, 4 = 4x, 5 = 8x, 6 = 12x
	 * @param channel Either a 1 or 2 value
	 */
	public void writeEXGGainSetting(int chipID,  int channel, int gainSetting){
		if ((mFirmwareVersionInternal >=8 && mFirmwareVersionCode==2) || mFirmwareVersionCode>2){
			if ((chipID==1 || chipID==2) && (channel==1 || channel==2)){
				if (chipID==1){
					if (channel==1){
						byte currentByteValue = mEXG1RegisterArray[3];
						byte[] reg = mEXG1RegisterArray;
						currentByteValue = (byte) (currentByteValue & 143);
						currentByteValue = (byte) (currentByteValue | (gainSetting<<4));
						mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,reg[0],reg[1],reg[2],currentByteValue,reg[4],reg[5],reg[6],reg[7],reg[8],reg[9]});
					} else {
						byte currentByteValue = mEXG1RegisterArray[4];
						byte[] reg = mEXG1RegisterArray;
						currentByteValue = (byte) (currentByteValue & 143);
						currentByteValue = (byte) (currentByteValue | (gainSetting<<4));
						mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,reg[0],reg[1],reg[2],reg[3],currentByteValue,reg[5],reg[6],reg[7],reg[8],reg[9]});
					}
				} else if (chipID==2){
					if (channel==1){
						byte currentByteValue = mEXG2RegisterArray[3];
						byte[] reg = mEXG2RegisterArray;
						currentByteValue = (byte) (currentByteValue & 143);
						currentByteValue = (byte) (currentByteValue | (gainSetting<<4));
						mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,reg[0],reg[1],reg[2],currentByteValue,reg[4],reg[5],reg[6],reg[7],reg[8],reg[9]});
					} else {
						byte currentByteValue = mEXG2RegisterArray[4];
						byte[] reg = mEXG2RegisterArray;
						currentByteValue = (byte) (currentByteValue & 143);
						currentByteValue = (byte) (currentByteValue | (gainSetting<<4));
						mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte) (chipID-1),0,10,reg[0],reg[1],reg[2],reg[3],currentByteValue,reg[5],reg[6],reg[7],reg[8],reg[9]});
					}
				}
			}
		}
	}
	
	/**
	 * Only supported on Shimmer3, note that unlike previous write commands where the values are only set within the instrument driver after the ACK is received, this is set immediately. Fail safe should the settings not be actually set successfully is a timeout will occur, and the ID will disconnect from the device
	 * @param reg A 10 byte value
	 * @param chipID value can either be 1 or 2.
	 */
	public void writeEXGConfiguration(byte[] reg,int chipID){
		if ((mFirmwareVersionInternal >=8 && mFirmwareVersionCode==2) || mFirmwareVersionCode>2){
			if (chipID==1 || chipID==2){
				mListofInstructions.add(new byte[]{SET_EXG_REGS_COMMAND,(byte)(chipID-1),0,10,reg[0],reg[1],reg[2],reg[3],reg[4],reg[5],reg[6],reg[7],reg[8],reg[9]});
			}
		}}
		
	/**
	 * writeGSRRange(range) sets the GSR range on the Shimmer to the value of the input range. 
	 * @param range numeric value defining the desired GSR range. Valid range settings are 0 (10kOhm to 56kOhm), 1 (56kOhm to 220kOhm), 2 (220kOhm to 680kOhm), 3 (680kOhm to 4.7MOhm) and 4 (Auto Range).
	 */
	public void writeGSRRange(int range) {
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			if (mFirmwareVersionCode!=1 || mFirmwareVersionInternal >4){
				mListofInstructions.add(new byte[]{SET_GSR_RANGE_COMMAND, (byte)range});
			}
		} else {
			mListofInstructions.add(new byte[]{SET_GSR_RANGE_COMMAND, (byte)range});
		}
	}
	
	/**
	 * writeMagRange(range) sets the MagSamplingRate on the Shimmer to the value of the input range. When setting/changing the accel range, please ensure you have the correct calibration parameters. Note that the Shimmer device can only carry one set of accel calibration parameters at a single time.
	 * @param range is the mag rang
	 */
	public void writeMagRange(int range) {
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{SET_MAG_GAIN_COMMAND, (byte)range});
		}
	}

	public void writeLEDCommand(int command) {
		
//		if (mShimmerVersion!=HW_ID.SHIMMER_3){
			if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
			} else {
				mListofInstructions.add(new byte[]{SET_BLINK_LED, (byte)command});
			}
//		}
	}

	public void writeAccelCalibrationParameters(byte[] calibrationParameters) {
		cmdcalibrationParameters[0] = SET_ACCEL_CALIBRATION_COMMAND;
		System.arraycopy(calibrationParameters, 0, cmdcalibrationParameters, 1, 21);
		mListofInstructions.add(cmdcalibrationParameters);	
	}
	
	public void writeGyroCalibrationParameters(byte[] calibrationParameters) {
		cmdcalibrationParameters[0] = SET_GYRO_CALIBRATION_COMMAND;
		System.arraycopy(calibrationParameters, 0, cmdcalibrationParameters, 1, 21);
		mListofInstructions.add(cmdcalibrationParameters);	
	}
	
	public void writeMagCalibrationParameters(byte[] calibrationParameters) {
		cmdcalibrationParameters[0] = SET_MAG_CALIBRATION_COMMAND;
		System.arraycopy(calibrationParameters, 0, cmdcalibrationParameters, 1, 21);
		mListofInstructions.add(cmdcalibrationParameters);	
	}

	public void writeWRAccelCalibrationParameters(byte[] calibrationParameters) {
		if(mHardwareVersion==HW_ID.SHIMMER_3){
			cmdcalibrationParameters[0] = SET_LSM303DLHC_ACCEL_CALIBRATION_COMMAND;
			System.arraycopy(calibrationParameters, 0, cmdcalibrationParameters, 1, 21);
			mListofInstructions.add(cmdcalibrationParameters);	
		}
	}

	public void writeECGCalibrationParameters(int offsetrall, int gainrall,int offsetlall, int gainlall) {
		byte[] data = new byte[8];
		data[0] = (byte) ((offsetlall>>8)& 0xFF); //MSB offset
		data[1] = (byte) ((offsetlall)& 0xFF);
		data[2] = (byte) ((gainlall>>8)& 0xFF); //MSB gain
		data[3] = (byte) ((gainlall)& 0xFF);
		data[4] = (byte) ((offsetrall>>8)& 0xFF); //MSB offset
		data[5] = (byte) ((offsetrall)& 0xFF);
		data[6] = (byte) ((gainrall>>8)& 0xFF); //MSB gain
		data[7] = (byte) ((gainrall)& 0xFF);
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{SET_ECG_CALIBRATION_COMMAND,data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7]});
		}
	}

	public void writeEMGCalibrationParameters(int offset, int gain) {
		byte[] data = new byte[4];
		data[0] = (byte) ((offset>>8)& 0xFF); //MSB offset
		data[1] = (byte) ((offset)& 0xFF);
		data[2] = (byte) ((gain>>8)& 0xFF); //MSB gain
		data[3] = (byte) ((gain)& 0xFF);
		if (mFirmwareVersionParsed.equals("BoilerPlate 0.1.0")){
		} else {
			mListofInstructions.add(new byte[]{SET_EMG_CALIBRATION_COMMAND,data[0],data[1],data[2],data[3]});
		}
	}
	
	/**
	 * writeBaudRate(value) sets the baud rate on the Shimmer. 
	 * @param value numeric value defining the desired Baud rate. Valid rate settings are 0 (115200 default),
	 *  1 (1200), 2 (2400), 3 (4800), 4 (9600) 5 (19200),
	 *  6 (38400), 7 (57600), 8 (230400), 9 (460800) and 10 (921600)
	 */
	public void writeBaudRate(int value) {
		if (mFirmwareVersionCode>=5){ 
			if(value>=0 && value<=10){
				mBluetoothBaudRate = value;
				mListofInstructions.add(new byte[]{SET_BAUD_RATE_COMMAND, (byte)value});
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.reconnect();
			}
			
		}
	}

	/**
	 * writeConfigByte0(configByte0) sets the config byte0 value on the Shimmer to the value of the input configByte0. 
	 * @param configByte0 is an unsigned 8 bit value defining the desired config byte 0 value.
	 */
	public void writeConfigByte0(byte configByte0) {
		mListofInstructions.add(new byte[]{SET_CONFIG_BYTE0_COMMAND,(byte) configByte0});
	}
	
	/**
	 * writeAccelRange(range) sets the Accelerometer range on the Shimmer to the value of the input range. When setting/changing the accel range, please ensure you have the correct calibration parameters. Note that the Shimmer device can only carry one set of accel calibration parameters at a single time.
	 * @param range is a numeric value defining the desired accelerometer range. Valid range setting values for the Shimmer 2 are 0 (+/- 1.5g), 1 (+/- 2g), 2 (+/- 4g) and 3 (+/- 6g). Valid range setting values for the Shimmer 2r are 0 (+/- 1.5g) and 3 (+/- 6g).
	 */
	public void writeBufferSize(int size) {
		mListofInstructions.add(new byte[]{SET_BUFFER_SIZE_COMMAND, (byte)size});
	}
	
	/**
	 * Sets the Pmux bit value on the Shimmer to the value of the input SETBIT. The PMux bit is the 2nd MSB of config byte0.
	 * @param setBit value defining the desired setting of the PMux (1=ON, 0=OFF).
	 */
	public void writePMux(int setBit) {
		mListofInstructions.add(new byte[]{SET_PMUX_COMMAND,(byte) setBit});
	}

	/**
	 * Sets the configGyroTempVref bit value on the Shimmer to the value of the input SETBIT. The configGyroTempVref bit is the 2nd MSB of config byte0.
	 * @param setBit value defining the desired setting of the Gyro Vref (1=ON, 0=OFF).
	 */
	/*public void writeConfigGyroTempVref(int setBit) {
    	while(getInstructionStatus()==false) {};
			//Bit value defining the desired setting of the PMux (1=ON, 0=OFF).
			if (setBit==1) {
				mTempByteValue=(byte) (mConfigByte0|32); 
			} else if (setBit==0) {
				mTempByteValue=(byte)(mConfigByte0 & 223);
			}
			mCurrentCommand=SET_GYRO_TEMP_VREF_COMMAND;
			write(new byte[]{SET_GYRO_TEMP_VREF_COMMAND,(byte) setBit});
			mWaitForAck=true;
			mTransactionCompleted=false;
			responseTimer(ACK_TIMER_DURATION);
	}*/

	/**
	 * Enable/disable the Internal Exp Power on the Shimmer3
	 * @param setBit value defining the desired setting of the Volt regulator (1=ENABLED, 0=DISABLED).
	 */
	public void writeInternalExpPower(int setBit) {
		if (mHardwareVersion == HW_ID.SHIMMER_3 && mFirmwareVersionCode>=2){
			mListofInstructions.add(new byte[]{SET_INTERNAL_EXP_POWER_ENABLE_COMMAND,(byte) setBit});
		} else {
			
		}
	}
	
	/**
	 * Enable/disable the 5 Volt Regulator on the Shimmer ExpBoard board
	 * @param setBit value defining the desired setting of the Volt regulator (1=ENABLED, 0=DISABLED).
	 */
	public void writeFiveVoltReg(int setBit) {
		mListofInstructions.add(new byte[]{SET_5V_REGULATOR_COMMAND,(byte) setBit});
	}
	
	//endregion
	
	
	//region --------- GET/SET FUNCTIONS --------- 
	
	/**** GET FUNCTIONS *****/

	/**
	 * This returns the variable mTransactionCompleted which indicates whether the Shimmer device is in the midst of a command transaction. True when no transaction is taking place. This is deprecated since the update to a thread model for executing commands
	 * @return mTransactionCompleted
	 */
	public boolean getInstructionStatus(){	
		boolean instructionStatus=false;
		if (mTransactionCompleted == true) {
			instructionStatus=true;
		} else {
			instructionStatus=false;
		}
		return instructionStatus;
	}
	
	public int getLowPowerAccelEnabled(){
		// TODO Auto-generated method stub
		if (mLowPowerAccelWR)
			return 1;
		else
			return 0;
	}

	public int getLowPowerGyroEnabled() {
		// TODO Auto-generated method stub
		if (mLowPowerGyro)
			return 1;
		else
			return 0;
	}

	public int getLowPowerMagEnabled() {
		// TODO Auto-generated method stub
		if (mLowPowerMag)
			return 1;
		else
			return 0;
	}
	
	public int getPacketSize(){
		return mPacketSize;
	}
	
	public boolean getInitialized(){
		return mInitialized;
	}

	public double getPacketReceptionRate(){
		return mPacketReceptionRate;
	}
	
	/**
	 * Get the 5V Reg. Only supported on Shimmer2/2R.
	 * @return 0 in case the 5V Reg is disableb, 1 in case the 5V Reg is enabled, and -1 in case the device doesn't support this feature
	 */
	public int get5VReg(){
		if(mHardwareVersion!=HW_ID.SHIMMER_3){
			if ((mConfigByte0 & (byte)128)!=0) {
				//then set ConfigByte0 at bit position 7
				return 1;
			} else {
				return 0;
			}
		}
		else
			return -1;
	}
	
	public String getDirectoryName(){
		if(mDirectoryName!=null)
			return mDirectoryName;
		else
			return "Directory not read yet";
	}

	public int getCurrentLEDStatus() {
		return mCurrentLEDStatus;
	}

	public int getFirmwareMajorVersion(){
		return mFirmwareVersionMajor;
	}
	
	public int getFirmwareMinorVersion(){
		return mFirmwareVersionMinor;
	}
	
	public int getFirmwareCode(){
		return mFirmwareVersionCode;
	}
	
	public String getFWVersionName(){
		return mFirmwareVersionParsed;
	}
	
	/**
	 * Get the FW Identifier. It is equal to 3 when LogAndStream, and equal to 4 when BTStream. 
	 * @return The FW identifier
	 */
	public int getFWIdentifier(){
		return (int) mFirmwareIdentifier;
	}
	
	public int getBaudRate(){
		return mBluetoothBaudRate;
	}
	
	public int getReferenceElectrode(){
		return mEXGReferenceElectrode;
	}
	
	public int getLeadOffDetectionMode(){
		return mLeadOffDetectionMode;
	}
	
	public int getLeadOffDetectionCurrent(){
		return mEXGLeadOffDetectionCurrent;
	}
	
	public int getLeadOffComparatorTreshold(){
		return mEXGLeadOffComparatorTreshold;
	}
	
	public byte[] getExG1Register(){

	       return mEXG1RegisterArray;

	    }

	   

	public byte[] getExG2Register(){

	       return mEXG2RegisterArray;

	    }
	
	public int getExGComparatorsChip1(){
		return mEXG1Comparators;
	}
	
	public int getExGComparatorsChip2(){
		return mEXG2Comparators;
	}
	
	public String getExpBoardID(){
		
		if(mExpBoardArray!=null){
//			if(mExpBoardName==null){
				int boardID = mExpBoardArray[1] & 0xFF;
				int boardRev = mExpBoardArray[2] & 0xFF;
				int specialRevision = mExpBoardArray[3] & 0xFF;
				String boardName;
				switch(boardID){
					case 8:
						boardName="Bridge Amplifier+";
					break;
					case 14:
						boardName="GSR+";
					break;
					case 36:
						boardName="PROTO3 Mini";
					break;
					case 37:
						boardName="ExG";
					break;
					case 38:
						boardName="PROTO3 Deluxe";
					break;
					default:
						boardName="Unknown";
					break;
					
				}
				if(!boardName.equals("Unknown")){
					boardName += " (SR" + boardID + "." + boardRev + "." + specialRevision +")";
				}
				
				mExpBoardName = boardName;
//			}
		}
		else
			return "Need to read ExpBoard ID first";
		
		return mExpBoardName;
	}
	
	public double getBattLimitWarning(){
		return mLowBattLimit;
	}

	public int getShimmerVersion(){
		return mHardwareVersion;
	}

	public String getShimmerName(){
		return mMyName;
	}
	
	public void setShimmerName(String name){
		mMyName = name;
	}
	
	/**
	 * Get the Gain value for the ExG1 Channel 1
	 * @return the value of the gain. The Gain can be 1, 2, 3, 4, 6 (default), 8 or 12. The function return -1 when it is not possible to get the value.
	 */
	public int getEXG1CH1GainValue(){
		
		int gain = -1;
		while(!mListofInstructions.isEmpty());
		int tmpGain = getExg1CH1GainValue();
		if(tmpGain==1 || tmpGain==2 || tmpGain==3 || tmpGain==4 || tmpGain==6 || tmpGain==8 || tmpGain==12){
			gain = tmpGain;
		}
		return gain;
	}
	
	/**
	 * Get the Gain value for the ExG1 Channel 1
	 * @return the value of the gain. The Gain can be 1, 2, 3, 4, 6 (default), 8 or 12. The function return -1 when it is not possible to get the value.
	 */
	public int getEXG1CH2GainValue(){
		
		int gain = -1;
		while(!mListofInstructions.isEmpty());
		int tmpGain = getExg1CH2GainValue();
		if(tmpGain==1 || tmpGain==2 || tmpGain==3 || tmpGain==4 || tmpGain==6 || tmpGain==8 || tmpGain==12){
			gain = tmpGain;
		}
		return gain;
	}
	
	/**
	 * Get the Gain value for the ExG1 Channel 1
	 * @return the value of the gain. The Gain can be 1, 2, 3, 4, 6 (default), 8 or 12. The function return -1 when it is not possible to get the value.
	 */
	public int getEXG2CH1GainValue(){
		
		int gain = -1;
		while(!mListofInstructions.isEmpty());
		int tmpGain = getExg2CH1GainValue();
		if(tmpGain==1 || tmpGain==2 || tmpGain==3 || tmpGain==4 || tmpGain==6 || tmpGain==8 || tmpGain==12){
			gain = tmpGain;
		}
		return gain;
	}

	/**
	 * Get the Gain value for the ExG1 Channel 1
	 * @return the value of the gain. The Gain can be 1, 2, 3, 4, 6 (default), 8 or 12. The function return -1 when it is not possible to get the value.
	 */
	public int getEXG2CH2GainValue(){
	
		int gain = -1;
		while(!mListofInstructions.isEmpty());
		int tmpGain = getExg2CH2GainValue();
		if(tmpGain==1 || tmpGain==2 || tmpGain==3 || tmpGain==4 || tmpGain==6 || tmpGain==8 || tmpGain==12){
			gain = tmpGain;
		}
		return gain;
	}
	
    public int getState(){
        return mState;
    }

    /** Returns true if device is streaming (Bluetooth)
     * @return
     */
    public boolean isStreaming(){
    	return mStreaming;
    }
    
    /**** SET FUNCTIONS *****/
    
    /**
	 * 
	 * Register a callback to be invoked after buildmsg has executed (A new packet has been successfully received -> raw bytes interpreted into Raw and Calibrated Sensor data)
	 * 
	 * @param d The callback that will be invoked
	 */
	public void setDataProcessing(DataProcessing d) {
		mDataProcessing=d;
	}
    
	/**
	 * Set the battery voltage limit, when the Shimmer device goes below the limit while streaming the LED on the Shimmer device will turn Yellow, in order to use battery voltage monitoring the Battery has to be enabled. See writeenabledsensors. Only to be used with Shimmer2. Calibration also has to be enabled, see enableCalibration.
	 * @param limit
	 */
	public void setBattLimitWarning(double limit){
		mLowBattLimit=limit;
	}
	
	public void setContinuousSync(boolean continousSync){
		mContinousSync=continousSync;
	}
	
	//endregion
	
    
    //region --------- IS+something FUNCTIONS --------- 
    
    public boolean isLowPowerMagEnabled(){
		return mLowPowerMag;
	}
    
    public boolean isGyroOnTheFlyCalEnabled(){
		return mEnableOntheFlyGyroOVCal;
	}

	public boolean is3DOrientatioEnabled(){
		return mOrientationEnabled;
	}
    
	/**Only used for LogAndStream
	 * @return
	 */
	public boolean isSensing(){
		return mSensingStatus;
	}
	
	public boolean isDocked(){
		return mDockedStatus;
	}
	
	public boolean isLowPowerAccelEnabled() {
		// TODO Auto-generated method stub
		return mLowPowerAccelWR;
	}

	public boolean isLowPowerGyroEnabled() {
		// TODO Auto-generated method stub
		return mLowPowerGyro;
	}
	
	public boolean isUsingDefaultLNAccelParam(){
		return mDefaultCalibrationParametersAccel;
	}
	
	public boolean isUsingDefaultAccelParam(){
		return mDefaultCalibrationParametersAccel;
	}
	
	public boolean isUsingDefaultWRAccelParam(){
		return mDefaultCalibrationParametersDigitalAccel; 
	}

	public boolean isUsingDefaultGyroParam(){
		return mDefaultCalibrationParametersGyro;
	}
	
	public boolean isUsingDefaultMagParam(){
		return mDefaultCalibrationParametersMag;
	}
	
	public boolean isUsingDefaultECGParam(){
		return mDefaultCalibrationParametersECG;
	}
	
	public boolean isUsingDefaultEMGParam(){
		return mDefaultCalibrationParametersEMG;
	}
	

	
	
	/**
	 * Checks if 16 bit ECG configuration is set on the Shimmer device. Do not use this command right after setting an EXG setting, as due to the execution model, the old settings might be returned, if this command is executed before an ack is received.
	 * @return true if 16 bit ECG is set
	 */
	@Override
	public boolean isEXGUsingECG16Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingECG16Configuration();
	}
	
	/**
	 * Checks if 24 bit ECG configuration is set on the Shimmer device. Do not use this command right after setting an EXG setting, as due to the execution model, the old settings might be returned, if this command is executed before an ack is received.
	 * @return true if 24 bit ECG is set
	 */
	@Override
	public boolean isEXGUsingECG24Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingECG24Configuration();
	}
	
	/**
	 * Checks if 16 bit EMG configuration is set on the Shimmer device.  Do not use this command right after setting an EXG setting, as due to the execution model, the old settings might be returned, if this command is executed before an ack is received. 
	 * @return true if 16 bit EMG is set
	 */
	@Override
	public boolean isEXGUsingEMG16Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingEMG16Configuration();
	}
	
	/**
	 * Checks if 24 bit EMG configuration is set on the Shimmer device.  Do not use this command right after setting an EXG setting, as due to the execution model, the old settings might be returned, if this command is executed before an ack is received.
	 * @return true if 24 bit EMG is set
	 */
	@Override
	public boolean isEXGUsingEMG24Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingEMG24Configuration();
	}
	
	/**
	 * Checks if 16 bit test signal configuration is set on the Shimmer device. Do not use this command right after setting an EXG setting, as due to the execution model, the old settings might be returned, if this command is executed before an ack is received.
	 * @return true if 24 bit test signal is set
	 */
	@Override
	public boolean isEXGUsingTestSignal16Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingTestSignal16Configuration();
	}
	
	/**
	 * Checks if 24 bit test signal configuration is set on the Shimmer device.
	 * @return true if 24 bit test signal is set
	 */
	@Override
	public boolean isEXGUsingTestSignal24Configuration(){
		while(!mListofInstructions.isEmpty());
		return super.isEXGUsingTestSignal24Configuration();
	}
	
    //endregion
    

	//region --------- ENABLE/DISABLE FUNCTIONS --------- 

	/**** ENABLE FUNCTIONS *****/
	
	//TODO: use set3DOrientation(enable) in ShimmerObject instead -> check that the "enable the sensors if they have not been enabled" comment is correct
	/**
	 * This enables the calculation of 3D orientation through the use of the gradient descent algorithm, note that the user will have to ensure that mEnableCalibration has been set to true (see enableCalibration), and that the accel, gyro and mag has been enabled
	 * @param enable
	 */
	public void enable3DOrientation(boolean enable){
		//enable the sensors if they have not been enabled 
		mOrientationEnabled = enable;
	}

	/**
	 * This enables the low power accel option. When not enabled the sampling rate of the accel is set to the closest value to the actual sampling rate that it can achieve. In low power mode it defaults to 10Hz. Also and additional low power mode is used for the LSM303DLHC. This command will only supports the following Accel range +4g, +8g , +16g 
	 * @param enable
	 */
	public void enableLowPowerAccel(boolean enable){
		enableHighResolutionMode(!enable);
		writeAccelSamplingRate(mLSM303DigitalAccelRate);
	}

	private void enableHighResolutionMode(boolean enable) {
		while(getInstructionStatus()==false) {};
		
		if (mFirmwareVersionCode==1 && mFirmwareVersionInternal==0) {

		} else if (mHardwareVersion == HW_ID.SHIMMER_3) {
			setLowPowerAccelWR(!enable);
//			setHighResAccelWR(enable);
			if (enable) {
				// High-Res = On, Low-power = Off
				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_HRMODE_COMMAND, (byte)0x01});
				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_LPMODE_COMMAND, (byte)0x00});
			} else {
				// High-Res = Off, Low-power = On
				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_HRMODE_COMMAND, (byte)0x00});
				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_LPMODE_COMMAND, (byte)0x01});
			}
		}
	}
	
//	private void enableLowResolutionMode(boolean enable){
//		while(getInstructionStatus()==false) {};
//		if (mFirmwareVersionCode==1 && mFirmwareVersionRelease==0) {
//
//		} else if (mShimmerVersion == HW_ID.SHIMMER_3) {
//			if (enable) {
//				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_LPMODE_COMMAND, (byte)0x01});
//				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_HRMODE_COMMAND, (byte)0x00});
//			} else {
//				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_HRMODE_COMMAND, (byte)0x01});
//				mListofInstructions.add(new byte[]{SET_LSM303DLHC_ACCEL_LPMODE_COMMAND, (byte)0x00});
//			}
//		}
//	}
	
	/**
	 * This enables the low power accel option. When not enabled the sampling rate of the accel is set to the closest value to the actual sampling rate that it can achieve. In low power mode it defaults to 10Hz. Also and additional low power mode is used for the LSM303DLHC. This command will only supports the following Accel range +4g, +8g , +16g 
	 * @param enable
	 */
	public void enableLowPowerGyro(boolean enable){
		setLowPowerGyro(enable);
		writeGyroSamplingRate(mMPU9150GyroAccelRate);
	}
	
	/**
	 * This enables the low power mag option. When not enabled the sampling rate of the mag is set to the closest value to the actual sampling rate that it can achieve. In low power mode it defaults to 10Hz
	 * @param enable
	 */
	public void enableLowPowerMag(boolean enable){
		setLowPowerMag(enable);
		writeMagSamplingRate(mLSM303MagRate);
	}
	

	/**
	 *This can only be used for Shimmer3 devices (EXG) 
	 *When a enable configuration is loaded, the advanced ExG configuration is removed, so it needs to be set again
	 */
	 public void enableDefaultECGConfiguration() {
		 if (mHardwareVersion==3){
			setDefaultECGConfiguration();
			writeEXGConfiguration(mEXG1RegisterArray,1);
			writeEXGConfiguration(mEXG2RegisterArray,2);
		 }
	}

	/**
	 * This can only be used for Shimmer3 devices (EXG)
	 * When a enable configuration is loaded, the advanced ExG configuration is removed, so it needs to be set again
	 */
	public void enableDefaultEMGConfiguration(){
		if (mHardwareVersion==3){
			setDefaultEMGConfiguration();
			writeEXGConfiguration(mEXG1RegisterArray,1);
			writeEXGConfiguration(mEXG2RegisterArray,2);
		}
	}

	/**
	 * This can only be used for Shimmer3 devices (EXG). Enables the test signal (square wave) of both EXG chips, to use, both EXG1 and EXG2 have to be enabled
	 */
	public void enableEXGTestSignal(){
		if (mHardwareVersion==3){
			setEXGTestSignal();
			writeEXGConfiguration(mEXG1RegisterArray,1);
			writeEXGConfiguration(mEXG2RegisterArray,2);
		}
	}
	
	/**** DISABLE FUNCTIONS *****/
	
	private long disableBit(long number,long disablebitvalue){
		if ((number&disablebitvalue)>0){
			number = number ^ disablebitvalue;
		}
		return number;
	}
	
	//endregion
	
	
	//region --------- MISCELLANEOUS FUNCTIONS ---------
	
	public void reconnect(){
        if (mState==ShimmerBluetooth.STATE_CONNECTED && !mStreaming){
        	String msgReconnect = "Reconnecting the Shimmer...";
			sendStatusMSGtoUI(msgReconnect);
            stop();
            try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            connect(mMyBluetoothAddress,"default");
        }
    }

	
	/**
	 * An inquiry is used to request for the current configuration parameters from the Shimmer device (e.g. Accelerometer settings, Configuration Byte, Sampling Rate, Number of Enabled Sensors and Sensors which have been enabled). 
	 */
	public void inquiry() {
		mListofInstructions.add(new byte[]{INQUIRY_COMMAND});
	}
	
	/**
	 * @param enabledSensors This takes in the current list of enabled sensors 
	 * @param sensorToCheck This takes in a single sensor which is to be enabled
	 * @return enabledSensors This returns the new set of enabled sensors, where any sensors which conflicts with sensorToCheck is disabled on the bitmap, so sensorToCheck can be accomodated (e.g. for Shimmer2 using ECG will disable EMG,GSR,..basically any daughter board)
	 *  
	 */
	public long sensorConflictCheckandCorrection(long enabledSensors,long sensorToCheck){

		if (mHardwareVersion==HW_ID.SHIMMER_2R || mHardwareVersion==HW_ID.SHIMMER_2){
			if ((sensorToCheck & SENSOR_GYRO) >0 || (sensorToCheck & SENSOR_MAG) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_ECG);
				enabledSensors = disableBit(enabledSensors,SENSOR_EMG);
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			} else if ((sensorToCheck & SENSOR_BRIDGE_AMP) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_ECG);
				enabledSensors = disableBit(enabledSensors,SENSOR_EMG);
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_GYRO);
				enabledSensors = disableBit(enabledSensors,SENSOR_MAG);
			} else if ((sensorToCheck & SENSOR_GSR) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_ECG);
				enabledSensors = disableBit(enabledSensors,SENSOR_EMG);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
				enabledSensors = disableBit(enabledSensors,SENSOR_GYRO);
				enabledSensors = disableBit(enabledSensors,SENSOR_MAG);
			} else if ((sensorToCheck & SENSOR_ECG) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EMG);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
				enabledSensors = disableBit(enabledSensors,SENSOR_GYRO);
				enabledSensors = disableBit(enabledSensors,SENSOR_MAG);
			} else if ((sensorToCheck & SENSOR_EMG) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_ECG);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
				enabledSensors = disableBit(enabledSensors,SENSOR_GYRO);
				enabledSensors = disableBit(enabledSensors,SENSOR_MAG);
			} else if ((sensorToCheck & SENSOR_HEART) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_EXP_BOARD_A0);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXP_BOARD_A7);
			} else if ((sensorToCheck & SENSOR_EXP_BOARD_A0) >0 || (sensorToCheck & SENSOR_EXP_BOARD_A7) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_HEART);
				enabledSensors = disableBit(enabledSensors,SENSOR_BATT);
			} else if ((sensorToCheck & SENSOR_BATT) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_EXP_BOARD_A0);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXP_BOARD_A7);
			}
		}

		else if(mHardwareVersion==HW_ID.SHIMMER_3){
			
			if((sensorToCheck & SENSOR_GSR) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A1);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A14);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			} 
			else if((sensorToCheck & SENSOR_EXG1_16BIT) >0 || (sensorToCheck & SENSOR_EXG2_16BIT) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A1);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A12);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A13);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A14);
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			}
			else if((sensorToCheck & SENSOR_EXG1_24BIT) >0 || (sensorToCheck & SENSOR_EXG2_24BIT) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A1);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A12);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A13);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A14);
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			}
			else if((sensorToCheck & SENSOR_BRIDGE_AMP) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A12);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A13);
				enabledSensors = disableBit(enabledSensors,SENSOR_INT_ADC_A14);
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
			}
			else if ((sensorToCheck & SENSOR_INT_ADC_A14) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
			}
			else if ((sensorToCheck & SENSOR_INT_ADC_A12) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			}
			else if ((sensorToCheck & SENSOR_INT_ADC_A13) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			}
			else if ((sensorToCheck & SENSOR_INT_ADC_A14) >0){
				enabledSensors = disableBit(enabledSensors,SENSOR_GSR);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_16BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG1_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_EXG2_24BIT);
				enabledSensors = disableBit(enabledSensors,SENSOR_BRIDGE_AMP);
			}
			
		}
		enabledSensors = enabledSensors ^ sensorToCheck;
		return enabledSensors;
	}
	
	public boolean sensorConflictCheck(long enabledSensors){
		boolean pass=true;
		if (mHardwareVersion != HW_ID.SHIMMER_3){
			if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
					pass=false;
				}else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
					pass=false;
				}else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
				if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_EMG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_ECG) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF)& SENSOR_GSR) > 0){
					pass=false;
				} else if (get5VReg()==1){ // if the 5volt reg is set 
					pass=false;
				}
			}

			if (((enabledSensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0) {
				if (((enabledSensors & 0xFFFFF) & SENSOR_BATT) > 0) {
					pass=false;
				} else if (getPMux()==1){
					
					writePMux(0);
				}
			}

			if (((enabledSensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0) {
				if (((enabledSensors & 0xFFFFF) & SENSOR_BATT) > 0) {
					pass=false;
				}else if (getPMux()==1){
					writePMux(0);
				}
			}

			if (((enabledSensors & 0xFFFFF) & SENSOR_BATT) > 0) {
				if (((enabledSensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0){
					pass=false;
				} 
				if (((enabledSensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0){
					pass=false;
				}
				if (((enabledSensors & 0xFFFFF) & SENSOR_BATT) > 0){
					if (getPMux()==0){
						
						writePMux(1);
					}
				}
			}
			if (!pass){
				
			}
		}
		
		else{
			
			if(((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0 || ((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
				
				if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A1) > 0){
					pass=false; 
				} else if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A12) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A13) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A14) > 0){
					pass=false;
				} else if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}
			
			if(((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0 || ((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
				
				if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A1) > 0){
					pass=false; 
				} else if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A12) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A13) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A14) > 0){
					pass=false;
				} else if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}

			
			if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
				
				if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A1) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A14) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}
			
			if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
				  
				if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A12) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A13) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A14) > 0){
					pass=false;
				} else if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
					pass=false;		
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				}
			}
			
			if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A1) > 0){
				  
				 if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				 }
			}
			
			if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A12) > 0){
				  
				if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}
			
			if (((enabledSensors & 0xFF00) & SENSOR_INT_ADC_A13) > 0){
				  
				if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				} else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				}
			}
			
			if (((enabledSensors & 0xFF0000) & SENSOR_INT_ADC_A14) > 0){
				  
				 if(((enabledSensors & 0xFF) & SENSOR_GSR) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF0000) & SENSOR_EXG1_16BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF0000) & SENSOR_EXG2_16BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF) & SENSOR_EXG1_24BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF) & SENSOR_EXG2_24BIT) > 0){
					pass=false;
				 } else if (((enabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0){
					pass=false;
				 }
			}
		}
		
		return pass;
	}
	
	/**
	 * @param enabledSensors this bitmap is only applicable for the instrument driver and does not correspond with the values in the firmware
	 * @return enabledSensorsFirmware returns the bitmap for the firmware
	 * The reason for this is hardware and firmware change may eventually need a different sensor bitmap, to keep the ID forward compatible, this function is used. Therefor the ID can have its own seperate sensor bitmap if needed
	 */
	private long generateSensorBitmapForHardwareControl(long enabledSensors){
		long hardwareSensorBitmap=0;

		//check if the batt volt is enabled (this is only applicable for HW_ID.SHIMMER_2R
		if (mHardwareVersion == HW_ID.SHIMMER_2R || mHardwareVersion == HW_ID.SHIMMER_2){
			if (((enabledSensors & 0xFFFFF) & SENSOR_BATT) > 0 ){
				enabledSensors = enabledSensors & 0xFFFF;
				enabledSensors = enabledSensors|SENSOR_EXP_BOARD_A0|SENSOR_EXP_BOARD_A7;
			}
			hardwareSensorBitmap  = enabledSensors;
		} else if (mHardwareVersion == HW_ID.SHIMMER_3){
			if (((enabledSensors & 0xFF)& SENSOR_ACCEL) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_A_ACCEL;
			}
			if ((enabledSensors & SENSOR_DACCEL) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL;
			}
			if (((enabledSensors & 0xFF)& SENSOR_EXG1_24BIT) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXG1_24BIT;
			}
			if (((enabledSensors & 0xFF)& SENSOR_EXG2_24BIT) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXG2_24BIT;
			}

			if ((enabledSensors& SENSOR_EXG1_16BIT) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXG1_16BIT;
			}
			if ((enabledSensors & SENSOR_EXG2_16BIT) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXG2_16BIT;
			}
			if (((enabledSensors & 0xFF)& SENSOR_GYRO) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO;
			}
			if (((enabledSensors & 0xFF)& SENSOR_MAG) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG;
			}
			if ((enabledSensors & SENSOR_BATT) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_VBATT;
			}
			if ((enabledSensors & SENSOR_EXT_ADC_A7) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXT_A7;
			}
			if ((enabledSensors & SENSOR_EXT_ADC_A6) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXT_A6;
			}
			if ((enabledSensors & SENSOR_EXT_ADC_A15) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_EXT_A15;
			}
			if ((enabledSensors & SENSOR_INT_ADC_A1) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A1;
			}
			if ((enabledSensors & SENSOR_INT_ADC_A12) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A12;
			}
			if ((enabledSensors & SENSOR_INT_ADC_A13) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A13;
			}
			if ((enabledSensors & SENSOR_INT_ADC_A14) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A14;
			}
			if  ((enabledSensors & SENSOR_BMP180) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_BMP180;
			} 
			if ((enabledSensors & SENSOR_GSR) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_GSR;
			}
			if ((enabledSensors & SENSOR_BRIDGE_AMP) > 0){
				hardwareSensorBitmap = hardwareSensorBitmap|Configuration.Shimmer3.SensorBitmap.SENSOR_BRIDGE_AMP;
			} 
		} else { 
			hardwareSensorBitmap  = enabledSensors;
		}

		return hardwareSensorBitmap;
	}

	public void toggleLed() {
		mListofInstructions.add(new byte[]{TOGGLE_LED_COMMAND});
	}
	
	@Override
	protected void checkBattery(){
		if (mStreaming ){
			if(mHardwareVersion == HW_ID.SHIMMER_3 && mFirmwareIdentifier==3){
				if (!mWaitForAck) {	
					if (mVSenseBattMA.getMean()<mLowBattLimit*1000*0.8) {
						if (mCurrentLEDStatus!=2) {
							writeLEDCommand(2);
						}
					} else if (mVSenseBattMA.getMean()<mLowBattLimit*1000) {
						if (mCurrentLEDStatus!=1) {
							writeLEDCommand(1);
						}
					} else if(mVSenseBattMA.getMean()>mLowBattLimit*1000+100) { //+100 is to make sure the limits are different to prevent excessive switching when the batt value is at the threshold
						if (mCurrentLEDStatus!=0) {
							writeLEDCommand(0);
						}
					}

				}
			}
			if(mHardwareVersion == HW_ID.SHIMMER_2R){
				if (!mWaitForAck) {	
					if (mVSenseBattMA.getMean()<mLowBattLimit*1000) {
						if (mCurrentLEDStatus!=1) {
							writeLEDCommand(1);
						}
					} else if(mVSenseBattMA.getMean()>mLowBattLimit*1000+100) { //+100 is to make sure the limits are different to prevent excessive switching when the batt value is at the threshold
						if (mCurrentLEDStatus!=0) {
							writeLEDCommand(0);
						}
					}

				}
			}
			

		
		}
	
	}
	
	public void enableCheckifAlive(boolean set){
		mCheckIfConnectionisAlive = set;
	}
	
	public void resetCalibratedTimeStamp(){
		mLastReceivedCalibratedTimeStamp = -1;
		mFirstTimeCalTime = true;
		mCurrentTimeStampCycle = 0;
	}
	
	//endregion
	
	public Object slotDetailsGetMethods(String componentName) {
		Object returnValue = null;
		switch(componentName){
//Booleans
			case(Configuration.Shimmer3.GuiLabelConfig.KINEMATIC_LPM):
				if(isLSM303DigitalAccelLPM()&&checkLowPowerGyro()&&checkLowPowerMag()) {
					returnValue = true;
				}
				else {
					returnValue = false;
				}
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_LPM):
				returnValue = isLSM303DigitalAccelLPM();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_LPM):
				returnValue = checkLowPowerGyro();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_LPM):
				returnValue = checkLowPowerMag();
	        	break;
			
//Integers
			case(Configuration.Shimmer3.GuiLabelConfig.BLUETOOTH_BAUD_RATE):
				returnValue = getBluetoothBaudRate();
	        	break;
    	
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE):
				returnValue = getAccelRange();
	        	break;
	        
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RANGE):
				returnValue = getGyroRange();
	        	break;
	
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RANGE):
				//TODO check below and commented out code
				returnValue = getMagRange();
			
//					// firmware sets mag range to 7 (i.e. index 6 in combobox) if user set mag range to 0 in config file
//					if(getMagRange() == 0) cmBx.setSelectedIndex(6);
//					else cmBx.setSelectedIndex(getMagRange()-1);
	    		break;
			
			case(Configuration.Shimmer3.GuiLabelConfig.PRESSURE_RESOLUTION):
				returnValue = getPressureResolution();
	    		break;
	    		
			case(Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE):
				returnValue = getGSRRange(); //TODO: check with RM re firmware bug??
	        	break;
	        	
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION):
				returnValue = getExGResolution();
	    		break;
	        	
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN):
				//TODO: What should this be?
				//returnValue = getExGGainSetting();
				//consolePrintLn("Get " + configValue);
	        	break;
	        	
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE):
				int configValue = getLSM303DigitalAccelRate(); 
				 
	        	if(!isLSM303DigitalAccelLPM()) {
		        	if(configValue==8) {
		        		configValue = 9;
		        	}
	        	}
				returnValue = configValue;
	    		break;
	    		
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RATE):
				returnValue = getLSM303MagRate();
	        	break;

			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_ACCEL_RANGE):
				returnValue = getMPU9150AccelRange();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_DMP_GYRO_CAL):
				returnValue = getMPU9150MotCalCfg();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_LPF):
				returnValue = getMPU9150LPF();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_RATE):
				returnValue = getMPU9150MPLSamplingRate();
        		break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MAG_RATE):
				returnValue = getMPU9150MagSamplingRate();
            	break;
            	
        	//TODO
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RATE):
				returnValue = getEXG1RateSetting();
				//returnValue = getEXG2RateSetting();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_REFERENCE_ELECTRODE):
				returnValue = getEXGReferenceElectrode();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_DETECTION):
				returnValue = getEXG2LeadOffCurrentMode();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_CURRENT):
				returnValue = getEXGLeadOffDetectionCurrent();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_COMPARATOR):
				returnValue = getEXGLeadOffComparatorTreshold();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_FREQ):
				returnValue = getEXG2RespirationDetectFreq();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_PHASE):
				returnValue = getEXG2RespirationDetectPhase();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER):
				returnValue = getInternalExpPower();
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.PPG_ADC_SELECTION):
				returnValue = getPpgAdcSelectionGsrBoard();
	    		break;
			case(Configuration.Shimmer3.GuiLabelConfig.PPG1_ADC_SELECTION):
				returnValue = getPpg1AdcSelectionProto3DeluxeBoard();
	    		break;
			case(Configuration.Shimmer3.GuiLabelConfig.PPG2_ADC_SELECTION):
				returnValue = getPpg2AdcSelectionProto3DeluxeBoard();
	    		break;
            	

			case(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_GSR):
				returnValue = getSamplingDividerGsr();
	    		break;
			case(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_LSM303DLHC_ACCEL):
				returnValue = getSamplingDividerLsm303dlhcAccel();
	    		break;
			case(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_PPG):
				returnValue = getSamplingDividerPpg();
	    		break;
			case(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_VBATT):
				returnValue = getSamplingDividerVBatt();
	    		break;
	    		
	    		
//Strings
			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_USER_ASSIGNED_NAME):
				returnValue = getShimmerUserAssignedName();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_NAME):
				returnValue = getExperimentName();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_SAMPLING_RATE):
		        Double readSamplingRate = getShimmerSamplingRate();
		    	Double actualSamplingRate = 32768/Math.floor(32768/readSamplingRate); // get Shimmer compatible sampling rate
		    	actualSamplingRate = (double)Math.round(actualSamplingRate * 100) / 100; // round sampling rate to two decimal places
//			    	consolePrintLn("GET SAMPLING RATE: " + componentName);
		    	returnValue = actualSamplingRate.toString();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.BUFFER_SIZE):
				returnValue = Integer.toString(getBufferSize());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.CONFIG_TIME):
	        	returnValue = getConfigTimeParsed();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_MAC_FROM_INFOMEM):
	        	returnValue = getMacIdFromInfoMem();
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_ID):
	        	returnValue = Integer.toString(getExperimentId());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_NUMBER_OF_SHIMMERS):
	        	returnValue = Integer.toString(getExperimentNumberOfShimmers());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_DURATION_ESTIMATED):
	        	returnValue = Integer.toString(getExperimentDurationEstimated());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_DURATION_MAXIMUM):
	        	returnValue = Integer.toString(getExperimentDurationMaximum());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.BROADCAST_INTERVAL):
	        	returnValue = Integer.toString(getSyncBroadcastInterval());
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RATE):
				returnValue = Double.toString((double)Math.round(getMPU9150GyroAccelRateInHz() * 100) / 100); // round sampling rate to two decimal places
//    		System.out.println("Gyro Sampling rate: " + getMPU9150GyroAccelRateInHz() + " " + returnValue);

	        	break;
	        default:
	        	break;
		}
		
		return returnValue;
	}		
	
	public Object slotDetailsSetMethods(String componentName, Object valueToSet) {

		Object returnValue = null;
		int buf = 0;

		switch(componentName){
//Booleans
			case(Configuration.Shimmer3.GuiLabelConfig.KINEMATIC_LPM):
				enableLowPowerAccel((boolean)valueToSet);
				enableLowPowerGyro((boolean)valueToSet);
            	enableLowPowerMag((boolean)valueToSet);
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_LPM):
				enableLowPowerAccel((boolean)valueToSet);
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_LPM):
            	this.enableLowPowerGyro((boolean)valueToSet);
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_LPM):
            	enableLowPowerMag((boolean)valueToSet);
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_BOOLEAN):
				if ((boolean)valueToSet){
					writeInternalExpPower(1);
				} else {
					writeInternalExpPower(0);
				}
	        	break;
        	
		
//Integers
			case(Configuration.Shimmer3.GuiLabelConfig.BLUETOOTH_BAUD_RATE):
				//TODO: Test
				writeBaudRate((int)valueToSet);
	        	break;
		        	
    		case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE):
    			writeAccelRange((int)valueToSet);
				break;
	        
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RANGE):
				writeGyroRange((int)valueToSet);
	        	break;
	
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RANGE):
				writeMagRange((int)valueToSet);
	    		break;
			
			case(Configuration.Shimmer3.GuiLabelConfig.PRESSURE_RESOLUTION):
				writePressureResolution((int)valueToSet);
	    		break;
	    		
			case(Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE):
	    		writeGSRRange((int)valueToSet);
	        	break;
	        	
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION):
				//writeExGResolution((int)valueToSet);
	    		break;
	    		
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN):
				
				//TODO: Implementation
				//consolePrintLn("before set " + getExGGain());
				//writeExGGainSetting((int)valueToSet);
				//consolePrintLn("after set " + getExGGain());
	        	break;
				
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE):
				writeAccelSamplingRate((int)valueToSet);
	    		break;
	    		
			case(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RATE):
				writeMagSamplingRate((int)valueToSet);
	        	break;
	        //TODO: regenerate EXG register bytes on each change (just in case)
	        	
        	//TODO
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_RATE):
				writeEXGRateSetting(1,(int)valueToSet);
				writeEXGRateSetting(2,(int)valueToSet);
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_REFERENCE_ELECTRODE):
				writeEXGReferenceElectrode((int)valueToSet);
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_DETECTION):
				writeEXGLeadOffDetectionMode((int)valueToSet);
				break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_CURRENT):
				writeEXGLeadOffDetectionCurrent((int)valueToSet);
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_COMPARATOR):
				writeEXGLeadOffComparatorTreshold((int)valueToSet);
            	break;
			case(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER):
				writeInternalExpPower((int)valueToSet);
            	break;
			
//Strings
			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_USER_ASSIGNED_NAME):
        		setShimmerName((String)valueToSet);
	        	break;
			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_SAMPLING_RATE):
	          	// don't let sampling rate be empty
	          	Double enteredSamplingRate;
	          	if(((String)valueToSet).isEmpty()) {
	          		enteredSamplingRate = 1.0;
	          	}            	
	          	else {
	          		enteredSamplingRate = Double.parseDouble((String)valueToSet);
	          	}
	      		setShimmerSamplingRate(enteredSamplingRate);
	      		
	      		returnValue = Double.toString(getShimmerSamplingRate());
	        	break;
//			case(Configuration.Shimmer3.GuiLabelConfig.BUFFER_SIZE):
//	        	break;
//			case(Configuration.Shimmer3.GuiLabelConfig.CONFIG_TIME):
//	        	break;
//			case(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_MAC_FROM_INFOMEM):
//	        	break;
			
			case(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RATE):
            	double bufDouble = 4.0; // Minimum = 4Hz
            	if(((String)valueToSet).isEmpty()) {
            		bufDouble = 4.0;
            	}
            	else {
            		bufDouble = Double.parseDouble((String)valueToSet);
            	}
            	
            	// Since user is manually entering a freq., clear low-power mode so that their chosen rate will be set correctly. Tick box will be re-enabled automatically if they enter LPM freq. 
            	enableLowPowerGyro(false); 
        		setMPU9150GyroAccelRateFromFreq(bufDouble);

        		returnValue = Double.toString((double)Math.round(getMPU9150GyroAccelRateInHz() * 100) / 100); // round sampling rate to two decimal places
//        		System.out.println("Gyro Sampling rate: " + getMPU9150GyroAccelRateInHz() + " " + returnValue);

	        	break;
	        default:
	        	break;
		}
			
		return returnValue;

	}

}
