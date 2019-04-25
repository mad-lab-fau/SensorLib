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
 * @author Jong Chern Lim, Ruaidhri Molloy, Alejandro Saez, Mark Nolan 
 * 
 * Changes since 1.8
 * - updated to work with SDLOG
 * - change method formatdatapacketreverse to protected
 * - added i32r to parsed data
 * - mSignalNameArray changed to protected
 * - fix twos complement for long numbers (32bit>=)
 * - fix 6dof calculation ~ long 
 * - fix intiali time stamp ~ long
 * 
 * 
 * Changes since 1.7
 * - remove multiplying factor (2.8) from the gain in the calculation of the Bridge Amplifier calibrated data
 * 
 * @date   September, 2014
 * 
 * Changes since 1.6
 * - Added functionality for plotmanager see MSSAPI , addExtraSignalProperty, removeExtraSignalProperty ,getListofEnabledSensorSignalsandFormats()
 * - various exg advance updates
 * 
 * @date   July, 2014
 * 
 * Changes since 1.5
 * - Bridge Amplifier gauge support for Shimmer3
 * - Bug fix for strain gauge calibration for Shimmer2r
 * - Enable 3D orientation for wide range accel, orientation algorithm defaults to low noise even if wide range is enabled
 * - Fixed quaternion naming typo
 * - Commented out initialisation mSensorBitmaptoName
 * - add method getPressureRawCoefficients
 *  
 * @date   October, 2013
 * 
 * Changes since 1.4
 * - fix getListofEnabledSensors, which was not returning accel shimmer2r
 * - fix null pointer graddes algo when using Shimmer2
 * - converted to abstract class , and added checkbattery abstract method for the Shimmer2r
 * - updated gsr calibrate command parameters for Shimmer3
 * - removed mShimmerSamplingRate decimal formatter, decimal formatter should be done on the UI
 * - fixed a GSR Shimmer2 problem when using autorange
 * - added VSense Batt and VSense Reg and Timestamp to getListOfEnabledSensorSignals
 * - added getSamplingRate()
 * - added get methods for calibration parameters accel,gyro,mag,accel2
 * - updated getoffsetaccel
 * - added exg configurations
 * - added i24r for exg
 * - add get exg configurations
 * - renamed i16* to i16r for consistency
 * - added EXG_CHIP1 = 0 and EXG_CHIP2=1
 * - updated Mag Default Range
 * 
 */

//v0.1.0 consensys

package com.shimmerresearch.driver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;


//import sun.util.calendar.BaseCalendar.Date;
//import sun.util.calendar.CalendarDate;
import java.util.Date;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.shimmerresearch.algorithms.GradDes3DOrientation;
import com.shimmerresearch.driver.ChannelDetails.ChannelDataEndian;
import com.shimmerresearch.driver.ChannelDetails.ChannelDataType;
import com.shimmerresearch.driver.Configuration.Shimmer2;
import com.shimmerresearch.driver.ShimmerVerDetails.FW_ID;
import com.shimmerresearch.driver.ShimmerVerDetails.HW_ID;
import com.shimmerresearch.driver.SensorDetails;
import com.shimmerresearch.driver.Configuration.Shimmer3;
import com.shimmerresearch.driver.ShimmerVerDetails.HW_ID_SR_CODES;
import com.shimmerresearch.algorithms.GradDes3DOrientation.Quaternion;

public abstract class ShimmerObject extends BasicProcessWithCallBack implements Serializable {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = -1364568867018921219L;
	
	public static final String ACCEL_CAL_UNIT = "m/s^2";
	public static final String ACCEL_DEFAULT_CAL_UNIT = "m/s^2";
	public static final String ADC_CAL_UNIT = "mV";
	public static final String GSR_CAL_UNIT = "kOhms";
	public static final String GYRO_CAL_UNIT = "deg/s";
	public static final String GYRO_DEFAULT_CAL_UNIT = "deg/s";
	public static final String MAG_CAL_UNIT = "local flux";
	public static final String MAG_DEFAULT_CAL_UNIT = "local flux";
	public static final String TEMP_CAL_UNIT = "Degrees Celsius";
	public static final String PRESSURE_CAL_UNIT = "kPa";
	public static final String HEARTRATE_CAL_UNIT = "bpm";
	
	public static final String NO_UNIT = "No Units";
	public static final String CLOCK_UNIT = "Ticks";
	
	public final static String DEFAULT_SHIMMER_NAME = "Shimmer";
	public final static String DEFAULT_EXPERIMENT_NAME = "Trial";

	protected boolean mFirstTime = true;
	double mFirstRawTS = 0;
	public int OFFSET_LENGTH = 9;
	
	//Sensor Bitmap for ID ; for the purpose of forward compatibility the sensor bitmap and the ID and the sensor bitmap for the Shimmer firmware has been kept separate, 
	public static final int SENSOR_ACCEL				   = 0x80; 
	public static final int SENSOR_DACCEL				   = 0x1000; //only 
	public static final int SENSOR_GYRO				   	   = 0x40;
	public static final int SENSOR_MAG					   = 0x20;
	public static final int SENSOR_ECG					   = 0x10;
	public static final int SENSOR_EMG					   = 0x08;
	public static final int SENSOR_EXG1_24BIT			   = 0x10; //only applicable for Shimmer3
	public static final int SENSOR_EXG2_24BIT			   = 0x08; //only applicable for Shimmer3
	public static final int SHIMMER3_SENSOR_ECG			   = SENSOR_EXG1_24BIT + SENSOR_EXG2_24BIT;
	public static final int SHIMMER3_SENSOR_EMG			   = SENSOR_EXG1_24BIT;
	public static final int SENSOR_GSR					   = 0x04;
	public static final int SENSOR_EXP_BOARD_A7		       = 0x02; //only Applicable for Shimmer2
	public static final int SENSOR_EXP_BOARD_A0		       = 0x01; //only Applicable for Shimmer2
	public static final int SENSOR_EXP_BOARD		       = SENSOR_EXP_BOARD_A7+SENSOR_EXP_BOARD_A0;
	public static final int SENSOR_BRIDGE_AMP			   = 0x8000;
	public static final int SENSOR_HEART				   = 0x4000;
	public static final int SENSOR_BATT	  			       = 0x2000; //THIS IS A DUMMY VALUE
	public static final int SENSOR_EXT_ADC_A7              = 0x02; //only Applicable for Shimmer3
	public static final int SENSOR_EXT_ADC_A6              = 0x01; //only Applicable for Shimmer3
	public static final int SENSOR_EXT_ADC_A15             = 0x0800;
	public static final int SENSOR_INT_ADC_A1              = 0x0400;
	public static final int SENSOR_INT_ADC_A12             = 0x0200;
	public static final int SENSOR_INT_ADC_A13             = 0x0100;
	public static final int SENSOR_INT_ADC_A14             = 0x800000;
	public static final int SENSOR_ALL_ADC_SHIMMER3        = SENSOR_INT_ADC_A14 | SENSOR_INT_ADC_A13 | SENSOR_INT_ADC_A12 | SENSOR_INT_ADC_A1 | SENSOR_EXT_ADC_A7 | SENSOR_EXT_ADC_A6 | SENSOR_EXT_ADC_A15; 
	public static final int SENSOR_BMP180              	   = 0x40000;
	public static final int SENSOR_EXG1_16BIT			   = 0x100000; //only applicable for Shimmer3
	public static final int SENSOR_EXG2_16BIT			   = 0x080000; //only applicable for Shimmer3
	public BiMap<String, String> mSensorBitmaptoName;  
	
	public class SDLogHeader {
		public final static int ACCEL_LN = 1<<7;
		public final static int GYRO = 1<<6;
		public final static int MAG = 1<<5;
		public final static int EXG1_24BIT = 1<<4;
		public final static int EXG2_24BIT = 1<<3;
		public final static int GSR = 1<<2;
		public final static int EXT_EXP_A7 = 1<<1;
		public final static int EXT_EXP_A6 = 1<<0;
		public final static int BRIDGE_AMP = 1<<15;
		// 1<<9 NONE 
		public final static int BATTERY = 1<<13;
		public final static int ACCEL_WR = 1<<12;
		public final static int EXT_EXP_A15 = 1<<11;
		public final static int INT_EXP_A1 = 1<<10;
		public final static int INT_EXP_A12 = 1<<9;
		public final static int INT_EXP_A13 = 1<<8;
		public final static int INT_EXP_A14 = 1<<23;
		public final static int ACCEL_MPU = 1<<22;
		public final static int MAG_MPU = 1<<21;
		public final static int EXG1_16BIT = 1<<20;
		public final static int EXG2_16BIT = 1<<19;
		public final static int BMP180 = 1<<18;
		public final static int MPL_TEMPERATURE = 1<<17;
		// 1<<23
		public final static int MPL_QUAT_6DOF = 1<<31;
		public final static int MPL_QUAT_9DOF = 1<<30;
		public final static int MPL_EULER_6DOF = 1<<29;
		public final static int MPL_EULER_9DOF = 1<<28;
		public final static int MPL_HEADING = 1<<27;
		public final static int MPL_PEDOMETER = 1<<26;
		public final static int MPL_TAP = 1<<25;
		public final static int MPL_MOTION_ORIENT = 1<<24;
		public final static long GYRO_MPU_MPL = (long)1<<39;
		public final static long ACCEL_MPU_MPL = (long)1<<38;
		public final static long MAG_MPU_MPL = (long)1<<37;
		public final static long SD_SENSOR_MPL_QUAT_6DOF_RAW = (long)1<<36;
	}
	
	public class SDLogHeaderDerivedSensors{
		public final static int PPG2_1_14 = 1<<4;
		public final static int PPG1_12_13 = 1<<3;
		public final static int PPG_12_13 = 1<<2;
		public final static int SKIN_TEMP = 1<<1;
		public final static int RES_AMP = 1<<0;
	}
	
	public class BTStream {
		public final static int ACCEL_LN = 0x80; 
		public final static int GYRO = 0x40;
		public final static int MAG = 0x20;
		public final static int EXG1_24BIT = 0x10;
		public final static int EXG2_24BIT = 0x08;
		public final static int GSR = 0x04;
		public final static int EXT_EXP_A7 = 0x02;
		public final static int EXT_EXP_A6 = 0x01;
		public final static int BRIDGE_AMP = 0x8000;
		// 1<<9 NONE 
		public final static int BATTERY = 0x2000;
		public final static int ACCEL_WR = 0x1000;
		public final static int EXT_EXP_A15 = 0x0800;
		public final static int INT_EXP_A1 = 0x0400;
		public final static int INT_EXP_A12 = 0x0200;
		public final static int INT_EXP_A13 = 0x0100;
		public final static int INT_EXP_A14 = 0x800000;
		public final static int ACCEL_MPU = 0x400000;
		public final static int MAG_MPU = 0x200000;
		public final static int EXG1_16BIT = 0x100000;
		public final static int EXG2_16BIT = 0x080000;
		public final static int BMP180 = 0x40000;
		public final static int MPL_TEMPERATURE = 1<<22;
		// 1<<23
		//public final static int MPL_QUAT_6DOF = 1<<24;
		//public final static int MPL_QUAT_9DOF = 1<<25;
		//public final static int MPL_EULER_6DOF = 1<<26;
		//public final static int MPL_EULER_9DOF = 1<<27;
		//public final static int MPL_HEADING = 1<<28;
		//public final static int MPL_PEDOMETER = 1<<29;
		//public final static int MPL_TAP = 1<<30;
		//public final static int MPL_MOTION_ORIENT = 1<<31;
		//public final static long GYRO_MPU_MPL = 1<<32;
		//public final static long ACCEL_MPU_MPL = 1<<33;
		//public final static long MAG_MPU_MPL = 1<<34;
		//public final static long SD_SENSOR_MPL_QUAT_6DOF_RAW = 1<<35;
	}
	
	
	/*
		{  
		final Map<String, String> tempSensorBMtoName = new HashMap<String, String>();  
		tempSensorBMtoName.put(Integer.toString(SENSOR_ACCEL), "Accelerometer");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_GYRO), "Gyroscope");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_MAG), "Magnetometer");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_ECG), "ECG");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_EMG), "EMG");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_GSR), "GSR");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A7), "Exp Board A7");
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A0), "Exp Board A0");
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD), "Exp Board");
		tempSensorBMtoName.put(Integer.toString(SENSOR_BRIDGE_AMP), "Bridge Amplifier");
		tempSensorBMtoName.put(Integer.toString(SENSOR_HEART), "Heart Rate");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_BATT), "Battery Voltage");
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A7), "External ADC A7");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A6), "External ADC A6");  
		tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A15), "External ADC A15");
		tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A1), "Internal ADC A1");
		tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A12), "Internal ADC A12");
		tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A13), "Internal ADC A13");
		tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A14), "Internal ADC A14");
		mSensorBitmaptoName = ImmutableBiMap.copyOf(Collections.unmodifiableMap(tempSensorBMtoName));
		}  
	 */
	
	protected TreeMap<Integer,SensorDetails> mSensorMap = new TreeMap<Integer,SensorDetails>();
	protected LinkedHashMap<String,SensorTileDetails> mSensorTileMap = new LinkedHashMap<String,SensorTileDetails>();
	protected HashMap<String,SensorConfigOptionDetails> mConfigOptionsMap = new HashMap<String,SensorConfigOptionDetails>();

	
	//Constants describing the packet type
	public static final byte DATA_PACKET                      		= (byte) 0x00;
	public static final byte INQUIRY_COMMAND                  		= (byte) 0x01;
	public static final byte INQUIRY_RESPONSE                 		= (byte) 0x02;
	public static final byte GET_SAMPLING_RATE_COMMAND 	   			= (byte) 0x03;
	public static final byte SAMPLING_RATE_RESPONSE           		= (byte) 0x04;
	public static final byte SET_SAMPLING_RATE_COMMAND        		= (byte) 0x05;
	public static final byte TOGGLE_LED_COMMAND              		= (byte) 0x06;
	public static final byte START_STREAMING_COMMAND          		= (byte) 0x07;
	public static final byte SET_SENSORS_COMMAND              		= (byte) 0x08;
	public static final byte SET_ACCEL_SENSITIVITY_COMMAND    		= (byte) 0x09;
	public static final byte ACCEL_SENSITIVITY_RESPONSE       		= (byte) 0x0A;
	public static final byte GET_ACCEL_SENSITIVITY_COMMAND    		= (byte) 0x0B;
	public static final byte SET_5V_REGULATOR_COMMAND         		= (byte) 0x0C; // only Shimmer 2
	public static final byte SET_PMUX_COMMAND                 		= (byte) 0x0D; // only Shimmer 2
	public static final byte SET_CONFIG_BYTE0_COMMAND   	   		= (byte) 0x0E;
	public static final byte CONFIG_BYTE0_RESPONSE      	   		= (byte) 0x0F;
	public static final byte GET_CONFIG_BYTE0_COMMAND   	   		= (byte) 0x10;
	public static final byte STOP_STREAMING_COMMAND           		= (byte) 0x20;
	public static final byte SET_ACCEL_CALIBRATION_COMMAND			= (byte) 0x11;
	public static final byte ACCEL_CALIBRATION_RESPONSE       		= (byte) 0x12;
	public static final byte LSM303DLHC_ACCEL_CALIBRATION_RESPONSE 	= (byte) 0x1B;
	public static final byte SET_LSM303DLHC_ACCEL_CALIBRATION_COMMAND = (byte) 0x1A;
	public static final byte GET_ACCEL_CALIBRATION_COMMAND    		= (byte) 0x13;
	public static final byte SET_GYRO_CALIBRATION_COMMAND 	  		= (byte) 0x14;
	public static final byte GYRO_CALIBRATION_RESPONSE        		= (byte) 0x15;
	public static final byte GET_GYRO_CALIBRATION_COMMAND     		= (byte) 0x16;
	public static final byte SET_MAG_CALIBRATION_COMMAND      		= (byte) 0x17;
	public static final byte MAG_CALIBRATION_RESPONSE         		= (byte) 0x18;
	public static final byte GET_MAG_CALIBRATION_COMMAND      		= (byte) 0x19;
	public static final byte SET_GSR_RANGE_COMMAND			   		= (byte) 0x21;
	public static final byte GSR_RANGE_RESPONSE			   			= (byte) 0x22;
	public static final byte GET_GSR_RANGE_COMMAND			   		= (byte) 0x23;
	public static final byte GET_SHIMMER_VERSION_COMMAND      		= (byte) 0x24;
	public static final byte GET_SHIMMER_VERSION_COMMAND_NEW      	= (byte) 0x3F; //this is to avoid the $ char which is used by rn42
	public static final byte GET_SHIMMER_VERSION_RESPONSE     		= (byte) 0x25;
	public static final byte SET_EMG_CALIBRATION_COMMAND      		= (byte) 0x26;
	public static final byte EMG_CALIBRATION_RESPONSE         		= (byte) 0x27;
	public static final byte GET_EMG_CALIBRATION_COMMAND      		= (byte) 0x28;
	public static final byte SET_ECG_CALIBRATION_COMMAND      		= (byte) 0x29;
	public static final byte ECG_CALIBRATION_RESPONSE         		= (byte) 0x2A;
	public static final byte GET_ECG_CALIBRATION_COMMAND      		= (byte) 0x2B;
	public static final byte GET_ALL_CALIBRATION_COMMAND      		= (byte) 0x2C;
	public static final byte ALL_CALIBRATION_RESPONSE         		= (byte) 0x2D; 
	public static final byte GET_FW_VERSION_COMMAND          		= (byte) 0x2E;
	public static final byte FW_VERSION_RESPONSE             	 	= (byte) 0x2F;
	public static final byte SET_BLINK_LED                    		= (byte) 0x30;
	public static final byte BLINK_LED_RESPONSE               		= (byte) 0x31;
	public static final byte GET_BLINK_LED                    		= (byte) 0x32;
	public static final byte SET_GYRO_TEMP_VREF_COMMAND       		= (byte) 0x33;
	public static final byte SET_BUFFER_SIZE_COMMAND          		= (byte) 0x34;
	public static final byte BUFFER_SIZE_RESPONSE             		= (byte) 0x35;
	public static final byte GET_BUFFER_SIZE_COMMAND          		= (byte) 0x36;
	public static final byte SET_MAG_GAIN_COMMAND             		= (byte) 0x37;
	public static final byte MAG_GAIN_RESPONSE                		= (byte) 0x38;
	public static final byte GET_MAG_GAIN_COMMAND             		= (byte) 0x39;
	public static final byte SET_MAG_SAMPLING_RATE_COMMAND    		= (byte) 0x3A;
	public static final byte MAG_SAMPLING_RATE_RESPONSE       		= (byte) 0x3B;
	public static final byte GET_MAG_SAMPLING_RATE_COMMAND    		= (byte) 0x3C;
	public static final byte SET_ACCEL_SAMPLING_RATE_COMMAND  		= (byte) 0x40;
	public static final byte ACCEL_SAMPLING_RATE_RESPONSE  			= (byte) 0x41;
	public static final byte GET_ACCEL_SAMPLING_RATE_COMMAND  		= (byte) 0x42;
	public static final byte SET_LSM303DLHC_ACCEL_LPMODE_COMMAND 	= (byte) 0x43;
	public static final byte LSM303DLHC_ACCEL_LPMODE_RESPONSE		= (byte) 0x44;
	public static final byte GET_LSM303DLHC_ACCEL_LPMODE_COMMAND 	= (byte) 0x45;
	public static final byte SET_LSM303DLHC_ACCEL_HRMODE_COMMAND	= (byte) 0x46;
	public static final byte LSM303DLHC_ACCEL_HRMODE_RESPONSE		= (byte) 0x47;
	public static final byte GET_LSM303DLHC_ACCEL_HRMODE_COMMAND 	= (byte) 0x48;
	public static final byte SET_MPU9150_GYRO_RANGE_COMMAND 		= (byte) 0x49;
	public static final byte MPU9150_GYRO_RANGE_RESPONSE 			= (byte) 0x4A;
	public static final byte GET_MPU9150_GYRO_RANGE_COMMAND 		= (byte) 0x4B;
	public static final byte SET_MPU9150_SAMPLING_RATE_COMMAND 		= (byte) 0x4C;
	public static final byte MPU9150_SAMPLING_RATE_RESPONSE 		= (byte) 0x4D;
	public static final byte GET_MPU9150_SAMPLING_RATE_COMMAND 		= (byte) 0x4E;
	public static final byte SET_BMP180_PRES_RESOLUTION_COMMAND 	= (byte) 0x52;
	public static final byte BMP180_PRES_RESOLUTION_RESPONSE 		= (byte) 0x53;
	public static final byte GET_BMP180_PRES_RESOLUTION_COMMAND 	= (byte) 0x54;
	public static final byte SET_BMP180_PRES_CALIBRATION_COMMAND	= (byte) 0x55;
	public static final byte BMP180_PRES_CALIBRATION_RESPONSE 		= (byte) 0x56;
	public static final byte GET_BMP180_PRES_CALIBRATION_COMMAND 	= (byte) 0x57;
	public static final byte BMP180_CALIBRATION_COEFFICIENTS_RESPONSE = (byte) 0x58;
	public static final byte GET_BMP180_CALIBRATION_COEFFICIENTS_COMMAND = (byte) 0x59;
	public static final byte RESET_TO_DEFAULT_CONFIGURATION_COMMAND = (byte) 0x5A;
	public static final byte RESET_CALIBRATION_VALUE_COMMAND 		= (byte) 0x5B;
	public static final byte MPU9150_MAG_SENS_ADJ_VALS_RESPONSE 	= (byte) 0x5C;
	public static final byte GET_MPU9150_MAG_SENS_ADJ_VALS_COMMAND 	= (byte) 0x5D;
	public static final byte SET_INTERNAL_EXP_POWER_ENABLE_COMMAND 	= (byte) 0x5E;
	public static final byte INTERNAL_EXP_POWER_ENABLE_RESPONSE 	= (byte) 0x5F;
	public static final byte GET_INTERNAL_EXP_POWER_ENABLE_COMMAND 	= (byte) 0x60;
	public static final byte SET_EXG_REGS_COMMAND 					= (byte) 0x61;
	public static final byte EXG_REGS_RESPONSE 						= (byte) 0x62;
	public static final byte GET_EXG_REGS_COMMAND 					= (byte) 0x63;
	public static final byte DAUGHTER_CARD_ID_RESPONSE 				= (byte) 0x65;
	public static final byte GET_DAUGHTER_CARD_ID_COMMAND			= (byte) 0x66;
	public static final byte SET_BAUD_RATE_COMMAND 					= (byte) 0x6A;
	public static final byte BAUD_RATE_RESPONSE 					= (byte) 0x6B;
	public static final byte GET_BAUD_RATE_COMMAND 					= (byte) 0x6C;
	public static final byte START_SDBT_COMMAND 					= (byte) 0x70;
	public static final byte STATUS_RESPONSE	 					= (byte) 0x71;
	public static final byte GET_STATUS_COMMAND 					= (byte) 0x72;
	public static final byte DIR_RESPONSE		 					= (byte) 0x88;
	public static final byte GET_DIR_COMMAND 						= (byte) 0x89;
	public static final byte INSTREAM_CMD_RESPONSE 					= (byte) 0x8A;
	public static final byte SET_RWC_COMMAND                        = (byte) 0x8F;
	public static final byte RWC_RESPONSE                           = (byte) 0x90;
	public static final byte GET_RWC_COMMAND                        = (byte) 0x91;
	public static final byte SET_VBATT_FREQ_COMMAND                 = (byte) 0x98;
	public static final byte VBATT_FREQ_RESPONSE                    = (byte) 0x99;
	public static final byte GET_VBATT_FREQ_COMMAND                 = (byte) 0x9A;
	public static final byte ACK_COMMAND_PROCESSED            		= (byte) 0xFF;
	protected String mMyName="";														// This stores the user assigned name
	protected String mMyBluetoothAddress="";
	public static final int MAX_NUMBER_OF_SIGNALS = 50; //used to be 11 but now 13 because of the SR30 + 8 for 3d orientation
	public static final int MAX_INQUIRY_PACKET_SIZE = 47;
//	protected int mFWCode=0;

	public final static int MSP430_5XX_INFOMEM_D_ADDRESS = 0x001800; 
	public final static int MSP430_5XX_INFOMEM_C_ADDRESS = 0x001880; 
	public final static int MSP430_5XX_INFOMEM_B_ADDRESS = 0x001900;
	public final static int MSP430_5XX_INFOMEM_A_ADDRESS = 0x001980; 
	public final static int MSP430_5XX_INFOMEM_LAST_ADDRESS = 0x0019FF;
//	public final static int MSP430_5XX_PROGRAM_START_ADDRESS = 0x00FFFE; 
	
	protected int mHardwareVersion=-1;
	public String mHardwareVersionParsed = "";

	public int mFirmwareVersionCode = 0;
	public int mFirmwareIdentifier = 0;
	public int mFirmwareVersionMajor = 0;
	public int mFirmwareVersionMinor = 0;
	public int mFirmwareVersionInternal = 0;
	public String mFirmwareVersionParsed = "";
	
	public int mExpansionBoardId = -1; 
	public int mExpansionBoardRev = -1;
	public int mExpansionBoardSpecialRev = -1;
	public String mExpansionBoardParsed = "";  
	public String mExpansionBoardParsedWithVer = "";  
	public static final int ANY_VERSION = -1;
	
	//TODO: change mExpBoardArray from ShimmerObject to mShimmerExpansionBoardId or are they different?
	//TODO: change mExpBoardName from ShimmerObject to mShimmerExpansionBoardParsed and mShimmerExpansionBoardParsedWithVer 
	protected byte[] mExpBoardArray; // Array where the expansion board response is stored
	protected String mExpBoardName; // Name of the expansion board. ONLY SHIMMER 3
	
	protected String mClassName="Shimmer";
	private double mLastReceivedTimeStamp=0;
	protected double mCurrentTimeStampCycle=0;
	protected double mShimmerSamplingRate; 	                                        	// 51.2Hz is the default sampling rate 
	protected long mEnabledSensors = (long)0;												// This stores the enabled sensors
	protected long mDerivedSensors = (long)0;												// This stores the sensors channels derived in SW
	protected int mBluetoothBaudRate=0;

	protected int mPacketSize=0; 													// Default 2 bytes for time stamp and 6 bytes for accelerometer 
	protected int mAccelRange=0;													// This stores the current accelerometer range being used. The accelerometer range is stored during two instances, once an ack packet is received after a writeAccelRange(), and after a response packet has been received after readAccelRange()  	
//	protected boolean mLowPowerAccelWR = false; //TODO: add comment and set from BT command
	protected boolean mHighResAccelWR = false; //TODO: add comment and set from BT command
	protected int mLSM303MagRate=4;												// This stores the current Mag Sampling rate, it is a value between 0 and 6; 0 = 0.5 Hz; 1 = 1.0 Hz; 2 = 2.0 Hz; 3 = 5.0 Hz; 4 = 10.0 Hz; 5 = 20.0 Hz; 6 = 50.0 Hz
	protected int mLSM303DigitalAccelRate=0;
	protected int mMPU9150GyroAccelRate=0;
	protected int mMagRange=1;														// Currently not supported on Shimmer2. This stores the current Mag Range, it is a value between 0 and 6; 0 = 0.7 Ga; 1 = 1.0 Ga; 2 = 1.5 Ga; 3 = 2.0 Ga; 4 = 3.2 Ga; 5 = 3.8 Ga; 6 = 4.5 Ga
	protected int mGyroRange=1;													// This stores the current Gyro Range, it is a value between 0 and 3; 0 = +/- 250dps,1 = 500dps, 2 = 1000dps, 3 = 2000dps 
	protected int mMPU9150AccelRange=0;											// This stores the current MPU9150 Accel Range. 0 = 2g, 1 = 4g, 2 = 8g, 4 = 16g
	protected int mGSRRange=4;													// This stores the current GSR range being used.
	protected int mPastGSRRange=4; // this is to fix a bug with SDLog v0.9
	protected int mPastGSRUncalibratedValue=4; // this is to fix a bug with SDLog v0.9
	protected boolean mPastGSRFirstTime=true; // this is to fix a bug with SDLog v0.9
	protected int mInternalExpPower=-1;													// This shows whether the internal exp power is enabled.
	protected long mConfigByte0;
	protected int mNChannels=0;	                                                // Default number of sensor channels set to three because of the on board accelerometer 
	protected int mBufferSize;                   							

	protected String[] mSignalNameArray=new String[MAX_NUMBER_OF_SIGNALS];							// 19 is the maximum number of signal thus far
	protected String[] mSignalDataTypeArray=new String[MAX_NUMBER_OF_SIGNALS];						// 19 is the maximum number of signal thus far
	protected boolean mDefaultCalibrationParametersECG = true;
	protected boolean mDefaultCalibrationParametersEMG = true;
	protected boolean mDefaultCalibrationParametersAccel = true;
	protected boolean mDefaultCalibrationParametersDigitalAccel = true; //Also known as the wide range accelerometer
	protected int mPressureResolution = 0;
	protected int mExGResolution = 0;
	protected int mShimmer2MagRate=0;
	
	//TODO add comments and default values
	//protected int mDigitalAccelRange = 0;
	//protected int mAccelRange = 0;
//	protected int mAccelLPM = 0;
//	protected int mAccelHPM = 0;
//	protected int mMPU9150GyroRate = 0;
//	protected int mMPUAccelRange = 0;
	protected int mMPU9150DMP = 0;
	protected int mMPU9150LPF = 0;
	protected int mMPU9150MotCalCfg = 0;
	protected int mMPU9150MPLSamplingRate = 0;
	protected int mMPU9150MagSamplingRate = 0;
	protected int mMPLSensorFusion = 0;
	protected int mMPLGyroCalTC = 0;
	protected int mMPLVectCompCal = 0;
	protected int mMPLMagDistCal = 0;
	protected int mMPLEnable = 0;
	protected int mButtonStart = 0;
	protected int mMasterShimmer = 0;
	protected int mSingleTouch = 0;
	protected int mTCXO = 0;
	protected long mConfigTime; //this is in milliseconds, utc
	protected long mRTCOffset; //this is in ticks
	protected int mSyncWhenLogging = 0;
	protected int mSyncBroadcastInterval = 0;
	protected byte[] mInfoMemBytes = createEmptyInfoMemByteArray(512);
	protected boolean mShimmerUsingConfigFromInfoMem = false;
	
	protected long mInitialTimeStamp;

	protected final static int FW_TYPE_BT=0;
	protected final static int FW_TYPE_SD=1;
	
	protected String mShimmerUserAssignedName = "";  //TODO: this seems to be a duplicate of mMyName and ?devicename?
	protected String mExperimentName = ""; //TODO: this seems to be a duplicate of mMyTrial
	protected int mExperimentId = 0;
	protected int mExperimentNumberOfShimmers = 0;
	protected int mExperimentDurationEstimated = 0;
	protected int mExperimentDurationMaximum = 0;
	
	protected String mMacIdFromInfoMem = "";
	protected String mMacIdFromUart = "";
	protected String mMacIdFromUartParsed = "";
	
	protected boolean mConfigFileCreationFlag = false;
	protected boolean mCalibFileCreationFlag = false;
	
	protected List<String> syncNodesList = new ArrayList<String>();
	
	protected int mPpgAdcSelectionGsrBoard = 0;
	protected int mPpg1AdcSelectionProto3DeluxeBoard = 0;
	protected int mPpg2AdcSelectionProto3DeluxeBoard = 0;
	

	//
	protected double[][] mAlignmentMatrixAnalogAccel = {{-1,0,0},{0,-1,0},{0,0,1}}; 			
	protected double[][] mSensitivityMatrixAnalogAccel = {{38,0,0},{0,38,0},{0,0,38}}; 	
	protected double[][] mOffsetVectorAnalogAccel = {{2048},{2048},{2048}};				
	protected abstract void checkBattery();
	protected double[][] mAlignmentMatrixWRAccel = {{-1,0,0},{0,1,0},{0,0,-1}}; 			
	protected double[][] mSensitivityMatrixWRAccel = {{1631,0,0},{0,1631,0},{0,0,1631}}; 	
	protected double[][] mOffsetVectorWRAccel = {{0},{0},{0}};	

	//Default values Shimmer2 
	protected static double[][] SensitivityMatrixAccel1p5gShimmer2 = {{101,0,0},{0,101,0},{0,0,101}};
	protected static double[][] SensitivityMatrixAccel2gShimmer2 = {{76,0,0},{0,76,0},{0,0,76}};
	protected static double[][] SensitivityMatrixAccel4gShimmer2 = {{38,0,0},{0,38,0},{0,0,38}};
	protected static double[][] SensitivityMatrixAccel6gShimmer2 = {{25,0,0},{0,25,0},{0,0,25}};
	protected static double[][] AlignmentMatrixAccelShimmer2 =  {{-1,0,0},{0,-1,0},{0,0,1}}; 			
	protected static double[][] OffsetVectorAccelShimmer2 = {{2048},{2048},{2048}};			
	//Shimmer3
	protected static double[][] SensitivityMatrixLowNoiseAccel2gShimmer3 = {{83,0,0},{0,83,0},{0,0,83}};
	protected static double[][] SensitivityMatrixWideRangeAccel2gShimmer3 = {{1631,0,0},{0,1631,0},{0,0,1631}};
	protected static double[][] SensitivityMatrixWideRangeAccel4gShimmer3 = {{815,0,0},{0,815,0},{0,0,815}};
	protected static double[][] SensitivityMatrixWideRangeAccel8gShimmer3 = {{408,0,0},{0,408,0},{0,0,408}};
	protected static double[][] SensitivityMatrixWideRangeAccel16gShimmer3 = {{135,0,0},{0,135,0},{0,0,135}};
	protected static double[][] AlignmentMatrixLowNoiseAccelShimmer3 = {{0,-1,0},{-1,0,0},{0,0,-1}};

	protected static double[][] AlignmentMatrixWideRangeAccelShimmer3 = {{-1,0,0},{0,1,0},{0,0,-1}}; 	
	protected static double[][] AlignmentMatrixAccelShimmer3 = {{0,-1,0},{-1,0,0},{0,0,-1}};
	protected static double[][] OffsetVectorLowNoiseAccelShimmer3 = {{2047},{2047},{2047}};
	protected static double[][] OffsetVectorWideRangeAccelShimmer3 = {{0},{0},{0}};

	protected double OffsetECGRALL=2060;
	protected double GainECGRALL=175;
	protected double OffsetECGLALL=2060;
	protected double GainECGLALL=175;
	protected double OffsetEMG=2060;
	protected double GainEMG=750;

	protected boolean mDefaultCalibrationParametersGyro = true;
	protected double[][] mAlignmentMatrixGyroscope = {{0,-1,0},{-1,0,0},{0,0,-1}}; 				
	protected double[][] mSensitivityMatrixGyroscope = {{2.73,0,0},{0,2.73,0},{0,0,2.73}}; 		
	protected double[][] mOffsetVectorGyroscope = {{1843},{1843},{1843}};						

	//Default values Shimmer2
	protected static double[][] AlignmentMatrixGyroShimmer2 = {{0,-1,0},{-1,0,0},{0,0,-1}}; 				
	protected static double[][] SensitivityMatrixGyroShimmer2 = {{2.73,0,0},{0,2.73,0},{0,0,2.73}}; 		
	protected static double[][] OffsetVectorGyroShimmer2 = {{1843},{1843},{1843}};
	//Shimmer3
	protected static double[][] SensitivityMatrixGyro250dpsShimmer3 = {{131,0,0},{0,131,0},{0,0,131}};
	protected static double[][] SensitivityMatrixGyro500dpsShimmer3 = {{65.5,0,0},{0,65.5,0},{0,0,65.5}};
	protected static double[][] SensitivityMatrixGyro1000dpsShimmer3 = {{32.8,0,0},{0,32.8,0},{0,0,32.8}};
	protected static double[][] SensitivityMatrixGyro2000dpsShimmer3 = {{16.4,0,0},{0,16.4,0},{0,0,16.4}};
	protected static double[][] AlignmentMatrixGyroShimmer3 = {{0,-1,0},{-1,0,0},{0,0,-1}}; 				
	protected static double[][] OffsetVectorGyroShimmer3 = {{0},{0},{0}};		

	protected int mCurrentLEDStatus=0;
	protected boolean mDefaultCalibrationParametersMag = true;
	protected double[][] mAlignmentMatrixMagnetometer = {{1,0,0},{0,1,0},{0,0,-1}}; 				
	protected double[][] mSensitivityMatrixMagnetometer = {{580,0,0},{0,580,0},{0,0,580}}; 		
	protected double[][] mOffsetVectorMagnetometer = {{0},{0},{0}};								

	//Default values Shimmer2 and Shimmer3
	protected static double[][] AlignmentMatrixMagShimmer2 = {{1,0,0},{0,1,0},{0,0,-1}};
	protected static double[][] SensitivityMatrixMagShimmer2 = {{580,0,0},{0,580,0},{0,0,580}}; 		
	protected static double[][] OffsetVectorMagShimmer2 = {{0},{0},{0}};				
	//Shimmer3
	protected static double[][] AlignmentMatrixMagShimmer3 = {{-1,0,0},{0,1,0},{0,0,-1}}; 				
	protected static double[][] SensitivityMatrixMagShimmer3 = {{1100,0,0},{0,1100,0},{0,0,980}}; 		
	protected static double[][] OffsetVectorMagShimmer3 = {{0},{0},{0}};		

	
	protected double[][] AlignmentMatrixMPLAccel = {{-1,0,0},{0,1,0},{0,0,-1}}; 			
	protected double[][] SensitivityMatrixMPLAccel = {{1631,0,0},{0,1631,0},{0,0,1631}}; 	
	protected double[][] OffsetVectorMPLAccel = {{0},{0},{0}};
	
	protected double[][] AlignmentMatrixMPLMag = {{-1,0,0},{0,1,0},{0,0,-1}}; 			
	protected double[][] SensitivityMatrixMPLMag = {{1631,0,0},{0,1631,0},{0,0,1631}}; 	
	protected double[][] OffsetVectorMPLMag = {{0},{0},{0}};
	
	protected double[][] AlignmentMatrixMPLGyro = {{-1,0,0},{0,1,0},{0,0,-1}}; 			
	protected double[][] SensitivityMatrixMPLGyro = {{1631,0,0},{0,1631,0},{0,0,1631}}; 	
	protected double[][] OffsetVectorMPLGyro = {{0},{0},{0}};
	

	protected static double[][] SensitivityMatrixMag1p3GaShimmer3 = {{1100,0,0},{0,1100,0},{0,0,980}};
	protected static double[][] SensitivityMatrixMag1p9GaShimmer3 = {{855,0,0},{0,855,0},{0,0,760}};
	protected static double[][] SensitivityMatrixMag2p5GaShimmer3 = {{670,0,0},{0,670,0},{0,0,600}};
	protected static double[][] SensitivityMatrixMag4GaShimmer3 = {{450,0,0},{0,450,0},{0,0,400}};
	protected static double[][] SensitivityMatrixMag4p7GaShimmer3 = {{400,0,0},{0,400,0},{0,0,355}};
	protected static double[][] SensitivityMatrixMag5p6GaShimmer3 = {{330,0,0},{0,330,0},{0,0,295}};
	protected static double[][] SensitivityMatrixMag8p1GaShimmer3 = {{230,0,0},{0,230,0},{0,0,205}};

	protected static double[][] SensitivityMatrixMag0p8GaShimmer2 = {{1370,0,0},{0,1370,0},{0,0,1370}};
	protected static double[][] SensitivityMatrixMag1p3GaShimmer2 = {{1090,0,0},{0,1090,0},{0,0,1090}};
	protected static double[][] SensitivityMatrixMag1p9GaShimmer2 = {{820,0,0},{0,820,0},{0,0,820}};
	protected static double[][] SensitivityMatrixMag2p5GaShimmer2 = {{660,0,0},{0,660,0},{0,0,660}};
	protected static double[][] SensitivityMatrixMag4p0GaShimmer2 = {{440,0,0},{0,440,0},{0,0,440}};
	protected static double[][] SensitivityMatrixMag4p7GaShimmer2 = {{390,0,0},{0,390,0},{0,0,390}};
	protected static double[][] SensitivityMatrixMag5p6GaShimmer2 = {{330,0,0},{0,330,0},{0,0,330}};
	protected static double[][] SensitivityMatrixMag8p1GaShimmer2 = {{230,0,0},{0,230,0},{0,0,230}};


	protected double AC1 = 408;
	protected double AC2 = -72;
	protected double AC3 = -14383;
	protected double AC4 = 332741;
	protected double AC5 = 32757;
	protected double AC6 = 23153;
	protected double B1 = 6190;
	protected double B2 = 4;
	protected double MB = -32767;
	protected double MC = -8711;
	protected double MD = 2868;

	protected boolean mLowPowerMag = false;
	protected boolean mLowPowerAccelWR = false;
	protected boolean mLowPowerGyro = false;
	
	protected long mPacketLossCount=0;
	protected double mPacketReceptionRate=100;
	protected double mLastReceivedCalibratedTimeStamp=-1; 
	protected boolean mFirstTimeCalTime=true;
	protected double mCalTimeStart;
	private double mLastKnownHeartRate=0;
	protected DescriptiveStatistics mVSenseBattMA= new DescriptiveStatistics(1024);
	Quat4d mQ = new Quat4d();
	GradDes3DOrientation mOrientationAlgo;
	protected boolean mOrientationEnabled = false;
	protected boolean mEnableOntheFlyGyroOVCal = false;

	protected double mGyroOVCalThreshold = 1.2;
	DescriptiveStatistics mGyroX;
	DescriptiveStatistics mGyroY;
	DescriptiveStatistics mGyroZ;
	DescriptiveStatistics mGyroXRaw;
	DescriptiveStatistics mGyroYRaw;
	DescriptiveStatistics mGyroZRaw;
	protected boolean mEnableCalibration = true;
	protected byte[] mInquiryResponseBytes;
	protected boolean mStreaming =false;											// This is used to monitor whether the device is in streaming mode
	//all raw params should start with a 1 byte identifier in position [0]
	protected byte[] mAccelCalRawParams = new byte[22];
	protected byte[] mDigiAccelCalRawParams  = new byte[22];
	protected byte[] mGyroCalRawParams  = new byte[22];
	protected byte[] mMagCalRawParams  = new byte[22];
	protected byte[] mPressureRawParams  = new byte[23];
	protected byte[] mEMGCalRawParams  = new byte[13];
	protected byte[] mECGCalRawParams = new byte[13];
	protected byte[] mPressureCalRawParams = new byte[23];

	//EXG
	protected byte[] mEXG1RegisterArray = new byte[10];
	protected byte[] mEXG2RegisterArray = new byte[10];
	protected int mEXG1RateSetting; //setting not value
	protected int mEXG1CH1GainSetting; // this is the setting not to be confused with the actual value
	protected int mEXG1CH1GainValue; // this is the value
	protected int mEXG1CH2GainSetting; // this is the setting not to be confused with the actual value
	protected int mEXG1CH2GainValue; // this is the value
	protected int mEXG2RateSetting; //setting not value
	protected int mEXG2CH1GainSetting; // this is the setting not to be confused with the actual value
	protected int mEXG2CH1GainValue; // this is the value
	protected int mEXG2CH2GainSetting; // this is the setting not to be confused with the actual value
	protected int mEXG2CH2GainValue; // this is the value
	protected static final int EXG_CHIP1 = 0;
	protected static final int EXG_CHIP2 = 1;
	//EXG ADVANCED
	protected int mEXGReferenceElectrode=-1;
	protected int mLeadOffDetectionMode;
	protected int mEXG1LeadOffCurrentMode;
	protected int mEXG2LeadOffCurrentMode;
	protected int mEXG1Comparators;
	protected int mEXG2Comparators;
	protected int mEXGRLDSense;
	protected int mEXG1LeadOffSenseSelection;
	protected int mEXG2LeadOffSenseSelection;
	protected int mEXGLeadOffDetectionCurrent;
	protected int mEXGLeadOffComparatorTreshold;	
	protected int mEXG2RespirationDetectState;
	protected int mEXG2RespirationDetectFreq;
	protected int mEXG2RespirationDetectPhase;
	
	
	protected int mTimeStampPacketByteSize = 2;
	protected int mTimeStampPacketRawMaxValue = 65536;// 16777216 or 65536 
	
	//This features are only used in LogAndStream FW 
	protected String mDirectoryName;
	protected int mDirectoryNameLength;
	protected boolean mSensingStatus;
	protected boolean mDockedStatus;
	protected boolean mIsSDLogging;
    protected boolean mIsStreaming;
	private List<String[]> mExtraSignalProperties = null;
	
	//Testing for GQ
	protected int mGqPacketNumHeaderBytes = 0;
	protected int mSamplingDividerVBatt = 0;
	protected int mSamplingDividerGsr = 0;
	protected int mSamplingDividerPpg = 0;
	protected int mSamplingDividerLsm303dlhcAccel = 0;

	protected ObjectCluster buildMsg(byte[] newPacket, int fwIdentifier, int timeSync) {
		ObjectCluster objectCluster = new ObjectCluster();
		
		objectCluster.mMyName = mMyName;
		objectCluster.mBluetoothAddress = mMyBluetoothAddress;
		objectCluster.mRawData = newPacket;
		objectCluster.mSystemTimeStamp=ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
		
		double [] calibratedData;
		double [] uncalibratedData;
		String [] uncalibratedDataUnits;
		String [] calibratedDataUnits;
		String [] sensorNames;
		
		
		
		if (fwIdentifier == FW_TYPE_BT){
			objectCluster.mSystemTimeStamp=ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
			calibratedData=new double[mNChannels + 1]; //plus 1 because of the time stamp
			uncalibratedData=new double[mNChannels + 1]; //plus 1 because of the time stamp	
			uncalibratedDataUnits = new String[mNChannels + 1];
			calibratedDataUnits = new String[mNChannels + 1];
			sensorNames = new String[mNChannels + 1];
		} else {
			if (mRTCOffset == 0){
				calibratedData=new double[mNChannels]; //sd log time stamp already included in mnChannels
				uncalibratedData=new double[mNChannels];
				uncalibratedDataUnits = new String[mNChannels];
				calibratedDataUnits = new String[mNChannels];
				sensorNames = new String[mNChannels];
			} else {
				calibratedData=new double[mNChannels+1]; //for RTC
				uncalibratedData=new double[mNChannels+1];
				uncalibratedDataUnits = new String[mNChannels+1];
				calibratedDataUnits = new String[mNChannels+1];
				sensorNames = new String[mNChannels+1];
			}
		}
		
		System.arraycopy(mSignalNameArray, 0, sensorNames, 0, sensorNames.length);
		
		//PARSE DATA
		long[] newPacketInt=parsedData(newPacket,mSignalDataTypeArray);
		
		double[] tempData=new double[3];
		Vector3d accelerometer = new Vector3d();
		Vector3d magnetometer = new Vector3d();
		Vector3d gyroscope = new Vector3d();
		
		
		if (mHardwareVersion==HW_ID.SHIMMER_SR30 || mHardwareVersion==HW_ID.SHIMMER_3){
			
			int iTimeStamp=getSignalIndex("TimeStamp"); //find index
			if(mFirstTime && fwIdentifier == FW_TYPE_SD){
				//this is to make sure the Raw starts from zero
				mFirstRawTS = (double)newPacketInt[iTimeStamp];
				mFirstTime = false;
			}
			double calibratedTS = calibrateTimeStamp((double)newPacketInt[iTimeStamp]);

			//TIMESTAMP
			if (fwIdentifier == FW_TYPE_SD){
				// RTC timestamp uncal. (shimmer timestamp + RTC offset from header); unit = ticks
				double unwrappedrawtimestamp = calibratedTS*32768/1000;
				unwrappedrawtimestamp = unwrappedrawtimestamp - mFirstRawTS; //deduct this so it will start from 0
				long sdlograwtimestamp = (long)mInitialTimeStamp + (long)unwrappedrawtimestamp;
				objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("RAW",CLOCK_UNIT,(double)sdlograwtimestamp));
				uncalibratedData[iTimeStamp] = (double)sdlograwtimestamp;
				uncalibratedDataUnits[iTimeStamp] = CLOCK_UNIT;

				if (mEnableCalibration){
					double sdlogcaltimestamp = (double)sdlograwtimestamp/32768*1000;
					objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("CAL","mSecs",sdlogcaltimestamp));
					calibratedData[iTimeStamp] = sdlogcaltimestamp;
					calibratedDataUnits[iTimeStamp] = "mSecs";
				}
			} else if (fwIdentifier == FW_TYPE_BT){
				objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iTimeStamp]));
				uncalibratedData[iTimeStamp] = (double)newPacketInt[iTimeStamp];
				uncalibratedDataUnits[iTimeStamp] = NO_UNIT;
				if (mEnableCalibration){
					objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("CAL","mSecs",calibratedTS));
					calibratedData[iTimeStamp] = calibratedTS;
					calibratedDataUnits[iTimeStamp] = "mSecs";
				}
			}

			//RAW RTC
			if ((fwIdentifier == FW_TYPE_SD) && mRTCOffset!=0)
			{
				double unwrappedrawtimestamp = calibratedTS*32768/1000;
				unwrappedrawtimestamp = unwrappedrawtimestamp - mFirstRawTS; //deduct this so it will start from 0
				long rtctimestamp = (long)mInitialTimeStamp + (long)unwrappedrawtimestamp + mRTCOffset;
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.REAL_TIME_CLOCK,new FormatCluster("RAW",CLOCK_UNIT,(double)rtctimestamp));
				uncalibratedData[sensorNames.length-1] = (double)rtctimestamp;
				uncalibratedDataUnits[sensorNames.length-1] = CLOCK_UNIT;
				sensorNames[sensorNames.length-1]= Shimmer3.ObjectClusterSensorName.REAL_TIME_CLOCK;
				if (mEnableCalibration){
					double rtctimestampcal = ((double)mInitialTimeStamp/32768.0*1000.0) + calibratedTS + ((double)mRTCOffset/32768.0*1000.0) - (mFirstRawTS/32768.0*1000.0);
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.REAL_TIME_CLOCK,new FormatCluster("CAL","mSecs",rtctimestampcal));
					calibratedData[sensorNames.length-1] = rtctimestampcal;
					calibratedDataUnits[sensorNames.length-1] = "mSecs";
				}
			}

			//OFFSET
			if(timeSync==1 && (fwIdentifier == FW_TYPE_SD)){
				int iOffset=getSignalIndex("Offset"); //find index
				double offsetValue = Double.NaN;
				if (OFFSET_LENGTH==9){
					if(newPacketInt[iOffset] == 1152921504606846975L){
						offsetValue=Double.NaN;
					} else {
						offsetValue=(double)newPacketInt[iOffset];
					}	
				}
				else{
					if(newPacketInt[iOffset] == 4294967295L){ //this is 4 bytes
						offsetValue=Double.NaN;
					} else {
						offsetValue=(double)newPacketInt[iOffset];
					}
				}

				objectCluster.mPropertyCluster.put("Offset",new FormatCluster("RAW",NO_UNIT,offsetValue));
				uncalibratedData[iOffset] = offsetValue;
				calibratedData[iOffset] = Double.NaN;
				uncalibratedDataUnits[iOffset] = NO_UNIT;
				calibratedDataUnits[iOffset] = NO_UNIT;
			} 


			objectCluster = callAdditionalServices(objectCluster);


			//first get raw and calibrated data, this is data derived from the Shimmer device and involves no involvement from the API


			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.ACCEL_LN) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.ACCEL_LN) > 0)
					){
				int iAccelX=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_LN_X); //find index
				int iAccelY=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y); //find index
				int iAccelZ=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z); //find index
				tempData[0]=(double)newPacketInt[iAccelX];
				tempData[1]=(double)newPacketInt[iAccelY];
				tempData[2]=(double)newPacketInt[iAccelZ];

				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));
				uncalibratedData[iAccelX]=(double)newPacketInt[iAccelX];
				uncalibratedData[iAccelY]=(double)newPacketInt[iAccelY];
				uncalibratedData[iAccelZ]=(double)newPacketInt[iAccelZ];
				uncalibratedDataUnits[iAccelX]=NO_UNIT;
				uncalibratedDataUnits[iAccelY]=NO_UNIT;
				uncalibratedDataUnits[iAccelZ]=NO_UNIT;


				if (mEnableCalibration){
					double[] accelCalibratedData;
					accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixAnalogAccel, mSensitivityMatrixAnalogAccel, mOffsetVectorAnalogAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];

					if (mDefaultCalibrationParametersAccel == true) {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,accelCalibratedData[2]));
						calibratedDataUnits[iAccelX]=ACCEL_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iAccelY]=ACCEL_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iAccelZ]=ACCEL_DEFAULT_CAL_UNIT;
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];

					} else {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,new FormatCluster("CAL",ACCEL_CAL_UNIT,accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,new FormatCluster("CAL",ACCEL_CAL_UNIT,accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,new FormatCluster("CAL",ACCEL_CAL_UNIT,accelCalibratedData[2]));
						calibratedDataUnits[iAccelX] = ACCEL_CAL_UNIT;
						calibratedDataUnits[iAccelY] = ACCEL_CAL_UNIT;
						calibratedDataUnits[iAccelZ] = ACCEL_CAL_UNIT;
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];

					}
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.ACCEL_WR) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.ACCEL_WR) > 0)
					){
				int iAccelX=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X); //find index
				int iAccelY=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y); //find index
				int iAccelZ=getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z); //find index
				//check range

				tempData[0]=(double)newPacketInt[iAccelX];
				tempData[1]=(double)newPacketInt[iAccelY];
				tempData[2]=(double)newPacketInt[iAccelZ];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));
				uncalibratedData[iAccelX]=(double)newPacketInt[iAccelX];
				uncalibratedData[iAccelY]=(double)newPacketInt[iAccelY];
				uncalibratedData[iAccelZ]=(double)newPacketInt[iAccelZ];
				uncalibratedDataUnits[iAccelX]=NO_UNIT;
				uncalibratedDataUnits[iAccelY]=NO_UNIT;
				uncalibratedDataUnits[iAccelZ]=NO_UNIT;

				if (mEnableCalibration){
					double[] accelCalibratedData;
					accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixWRAccel, mSensitivityMatrixWRAccel, mOffsetVectorWRAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];
					if (mDefaultCalibrationParametersDigitalAccel == true) {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,calibratedData[iAccelX]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,calibratedData[iAccelY]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,new FormatCluster("CAL",ACCEL_DEFAULT_CAL_UNIT,calibratedData[iAccelZ]));
						calibratedDataUnits[iAccelX]=ACCEL_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iAccelY]=ACCEL_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iAccelZ]=ACCEL_DEFAULT_CAL_UNIT;
						if (((mEnabledSensors & SENSOR_ACCEL) == 0)){
							accelerometer.x=accelCalibratedData[0]; //this is used to calculate quaternions // skip if Low noise is enabled
							accelerometer.y=accelCalibratedData[1];
							accelerometer.z=accelCalibratedData[2];
						}
					} else {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,new FormatCluster("CAL",ACCEL_CAL_UNIT,calibratedData[iAccelX]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,new FormatCluster("CAL",ACCEL_CAL_UNIT,calibratedData[iAccelY]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,new FormatCluster("CAL",ACCEL_CAL_UNIT,calibratedData[iAccelZ]));
						calibratedDataUnits[iAccelX]=ACCEL_CAL_UNIT;
						calibratedDataUnits[iAccelY]=ACCEL_CAL_UNIT;
						calibratedDataUnits[iAccelZ]=ACCEL_CAL_UNIT;
						if (((mEnabledSensors & SENSOR_ACCEL) == 0)){
							accelerometer.x=accelCalibratedData[0];
							accelerometer.y=accelCalibratedData[1];
							accelerometer.z=accelCalibratedData[2];
						}

					}	
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.GYRO) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.GYRO) > 0)
					) {
				int iGyroX=getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_X);
				int iGyroY=getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_Y);
				int iGyroZ=getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_Z);
				tempData[0]=(double)newPacketInt[iGyroX];
				tempData[1]=(double)newPacketInt[iGyroY];
				tempData[2]=(double)newPacketInt[iGyroZ];


				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroX]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroY]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroZ]));
				uncalibratedData[iGyroX]=(double)newPacketInt[iGyroX];
				uncalibratedData[iGyroY]=(double)newPacketInt[iGyroY];
				uncalibratedData[iGyroZ]=(double)newPacketInt[iGyroZ];
				uncalibratedDataUnits[iGyroX]=NO_UNIT;
				uncalibratedDataUnits[iGyroY]=NO_UNIT;
				uncalibratedDataUnits[iGyroZ]=NO_UNIT;
				if (mEnableCalibration){
					double[] gyroCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixGyroscope, mSensitivityMatrixGyroscope, mOffsetVectorGyroscope);
					calibratedData[iGyroX]=gyroCalibratedData[0];
					calibratedData[iGyroY]=gyroCalibratedData[1];
					calibratedData[iGyroZ]=gyroCalibratedData[2];
					if (mDefaultCalibrationParametersGyro == true) {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_X,new FormatCluster("CAL",GYRO_DEFAULT_CAL_UNIT,gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Y,new FormatCluster("CAL",GYRO_DEFAULT_CAL_UNIT,gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Z,new FormatCluster("CAL",GYRO_DEFAULT_CAL_UNIT,gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
						calibratedDataUnits[iGyroX]=GYRO_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iGyroY]=GYRO_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iGyroZ]=GYRO_DEFAULT_CAL_UNIT;
					} else {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_X,new FormatCluster("CAL",GYRO_CAL_UNIT,gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Y,new FormatCluster("CAL",GYRO_CAL_UNIT,gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GYRO_Z,new FormatCluster("CAL",GYRO_CAL_UNIT,gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
						calibratedDataUnits[iGyroX]=GYRO_CAL_UNIT;
						calibratedDataUnits[iGyroY]=GYRO_CAL_UNIT;
						calibratedDataUnits[iGyroZ]=GYRO_CAL_UNIT;

					} 
					if (mEnableOntheFlyGyroOVCal){
						mGyroX.addValue(gyroCalibratedData[0]);
						mGyroY.addValue(gyroCalibratedData[1]);
						mGyroZ.addValue(gyroCalibratedData[2]);
						mGyroXRaw.addValue((double)newPacketInt[iGyroX]);
						mGyroYRaw.addValue((double)newPacketInt[iGyroY]);
						mGyroZRaw.addValue((double)newPacketInt[iGyroZ]);
						if (mGyroX.getStandardDeviation()<mGyroOVCalThreshold && mGyroY.getStandardDeviation()<mGyroOVCalThreshold && mGyroZ.getStandardDeviation()<mGyroOVCalThreshold){
							mOffsetVectorGyroscope[0][0]=mGyroXRaw.getMean();
							mOffsetVectorGyroscope[1][0]=mGyroYRaw.getMean();
							mOffsetVectorGyroscope[2][0]=mGyroZRaw.getMean();
						}
					}
				}

			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.MAG) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.MAG) > 0)
					) {
				int iMagX=getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_X);
				int iMagY=getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_Y);
				int iMagZ=getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_Z);
				tempData[0]=(double)newPacketInt[iMagX];
				tempData[1]=(double)newPacketInt[iMagY];
				tempData[2]=(double)newPacketInt[iMagZ];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagX]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagY]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagZ]));
				uncalibratedData[iMagX]=(double)newPacketInt[iMagX];
				uncalibratedData[iMagY]=(double)newPacketInt[iMagY];
				uncalibratedData[iMagZ]=(double)newPacketInt[iMagZ];
				uncalibratedDataUnits[iMagX]=NO_UNIT;
				uncalibratedDataUnits[iMagY]=NO_UNIT;
				uncalibratedDataUnits[iMagZ]=NO_UNIT;
				if (mEnableCalibration){
					double[] magCalibratedData;
					magCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixMagnetometer, mSensitivityMatrixMagnetometer, mOffsetVectorMagnetometer);
					calibratedData[iMagX]=magCalibratedData[0];
					calibratedData[iMagY]=magCalibratedData[1];
					calibratedData[iMagZ]=magCalibratedData[2];
					if (mDefaultCalibrationParametersMag == true) {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_X,new FormatCluster("CAL",MAG_DEFAULT_CAL_UNIT,magCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Y,new FormatCluster("CAL",MAG_DEFAULT_CAL_UNIT,magCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Z,new FormatCluster("CAL",MAG_DEFAULT_CAL_UNIT,magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
						calibratedDataUnits[iMagX]=MAG_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iMagY]=MAG_DEFAULT_CAL_UNIT;
						calibratedDataUnits[iMagZ]=MAG_DEFAULT_CAL_UNIT;
					} else {
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_X,new FormatCluster("CAL",MAG_CAL_UNIT,magCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Y,new FormatCluster("CAL",MAG_CAL_UNIT,magCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.MAG_Z,new FormatCluster("CAL",MAG_CAL_UNIT,magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
						calibratedDataUnits[iMagX]=MAG_CAL_UNIT;
						calibratedDataUnits[iMagY]=MAG_CAL_UNIT;
						calibratedDataUnits[iMagZ]=MAG_CAL_UNIT;
					}
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.BATTERY) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.BATTERY) > 0)
					) {
				int iBatt = getSignalIndex(Shimmer3.ObjectClusterSensorName.BATTERY);
				tempData[0] = (double)newPacketInt[iBatt];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.BATTERY,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBatt]));
				uncalibratedData[iBatt]=(double)newPacketInt[iBatt];
				uncalibratedDataUnits[iBatt]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iBatt]=calibrateU12AdcValue(tempData[0],0,3,1)*2;
					calibratedDataUnits[iBatt] = ADC_CAL_UNIT;
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.BATTERY,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iBatt]));
					mVSenseBattMA.addValue(calibratedData[iBatt]);
					checkBattery();
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXT_EXP_A7) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXT_EXP_A7) > 0)
					) {
				int iA7 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXT_EXP_A7);
				tempData[0] = (double)newPacketInt[iA7];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A7,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));
				uncalibratedData[iA7]=(double)newPacketInt[iA7];
				uncalibratedDataUnits[iA7]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1);
					calibratedDataUnits[iA7] = ADC_CAL_UNIT;
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A7,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA7]));
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXT_EXP_A6) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXT_EXP_A6) > 0)
					) {
				int iA6 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXT_EXP_A6);
				tempData[0] = (double)newPacketInt[iA6];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A6,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA6]));
				uncalibratedData[iA6]=(double)newPacketInt[iA6];
				uncalibratedDataUnits[iA6]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iA6]=calibrateU12AdcValue(tempData[0],0,3,1);
					calibratedDataUnits[iA6] = ADC_CAL_UNIT;
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A6,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA6]));
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXT_EXP_A15) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXT_EXP_A15) > 0)
					) {
				int iA15 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXT_EXP_A15);
				tempData[0] = (double)newPacketInt[iA15];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA15]));
				uncalibratedData[iA15]=(double)newPacketInt[iA15];
				uncalibratedDataUnits[iA15]=NO_UNIT;

				if (mEnableCalibration){
					calibratedData[iA15]=calibrateU12AdcValue(tempData[0],0,3,1);
					calibratedDataUnits[iA15] = ADC_CAL_UNIT;
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA15]));
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.INT_EXP_A1) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.INT_EXP_A1) > 0)
					) {
					int iA1 = getSignalIndex(Shimmer3.ObjectClusterSensorName.INT_EXP_A1);
					tempData[0] = (double)newPacketInt[iA1];
					String sensorName = Shimmer3.ObjectClusterSensorName.INT_EXP_A1;
					
					//to Support derived sensor renaming
					if (fwIdentifier == FW_TYPE_SD){
						//change name based on derived sensor value
						if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG2_1_14)>0){
							sensorName = Shimmer3.ObjectClusterSensorName.PPG2_A1;
						} else if ((mDerivedSensors & SDLogHeaderDerivedSensors.RES_AMP)>0){
							sensorName = Shimmer3.ObjectClusterSensorName.RESISTANCE_AMP;
						}
					}
					sensorNames[iA1]=sensorName;
					objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA1]));
					uncalibratedData[iA1]=(double)newPacketInt[iA1];
					uncalibratedDataUnits[iA1]=NO_UNIT;
					if (mEnableCalibration){
						calibratedData[iA1]=calibrateU12AdcValue(tempData[0],0,3,1);
						calibratedDataUnits[iA1] = ADC_CAL_UNIT;
						objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA1]));
					}
				
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.INT_EXP_A12) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.INT_EXP_A12) > 0)
					) {
				int iA12 = getSignalIndex(Shimmer3.ObjectClusterSensorName.INT_EXP_A12);
				tempData[0] = (double)newPacketInt[iA12];
				String sensorName = Shimmer3.ObjectClusterSensorName.INT_EXP_A12;
				//to Support derived sensor renaming
				if (fwIdentifier == FW_TYPE_SD){
					//change name based on derived sensor value
					if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG_12_13)>0){
						sensorName = Shimmer3.ObjectClusterSensorName.PPG_A12;
					} else if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG1_12_13)>0){
						sensorName = Shimmer3.ObjectClusterSensorName.PPG1_A12;
					}
				}
				
				sensorNames[iA12]=sensorName;
				objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA12]));
				uncalibratedData[iA12]=(double)newPacketInt[iA12];
				uncalibratedDataUnits[iA12]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iA12]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA12]));
					calibratedDataUnits[iA12] = ADC_CAL_UNIT;

				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.INT_EXP_A13) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.INT_EXP_A13) > 0)
					) {
				int iA13 = getSignalIndex(Shimmer3.ObjectClusterSensorName.INT_EXP_A13);
				tempData[0] = (double)newPacketInt[iA13];
				String sensorName = Shimmer3.ObjectClusterSensorName.INT_EXP_A13;
				//to Support derived sensor renaming
				if (fwIdentifier == FW_TYPE_SD){
					//change name based on derived sensor value
					if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG_12_13)>0){
						sensorName = Shimmer3.ObjectClusterSensorName.PPG_A13;
					} else if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG1_12_13)>0){
						sensorName = Shimmer3.ObjectClusterSensorName.PPG1_A13;
					}
				}
				
				sensorNames[iA13]=sensorName;
				objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA13]));
				uncalibratedData[iA13]=(double)newPacketInt[iA13];
				uncalibratedDataUnits[iA13]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iA13]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA13]));
					calibratedDataUnits[iA13] = ADC_CAL_UNIT;
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.INT_EXP_A14) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.INT_EXP_A14) > 0)
					) {
				int iA14 = getSignalIndex(Shimmer3.ObjectClusterSensorName.INT_EXP_A14);
				tempData[0] = (double)newPacketInt[iA14];
				String sensorName = Shimmer3.ObjectClusterSensorName.INT_EXP_A14;
				//to Support derived sensor renaming
				if (fwIdentifier == FW_TYPE_SD){
					//change name based on derived sensor value
					if ((mDerivedSensors & SDLogHeaderDerivedSensors.PPG2_1_14)>0){
						sensorName = Shimmer3.ObjectClusterSensorName.PPG2_A14;
					}
				}
				sensorNames[iA14]=sensorName;
				objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA14]));
				uncalibratedData[iA14]=(double)newPacketInt[iA14];
				uncalibratedDataUnits[iA14]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iA14]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put(sensorName,new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iA14]));
					calibratedDataUnits[iA14] = ADC_CAL_UNIT;
				}
			}
			//((fwIdentifier == FW_IDEN_BTSTREAM) && (mEnabledSensors & BTStream.ACCEL_WR) > 0) 
			//|| ((fwIdentifier == FW_IDEN_SD) && (mEnabledSensors & SDLogHeader.ACCEL_WR) > 0)




			if (((fwIdentifier == FW_TYPE_BT)&&((mEnabledSensors & BTStream.ACCEL_LN) > 0 || (mEnabledSensors & BTStream.ACCEL_WR) > 0) && ((mEnabledSensors & BTStream.GYRO) > 0) && ((mEnabledSensors & BTStream.MAG) > 0) && mOrientationEnabled )
					||((fwIdentifier == FW_TYPE_SD)&&((mEnabledSensors & SDLogHeader.ACCEL_LN) > 0 || (mEnabledSensors & SDLogHeader.ACCEL_WR) > 0) && ((mEnabledSensors & SDLogHeader.GYRO) > 0) && ((mEnabledSensors & SDLogHeader.MAG) > 0) && mOrientationEnabled )){
				if (mEnableCalibration){
					if (mOrientationAlgo==null){
						mOrientationAlgo = new GradDes3DOrientation(0.4, (double)1/mShimmerSamplingRate, 1, 0, 0,0);
					}
					Quaternion q = mOrientationAlgo.update(accelerometer.x,accelerometer.y,accelerometer.z, gyroscope.x,gyroscope.y,gyroscope.z, magnetometer.x,magnetometer.y,magnetometer.z);					double theta, Rx, Ry, Rz, rho;
					rho = Math.acos(q.q1);
					theta = rho * 2;
					Rx = q.q2 / Math.sin(rho);
					Ry = q.q3 / Math.sin(rho);
					Rz = q.q4 / Math.sin(rho);
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_A,new FormatCluster("CAL","local",theta));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_X,new FormatCluster("CAL","local",Rx));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Y,new FormatCluster("CAL","local",Ry));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Z,new FormatCluster("CAL","local",Rz));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_W,new FormatCluster("CAL","local",q.q1));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_X,new FormatCluster("CAL","local",q.q2));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y,new FormatCluster("CAL","local",q.q3));
					objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z,new FormatCluster("CAL","local",q.q4));
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXG1_24BIT) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXG1_24BIT) > 0)
					){
				int iexg1ch1 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT);
				int iexg1ch2 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT);
				int iexg1sta = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_STATUS);
				double exg1ch1 = (double)newPacketInt[iexg1ch1];
				double exg1ch2 = (double)newPacketInt[iexg1ch2];
				double exg1sta = (double)newPacketInt[iexg1sta];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_STATUS,new FormatCluster("RAW",NO_UNIT,exg1sta));
				uncalibratedData[iexg1sta]=(double)newPacketInt[iexg1sta];
				uncalibratedData[iexg1ch1]=(double)newPacketInt[iexg1ch1];
				uncalibratedData[iexg1ch2]=(double)newPacketInt[iexg1ch2];
				uncalibratedDataUnits[iexg1sta]=NO_UNIT;
				uncalibratedDataUnits[iexg1ch1]=NO_UNIT;
				uncalibratedDataUnits[iexg1ch2]=NO_UNIT;
				if (mEnableCalibration){
					double calexg1ch1 = exg1ch1 *(((2.42*1000)/mEXG1CH1GainValue)/(Math.pow(2,23)-1));
					double calexg1ch2 = exg1ch2 *(((2.42*1000)/mEXG1CH2GainValue)/(Math.pow(2,23)-1));
					calibratedData[iexg1ch1]=calexg1ch1;
					calibratedData[iexg1ch2]=calexg1ch2;
					calibratedData[iexg1sta]=(double)newPacketInt[iexg1sta];
					calibratedDataUnits[iexg1sta]=NO_UNIT;
					calibratedDataUnits[iexg1ch1]=ADC_CAL_UNIT;
					calibratedDataUnits[iexg1ch2]=ADC_CAL_UNIT;
					if (isEXGUsingDefaultECGConfiguration()){
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					} else if (isEXGUsingDefaultEMGConfiguration()){
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					} else {
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					}
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXG2_24BIT) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXG2_24BIT) > 0)
					){
				int iexg2ch1 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT);
				int iexg2ch2 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT);
				int iexg2sta = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_STATUS);
				double exg2ch1 = (double)newPacketInt[iexg2ch1];
				double exg2ch2 = (double)newPacketInt[iexg2ch2];
				double exg2sta = (double)newPacketInt[iexg2sta];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_STATUS,new FormatCluster("RAW",NO_UNIT,exg2sta));
				uncalibratedData[iexg2sta]=(double)newPacketInt[iexg2sta];
				uncalibratedData[iexg2ch1]=(double)newPacketInt[iexg2ch1];
				uncalibratedData[iexg2ch2]=(double)newPacketInt[iexg2ch2];
				uncalibratedDataUnits[iexg2sta]=NO_UNIT;
				uncalibratedDataUnits[iexg2ch1]=NO_UNIT;
				uncalibratedDataUnits[iexg2ch2]=NO_UNIT;
				if (mEnableCalibration){
					double calexg2ch1 = exg2ch1 *(((2.42*1000)/mEXG2CH1GainValue)/(Math.pow(2,23)-1));
					double calexg2ch2 = exg2ch2 *(((2.42*1000)/mEXG2CH2GainValue)/(Math.pow(2,23)-1));
					calibratedData[iexg2ch1]=calexg2ch1;
					calibratedData[iexg2ch2]=calexg2ch2;
					calibratedData[iexg2sta]=(double)newPacketInt[iexg2sta];
					calibratedDataUnits[iexg2sta]=NO_UNIT;
					calibratedDataUnits[iexg2ch1]=ADC_CAL_UNIT;
					calibratedDataUnits[iexg2ch2]=ADC_CAL_UNIT;
					if (isEXGUsingDefaultECGConfiguration()){
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT,new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch2));
					}
					else if (isEXGUsingDefaultEMGConfiguration()){
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,0));
					} else {
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch2));	
					}
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXG1_16BIT) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXG1_16BIT) > 0)
					){
				int iexg1ch1 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT);
				int iexg1ch2 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT);
				int iexg1sta = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG1_STATUS);
				double exg1ch1 = (double)newPacketInt[iexg1ch1];
				double exg1ch2 = (double)newPacketInt[iexg1ch2];
				double exg1sta = (double)newPacketInt[iexg1sta];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_STATUS,new FormatCluster("RAW",NO_UNIT,exg1sta));
				uncalibratedData[iexg1sta]=(double)newPacketInt[iexg1sta];
				uncalibratedData[iexg1ch1]=(double)newPacketInt[iexg1ch1];
				uncalibratedData[iexg1ch2]=(double)newPacketInt[iexg1ch2];
				uncalibratedDataUnits[iexg1sta]=NO_UNIT;
				uncalibratedDataUnits[iexg1ch1]=NO_UNIT;
				uncalibratedDataUnits[iexg1ch2]=NO_UNIT;
				if (mEnableCalibration){
					double calexg1ch1 = exg1ch1 *(((2.42*1000)/(mEXG1CH1GainValue*2))/(Math.pow(2,15)-1));
					double calexg1ch2 = exg1ch2 *(((2.42*1000)/(mEXG1CH2GainValue*2))/(Math.pow(2,15)-1));
					calibratedData[iexg1ch1]=calexg1ch1;
					calibratedData[iexg1ch2]=calexg1ch2;
					calibratedData[iexg1sta]=(double)newPacketInt[iexg1sta];
					calibratedDataUnits[iexg1sta]=NO_UNIT;
					calibratedDataUnits[iexg1ch1]=ADC_CAL_UNIT;
					calibratedDataUnits[iexg1ch2]=ADC_CAL_UNIT;
					if (isEXGUsingDefaultECGConfiguration()){
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					} else if (isEXGUsingDefaultEMGConfiguration()){
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					} else {
						sensorNames[iexg1ch1]=Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT;
						sensorNames[iexg1ch2]=Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT,new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg1ch2));
					}
				}
			}
			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.EXG2_16BIT) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.EXG2_16BIT) > 0)
					){
				int iexg2ch1 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT);
				int iexg2ch2 = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT);
				int iexg2sta = getSignalIndex(Shimmer3.ObjectClusterSensorName.EXG2_STATUS);
				double exg2ch1 = (double)newPacketInt[iexg2ch1];
				double exg2ch2 = (double)newPacketInt[iexg2ch2];
				double exg2sta = (double)newPacketInt[iexg2sta];
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_STATUS,new FormatCluster("RAW",NO_UNIT,exg2sta));
				uncalibratedData[iexg2sta]=(double)newPacketInt[iexg2sta];
				uncalibratedData[iexg2ch1]=(double)newPacketInt[iexg2ch1];
				uncalibratedData[iexg2ch2]=(double)newPacketInt[iexg2ch2];
				uncalibratedDataUnits[iexg2sta]=NO_UNIT;
				uncalibratedDataUnits[iexg2ch1]=NO_UNIT;
				uncalibratedDataUnits[iexg2ch2]=NO_UNIT;
				if (mEnableCalibration){
					double calexg2ch1 = ((exg2ch1)) *(((2.42*1000)/(mEXG2CH1GainValue*2))/(Math.pow(2,15)-1));
					double calexg2ch2 = ((exg2ch2)) *(((2.42*1000)/(mEXG2CH2GainValue*2))/(Math.pow(2,15)-1));
					calibratedData[iexg2ch1]=calexg2ch1;
					calibratedData[iexg2ch2]=calexg2ch2;
					calibratedData[iexg2sta]=(double)newPacketInt[iexg2sta];
					calibratedDataUnits[iexg2sta]=NO_UNIT;
					calibratedDataUnits[iexg2ch1]=ADC_CAL_UNIT;
					calibratedDataUnits[iexg2ch2]=ADC_CAL_UNIT;
					if (isEXGUsingDefaultECGConfiguration()){
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT,new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch2));
					}
					else if (isEXGUsingDefaultEMGConfiguration()){
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,0));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,0));
					} else {
						sensorNames[iexg2ch1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT;
						sensorNames[iexg2ch2]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT;
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch1));
						objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,new FormatCluster("CAL",ADC_CAL_UNIT,calexg2ch1));	
					}
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.BMP180) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.BMP180) > 0)
					){
				int iUT = getSignalIndex(Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180);
				int iUP = getSignalIndex(Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180);
				double UT = (double)newPacketInt[iUT];
				double UP = (double)newPacketInt[iUP];
				UP=UP/Math.pow(2,8-mPressureResolution);
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180,new FormatCluster("RAW",NO_UNIT,UP));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180,new FormatCluster("RAW",NO_UNIT,UT));
				uncalibratedData[iUT]=(double)newPacketInt[iUT];
				uncalibratedData[iUP]=(double)newPacketInt[iUP];
				uncalibratedDataUnits[iUT]=NO_UNIT;
				uncalibratedDataUnits[iUP]=NO_UNIT;
				if (mEnableCalibration){
					double[] bmp180caldata= calibratePressureSensorData(UP,UT);
					objectCluster.mPropertyCluster.put("Pressure",new FormatCluster("CAL",PRESSURE_CAL_UNIT,bmp180caldata[0]/1000));
					objectCluster.mPropertyCluster.put("Temperature",new FormatCluster("CAL",TEMP_CAL_UNIT,bmp180caldata[1]));
					calibratedData[iUT]=bmp180caldata[1];
					calibratedData[iUP]=bmp180caldata[0]/1000;
					calibratedDataUnits[iUT]=TEMP_CAL_UNIT;
					calibratedDataUnits[iUP]=PRESSURE_CAL_UNIT;
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.BRIDGE_AMP) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.BRIDGE_AMP) > 0)
					) {
				int iBAHigh = getSignalIndex(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH);
				int iBALow = getSignalIndex(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW);
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBAHigh]));
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBALow]));
				uncalibratedData[iBAHigh]=(double)newPacketInt[iBAHigh];
				uncalibratedData[iBALow]=(double)newPacketInt[iBALow];
				uncalibratedDataUnits[iBAHigh]=NO_UNIT;
				uncalibratedDataUnits[iBALow]=NO_UNIT;
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iBAHigh];
					tempData[1] = (double)newPacketInt[iBALow];
					calibratedData[iBAHigh]=calibrateU12AdcValue(tempData[0],60,3,551);
					calibratedData[iBALow]=calibrateU12AdcValue(tempData[1],1950,3,183.7);
					calibratedDataUnits[iBAHigh]=ADC_CAL_UNIT;
					calibratedDataUnits[iBALow]=ADC_CAL_UNIT;
					objectCluster.mPropertyCluster.put("Bridge Amplifier High",new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iBAHigh]));
					objectCluster.mPropertyCluster.put("Bridge Amplifier Low",new FormatCluster("CAL",ADC_CAL_UNIT,calibratedData[iBALow]));	
				}
			}

			if (((fwIdentifier == FW_TYPE_BT) && (mEnabledSensors & BTStream.GSR) > 0) 
					|| ((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.GSR) > 0)
					) {
				int iGSR = getSignalIndex(Shimmer3.ObjectClusterSensorName.GSR);
				double p1=0,p2=0;//,p3=0,p4=0,p5=0;
				if (fwIdentifier == FW_TYPE_SD && mFirmwareVersionMajor ==0 && mFirmwareVersionMinor==9){
					tempData[0] = (double)newPacketInt[iGSR];
					int gsrUncalibratedData = ((int)tempData[0] & 4095); 
					int newGSRRange = -1; // initialized to -1 so it will only come into play if mGSRRange = 4  

					/*
					 * 	for i = 2:length(range)
    				 *		if range(i-1)~=range(i)
        			 *			if abs(gsrValue(i-1)-gsrValue(i))< 300
            		 *				range(i) = range(i-1);
        			 *			end
    				 *		end
					 *	end
					 */
					
					if (mGSRRange==4){
						newGSRRange=(49152 & (int)tempData[0])>>14;
						if (mPastGSRFirstTime){
							mPastGSRRange = newGSRRange;
							mPastGSRFirstTime = false;
						}
						if (newGSRRange != mPastGSRRange)
						{
							
							if (Math.abs(mPastGSRUncalibratedValue-gsrUncalibratedData)<300){
								newGSRRange = mPastGSRRange;
							} else {
								mPastGSRRange = newGSRRange;
							}
							mPastGSRUncalibratedValue = gsrUncalibratedData;
						}
						
					}
					if (mGSRRange==0 || newGSRRange==0) { //Note that from FW 1.0 onwards the MSB of the GSR data contains the range
						// the polynomial function used for calibration has been deprecated, it is replaced with a linear function
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0373;
							p2 = -24.9915;
						} else {
							//p1 = 0.0363;
							//p2 = -24.8617;
							p1 = 0.0373;
							p2 = -24.9915;
						}
					} else if (mGSRRange==1 || newGSRRange==1) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0054;
							p2 = -3.5194;
						} else {
							//p1 = 0.0051;
							//p2 = -3.8357;
							p1 = 0.0054;
							p2 = -3.5194;
						}
					} else if (mGSRRange==2 || newGSRRange==2) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0015;
							p2 = -1.0163;
						} else {
							//p1 = 0.0015;
							//p2 = -1.0067;
							p1 = 0.0015;
							p2 = -1.0163;
						}
					} else if (mGSRRange==3  || newGSRRange==3) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 4.5580e-04;
							p2 = -0.3014;
						} else {
							//p1 = 4.4513e-04;
							//p2 = -0.3193;
							p1 = 4.5580e-04;
							p2 = -0.3014;
						}
					}
				} else {
					tempData[0] = (double)newPacketInt[iGSR];
					int newGSRRange = -1; // initialized to -1 so it will only come into play if mGSRRange = 4  

					if (mGSRRange==4){
						newGSRRange=(49152 & (int)tempData[0])>>14; 
					}
					if (mGSRRange==0 || newGSRRange==0) { //Note that from FW 1.0 onwards the MSB of the GSR data contains the range
						// the polynomial function used for calibration has been deprecated, it is replaced with a linear function
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0373;
							p2 = -24.9915;

						} else { //Values have been reverted to 2r values
							//p1 = 0.0363;
							//p2 = -24.8617;
							p1 = 0.0373;
							p2 = -24.9915;
						}
					} else if (mGSRRange==1 || newGSRRange==1) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0054;
							p2 = -3.5194;
						} else {
							//p1 = 0.0051;
							//p2 = -3.8357;
							p1 = 0.0054;
							p2 = -3.5194;
						}
					} else if (mGSRRange==2 || newGSRRange==2) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 0.0015;
							p2 = -1.0163;
						} else {
							//p1 = 0.0015;
							//p2 = -1.0067;
							p1 = 0.0015;
							p2 = -1.0163;
						}
					} else if (mGSRRange==3  || newGSRRange==3) {
						if (mHardwareVersion!=HW_ID.SHIMMER_3){
							p1 = 4.5580e-04;
							p2 = -0.3014;
						} else {
							//p1 = 4.4513e-04;
							//p2 = -0.3193;
							p1 = 4.5580e-04;
							p2 = -0.3014;
						}
					}
				}
				objectCluster.mPropertyCluster.put(Shimmer3.ObjectClusterSensorName.GSR,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGSR]));
				uncalibratedData[iGSR]=(double)newPacketInt[iGSR];
				uncalibratedDataUnits[iGSR]=NO_UNIT;
				if (mEnableCalibration){
					calibratedData[iGSR] = calibrateGsrData(tempData[0],p1,p2);
					calibratedDataUnits[iGSR]=GSR_CAL_UNIT;
					objectCluster.mPropertyCluster.put("GSR",new FormatCluster("CAL",GSR_CAL_UNIT,calibratedData[iGSR]));
				}
			}

			if (((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.GYRO_MPU_MPL) > 0)){
				int iGyroX = getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_X);
				int iGyroY = getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Y);
				int iGyroZ = getSignalIndex(Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Z);
				calibratedData[iGyroX] = (double)newPacketInt[iGyroX]/Math.pow(2, 16);
				calibratedData[iGyroY] = (double)newPacketInt[iGyroY]/Math.pow(2, 16);
				calibratedData[iGyroZ] = (double)newPacketInt[iGyroZ]/Math.pow(2, 16);
				calibratedDataUnits[iGyroX] = GYRO_CAL_UNIT;
				calibratedDataUnits[iGyroY] = GYRO_CAL_UNIT;
				calibratedDataUnits[iGyroZ] = GYRO_CAL_UNIT;
				uncalibratedData[iGyroX] = (double)newPacketInt[iGyroX];
				uncalibratedData[iGyroY] = (double)newPacketInt[iGyroY];
				uncalibratedData[iGyroZ] = (double)newPacketInt[iGyroZ];
				uncalibratedDataUnits[iGyroX] = NO_UNIT;
				uncalibratedDataUnits[iGyroY] = NO_UNIT;
				uncalibratedDataUnits[iGyroZ] = NO_UNIT;
			}

			if (((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.ACCEL_MPU_MPL) > 0)){
				int iAccelX = getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_X);
				int iAccelY = getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Y);
				int iAccelZ = getSignalIndex(Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Z);
				calibratedData[iAccelX] = (double)newPacketInt[iAccelX]/Math.pow(2, 16);
				calibratedData[iAccelY] = (double)newPacketInt[iAccelY]/Math.pow(2, 16);
				calibratedData[iAccelZ] = (double)newPacketInt[iAccelZ]/Math.pow(2, 16);
				calibratedDataUnits[iAccelX] = ACCEL_CAL_UNIT;
				calibratedDataUnits[iAccelY] = ACCEL_CAL_UNIT;
				calibratedDataUnits[iAccelZ] = ACCEL_CAL_UNIT;
				uncalibratedData[iAccelX] = (double)newPacketInt[iAccelX];
				uncalibratedData[iAccelY] = (double)newPacketInt[iAccelX];
				uncalibratedData[iAccelZ] = (double)newPacketInt[iAccelX];
				uncalibratedDataUnits[iAccelX] = NO_UNIT;
				uncalibratedDataUnits[iAccelY] = NO_UNIT;
				uncalibratedDataUnits[iAccelZ] = NO_UNIT;
			}

			if (((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.MAG_MPU_MPL) > 0)){
				int iMagX = getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_X);
				int iMagY = getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Y);
				int iMagZ = getSignalIndex(Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Z);
				calibratedData[iMagX] = (double)newPacketInt[iMagX]/Math.pow(2, 16);
				calibratedData[iMagY] = (double)newPacketInt[iMagY]/Math.pow(2, 16);
				calibratedData[iMagZ] = (double)newPacketInt[iMagZ]/Math.pow(2, 16);
				calibratedDataUnits[iMagX] = MAG_CAL_UNIT;
				calibratedDataUnits[iMagY] = MAG_CAL_UNIT;
				calibratedDataUnits[iMagZ] = MAG_CAL_UNIT;
				uncalibratedData[iMagX] = (double)newPacketInt[iMagX];
				uncalibratedData[iMagY] = (double)newPacketInt[iMagY];
				uncalibratedData[iMagZ] = (double)newPacketInt[iMagZ];
				uncalibratedDataUnits[iMagX] = NO_UNIT;
				uncalibratedDataUnits[iMagY] = NO_UNIT;
				uncalibratedDataUnits[iMagZ] = NO_UNIT;
			}

			if (((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.MPL_QUAT_6DOF) > 0)){
				int iQW = getSignalIndex(Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_W);
				int iQX = getSignalIndex(Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_X);
				int iQY = getSignalIndex(Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Y);
				int iQZ = getSignalIndex(Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Z);
				calibratedData[iQW] = (double)newPacketInt[iQW]/Math.pow(2, 30);
				calibratedData[iQX] = (double)newPacketInt[iQX]/Math.pow(2, 30);
				calibratedData[iQY] = (double)newPacketInt[iQY]/Math.pow(2, 30);
				calibratedData[iQZ] = (double)newPacketInt[iQZ]/Math.pow(2, 30);
				calibratedDataUnits[iQW] = NO_UNIT;
				calibratedDataUnits[iQX] = NO_UNIT;
				calibratedDataUnits[iQY] = NO_UNIT;
				calibratedDataUnits[iQZ] = NO_UNIT;
				uncalibratedData[iQW] = (double)newPacketInt[iQW];
				uncalibratedData[iQX] = (double)newPacketInt[iQX];
				uncalibratedData[iQY] = (double)newPacketInt[iQY];
				uncalibratedData[iQZ] = (double)newPacketInt[iQZ];
				uncalibratedDataUnits[iQW] = NO_UNIT;
				uncalibratedDataUnits[iQX] = NO_UNIT;
				uncalibratedDataUnits[iQY] = NO_UNIT;
				calibratedDataUnits[iQZ] = NO_UNIT;
			}
			if (((fwIdentifier == FW_TYPE_SD) && (mEnabledSensors & SDLogHeader.MPL_TEMPERATURE) > 0)){
				int iT = getSignalIndex(Shimmer3.ObjectClusterSensorName.MPL_TEMPERATURE);
				calibratedData[iT] = (double)newPacketInt[iT]/Math.pow(2, 16);
				calibratedDataUnits[iT] = TEMP_CAL_UNIT;
				uncalibratedData[iT] = (double)newPacketInt[iT];
				uncalibratedDataUnits[iT] = NO_UNIT;
			}
			objectCluster.mCalData = calibratedData;
			objectCluster.mUncalData = uncalibratedData;
			objectCluster.mUnitCal = calibratedDataUnits;
			objectCluster.mUnitUncal = uncalibratedDataUnits;
			objectCluster.mSensorNames = sensorNames;

		} else if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){
			 //start of Shimmer2

			int iTimeStamp=getSignalIndex("TimeStamp"); //find index
			double calibratedTS = calibrateTimeStamp((double)newPacketInt[iTimeStamp]);

			//TIMESTAMP
			if (fwIdentifier == FW_TYPE_BT){
				objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iTimeStamp]));
				uncalibratedData[iTimeStamp] = (double)newPacketInt[iTimeStamp];
				uncalibratedDataUnits[iTimeStamp] = NO_UNIT;
				if (mEnableCalibration){
					objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("CAL","mSecs",calibratedTS));
					calibratedData[iTimeStamp] = calibratedTS;
					calibratedDataUnits[iTimeStamp] = "mSecs";
				}
			}
			
			if ((mEnabledSensors & SENSOR_ACCEL) > 0){
				int iAccelX=getSignalIndex(Shimmer2.ObjectClusterSensorName.ACCEL_X); //find index
				int iAccelY=getSignalIndex(Shimmer2.ObjectClusterSensorName.ACCEL_Y); //find index
				int iAccelZ=getSignalIndex(Shimmer2.ObjectClusterSensorName.ACCEL_Z); //find index
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iAccelX];
					tempData[1]=(double)newPacketInt[iAccelY];
					tempData[2]=(double)newPacketInt[iAccelZ];
					double[] accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixAnalogAccel, mSensitivityMatrixAnalogAccel, mOffsetVectorAnalogAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];
					if (mDefaultCalibrationParametersAccel == true) {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_X,new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Y,new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Z,new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_X,new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Y,new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ACCEL_Z,new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];
					}
				}

			}

			if ((mEnabledSensors & SENSOR_GYRO) > 0) {
				int iGyroX=getSignalIndex(Shimmer2.ObjectClusterSensorName.GYRO_X);
				int iGyroY=getSignalIndex(Shimmer2.ObjectClusterSensorName.GYRO_Y);
				int iGyroZ=getSignalIndex(Shimmer2.ObjectClusterSensorName.GYRO_Z);					
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroX]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroY]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iGyroX];
					tempData[1]=(double)newPacketInt[iGyroY];
					tempData[2]=(double)newPacketInt[iGyroZ];
					double[] gyroCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixGyroscope, mSensitivityMatrixGyroscope, mOffsetVectorGyroscope);
					calibratedData[iGyroX]=gyroCalibratedData[0];
					calibratedData[iGyroY]=gyroCalibratedData[1];
					calibratedData[iGyroZ]=gyroCalibratedData[2];
					if (mDefaultCalibrationParametersGyro == true) {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_X,new FormatCluster("CAL","deg/sec*",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Y,new FormatCluster("CAL","deg/sec*",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Z,new FormatCluster("CAL","deg/sec*",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_X,new FormatCluster("CAL","deg/sec",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Y,new FormatCluster("CAL","deg/sec",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GYRO_Z,new FormatCluster("CAL","deg/sec",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
						if (mEnableOntheFlyGyroOVCal){
							mGyroX.addValue(gyroCalibratedData[0]);
							mGyroY.addValue(gyroCalibratedData[1]);
							mGyroZ.addValue(gyroCalibratedData[2]);
							mGyroXRaw.addValue((double)newPacketInt[iGyroX]);
							mGyroYRaw.addValue((double)newPacketInt[iGyroY]);
							mGyroZRaw.addValue((double)newPacketInt[iGyroZ]);
							if (mGyroX.getStandardDeviation()<mGyroOVCalThreshold && mGyroY.getStandardDeviation()<mGyroOVCalThreshold && mGyroZ.getStandardDeviation()<mGyroOVCalThreshold){
								mOffsetVectorGyroscope[0][0]=mGyroXRaw.getMean();
								mOffsetVectorGyroscope[1][0]=mGyroYRaw.getMean();
								mOffsetVectorGyroscope[2][0]=mGyroZRaw.getMean();
							}
						}
					} 
				}
			}
			if ((mEnabledSensors & SENSOR_MAG) > 0) {
				int iMagX=getSignalIndex(Shimmer2.ObjectClusterSensorName.MAG_X);
				int iMagY=getSignalIndex(Shimmer2.ObjectClusterSensorName.MAG_Y);
				int iMagZ=getSignalIndex(Shimmer2.ObjectClusterSensorName.MAG_Z);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_X,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagX]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Y,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagY]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Z,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iMagX];
					tempData[1]=(double)newPacketInt[iMagY];
					tempData[2]=(double)newPacketInt[iMagZ];
					double[] magCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixMagnetometer, mSensitivityMatrixMagnetometer, mOffsetVectorMagnetometer);
					calibratedData[iMagX]=magCalibratedData[0];
					calibratedData[iMagY]=magCalibratedData[1];
					calibratedData[iMagZ]=magCalibratedData[2];
					if (mDefaultCalibrationParametersMag == true) {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_X,new FormatCluster("CAL","local*",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Y,new FormatCluster("CAL","local*",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Z,new FormatCluster("CAL","local*",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_X,new FormatCluster("CAL","local",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Y,new FormatCluster("CAL","local",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.MAG_Z,new FormatCluster("CAL","local",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					}
				}
			}


			if ((mEnabledSensors & SENSOR_ACCEL) > 0 && (mEnabledSensors & SENSOR_GYRO) > 0 && (mEnabledSensors & SENSOR_MAG) > 0 && mOrientationEnabled ){
				if (mEnableCalibration){
					if (mOrientationAlgo==null){
						mOrientationAlgo = new GradDes3DOrientation(0.4, (double)1/mShimmerSamplingRate, 1, 0, 0,0);
					}
					Quaternion q = mOrientationAlgo.update(accelerometer.x,accelerometer.y,accelerometer.z, gyroscope.x,gyroscope.y,gyroscope.z, magnetometer.x,magnetometer.y,magnetometer.z);
					double theta, Rx, Ry, Rz, rho;
					rho = Math.acos(q.q1);
					theta = rho * 2;
					Rx = q.q2 / Math.sin(rho);
					Ry = q.q3 / Math.sin(rho);
					Rz = q.q4 / Math.sin(rho);
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_A,new FormatCluster("CAL","local",theta));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_X,new FormatCluster("CAL","local",Rx));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Y,new FormatCluster("CAL","local",Ry));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Z,new FormatCluster("CAL","local",Rz));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_W,new FormatCluster("CAL","local",q.q1));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_X,new FormatCluster("CAL","local",q.q2));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y,new FormatCluster("CAL","local",q.q3));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z,new FormatCluster("CAL","local",q.q4));
				}
			}

			if ((mEnabledSensors & SENSOR_GSR) > 0) {
				int iGSR = getSignalIndex(Shimmer2.ObjectClusterSensorName.GSR);
				tempData[0] = (double)newPacketInt[iGSR];
				int newGSRRange = -1; // initialized to -1 so it will only come into play if mGSRRange = 4  

				double p1=0,p2=0;//,p3=0,p4=0,p5=0;
				if (mGSRRange==4){
					newGSRRange=(49152 & (int)tempData[0])>>14; 
				}
				if (mGSRRange==0 || newGSRRange==0) { //Note that from FW 1.0 onwards the MSB of the GSR data contains the range
					// the polynomial function used for calibration has been deprecated, it is replaced with a linear function
					
					p1 = 0.0373;
					p2 = -24.9915;
				} else if (mGSRRange==1 || newGSRRange==1) {
					p1 = 0.0054;
					p2 = -3.5194;
				} else if (mGSRRange==2 || newGSRRange==2) {
					p1 = 0.0015;
					p2 = -1.0163;
				} else if (mGSRRange==3  || newGSRRange==3) {
					p1 = 4.5580e-04;
					p2 = -0.3014;
				}
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GSR,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGSR]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iGSR];
					calibratedData[iGSR] = calibrateGsrData(tempData[0],p1,p2);
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.GSR,new FormatCluster("CAL","kOhms",calibratedData[iGSR]));
				}
			}
			if ((mEnabledSensors & SENSOR_ECG) > 0) {
				int iECGRALL = getSignalIndex(Shimmer2.ObjectClusterSensorName.ECG_RA_LL);
				int iECGLALL = getSignalIndex(Shimmer2.ObjectClusterSensorName.ECG_LA_LL);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_RA_LL,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iECGRALL]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_LA_LL,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iECGLALL]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iECGRALL];
					tempData[1] = (double)newPacketInt[iECGLALL];
					calibratedData[iECGRALL]=calibrateU12AdcValue(tempData[0],OffsetECGRALL,3,GainECGRALL);
					calibratedData[iECGLALL]=calibrateU12AdcValue(tempData[1],OffsetECGLALL,3,GainECGLALL);
					if (mDefaultCalibrationParametersECG == true) {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_RA_LL,new FormatCluster("CAL","mVolts*",calibratedData[iECGRALL]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_LA_LL,new FormatCluster("CAL","mVolts*",calibratedData[iECGLALL]));
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_RA_LL,new FormatCluster("CAL","mVolts",calibratedData[iECGRALL]));
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.ECG_LA_LL,new FormatCluster("CAL","mVolts",calibratedData[iECGLALL]));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_EMG) > 0) {
				int iEMG = getSignalIndex(Shimmer2.ObjectClusterSensorName.EMG);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EMG,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iEMG]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iEMG];
					calibratedData[iEMG]=calibrateU12AdcValue(tempData[0],OffsetEMG,3,GainEMG);
					if (mDefaultCalibrationParametersEMG == true){
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EMG,new FormatCluster("CAL","mVolts*",calibratedData[iEMG]));
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EMG,new FormatCluster("CAL","mVolts",calibratedData[iEMG]));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_BRIDGE_AMP) > 0) {
				int iBAHigh = getSignalIndex(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH);
				int iBALow = getSignalIndex(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBAHigh]));
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBALow]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iBAHigh];
					tempData[1] = (double)newPacketInt[iBALow];
					calibratedData[iBAHigh]=calibrateU12AdcValue(tempData[0],60,3,551);
					calibratedData[iBALow]=calibrateU12AdcValue(tempData[1],1950,3,183.7);
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH,new FormatCluster("CAL","mVolts",calibratedData[iBAHigh]));
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW,new FormatCluster("CAL","mVolts",calibratedData[iBALow]));	
				}
			}
			if ((mEnabledSensors & SENSOR_HEART) > 0) {
				int iHeartRate = getSignalIndex(Shimmer2.ObjectClusterSensorName.HEART_RATE);
				tempData[0] = (double)newPacketInt[iHeartRate];
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.HEART_RATE,new FormatCluster("RAW",NO_UNIT,tempData[0]));
				if (mEnableCalibration){
					calibratedData[iHeartRate]=tempData[0];
					if (mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1){

					} else {
						if (tempData[0]==0){
							calibratedData[iHeartRate]=	mLastKnownHeartRate;
						} else {
							calibratedData[iHeartRate]=(int)(1024/tempData[0]*60);
							mLastKnownHeartRate=calibratedData[iHeartRate];
						}
					}
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.HEART_RATE,new FormatCluster("CAL","BPM",calibratedData[iHeartRate]));	
				}
			}
			if ((mEnabledSensors& SENSOR_EXP_BOARD_A0) > 0) {
				int iA0 = getSignalIndex(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0);
				tempData[0] = (double)newPacketInt[iA0];
				if (getPMux()==0){
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
					if (mEnableCalibration){
						calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0,new FormatCluster("CAL","mVolts",calibratedData[iA0]));
					}
				} else {
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.VOLT_REG,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
					if (mEnableCalibration){
						calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1)*1.988;
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.VOLT_REG,new FormatCluster("CAL","mVolts",calibratedData[iA0]));
					}

				}
			}					
			if ((mEnabledSensors & SENSOR_EXP_BOARD_A7) > 0) {
				int iA7 = getSignalIndex(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7);
				tempData[0] = (double)newPacketInt[iA7];
				if (getPMux()==0){
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));
					if (mEnableCalibration){
						calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1);
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7,new FormatCluster("CAL","mVolts",calibratedData[iA7]));
					} else {
						objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BATTERY,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));
						if (mEnableCalibration){
							calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1)*2;
							objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BATTERY,new FormatCluster("CAL","mVolts",calibratedData[iA7]));
						}

					}
				} 
			}
			if  ((mEnabledSensors & SENSOR_BATT) > 0) {
				int iA0 = getSignalIndex(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.VOLT_REG,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));

				int iA7 = getSignalIndex(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7);
				objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BATTERY,new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));


				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iA0];
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1)*1.988;
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.VOLT_REG,new FormatCluster("CAL","mVolts",calibratedData[iA0]));

					tempData[0] = (double)newPacketInt[iA7];
					calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1)*2;
					objectCluster.mPropertyCluster.put(Shimmer2.ObjectClusterSensorName.BATTERY,new FormatCluster("CAL","mVolts",calibratedData[iA7]));	
					mVSenseBattMA.addValue(calibratedData[iA7]);
					checkBattery();
				}
			}
		
			
		}
		
		
		return objectCluster;
	}
	
	@Deprecated
	protected Object buildMsg(byte[] newPacket, Object object) {
		ObjectCluster objectCluster = (ObjectCluster) object;
		objectCluster.mRawData = newPacket;
		objectCluster.mSystemTimeStamp=ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
		double [] calibratedData=new double[mNChannels + 1]; //plus 1 because of the time stamp
		long[] newPacketInt=parsedData(newPacket,mSignalDataTypeArray);
		double[] tempData=new double[3];
		Vector3d accelerometer = new Vector3d();
		Vector3d magnetometer = new Vector3d();
		Vector3d gyroscope = new Vector3d();

		int iTimeStamp=getSignalIndex("TimeStamp"); //find index
		tempData[0]=(double)newPacketInt[1];
		objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iTimeStamp]));
		if (mEnableCalibration){
			objectCluster.mPropertyCluster.put("Timestamp",new FormatCluster("CAL","mSecs",calibrateTimeStamp((double)newPacketInt[iTimeStamp])));
		}
		objectCluster = callAdditionalServices(objectCluster);


		if (mHardwareVersion==HW_ID.SHIMMER_SR30 || mHardwareVersion==HW_ID.SHIMMER_3){
			if (((mEnabledSensors & SENSOR_ACCEL) > 0)){
				int iAccelX=getSignalIndex("Low Noise Accelerometer X"); //find index
				int iAccelY=getSignalIndex("Low Noise Accelerometer Y"); //find index
				int iAccelZ=getSignalIndex("Low Noise Accelerometer Z"); //find index
				tempData[0]=(double)newPacketInt[iAccelX];
				tempData[1]=(double)newPacketInt[iAccelY];
				tempData[2]=(double)newPacketInt[iAccelZ];

				objectCluster.mPropertyCluster.put("Low Noise Accelerometer X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put("Low Noise Accelerometer Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put("Low Noise Accelerometer Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));

				if (mEnableCalibration){
					double[] accelCalibratedData;
					accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixAnalogAccel, mSensitivityMatrixAnalogAccel, mOffsetVectorAnalogAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];
					if (mDefaultCalibrationParametersAccel == true) {
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer X",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer Y",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer Z",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];

					} else {
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer X",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer Y",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Low Noise Accelerometer Z",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];

					}
				}
			}
			if (((mEnabledSensors & SENSOR_DACCEL) > 0)){
				int iAccelX=getSignalIndex("Wide Range Accelerometer X"); //find index
				int iAccelY=getSignalIndex("Wide Range Accelerometer Y"); //find index
				int iAccelZ=getSignalIndex("Wide Range Accelerometer Z"); //find index
				//check range

				tempData[0]=(double)newPacketInt[iAccelX];
				tempData[1]=(double)newPacketInt[iAccelY];
				tempData[2]=(double)newPacketInt[iAccelZ];
				objectCluster.mPropertyCluster.put("Wide Range Accelerometer X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put("Wide Range Accelerometer Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put("Wide Range Accelerometer Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));


				if (mEnableCalibration){
					double[] accelCalibratedData;
					accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixWRAccel, mSensitivityMatrixWRAccel, mOffsetVectorWRAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];
					if (mDefaultCalibrationParametersDigitalAccel == true) {
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer X",new FormatCluster("CAL","m/(sec^2)*",calibratedData[iAccelX]));
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer Y",new FormatCluster("CAL","m/(sec^2)*",calibratedData[iAccelY]));
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer Z",new FormatCluster("CAL","m/(sec^2)*",calibratedData[iAccelZ]));
						if (((mEnabledSensors & SENSOR_ACCEL) == 0)){
							accelerometer.x=accelCalibratedData[0]; //this is used to calculate quaternions // skip if Low noise is enabled
							accelerometer.y=accelCalibratedData[1];
							accelerometer.z=accelCalibratedData[2];
						}
					} else {
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer X",new FormatCluster("CAL","m/(sec^2)",calibratedData[iAccelX]));
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer Y",new FormatCluster("CAL","m/(sec^2)",calibratedData[iAccelY]));
						objectCluster.mPropertyCluster.put("Wide Range Accelerometer Z",new FormatCluster("CAL","m/(sec^2)",calibratedData[iAccelZ]));
						if (((mEnabledSensors & SENSOR_ACCEL) == 0)){
							accelerometer.x=accelCalibratedData[0];
							accelerometer.y=accelCalibratedData[1];
							accelerometer.z=accelCalibratedData[2];
						}

					}	
				}
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
				int iGyroX=getSignalIndex("Gyroscope X");
				int iGyroY=getSignalIndex("Gyroscope Y");
				int iGyroZ=getSignalIndex("Gyroscope Z");
				tempData[0]=(double)newPacketInt[iGyroX];
				tempData[1]=(double)newPacketInt[iGyroY];
				tempData[2]=(double)newPacketInt[iGyroZ];


				objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroX]));
				objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroY]));
				objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroZ]));

				if (mEnableCalibration){
					double[] gyroCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixGyroscope, mSensitivityMatrixGyroscope, mOffsetVectorGyroscope);
					calibratedData[iGyroX]=gyroCalibratedData[0];
					calibratedData[iGyroY]=gyroCalibratedData[1];
					calibratedData[iGyroZ]=gyroCalibratedData[2];
					if (mDefaultCalibrationParametersGyro == true) {
						objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
					} else {
						objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("CAL","deg/sec",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("CAL","deg/sec",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("CAL","deg/sec",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
						if (mEnableOntheFlyGyroOVCal){
							mGyroX.addValue(gyroCalibratedData[0]);
							mGyroY.addValue(gyroCalibratedData[1]);
							mGyroZ.addValue(gyroCalibratedData[2]);
							mGyroXRaw.addValue((double)newPacketInt[iGyroX]);
							mGyroYRaw.addValue((double)newPacketInt[iGyroY]);
							mGyroZRaw.addValue((double)newPacketInt[iGyroZ]);
							if (mGyroX.getStandardDeviation()<mGyroOVCalThreshold && mGyroY.getStandardDeviation()<mGyroOVCalThreshold && mGyroZ.getStandardDeviation()<mGyroOVCalThreshold){
								mOffsetVectorGyroscope[0][0]=mGyroXRaw.getMean();
								mOffsetVectorGyroscope[1][0]=mGyroYRaw.getMean();
								mOffsetVectorGyroscope[2][0]=mGyroZRaw.getMean();
							}
						}
					} 
				}

			}
			if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
				int iMagX=getSignalIndex("Magnetometer X");
				int iMagY=getSignalIndex("Magnetometer Y");
				int iMagZ=getSignalIndex("Magnetometer Z");
				tempData[0]=(double)newPacketInt[iMagX];
				tempData[1]=(double)newPacketInt[iMagY];
				tempData[2]=(double)newPacketInt[iMagZ];
				objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagX]));
				objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagY]));
				objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagZ]));
				if (mEnableCalibration){
					double[] magCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixMagnetometer, mSensitivityMatrixMagnetometer, mOffsetVectorMagnetometer);
					calibratedData[iMagX]=magCalibratedData[0];
					calibratedData[iMagY]=magCalibratedData[1];
					calibratedData[iMagZ]=magCalibratedData[2];
					if (mDefaultCalibrationParametersMag == true) {
						objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("CAL","local*",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("CAL","local*",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("CAL","local*",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					} else {
						objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("CAL","local",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("CAL","local",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("CAL","local",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					}
				}
			}

			if ((mEnabledSensors & SENSOR_BATT) > 0) {
				int iA0 = getSignalIndex("VSenseBatt");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("VSenseBatt",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1)*1.988;
					objectCluster.mPropertyCluster.put("VSenseBatt",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
					mVSenseBattMA.addValue(calibratedData[iA0]);
					checkBattery();
				}
			}
			if ((mEnabledSensors & SENSOR_EXT_ADC_A7) > 0) {
				int iA0 = getSignalIndex("External ADC A7");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("External ADC A7",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("External ADC A7",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_EXT_ADC_A6) > 0) {
				int iA0 = getSignalIndex("External ADC A6");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("External ADC A6",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("External ADC A6",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_EXT_ADC_A15) > 0) {
				int iA0 = getSignalIndex("External ADC A15");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("External ADC A15",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("External ADC A15",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_INT_ADC_A1) > 0) {
				int iA0 = getSignalIndex("Internal ADC A1");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("Internal ADC A1",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("Internal ADC A1",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_INT_ADC_A12) > 0) {
				int iA0 = getSignalIndex("Internal ADC A12");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("Internal ADC A12",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("Internal ADC A12",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_INT_ADC_A13) > 0) {
				int iA0 = getSignalIndex("Internal ADC A13");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("Internal ADC A13",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("Internal ADC A13",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if ((mEnabledSensors & SENSOR_INT_ADC_A14) > 0) {
				int iA0 = getSignalIndex("Internal ADC A14");
				tempData[0] = (double)newPacketInt[iA0];
				objectCluster.mPropertyCluster.put("Internal ADC A14",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
				if (mEnableCalibration){
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
					objectCluster.mPropertyCluster.put("Internal ADC A14",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
				}
			}
			if (((mEnabledSensors & SENSOR_ACCEL) > 0 || (mEnabledSensors & SENSOR_DACCEL) > 0) && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled ){
				if (mEnableCalibration){
					if (mOrientationAlgo==null){
						mOrientationAlgo = new GradDes3DOrientation(0.4, (double)1/mShimmerSamplingRate, 1, 0, 0,0);
					}
					Quaternion q = mOrientationAlgo.update(accelerometer.x,accelerometer.y,accelerometer.z, gyroscope.x,gyroscope.y,gyroscope.z, magnetometer.x,magnetometer.y,magnetometer.z);					double theta, Rx, Ry, Rz, rho;
					rho = Math.acos(q.q1);
					theta = rho * 2;
					Rx = q.q2 / Math.sin(rho);
					Ry = q.q3 / Math.sin(rho);
					Rz = q.q4 / Math.sin(rho);
					objectCluster.mPropertyCluster.put("Axis Angle A",new FormatCluster("CAL","local",theta));
					objectCluster.mPropertyCluster.put("Axis Angle X",new FormatCluster("CAL","local",Rx));
					objectCluster.mPropertyCluster.put("Axis Angle Y",new FormatCluster("CAL","local",Ry));
					objectCluster.mPropertyCluster.put("Axis Angle Z",new FormatCluster("CAL","local",Rz));
					objectCluster.mPropertyCluster.put("Quaternion 0",new FormatCluster("CAL","local",q.q1));
					objectCluster.mPropertyCluster.put("Quaternion 1",new FormatCluster("CAL","local",q.q2));
					objectCluster.mPropertyCluster.put("Quaternion 2",new FormatCluster("CAL","local",q.q3));
					objectCluster.mPropertyCluster.put("Quaternion 3",new FormatCluster("CAL","local",q.q4));
				}
			}

			if ((mEnabledSensors & SENSOR_EXG1_24BIT) >0){
				int iexg1ch1 = getSignalIndex("EXG1 24Bit CH1");
				int iexg1ch2 = getSignalIndex("EXG1 24Bit CH2");
				int iexg1sta = getSignalIndex("EXG1 STATUS");
				double exg1ch1 = (double)newPacketInt[iexg1ch1];
				double exg1ch2 = (double)newPacketInt[iexg1ch2];
				double exg1sta = (double)newPacketInt[iexg1sta];
				objectCluster.mPropertyCluster.put("EXG1 STATUS",new FormatCluster("RAW",NO_UNIT,exg1sta));
				if (mEnableCalibration){
					double calexg1ch1 = exg1ch1 *(((2.42*1000)/mEXG1CH1GainValue)/(Math.pow(2,23)-1));
					double calexg1ch2 = exg1ch2 *(((2.42*1000)/mEXG1CH2GainValue)/(Math.pow(2,23)-1));
					if (isEXGUsingDefaultECGConfiguration()){
						objectCluster.mPropertyCluster.put("ECG LL-RA",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("ECG LA-RA",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("ECG LL-RA",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("ECG LA-RA",new FormatCluster("CAL","mVolts",calexg1ch2));
					} else if (isEXGUsingDefaultEMGConfiguration()){
						objectCluster.mPropertyCluster.put("EMG CH1",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("EMG CH2",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("EMG CH1",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("EMG CH2",new FormatCluster("CAL","mVolts",calexg1ch2));
					} else {
						objectCluster.mPropertyCluster.put("EXG1 CH1",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("EXG1 CH2",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("EXG1 CH1",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("EXG1 CH2",new FormatCluster("CAL","mVolts",calexg1ch2));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_EXG2_24BIT) >0){
				int iexg2ch1 = getSignalIndex("EXG2 24Bit CH1");
				int iexg2ch2 = getSignalIndex("EXG2 24Bit CH2");
				int iexg2sta = getSignalIndex("EXG2 STATUS");
				double exg2ch1 = (double)newPacketInt[iexg2ch1];
				double exg2ch2 = (double)newPacketInt[iexg2ch2];
				double exg2sta = (double)newPacketInt[iexg2sta];

				objectCluster.mPropertyCluster.put("EXG2 STATUS",new FormatCluster("RAW",NO_UNIT,exg2sta));
				if (mEnableCalibration){
					double calexg2ch1 = exg2ch1 *(((2.42*1000)/mEXG2CH1GainValue)/(Math.pow(2,23)-1));
					double calexg2ch2 = exg2ch2 *(((2.42*1000)/mEXG2CH2GainValue)/(Math.pow(2,23)-1));
					if (isEXGUsingDefaultECGConfiguration()){
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put("ECG Vx-RL",new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("CAL","mVolts",calexg2ch1));
						objectCluster.mPropertyCluster.put("ECG Vx-RL",new FormatCluster("CAL","mVolts",calexg2ch2));
					}
					else if (isEXGUsingDefaultEMGConfiguration()){
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("CAL","mVolts",0));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("CAL","mVolts",0));
					} else {
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("CAL","mVolts",calexg2ch1));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("CAL","mVolts",calexg2ch2));	
					}
				}
			}

			if ((mEnabledSensors & SENSOR_EXG1_16BIT) >0){
				int iexg1ch1 = getSignalIndex("EXG1 16Bit CH1");
				int iexg1ch2 = getSignalIndex("EXG1 16Bit CH2");
				int iexg1sta = getSignalIndex("EXG1 STATUS");
				double exg1ch1 = (double)newPacketInt[iexg1ch1];
				double exg1ch2 = (double)newPacketInt[iexg1ch2];
				double exg1sta = (double)newPacketInt[iexg1sta];
				objectCluster.mPropertyCluster.put("EXG1 STATUS",new FormatCluster("RAW",NO_UNIT,exg1sta));
				if (mEnableCalibration){
					double calexg1ch1 = exg1ch1 *(((2.42*1000)/(mEXG1CH1GainValue*2))/(Math.pow(2,15)-1));
					double calexg1ch2 = exg1ch2 *(((2.42*1000)/(mEXG1CH2GainValue*2))/(Math.pow(2,15)-1));
					if (isEXGUsingDefaultECGConfiguration()){
						objectCluster.mPropertyCluster.put("ECG LL-RA",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("ECG LA-RA",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("ECG LL-RA",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("ECG LA-RA",new FormatCluster("CAL","mVolts",calexg1ch2));
					} else if (isEXGUsingDefaultEMGConfiguration()){
						objectCluster.mPropertyCluster.put("EMG CH1",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("EMG CH2",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("EMG CH1",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("EMG CH2",new FormatCluster("CAL","mVolts",calexg1ch2));
					} else {
						objectCluster.mPropertyCluster.put("EXG1 CH1 16Bit",new FormatCluster("RAW",NO_UNIT,exg1ch1));
						objectCluster.mPropertyCluster.put("EXG1 CH2 16Bit",new FormatCluster("RAW",NO_UNIT,exg1ch2));
						objectCluster.mPropertyCluster.put("EXG1 CH1 16Bit",new FormatCluster("CAL","mVolts",calexg1ch1));
						objectCluster.mPropertyCluster.put("EXG1 CH2 16Bit",new FormatCluster("CAL","mVolts",calexg1ch2));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_EXG2_16BIT) >0){
				int iexg2ch1 = getSignalIndex("EXG2 16Bit CH1");
				int iexg2ch2 = getSignalIndex("EXG2 16Bit CH2");
				int iexg2sta = getSignalIndex("EXG2 STATUS");
				double exg2ch1 = (double)newPacketInt[iexg2ch1];
				double exg2ch2 = (double)newPacketInt[iexg2ch2];
				double exg2sta = (double)newPacketInt[iexg2sta];

				objectCluster.mPropertyCluster.put("EXG2 STATUS",new FormatCluster("RAW",NO_UNIT,exg2sta));
				if (mEnableCalibration){
					double calexg2ch1 = ((exg2ch1)) *(((2.42*1000)/(mEXG2CH1GainValue*2))/(Math.pow(2,15)-1));
					double calexg2ch2 = ((exg2ch2)) *(((2.42*1000)/(mEXG2CH2GainValue*2))/(Math.pow(2,15)-1));
					if (isEXGUsingDefaultECGConfiguration()){
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put("ECG Vx-RL",new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("CAL","mVolts",calexg2ch1));
						objectCluster.mPropertyCluster.put("ECG Vx-RL",new FormatCluster("CAL","mVolts",calexg2ch2));
					}
					else if (isEXGUsingDefaultEMGConfiguration()){
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("RAW",NO_UNIT,0));
						objectCluster.mPropertyCluster.put("EXG2 CH1",new FormatCluster("CAL","mVolts",0));
						objectCluster.mPropertyCluster.put("EXG2 CH2",new FormatCluster("CAL","mVolts",0));
					} else {
						objectCluster.mPropertyCluster.put("EXG2 CH1 16Bit",new FormatCluster("RAW",NO_UNIT,exg2ch1));
						objectCluster.mPropertyCluster.put("EXG2 CH2 16Bit",new FormatCluster("RAW",NO_UNIT,exg2ch2));
						objectCluster.mPropertyCluster.put("EXG2 CH1 16Bit",new FormatCluster("CAL","mVolts",calexg2ch1));
						objectCluster.mPropertyCluster.put("EXG2 CH2 16Bit",new FormatCluster("CAL","mVolts",calexg2ch2));
					}
				}
			}

			if ((mEnabledSensors & SENSOR_BMP180) >0){
				int iUT = getSignalIndex("Temperature");
				int iUP = getSignalIndex("Pressure");
				double UT = (double)newPacketInt[iUT];
				double UP = (double)newPacketInt[iUP];
				UP=UP/Math.pow(2,8-mPressureResolution);
				objectCluster.mPropertyCluster.put("Pressure",new FormatCluster("RAW",NO_UNIT,UP));
				objectCluster.mPropertyCluster.put("Temperature",new FormatCluster("RAW",NO_UNIT,UT));
				if (mEnableCalibration){
					double[] bmp180caldata= calibratePressureSensorData(UP,UT);
					objectCluster.mPropertyCluster.put("Pressure",new FormatCluster("CAL","kPa",bmp180caldata[0]/1000));
					objectCluster.mPropertyCluster.put("Temperature",new FormatCluster("CAL","Celsius",bmp180caldata[1]));
				}
			}

			if ((mEnabledSensors & SENSOR_BRIDGE_AMP) > 0) {
				int iBAHigh = getSignalIndex("Bridge Amplifier High");
				int iBALow = getSignalIndex("Bridge Amplifier Low");
				objectCluster.mPropertyCluster.put("Bridge Amplifier High",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBAHigh]));
				objectCluster.mPropertyCluster.put("Bridge Amplifier Low",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBALow]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iBAHigh];
					tempData[1] = (double)newPacketInt[iBALow];
					calibratedData[iBAHigh]=calibrateU12AdcValue(tempData[0],60,3,551);
					calibratedData[iBALow]=calibrateU12AdcValue(tempData[1],1950,3,183.7);
					objectCluster.mPropertyCluster.put("Bridge Amplifier High",new FormatCluster("CAL","mVolts",calibratedData[iBAHigh]));
					objectCluster.mPropertyCluster.put("Bridge Amplifier Low",new FormatCluster("CAL","mVolts",calibratedData[iBALow]));	
				}
			}

			if ((mEnabledSensors & SENSOR_GSR) > 0) {
				int iGSR = getSignalIndex("GSR Raw");
				tempData[0] = (double)newPacketInt[iGSR];
				int newGSRRange = -1; // initialized to -1 so it will only come into play if mGSRRange = 4  

				double p1=0,p2=0;//,p3=0,p4=0,p5=0;
				if (mGSRRange==4){
					newGSRRange=(49152 & (int)tempData[0])>>14; 
				}
				if (mGSRRange==0 || newGSRRange==0) { //Note that from FW 1.0 onwards the MSB of the GSR data contains the range
					// the polynomial function used for calibration has been deprecated, it is replaced with a linear function
					/* p1 = 6.5995E-9;
		                    p2 = -6.895E-5;
		                    p3 = 2.699E-1;
		                    p4 = -4.769835E+2;
		                    p5 = 3.403513341E+5;*/
					if (mHardwareVersion!=HW_ID.SHIMMER_3){
						p1 = 0.0373;
						p2 = -24.9915;
					} else {
						p1 = 0.0363;
						p2 = -24.8617;
					}
				} else if (mGSRRange==1 || newGSRRange==1) {
					/*p1 = 1.3569627E-8;
		                    p2 = -1.650399E-4;
		                    p3 = 7.54199E-1;
		                    p4 = -1.5726287856E+3;
		                    p5 = 1.367507927E+6;*/
					if (mHardwareVersion!=HW_ID.SHIMMER_3){
						p1 = 0.0054;
						p2 = -3.5194;
					} else {
						p1 = 0.0051;
						p2 = -3.8357;
					}
				} else if (mGSRRange==2 || newGSRRange==2) {
					/*p1 = 2.550036498E-8;
		                    p2 = -3.3136E-4;
		                    p3 = 1.6509426597E+0;
		                    p4 = -3.833348044E+3;
		                    p5 = 3.8063176947E+6;*/
					if (mHardwareVersion!=HW_ID.SHIMMER_3){
						p1 = 0.0015;
						p2 = -1.0163;
					} else {
						p1 = 0.0015;
						p2 = -1.0067;
					}
				} else if (mGSRRange==3  || newGSRRange==3) {
					/*p1 = 3.7153627E-7;
		                    p2 = -4.239437E-3;
		                    p3 = 1.7905709E+1;
		                    p4 = -3.37238657E+4;
		                    p5 = 2.53680446279E+7;*/
					if (mHardwareVersion!=HW_ID.SHIMMER_3){
						p1 = 4.5580e-04;
						p2 = -0.3014;
					} else {
						p1 = 4.4513e-04;
						p2 = -0.3193;
					}
				}
				objectCluster.mPropertyCluster.put("GSR",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGSR]));
				if (mEnableCalibration){
					calibratedData[iGSR] = calibrateGsrData(tempData[0],p1,p2);
					objectCluster.mPropertyCluster.put("GSR",new FormatCluster("CAL","kOhms",calibratedData[iGSR]));
				}
			}
		} else { //start of Shimmer2

			if ((mEnabledSensors & SENSOR_ACCEL) > 0){
				int iAccelX=getSignalIndex("Accelerometer X"); //find index
				int iAccelY=getSignalIndex("Accelerometer Y"); //find index
				int iAccelZ=getSignalIndex("Accelerometer Z"); //find index
				objectCluster.mPropertyCluster.put("Accelerometer X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelX]));
				objectCluster.mPropertyCluster.put("Accelerometer Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelY]));
				objectCluster.mPropertyCluster.put("Accelerometer Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iAccelZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iAccelX];
					tempData[1]=(double)newPacketInt[iAccelY];
					tempData[2]=(double)newPacketInt[iAccelZ];
					double[] accelCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixAnalogAccel, mSensitivityMatrixAnalogAccel, mOffsetVectorAnalogAccel);
					calibratedData[iAccelX]=accelCalibratedData[0];
					calibratedData[iAccelY]=accelCalibratedData[1];
					calibratedData[iAccelZ]=accelCalibratedData[2];
					if (mDefaultCalibrationParametersAccel == true) {
						objectCluster.mPropertyCluster.put("Accelerometer X",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Accelerometer Y",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Accelerometer Z",new FormatCluster("CAL","m/(sec^2)*",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];
					} else {
						objectCluster.mPropertyCluster.put("Accelerometer X",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Accelerometer Y",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Accelerometer Z",new FormatCluster("CAL","m/(sec^2)",accelCalibratedData[2]));
						accelerometer.x=accelCalibratedData[0];
						accelerometer.y=accelCalibratedData[1];
						accelerometer.z=accelCalibratedData[2];
					}
				}

			}

			if ((mEnabledSensors & SENSOR_GYRO) > 0) {
				int iGyroX=getSignalIndex("Gyroscope X");
				int iGyroY=getSignalIndex("Gyroscope Y");
				int iGyroZ=getSignalIndex("Gyroscope Z");					
				objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroX]));
				objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroY]));
				objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGyroZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iGyroX];
					tempData[1]=(double)newPacketInt[iGyroY];
					tempData[2]=(double)newPacketInt[iGyroZ];
					double[] gyroCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixGyroscope, mSensitivityMatrixGyroscope, mOffsetVectorGyroscope);
					calibratedData[iGyroX]=gyroCalibratedData[0];
					calibratedData[iGyroY]=gyroCalibratedData[1];
					calibratedData[iGyroZ]=gyroCalibratedData[2];
					if (mDefaultCalibrationParametersGyro == true) {
						objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("CAL","deg/sec*",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
					} else {
						objectCluster.mPropertyCluster.put("Gyroscope X",new FormatCluster("CAL","deg/sec",gyroCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Gyroscope Y",new FormatCluster("CAL","deg/sec",gyroCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Gyroscope Z",new FormatCluster("CAL","deg/sec",gyroCalibratedData[2]));
						gyroscope.x=gyroCalibratedData[0]*Math.PI/180;
						gyroscope.y=gyroCalibratedData[1]*Math.PI/180;
						gyroscope.z=gyroCalibratedData[2]*Math.PI/180;
						if (mEnableOntheFlyGyroOVCal){
							mGyroX.addValue(gyroCalibratedData[0]);
							mGyroY.addValue(gyroCalibratedData[1]);
							mGyroZ.addValue(gyroCalibratedData[2]);
							mGyroXRaw.addValue((double)newPacketInt[iGyroX]);
							mGyroYRaw.addValue((double)newPacketInt[iGyroY]);
							mGyroZRaw.addValue((double)newPacketInt[iGyroZ]);
							if (mGyroX.getStandardDeviation()<mGyroOVCalThreshold && mGyroY.getStandardDeviation()<mGyroOVCalThreshold && mGyroZ.getStandardDeviation()<mGyroOVCalThreshold){
								mOffsetVectorGyroscope[0][0]=mGyroXRaw.getMean();
								mOffsetVectorGyroscope[1][0]=mGyroYRaw.getMean();
								mOffsetVectorGyroscope[2][0]=mGyroZRaw.getMean();
							}
						}
					} 
				}
			}
			if ((mEnabledSensors & SENSOR_MAG) > 0) {
				int iMagX=getSignalIndex("Magnetometer X");
				int iMagY=getSignalIndex("Magnetometer Y");
				int iMagZ=getSignalIndex("Magnetometer Z");
				objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagX]));
				objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagY]));
				objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iMagZ]));
				if (mEnableCalibration){
					tempData[0]=(double)newPacketInt[iMagX];
					tempData[1]=(double)newPacketInt[iMagY];
					tempData[2]=(double)newPacketInt[iMagZ];
					double[] magCalibratedData=calibrateInertialSensorData(tempData, mAlignmentMatrixMagnetometer, mSensitivityMatrixMagnetometer, mOffsetVectorMagnetometer);
					calibratedData[iMagX]=magCalibratedData[0];
					calibratedData[iMagY]=magCalibratedData[1];
					calibratedData[iMagZ]=magCalibratedData[2];
					if (mDefaultCalibrationParametersMag == true) {
						objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("CAL","local*",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("CAL","local*",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("CAL","local*",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					} else {
						objectCluster.mPropertyCluster.put("Magnetometer X",new FormatCluster("CAL","local",magCalibratedData[0]));
						objectCluster.mPropertyCluster.put("Magnetometer Y",new FormatCluster("CAL","local",magCalibratedData[1]));
						objectCluster.mPropertyCluster.put("Magnetometer Z",new FormatCluster("CAL","local",magCalibratedData[2]));
						magnetometer.x=magCalibratedData[0];
						magnetometer.y=magCalibratedData[1];
						magnetometer.z=magCalibratedData[2];
					}
				}
			}


			if ((mEnabledSensors & SENSOR_ACCEL) > 0 && (mEnabledSensors & SENSOR_GYRO) > 0 && (mEnabledSensors & SENSOR_MAG) > 0 && mOrientationEnabled ){
				if (mEnableCalibration){
					if (mOrientationAlgo==null){
						mOrientationAlgo = new GradDes3DOrientation(0.4, (double)1/mShimmerSamplingRate, 1, 0, 0,0);
					}
					Quaternion q = mOrientationAlgo.update(accelerometer.x,accelerometer.y,accelerometer.z, gyroscope.x,gyroscope.y,gyroscope.z, magnetometer.x,magnetometer.y,magnetometer.z);
					double theta, Rx, Ry, Rz, rho;
					rho = Math.acos(q.q1);
					theta = rho * 2;
					Rx = q.q2 / Math.sin(rho);
					Ry = q.q3 / Math.sin(rho);
					Rz = q.q4 / Math.sin(rho);
					objectCluster.mPropertyCluster.put("Axis Angle A",new FormatCluster("CAL","local",theta));
					objectCluster.mPropertyCluster.put("Axis Angle X",new FormatCluster("CAL","local",Rx));
					objectCluster.mPropertyCluster.put("Axis Angle Y",new FormatCluster("CAL","local",Ry));
					objectCluster.mPropertyCluster.put("Axis Angle Z",new FormatCluster("CAL","local",Rz));
					objectCluster.mPropertyCluster.put("Quaternion 0",new FormatCluster("CAL","local",q.q1));
					objectCluster.mPropertyCluster.put("Quaternion 1",new FormatCluster("CAL","local",q.q2));
					objectCluster.mPropertyCluster.put("Quaternion 2",new FormatCluster("CAL","local",q.q3));
					objectCluster.mPropertyCluster.put("Quaternion 3",new FormatCluster("CAL","local",q.q4));
				}
			}

			if ((mEnabledSensors & SENSOR_GSR) > 0) {
				int iGSR = getSignalIndex("GSR Raw");
				tempData[0] = (double)newPacketInt[iGSR];
				int newGSRRange = -1; // initialized to -1 so it will only come into play if mGSRRange = 4  

				double p1=0,p2=0;//,p3=0,p4=0,p5=0;
				if (mGSRRange==4){
					newGSRRange=(49152 & (int)tempData[0])>>14; 
				}
				if (mGSRRange==0 || newGSRRange==0) { //Note that from FW 1.0 onwards the MSB of the GSR data contains the range
					// the polynomial function used for calibration has been deprecated, it is replaced with a linear function
					/* p1 = 6.5995E-9;
		                    p2 = -6.895E-5;
		                    p3 = 2.699E-1;
		                    p4 = -4.769835E+2;
		                    p5 = 3.403513341E+5;*/
					p1 = 0.0373;
					p2 = -24.9915;
				} else if (mGSRRange==1 || newGSRRange==1) {
					/*p1 = 1.3569627E-8;
		                    p2 = -1.650399E-4;
		                    p3 = 7.54199E-1;
		                    p4 = -1.5726287856E+3;
		                    p5 = 1.367507927E+6;*/
					p1 = 0.0054;
					p2 = -3.5194;
				} else if (mGSRRange==2 || newGSRRange==2) {
					/*p1 = 2.550036498E-8;
		                    p2 = -3.3136E-4;
		                    p3 = 1.6509426597E+0;
		                    p4 = -3.833348044E+3;
		                    p5 = 3.8063176947E+6;*/
					p1 = 0.0015;
					p2 = -1.0163;
				} else if (mGSRRange==3  || newGSRRange==3) {
					/*p1 = 3.7153627E-7;
		                    p2 = -4.239437E-3;
		                    p3 = 1.7905709E+1;
		                    p4 = -3.37238657E+4;
		                    p5 = 2.53680446279E+7;*/
					p1 = 4.5580e-04;
					p2 = -0.3014;
				}
				objectCluster.mPropertyCluster.put("GSR",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iGSR]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iGSR];
					calibratedData[iGSR] = calibrateGsrData(tempData[0],p1,p2);
					objectCluster.mPropertyCluster.put("GSR",new FormatCluster("CAL","kOhms",calibratedData[iGSR]));
				}
			}
			if ((mEnabledSensors & SENSOR_ECG) > 0) {
				int iECGRALL = getSignalIndex("ECG RA LL");
				int iECGLALL = getSignalIndex("ECG LA LL");
				objectCluster.mPropertyCluster.put("ECG RA-LL",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iECGRALL]));
				objectCluster.mPropertyCluster.put("ECG LA-LL",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iECGLALL]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iECGRALL];
					tempData[1] = (double)newPacketInt[iECGLALL];
					calibratedData[iECGRALL]=calibrateU12AdcValue(tempData[0],OffsetECGRALL,3,GainECGRALL);
					calibratedData[iECGLALL]=calibrateU12AdcValue(tempData[1],OffsetECGLALL,3,GainECGLALL);
					if (mDefaultCalibrationParametersECG == true) {
						objectCluster.mPropertyCluster.put("ECG RA-LL",new FormatCluster("CAL","mVolts*",calibratedData[iECGRALL]));
						objectCluster.mPropertyCluster.put("ECG LA-LL",new FormatCluster("CAL","mVolts*",calibratedData[iECGLALL]));
					} else {
						objectCluster.mPropertyCluster.put("ECG RA-LL",new FormatCluster("CAL","mVolts",calibratedData[iECGRALL]));
						objectCluster.mPropertyCluster.put("ECG LA-LL",new FormatCluster("CAL","mVolts",calibratedData[iECGLALL]));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_EMG) > 0) {
				int iEMG = getSignalIndex("EMG");
				objectCluster.mPropertyCluster.put("EMG",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iEMG]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iEMG];
					calibratedData[iEMG]=calibrateU12AdcValue(tempData[0],OffsetEMG,3,GainEMG);
					if (mDefaultCalibrationParametersEMG == true){
						objectCluster.mPropertyCluster.put("EMG",new FormatCluster("CAL","mVolts*",calibratedData[iEMG]));
					} else {
						objectCluster.mPropertyCluster.put("EMG",new FormatCluster("CAL","mVolts",calibratedData[iEMG]));
					}
				}
			}
			if ((mEnabledSensors & SENSOR_BRIDGE_AMP) > 0) {
				int iBAHigh = getSignalIndex("Bridge Amplifier High");
				int iBALow = getSignalIndex("Bridge Amplifier Low");
				objectCluster.mPropertyCluster.put("Bridge Amplifier High",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBAHigh]));
				objectCluster.mPropertyCluster.put("Bridge Amplifier Low",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iBALow]));
				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iBAHigh];
					tempData[1] = (double)newPacketInt[iBALow];
					calibratedData[iBAHigh]=calibrateU12AdcValue(tempData[0],60,3,551);
					calibratedData[iBALow]=calibrateU12AdcValue(tempData[1],1950,3,183.7);
					objectCluster.mPropertyCluster.put("Bridge Amplifier High",new FormatCluster("CAL","mVolts",calibratedData[iBAHigh]));
					objectCluster.mPropertyCluster.put("Bridge Amplifier Low",new FormatCluster("CAL","mVolts",calibratedData[iBALow]));	
				}
			}
			if ((mEnabledSensors & SENSOR_HEART) > 0) {
				int iHeartRate = getSignalIndex("Heart Rate");
				tempData[0] = (double)newPacketInt[iHeartRate];
				objectCluster.mPropertyCluster.put("Heart Rate",new FormatCluster("RAW",NO_UNIT,tempData[0]));
				if (mEnableCalibration){
					calibratedData[iHeartRate]=tempData[0];
					if (mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1){

					} else {
						if (tempData[0]==0){
							calibratedData[iHeartRate]=	mLastKnownHeartRate;
						} else {
							calibratedData[iHeartRate]=(int)(1024/tempData[0]*60);
							mLastKnownHeartRate=calibratedData[iHeartRate];
						}
					}
					objectCluster.mPropertyCluster.put("Heart Rate",new FormatCluster("CAL","BPM",calibratedData[iHeartRate]));	
				}
			}
			if ((mEnabledSensors& SENSOR_EXP_BOARD_A0) > 0) {
				int iA0 = getSignalIndex("Exp Board A0");
				tempData[0] = (double)newPacketInt[iA0];
				if (getPMux()==0){
					objectCluster.mPropertyCluster.put("ExpBoard A0",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
					if (mEnableCalibration){
						calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1);
						objectCluster.mPropertyCluster.put("ExpBoard A0",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
					}
				} else {
					objectCluster.mPropertyCluster.put("VSenseReg",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));
					if (mEnableCalibration){
						calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1)*1.988;
						objectCluster.mPropertyCluster.put("VSenseReg",new FormatCluster("CAL","mVolts",calibratedData[iA0]));
					}

				}
			}					
			if ((mEnabledSensors & SENSOR_EXP_BOARD_A7) > 0) {
				int iA7 = getSignalIndex("Exp Board A7");
				tempData[0] = (double)newPacketInt[iA7];
				if (getPMux()==0){
					objectCluster.mPropertyCluster.put("ExpBoard A7",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));
					if (mEnableCalibration){
						calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1);
						objectCluster.mPropertyCluster.put("ExpBoard A7",new FormatCluster("CAL","mVolts",calibratedData[iA7]));
					}
				} 
			}
			if  ((mEnabledSensors & SENSOR_BATT) > 0) {
				int iA0 = getSignalIndex("Exp Board A0");
				objectCluster.mPropertyCluster.put("VSenseReg",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA0]));

				int iA7 = getSignalIndex("Exp Board A7");
				objectCluster.mPropertyCluster.put("VSenseBatt",new FormatCluster("RAW",NO_UNIT,(double)newPacketInt[iA7]));


				if (mEnableCalibration){
					tempData[0] = (double)newPacketInt[iA0];
					calibratedData[iA0]=calibrateU12AdcValue(tempData[0],0,3,1)*1.988;
					objectCluster.mPropertyCluster.put("VSenseReg",new FormatCluster("CAL","mVolts",calibratedData[iA0]));

					tempData[0] = (double)newPacketInt[iA7];
					calibratedData[iA7]=calibrateU12AdcValue(tempData[0],0,3,1)*2;
					objectCluster.mPropertyCluster.put("VSenseBatt",new FormatCluster("CAL","mVolts",calibratedData[iA7]));	
					mVSenseBattMA.addValue(calibratedData[iA7]);
					checkBattery();
				}
			}
		}
		
		
		
		return objectCluster;
	}




	//protected abstract void writeLEDCommand(int i);

	/**
	 * Converts the raw packet byte values, into the corresponding calibrated and uncalibrated sensor values, the Instruction String determines the output 
	 * @param newPacket a byte array containing the current received packet
	 * @param Instructions an array string containing the commands to execute. It is currently not fully supported
	 * @return
	 */

	protected long[] parsedData(byte[] data,String[] dataType)
	{
		int iData=0;
		long[] formattedData=new long[dataType.length];

		for (int i=0;i<dataType.length;i++)
			if (dataType[i].equals("u8")) {
				formattedData[i]=(int)0xFF & data[iData];
				iData=iData+1;
			} else if (dataType[i].equals("i8")) {
				formattedData[i]=calculatetwoscomplement((int)((int)0xFF & data[iData]),8);
				iData=iData+1;
			} else if (dataType[i].equals("u12")) {

				formattedData[i]=(int)((int)(data[iData] & 0xFF) + ((int)(data[iData+1] & 0xFF) << 8));
				iData=iData+2;
			} else if (dataType[i].equals("i12>")) {
				formattedData[i]=calculatetwoscomplement((int)((int)(data[iData] & 0xFF) + ((int)(data[iData+1] & 0xFF) << 8)),16);
				formattedData[i]=formattedData[i]>>4; // shift right by 4 bits
				iData=iData+2;
			} else if (dataType[i].equals("u16")) {				
				formattedData[i]=(int)((int)(data[iData] & 0xFF) + ((int)(data[iData+1] & 0xFF) << 8));
				iData=iData+2;
			} else if (dataType[i].equals("u16r")) {				
				formattedData[i]=(int)((int)(data[iData+1] & 0xFF) + ((int)(data[iData+0] & 0xFF) << 8));
				iData=iData+2;
			} else if (dataType[i].equals("i16")) {
				formattedData[i]=calculatetwoscomplement((int)((int)(data[iData] & 0xFF) + ((int)(data[iData+1] & 0xFF) << 8)),16);
				//formattedData[i]=ByteBuffer.wrap(arrayb).order(ByteOrder.LITTLE_ENDIAN).getShort();
				iData=iData+2;
			} else if (dataType[i].equals("i16r")){
				formattedData[i]=calculatetwoscomplement((int)((int)(data[iData+1] & 0xFF) + ((int)(data[iData] & 0xFF) << 8)),16);
				//formattedData[i]=ByteBuffer.wrap(arrayb).order(ByteOrder.LITTLE_ENDIAN).getShort();
				iData=iData+2;
			} else if (dataType[i].equals("u24r")) {
				long xmsb =((long)(data[iData+0] & 0xFF) << 16);
				long msb =((long)(data[iData+1] & 0xFF) << 8);
				long lsb =((long)(data[iData+2] & 0xFF));
				formattedData[i]=xmsb + msb + lsb;
				iData=iData+3;
			}  else if (dataType[i].equals("u24")) {				
				long xmsb =((long)(data[iData+2] & 0xFF) << 16);
				long msb =((long)(data[iData+1] & 0xFF) << 8);
				long lsb =((long)(data[iData+0] & 0xFF));
				formattedData[i]=xmsb + msb + lsb;
				iData=iData+3;
			} else if (dataType[i].equals("i24r")) {
				long xmsb =((long)(data[iData+0] & 0xFF) << 16);
				long msb =((long)(data[iData+1] & 0xFF) << 8);
				long lsb =((long)(data[iData+2] & 0xFF));
				formattedData[i]=calculatetwoscomplement((int)(xmsb + msb + lsb),24);
				iData=iData+3;
			} else if (dataType[i].equals("u32signed")) {
				long offset = (((long)data[iData] & 0xFF));
				if (offset == 255){
					offset = 0;
				}
				long xxmsb =(((long)data[iData+4] & 0xFF) << 24);
				long xmsb =(((long)data[iData+3] & 0xFF) << 16);
				long msb =(((long)data[iData+2] & 0xFF) << 8);
				long lsb =(((long)data[iData+1] & 0xFF));
				formattedData[i]=(1-2*offset)*(xxmsb + xmsb + msb + lsb);
				iData=iData+5;
			} else if (dataType[i].equals("i32r")) {
				long xxmsb =((long)(data[iData+0] & 0xFF) << 24);
				long xmsb =((long)(data[iData+1] & 0xFF) << 16);
				long msb =((long)(data[iData+2] & 0xFF) << 8);
				long lsb =((long)(data[iData+3] & 0xFF) << 0);
				formattedData[i]=calculatetwoscomplement((long)(xxmsb + xmsb + msb + lsb),32);
				iData=iData+4;
			} else if (dataType[i].equals("u72")){
				// do something to parse the 9 byte data
				long offset = (((long)data[iData] & 0xFF));
				if (offset == 255){
					offset = 0;
				}
				
				long eigthmsb =(((long)data[iData+8] & 0x0FL) << 56);
				long seventhmsb =(((long)data[iData+7] & 0xFFL) << 48);
				long sixthmsb =(((long)data[iData+6] & 0xFFL) << 40);
				long fifthmsb =(((long)data[iData+5] & 0xFFL) << 32);
				long forthmsb =(((long)data[iData+4] & 0xFFL) << 24);
				long thirdmsb =(((long)data[iData+3] & 0xFFL) << 16);
				long msb =(((long)data[iData+2] & 0xFF) << 8);
				long lsb =(((long)data[iData+1] & 0xFF));
				formattedData[i]=(1-2*offset)*(eigthmsb + seventhmsb + sixthmsb + fifthmsb+ forthmsb+ thirdmsb + msb + lsb);
				iData=iData+9;
			}
		return formattedData;
	}
	/*
	 * Data Methods
	 * */  


	protected int[] formatdatapacketreverse(byte[] data,String[] dataType)
	{
		int iData=0;
		int[] formattedData=new int[dataType.length];

		for (int i=0;i<dataType.length;i++)
			if (dataType[i].equals("u8")) {
				formattedData[i]=(int)data[iData];
				iData=iData+1;
			}
			else if (dataType[i].equals("i8")) {
				formattedData[i]=calculatetwoscomplement((int)((int)0xFF & data[iData]),8);
				iData=iData+1;
			}
			else if (dataType[i].equals("u12")) {

				formattedData[i]=(int)((int)(data[iData+1] & 0xFF) + ((int)(data[iData] & 0xFF) << 8));
				iData=iData+2;
			}
			else if (dataType[i].equals("u16")) {

				formattedData[i]=(int)((int)(data[iData+1] & 0xFF) + ((int)(data[iData] & 0xFF) << 8));
				iData=iData+2;
			}
			else if (dataType[i].equals("i16")) {

				formattedData[i]=calculatetwoscomplement((int)((int)(data[iData+1] & 0xFF) + ((int)(data[iData] & 0xFF) << 8)),16);
				iData=iData+2;
			}
		return formattedData;
	}

	private int calculatetwoscomplement(int signedData, int bitLength)
	{
		int newData=signedData;
		if (signedData>=(1<<(bitLength-1))) {
			newData=-((signedData^(int)(Math.pow(2, bitLength)-1))+1);
		}

		return newData;
	}

	private long calculatetwoscomplement(long signedData, int bitLength)
	{
		long newData=signedData;
		if (signedData>=(1L<<(bitLength-1))) {
			newData=-((signedData^(long)(Math.pow(2, bitLength)-1))+1);
		}

		return newData;
	}
	
	protected int getSignalIndex(String signalName) {
		int iSignal=-1; //better to fail //used to be -1, putting to zero ensure it works eventhough it might be wrong SR30
		for (int i=0;i<mSignalNameArray.length;i++) {
			if (signalName==mSignalNameArray[i]) {
				iSignal=i;
			}
		}

		return iSignal;
	}

	
	/** Only for Bluetooth
	 * @param nC
	 * @param signalid
	 */
	protected void interpretdatapacketformat(int nC, byte[] signalid)
	{
		String [] signalNameArray=new String[MAX_NUMBER_OF_SIGNALS];
		String [] signalDataTypeArray=new String[MAX_NUMBER_OF_SIGNALS];
		/*signalNameArray[0]="TimeStamp";
		signalDataTypeArray[0]="u16";
		int packetSize=2; // Time stamp*/
		
		signalNameArray[0]="TimeStamp";
		int packetSize=mTimeStampPacketByteSize; // Time stamp
		if (mTimeStampPacketByteSize==2){
			signalDataTypeArray[0]="u16";
		} else if (mTimeStampPacketByteSize==3) {
			signalDataTypeArray[0]="u24";
		}
		
		
		int enabledSensors= 0x00;
		for (int i=0;i<nC;i++) {
			if ((byte)signalid[i]==(byte)0x00)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_SR30 || mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_LN_X;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_A_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.ACCEL_X;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_ACCEL);
				}
			}
			else if ((byte)signalid[i]==(byte)0x01)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_SR30 || mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2; 
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_A_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.ACCEL_Y;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_ACCEL);
				}
			}
			else if ((byte)signalid[i]==(byte)0x02)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_SR30 || mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_A_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.ACCEL_Z;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_ACCEL);
				}
			}
			else if ((byte)signalid[i]==(byte)0x03)
			{

				if (mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_X;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.BATTERY; //should be the battery but this will do for now
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_VBATT);	
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.GYRO_X;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GYRO);
				}
			}
			else if ((byte)signalid[i]==(byte)0x04)
			{

				if (mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_Y;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_X;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.GYRO_Y;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GYRO);
				}
			}
			else if ((byte)signalid[i]==(byte)0x05)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_Z;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.GYRO_Z;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GYRO);
				}
			}
			else if ((byte)signalid[i]==(byte)0x06)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.BATTERY; //should be the battery but this will do for now
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_VBATT);	
				} else if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.MAG_X;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_MAG);
				}


			}
			else if ((byte)signalid[i]==(byte)0x07)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_X;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else if(mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_X;
					signalDataTypeArray[i+1] = "i16r";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				} else {
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.MAG_Y;
					enabledSensors= (enabledSensors|SENSOR_MAG);
				}


			}
			else if ((byte)signalid[i]==(byte)0x08)
			{	
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else if(mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_Y;
					signalDataTypeArray[i+1] = "i16r";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				} else {
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.MAG_Z;
					enabledSensors= (enabledSensors|SENSOR_MAG);
				}

			}
			else if ((byte)signalid[i]==(byte)0x09)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z;
					signalDataTypeArray[i+1] = "i16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_D_ACCEL);
				} else if(mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_Z;
					signalDataTypeArray[i+1] = "i16r";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.ECG_RA_LL;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_ECG);
				}


			}
			else if ((byte)signalid[i]==(byte)0x0A)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_X;
					signalDataTypeArray[i+1] = "i16";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				} else if (mHardwareVersion==HW_ID.SHIMMER_3) {
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_X;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else {

					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.ECG_LA_LL;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_ECG);
				}
			}
			else if ((byte)signalid[i]==(byte)0x0B)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_Y;
					signalDataTypeArray[i+1] = "i16";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				}  else if (mHardwareVersion==HW_ID.SHIMMER_3) {
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_Y;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.GSR;
					signalDataTypeArray[i+1] = "u16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GSR);
				}
			}
			else if ((byte)signalid[i]==(byte)0x0C)
			{
				if(mHardwareVersion==HW_ID.SHIMMER_SR30){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.MAG_Z;
					signalDataTypeArray[i+1] = "i16";			
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_MAG);
				} else if (mHardwareVersion==HW_ID.SHIMMER_3) {
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GYRO_Z;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|Configuration.Shimmer3.SensorBitmap.SENSOR_GYRO);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.GSR_RES;
					signalDataTypeArray[i+1] = "u16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GSR);
				}
			}
			else if ((byte)signalid[i]==(byte)0x0D)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXT_EXP_A7;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXT_ADC_A7);
				} else{
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.EMG;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EMG);
				}
			}
			else if ((byte)signalid[i]==(byte)0x0E)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXT_EXP_A6;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXT_ADC_A6);
				} else{
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXP_BOARD_A0);
				}
			}
			else if ((byte)signalid[i]==(byte)0x0F)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXT_EXP_A15;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXT_ADC_A15);
				} else{
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXP_BOARD_A7);
				}
			}
			else if ((byte)signalid[i]==(byte)0x10)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.INT_EXP_A1;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_INT_ADC_A1);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_BRIDGE_AMP);
				}
			}

			else if ((byte)signalid[i]==(byte)0x11)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.INT_EXP_A12;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_INT_ADC_A12);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_BRIDGE_AMP);
				}
			}
			else if ((byte)signalid[i]==(byte)0x12)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.INT_EXP_A13;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_INT_ADC_A13);
				} else {
					signalNameArray[i+1]=Shimmer2.ObjectClusterSensorName.HEART_RATE;
					if (mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1){
						signalDataTypeArray[i+1] = "u8";
						packetSize=packetSize+1;
					} else {
						signalDataTypeArray[i+1] = "u16"; 
						packetSize=packetSize+2;
					}
					enabledSensors= (enabledSensors|SENSOR_HEART);
				}
			}
			else if ((byte)signalid[i]==(byte)0x13)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.INT_EXP_A14;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_INT_ADC_A14);
				}
			}
			else if ((byte)signalid[i]==(byte)0x1A){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180;
					signalDataTypeArray[i+1] = "u16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_BMP180);
				}
			}
			else if ((byte)signalid[i]==(byte)0x1B){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180;
					signalDataTypeArray[i+1] = "u24r";
					packetSize=packetSize+3;
					enabledSensors= (enabledSensors|SENSOR_BMP180);
				}
			}
			else if ((byte)signalid[i]==(byte)0x1C){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.GSR;
					signalDataTypeArray[i+1] = "u16";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_GSR);
				}
			}
			else if ((byte)signalid[i]==(byte)0x1D){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG1_STATUS;
					signalDataTypeArray[i+1] = "u8";
					packetSize=packetSize+1;

				}
			}
			else if ((byte)signalid[i]==(byte)0x1E){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT;
					signalDataTypeArray[i+1] = "i24r";
					packetSize=packetSize+3;
					enabledSensors= (enabledSensors|SENSOR_EXG1_24BIT);
				}
			}
			else if ((byte)signalid[i]==(byte)0x1F){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT;
					signalDataTypeArray[i+1] = "i24r";
					packetSize=packetSize+3;
					enabledSensors= (enabledSensors|SENSOR_EXG1_24BIT);
				}
			}

			else if ((byte)signalid[i]==(byte)0x20){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG2_STATUS;
					signalDataTypeArray[i+1] = "u8";
					packetSize=packetSize+1;

				}
			}
			else if ((byte)signalid[i]==(byte)0x21){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT;
					signalDataTypeArray[i+1] = "i24r";
					packetSize=packetSize+3;
					enabledSensors= (enabledSensors|SENSOR_EXG2_24BIT);
				}
			}
			else if ((byte)signalid[i]==(byte)0x22){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT;
					signalDataTypeArray[i+1] = "i24r";
					packetSize=packetSize+3;
					enabledSensors= (enabledSensors|SENSOR_EXG2_24BIT);
				}
			}

			else if ((byte)signalid[i]==(byte)0x23){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXG1_16BIT);
				}
			}
			else if ((byte)signalid[i]==(byte)0x24){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXG1_16BIT);
				}
			}

			else if ((byte)signalid[i]==(byte)0x25){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXG2_16BIT);
				}
			}
			else if ((byte)signalid[i]==(byte)0x26){
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT;
					signalDataTypeArray[i+1] = "i16r";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_EXG2_16BIT);
				}
			}
			else if ((byte)signalid[i]==(byte)0x27)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_BRIDGE_AMP);
				} 
			}
			else if ((byte)signalid[i]==(byte)0x28)
			{
				if (mHardwareVersion==HW_ID.SHIMMER_3){
					signalNameArray[i+1]=Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW;
					signalDataTypeArray[i+1] = "u12";
					packetSize=packetSize+2;
					enabledSensors= (enabledSensors|SENSOR_BRIDGE_AMP);
				} 
			}
			else
			{
				signalNameArray[i+1]=Byte.toString(signalid[i]);
				signalDataTypeArray[i+1] = "u12";
				packetSize=packetSize+2;
			}

		}
		mSignalNameArray=signalNameArray;
		mSignalDataTypeArray=signalDataTypeArray;
		mPacketSize=packetSize;
	}

	protected void retrievepressurecalibrationparametersfrompacket(byte[] pressureResoRes, int packetType)
	{
		if (packetType == BMP180_CALIBRATION_COEFFICIENTS_RESPONSE){
			AC1 = calculatetwoscomplement((int)((int)(pressureResoRes[1] & 0xFF) + ((int)(pressureResoRes[0] & 0xFF) << 8)),16);
			AC2 = calculatetwoscomplement((int)((int)(pressureResoRes[3] & 0xFF) + ((int)(pressureResoRes[2] & 0xFF) << 8)),16);
			AC3 = calculatetwoscomplement((int)((int)(pressureResoRes[5] & 0xFF) + ((int)(pressureResoRes[4] & 0xFF) << 8)),16);
			AC4 = (int)((int)(pressureResoRes[7] & 0xFF) + ((int)(pressureResoRes[6] & 0xFF) << 8));
			AC5 = (int)((int)(pressureResoRes[9] & 0xFF) + ((int)(pressureResoRes[8] & 0xFF) << 8));
			AC6 = (int)((int)(pressureResoRes[11] & 0xFF) + ((int)(pressureResoRes[10] & 0xFF) << 8));
			B1 = calculatetwoscomplement((int)((int)(pressureResoRes[13] & 0xFF) + ((int)(pressureResoRes[12] & 0xFF) << 8)),16);
			B2 = calculatetwoscomplement((int)((int)(pressureResoRes[15] & 0xFF) + ((int)(pressureResoRes[14] & 0xFF) << 8)),16);
			MB = calculatetwoscomplement((int)((int)(pressureResoRes[17] & 0xFF) + ((int)(pressureResoRes[16] & 0xFF) << 8)),16);
			MC = calculatetwoscomplement((int)((int)(pressureResoRes[19] & 0xFF) + ((int)(pressureResoRes[18] & 0xFF) << 8)),16);
			MD = calculatetwoscomplement((int)((int)(pressureResoRes[21] & 0xFF) + ((int)(pressureResoRes[20] & 0xFF) << 8)),16);
		}



	}
	
	/**
	 * Should only be used when Shimmer is Connected and Initialized
	 */
	public static BiMap<String,String> generateBiMapSensorIDtoSensorName(int shimmerVersion){
		BiMap<String, String> sensorBitmaptoName =null;  
		if (shimmerVersion != HW_ID.SHIMMER_2R){
			final Map<String, String> tempSensorBMtoName = new HashMap<String, String>();  
			tempSensorBMtoName.put(Integer.toString(SENSOR_GYRO), "Gyroscope");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_MAG), "Magnetometer");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_GSR), "GSR");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A7), "Exp Board A7");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A0), "Exp Board A0");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD), "Exp Board");
			tempSensorBMtoName.put(Integer.toString(SENSOR_BRIDGE_AMP), "Bridge Amplifier");
			tempSensorBMtoName.put(Integer.toString(SENSOR_HEART), "Heart Rate");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_BATT), "Battery Voltage");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A7), "External ADC A7");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A6), "External ADC A6");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A15), "External ADC A15");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A1), "Internal ADC A1");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A12), "Internal ADC A12");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A13), "Internal ADC A13");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A14), "Internal ADC A14");
			tempSensorBMtoName.put(Integer.toString(SENSOR_BMP180), "Pressure");
			tempSensorBMtoName.put(Integer.toString(SENSOR_ACCEL), "Low Noise Accelerometer");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_DACCEL), "Wide Range Accelerometer");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXG1_24BIT), "EXG1");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXG2_24BIT), "EXG2");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXG1_16BIT), "EXG1 16Bit");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXG2_16BIT), "EXG2 16Bit");
			sensorBitmaptoName = ImmutableBiMap.copyOf(Collections.unmodifiableMap(tempSensorBMtoName));
		} else {
			final Map<String, String> tempSensorBMtoName = new HashMap<String, String>();  
			tempSensorBMtoName.put(Integer.toString(SENSOR_ACCEL), "Accelerometer");
			tempSensorBMtoName.put(Integer.toString(SENSOR_GYRO), "Gyroscope");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_MAG), "Magnetometer");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EMG), "EMG");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_ECG), "ECG");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_GSR), "GSR");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A7), "Exp Board A7");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A0), "Exp Board A0");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD), "Exp Board");
			tempSensorBMtoName.put(Integer.toString(SENSOR_BRIDGE_AMP), "Bridge Amplifier");
			tempSensorBMtoName.put(Integer.toString(SENSOR_HEART), "Heart Rate");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_BATT), "Battery Voltage");
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A7), "External ADC A7");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A6), "External ADC A6");  
			tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A15), "External ADC A15");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A1), "Internal ADC A1");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A12), "Internal ADC A12");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A13), "Internal ADC A13");
			tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A14), "Internal ADC A14");
			sensorBitmaptoName = ImmutableBiMap.copyOf(Collections.unmodifiableMap(tempSensorBMtoName));
		}
		return sensorBitmaptoName;
	}

	/**
	 * Should only be used when Shimmer is Connected and Initialized
	 */
	public void generateBiMapSensorIDtoSensorName(){
		if (mHardwareVersion != -1){
			if (mHardwareVersion != HW_ID.SHIMMER_2R){
				final Map<String, String> tempSensorBMtoName = new HashMap<String, String>();  
				tempSensorBMtoName.put(Integer.toString(SENSOR_BMP180), "Pressure");
				tempSensorBMtoName.put(Integer.toString(SENSOR_GYRO), "Gyroscope");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_MAG), "Magnetometer");  
				tempSensorBMtoName.put(Integer.toString(SHIMMER3_SENSOR_ECG), "ECG");  
				tempSensorBMtoName.put(Integer.toString(SHIMMER3_SENSOR_EMG), "EMG");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_GSR), "GSR");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A7), "Exp Board A7");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A0), "Exp Board A0");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD), "Exp Board");
				tempSensorBMtoName.put(Integer.toString(SENSOR_BRIDGE_AMP), "Bridge Amplifier");
				tempSensorBMtoName.put(Integer.toString(SENSOR_HEART), "Heart Rate");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_BATT), "Battery Voltage");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A7), "External ADC A7");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A6), "External ADC A6");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A15), "External ADC A15");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A1), "Internal ADC A1");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A12), "Internal ADC A12");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A13), "Internal ADC A13");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A14), "Internal ADC A14");
				tempSensorBMtoName.put(Integer.toString(SENSOR_ACCEL), "Low Noise Accelerometer");
				tempSensorBMtoName.put(Integer.toString(SENSOR_DACCEL), "Wide Range Accelerometer");
				mSensorBitmaptoName = ImmutableBiMap.copyOf(Collections.unmodifiableMap(tempSensorBMtoName));


			} else {
				final Map<String, String> tempSensorBMtoName = new HashMap<String, String>();  
				tempSensorBMtoName.put(Integer.toString(SENSOR_ACCEL), "Accelerometer");
				tempSensorBMtoName.put(Integer.toString(SENSOR_GYRO), "Gyroscope");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_MAG), "Magnetometer");
				tempSensorBMtoName.put(Integer.toString(SENSOR_ECG), "ECG");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EMG), "EMG");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_GSR), "GSR");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A7), "Exp Board A7");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD_A0), "Exp Board A0");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXP_BOARD), "Exp Board");
				tempSensorBMtoName.put(Integer.toString(SENSOR_BRIDGE_AMP), "Bridge Amplifier");
				tempSensorBMtoName.put(Integer.toString(SENSOR_HEART), "Heart Rate");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_BATT), "Battery Voltage");
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A7), "External ADC A7");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A6), "External ADC A6");  
				tempSensorBMtoName.put(Integer.toString(SENSOR_EXT_ADC_A15), "External ADC A15");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A1), "Internal ADC A1");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A12), "Internal ADC A12");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A13), "Internal ADC A13");
				tempSensorBMtoName.put(Integer.toString(SENSOR_INT_ADC_A14), "Internal ADC A14");
				mSensorBitmaptoName = ImmutableBiMap.copyOf(Collections.unmodifiableMap(tempSensorBMtoName));
			}

		}
	}



	public String[] getListofEnabledSensorSignals(){
		List<String> listofSignals = new ArrayList<String>();
		String[] enabledSignals; 
		if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){
			listofSignals.add("Timestamp");
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0){
				listofSignals.add(Shimmer2.ObjectClusterSensorName.ACCEL_X);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.ACCEL_Y);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.ACCEL_Z);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.GYRO_X);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.GYRO_Y);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.GYRO_Z);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.MAG_X);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.MAG_Y);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.MAG_Z);
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_GSR) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.GSR);
				//listofSignals.add(Shimmer2.ObjectClusterSensorName.GSR_RES);
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_ECG) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.ECG_RA_LL);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.ECG_LA_LL);
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_EMG) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.EMG);
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW);
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_HEART) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.HEART_RATE);
			}
			if ((((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0) && getPMux() == 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0);
			}
			if ((((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0 && getPMux() == 0)) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7);
			}
			if (((mEnabledSensors & 0xFFFF) & SENSOR_BATT) > 0) {
				listofSignals.add(Shimmer2.ObjectClusterSensorName.BATTERY);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.REG);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				listofSignals.add(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_A);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_X);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Y);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Z);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				listofSignals.add(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_W);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_X);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y);
				listofSignals.add(Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z);
			}

		} else {
			listofSignals.add("Timestamp");
			if ((mEnabledSensors & SENSOR_ACCEL) >0){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_LN_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z);
			}
			if ((mEnabledSensors& SENSOR_DACCEL) >0){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_WR_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.GYRO_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.GYRO_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.GYRO_Z);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.MAG_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.MAG_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.MAG_Z);
			} 
			if (((mEnabledSensors & 0xFFFF) & SENSOR_BATT) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.BATTERY);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A15) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.EXT_EXP_A15);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A7) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.EXT_EXP_A7);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A6) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.EXT_EXP_A6);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A1) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.INT_EXP_A1);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A12) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.INT_EXP_A12);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A13) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.INT_EXP_A13);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A14) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.INT_EXP_A14);
			}
			if ((((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 || ((mEnabledSensors & 0xFFFF)& SENSOR_DACCEL) > 0)&& ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_A);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Z);
			}
			if ((((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 || ((mEnabledSensors & 0xFFFF)& SENSOR_DACCEL) > 0) && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_W);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_X);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z);
			}
			if ((mEnabledSensors & SENSOR_BMP180)>0){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180);
			}
			if ((mEnabledSensors & SENSOR_GSR)>0){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.GSR);
			}
			if (((mEnabledSensors & SENSOR_EXG1_24BIT)>0)|| ((mEnabledSensors & SENSOR_EXG1_16BIT)>0)){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG1_STATUS);
			}
			if (((mEnabledSensors & SENSOR_EXG2_24BIT)>0)|| ((mEnabledSensors & SENSOR_EXG2_16BIT)>0)){
				listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_STATUS);
			}
			if ((mEnabledSensors & SENSOR_EXG1_24BIT)>0){
				if (isEXGUsingDefaultECGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT);
				} else {
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT);
				}

			}
			if ((mEnabledSensors & SENSOR_EXG2_24BIT)>0){
				if (isEXGUsingDefaultECGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT);
				} else {
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT);
				}
			}
			if ((mEnabledSensors & SENSOR_EXG1_16BIT)>0){
				if (isEXGUsingDefaultECGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT);
				} else {
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT);
				}
			}
			if ((mEnabledSensors & SENSOR_EXG2_16BIT)>0){
				if (isEXGUsingDefaultECGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT);
				} else {
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT);
					listofSignals.add(Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT);
				}
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0) {
				listofSignals.add(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH);
				listofSignals.add(Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW);
			}
		}
		enabledSignals = listofSignals.toArray(new String[listofSignals.size()]);
		return enabledSignals;
	}	

	protected void retrievebiophysicalcalibrationparametersfrompacket(byte[] bufferCalibrationParameters, int packetType)
	{
		if (packetType == ECG_CALIBRATION_RESPONSE){
			if (bufferCalibrationParameters[0]==-1 && bufferCalibrationParameters[1] == -1 && bufferCalibrationParameters[2] == -1 && bufferCalibrationParameters[3]==-1){
				mDefaultCalibrationParametersECG = true;
			} else {
				mDefaultCalibrationParametersECG = false;
				OffsetECGLALL=(double)((bufferCalibrationParameters[0]&0xFF)<<8)+(bufferCalibrationParameters[1]&0xFF);
				GainECGLALL=(double)((bufferCalibrationParameters[2]&0xFF)<<8)+(bufferCalibrationParameters[3]&0xFF);
				OffsetECGRALL=(double)((bufferCalibrationParameters[4]&0xFF)<<8)+(bufferCalibrationParameters[5]&0xFF);
				GainECGRALL=(double)((bufferCalibrationParameters[6]&0xFF)<<8)+(bufferCalibrationParameters[7]&0xFF);
			}	
		}

		if (packetType == EMG_CALIBRATION_RESPONSE){

			if (bufferCalibrationParameters[0]==-1 && bufferCalibrationParameters[1] == -1 && bufferCalibrationParameters[2] == -1 && bufferCalibrationParameters[3]==-1){
				mDefaultCalibrationParametersEMG = true;
			} else {
				mDefaultCalibrationParametersEMG = false;
				OffsetEMG=(double)((bufferCalibrationParameters[0]&0xFF)<<8)+(bufferCalibrationParameters[1]&0xFF);
				GainEMG=(double)((bufferCalibrationParameters[2]&0xFF)<<8)+(bufferCalibrationParameters[3]&0xFF);
			}
		}

	}

	protected void retrievekinematiccalibrationparametersfrompacket(byte[] bufferCalibrationParameters, int packetType)
	{
		if (packetType==ACCEL_CALIBRATION_RESPONSE || packetType==LSM303DLHC_ACCEL_CALIBRATION_RESPONSE || packetType==GYRO_CALIBRATION_RESPONSE || packetType==MAG_CALIBRATION_RESPONSE ){
			String[] dataType={"i16","i16","i16","i16","i16","i16","i8","i8","i8","i8","i8","i8","i8","i8","i8"}; 
			int[] formattedPacket=formatdatapacketreverse(bufferCalibrationParameters,dataType); // using the datatype the calibration parameters are converted
			double[] AM=new double[9];
			for (int i=0;i<9;i++)
			{
				AM[i]=((double)formattedPacket[6+i])/100;
			}

			double[][] AlignmentMatrix = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
			double[][] SensitivityMatrix = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
			double[][] OffsetVector = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};


			if (packetType==ACCEL_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]!=-1) {   //used to be 65535 but changed to -1 as we are now using i16
				mDefaultCalibrationParametersAccel = false;
				mAlignmentMatrixAnalogAccel = AlignmentMatrix;
				mOffsetVectorAnalogAccel = OffsetVector;
				mSensitivityMatrixAnalogAccel = SensitivityMatrix;
			} else if(packetType==ACCEL_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]==-1){
				mDefaultCalibrationParametersAccel = true;
				if (mHardwareVersion!=3){
					mAlignmentMatrixAnalogAccel = AlignmentMatrixAccelShimmer2;
					mOffsetVectorAnalogAccel = OffsetVectorAccelShimmer2;
					if (getAccelRange()==0){
						mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel1p5gShimmer2; 
					} else if (getAccelRange()==1){
						mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel2gShimmer2; 
					} else if (getAccelRange()==2){
						mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel4gShimmer2; 
					} else if (getAccelRange()==3){
						mSensitivityMatrixAnalogAccel = SensitivityMatrixAccel6gShimmer2; 
					}
				} else {
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

			if (packetType==LSM303DLHC_ACCEL_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]!=-1) {   //used to be 65535 but changed to -1 as we are now using i16
				mDefaultCalibrationParametersDigitalAccel = false;
				mAlignmentMatrixWRAccel = AlignmentMatrix;
				mOffsetVectorWRAccel = OffsetVector;
				mSensitivityMatrixWRAccel = SensitivityMatrix;
			} else if(packetType==LSM303DLHC_ACCEL_CALIBRATION_RESPONSE  && SensitivityMatrix[0][0]==-1){
				mDefaultCalibrationParametersDigitalAccel = true;
				if (getAccelRange()==0){
					mSensitivityMatrixWRAccel = SensitivityMatrixWideRangeAccel2gShimmer3;
					mAlignmentMatrixWRAccel = AlignmentMatrixWideRangeAccelShimmer3;
					mOffsetVectorWRAccel = OffsetVectorWideRangeAccelShimmer3;
				} else if (getAccelRange()==1){
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
				}
			}
			if (packetType==GYRO_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]!=-1) {
				mDefaultCalibrationParametersGyro = false;
				mAlignmentMatrixGyroscope = AlignmentMatrix;
				mOffsetVectorGyroscope = OffsetVector;
				mSensitivityMatrixGyroscope = SensitivityMatrix;
				mSensitivityMatrixGyroscope[0][0] = mSensitivityMatrixGyroscope[0][0]/100;
				mSensitivityMatrixGyroscope[1][1] = mSensitivityMatrixGyroscope[1][1]/100;
				mSensitivityMatrixGyroscope[2][2] = mSensitivityMatrixGyroscope[2][2]/100;

			} else if(packetType==GYRO_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]==-1){
				mDefaultCalibrationParametersGyro = true;
				if (mHardwareVersion!=3){
					mAlignmentMatrixGyroscope = AlignmentMatrixGyroShimmer2;
					mOffsetVectorGyroscope = OffsetVectorGyroShimmer2;
					mSensitivityMatrixGyroscope = SensitivityMatrixGyroShimmer2;	
				} else {
					if (mGyroRange==0){
						mSensitivityMatrixGyroscope = SensitivityMatrixGyro250dpsShimmer3;

					} else if (mGyroRange==1){
						mSensitivityMatrixGyroscope = SensitivityMatrixGyro500dpsShimmer3;

					} else if (mGyroRange==2){
						mSensitivityMatrixGyroscope = SensitivityMatrixGyro1000dpsShimmer3;

					} else if (mGyroRange==3){
						mSensitivityMatrixGyroscope = SensitivityMatrixGyro2000dpsShimmer3;
					}
					mAlignmentMatrixGyroscope = AlignmentMatrixGyroShimmer3;
					mOffsetVectorGyroscope = OffsetVectorGyroShimmer3;
				}
			} 
			if (packetType==MAG_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]!=-1) {
				mDefaultCalibrationParametersMag = false;
				mAlignmentMatrixMagnetometer = AlignmentMatrix;
				mOffsetVectorMagnetometer = OffsetVector;
				mSensitivityMatrixMagnetometer = SensitivityMatrix;

			} else if(packetType==MAG_CALIBRATION_RESPONSE && SensitivityMatrix[0][0]==-1){
				mDefaultCalibrationParametersMag = true;
				if (mHardwareVersion!=3){
					mAlignmentMatrixMagnetometer = AlignmentMatrixMagShimmer2;
					mOffsetVectorMagnetometer = OffsetVectorMagShimmer2;
					if (mMagRange==0){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag0p8GaShimmer2;
					} else if (mMagRange==1){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag1p3GaShimmer2;
					} else if (mMagRange==2){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag1p9GaShimmer2;
					} else if (mMagRange==3){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag2p5GaShimmer2;
					} else if (mMagRange==4){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag4p0GaShimmer2;
					} else if (mMagRange==5){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag4p7GaShimmer2;
					} else if (mMagRange==6){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag5p6GaShimmer2;
					} else if (mMagRange==7){
						mSensitivityMatrixMagnetometer = SensitivityMatrixMag8p1GaShimmer2;
					}
				} else {
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
		}
	}

	private double[][] matrixinverse3x3(double[][] data) {
		double a,b,c,d,e,f,g,h,i;
		a=data[0][0];
		b=data[0][1];
		c=data[0][2];
		d=data[1][0];
		e=data[1][1];
		f=data[1][2];
		g=data[2][0];
		h=data[2][1];
		i=data[2][2];
		//
		double deter=a*e*i+b*f*g+c*d*h-c*e*g-b*d*i-a*f*h;
		double[][] answer=new double[3][3];
		answer[0][0]=(1/deter)*(e*i-f*h);

		answer[0][1]=(1/deter)*(c*h-b*i);
		answer[0][2]=(1/deter)*(b*f-c*e);
		answer[1][0]=(1/deter)*(f*g-d*i);
		answer[1][1]=(1/deter)*(a*i-c*g);
		answer[1][2]=(1/deter)*(c*d-a*f);
		answer[2][0]=(1/deter)*(d*h-e*g);
		answer[2][1]=(1/deter)*(g*b-a*h);
		answer[2][2]=(1/deter)*(a*e-b*d);
		return answer;
	}
	private double[][] matrixminus(double[][] a ,double[][] b) {
		int aRows = a.length,
				aColumns = a[0].length,
				bRows = b.length,
				bColumns = b[0].length;
		if (( aColumns != bColumns )&&( aRows != bRows )) {
			throw new IllegalArgumentException(" Matrix did not match");
		}
		double[][] resultant = new double[aRows][bColumns];
		for(int i = 0; i < aRows; i++) { // aRow
			for(int k = 0; k < aColumns; k++) { // aColumn

				resultant[i][k]=a[i][k]-b[i][k];

			}
		}
		return resultant;
	}

	private double[][] matrixmultiplication(double[][] a,double[][] b) {

		int aRows = a.length,
				aColumns = a[0].length,
				bRows = b.length,
				bColumns = b[0].length;

		if ( aColumns != bRows ) {
			throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
		}

		double[][] resultant = new double[aRows][bColumns];

		for(int i = 0; i < aRows; i++) { // aRow
			for(int j = 0; j < bColumns; j++) { // bColumn
				for(int k = 0; k < aColumns; k++) { // aColumn
					resultant[i][j] += a[i][k] * b[k][j];
				}
			}
		}

		return resultant;
	}

	protected double calibrateTimeStamp(double timeStamp){
		//first convert to continuous time stamp
		double calibratedTimeStamp=0;
		if (mLastReceivedTimeStamp>(timeStamp+(mTimeStampPacketRawMaxValue*mCurrentTimeStampCycle))){ 
			mCurrentTimeStampCycle=mCurrentTimeStampCycle+1;
		}

		mLastReceivedTimeStamp=(timeStamp+(mTimeStampPacketRawMaxValue*mCurrentTimeStampCycle));
		calibratedTimeStamp=mLastReceivedTimeStamp/32768*1000;   // to convert into mS
		if (mFirstTimeCalTime){
			mFirstTimeCalTime=false;
			mCalTimeStart = calibratedTimeStamp;
		}
		if (mLastReceivedCalibratedTimeStamp!=-1){
			double timeDifference=calibratedTimeStamp-mLastReceivedCalibratedTimeStamp;
			double expectedTimeDifference = (1/mShimmerSamplingRate)*1000;
			double expectedTimeDifferenceLimit = expectedTimeDifference + (expectedTimeDifference*0.1); 
			//if (timeDifference>(1/(mShimmerSamplingRate-1))*1000){
			if (timeDifference>expectedTimeDifferenceLimit){
//				mPacketLossCount=mPacketLossCount+1;
				mPacketLossCount+= (long) (timeDifference/expectedTimeDifferenceLimit);
				Long mTotalNumberofPackets=(long) ((calibratedTimeStamp-mCalTimeStart)/(1/mShimmerSamplingRate*1000));

				mPacketReceptionRate = (double)((mTotalNumberofPackets-mPacketLossCount)/(double)mTotalNumberofPackets)*100;
				sendStatusMsgPacketLossDetected();
			}
		}	
		mLastReceivedCalibratedTimeStamp=calibratedTimeStamp;
		return calibratedTimeStamp;
	}

	//protected abstract void sendStatusMsgPacketLossDetected();
	protected void sendStatusMsgPacketLossDetected() {
	}

	protected double[] calibrateInertialSensorData(double[] data, double[][] AM, double[][] SM, double[][] OV) {
		/*  Based on the theory outlined by Ferraris F, Grimaldi U, and Parvis M.  
           in "Procedure for effortless in-field calibration of three-axis rate gyros and accelerometers" Sens. Mater. 1995; 7: 311-30.            
           C = [R^(-1)] .[K^(-1)] .([U]-[B])
			where.....
			[C] -> [3 x n] Calibrated Data Matrix 
			[U] -> [3 x n] Uncalibrated Data Matrix
			[B] ->  [3 x n] Replicated Sensor Offset Vector Matrix 
			[R^(-1)] -> [3 x 3] Inverse Alignment Matrix
			[K^(-1)] -> [3 x 3] Inverse Sensitivity Matrix
			n = Number of Samples
		 */
		double [][] data2d=new double [3][1];
		data2d[0][0]=data[0];
		data2d[1][0]=data[1];
		data2d[2][0]=data[2];
		data2d= matrixmultiplication(matrixmultiplication(matrixinverse3x3(AM),matrixinverse3x3(SM)),matrixminus(data2d,OV));
		double[] ansdata=new double[3];
		ansdata[0]=data2d[0][0];
		ansdata[1]=data2d[1][0];
		ansdata[2]=data2d[2][0];
		return ansdata;
	}

	protected double[] calibratePressureSensorData(double UP, double UT){
		double X1 = (UT - AC6) * AC5 / 32768;
		double X2 = (MC * 2048 / (X1 + MD));
		double B5 = X1 + X2;
		double T = (B5 + 8) / 16;

		double B6 = B5 - 4000;
		X1 = (B2 * (Math.pow(B6,2)/ 4096)) / 2048;
		X2 = AC2 * B6 / 2048;
		double X3 = X1 + X2;
		double B3 = (((AC1 * 4 + X3)*(1<<mPressureResolution) + 2)) / 4;
		X1 = AC3 * B6 / 8192;
		X2 = (B1 * (Math.pow(B6,2)/ 4096)) / 65536;
		X3 = ((X1 + X2) + 2) / 4;
		double B4 = AC4 * (X3 + 32768) / 32768;
		double B7 = (UP - B3) * (50000>>mPressureResolution);
		double p=0;
		if (B7 < 2147483648L ){ //0x80000000
			p = (B7 * 2) / B4;
		}
		else{
			p = (B7 / B4) * 2;
		}
		X1 = ((p / 256.0) * (p / 256.0) * 3038) / 65536;
		X2 = (-7357 * p) / 65536;
		p = p +( (X1 + X2 + 3791) / 16);

		double[] caldata = new double[2];
		caldata[0]=p;
		caldata[1]=T/10;
		return caldata;
	}


	protected double calibrateU12AdcValue(double uncalibratedData,double offset,double vRefP,double gain){
		double calibratedData=(uncalibratedData-offset)*(((vRefP*1000)/gain)/4095);
		return calibratedData;
	}

	protected double calibrateGsrData(double gsrUncalibratedData,double p1, double p2){
		gsrUncalibratedData = (double)((int)gsrUncalibratedData & 4095); 
		//the following polynomial is deprecated and has been replaced with a more accurate linear one, see GSR user guide for further details
		//double gsrCalibratedData = (p1*Math.pow(gsrUncalibratedData,4)+p2*Math.pow(gsrUncalibratedData,3)+p3*Math.pow(gsrUncalibratedData,2)+p4*gsrUncalibratedData+p5)/1000;
		//the following is the new linear method see user GSR user guide for further details
		double gsrCalibratedData = (1/((p1*gsrUncalibratedData)+p2)*1000); //kohms 
		return gsrCalibratedData;  
	}

	public double getSamplingRate(){
		return mShimmerSamplingRate;
	}

	public int getAccelRange(){
		return mAccelRange;
	}

	public int getPressureResolution(){
		return mPressureResolution;
	}

	public int getMagRange(){
		return mMagRange;
	}

	public int getGyroRange(){
		return mGyroRange;
	}

	public int getGSRRange(){
		return mGSRRange;
	}

	public int getInternalExpPower(){
		return mInternalExpPower;
	}

	public int getPMux(){
		if ((mConfigByte0 & (byte)64)!=0) {
			//then set ConfigByte0 at bit position 7
			return 1;
		} else{
			return 0;
		}
	}

	protected ObjectCluster callAdditionalServices(ObjectCluster objectCluster) {
		return objectCluster;
	}

	protected void interpretInqResponse(byte[] bufferInquiry){
		if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){

			mPacketSize = mTimeStampPacketByteSize+bufferInquiry[3]*2; 
			mShimmerSamplingRate = (double)1024/bufferInquiry[0];
			if (mLSM303MagRate==3 && mShimmerSamplingRate>10){
				mLowPowerMag = true;
			}
			mAccelRange = bufferInquiry[1];
			mConfigByte0 = bufferInquiry[2] & 0xFF; //convert the byte to unsigned integer
			mNChannels = bufferInquiry[3];
			mBufferSize = bufferInquiry[4];
			byte[] signalIdArray = new byte[mNChannels];
			System.arraycopy(bufferInquiry, 5, signalIdArray, 0, mNChannels);
			updateEnabledSensorsFromChannels(signalIdArray);
			interpretdatapacketformat(mNChannels,signalIdArray);
			mInquiryResponseBytes = new byte[5+mNChannels];
			System.arraycopy(bufferInquiry, 0, mInquiryResponseBytes , 0, mInquiryResponseBytes.length);
		} else if (mHardwareVersion==HW_ID.SHIMMER_3) {
			mPacketSize = mTimeStampPacketByteSize+bufferInquiry[6]*2; 
			mShimmerSamplingRate = (32768/(double)((int)(bufferInquiry[0] & 0xFF) + ((int)(bufferInquiry[1] & 0xFF) << 8)));
			mNChannels = bufferInquiry[6];
			mBufferSize = bufferInquiry[7];
			mConfigByte0 = ((long)(bufferInquiry[2] & 0xFF) +((long)(bufferInquiry[3] & 0xFF) << 8)+((long)(bufferInquiry[4] & 0xFF) << 16) +((long)(bufferInquiry[5] & 0xFF) << 24));
			mAccelRange = ((int)(mConfigByte0 & 0xC))>>2;
			mGyroRange = ((int)(mConfigByte0 & 196608))>>16;
			mMagRange = ((int)(mConfigByte0 & 14680064))>>21;
			mLSM303DigitalAccelRate = ((int)(mConfigByte0 & 0xF0))>>4;
			mMPU9150GyroAccelRate = ((int)(mConfigByte0 & 65280))>>8;
			mLSM303MagRate = ((int)(mConfigByte0 & 1835008))>>18; 
			mPressureResolution = (((int)(mConfigByte0 >>28)) & 3);
			mGSRRange  = (((int)(mConfigByte0 >>25)) & 7);
			mInternalExpPower = (((int)(mConfigByte0 >>24)) & 1);
			mInquiryResponseBytes = new byte[8+mNChannels];
			System.arraycopy(bufferInquiry, 0, mInquiryResponseBytes , 0, mInquiryResponseBytes.length);
			if ((mLSM303DigitalAccelRate==2 && mShimmerSamplingRate>10)){
				mLowPowerAccelWR = true;
			}
			if ((mMPU9150GyroAccelRate==0xFF && mShimmerSamplingRate>10)){
				mLowPowerGyro = true;
			}
			if ((mLSM303MagRate==4 && mShimmerSamplingRate>10)){
				mLowPowerMag = true;
			}
			byte[] signalIdArray = new byte[mNChannels];
			System.arraycopy(bufferInquiry, 8, signalIdArray, 0, mNChannels);
			updateEnabledSensorsFromChannels(signalIdArray);
			interpretdatapacketformat(mNChannels,signalIdArray);
		} else if (mHardwareVersion==HW_ID.SHIMMER_SR30) {
			mPacketSize = mTimeStampPacketByteSize+bufferInquiry[2]*2; 
			mShimmerSamplingRate = (double)1024/bufferInquiry[0];
			mAccelRange = bufferInquiry[1];
			mNChannels = bufferInquiry[2];
			mBufferSize = bufferInquiry[3];
			byte[] signalIdArray = new byte[mNChannels];
			System.arraycopy(bufferInquiry, 4, signalIdArray, 0, mNChannels); // this is 4 because there is no config byte
			interpretdatapacketformat(mNChannels,signalIdArray);

		}
	}


	protected void updateEnabledSensorsFromChannels(byte[] channels){
		// set the sensors value
		// crude way of getting this value, but allows for more customised firmware
		// to still work with this application
		// e.g. if any axis of the accelerometer is being transmitted, then it will
		// recognise that the accelerometer is being sampled
		int enabledSensors = 0;
		for (int i=0;i<channels.length;i++)
		{
			if (mHardwareVersion==HW_ID.SHIMMER_3){
				if (channels[i]==Configuration.Shimmer3.Channel.XAAccel || channels[i]==Configuration.Shimmer3.Channel.YAAccel || channels[i]==Configuration.Shimmer3.Channel.ZAAccel){
					enabledSensors = enabledSensors | SENSOR_ACCEL;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.XDAccel || channels[i]==Configuration.Shimmer3.Channel.YDAccel||channels[i]==Configuration.Shimmer3.Channel.ZDAccel){
					enabledSensors = enabledSensors | SENSOR_DACCEL;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.XGyro || channels[i]==Configuration.Shimmer3.Channel.YGyro||channels[i]==Configuration.Shimmer3.Channel.ZGyro){
					enabledSensors = enabledSensors | SENSOR_GYRO;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.XMag || channels[i]==Configuration.Shimmer3.Channel.YMag||channels[i]==Configuration.Shimmer3.Channel.ZMag){
					enabledSensors = enabledSensors | SENSOR_MAG;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.VBatt){
					enabledSensors = enabledSensors | SENSOR_BATT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.ExtAdc7){
					enabledSensors = enabledSensors | SENSOR_EXT_ADC_A7;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.ExtAdc6){
					enabledSensors = enabledSensors | SENSOR_EXT_ADC_A6;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.ExtAdc15){
					enabledSensors = enabledSensors | SENSOR_EXT_ADC_A15;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.IntAdc1){
					enabledSensors = enabledSensors | SENSOR_INT_ADC_A1;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.IntAdc12){
					enabledSensors = enabledSensors | SENSOR_INT_ADC_A12;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.IntAdc13){
					enabledSensors = enabledSensors | SENSOR_INT_ADC_A13;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.IntAdc14){
					enabledSensors = enabledSensors | SENSOR_INT_ADC_A14;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.Pressure){
					enabledSensors = enabledSensors | SENSOR_BMP180;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.Temperature){
					enabledSensors = enabledSensors | SENSOR_BMP180;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.GsrRaw){
					enabledSensors = enabledSensors | SENSOR_GSR;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_1_STATUS){
					//enabledSensors = enabledSensors | SENSOR_EXG1_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_1_CH1_24BIT){
					enabledSensors = enabledSensors | SENSOR_EXG1_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_1_CH2_24BIT){
					enabledSensors = enabledSensors | SENSOR_EXG1_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_1_CH1_16BIT){
					enabledSensors = enabledSensors | SENSOR_EXG1_16BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_1_CH2_16BIT){
					enabledSensors = enabledSensors | SENSOR_EXG1_16BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_2_STATUS){
					//enabledSensors = enabledSensors | SENSOR_EXG2_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_2_CH1_24BIT){
					enabledSensors = enabledSensors | SENSOR_EXG2_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_2_CH2_24BIT){
					enabledSensors = enabledSensors | SENSOR_EXG2_24BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_2_CH1_16BIT){
					enabledSensors = enabledSensors | SENSOR_EXG2_16BIT;
				}
				if (channels[i]==Configuration.Shimmer3.Channel.EXG_ADS1292R_2_CH2_16BIT){
					enabledSensors = enabledSensors | SENSOR_EXG2_16BIT;
				}
				if ((channels[i] == Configuration.Shimmer3.Channel.BridgeAmpHigh) || (channels[i] == Configuration.Shimmer3.Channel.BridgeAmpLow))
				{
					enabledSensors = enabledSensors | SENSOR_BRIDGE_AMP;
				}

			} else if(mHardwareVersion==HW_ID.SHIMMER_2R){
				if (channels[i]==Configuration.Shimmer2.Channel.XAccel || channels[i]==Configuration.Shimmer2.Channel.YAccel||channels[i]==Configuration.Shimmer2.Channel.ZAccel){
					enabledSensors = enabledSensors | SENSOR_ACCEL;
				}
				if (channels[i]==Configuration.Shimmer2.Channel.XGyro || channels[i]==Configuration.Shimmer2.Channel.YGyro ||channels[i]==Configuration.Shimmer2.Channel.ZGyro){
					enabledSensors = enabledSensors | SENSOR_GYRO;
				}
				if (channels[i]==Configuration.Shimmer2.Channel.XMag || channels[i]==Configuration.Shimmer2.Channel.XMag ||channels[i]==Configuration.Shimmer2.Channel.XMag){
					enabledSensors = enabledSensors | SENSOR_MAG;
				}        	
				if ((channels[i] == Configuration.Shimmer2.Channel.EcgLaLl) || (channels[i] == Configuration.Shimmer2.Channel.EcgRaLl))
				{
					enabledSensors = enabledSensors | SENSOR_ECG;
				}
				else if (channels[i] == Configuration.Shimmer2.Channel.Emg)
				{
					enabledSensors = enabledSensors | SENSOR_EMG;
				}
				else if (channels[i] == Configuration.Shimmer2.Channel.AnExA0 && getPMux()==0)
				{
					enabledSensors = enabledSensors | SENSOR_EXP_BOARD_A0;
				}
				else if (channels[i] == Configuration.Shimmer2.Channel.AnExA7 && getPMux()==0)
				{
					enabledSensors = enabledSensors | SENSOR_EXP_BOARD_A7;
				}
				else if ((channels[i] == Configuration.Shimmer2.Channel.BridgeAmpHigh) || (channels[i] == Configuration.Shimmer2.Channel.BridgeAmpLow))
				{
					enabledSensors = enabledSensors | SENSOR_BRIDGE_AMP;
				}
				else if ((channels[i] == Configuration.Shimmer2.Channel.GsrRaw) || (channels[i] == Configuration.Shimmer2.Channel.GsrRes))
				{
					enabledSensors = enabledSensors | SENSOR_GSR;
				}
				else if (channels[i] == Configuration.Shimmer2.Channel.HeartRate)
				{
					enabledSensors = enabledSensors | SENSOR_HEART;
				}   else if (channels[i] == Configuration.Shimmer2.Channel.AnExA0 && getPMux()==1)
				{
					enabledSensors = enabledSensors | SENSOR_BATT;
				}
				else if (channels[i] == Configuration.Shimmer2.Channel.AnExA7 && getPMux()==1)
				{
					enabledSensors = enabledSensors | SENSOR_BATT;
				}
			} 
		}
		mEnabledSensors=enabledSensors;	
	}
	public String getDeviceName(){
		return mMyName;
	}
	public String getBluetoothAddress(){
		return  mMyBluetoothAddress;
	}
	public void setDeviceName(String deviceName) {
		mMyName = deviceName;
	}
	public byte[] getRawInquiryResponse(){
		return mInquiryResponseBytes;
	}

	public byte[] getRawCalibrationParameters(){

		byte[] rawcal=new byte[1];
		if (mHardwareVersion==HW_ID.SHIMMER_3)
		{
			//Accel + Digi Accel + Gyro + Mag + Pressure
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			try {
				outputStream.write(5); // write the number of different calibration parameters
				outputStream.write( mAccelCalRawParams.length);
				outputStream.write( mAccelCalRawParams);
				outputStream.write( mDigiAccelCalRawParams.length);
				outputStream.write( mDigiAccelCalRawParams );
				outputStream.write( mGyroCalRawParams.length );
				outputStream.write( mGyroCalRawParams );
				outputStream.write( mMagCalRawParams.length );
				outputStream.write( mMagCalRawParams );
				outputStream.write( mPressureCalRawParams.length);
				outputStream.write( mPressureCalRawParams );
				rawcal = outputStream.toByteArray( );
			} catch (IOException e) {
				e.printStackTrace();
			}			

		} else if (mHardwareVersion==HW_ID.SHIMMER_2 ||mHardwareVersion==HW_ID.SHIMMER_2R)
		{
			//Accel + Digi Accel + Gyro + Mag + Pressure
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			try 
			{
				outputStream.write(5); // write the number of different calibration parameters
				outputStream.write( mAccelCalRawParams.length);
				outputStream.write( mAccelCalRawParams);
				outputStream.write( mGyroCalRawParams.length );
				outputStream.write( mGyroCalRawParams );
				outputStream.write( mMagCalRawParams.length );
				outputStream.write( mMagCalRawParams );
				outputStream.write( mECGCalRawParams.length );
				outputStream.write( mECGCalRawParams );
				outputStream.write( mEMGCalRawParams.length );
				outputStream.write( mEMGCalRawParams );
				rawcal = outputStream.toByteArray( );
			} catch (IOException e) {
				e.printStackTrace();
			}		

		} else {
			rawcal[0]=0;
		}
		return rawcal;

	}



	public List<String> getListofEnabledSensors(){
		List<String> listofSensors = new ArrayList<String>();
		if (mHardwareVersion==HW_ID.SHIMMER_3){
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0){
				listofSensors.add("Low Noise Accelerometer");
			}
			if ((mEnabledSensors & SENSOR_DACCEL) > 0){
				listofSensors.add("Wide Range Accelerometer");
			}
		} else {
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0){
				listofSensors.add("Accelerometer");
			}
		}
		if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
			listofSensors.add("Gyroscope");
		}
		if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
			listofSensors.add("Magnetometer");
		}
		if (((mEnabledSensors & 0xFF) & SENSOR_GSR) > 0) {
			listofSensors.add("GSR");
		}
		if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){
			if (((mEnabledSensors & 0xFF) & SENSOR_ECG) > 0) {
				listofSensors.add("ECG");
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_EMG) > 0) {
				listofSensors.add("EMG");
			}
		}
			if (((mEnabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0) {
				listofSensors.add("Bridge Amplifier");
			}
		
		if (((mEnabledSensors & 0xFF00) & SENSOR_HEART) > 0) {
			listofSensors.add("Heart Rate");
		}
		if (((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0 && (mEnabledSensors & SENSOR_BATT) == 0 && mHardwareVersion != HW_ID.SHIMMER_3) {
			listofSensors.add("ExpBoard A0");
		}
		if (((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0  && (mEnabledSensors & SENSOR_BATT) == 0 && mHardwareVersion != HW_ID.SHIMMER_3) {
			listofSensors.add("ExpBoard A7");
		}
		if ((mEnabledSensors & SENSOR_BATT) > 0) {
			listofSensors.add("Battery Voltage");
		}
		if (((mEnabledSensors & 0xFF) & SENSOR_EXT_ADC_A7) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("External ADC A7");
		}
		if (((mEnabledSensors & 0xFF) & SENSOR_EXT_ADC_A6) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("External ADC A6");
		}
		if (((mEnabledSensors & 0xFFFF) & SENSOR_EXT_ADC_A15) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("External ADC A15");
		}
		if (((mEnabledSensors & 0xFFFF) & SENSOR_INT_ADC_A1) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("Internal ADC A1");
		}
		if (((mEnabledSensors & 0xFFFF) & SENSOR_INT_ADC_A12) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("Internal ADC A12");
		}
		if (((mEnabledSensors & 0xFFFFFF) & SENSOR_INT_ADC_A13) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("Internal ADC A13");
		}
		if (((mEnabledSensors & 0xFFFFFF) & SENSOR_INT_ADC_A14) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("Internal ADC A14");
		}
		if ((mEnabledSensors & SENSOR_BMP180) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("Pressure");
		}
		if ((mEnabledSensors & SENSOR_EXG1_24BIT) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("EXG1");
		}
		if ((mEnabledSensors & SENSOR_EXG2_24BIT) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("EXG2");
		}
		if ((mEnabledSensors & SENSOR_EXG1_16BIT) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("EXG1 16Bit");
		}
		if ((mEnabledSensors & SENSOR_EXG2_16BIT) > 0  && mHardwareVersion == HW_ID.SHIMMER_3) {
			listofSensors.add("EXG2 16Bit");
		}

		return listofSensors;
	}
	
	/** Returns a list of string[] of the four properties. 1) Shimmer Name - 2) Property/Signal Name - 3) Format Name - 4) Unit Name
	 * @return list string array of properties
	 */
	public List<String[]> getListofEnabledSensorSignalsandFormats(){
		List<String[]> listofSignals = new ArrayList<String[]>();
		 
		if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R){
			String[] channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.TIMESTAMP,"CAL","mSecs"};
			listofSignals.add(channel);
			channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.TIMESTAMP,"RAW",NO_UNIT};
			listofSignals.add(channel);
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0){
				String unit = "m/(sec^2)";
				if (mDefaultCalibrationParametersAccel == true) {
					unit = "m/(sec^2)*";
				}
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ACCEL_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
				String unit = "deg/sec";
				if (mDefaultCalibrationParametersGyro == true) {
					unit = "deg/sec*";
				} 
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GYRO_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
				String unit = "local";
				if (mDefaultCalibrationParametersGyro == true) {
					unit = "local*";
				} 
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.MAG_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_GSR) > 0) {
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GSR,"CAL","kOhms"};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GSR,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				//channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.GSR_RES,"RAW",NO_UNIT};
				//listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_ECG) > 0) {
				
				String unit = "mVolts";
				if (mDefaultCalibrationParametersECG == true) {
					unit = "mVolts*";
				}
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ECG_RA_LL,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ECG_RA_LL,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ECG_LA_LL,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.ECG_LA_LL,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			}
			if (((mEnabledSensors & 0xFF) & SENSOR_EMG) > 0) {
				String unit = "mVolts";
				if (mDefaultCalibrationParametersECG == true) {
					unit = "mVolts*";
				}
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EMG,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EMG,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_HEART) > 0) {
				String unit = "BPM";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.HEART_RATE,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.HEART_RATE,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if ((((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A0) > 0) && getPMux() == 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if ((((mEnabledSensors & 0xFF) & SENSOR_EXP_BOARD_A7) > 0 && getPMux() == 0)) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFF) & SENSOR_BATT) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.REG,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.REG,"RAW",NO_UNIT};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BATTERY,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.BATTERY,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				String unit = "local";
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_A,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_W,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z,"CAL",unit};
				listofSignals.add(channel);
			}
//			if (((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
//				String unit = "local";
//				channel = new String[]{mMyName,"Quaternion 0","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 1","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 2","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 3","CAL",unit};
//				listofSignals.add(channel);
//			}

		} else if (mHardwareVersion==HW_ID.SHIMMER_3) {

			String[] channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.TIMESTAMP,"CAL","mSecs"};
			listofSignals.add(channel);
			channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.TIMESTAMP,"RAW",NO_UNIT};
			listofSignals.add(channel);
			if ((mEnabledSensors & SENSOR_ACCEL) >0){

				String unit = "m/(sec^2)";
				if (mDefaultCalibrationParametersAccel == true) {
					unit = "m/(sec^2)*";
				}
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			}
			if ((mEnabledSensors& SENSOR_DACCEL) >0){


				String unit = "m/(sec^2)";
				if (mDefaultCalibrationParametersDigitalAccel == true) {
					unit = "m/(sec^2)*";
				}
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0) {
				String unit = "deg/sec";
				if (mDefaultCalibrationParametersGyro == true) {
					unit = "deg/sec*";
				} 
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GYRO_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0) {
				String unit = "local";
				if (mDefaultCalibrationParametersGyro == true) {
					unit = "local*";
				} 
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_X,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_Y,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.MAG_Z,"RAW",NO_UNIT};
				listofSignals.add(channel);
			} 
			if (((mEnabledSensors & 0xFFFF) & SENSOR_BATT) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BATTERY,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BATTERY,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A15) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A7) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A7,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A7,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_EXT_ADC_A6) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A6,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXT_EXP_A6,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A1) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A1,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A1,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A12) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A12,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A12,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A13) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A13,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A13,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & 0xFFFFFF)& SENSOR_INT_ADC_A14) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A14,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.INT_EXP_A14,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if ((((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 || ((mEnabledSensors & 0xFFFF)& SENSOR_DACCEL) > 0)&& ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
				String unit = "local";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_A,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Z,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_W,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_X,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z,"CAL",unit};
				listofSignals.add(channel);
			}
//			if ((((mEnabledSensors & 0xFF)& SENSOR_ACCEL) > 0 || ((mEnabledSensors & 0xFFFF)& SENSOR_DACCEL) > 0) && ((mEnabledSensors & 0xFF)& SENSOR_GYRO) > 0 && ((mEnabledSensors & 0xFF)& SENSOR_MAG) > 0 && mOrientationEnabled){
//				String unit = "local";
//				channel = new String[]{mMyName,"Quaternion 0","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 1","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 2","CAL",unit};
//				listofSignals.add(channel);
//				channel = new String[]{mMyName,"Quaternion 3","CAL",unit};
//				listofSignals.add(channel);
//			}
			if ((mEnabledSensors & SENSOR_BMP180)>0){
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180,"CAL","kPa"};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180,"RAW",NO_UNIT};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180,"CAL","Celsius"};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if ((mEnabledSensors & SENSOR_GSR)>0){
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.GSR,"CAL","kOhms"};
				listofSignals.add(channel);
				//channel = new String[]{mMyName,"Magnetometer X","RAW",NO_UNIT};
				//listofSignals.add(channel);
			}
			if (((mEnabledSensors & SENSOR_EXG1_24BIT)>0)|| ((mEnabledSensors & SENSOR_EXG1_16BIT)>0)){
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_STATUS,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if (((mEnabledSensors & SENSOR_EXG2_24BIT)>0)|| ((mEnabledSensors & SENSOR_EXG2_16BIT)>0)){
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_STATUS,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
			if ((mEnabledSensors & SENSOR_EXG1_24BIT)>0){
				String unit = "mVolts";
				if (isEXGUsingDefaultECGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				} else {
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}

			}
			if ((mEnabledSensors & SENSOR_EXG2_24BIT)>0){
				String unit = "mVolts";
				if (isEXGUsingDefaultECGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else {
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);

					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);

				}
			}
			if ((mEnabledSensors & SENSOR_EXG1_16BIT)>0){
				String unit = "mVolts";
				if (isEXGUsingDefaultECGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				} else {
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}

			}
			if ((mEnabledSensors & SENSOR_EXG2_16BIT)>0){
				String unit = "mVolts";
				if (isEXGUsingDefaultECGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else if (isEXGUsingDefaultEMGConfiguration()){
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
					
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);
				}
				else {
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);

					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,"CAL",unit};
					listofSignals.add(channel);
					channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,"RAW",NO_UNIT};
					listofSignals.add(channel);

				}
			}
			if (((mEnabledSensors & 0xFF00) & SENSOR_BRIDGE_AMP) > 0) {
				String unit = "mVolts";
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH,"RAW",NO_UNIT};
				listofSignals.add(channel);
				
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW,"CAL",unit};
				listofSignals.add(channel);
				channel = new String[]{mMyName,Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW,"RAW",NO_UNIT};
				listofSignals.add(channel);
			}
		}
		
		if (mExtraSignalProperties != null){
			listofSignals.addAll(mExtraSignalProperties);
		}
		
		return listofSignals;
	}	
	
	public void addExtraSignalProperty(String [] property){
		if (mExtraSignalProperties==null){
			mExtraSignalProperties = new ArrayList<String[]>();
		}
		mExtraSignalProperties.add(property);
	}
	
	public void removeExtraSignalProperty(String [] property){
		for (int i=mExtraSignalProperties.size()-1;i>-1;i--){
			String[]p = mExtraSignalProperties.get(i);
			if (p[0].equals(property[0]) && p[1].equals(property[1]) && p[2].equals(property[2]) && p[3].equals(property[3])){
				mExtraSignalProperties.remove(i);
			}
			
		}
	}
	
	//AlignmentMatrixMag, SensitivityMatrixMag, OffsetVectorMag

	public double[][] getAlignmentMatrixMag(){
		return mAlignmentMatrixMagnetometer;
	}

	public double[][] getSensitivityMatrixMag(){
		return mSensitivityMatrixMagnetometer;
	}

	public double[][] getOffsetVectorMatrixMag(){
		return mOffsetVectorMagnetometer;
	}

	public double[][] getAlighmentMatrixGyro(){
		return mAlignmentMatrixGyroscope;
	}

	public double[][] getSensitivityMatrixGyro(){
		return mSensitivityMatrixGyroscope;
	}

	public double[][] getOffsetVectorMatrixGyro(){
		return mOffsetVectorGyroscope;
	}

	public double[][] getAlighmentMatrixAccel(){
		return mAlignmentMatrixAnalogAccel;
	}

	public double[][] getSensitivityMatrixAccel(){
		return mSensitivityMatrixAnalogAccel;
	}

	public double[][] getOffsetVectorMatrixAccel(){
		return mOffsetVectorAnalogAccel;
	}

	public double[][] getAlighmentMatrixWRAccel(){
		return mAlignmentMatrixWRAccel;
	}

	public double[][] getSensitivityMatrixWRAccel(){
		return mSensitivityMatrixWRAccel;
	}

	public double[][] getOffsetVectorMatrixWRAccel(){
		return mOffsetVectorWRAccel;
	}
	
	public double[][] getOffsetVectorMPLAccel(){
		return OffsetVectorMPLAccel;
	}
	
	public double[][] getSensitivityMatrixMPLAccel(){
		return SensitivityMatrixMPLAccel;
	}
	
	public double[][] getAlignmentMatrixMPLAccel(){
		return AlignmentMatrixMPLAccel;
	}
	
	public double[][] getOffsetVectorMPLMag(){
		return OffsetVectorMPLMag;
	}
	
	public double[][] getSensitivityMatrixMPLMag(){
		return SensitivityMatrixMPLMag;
	}
	
	public double[][] getAlignmentMatrixMPLMag(){
		return AlignmentMatrixMPLMag;
	}
	
	public double[][] getOffsetVectorMPLGyro(){
		return OffsetVectorMPLGyro;
	}
	
	public double[][] getSensitivityMatrixMPLGyro(){
		return SensitivityMatrixMPLGyro;
	}
	
	public double[][] getAlignmentMatrixMPLGyro(){
		return AlignmentMatrixMPLGyro;
	}


	public long getEnabledSensors() {
		return mEnabledSensors;
	}

	public String[] getListofSupportedSensors(){
		String[] sensorNames = null;
		if (mHardwareVersion==HW_ID.SHIMMER_2R || mHardwareVersion==HW_ID.SHIMMER_2){
			sensorNames = Configuration.Shimmer2.ListofCompatibleSensors;
		} else if  (mHardwareVersion==HW_ID.SHIMMER_3){
			sensorNames = Configuration.Shimmer3.ListofCompatibleSensors;
		}
		return sensorNames;
	}

	public static String[] getListofSupportedSensors(int shimmerVersion){
		String[] sensorNames = null;
		if (shimmerVersion==HW_ID.SHIMMER_2R || shimmerVersion==HW_ID.SHIMMER_2){
			sensorNames = Configuration.Shimmer2.ListofCompatibleSensors;
		} else if  (shimmerVersion==HW_ID.SHIMMER_3){
			sensorNames = Configuration.Shimmer3.ListofCompatibleSensors;
		}
		return sensorNames;
	}

	/**
	 * @param enable this enables the calibration of the gyroscope while streaming
	 * @param bufferSize sets the buffersize of the window used to determine the new calibration parameters, see implementation for more details
	 * @param threshold sets the threshold of when to use the incoming data to recalibrate gyroscope offset, this is in degrees, and the default value is 1.2
	 */
	public void enableOnTheFlyGyroCal(boolean enable,int bufferSize,double threshold){
		if (enable){
			mGyroOVCalThreshold=threshold;
			mGyroX=new DescriptiveStatistics(bufferSize);
			mGyroY=new DescriptiveStatistics(bufferSize);
			mGyroZ=new DescriptiveStatistics(bufferSize);
			mGyroXRaw=new DescriptiveStatistics(bufferSize);
			mGyroYRaw=new DescriptiveStatistics(bufferSize);
			mGyroZRaw=new DescriptiveStatistics(bufferSize);
			mEnableOntheFlyGyroOVCal = enable;
		}
	}

	protected int convertEXGGainSettingToValue(int setting){

		if (setting==0){
			return 6;
		} else if (setting==1){
			return 1;
		} else if (setting==2){
			return 2;
		} else if (setting==3){
			return 3;
		} else if (setting==4){
			return 4;
		} else if (setting==5){
			return 8;
		} else if (setting==6){
			return 12;
		}
		else {
			return -1; // -1 means invalid value
		}

	}

	/**
	 * Checks the EXG register bytes, and determines whether default ecg/emg are being used. 
	 * @return 0 for ECG, 1 for EMG, 3 for test signal and 4 for custom
	 */
	public int checkEXGConfiguration(){
		return -1;
	}

	public byte[] getEXG1RegisterContents(){
		return mEXG1RegisterArray;
	}

	public byte[] getEXG2RegisterContents(){
		return mEXG2RegisterArray;
	}

	public boolean isEXGUsingDefaultRespirationConfiguration(){
		boolean using = false;
		if(((mEXG1RegisterArray[3] & 0x0F)==0)&&((mEXG1RegisterArray[4] & 0x0F)==0)&&((mEXG2RegisterArray[3] & 0x0F)==0)&&((mEXG2RegisterArray[4] & 0x0F)==7)&&((mEXG2RegisterArray[8] & 0xC0)==0xC0)){
			using = true;
		}
		return using;
	}
	
	public boolean isEXGUsingDefaultECGConfiguration(){
		boolean using = false;
		if(((mEXG1RegisterArray[3] & 0x0F)==0)&&((mEXG1RegisterArray[4] & 0x0F)==0)&& ((mEXG2RegisterArray[3] & 0x0F)==0)&&((mEXG2RegisterArray[4] & 0x0F)==7)){
			using = true;
		}
		return using;
	}

	public boolean isEXGUsingDefaultTestSignalConfiguration(){
		boolean using = false;
		if(((mEXG1RegisterArray[3] & 0x0F)==5)&&((mEXG1RegisterArray[4] & 0x0F)==5)&& ((mEXG2RegisterArray[3] & 0x0F)==5)&&((mEXG2RegisterArray[4] & 0x0F)==5)){
			using = true;
		}
		return using;
	}

	public boolean isEXGUsingDefaultEMGConfiguration(){
		boolean using = false;
		if(((mEXG1RegisterArray[3] & 0x0F)==9)&&((mEXG1RegisterArray[4] & 0x0F)==0)&& ((mEXG2RegisterArray[3] & 0x0F)==1)&&((mEXG2RegisterArray[4] & 0x0F)==1)){
			using = true;
		}
		return using;
	}

	public byte[] getPressureRawCoefficients(){
		return mPressureCalRawParams;
	}
	
	public int getExg1CH1GainValue(){
		return mEXG1CH1GainValue;
	}
	
	public int getExg1CH2GainValue(){
		return mEXG1CH2GainValue;
	}
	
	public int getExg2CH1GainValue(){
		return mEXG2CH1GainValue;
	}
	
	public int getExg2CH2GainValue(){
		return mEXG2CH2GainValue;
	}
	
	public String parseReferenceElectrodeTotring(int referenceElectrode){
		String refElectrode = "Unknown";
		
		if(referenceElectrode==0 && (isEXGUsingDefaultECGConfiguration() || isEXGUsingDefaultEMGConfiguration()))
			refElectrode = "Fixed Potential";
		else if(referenceElectrode==13 && isEXGUsingDefaultECGConfiguration())
			refElectrode = "Inverse Wilson CT";
		else if(referenceElectrode==3 && isEXGUsingDefaultEMGConfiguration())
			refElectrode = "Inverse of Ch1";
		
		return refElectrode;
	}
	
	public String parseLeadOffComparatorTresholdToString(int treshold){
		
		String tresholdString="";
		switch(treshold){
			case 0:
				tresholdString = "Pos:95% - Neg:5%";
			break;
			case 1:
				tresholdString = "Pos:92.5% - Neg:7.5%";
			break;
			case 2:
				tresholdString = "Pos:90% - Neg:10%";
			break;
			case 3:
				tresholdString = "Pos:87.5% - Neg:12.5%";
			break;
			case 4:
				tresholdString = "Pos:85% - Neg:15%";
			break;
			case 5:
				tresholdString = "Pos:80% - Neg:20%";
			break;
			case 6:
				tresholdString = "Pos:75% - Neg:25%";
				break;
			case 7:
				tresholdString = "Pos:70% - Neg:30%";
			break;
			default:
				tresholdString = "Treshold unread";
			break;
		}
		
		return tresholdString;
	}
	
	public String parseLeadOffModeToString(int leadOffMode){
		
		String modeString="";
		switch(leadOffMode){
			case 0:
				modeString +="Off";
			break;
			case 1:
				modeString +="DC Current";
			break;
			case 2:
				modeString +="AC Current";
			break;
			default:
				modeString +="No mode selected";
			break;
		}
		
		return modeString;
	}
	
	public String parseLeadOffDetectionCurrentToString(int current){
		
		String currentString="";
		switch(current){
			case 0:
				currentString +="6 nA";
			break;
			case 1:
				currentString +="22 nA";
			break;
			case 2:
				currentString +="6 uA";
			break;
			default:
				currentString +="22 uA";
			break;
		}
		
		return currentString;
	}
	
	
	/**
	 * Computes the closest compatible sampling rate for the Shimmer based on
	 * the passed in 'rate' variable. Also computes the next highest available
	 * sampling rate for the Shimmer's sensors (dependent on pre-set low-power
	 * modes).
	 * 
	 * @param rate
	 */
	public void setShimmerSamplingRate(double rate){
		
		double maxRate = 0.0;
		if (mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R) {
			maxRate = 1024.0;
		} else if (mHardwareVersion==HW_ID.SHIMMER_3 || mHardwareVersion==HW_ID.SHIMMER_GQ) {
			maxRate = 2048.0;
		}		
    	// don't let sampling rate < 0 OR > maxRate
    	if(rate < 1) {
    		rate = 1.0;
    	}
    	else if (rate > maxRate) {
    		rate = maxRate;
    	}
    	
    	 // get Shimmer compatible sampling rate
    	Double actualSamplingRate = maxRate/Math.floor(maxRate/rate);
    	 // round sampling rate to two decimal places
    	actualSamplingRate = (double)Math.round(actualSamplingRate * 100) / 100;
		mShimmerSamplingRate = actualSamplingRate;
		
		if(mHardwareVersion==HW_ID.SHIMMER_2 || mHardwareVersion==HW_ID.SHIMMER_2R) {
			if(!mLowPowerMag){
				if(rate<=10) {
					mShimmer2MagRate = 4;
				} 
				else if (rate<=20) {
					mShimmer2MagRate = 5;
				} 
				else {
					mShimmer2MagRate = 6;
				}
			} 
			else {
				mShimmer2MagRate = 4;
			}
		} 
		else if (mHardwareVersion==HW_ID.SHIMMER_3 || mHardwareVersion==HW_ID.SHIMMER_GQ) {
			setLSM303MagRateFromFreq(mShimmerSamplingRate);
			setLSM303AccelRateFromFreq(mShimmerSamplingRate);
			setMPU9150GyroAccelRateFromFreq(mShimmerSamplingRate);
			setExGRateFromFreq(mShimmerSamplingRate);
		}
	}
	
	/**
	 * Computes next higher available sensor sampling rate setting based on
	 * passed in "freq" variable and dependent on whether low-power mode is set.
	 * 
	 * @param freq
	 * @return int the rate configuration setting for the respective sensor
	 */
	private int setLSM303AccelRateFromFreq(double freq) {
		// Unused: 8 = 1.620kHz (only low-power mode), 9 = 1.344kHz (normal-mode) / 5.376kHz (low-power mode)
		
		// Check if channel is enabled 
		if (mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL) != null) {
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mIsEnabled) {
				mLSM303DigitalAccelRate = 0; // Power down
				return mLSM303DigitalAccelRate;
			}
		}
		
		if (!mLowPowerAccelWR){
			if (freq<=1){
				mLSM303DigitalAccelRate = 1; // 1Hz
			} else if (freq<=10){
				mLSM303DigitalAccelRate = 2; // 10Hz
			} else if (freq<=25){
				mLSM303DigitalAccelRate = 3; // 25Hz
			} else if (freq<=50){
				mLSM303DigitalAccelRate = 4; // 50Hz
			} else if (freq<=100){
				mLSM303DigitalAccelRate = 5; // 100Hz
			} else if (freq<=200){
				mLSM303DigitalAccelRate = 6; // 200Hz
			} else if (freq<=400){
				mLSM303DigitalAccelRate = 7; // 400Hz
			} else {
				mLSM303DigitalAccelRate = 9; // 1344Hz
			}
		}
		else {
			if (freq>=10){
				mLSM303DigitalAccelRate = 2; // 10Hz
			} else {
				mLSM303DigitalAccelRate = 1; // 1Hz
			}
		}
		return mLSM303DigitalAccelRate;
	}
	
	/**
	 * Computes next higher available sensor sampling rate setting based on
	 * passed in "freq" variable and dependent on whether low-power mode is set.
	 * 
	 * @param freq
	 * @return int the rate configuration setting for the respective sensor
	 */
	private int setLSM303MagRateFromFreq(double freq) {
		// Check if channel is enabled 
		if (mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG) != null) {
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mIsEnabled) {
				mLSM303MagRate = 0; // 0.75Hz
				return mLSM303MagRate;
			}
		}
		
		if (!mLowPowerMag){
			if (freq<=0.75){
				mLSM303MagRate = 0; // 0.75Hz
			} else if (freq<=1){
				mLSM303MagRate = 1; // 1.5Hz
			} else if (freq<=3) {
				mLSM303MagRate = 2; // 3Hz
			} else if (freq<=7.5) {
				mLSM303MagRate = 3; // 7.5Hz
			} else if (freq<=15) {
				mLSM303MagRate = 4; // 15Hz
			} else if (freq<=30) {
				mLSM303MagRate = 5; // 30Hz
			} else if (freq<=75) {
				mLSM303MagRate = 6; // 75Hz
			} else {
				mLSM303MagRate = 7; // 220Hz
			}
		} else {
			if (freq>=10){
				mLSM303MagRate = 4; // 15Hz
			} else {
				mLSM303MagRate = 1; // 1.5Hz
			}
		}		
		return mLSM303MagRate;
	}
	
	/**
	 * Computes next higher available sensor sampling rate setting based on
	 * passed in "freq" variable and dependent on whether low-power mode is set.
	 * 
	 * @param freq
	 * @return int the rate configuration setting for the respective sensor
	 */
	public int setMPU9150GyroAccelRateFromFreq(double freq) {
		// Check if channel is enabled 
		if (mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO) != null) {
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mIsEnabled) {
				mMPU9150GyroAccelRate = 0xFF; // Dec. = 255, Freq. = 31.25Hz
				return mMPU9150GyroAccelRate;
			}
		}
		
		// Gyroscope Output Rate = 8kHz when the DLPF (Digital Low-pass filter) is disabled (DLPF_CFG = 0 or 7), and 1kHz when the DLPF is enabled
		double numerator = 1000;
		if(mMPU9150LPF == 0) {
			numerator = 8000;
		}

		if (!mLowPowerGyro){
			if(freq<4) {
				freq = 4;
			}
			else if(freq>numerator) {
				freq = numerator;
			}
			int result = (int) Math.floor(((numerator / freq) - 1));
			if(result>255) {
				result = 255;
			}
			mMPU9150GyroAccelRate = result;

		}
		else {
			mMPU9150GyroAccelRate = 0xFF; // Dec. = 255, Freq. = 31.25Hz
		}
		return mMPU9150GyroAccelRate;
	}
	
	/**
	 * Computes next higher available sensor sampling rate setting based on
	 * passed in "freq" variable and dependent on whether low-power mode is set.
	 * 
	 * @param freq
	 * @return int the rate configuration setting for the respective sensor
	 */
	public int setExGRateFromFreq(double freq) {
		if (freq<=125) {
			mEXG1RateSetting = 0x00; // 125Hz
		} else if (freq<=250) {
			mEXG1RateSetting = 0x01; // 250Hz
		} else if (freq<=500) {
			mEXG1RateSetting = 0x02; // 500Hz
		} else if (freq<=1000) {
			mEXG1RateSetting = 0x03; // 1000Hz
		} else if (freq<=2000) {
			mEXG1RateSetting = 0x04; // 2000Hz
		} else if (freq<=4000) {
			mEXG1RateSetting = 0x05; // 4000Hz
		} else if (freq<=8000) {
			mEXG1RateSetting = 0x06; // 8000Hz
		} else {
			mEXG1RateSetting = 0x02; // 500Hz
		}
		mEXG2RateSetting = mEXG1RateSetting;
		
		mEXG1RegisterArray[0] = (byte) ((mEXG1RegisterArray[0] & ~0x07) | mEXG1RateSetting);
		mEXG2RegisterArray[0] = (byte) ((mEXG2RegisterArray[0] & ~0x07) | mEXG2RateSetting);
		
		//TODO decide whether to include exgBytesGetFromConfig() here or not
		//exgBytesGetFromConfig();
		return mEXG1RateSetting;
	}
	
	/**
	 * Checks to see if the MPU9150 gyro is in low power mode. As determined by
	 * the sensor's sampling rate being set to the lowest possible value and not
	 * related to any specific configuration bytes sent to the Shimmer/MPU9150.
	 * 
	 * @return boolean, true if low-power mode enabled
	 */
	public boolean checkLowPowerGyro() {
		if(mMPU9150GyroAccelRate == 0xFF) {
			mLowPowerGyro = true;
		}
		else {
			mLowPowerGyro = false;
		}
		return mLowPowerGyro;
	}

	/**
	 * Checks to see if the LSM303DLHC Mag is in low power mode. As determined by
	 * the sensor's sampling rate being set to the lowest possible value and not
	 * related to any specific configuration bytes sent to the Shimmer/MPU9150.
	 * 
	 * @return boolean, true if low-power mode enabled
	 */
	public boolean checkLowPowerMag() {
		if(mLSM303MagRate <= 4) {
			mLowPowerMag = true;
		}
		else {
			mLowPowerMag = false;
		}
		return mLowPowerMag;
	}
	
	
	/**
	 * Populates the individual ExG related variables in ShimmerObject per ExG
	 * chip based on the ExG configuration byte arrays
	 * 
	 * @param chipIndex
	 *            indicates whether the bytes are specific to chip 1 or chip 2
	 *            on the ExG expansion board.
	 * @param byteArray
	 *            the configuration byte array for an individual chip (10-bytes
	 *            long)
	 */
	public void exgBytesGetConfigFrom(int chipIndex, byte[] byteArray) {
		// to overcome possible backward compatability issues (where
		// bufferAns.length was 11 or 12 using all of the ExG config bytes)
		int index = 1;
		if(byteArray.length == 10) {
			index = 0;
		}
		
		if (chipIndex==1){
			System.arraycopy(byteArray, index, mEXG1RegisterArray, 0, 10);
			// retrieve the gain and rate from the the registers
			mEXG1RateSetting = mEXG1RegisterArray[0] & 7;
			mEXGLeadOffDetectionCurrent = (mEXG1RegisterArray[2] >> 2) & 3;
			mEXGLeadOffComparatorTreshold = (mEXG1RegisterArray[2] >> 5) & 7;
			mEXG1CH1GainSetting = (mEXG1RegisterArray[3] >> 4) & 7;
			mEXG1CH1GainValue = convertEXGGainSettingToValue(mEXG1CH1GainSetting);
			mEXG1CH2GainSetting = (mEXG1RegisterArray[4] >> 4) & 7;
			mEXG1CH2GainValue = convertEXGGainSettingToValue(mEXG1CH2GainSetting);
			mEXGReferenceElectrode = mEXG1RegisterArray[5] & 0x0F;
			mEXG1LeadOffCurrentMode = mEXG1RegisterArray[2] & 1;
			mEXG1Comparators = mEXG1RegisterArray[1] & 0x40;								
			mEXGRLDSense = mEXG1RegisterArray[5] & 0x10;
			mEXG1LeadOffSenseSelection = mEXG1RegisterArray[6] & 0x0f; //2P1N1P
			
		} else if (chipIndex==2){
			System.arraycopy(byteArray, index, mEXG2RegisterArray, 0, 10);
			mEXG2RateSetting = mEXG2RegisterArray[0] & 7;
			mEXG2CH1GainSetting = (mEXG2RegisterArray[3] >> 4) & 7;
			mEXG2CH1GainValue = convertEXGGainSettingToValue(mEXG2CH1GainSetting);
			mEXG2CH2GainSetting = (mEXG2RegisterArray[4] >> 4) & 7;
			mEXG2CH2GainValue = convertEXGGainSettingToValue(mEXG2CH2GainSetting);
			mEXG2LeadOffCurrentMode = mEXG2RegisterArray[2] & 1;
			mEXG2Comparators = mEXG2RegisterArray[1] & 0x40;
			mEXG2LeadOffSenseSelection = mEXG2RegisterArray[6] & 0x0f; //2P
			
			mEXG2RespirationDetectState = (mEXG2RegisterArray[8] >> 6) & 0x03;
			mEXG2RespirationDetectPhase = (mEXG2RegisterArray[8] >> 2) & 0x0F;
			mEXG2RespirationDetectFreq = (mEXG2RegisterArray[9] >> 2) & 0x01;
		}
	}

	/**
	 * Generates the ExG configuration byte arrays based on the individual ExG
	 * related variables stored in ShimmerObject. The resulting arrays are
	 * stored in the global variables mEXG1RegisterArray and mEXG2RegisterArray.
	 * 
	 */
	public void exgBytesGetFromConfig() {
		//ExG Chip1
		mEXG1RegisterArray[0] &= ~(0x07 << 0);
		mEXG1RegisterArray[0] |= (mEXG1RateSetting & 7) << 0;
		
		mEXG1RegisterArray[1] &= (~(0x40 << 0));
		mEXG1RegisterArray[1] |= ((mEXG1Comparators & 0x40) << 0);
		
		mEXG1RegisterArray[2] &= (~(0x01 << 0));
		mEXG1RegisterArray[2] |= ((mEXG1LeadOffCurrentMode & 0x01) << 0);
		mEXG1RegisterArray[2] &= (~(0x03 << 2));
		mEXG1RegisterArray[2] |= ((mEXGLeadOffDetectionCurrent & 0x03) << 2);
		mEXG1RegisterArray[2] &= (~(0x07 << 5));
		mEXG1RegisterArray[2] |= ((mEXGLeadOffComparatorTreshold & 0x07) << 5);
		
		mEXG1RegisterArray[3] &= (~(0x07 << 4));
		mEXG1RegisterArray[3] |= ((mEXG1CH1GainSetting & 0x07) << 4);
		mEXG1RegisterArray[4] &= (~(0x07 << 4));
		mEXG1RegisterArray[4] |= ((mEXG1CH2GainSetting & 0x07) << 4);
		
		mEXG1RegisterArray[5] &= (~(0x0F << 0));
		mEXG1RegisterArray[5] |= ((mEXGReferenceElectrode & 0x0F) << 0);
		mEXG1RegisterArray[1] &= (~(0x04 << 0));
		mEXG1RegisterArray[1] |= ((mEXG1Comparators & 0x40) << 0);
		mEXG1RegisterArray[5] &= (~(0x10 << 0));
		mEXG1RegisterArray[5] |= ((mEXGRLDSense & 0x10) << 0);
		mEXG1RegisterArray[6] &= (~(0x0F << 0));
		mEXG1RegisterArray[6] |= ((mEXG1LeadOffSenseSelection & 0x0F) << 0);

		//ExG Chip2
		mEXG2RegisterArray[0] &= (~(0x07 << 0));
		mEXG2RegisterArray[0] |= ((mEXG2RateSetting & 0x07) << 0);
		
		mEXG2RegisterArray[1] &= (~(0x40 << 0));
		mEXG2RegisterArray[1] |= ((mEXG2Comparators & 0x40) << 0);
		
		mEXG2RegisterArray[2] &= (~(0x01 << 0));
		mEXG2RegisterArray[2] |= ((mEXG2LeadOffCurrentMode & 0x01) << 0);
		mEXG2RegisterArray[2] &= (~(0x03 << 2));
		mEXG2RegisterArray[2] |= ((mEXGLeadOffDetectionCurrent & 0x03) << 2);
		mEXG2RegisterArray[2] &= (~(0x07 << 5));
		mEXG2RegisterArray[2] |= ((mEXGLeadOffComparatorTreshold & 0x07) << 5);

		mEXG2RegisterArray[3] &= (~(0x07 << 4));
		mEXG2RegisterArray[3] |= ((mEXG2CH1GainSetting & 0x07) << 4);
		mEXG2RegisterArray[4] &= (~(0x07 << 4));
		mEXG2RegisterArray[4] |= ((mEXG2CH2GainSetting & 0x07) << 4);
		
		mEXG2RegisterArray[6] &= (~(0x0F << 0));
		mEXG2RegisterArray[6] |= ((mEXG2LeadOffSenseSelection & 0x0F) << 0);
		
		mEXG2RegisterArray[8] &= (~(0x03 << 6));
		mEXG2RegisterArray[8] |= ((mEXG2RespirationDetectState & 0x03) << 6);
		mEXG2RegisterArray[8] &= (~(0x03 << 2));
		mEXG2RegisterArray[8] |= ((mEXG2RespirationDetectPhase & 0x0F) << 2);
		mEXG2RegisterArray[9] &= (~(0x01 << 2));
		mEXG2RegisterArray[9] |= ((mEXG2RespirationDetectFreq & 0x01) << 2);
	}
	
	
	/**
	 * Checks if 16 bit ECG configuration is set on the Shimmer device. Do not
	 * use this command right after setting an EXG setting, as due to the
	 * execution model, the old settings might be returned, if this command is
	 * executed before an ack is received.
	 * 
	 * @return true if 16 bit ECG is set
	 */
	public boolean isEXGUsingECG16Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_16BIT)>0 && (mEnabledSensors & SENSOR_EXG2_16BIT)>0){
			if(isEXGUsingDefaultECGConfiguration()){
				using = true;
			}
		}
		return using;
	}
	
	/**
	 * Checks if 24 bit ECG configuration is set on the Shimmer device. Do not
	 * use this command right after setting an EXG setting, as due to the
	 * execution model, the old settings might be returned, if this command is
	 * executed before an ack is received.
	 * 
	 * @return true if 24 bit ECG is set
	 */
	public boolean isEXGUsingECG24Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_24BIT)>0 && (mEnabledSensors & SENSOR_EXG2_24BIT)>0){
			if(isEXGUsingDefaultECGConfiguration()){
				using = true;
			}
		}
		return using;
	}
	
	/**
	 * Checks if 16 bit EMG configuration is set on the Shimmer device. Do not
	 * use this command right after setting an EXG setting, as due to the
	 * execution model, the old settings might be returned, if this command is
	 * executed before an ack is received.
	 * 
	 * @return true if 16 bit EMG is set
	 */
	public boolean isEXGUsingEMG16Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_16BIT)>0 && (mEnabledSensors & SENSOR_EXG2_16BIT)>0){
			if(isEXGUsingDefaultEMGConfiguration()){
				using = true;
			}
		}
		return using;
	}
	
	/**
	 * Checks if 24 bit EMG configuration is set on the Shimmer device. Do not
	 * use this command right after setting an EXG setting, as due to the
	 * execution model, the old settings might be returned, if this command is
	 * executed before an ack is received.
	 * 
	 * @return true if 24 bit EMG is set
	 */
	public boolean isEXGUsingEMG24Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_24BIT)>0 && (mEnabledSensors & SENSOR_EXG2_24BIT)>0){
			if(isEXGUsingDefaultEMGConfiguration()){
				using = true;
			}
		}
		return using;
	}
	
	/**
	 * Checks if 16 bit test signal configuration is set on the Shimmer device.
	 * Do not use this command right after setting an EXG setting, as due to the
	 * execution model, the old settings might be returned, if this command is
	 * executed before an ack is received.
	 * 
	 * @return true if 24 bit test signal is set
	 */
	public boolean isEXGUsingTestSignal16Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_16BIT)>0 && (mEnabledSensors & SENSOR_EXG2_16BIT)>0){
			if(isEXGUsingDefaultTestSignalConfiguration()){
				using = true;
			}
		}
		return using;
	}
	
	/**
	 * Checks if 24 bit test signal configuration is set on the Shimmer device.
	 * @return true if 24 bit test signal is set
	 */
	public boolean isEXGUsingTestSignal24Configuration(){
		boolean using = false;
		if ((mEnabledSensors & SENSOR_EXG1_24BIT)>0 && (mEnabledSensors & SENSOR_EXG2_24BIT)>0){
			if(isEXGUsingDefaultTestSignalConfiguration()){
				using = true;
			}
		}
		return using;
	}	
	
	/**
	 * This enables the calculation of 3D orientation through the use of the
	 * gradient descent algorithm, note that the user will have to ensure that
	 * mEnableCalibration has been set to true (see enableCalibration), and that
	 * the accel, gyro and mag has been enabled
	 * 
	 * @param enable
	 */
	protected void set3DOrientation(boolean enable){
		//enable the sensors if they have not been enabled 
		mOrientationEnabled = enable;
	}	
	
	/**
	 * This enables the low power accel option. When not enabled the sampling
	 * rate of the accel is set to the closest value to the actual sampling rate
	 * that it can achieve. In low power mode it defaults to 10Hz. Also and
	 * additional low power mode is used for the LSM303DLHC. This command will
	 * only supports the following Accel range +4g, +8g , +16g
	 * 
	 * @param enable
	 */
	protected void setLowPowerAccelWR(boolean enable){
		mLowPowerAccelWR = enable;
		mHighResAccelWR = !enable;

		setLSM303AccelRateFromFreq(mShimmerSamplingRate);
		
		if(mConfigOptionsMap!=null) {
			if(mLowPowerAccelWR) {
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpm, 
												Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpmConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
			}
			else {
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRate, 
												Configuration.Shimmer3.ListofLSM303DLHCAccelRateConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
			}
		}
	}
	
	/**
	 * This enables the low-power gyro option. When not enabled the sampling
	 * rate of the gyro is set to the closest supported value to the actual
	 * sampling rate that it can achieve. For the Shimmer2, in low power mode it
	 * defaults to 10Hz.
	 * 
	 * @param enable
	 */
	protected void setLowPowerGyro(boolean enable){
		mLowPowerGyro = enable;
		setMPU9150GyroAccelRateFromFreq(mShimmerSamplingRate);
	}
	
	/**
	 * This enables the low power mag option. When not enabled the sampling rate
	 * of the mag is set to the closest supported value to the actual sampling
	 * rate that it can achieve. In low power mode it defaults to 10Hz
	 * 
	 * @param enable
	 */
	protected void setLowPowerMag(boolean enable){
		mLowPowerMag = enable;
		if((mHardwareVersion==HW_ID.SHIMMER_2)||(mHardwareVersion==HW_ID.SHIMMER_2R)){
			if (!mLowPowerMag){
				if (mShimmerSamplingRate>=50){
					mShimmer2MagRate = 6;
				} else if (mShimmerSamplingRate>=20) {
					mShimmer2MagRate = 5;
				} else if (mShimmerSamplingRate>=10) {
					mShimmer2MagRate = 4;
				} else {
					mShimmer2MagRate = 3;
				}
			} else {
				mShimmer2MagRate = 4;
			}
		} else {
			setLSM303MagRateFromFreq(mShimmerSamplingRate);
		}
	}
	
	/**
	 * This can only be used for Shimmer3 devices (EXG) When a enable
	 * configuration is load, the advanced exg configuration is removed, so it
	 * needs to be set again
	 * 
	 */
	 protected void setDefaultRespirationConfiguration() {
		 if (mHardwareVersion==HW_ID.SHIMMER_3){
			mEXG1RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 64,(byte) 64,(byte) 0x20,(byte) 0,(byte) 0,(byte) 2,(byte) 3};
			mEXG2RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 64,(byte) 71,(byte) 0,(byte) 0,(byte) 0,(byte) 0xEA,(byte) 1};
			setExGRateFromFreq(mShimmerSamplingRate);
			exgBytesGetConfigFrom(1, mEXG1RegisterArray);
			exgBytesGetConfigFrom(2, mEXG2RegisterArray);
		 }
	}
	
	/**
	 * This can only be used for Shimmer3 devices (EXG) When a enable
	 * configuration is load, the advanced exg configuration is removed, so it
	 * needs to be set again
	 * 
	 */
	 protected void setDefaultECGConfiguration() {
		 if (mHardwareVersion==HW_ID.SHIMMER_3){
			mEXG1RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 64,(byte) 64,(byte) 45,(byte) 0,(byte) 0,(byte) 2,(byte) 3};
			mEXG2RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 64,(byte) 71,(byte) 0,(byte) 0,(byte) 0,(byte) 2,(byte) 1};
			setExGRateFromFreq(mShimmerSamplingRate);
			exgBytesGetConfigFrom(1, mEXG1RegisterArray);
			exgBytesGetConfigFrom(2, mEXG2RegisterArray);
		 }
	}

	/**
	 * This can only be used for Shimmer3 devices (EXG) When a enable
	 * configuration is load, the advanced exg configuration is removed, so it
	 * needs to be set again
	 */
	 protected void setDefaultEMGConfiguration(){
		if (mHardwareVersion==HW_ID.SHIMMER_3){
			mEXG1RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 105,(byte) 96,(byte) 32,(byte) 0,(byte) 0,(byte) 2,(byte) 3};
			mEXG2RegisterArray = new byte[]{(byte) 2,(byte) 160,(byte) 16,(byte) 129,(byte) 129,(byte) 0,(byte) 0,(byte) 0,(byte) 2,(byte) 1};
			setExGRateFromFreq(mShimmerSamplingRate);
			exgBytesGetConfigFrom(1, mEXG1RegisterArray);
			exgBytesGetConfigFrom(2, mEXG2RegisterArray);
		}
		
	}

	/**
	 * This can only be used for Shimmer3 devices (EXG). Enables the test signal
	 * (square wave) of both EXG chips, to use, both EXG1 and EXG2 have to be
	 * enabled
	 */
	 protected void setEXGTestSignal(){
		if (mHardwareVersion==HW_ID.SHIMMER_3){
			mEXG1RegisterArray = new byte[]{(byte) 2,(byte) 163,(byte) 16,(byte) 5,(byte) 5,(byte) 0,(byte) 0,(byte) 0,(byte) 2,(byte) 1};
			mEXG2RegisterArray = new byte[]{(byte) 2,(byte) 163,(byte) 16,(byte) 5,(byte) 5,(byte) 0,(byte) 0,(byte) 0,(byte) 2,(byte) 1};
			setExGRateFromFreq(mShimmerSamplingRate);
			exgBytesGetConfigFrom(1, mEXG1RegisterArray);
			exgBytesGetConfigFrom(2, mEXG2RegisterArray);
		}
	}

	
	/**Sets all default Shimmer settings in ShimmerObject.
	 * 
	 */
	public void setDefaultShimmerConfiguration() {
		if (mHardwareVersion != -1){
			
			mShimmerUserAssignedName = DEFAULT_SHIMMER_NAME;
			mExperimentName = DEFAULT_EXPERIMENT_NAME;
			
			mExperimentNumberOfShimmers = 1;
			mExperimentId = 0;
			mButtonStart = 1;
			
			mBluetoothBaudRate=0;

			mInternalExpPower=0;

			mExGResolution = 0;
			mShimmer2MagRate=0;
			
			mMasterShimmer = 0;
			mSingleTouch = 0;
			mTCXO = 0;
			
			mPacketSize=0; 
			mConfigByte0=0;
			mNChannels=0; 
			mBufferSize=0;                   							
			mSyncBroadcastInterval = 0;
			mInitialTimeStamp = 0;
			
			setShimmerSamplingRate(51.2);
			setDefaultECGConfiguration(); 
			
			syncNodesList.clear();
			
			sensorAndConfigMapsCreate();
			if (mHardwareVersion == HW_ID.SHIMMER_3){
				setSensorEnabledState(Configuration.Shimmer3.SensorMapKey.A_ACCEL, true);
				setSensorEnabledState(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO, true);
				setSensorEnabledState(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG, true);
				setSensorEnabledState(Configuration.Shimmer3.SensorMapKey.VBATT, true);
			}
		}
	}
	
	
	/**
	 * Parse the Shimmer's Information Memory when read through the Shimmer
	 * Dock/Consensys Base. The Information Memory is a region of the Shimmer's
	 * inbuilt RAM where all configuration information is stored.
	 * 
	 * @param infoMemContents
	 *            the array of InfoMem bytes.
	 */
	protected void infoMemByteArrayParse(byte[] infoMemContents) {
		String shimmerName = "";

		// Check first 6 bytes of InfoMem for 0xFF to determine if contents are valid 
		byte[] comparisonBuffer = new byte[]{-1,-1,-1,-1,-1,-1};
		byte[] detectionBuffer = new byte[comparisonBuffer.length];
		System.arraycopy(infoMemContents, 0, detectionBuffer, 0, detectionBuffer.length);
		if(Arrays.equals(comparisonBuffer, detectionBuffer)) {
			// InfoMem not valid
			setDefaultShimmerConfiguration();
			mShimmerUsingConfigFromInfoMem = false;

//			mShimmerInfoMemBytes = infoMemByteArrayGenerate();
//			mShimmerInfoMemBytes = new byte[infoMemContents.length];
			mInfoMemBytes = infoMemContents;
		}
		else {
			mShimmerUsingConfigFromInfoMem = true;

			// InfoMem valid
			
			mInfoMemBytes = infoMemContents;
			InfoMemLayout infoMemMap = new InfoMemLayout(mFirmwareIdentifier, mFirmwareVersionMajor, mFirmwareVersionMinor, mFirmwareVersionInternal);
			
			// InfoMem D - Start - used by BtStream, SdLog and LogAndStream
			// Sampling Rate
			mShimmerSamplingRate = (32768/(double)((int)(infoMemContents[infoMemMap.idxShimmerSamplingRate] & infoMemMap.maskShimmerSamplingRate) + ((int)(infoMemContents[infoMemMap.idxShimmerSamplingRate+1] & infoMemMap.maskShimmerSamplingRate) << 8)));
	
			mBufferSize = (int)(infoMemContents[infoMemMap.idxBufferSize] & infoMemMap.maskBufferSize);
			
			// Sensors
			mEnabledSensors = ((long)infoMemContents[infoMemMap.idxSensors0] & infoMemMap.maskSensors) << infoMemMap.byteShiftSensors0;
			mEnabledSensors += ((long)infoMemContents[infoMemMap.idxSensors1] & infoMemMap.maskSensors) << infoMemMap.byteShiftSensors1;
			mEnabledSensors += ((long)infoMemContents[infoMemMap.idxSensors2] & infoMemMap.maskSensors) << infoMemMap.byteShiftSensors2;

			// Configuration
			mLSM303DigitalAccelRate = (infoMemContents[infoMemMap.idxConfigSetupByte0] >> infoMemMap.bitShiftLSM303DLHCAccelSamplingRate) & infoMemMap.maskLSM303DLHCAccelSamplingRate; 
			mAccelRange = (infoMemContents[infoMemMap.idxConfigSetupByte0] >> infoMemMap.bitShiftLSM303DLHCAccelRange) & infoMemMap.maskLSM303DLHCAccelRange;
			if(((infoMemContents[infoMemMap.idxConfigSetupByte0] >> infoMemMap.bitShiftLSM303DLHCAccelLPM) & infoMemMap.maskLSM303DLHCAccelLPM) == infoMemMap.maskLSM303DLHCAccelLPM) {
				mLowPowerAccelWR = true;
			}
			else {
				mLowPowerAccelWR = false;
			}
			if(((infoMemContents[infoMemMap.idxConfigSetupByte0] >> infoMemMap.bitShiftLSM303DLHCAccelHRM) & infoMemMap.maskLSM303DLHCAccelHRM) == infoMemMap.maskLSM303DLHCAccelHRM) {
				mHighResAccelWR = true;
			}
			else {
				mHighResAccelWR = false;
			}
			mMPU9150GyroAccelRate = (infoMemContents[infoMemMap.idxConfigSetupByte1] >> infoMemMap.bitShiftMPU9150AccelGyroSamplingRate) & infoMemMap.maskMPU9150AccelGyroSamplingRate;
			checkLowPowerGyro(); // check rate to determine if Sensor is in LPM mode
			
			mMagRange = (infoMemContents[infoMemMap.idxConfigSetupByte2] >> infoMemMap.bitShiftLSM303DLHCMagRange) & infoMemMap.maskLSM303DLHCMagRange;
			mLSM303MagRate = (infoMemContents[infoMemMap.idxConfigSetupByte2] >> infoMemMap.bitShiftLSM303DLHCMagSamplingRate) & infoMemMap.maskLSM303DLHCMagSamplingRate;
			checkLowPowerMag(); // check rate to determine if Sensor is in LPM mode

			mGyroRange = (infoMemContents[infoMemMap.idxConfigSetupByte2] >> infoMemMap.bitShiftMPU9150GyroRange) & infoMemMap.maskMPU9150GyroRange;
			
			mMPU9150AccelRange = (infoMemContents[infoMemMap.idxConfigSetupByte3] >> infoMemMap.bitShiftMPU9150AccelRange) & infoMemMap.maskMPU9150AccelRange;
			mPressureResolution = (infoMemContents[infoMemMap.idxConfigSetupByte3] >> infoMemMap.bitShiftBMP180PressureResolution) & infoMemMap.maskBMP180PressureResolution;
			mGSRRange = (infoMemContents[infoMemMap.idxConfigSetupByte3] >> infoMemMap.bitShiftGSRRange) & infoMemMap.maskGSRRange;
			mInternalExpPower = (infoMemContents[infoMemMap.idxConfigSetupByte3] >> infoMemMap.bitShiftEXPPowerEnable) & infoMemMap.maskEXPPowerEnable;
			
			//EXG Configuration
			System.arraycopy(infoMemContents, infoMemMap.idxEXGADS1292RChip1Config1, mEXG1RegisterArray, 0, 10);
			exgBytesGetConfigFrom(1,mEXG1RegisterArray);
	
			System.arraycopy(infoMemContents, infoMemMap.idxEXGADS1292RChip2Config1, mEXG2RegisterArray, 0, 10);
			exgBytesGetConfigFrom(2,mEXG2RegisterArray);
			
			mBluetoothBaudRate = infoMemContents[infoMemMap.idxBtCommBaudRate] & infoMemMap.maskBaudRate;
			//TODO: hack below -> fix
//			if(!(mBluetoothBaudRate>=0 && mBluetoothBaudRate<=10)){
//				mBluetoothBaudRate = 0; 
//			}
			
			String[] dataType={"i16","i16","i16","i16","i16","i16","i8","i8","i8","i8","i8","i8","i8","i8","i8"};
			// Analog Accel Calibration Parameters
			byte[] bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
			System.arraycopy(infoMemContents, infoMemMap.idxAnalogAccelCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
			int[] formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
			double[] AM=new double[9];
			for (int i=0;i<9;i++)
			{
				AM[i]=((double)formattedPacket[6+i])/100;
			}
			double[][] alignmentMatrixAA = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
			double[][] sensitivityMatrixAA = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
			double[][] offsetVectorAA = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
			mAlignmentMatrixAnalogAccel = alignmentMatrixAA; 			
			mSensitivityMatrixAnalogAccel = sensitivityMatrixAA; 	
			mOffsetVectorAnalogAccel = offsetVectorAA;
			
			// MPU9150 Gyroscope Calibration Parameters
			bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
			System.arraycopy(infoMemContents, infoMemMap.idxMPU9150GyroCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
			formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
			AM=new double[9];
			for (int i=0;i<9;i++)
			{
				AM[i]=((double)formattedPacket[6+i])/100;
			}
			double[][] alignmentMatrixG = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
			double[][] sensitivityMatrixG = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
			double[][] offsetVectorG = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
			mAlignmentMatrixGyroscope = alignmentMatrixG; 			
			mSensitivityMatrixGyroscope = sensitivityMatrixG; 	
			mSensitivityMatrixGyroscope[0][0] = mSensitivityMatrixGyroscope[0][0]/100;
			mSensitivityMatrixGyroscope[1][1] = mSensitivityMatrixGyroscope[1][1]/100;
			mSensitivityMatrixGyroscope[2][2] = mSensitivityMatrixGyroscope[2][2]/100;
			mOffsetVectorGyroscope = offsetVectorG;
	
			// LSM303DLHC Magnetometer Calibration Parameters
			bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
			System.arraycopy(infoMemContents, infoMemMap.idxLSM303DLHCMagCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
			formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
			AM=new double[9];
			for (int i=0;i<9;i++)
			{
				AM[i]=((double)formattedPacket[6+i])/100;
			}
			double[][] alignmentMatrixM = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
			double[][] sensitivityMatrixM = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
			double[][] offsetVectorM = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
			mAlignmentMatrixMagnetometer = alignmentMatrixM; 			
			mSensitivityMatrixMagnetometer = sensitivityMatrixM; 	
			mOffsetVectorMagnetometer = offsetVectorM;
			
			// LSM303DLHC Digital Accel Calibration Parameters
			bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
			System.arraycopy(infoMemContents, infoMemMap.idxLSM303DLHCAccelCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
			parseCalParamLSM303DLHCAccel(bufferCalibrationParameters);

			//TODO: decide what to do
			// BMP180 Pressure Calibration Parameters

//			mDerivedSensors = (long)0;
//			if((infoMemMap.idxDerivedSensors0>=0)&&(infoMemMap.idxDerivedSensors1>=0)) { // Check if compatible
//				if((infoMemContents[infoMemMap.idxDerivedSensors0] != (byte)infoMemMap.maskDerivedChannelsByte)
//						&&(infoMemContents[infoMemMap.idxDerivedSensors1] != (byte)infoMemMap.maskDerivedChannelsByte)){
//					mDerivedSensors = ((long)infoMemContents[infoMemMap.idxDerivedSensors0] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors0;
//					mDerivedSensors |= ((long)infoMemContents[infoMemMap.idxDerivedSensors1] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors1;
//					if(infoMemMap.idxDerivedSensors2>=0) { // Check if compatible
//						mDerivedSensors |= ((long)infoMemContents[infoMemMap.idxDerivedSensors2] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors2;
//					}
//				}
//			}
			
			mDerivedSensors = (long)0;
			if((infoMemMap.idxDerivedSensors0>=0)&&(infoMemContents[infoMemMap.idxDerivedSensors0] != (byte)infoMemMap.maskDerivedChannelsByte)
					&&(infoMemMap.idxDerivedSensors1>=0)&&(infoMemContents[infoMemMap.idxDerivedSensors1] != (byte)infoMemMap.maskDerivedChannelsByte)) { // Check if compatible
				
				mDerivedSensors = ((long)infoMemContents[infoMemMap.idxDerivedSensors0] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors0;
				mDerivedSensors |= ((long)infoMemContents[infoMemMap.idxDerivedSensors1] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors1;
				
				if((infoMemMap.idxDerivedSensors2>=0)&&(infoMemContents[infoMemMap.idxDerivedSensors2] != (byte)infoMemMap.maskDerivedChannelsByte)){ // Check if compatible
					mDerivedSensors |= ((long)infoMemContents[infoMemMap.idxDerivedSensors2] & infoMemMap.maskDerivedChannelsByte) << infoMemMap.byteShiftDerivedSensors2;
				}
			}

			// InfoMem D - End

			//SDLog and LogAndStream
			if(((mFirmwareIdentifier==FW_ID.SHIMMER3.LOGANDSTREAM)||(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG))&&(mInfoMemBytes.length >=384)) {
				
				// InfoMem C - Start - used by SdLog and LogAndStream
				if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
					mMPU9150DMP = (infoMemContents[infoMemMap.idxConfigSetupByte4] >> infoMemMap.bitShiftMPU9150DMP) & infoMemMap.maskMPU9150DMP;
					mMPU9150LPF = (infoMemContents[infoMemMap.idxConfigSetupByte4] >> infoMemMap.bitShiftMPU9150LPF) & infoMemMap.maskMPU9150LPF;
					mMPU9150MotCalCfg =  (infoMemContents[infoMemMap.idxConfigSetupByte4] >> infoMemMap.bitShiftMPU9150MotCalCfg) & infoMemMap.maskMPU9150MotCalCfg;
					
					mMPU9150MPLSamplingRate = (infoMemContents[infoMemMap.idxConfigSetupByte5] >> infoMemMap.bitShiftMPU9150MPLSamplingRate) & infoMemMap.maskMPU9150MPLSamplingRate;
					mMPU9150MagSamplingRate = (infoMemContents[infoMemMap.idxConfigSetupByte5] >> infoMemMap.bitShiftMPU9150MagSamplingRate) & infoMemMap.maskMPU9150MagSamplingRate;
					
					mEnabledSensors += ((long)infoMemContents[infoMemMap.idxSensors3] & 0xFF) << infoMemMap.bitShiftSensors3;
					mEnabledSensors += ((long)infoMemContents[infoMemMap.idxSensors4] & 0xFF) << infoMemMap.bitShiftSensors4;
					
					mMPLSensorFusion = (infoMemContents[infoMemMap.idxConfigSetupByte6] >> infoMemMap.bitShiftMPLSensorFusion) & infoMemMap.maskMPLSensorFusion;
					mMPLGyroCalTC = (infoMemContents[infoMemMap.idxConfigSetupByte6] >> infoMemMap.bitShiftMPLGyroCalTC) & infoMemMap.maskMPLGyroCalTC;
					mMPLVectCompCal = (infoMemContents[infoMemMap.idxConfigSetupByte6] >> infoMemMap.bitShiftMPLVectCompCal) & infoMemMap.maskMPLVectCompCal;
					mMPLMagDistCal = (infoMemContents[infoMemMap.idxConfigSetupByte6] >> infoMemMap.bitShiftMPLMagDistCal) & infoMemMap.maskMPLMagDistCal;
					mMPLEnable = (infoMemContents[infoMemMap.idxConfigSetupByte6] >> infoMemMap.bitShiftMPLEnable) & infoMemMap.maskMPLEnable;
					
					//MPL Accel Calibration Parameters
					bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
					System.arraycopy(infoMemContents, infoMemMap.idxMPLAccelCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
					formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
					AM=new double[9];
					for (int i=0;i<9;i++)
					{
						AM[i]=((double)formattedPacket[6+i])/100;
					}
					double[][] alignmentMatrixMPLA = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
					double[][] sensitivityMatrixMPLA = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
					double[][] offsetVectorMPLA = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
					AlignmentMatrixMPLAccel = alignmentMatrixMPLA; 			
					SensitivityMatrixMPLAccel = sensitivityMatrixMPLA; 	
					OffsetVectorMPLAccel = offsetVectorMPLA;
			
					//MPL Mag Calibration Configuration
					bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
					System.arraycopy(infoMemContents, infoMemMap.idxMPLMagCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
					formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
					AM=new double[9];
					for (int i=0;i<9;i++)
					{
						AM[i]=((double)formattedPacket[6+i])/100;
					}
					double[][] alignmentMatrixMPLMag = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
					double[][] sensitivityMatrixMPLMag = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
					double[][] offsetVectorMPLMag = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
					AlignmentMatrixMPLMag = alignmentMatrixMPLMag; 			
					SensitivityMatrixMPLMag = sensitivityMatrixMPLMag; 	
					OffsetVectorMPLMag = offsetVectorMPLMag;
			
					//MPL Gyro Calibration Configuration
					bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
					System.arraycopy(infoMemContents, infoMemMap.idxMPLGyroCalibration, bufferCalibrationParameters, 0 , infoMemMap.lengthGeneralCalibrationBytes);
					formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
					AM=new double[9];
					for (int i=0;i<9;i++)
					{
						AM[i]=((double)formattedPacket[6+i])/100;
					}
					double[][] alignmentMatrixMPLGyro = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
					double[][] sensitivityMatrixMPLGyro = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
					double[][] offsetVectorMPLGyro = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
					AlignmentMatrixMPLGyro = alignmentMatrixMPLGyro; 			
					SensitivityMatrixMPLGyro = sensitivityMatrixMPLGyro; 	
					OffsetVectorMPLGyro = offsetVectorMPLGyro;
				}
				
				// Shimmer Name
				byte[] shimmerNameBuffer = new byte[infoMemMap.lengthShimmerName];
				System.arraycopy(infoMemContents, infoMemMap.idxSDShimmerName, shimmerNameBuffer, 0 , infoMemMap.lengthShimmerName);
				for(byte b : shimmerNameBuffer) {
					if(!Util.isAsciiPrintable((char)b)) {
						break;
					}
					shimmerName += (char)b;
				}
				
				// Experiment Name
				byte[] experimentNameBuffer = new byte[infoMemMap.lengthExperimentName];
				System.arraycopy(infoMemContents, infoMemMap.idxSDEXPIDName, experimentNameBuffer, 0 , infoMemMap.lengthExperimentName);
				String experimentName = "";
				for(byte b : experimentNameBuffer) {
					if(!Util.isAsciiPrintable((char)b)) {
						break;
					}
					experimentName += (char)b;
				}
				mExperimentName = new String(experimentName);
	
				//Configuration Time
				int bitShift = (infoMemMap.lengthConfigTimeBytes-1) * 8;
				mConfigTime = 0;
				for(int x=0; x<infoMemMap.lengthConfigTimeBytes; x++ ) {
					mConfigTime += (((long)(infoMemContents[infoMemMap.idxSDConfigTime0+x] & 0xFF)) << bitShift);
					bitShift -= 8;
				}
//				//if ConfigTime is all F's, reset the time to 0 
//				if((mConfigTime&(2^32)) == (2^32)) {
//					mConfigTime = 0;
//				}

				if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
					mExperimentId = infoMemContents[infoMemMap.idxSDMyTrialID] & 0xFF;
					mExperimentNumberOfShimmers = infoMemContents[infoMemMap.idxSDNumOfShimmers] & 0xFF;
				}
				
				mButtonStart = (infoMemContents[infoMemMap.idxSDExperimentConfig0] >> infoMemMap.bitShiftButtonStart) & infoMemMap.maskButtonStart;

				if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
					mSyncWhenLogging = (infoMemContents[infoMemMap.idxSDExperimentConfig0] >> infoMemMap.bitShiftTimeSyncWhenLogging) & infoMemMap.maskTimeSyncWhenLogging;
					mMasterShimmer = (infoMemContents[infoMemMap.idxSDExperimentConfig0] >> infoMemMap.bitShiftMasterShimmer) & infoMemMap.maskTimeMasterShimmer;
					mSingleTouch = (infoMemContents[infoMemMap.idxSDExperimentConfig1] >> infoMemMap.bitShiftSingleTouch) & infoMemMap.maskTimeSingleTouch;
					mTCXO = (infoMemContents[infoMemMap.idxSDExperimentConfig1] >> infoMemMap.bitShiftTCX0) & infoMemMap.maskTimeTCX0;
					
					mSyncBroadcastInterval = (int)(infoMemContents[infoMemMap.idxSDBTInterval] & 0xFF);
					
					// Maximum and Estimated Length in minutes
					mExperimentDurationEstimated =  ((int)(infoMemContents[infoMemMap.idxEstimatedExpLengthLsb] & 0xFF) + (((int)(infoMemContents[infoMemMap.idxEstimatedExpLengthMsb] & 0xFF)) << 8));
					mExperimentDurationMaximum =  ((int)(infoMemContents[infoMemMap.idxMaxExpLengthLsb] & 0xFF) + (((int)(infoMemContents[infoMemMap.idxMaxExpLengthMsb] & 0xFF)) << 8));
				}
					
				byte[] macIdBytes = new byte[infoMemMap.lengthMacIdBytes];
				System.arraycopy(infoMemContents, infoMemMap.idxMacAddress, macIdBytes, 0 , infoMemMap.lengthMacIdBytes);
				mMacIdFromInfoMem = Util.bytesToHex(macIdBytes);
				

				if(((infoMemContents[infoMemMap.idxSDConfigDelayFlag]>>infoMemMap.bitShiftSDCfgFileWriteFlag)&infoMemMap.maskSDCfgFileWriteFlag) == infoMemMap.maskSDCfgFileWriteFlag) {
					mConfigFileCreationFlag = true;
				}
				else {
					mConfigFileCreationFlag = false;
				}
				if(((infoMemContents[infoMemMap.idxSDConfigDelayFlag]>>infoMemMap.bitShiftSDCalibFileWriteFlag)&infoMemMap.maskSDCalibFileWriteFlag) == infoMemMap.maskSDCalibFileWriteFlag) {
					mCalibFileCreationFlag = true;
				}
				else {
					mCalibFileCreationFlag = false;
				}

				// InfoMem C - End
					
				if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
					// InfoMem B Start -> Slave MAC ID for Multi-Shimmer Syncronisation
					syncNodesList.clear();
					for (int i = 0; i < infoMemMap.maxNumOfExperimentNodes; i++) {
						System.arraycopy(infoMemContents, infoMemMap.idxNode0 + (i*infoMemMap.lengthMacIdBytes), macIdBytes, 0 , infoMemMap.lengthMacIdBytes);
						if(Arrays.equals(macIdBytes, infoMemMap.invalidMacId)) {
//						if(Arrays.equals(macIdBytes, new byte[]{-1,-1,-1,-1,-1,-1})) {
							break;
						}
						else {
							syncNodesList.add(Util.bytesToHex(macIdBytes));
						}
					}
					// InfoMem B End
				}
			}
			
			//TODO Complete and tidy below
			sensorAndConfigMapsCreate();
			sensorMapUpdateWithEnabledSensors();
			
//			sensorMapCheckandCorrectSensorDependencies();
		}
		
		// Set name if nothing was read from InfoMem
		if(!shimmerName.isEmpty()) {
			mShimmerUserAssignedName = new String(shimmerName);
		}
		else {
			mShimmerUserAssignedName = DEFAULT_SHIMMER_NAME;
			if(!mMacIdFromUartParsed.isEmpty()) {
				mShimmerUserAssignedName += "_" + mMacIdFromUartParsed;
			}
		}
		
	}

	/**
	 * Generate the Shimmer's Information Memory byte array based on the
	 * settings stored in ShimmerObject. These bytes can then be written to the
	 * Shimmer via the Shimmer Dock/Consensys Base. The Information Memory is is
	 * a region of the Shimmer's inbuilt RAM where all configuration information
	 * is stored.
	 * 
	 * @param generateForWritingToShimmer
	 * @return
	 */
	protected byte[] infoMemByteArrayGenerate(boolean generateForWritingToShimmer) {

		InfoMemLayout infoMemMap = new InfoMemLayout(mFirmwareIdentifier, mFirmwareVersionMajor, mFirmwareVersionMinor, mFirmwareVersionInternal);
		
		byte[] infoMemBackup = mInfoMemBytes.clone();
		
		mInfoMemBytes = new byte[infoMemMap.mInfoMemSize];
//		mShimmerInfoMemBytes = createEmptyInfoMemByteArray(getExpectedInfoMemByteLength());
		
		// InfoMem defaults to 0xFF on firmware flash
		for(int i =0; i < mInfoMemBytes.length; i++) {
			mInfoMemBytes[i] = (byte) 0xFF;
		}
		
		// If not being generated from scratch then copy across exisiting InfoMem contents
		if(!generateForWritingToShimmer) {
			System.arraycopy(infoMemBackup, 0, mInfoMemBytes, 0, (infoMemBackup.length > mInfoMemBytes.length) ? mInfoMemBytes.length:infoMemBackup.length);
		}	
		
		
		// InfoMem D - Start - used by BtStream, SdLog and LogAndStream
		// Sampling Rate
		int samplingRate = (int)(32768 / mShimmerSamplingRate);
		mInfoMemBytes[infoMemMap.idxShimmerSamplingRate] = (byte) (samplingRate & infoMemMap.maskShimmerSamplingRate); 
		mInfoMemBytes[infoMemMap.idxShimmerSamplingRate+1] = (byte) ((samplingRate >> 8) & infoMemMap.maskShimmerSamplingRate); 

		//FW not using this feature and BtStream will reject infomem if this isn't set to '1'
		mInfoMemBytes[infoMemMap.idxBufferSize] = (byte) 1;//(byte) (mBufferSize & infoMemMap.maskBufferSize); 
		
		// Sensors
		//TODO: check version compatible?
		mEnabledSensors = (long)0;
		sensorMapCheckandCorrectHwDependencies();
		for(Integer key:mSensorMap.keySet()) {
			if(mSensorMap.get(key).mIsEnabled) {
				mEnabledSensors |= mSensorMap.get(key).mSensorBitmapIDSDLogHeader;
			}
		}
		mInfoMemBytes[infoMemMap.idxSensors0] = (byte) ((mEnabledSensors >> infoMemMap.byteShiftSensors0) & infoMemMap.maskSensors);
		mInfoMemBytes[infoMemMap.idxSensors1] = (byte) ((mEnabledSensors >> infoMemMap.byteShiftSensors1) & infoMemMap.maskSensors);
		mInfoMemBytes[infoMemMap.idxSensors2] = (byte) ((mEnabledSensors >> infoMemMap.byteShiftSensors2) & infoMemMap.maskSensors);
//		setDefaultConfigForDisabledSensors();
		
		// Configuration
		mInfoMemBytes[infoMemMap.idxConfigSetupByte0] = (byte) ((mLSM303DigitalAccelRate & infoMemMap.maskLSM303DLHCAccelSamplingRate) << infoMemMap.bitShiftLSM303DLHCAccelSamplingRate);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte0] |= (byte) ((mAccelRange & infoMemMap.maskLSM303DLHCAccelRange) << infoMemMap.bitShiftLSM303DLHCAccelRange);
		if(mLowPowerAccelWR) {
			mInfoMemBytes[infoMemMap.idxConfigSetupByte0] |= (infoMemMap.maskLSM303DLHCAccelLPM << infoMemMap.bitShiftLSM303DLHCAccelLPM);
		}
		if(mHighResAccelWR) {
			mInfoMemBytes[infoMemMap.idxConfigSetupByte0] |= (infoMemMap.maskLSM303DLHCAccelHRM << infoMemMap.bitShiftLSM303DLHCAccelHRM);
		}

		mInfoMemBytes[infoMemMap.idxConfigSetupByte1] = (byte) ((mMPU9150GyroAccelRate & infoMemMap.maskMPU9150AccelGyroSamplingRate) << infoMemMap.bitShiftMPU9150AccelGyroSamplingRate);

		mInfoMemBytes[infoMemMap.idxConfigSetupByte2] = (byte) ((mMagRange & infoMemMap.maskLSM303DLHCMagRange) << infoMemMap.bitShiftLSM303DLHCMagRange);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte2] |= (byte) ((mLSM303MagRate & infoMemMap.maskLSM303DLHCMagSamplingRate) << infoMemMap.bitShiftLSM303DLHCMagSamplingRate);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte2] |= (byte) ((mGyroRange & infoMemMap.maskMPU9150GyroRange) << infoMemMap.bitShiftMPU9150GyroRange);
		
		mInfoMemBytes[infoMemMap.idxConfigSetupByte3] = (byte) ((mMPU9150AccelRange & infoMemMap.maskMPU9150AccelRange) << infoMemMap.bitShiftMPU9150AccelRange);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte3] |= (byte) ((mPressureResolution & infoMemMap.maskBMP180PressureResolution) << infoMemMap.bitShiftBMP180PressureResolution);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte3] |= (byte) ((mGSRRange & infoMemMap.maskGSRRange) << infoMemMap.bitShiftGSRRange);
		mInfoMemBytes[infoMemMap.idxConfigSetupByte3] |= (byte) ((mInternalExpPower & infoMemMap.maskEXPPowerEnable) << infoMemMap.bitShiftEXPPowerEnable);
		
		//EXG Configuration
		//update mEXG1Register and mEXG2Register 
		exgBytesGetFromConfig();
		System.arraycopy(mEXG1RegisterArray, 0, mInfoMemBytes, infoMemMap.idxEXGADS1292RChip1Config1, 10);
		System.arraycopy(mEXG2RegisterArray, 0, mInfoMemBytes, infoMemMap.idxEXGADS1292RChip2Config1, 10);
//		exgBytesGetConfigFrom(1, mEXG1Register);
//		exgBytesGetConfigFrom(2, mEXG2Register);
		
		mInfoMemBytes[infoMemMap.idxBtCommBaudRate] = (byte) (mBluetoothBaudRate & infoMemMap.maskBaudRate);

		// Analog Accel Calibration Parameters
		byte[] bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
		// offsetVector -> buffer offset = 0
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[0+(i*2)] = (byte) ((((int)mOffsetVectorAnalogAccel[i][0]) >> 0) & 0xFF);
			bufferCalibrationParameters[0+(i*2)+1] = (byte) ((((int)mOffsetVectorAnalogAccel[i][0]) >> 8) & 0xFF);
		}
		// sensitivityMatrix -> buffer offset = 6
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[6+(i*2)] = (byte) ((((int)mSensitivityMatrixAnalogAccel[i][i]) >> 0) & 0xFF);
			bufferCalibrationParameters[6+(i*2)+1] = (byte) ((((int)mSensitivityMatrixAnalogAccel[i][i]) >> 8) & 0xFF);
		}
		// alignmentMatrix -> buffer offset = 12
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[12+(i*3)] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][0]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+1] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][1]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+2] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][2]*100)) & 0xFF);
		}
		System.arraycopy(bufferCalibrationParameters, 0, mInfoMemBytes, infoMemMap.idxAnalogAccelCalibration, infoMemMap.lengthGeneralCalibrationBytes);

		// MPU9150 Gyroscope Calibration Parameters
		bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
		// offsetVector -> buffer offset = 0
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[0+(i*2)] = (byte) ((((int)mOffsetVectorGyroscope[i][0]) >> 0) & 0xFF);
			bufferCalibrationParameters[0+(i*2)+1] = (byte) ((((int)mOffsetVectorGyroscope[i][0]) >> 8) & 0xFF);
		}
		// sensitivityMatrix -> buffer offset = 6
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[6+(i*2)] = (byte) ((((int)mSensitivityMatrixGyroscope[i][i]) >> 0) & 0xFF);
			bufferCalibrationParameters[6+(i*2)+1] = (byte) ((((int)mSensitivityMatrixGyroscope[i][i]) >> 8) & 0xFF);
		}
		// alignmentMatrix -> buffer offset = 12
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[12+(i*3)] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][0]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+1] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][1]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+2] = (byte) (((int)(mAlignmentMatrixAnalogAccel[i][2]*100)) & 0xFF);
		}
		System.arraycopy(bufferCalibrationParameters, 0, mInfoMemBytes, infoMemMap.idxMPU9150GyroCalibration, infoMemMap.lengthGeneralCalibrationBytes);

		// LSM303DLHC Magnetometer Calibration Parameters
		bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
		// offsetVector -> buffer offset = 0
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[0+(i*2)] = (byte) ((((int)mOffsetVectorMagnetometer[i][0]) >> 0) & 0xFF);
			bufferCalibrationParameters[0+(i*2)+1] = (byte) ((((int)mOffsetVectorMagnetometer[i][0]) >> 8) & 0xFF);
		}
		// sensitivityMatrix -> buffer offset = 6
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[6+(i*2)] = (byte) ((((int)mSensitivityMatrixMagnetometer[i][i]) >> 0) & 0xFF);
			bufferCalibrationParameters[6+(i*2)+1] = (byte) ((((int)mSensitivityMatrixMagnetometer[i][i]) >> 8) & 0xFF);
		}
		// alignmentMatrix -> buffer offset = 12
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[12+(i*3)] = (byte) (((int)(mAlignmentMatrixMagnetometer[i][0]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+1] = (byte) (((int)(mAlignmentMatrixMagnetometer[i][1]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+2] = (byte) (((int)(mAlignmentMatrixMagnetometer[i][2]*100)) & 0xFF);
		}
		System.arraycopy(bufferCalibrationParameters, 0, mInfoMemBytes, infoMemMap.idxLSM303DLHCMagCalibration, infoMemMap.lengthGeneralCalibrationBytes);

		// LSM303DLHC Digital Accel Calibration Parameters
//		bufferCalibrationParameters = new byte[infoMemMap.lengthGeneralCalibrationBytes];
		bufferCalibrationParameters = generateCalParamLSM303DLHCAccel();
		System.arraycopy(bufferCalibrationParameters, 0, mInfoMemBytes, infoMemMap.idxLSM303DLHCAccelCalibration, infoMemMap.lengthGeneralCalibrationBytes);
		
		//TODO: decide what to do
		// BMP180 Pressure Calibration Parameters
		
		// Derived Sensors
		mDerivedSensors = (long)0;
		if((infoMemMap.idxDerivedSensors0>=0)&&(infoMemMap.idxDerivedSensors1>=0)) { // Check if compatible
			for(Integer key:mSensorMap.keySet()) {
				if(mSensorMap.get(key).mIsEnabled) {
					mDerivedSensors |= mSensorMap.get(key).mDerivedSensorBitmapID;
				}
			}
			mInfoMemBytes[infoMemMap.idxDerivedSensors0] = (byte) ((mDerivedSensors >> infoMemMap.byteShiftDerivedSensors0) & infoMemMap.maskDerivedChannelsByte);
			mInfoMemBytes[infoMemMap.idxDerivedSensors1] = (byte) ((mDerivedSensors >> infoMemMap.byteShiftDerivedSensors1) & infoMemMap.maskDerivedChannelsByte);
			if(infoMemMap.idxDerivedSensors2>=0) { // Check if compatible
				mInfoMemBytes[infoMemMap.idxDerivedSensors2] = (byte) ((mDerivedSensors >> infoMemMap.byteShiftDerivedSensors2) & infoMemMap.maskDerivedChannelsByte);
			}

		}
		
		// InfoMem D - End

		
		//TODO: Add full FW version checking here to support future changes to FW
		//SDLog and LogAndStream
		if(((mFirmwareIdentifier==FW_ID.SHIMMER3.LOGANDSTREAM)||(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG))&&(mInfoMemBytes.length >=384)) {

			// InfoMem C - Start - used by SdLog and LogAndStream
			if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
				mInfoMemBytes[infoMemMap.idxConfigSetupByte4] = (byte) ((mMPU9150DMP & infoMemMap.maskMPU9150DMP) << infoMemMap.bitShiftMPU9150DMP);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte4] |= (byte) ((mMPU9150LPF & infoMemMap.maskMPU9150LPF) << infoMemMap.bitShiftMPU9150LPF);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte4] |= (byte) ((mMPU9150MotCalCfg & infoMemMap.maskMPU9150MotCalCfg) << infoMemMap.bitShiftMPU9150MotCalCfg);

				mInfoMemBytes[infoMemMap.idxConfigSetupByte5] = (byte) ((mMPU9150MPLSamplingRate & infoMemMap.maskMPU9150MPLSamplingRate) << infoMemMap.bitShiftMPU9150MPLSamplingRate);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte5] |= (byte) ((mMPU9150MagSamplingRate & infoMemMap.maskMPU9150MPLSamplingRate) << infoMemMap.bitShiftMPU9150MagSamplingRate);

				mInfoMemBytes[infoMemMap.idxSensors3] = (byte) ((mEnabledSensors >> infoMemMap.bitShiftSensors3) & 0xFF);
				mInfoMemBytes[infoMemMap.idxSensors4] = (byte) ((mEnabledSensors >> infoMemMap.bitShiftSensors4) & 0xFF);

				mInfoMemBytes[infoMemMap.idxConfigSetupByte6] = (byte) ((mMPLSensorFusion & infoMemMap.maskMPLSensorFusion) << infoMemMap.bitShiftMPLSensorFusion);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte6] |= (byte) ((mMPLGyroCalTC & infoMemMap.maskMPLGyroCalTC) << infoMemMap.bitShiftMPLGyroCalTC);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte6] |= (byte) ((mMPLVectCompCal & infoMemMap.maskMPLVectCompCal) << infoMemMap.bitShiftMPLVectCompCal);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte6] |= (byte) ((mMPLMagDistCal & infoMemMap.maskMPLMagDistCal) << infoMemMap.bitShiftMPLMagDistCal);
				mInfoMemBytes[infoMemMap.idxConfigSetupByte6] |= (byte) ((mMPLEnable & infoMemMap.maskMPLEnable) << infoMemMap.bitShiftMPLEnable);
				
				//TODO: decide what to do
				//MPL Accel Calibration Parameters
				//MPL Mag Calibration Configuration
				//MPL Gyro Calibration Configuration
			}

			// Shimmer Name
			for (int i = 0; i < infoMemMap.lengthShimmerName; i++) {
				if (i < mShimmerUserAssignedName.length()) {
					mInfoMemBytes[infoMemMap.idxSDShimmerName + i] = (byte) mShimmerUserAssignedName.charAt(i);
				}
				else {
					mInfoMemBytes[infoMemMap.idxSDShimmerName + i] = (byte) 0xFF;
				}
			}
			
			// Experiment Name
			for (int i = 0; i < infoMemMap.lengthExperimentName; i++) {
				if (i < mExperimentName.length()) {
					mInfoMemBytes[infoMemMap.idxSDEXPIDName + i] = (byte) mExperimentName.charAt(i);
				}
				else {
					mInfoMemBytes[infoMemMap.idxSDEXPIDName + i] = (byte) 0xFF;
				}
			}

			//Configuration Time
			mInfoMemBytes[infoMemMap.idxSDConfigTime0] = (byte) ((mConfigTime >> infoMemMap.bitShiftSDConfigTime0) & 0xFF);
			mInfoMemBytes[infoMemMap.idxSDConfigTime1] = (byte) ((mConfigTime >> infoMemMap.bitShiftSDConfigTime1) & 0xFF);
			mInfoMemBytes[infoMemMap.idxSDConfigTime2] = (byte) ((mConfigTime >> infoMemMap.bitShiftSDConfigTime2) & 0xFF);
			mInfoMemBytes[infoMemMap.idxSDConfigTime3] = (byte) ((mConfigTime >> infoMemMap.bitShiftSDConfigTime3) & 0xFF);
			
			if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
				mInfoMemBytes[infoMemMap.idxSDMyTrialID] = (byte) (mExperimentId & 0xFF);
	
				mInfoMemBytes[infoMemMap.idxSDNumOfShimmers] = (byte) (mExperimentNumberOfShimmers & 0xFF);
			}
			
			mInfoMemBytes[infoMemMap.idxSDExperimentConfig0] = (byte) ((mButtonStart & infoMemMap.maskButtonStart) << infoMemMap.bitShiftButtonStart);
			
			if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
				mInfoMemBytes[infoMemMap.idxSDExperimentConfig0] |= (byte) ((mSyncWhenLogging & infoMemMap.maskTimeSyncWhenLogging) << infoMemMap.bitShiftTimeSyncWhenLogging);
				mInfoMemBytes[infoMemMap.idxSDExperimentConfig0] |= (byte) ((mMasterShimmer & infoMemMap.maskTimeMasterShimmer) << infoMemMap.bitShiftMasterShimmer);
				
				mInfoMemBytes[infoMemMap.idxSDExperimentConfig1] = (byte) ((mSingleTouch & infoMemMap.maskTimeSingleTouch) << infoMemMap.bitShiftSingleTouch);
				mInfoMemBytes[infoMemMap.idxSDExperimentConfig1] |= (byte) ((mTCXO & infoMemMap.maskTimeTCX0) << infoMemMap.bitShiftTCX0);
			
				mInfoMemBytes[infoMemMap.idxSDBTInterval] = (byte) (mSyncBroadcastInterval & 0xFF);
			
				// Maximum and Estimated Length in minutes
				mInfoMemBytes[infoMemMap.idxEstimatedExpLengthLsb] = (byte) ((mExperimentDurationEstimated >> 0) & 0xFF);
				mInfoMemBytes[infoMemMap.idxEstimatedExpLengthMsb] = (byte) ((mExperimentDurationEstimated >> 8) & 0xFF);
				mInfoMemBytes[infoMemMap.idxMaxExpLengthLsb] = (byte) ((mExperimentDurationMaximum >> 0) & 0xFF);
				mInfoMemBytes[infoMemMap.idxMaxExpLengthMsb] = (byte) ((mExperimentDurationMaximum >> 8) & 0xFF);
			}
			
			if(generateForWritingToShimmer) {
				// MAC address - set to all 0xFF (i.e. invalid MAC) so that Firmware will know to check for MAC from Bluetooth transceiver
				// (already set to 0xFF at start of method but just incase)
				System.arraycopy(infoMemMap.invalidMacId, 0, mInfoMemBytes, infoMemMap.idxMacAddress, infoMemMap.lengthMacIdBytes);

				//TODO base below off mCalibFileCreationFlag and mCfgFileCreationFlag global variables? 
				
				 // Tells the Shimmer to create a new config file on undock/power cycle
				mInfoMemBytes[infoMemMap.idxSDConfigDelayFlag] = (byte) (infoMemMap.maskSDCfgFileWriteFlag << infoMemMap.bitShiftSDCfgFileWriteFlag);

				//TODO decide what to do about calibration info
				//mInfoMemBytes[infoMemMap.idxSDConfigDelayFlag] = (byte) (infoMemMap.maskSDCalibFileWriteFlag << infoMemMap.bitShiftSDCalibFileWriteFlag);
			}
			// InfoMem C - End
				
			if(mFirmwareIdentifier==FW_ID.SHIMMER3.SDLOG) {
				// InfoMem B Start -> Slave MAC ID for Multi-Shimmer Syncronisation
				for (int i = 0; i < infoMemMap.maxNumOfExperimentNodes; i++) { // Limit of 21 nodes
					byte[] macIdArray;
					if((syncNodesList.size()>0) && (i<syncNodesList.size()) && (mSyncWhenLogging>0)) {
						macIdArray = Util.hexStringToByteArray(syncNodesList.get(i));
					}
					else {
						macIdArray = infoMemMap.invalidMacId;
					}
					System.arraycopy(macIdArray, 0, mInfoMemBytes, infoMemMap.idxNode0 + (i*infoMemMap.lengthMacIdBytes), infoMemMap.lengthMacIdBytes);
				}
				// InfoMem B End
			}
		}
		return mInfoMemBytes;
	}
	

	/**
	 * Creates an empty byte array for the purposes of generating the
	 * configuration bytes to write to the Shimmer (default all bytes = 0xFF).
	 * 
	 * @param size the size of the byte array to create.
	 * @return byte array
	 */
	public byte[] createEmptyInfoMemByteArray(int size) {
		byte[] newArray = new byte[size];
		for(byte b:newArray) {
			b = (byte)0xFF;
		}
		return newArray;
	}

	/**
	 * Parses the LSM303DLHC Accel calibration variables from a byte array stored
	 * in the Shimmer's infomem or in the SD header of logged data files.
	 * 
	 * @param bufferCalibrationParameters
	 *            the byte array containing the LSM303DLHC Accel calibration
	 */
	public void parseCalParamLSM303DLHCAccel(byte[] bufferCalibrationParameters){
		String[] dataType={"i16","i16","i16","i16","i16","i16","i8","i8","i8","i8","i8","i8","i8","i8","i8"};
		int[] formattedPacket = formatdatapacketreverse(bufferCalibrationParameters,dataType);
		double[] AM=new double[9];
		for (int i=0;i<9;i++)
		{
			AM[i]=((double)formattedPacket[6+i])/100;
		}
		double[][] alignmentMatrixDA = {{AM[0],AM[1],AM[2]},{AM[3],AM[4],AM[5]},{AM[6],AM[7],AM[8]}}; 				
		double[][] sensitivityMatrixDA = {{formattedPacket[3],0,0},{0,formattedPacket[4],0},{0,0,formattedPacket[5]}}; 
		double[][] offsetVectorDA = {{formattedPacket[0]},{formattedPacket[1]},{formattedPacket[2]}};
		mAlignmentMatrixWRAccel = alignmentMatrixDA; 			
		mSensitivityMatrixWRAccel = sensitivityMatrixDA; 	
		mOffsetVectorWRAccel = offsetVectorDA;
	}

	/**
	 * Converts the LSM303DLHC Accel calibration variables from Shimmer Object
	 * into a byte array for sending to the Shimmer.
	 * 
	 * @return the bytes array containing the LSM303DLHC Accel calibration
	 */
	public byte[] generateCalParamLSM303DLHCAccel(){
		byte[] bufferCalibrationParameters = new byte[21];
		// offsetVector -> buffer offset = 0
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[0+(i*2)] = (byte) ((((int)mOffsetVectorWRAccel[i][0]) >> 0) & 0xFF);
			bufferCalibrationParameters[0+(i*2)+1] = (byte) ((((int)mOffsetVectorWRAccel[i][0]) >> 8) & 0xFF);
		}
		// sensitivityMatrix -> buffer offset = 6
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[6+(i*2)] = (byte) ((((int)mSensitivityMatrixWRAccel[i][i]) >> 0) & 0xFF);
			bufferCalibrationParameters[6+(i*2)+1] = (byte) ((((int)mSensitivityMatrixWRAccel[i][i]) >> 8) & 0xFF);
		}
		// alignmentMatrix -> buffer offset = 12
		for (int i=0; i<3; i++) {
			bufferCalibrationParameters[12+(i*3)] = (byte) (((int)(mAlignmentMatrixWRAccel[i][0]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+1] = (byte) (((int)(mAlignmentMatrixWRAccel[i][1]*100)) & 0xFF);
			bufferCalibrationParameters[12+(i*3)+2] = (byte) (((int)(mAlignmentMatrixWRAccel[i][2]*100)) & 0xFF);
		}
		return bufferCalibrationParameters;
	}

	/**
	 * This method is used as the basis for configuring a Shimmer through the
	 * Consensys application. All sensor and configuration information is stored
	 * in dynamically created and controlled Maps.
	 * 
	 * Should only be used after the Shimmer HW and FW version information is
	 * set
	 */
	public void sensorAndConfigMapsCreate() {
		
		mSensorMap = new TreeMap<Integer,SensorDetails>();
		mSensorTileMap = new LinkedHashMap<String,SensorTileDetails>();
		mConfigOptionsMap = new HashMap<String,SensorConfigOptionDetails>();
		
		if (mHardwareVersion != -1){
			if (mHardwareVersion == HW_ID.SHIMMER_2R){
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.ACCEL, new SensorDetails(false, 0x80, 0, "Accelerometer"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.GYRO, new SensorDetails(false, 0x40, 0, "Gyroscope"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.MAG, new SensorDetails(false, 0x20, 0, "Magnetometer"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.EMG, new SensorDetails(false, 0x08, 0, "EMG"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.ECG, new SensorDetails(false, 0x10, 0, "ECG"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.GSR, new SensorDetails(false, 0x04, 0, "GSR"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A7, new SensorDetails(false, 0x02, 0, "Exp Board A7"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A0, new SensorDetails(false, 0x01, 0, "Exp Board A0"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.EXP_BOARD, new SensorDetails(false, 0x02|0x01, 0, "Exp Board"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP, new SensorDetails(false, 0x8000, 0, "Bridge Amplifier"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.HEART, new SensorDetails(false, 0x4000, 0, "Heart Rate"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.BATT, new SensorDetails(false, 0x2000, 0, "Battery Voltage"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.EXT_ADC_A15, new SensorDetails(false, 0x0800, 0, "External ADC A15"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.INT_ADC_A1, new SensorDetails(false, 0x0400, 0, "Internal ADC A1"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.INT_ADC_A12, new SensorDetails(false, 0x0200, 0, "Internal ADC A12"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.INT_ADC_A13, new SensorDetails(false, 0x0100, 0, "Internal ADC A13"));
				mSensorMap.put(Configuration.Shimmer2.SensorMapKey.INT_ADC_A14, new SensorDetails(false, 0x800000, 0, "Internal ADC A14"));
				
				// Conflicting Channels
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.GYRO).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.ECG,
					Configuration.Shimmer2.SensorMapKey.EMG,
					Configuration.Shimmer2.SensorMapKey.GSR,
					Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.MAG).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.ECG,
					Configuration.Shimmer2.SensorMapKey.EMG,
					Configuration.Shimmer2.SensorMapKey.GSR,
					Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.EMG).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.GSR,
					Configuration.Shimmer2.SensorMapKey.ECG,
					Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP,
					Configuration.Shimmer2.SensorMapKey.GYRO,
					Configuration.Shimmer2.SensorMapKey.MAG);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.ECG).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.GSR,
					Configuration.Shimmer2.SensorMapKey.EMG,
					Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP,
					Configuration.Shimmer2.SensorMapKey.GYRO,
					Configuration.Shimmer2.SensorMapKey.MAG);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.GSR).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.ECG,
					Configuration.Shimmer2.SensorMapKey.EMG,
					Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP,
					Configuration.Shimmer2.SensorMapKey.GYRO,
					Configuration.Shimmer2.SensorMapKey.MAG);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A7).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.HEART,
					Configuration.Shimmer2.SensorMapKey.BATT);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A0).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.HEART,
					Configuration.Shimmer2.SensorMapKey.BATT);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.BRIDGE_AMP).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.ECG,
					Configuration.Shimmer2.SensorMapKey.EMG,
					Configuration.Shimmer2.SensorMapKey.GSR,
					Configuration.Shimmer2.SensorMapKey.GYRO,
					Configuration.Shimmer2.SensorMapKey.MAG);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.HEART).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A0,
					Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A7);
				mSensorMap.get(Configuration.Shimmer2.SensorMapKey.BATT).mListOfSensorMapKeysConflicting = Arrays.asList(
					Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A0,
					Configuration.Shimmer2.SensorMapKey.EXP_BOARD_A7);

			} 
			else if (mHardwareVersion == HW_ID.SHIMMER_3) {
				InfoMemLayout infoMemMap = new InfoMemLayout(mFirmwareIdentifier, mFirmwareVersionMajor, mFirmwareVersionMinor, mFirmwareVersionInternal);
				
				// These can be used to enable/disble GUI options depending on what HW, FW, Expansion boards versions are present
				ShimmerVerObject baseAnyIntExpBoardAndFw = 			new ShimmerVerObject(HW_ID.SHIMMER_3,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION);
				ShimmerVerObject baseAnyIntExpBoardAndSdlog = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION);
				ShimmerVerObject baseAnyIntExpBoardAndBtStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION);
				ShimmerVerObject baseAnyIntExpBoardAndLogAndStream = new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION);

				ShimmerVerObject baseNoIntExpBoardSdLog = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,ShimmerVerDetails.EXP_BRD_NONE_ID);

				ShimmerVerObject baseSdLog = 				new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,ANY_VERSION);
				ShimmerVerObject baseBtStream = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,ANY_VERSION);
				ShimmerVerObject baseLogAndStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,ANY_VERSION);
				
				ShimmerVerObject baseExgSdLog = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_EXG); 
				ShimmerVerObject baseExgUnifiedSdLog = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_EXG_UNIFIED);
				ShimmerVerObject baseExgBtStream = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_EXG);
				ShimmerVerObject baseExgUnifiedBtStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_EXG_UNIFIED);
				ShimmerVerObject baseExgLogAndStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_EXG);
				ShimmerVerObject baseExgUnifiedLogAndStream = new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_EXG_UNIFIED);
				
				ShimmerVerObject baseGsrSdLog = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_GSR);
				ShimmerVerObject baseGsrUnifiedSdLog = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_GSR_UNIFIED);
				ShimmerVerObject baseGsrBtStream = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_GSR);
				ShimmerVerObject baseGsrUnifiedBtStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_GSR_UNIFIED);
				ShimmerVerObject baseGsrLogAndStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_GSR);
				ShimmerVerObject baseGsrUnifiedLogAndStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_GSR_UNIFIED);
				ShimmerVerObject baseGsrGq = 				new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.GQ_GSR,ANY_VERSION,ANY_VERSION,ANY_VERSION,HW_ID_SR_CODES.EXP_BRD_GSR);
				ShimmerVerObject baseGsrUnifiedGq = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.GQ_GSR,ANY_VERSION,ANY_VERSION,ANY_VERSION,HW_ID_SR_CODES.EXP_BRD_GSR_UNIFIED);

				ShimmerVerObject baseBrAmpSdLog = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_BR_AMP);
				ShimmerVerObject baseBrAmpUnifiedSdLog = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_BR_AMP_UNIFIED);
				ShimmerVerObject baseBrAmpBtStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_BR_AMP);
				ShimmerVerObject baseBrAmpUnifiedBtStream = new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_BR_AMP_UNIFIED);
				ShimmerVerObject baseBrAmpLogAndStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_BR_AMP);
				ShimmerVerObject baseBrAmpUnifiedLogAndStream = new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_BR_AMP_UNIFIED);
				
				ShimmerVerObject baseProto3MiniSdLog = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_PROTO3_MINI);
				ShimmerVerObject baseProto3MiniBtStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_PROTO3_MINI);
				ShimmerVerObject baseProto3MiniLogAndStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_PROTO3_MINI);

				ShimmerVerObject baseProto3DeluxeSdLog = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_PROTO3_DELUXE);
				ShimmerVerObject baseProto3DeluxeBtStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_PROTO3_DELUXE);
				ShimmerVerObject baseProto3DeluxeLogAndStream =	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_PROTO3_DELUXE);

				ShimmerVerObject baseHighGAccelSdLog = 			new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.SDLOG,0,8,0,HW_ID_SR_CODES.EXP_BRD_HIGH_G_ACCEL);
				ShimmerVerObject baseHighGAccelBtStream = 		new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.BTSTREAM,0,5,0,HW_ID_SR_CODES.EXP_BRD_HIGH_G_ACCEL);
				ShimmerVerObject baseHighGAccelLogAndStream = 	new ShimmerVerObject(HW_ID.SHIMMER_3,FW_ID.SHIMMER3.LOGANDSTREAM,0,3,3,HW_ID_SR_CODES.EXP_BRD_HIGH_G_ACCEL);

				List<ShimmerVerObject> listOfCompatibleVersionInfoExg = Arrays.asList(
						baseExgSdLog, baseExgBtStream, baseExgLogAndStream,  
						baseExgUnifiedSdLog, baseExgUnifiedBtStream, baseExgUnifiedLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoSdLog = Arrays.asList(baseSdLog);
				List<ShimmerVerObject> listOfCompatibleVersionInfoAnyExpBoardAndFw = Arrays.asList(baseAnyIntExpBoardAndFw);
				List<ShimmerVerObject> listOfCompatibleVersionInfoAnyExpBoardStandardFW = Arrays.asList(
						baseAnyIntExpBoardAndSdlog,baseAnyIntExpBoardAndBtStream,baseAnyIntExpBoardAndLogAndStream);

				List<ShimmerVerObject> listOfCompatibleVersionInfoGsr = Arrays.asList(
						baseGsrSdLog, baseGsrBtStream, baseGsrLogAndStream, baseGsrGq,
						baseGsrUnifiedSdLog,  baseGsrUnifiedBtStream, baseGsrUnifiedLogAndStream, baseGsrUnifiedGq);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoBrAmp = Arrays.asList(
						baseBrAmpSdLog, baseBrAmpBtStream, baseBrAmpLogAndStream,  
						baseBrAmpUnifiedSdLog,  baseBrAmpUnifiedBtStream, baseBrAmpUnifiedLogAndStream);

				List<ShimmerVerObject> listOfCompatibleVersionInfoProto3Mini = Arrays.asList(
						baseProto3MiniSdLog, baseProto3MiniBtStream, baseProto3MiniLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoProto3Deluxe = Arrays.asList(
						baseProto3DeluxeSdLog, baseProto3DeluxeBtStream, baseProto3DeluxeLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoIntExpA1 = Arrays.asList(
						baseProto3MiniSdLog, baseProto3MiniBtStream, baseProto3MiniLogAndStream, 
						baseProto3DeluxeSdLog, baseProto3DeluxeBtStream, baseProto3DeluxeLogAndStream, 
						baseHighGAccelSdLog, baseHighGAccelBtStream, baseHighGAccelLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoIntExpA12 = Arrays.asList(
						baseProto3MiniSdLog, baseProto3MiniBtStream, baseProto3MiniLogAndStream, 
						baseProto3DeluxeSdLog, baseProto3DeluxeBtStream, baseProto3DeluxeLogAndStream, 
						baseGsrSdLog, baseGsrBtStream, baseGsrLogAndStream, 
						baseGsrUnifiedSdLog, baseGsrUnifiedBtStream, baseGsrUnifiedLogAndStream,
						baseHighGAccelSdLog, baseHighGAccelBtStream, baseHighGAccelLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoIntExpA13 = Arrays.asList(
						baseProto3MiniSdLog, baseProto3MiniBtStream, baseProto3MiniLogAndStream, 
						baseProto3DeluxeSdLog, baseProto3DeluxeBtStream, baseProto3DeluxeLogAndStream, 
						baseHighGAccelSdLog, baseHighGAccelBtStream, baseHighGAccelLogAndStream, 
						baseGsrSdLog, baseGsrBtStream, baseGsrLogAndStream, 
						baseGsrUnifiedSdLog, baseGsrUnifiedBtStream, baseGsrUnifiedLogAndStream 
						);

				List<ShimmerVerObject> listOfCompatibleVersionInfoIntExpA14 = Arrays.asList(
						baseProto3MiniSdLog, baseProto3MiniBtStream, baseProto3MiniLogAndStream, 
						baseProto3DeluxeSdLog, baseProto3DeluxeBtStream, baseProto3DeluxeLogAndStream, 
						baseHighGAccelSdLog, baseHighGAccelBtStream, baseHighGAccelLogAndStream 
						);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoStreaming = Arrays.asList(
						baseBtStream, baseLogAndStream);

				List<ShimmerVerObject> listOfCompatibleVersionInfoLogging = Arrays.asList(
						baseSdLog, baseLogAndStream);
				
				List<ShimmerVerObject> listOfCompatibleVersionInfoRespiration = Arrays.asList(
						baseExgUnifiedSdLog, baseExgUnifiedBtStream, baseExgUnifiedLogAndStream);

				List<ShimmerVerObject> listOfCompatibleVersionInfoHighGAccel = Arrays.asList(
						baseHighGAccelSdLog,baseHighGAccelBtStream,baseHighGAccelLogAndStream);
				
				
				// Assemble the channel map
				// NV_SENSORS0
				long streamingByteIndex = 0;
				long logHeaderByteIndex = 0;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.A_ACCEL, new SensorDetails(false, 0x80<<(streamingByteIndex*8), 0x80<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.ACCEL_LN));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO, new SensorDetails(false, 0x40<<(streamingByteIndex*8), 0x40<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.GYRO));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG, new SensorDetails(false, 0x20<<(streamingByteIndex*8), 0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MAG));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT, new SensorDetails(false, 0x10<<(streamingByteIndex*8), 0x10<<(logHeaderByteIndex*8), Configuration.Shimmer3.GuiLabelSensors.EXG1_24BIT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT, new SensorDetails(false, 0x08<<(streamingByteIndex*8), 0x08<<(logHeaderByteIndex*8), Configuration.Shimmer3.GuiLabelSensors.EXG2_24BIT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.GSR, new SensorDetails(false, 0x04<<(streamingByteIndex*8), 0x04<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.GSR));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A7, new SensorDetails(false, 0x02<<(streamingByteIndex*8), 0x02<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.EXT_EXP_A7));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A6, new SensorDetails(false, 0x01<<(streamingByteIndex*8), 0x01<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.EXT_EXP_A6));
				
				// NV_SENSORS1
				streamingByteIndex = 1;
				logHeaderByteIndex = 1;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP, new SensorDetails(false, 0x80<<(streamingByteIndex*8), 0x80<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.BRIDGE_AMPLIFIER));
				//shimmerChannels.put(, new ChannelDetails(false, 0x40<<(streamingByteIndex*8), 0x40<<(logHeaderByteIndex*8), "")); // unused? - new PPG bit might be here now
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.VBATT, new SensorDetails(false, 0x20<<(streamingByteIndex*8), 0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.BATTERY));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL, new SensorDetails(false, 0x10<<(streamingByteIndex*8), 0x10<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.ACCEL_WR));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A15, new SensorDetails(false, 0x08<<(streamingByteIndex*8), 0x08<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.EXT_EXP_A15));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1, new SensorDetails(false, 0x04<<(streamingByteIndex*8), 0x04<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.INT_EXP_A1));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12, new SensorDetails(false, 0x02<<(streamingByteIndex*8), 0x02<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.INT_EXP_A12));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13, new SensorDetails(false, 0x01<<(streamingByteIndex*8), 0x01<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.INT_EXP_A13));

				// NV_SENSORS2
				streamingByteIndex = 2;
				logHeaderByteIndex = 2;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14, new SensorDetails(false, 0x80<<(streamingByteIndex*8), 0x80<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.INT_EXP_A14));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL, new SensorDetails(false, 0x40<<(streamingByteIndex*8), 0x40<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.ACCEL_MPU));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MAG, new SensorDetails(false, 0x20<<(streamingByteIndex*8), 0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MAG_MPU));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT, new SensorDetails(false, 0x10<<(streamingByteIndex*8), 0x10<<(logHeaderByteIndex*8), Configuration.Shimmer3.GuiLabelSensors.EXG1_16BIT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT, new SensorDetails(false, 0x08<<(streamingByteIndex*8), 0x08<<(logHeaderByteIndex*8), Configuration.Shimmer3.GuiLabelSensors.EXG2_16BIT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE, new SensorDetails(false, 0x04<<(streamingByteIndex*8), 0x04<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.PRESS_TEMP_BMP180));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_TEMP, new SensorDetails(false, 0x20<<(streamingByteIndex*8), 0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MPL_TEMPERATURE));
				//shimmerChannels.put(SENSOR_SHIMMER3_MSP430_TEMPERATURE, new ChannelDetails(false, 0x01<<(btStreamByteIndex*8), 0x01<<(sDHeaderByteIndex*8), "")); // not yet implemented
				//shimmerChannels.put(SENSOR_SHIMMER3_LSM303DLHC_TEMPERATURE, new ChannelDetails(false, 0x01<<(btStreamByteIndex*8), 0x01<<(sDHeaderByteIndex*8), "")); // not yet implemented

				// NV_SENSORS3				
				streamingByteIndex = 3;
				logHeaderByteIndex = 3;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF, new SensorDetails(false, (long)0, (long)0x80<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.QUAT_MPL_6DOF));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF, new SensorDetails(false, (long)0, (long)0x40<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.QUAT_MPL_9DOF));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF, new SensorDetails(false, (long)0, (long)0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.EULER_ANGLES_6DOF));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF, new SensorDetails(false, (long)0, (long)0x10<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.EULER_ANGLES_9DOF));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING, new SensorDetails(false, (long)0, (long)0x08<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MPL_HEADING));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER, new SensorDetails(false, (long)0, (long)0x04<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MPL_PEDOM_CNT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP, new SensorDetails(false, (long)0, (long)0x02<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MPL_TAPDIRANDTAPCNT));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT, new SensorDetails(false, (long)0, (long)0x01<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MPL_MOTIONANDORIENT));

				// NV_SENSORS4
				streamingByteIndex = 4;
				logHeaderByteIndex = 4;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO, new SensorDetails(false, (long)0, (long)0x80<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.GYRO_MPU_MPL));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL, new SensorDetails(false, (long)0, (long)0x40<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.ACCEL_MPU_MPL));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG, new SensorDetails(false, (long)0, (long)0x20<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.MAG_MPU_MPL));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW, new SensorDetails(false, (long)0, (long)0x10<<(logHeaderByteIndex*8), Shimmer3.GuiLabelSensors.QUAT_DMP_6DOF));
				//shimmerChannels.put(, new ChannelDetails(false, 0, 0x08<<(loggingHeaderByteIndex*8), "")); // unused
				//shimmerChannels.put(, new ChannelDetails(false, 0, 0x04<<(loggingHeaderByteIndex*8), "")); // unused
				//shimmerChannels.put(, new ChannelDetails(false, 0, 0x02<<(loggingHeaderByteIndex*8), "")); // unused
				//shimmerChannels.put(, new ChannelDetails(false, 0, 0x01<<(loggingHeaderByteIndex*8), "")); // unused
				
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.ECG, new SensorDetails(false, 0, 0, Shimmer3.GuiLabelSensors.ECG));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG_TEST, new SensorDetails(false, 0, 0, Shimmer3.GuiLabelSensors.EXG_TEST));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.EXG_RESPIRATION));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.EMG, new SensorDetails(false, 0, 0, Shimmer3.GuiLabelSensors.EMG));

				// Derived Channels - Bridge Amp Board
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP, new SensorDetails(false, 0, 0, Shimmer3.GuiLabelSensors.RESISTANCE_AMP));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelResAmp;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE, new SensorDetails(false, 0, 0, Shimmer3.GuiLabelSensors.SKIN_TEMP_PROBE));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelSkinTemp;
				
				// Derived Channels - GSR Board
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG_DUMMY));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG_A12, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG_A12));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg_ADC12ADC13;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG_A13, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG_A13));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg_ADC12ADC13;
				
				// Derived Channels - Proto3 Board
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG1_DUMMY));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG1_A12, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG1_A12));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg1_ADC12ADC13;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG1_A13, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG1_A13));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg1_ADC12ADC13;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG2_DUMMY));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG2_A1, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG2_A1));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg2_ADC1ADC14;
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG2_A14, new SensorDetails(false, 0, 0, Configuration.Shimmer3.GuiLabelSensors.PPG2_A14));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mSensorBitmapIDStreaming = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mSensorBitmapIDStreaming;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mSensorBitmapIDSDLogHeader = mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mSensorBitmapIDSDLogHeader;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mDerivedSensorBitmapID = infoMemMap.maskDerivedChannelPpg2_ADC1ADC14;

				
				// Now that channel map is assembled we can add compatiblity
				// information, internal expansion board power requirements,
				// associated required channels, conflicting channels and
				// associated configuration options.
				
				// Channels that have compatibility considerations (used to auto-hide/disable channels/config options in GUI)
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoBrAmp;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoIntExpA1;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoIntExpA12;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoIntExpA13;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoIntExpA14;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_TEMP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoSdLog;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoRespiration;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoBrAmp;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoBrAmp;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.A_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A6).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A7).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A15).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				
				// Channels that require Internal Expansion board power (automatically enabled internal expansion board power when each Channel is enabled)
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIntExpBoardPowerRequired = true;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIntExpBoardPowerRequired = true;
				
				// Required Channels - these get auto enabled/disabled when the parent channel is enabled/disabled
				//mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mListOfSensorMapKeysRequired = Arrays.asList(
				//		Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1);
				//mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mListOfSensorMapKeysRequired = Arrays.asList(
				//		Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1);
				
				// Conflicting Channels (stop two conflicting channels from being enabled and the same time)
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG2_A1,
						Configuration.Shimmer3.SensorMapKey.PPG2_A14,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.PPG_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG_A12,
						Configuration.Shimmer3.SensorMapKey.PPG_A13,
						Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG1_A12,
						Configuration.Shimmer3.SensorMapKey.PPG1_A13,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.PPG_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG_A12,
						Configuration.Shimmer3.SensorMapKey.PPG_A13,
						Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG1_A12,
						Configuration.Shimmer3.SensorMapKey.PPG1_A13,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY,
						Configuration.Shimmer3.SensorMapKey.PPG2_A1,
						Configuration.Shimmer3.SensorMapKey.PPG2_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST);
				
				//The A12 and A13 based PPG channels have the same channel exceptions as GSR with the addition of their counterpart channel 
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mListOfSensorMapKeysConflicting = new ArrayList<Integer>(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfSensorMapKeysConflicting);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.PPG_A13);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfSensorMapKeysConflicting = new ArrayList<Integer>(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfSensorMapKeysConflicting);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.PPG_A12);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfSensorMapKeysConflicting.add(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13);

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.PPG1_A13,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13);
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.PPG1_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13);
						
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.PPG2_A14,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14);
						
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mListOfSensorMapKeysConflicting = Arrays.asList(
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.GSR,
						Configuration.Shimmer3.SensorMapKey.ECG,
						Configuration.Shimmer3.SensorMapKey.EMG,
						Configuration.Shimmer3.SensorMapKey.EXG_TEST,
						Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION,
						Configuration.Shimmer3.SensorMapKey.EXG1_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_16BIT,
						Configuration.Shimmer3.SensorMapKey.EXG1_24BIT,
						Configuration.Shimmer3.SensorMapKey.EXG2_24BIT,
						Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP,
						Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
						Configuration.Shimmer3.SensorMapKey.PPG2_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
						Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14);
				
				// Associated config options for each channel (currently used for the ChannelTileMap)
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE,
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RANGE,
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RATE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RANGE,
						Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RATE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.PRESSURE_RESOLUTION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION,
						Configuration.Shimmer3.GuiLabelConfig.EXG_REFERENCE_ELECTRODE,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_DETECTION,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_CURRENT,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_COMPARATOR,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_FREQ,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_PHASE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION,
						Configuration.Shimmer3.GuiLabelConfig.EXG_REFERENCE_ELECTRODE,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_DETECTION,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_CURRENT,
						Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_COMPARATOR);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_FREQ,
						Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_PHASE);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.PPG_ADC_SELECTION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.PPG1_ADC_SELECTION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.PPG2_ADC_SELECTION);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER);

				
				//Sensor Grouping for Configuration Panel 'tile' generation. 
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.LOW_NOISE_ACCEL, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.A_ACCEL)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.WIDE_RANGE_ACCEL, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.GYRO, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.MAG, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.PRESSURE_TEMPERATURE, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.BATTERY_MONITORING, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.VBATT)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.EXTERNAL_EXPANSION_ADC, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A6,
									Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A7,
									Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A15)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.GSR, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.GSR,
									Configuration.Shimmer3.SensorMapKey.PPG_DUMMY)));
//									Configuration.Shimmer3.SensorMapKey.PPG_A12,
//									Configuration.Shimmer3.SensorMapKey.PPG_A13)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.EXG, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.ECG,
									Configuration.Shimmer3.SensorMapKey.EMG,
									Configuration.Shimmer3.SensorMapKey.EXG_TEST,
									Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_MINI, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_DELUXE, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_DELUXE_SUPP, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY,
									Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.BRIDGE_AMPLIFIER, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP,
									Configuration.Shimmer3.SensorMapKey.RESISTANCE_AMP)));
				//mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.BRIDGE_AMPLIFIER_SUPP, new SensorTileDetails(
				//		Arrays.asList(Configuration.Shimmer3.SensorMapKey.SKIN_TEMP_PROBE)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.HIGH_G_ACCEL, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12, //X-axis
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13, //Y-axis
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14, //Z-axis
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1))); //unused but accessible
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.INTERNAL_EXPANSION_ADC, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13,
									Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14)));
				//mChannelTileMap.put(Configuration.Shimmer3.GUI_LABEL_CHANNELTILE_9DOF, new ChannelTileDetails(
				//		Arrays.asList(Configuration.Shimmer3.SensorMapKey.A_ACCEL,
				//					Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL,
				//					Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO,
				//					Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG)));
				//Not implemented: GUI_LABEL_CHANNEL_GROUPING_GPS
				
				// ChannelTiles that have compatibility considerations (used to auto generate tiles in GUI)
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.LOW_NOISE_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.GYRO).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.MAG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.BATTERY_MONITORING).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardAndFw;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.WIDE_RANGE_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardAndFw;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.PRESSURE_TEMPERATURE).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.EXTERNAL_EXPANSION_ADC).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoAnyExpBoardStandardFW;
				//mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.INTERNAL_EXPANSION_ADC).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.GSR).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGsr;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.EXG).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoExg;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_MINI).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Mini;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_DELUXE).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.PROTO3_DELUXE_SUPP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoProto3Deluxe;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.BRIDGE_AMPLIFIER).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoBrAmp;
				//mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.BRIDGE_AMPLIFIER_SUPP).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoBrAmp;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.HIGH_G_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoHighGAccel;
				//mShimmerChannelGroupingMap.get(Configuration.Shimmer3.GUI_LABEL_CHANNEL_GROUPING_GPS).mCompatibleVersionInfo = listOfCompatibleVersionInfoGps;
				
				// For loop to automatically inherit associated channel configuration options from mSensorMap in the mSensorTileMap
				for (String channelGroup : mSensorTileMap.keySet()) {
					// Ok to clear here because variable is initiated in the class
					mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.clear();
					for (Integer channel : mSensorTileMap.get(channelGroup).mListOfSensorMapKeysAssociated) {
						List<String> associatedConfigOptions = mSensorMap.get(channel).mListOfConfigOptionKeysAssociated;
						if (associatedConfigOptions != null) {
							for (String configOption : associatedConfigOptions) {
								// do not add duplicates
								if (!(mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.contains(configOption))) {
									mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.add(configOption);
								}
							}
						}
					}
				}				
				
				
				// Assemble the channel configuration options map
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_USER_ASSIGNED_NAME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoLogging));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_SAMPLING_RATE, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_MAC_FROM_INFOMEM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.BUFFER_SIZE, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.CONFIG_TIME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoLogging));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_NAME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoLogging));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_ID, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoLogging));

				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_NUMBER_OF_SHIMMERS, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_DURATION_ESTIMATED, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_DURATION_MAXIMUM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.BROADCAST_INTERVAL, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
												new ArrayList(){}));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.BLUETOOTH_BAUD_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofBluetoothBaudRates, 
												Configuration.Shimmer3.ListofBluetoothBaudRatesConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoStreaming));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofAccelRange, 
												Configuration.Shimmer3.ListofLSM303DLHCAccelRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				if(mLowPowerAccelWR) {
					mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
							new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpm, 
													Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpmConfigValues, 
													SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				}
				else {
					mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
							new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRate, 
													Configuration.Shimmer3.ListofLSM303DLHCAccelRateConfigValues, 
													SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				}
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofGyroRange, 
												Configuration.Shimmer3.ListofMPU9150GyroRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMagRange, 
												Configuration.Shimmer3.ListofMagRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCMagRate, 
												Configuration.Shimmer3.ListofLSM303DLHCMagRateConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.PRESSURE_RESOLUTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofPressureResolution, 
												Configuration.Shimmer3.ListofPressureResolutionConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoAnyExpBoardStandardFW));

				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofGSRRange, 
												Configuration.Shimmer3.ListofGSRRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoGsr));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_GAIN, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGGain, 
												Configuration.Shimmer3.ListOfExGGainConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_RESOLUTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGResolutions, 
												Configuration.Shimmer3.ListOfExGResolutionsConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));

				//Advanced ExG		
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_REFERENCE_ELECTRODE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfECGReferenceElectrode, 
												Configuration.Shimmer3.ListOfECGReferenceElectrodeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				//TODO EMG vs. ECG vs. ExG Test vs. Respiration
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGRate, 
												Configuration.Shimmer3.ListOfExGRateConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_DETECTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGLeadOffDetection, 
												Configuration.Shimmer3.ListOfExGLeadOffDetectionConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_CURRENT, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGLeadOffCurrent, 
												Configuration.Shimmer3.ListOfExGLeadOffCurrentConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_LEAD_OFF_COMPARATOR, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGLeadOffComparator, 
												Configuration.Shimmer3.ListOfExGLeadOffComparatorConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoExg));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_FREQ, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGRespirationDetectFreq, 
												Configuration.Shimmer3.ListOfExGRespirationDetectFreqConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoRespiration));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXG_RESPIRATION_DETECT_PHASE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfExGRespirationDetectPhase32khz, 
												Configuration.Shimmer3.ListOfExGRespirationDetectPhase32khzConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoRespiration));
				
				//MPL Options
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_ACCEL_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMPU9150AccelRange, 
												Configuration.Shimmer3.ListofMPU9150AccelRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_DMP_GYRO_CAL, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMPU9150MplCalibrationOptions, 
												Configuration.Shimmer3.ListofMPU9150MplCalibrationOptionsConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_LPF, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMPU9150MplLpfOptions, 
												Configuration.Shimmer3.ListofMPU9150MplLpfOptionsConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMPU9150MplRate, 
												Configuration.Shimmer3.ListofMPU9150MplRateConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MAG_RATE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofMPU9150MagRate, 
												Configuration.Shimmer3.ListofMPU9150MagRateConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoSdLog));

			    mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER, 
			    	      new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfOnOff, 
					    	        Configuration.Shimmer3.ListOfOnOffConfigValues,
					    	        SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				
				//MPL CheckBoxes
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_DMP, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_9DOF_SENSOR_FUSION, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_GYRO_CAL, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_VECTOR_CAL, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_MPL_MAG_CAL, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				
				//General Config
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_RATE, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.USER_BUTTON_START, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoLogging));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SINGLE_TOUCH_START, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_MASTER_SHIMMER, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_SYNC_WHEN_LOGGING, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));

				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.KINEMATIC_LPM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_LPM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.MPU9150_GYRO_LPM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_MAG_LPM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoAnyExpBoardStandardFW));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.TCX0, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoSdLog));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_BOOLEAN, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.PPG_ADC_SELECTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfPpgAdcSelection, 
												Configuration.Shimmer3.ListOfPpgAdcSelectionConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoGsr));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.PPG1_ADC_SELECTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfPpg1AdcSelection, 
												Configuration.Shimmer3.ListOfPpg1AdcSelectionConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoProto3Deluxe));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.PPG2_ADC_SELECTION, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfPpg2AdcSelection, 
												Configuration.Shimmer3.ListOfPpg2AdcSelectionConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoProto3Deluxe));

				
				// All Information required for parsing each of the channels
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.A_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_X,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.A_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.A_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.VBATT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.BATTERY,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));

				//ADC
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A7).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A6).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXT_EXP_A6,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXT_EXP_ADC_A15).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXT_EXP_A15,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.INT_EXP_A1,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.INT_EXP_A12,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.INT_EXP_A13,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.INT_EXP_A14,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));

				//Bridge Amp High
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BRIDGE_AMP).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW,ChannelDataType.UINT12,2,ChannelDataEndian.LSB));
				
				//GSR
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GSR,ChannelDataType.UINT16,2,ChannelDataEndian.LSB));

				//GyroMPU
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_X,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Y,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_Z,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X,ChannelDataType.INT16,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y,ChannelDataType.INT16,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z,ChannelDataType.INT16,2,ChannelDataEndian.LSB));
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_X,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_Y,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_Z,ChannelDataType.INT16,2,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_X,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Y,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Z,ChannelDataType.INT16,2,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_X,ChannelDataType.INT16,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_Y,ChannelDataType.INT16,2,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_Z,ChannelDataType.INT16,2,ChannelDataEndian.LSB));

				//bmp180 - pressure
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180,ChannelDataType.UINT24,3,ChannelDataEndian.MSB));
				//bmp180 - temp
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180,ChannelDataType.UINT16,2,ChannelDataEndian.MSB));
				
				//exg
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_STATUS,ChannelDataType.UINT8,1,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT,ChannelDataType.INT24,3,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT,ChannelDataType.INT24,3,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_STATUS,ChannelDataType.UINT8,1,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT,ChannelDataType.INT24,3,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT,ChannelDataType.INT24,3,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_STATUS,ChannelDataType.UINT8,1,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT,ChannelDataType.INT16,2,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_STATUS,ChannelDataType.UINT8,1,ChannelDataEndian.LSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT,ChannelDataType.INT16,2,ChannelDataEndian.MSB));
				

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_W,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_W,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				
				//euler
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				//heading
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MPL_HEADING,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				//mpu temp
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_TEMP).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MPL_TEMPERATURE,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				//mpl pedom
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MPL_PEDOM_CNT,ChannelDataType.UINT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MPL_PEDOM_TIME,ChannelDataType.UINT32,4,ChannelDataEndian.MSB));

				//mpl tap
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.TAPDIRANDTAPCNT,ChannelDataType.UINT8,1,ChannelDataEndian.MSB));

				//mpl motion orient
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.TAPDIRANDTAPCNT,ChannelDataType.UINT8,1,ChannelDataEndian.MSB));

				//mpl gyro cal
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				//mpl accel cal
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));

				//mpl mag cal
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				
				// Raw 6DOF quaterians from the DMP hardware module of the MPU9150
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_W,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_X,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_Y,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mListOfChannels.add(new ChannelDetails(Configuration.Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_Z,ChannelDataType.INT32,4,ChannelDataEndian.MSB));
				
			}
			else if (mHardwareVersion == HW_ID.SHIMMER_GQ) {

				// These can be used to enable/disble GUI options depending on what HW, FW, Expansion boards versions are present
				ShimmerVerObject baseAnyIntExpBoardAndFw = new ShimmerVerObject(HW_ID.SHIMMER_GQ,FW_ID.SHIMMER3.GQ_GSR,ANY_VERSION,ANY_VERSION,ANY_VERSION,ANY_VERSION);
				List<ShimmerVerObject> listOfCompatibleVersionInfoGq = Arrays.asList(baseAnyIntExpBoardAndFw);
				
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.VBATT, new SensorDetails(false, Shimmer3.GuiLabelSensors.BATTERY));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL, new SensorDetails(false, Shimmer3.GuiLabelSensors.ACCEL_WR));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.GSR, new SensorDetails(false, Shimmer3.GuiLabelSensors.GSR));
				mSensorMap.put(Configuration.Shimmer3.SensorMapKey.PPG_A13, new SensorDetails(false, Shimmer3.GuiLabelSensors.PPG_A13));
				
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.VBATT).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;

				// Associated config options for each channel (currently used for the ChannelTileMap)
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.VBATT).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_VBATT);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_PPG);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE,
						Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE,
						Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_LSM303DLHC_ACCEL);
				mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mListOfConfigOptionKeysAssociated = Arrays.asList(
						Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE,
						Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_GSR);

				//Sensor Grouping for Configuration Panel 'tile' generation. 
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.BATTERY_MONITORING, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.VBATT)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.WIDE_RANGE_ACCEL, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL)));
				mSensorTileMap.put(Configuration.Shimmer3.GuiLabelSensorTiles.GSR, new SensorTileDetails(
						Arrays.asList(Configuration.Shimmer3.SensorMapKey.GSR,
									Configuration.Shimmer3.SensorMapKey.PPG_A12)));
				
				// ChannelTiles that have compatibility considerations (used to auto generate tiles in GUI)
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.BATTERY_MONITORING).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.WIDE_RANGE_ACCEL).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;
				mSensorTileMap.get(Configuration.Shimmer3.GuiLabelSensorTiles.GSR).mListOfCompatibleVersionInfo = listOfCompatibleVersionInfoGq;
				
				// For loop to automatically inherit associated channel configuration options from ChannelMap in the ChannelTileMap
				for (String channelGroup : mSensorTileMap.keySet()) {
					// Ok to clear here because variable is initiated in the class
					mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.clear();
					for (Integer channel : mSensorTileMap.get(channelGroup).mListOfSensorMapKeysAssociated) {
						if(mSensorMap.containsKey(channel)){
							List<String> associatedConfigOptions = mSensorMap.get(channel).mListOfConfigOptionKeysAssociated;
							if (associatedConfigOptions != null) {
								for (String configOption : associatedConfigOptions) {
									// do not add duplicates
									if (!(mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.contains(configOption))) {
										mSensorTileMap.get(channelGroup).mListOfConfigOptionKeysAssociated.add(configOption);
									}
								}
							}
						}
					}
				}
				
				// Assemble the channel configuration options map
				mConfigOptionsMap = new HashMap<String,SensorConfigOptionDetails>();
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_USER_ASSIGNED_NAME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
								listOfCompatibleVersionInfoGq));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_SAMPLING_RATE, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SHIMMER_MAC_FROM_INFOMEM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.CONFIG_TIME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
								listOfCompatibleVersionInfoGq));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_NAME, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
								listOfCompatibleVersionInfoGq));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.EXPERIMENT_ID, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.TEXTFIELD,
								listOfCompatibleVersionInfoGq));

				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofAccelRange, 
												Configuration.Shimmer3.ListofLSM303DLHCAccelRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				if(mLowPowerAccelWR) {
					mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
							new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpm, 
													Configuration.Shimmer3.ListofLSM303DLHCAccelRateLpmConfigValues, 
													SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				}
				else {
					mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_RATE, 
							new SensorConfigOptionDetails(Configuration.Shimmer3.ListofLSM303DLHCAccelRate, 
													Configuration.Shimmer3.ListofLSM303DLHCAccelRateConfigValues, 
													SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				}
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.GSR_RANGE, 
						new SensorConfigOptionDetails(Configuration.Shimmer3.ListofGSRRange, 
												Configuration.Shimmer3.ListofGSRRangeConfigValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX,
												listOfCompatibleVersionInfoGq));
				
				mConfigOptionsMap.put(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_GSR, 
						new SensorConfigOptionDetails(Configuration.Shimmer3Gq.ListofSamplingRateDividers, 
												Configuration.Shimmer3Gq.ListofSamplingRateDividersValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				mConfigOptionsMap.put(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_LSM303DLHC_ACCEL, 
						new SensorConfigOptionDetails(Configuration.Shimmer3Gq.ListofSamplingRateDividers, 
												Configuration.Shimmer3Gq.ListofSamplingRateDividersValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				mConfigOptionsMap.put(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_PPG, 
						new SensorConfigOptionDetails(Configuration.Shimmer3Gq.ListofSamplingRateDividers, 
												Configuration.Shimmer3Gq.ListofSamplingRateDividersValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				mConfigOptionsMap.put(Configuration.Shimmer3Gq.GuiLabelConfig.SAMPLING_RATE_DIVIDER_VBATT, 
						new SensorConfigOptionDetails(Configuration.Shimmer3Gq.ListofSamplingRateDividers, 
												Configuration.Shimmer3Gq.ListofSamplingRateDividersValues, 
												SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));

			    mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_INTEGER, 
			    	      new SensorConfigOptionDetails(Configuration.Shimmer3.ListOfOnOff, 
					    	        Configuration.Shimmer3.ListOfOnOffConfigValues,
					    	        SensorConfigOptionDetails.GUI_COMPONENT_TYPE.COMBOBOX));
				
				//General Config
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.USER_BUTTON_START, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoGq));
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.SINGLE_TOUCH_START, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX,listOfCompatibleVersionInfoGq));

				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.LSM303DLHC_ACCEL_LPM, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX));
				
				mConfigOptionsMap.put(Configuration.Shimmer3.GuiLabelConfig.INT_EXP_BRD_POWER_BOOLEAN, 
						new SensorConfigOptionDetails(SensorConfigOptionDetails.GUI_COMPONENT_TYPE.CHECKBOX));
			}
		}
	}
	
	
	/**
	 * Used to convert from the enabledSensors long variable read from the
	 * Shimmer to the set enabled status of the relative entries in the Sensor
	 * Map. Used in Consensys for dynamic GUI generation to configure a Shimmer.
	 * 
	 */
	public void sensorMapUpdateWithEnabledSensors() {

		if((mEnabledSensors != 0) && (mSensorMap != null)) {

			if (mHardwareVersion == HW_ID.SHIMMER_3) {
				
				for(Integer sensorMapKey:mSensorMap.keySet()) {
					boolean skipKey = false;

					// Skip if ExG channels here -> handle them after for loop.
					if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.ECG)
							||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EMG)
							||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_TEST)
							||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)) {
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
						skipKey = true;
					}
					// Handle derived sensors based on int adc channels (e.g. PPG vs. A12/A13)
					else if(((sensorMapKey==Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12)
						||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13)
						||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1)
						||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14))){

						//Check if a derived channel is enabled, if it is ignore disable and skip 
						innerloop:
						for(Integer conflictKey:mSensorMap.get(sensorMapKey).mListOfSensorMapKeysConflicting) {
							if(mSensorMap.get(conflictKey).isDerivedChannel()) {
								if((mDerivedSensors&mSensorMap.get(conflictKey).mDerivedSensorBitmapID) == mSensorMap.get(conflictKey).mDerivedSensorBitmapID) {
									mSensorMap.get(sensorMapKey).mIsEnabled = false;
									skipKey = true;
									break innerloop;
								}
							}
						}
					}

					// Process remaining channels
					if(!skipKey) {
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
						// Check if this sensor is a derived sensor
						if(mSensorMap.get(sensorMapKey).isDerivedChannel()) {
							//Check if all associated derived channels are enabled 
							if((mDerivedSensors&mSensorMap.get(sensorMapKey).mDerivedSensorBitmapID) == mSensorMap.get(sensorMapKey).mDerivedSensorBitmapID) {
								//TODO comment
								if((mEnabledSensors & mSensorMap.get(sensorMapKey).mSensorBitmapIDSDLogHeader) == mSensorMap.get(sensorMapKey).mSensorBitmapIDSDLogHeader) {
									mSensorMap.get(sensorMapKey).mIsEnabled = true;
								}
								else {
									mSensorMap.get(sensorMapKey).mIsEnabled = false;
								}
							}
							else {
								//System.out.println("Derived Channel - " + mSensorMap.get(sensorMapKey).mLabel + " no associated required keys:");
							}
						}
						// This is not a derived sensor
						else {
							//Check if sensor's bit in sensor bitmap is enabled
							if((mEnabledSensors & mSensorMap.get(sensorMapKey).mSensorBitmapIDSDLogHeader) == mSensorMap.get(sensorMapKey).mSensorBitmapIDSDLogHeader) {
								mSensorMap.get(sensorMapKey).mIsEnabled = true;
							}
						}
					}
				}
				
				// Now that all main sensor channels have been parsed, deal with
				// sensor channels that have special conditions. E.g. deciding
				// what type of signal the ExG is configured for or what derived
				// channel is enabled like whether PPG is on ADC12 or ADC13
	
				//Handle ExG sensors
				if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled)
						||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled)
						||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled)
						||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled)) {
					if(isEXGUsingDefaultRespirationConfiguration()) {
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled = true;
					}
					else if(isEXGUsingDefaultECGConfiguration()) {
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled = true;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled = false;
					}
					else if(isEXGUsingDefaultEMGConfiguration()) {
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled = true;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled = false;
					}
					else if(isEXGUsingDefaultTestSignalConfiguration()){
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled = true;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled = false;
					}
					else {
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled = false;
						mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled = false;
					}
				}
				
				// Set the ExG resolution variable based on which channels are
				// enabled in the sensor map
				if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled)
						||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled)) {
					mExGResolution = 0;
				}
				else if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled)
						||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled)) {
					mExGResolution = 1;
				}

				// Handle PPG sensors so that it appears in Consensys as a
				// single PPG channel with a selectable ADC based on different
				// hardware versions.
				
				//Used for Shimmer GSR hardware
				if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mIsEnabled)||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mIsEnabled)) {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY).mIsEnabled = true;
					if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A12).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG_A12, Configuration.Shimmer3.SensorMapKey.PPG_DUMMY);
						mPpgAdcSelectionGsrBoard = Configuration.Shimmer3.ListOfPpgAdcSelectionConfigValues[1]; // PPG_A12
					}
					else if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_A13).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG_A13, Configuration.Shimmer3.SensorMapKey.PPG_DUMMY);
						mPpgAdcSelectionGsrBoard = Configuration.Shimmer3.ListOfPpgAdcSelectionConfigValues[0]; // PPG_A13
					}
				}
				else {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY).mIsEnabled = false;
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG_DUMMY).resetMapKeyLists();
				}
				//Used for Shimmer Proto3 Deluxe hardware
				if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mIsEnabled)||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mIsEnabled)) {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY).mIsEnabled = true;
					if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A12).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG1_A12, Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY);
						mPpg1AdcSelectionProto3DeluxeBoard = Configuration.Shimmer3.ListOfPpg1AdcSelectionConfigValues[1]; // PPG1_A12
					}
					else if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_A13).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG1_A13, Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY);
						mPpg1AdcSelectionProto3DeluxeBoard = Configuration.Shimmer3.ListOfPpg1AdcSelectionConfigValues[0]; // PPG1_A13
					}
				}
				else {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY).mIsEnabled = false;
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY).resetMapKeyLists();
				}
				//Used for Shimmer Proto3 Deluxe hardware
				if((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mIsEnabled)||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mIsEnabled)) {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY).mIsEnabled = true;
					if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A1).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG2_A1, Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY);
						mPpg2AdcSelectionProto3DeluxeBoard = Configuration.Shimmer3.ListOfPpg2AdcSelectionConfigValues[0]; // PPG2_A1
					}
					else if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_A14).mIsEnabled) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG2_A14, Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY);
						mPpg2AdcSelectionProto3DeluxeBoard = Configuration.Shimmer3.ListOfPpg2AdcSelectionConfigValues[1]; // PPG2_A14
					}
				}
				else {
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY).mIsEnabled = false;
					mSensorMap.get(Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY).resetMapKeyLists();
				}
			}
			else if (mHardwareVersion == HW_ID.SHIMMER_GQ) {
				
			}
			
		}
	}
	
	/**
	 * Used to changed the enabled state of a sensor in the sensormap. This is
	 * only used in Consensys for dynamic configuration of a Shimmer. This
	 * method deals with everything assciated with enabling a sensor such as:
	 * 1) dealing with conflicting sensors
	 * 2) dealing with other required sensors for the chosen sensor
	 * 3) determining whether expansion board power is required
	 * 4) setting default settings for disabled sensors 
	 * 5) etc.
	 * 
	 * @param sensorMapKey the sensormap key of the sensor to be enabled/disabled
	 * @param state the sensor state to set 
	 * @return a boolean indicating if the sensors state was successfully changed
	 */
	public boolean setSensorEnabledState(int sensorMapKey, boolean state) {
		
		if(mSensorMap!=null) {
			
			if (mHardwareVersion == HW_ID.SHIMMER_3){
				
				// Special case for Dummy entries in the Sensor Map
				if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.PPG_DUMMY) {
					mSensorMap.get(sensorMapKey).mIsEnabled = state;
					if(Configuration.Shimmer3.ListOfPpgAdcSelection[mPpgAdcSelectionGsrBoard].contains("A12")) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG_A12, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG_A12;
					}
					else {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG_A13, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG_A13;
					}
				}		
				else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.PPG1_DUMMY) {
					mSensorMap.get(sensorMapKey).mIsEnabled = state;
					if(Configuration.Shimmer3.ListOfPpg1AdcSelection[mPpg1AdcSelectionProto3DeluxeBoard].contains("A12")) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG1_A12, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG1_A12;
					}
					else {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG1_A13, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG1_A13;
					}
				}		
				else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.PPG2_DUMMY) {
					mSensorMap.get(sensorMapKey).mIsEnabled = state;
					if(Configuration.Shimmer3.ListOfPpg2AdcSelection[mPpg2AdcSelectionProto3DeluxeBoard].contains("A14")) {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG2_A14, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG2_A14;
					}
					else {
						copySensorMapSensorDetails(Configuration.Shimmer3.SensorMapKey.PPG2_A1, sensorMapKey);
						sensorMapKey = Configuration.Shimmer3.SensorMapKey.PPG2_A1;
					}
				}		
				
				// Automatically handle required channels for each sensor
				if(mSensorMap.get(sensorMapKey).mListOfSensorMapKeysRequired != null) {
					for(Integer i:mSensorMap.get(sensorMapKey).mListOfSensorMapKeysRequired) {
						mSensorMap.get(i).mIsEnabled = state;
					}
				}
				
				// Unique cases for Shimmer3 ExG
				if((sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG1_16BIT)
						||(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG2_16BIT)) {
					mExGResolution = 0;
				}
				else if((sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG1_24BIT)
						||(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG2_24BIT)) {
					mExGResolution = 1;
				}
				else if((sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)
						|| (sensorMapKey == Configuration.Shimmer3.SensorMapKey.ECG)
						|| (sensorMapKey == Configuration.Shimmer3.SensorMapKey.EMG)
						|| (sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_TEST)) {
					
					// If ExG sensor enabled, set default settings for that
					// sensor. Otherwise set the default ECG configuration.
					if(state) { 
						if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION) {
							setDefaultRespirationConfiguration();
						}
						else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.ECG) {
							setDefaultECGConfiguration();
						}
						else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EMG) {
							setDefaultEMGConfiguration();
						}
						else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_TEST) {
							setEXGTestSignal();
						}
					}
					else {
						setDefaultECGConfiguration();
					}
					
					if((sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)
							||(sensorMapKey == Configuration.Shimmer3.SensorMapKey.ECG)
							||(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EXG_TEST)) {
						if(mExGResolution == 0) {// 16-bit
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled = state;
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled = state;
						}
						else { // 24-bit
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled = state;
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled = state;
						}
					}
					else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.EMG) {
						if(mExGResolution == 0) {// 16-bit
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled = state;
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled = false;
						}
						else { // 24-bit
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled = state;
							mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled = false;
						}
					}

				}
				
			}
			else if (mHardwareVersion == HW_ID.SHIMMER_GQ) {
				
			}

			
			//Set sensor state
			mSensorMap.get(sensorMapKey).mIsEnabled = state;
			
			
			
			sensorMapConflictCheckandCorrect(sensorMapKey);
			setDefaultConfigForSensor(sensorMapKey, mSensorMap.get(sensorMapKey).mIsEnabled);

			// Automatically control internal expansion board power
			checkIfInternalExpBrdPowerIsNeeded();
			
			if(mSensorMap.get(sensorMapKey).mIsEnabled == state) {
				return true;
			}
			else {
				return false;
			}
			
		}
		else {
			return false;
		}
	}
	
	/**
	 * Method to set force defaults for disabled sensors. Need to ensure
	 * consistency across all configured Shimmers. Without the below, if a
	 * Shimmer is read from and then configured without changing any of the
	 * configuration, the configuration will not be checked. Another application
	 * could have saved incorrect configuration to the Shimmer.
	 * 
	 */
	public void checkShimmerConfigurationBeforeConfiguring() {
		
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL).mIsEnabled) {
				setDefaultLsm303dlhcAccelSensorConfig(false);
			}
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG).mIsEnabled) {
				setDefaultLsm303dlhcMagSensorConfig(false);
			}
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO).mIsEnabled){
				setDefaultMpu9150GyroSensorConfig(false);
			}
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL).mIsEnabled){
				setDefaultMpu9150AccelSensorConfig(false);
			}
			
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE).mIsEnabled) {
				setDefaultBmp180PressureSensorConfig(false);
			}
			if(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.GSR).mIsEnabled) {
				setDefaultGsrSensorConfig(false);
			}
			if((!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.ECG).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_TEST).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EMG).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled)) {
//				if(!checkIfOtherExgChannelEnabled()) {
					setDefaultECGConfiguration();
//				}
			}
			if((!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG).mIsEnabled)
					&&(!mSensorMap.get(Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW).mIsEnabled)) {
//				if(!checkIfOtherMplChannelEnabled()) {
					setDefaultMpu9150MplSensorConfig(false);
//				}
			}
			
			checkIfInternalExpBrdPowerIsNeeded();
		}
		
	}

	/**Automatically control internal expansion board power based on sensor map
	 * 
	 */
	private void checkIfInternalExpBrdPowerIsNeeded(){

		if (mHardwareVersion == HW_ID.SHIMMER_3){
			for(Integer channelKey:mSensorMap.keySet()) {
				if(mSensorMap.get(channelKey).mIsEnabled && mSensorMap.get(channelKey).mIntExpBoardPowerRequired) {
					mInternalExpPower = 1;
					break;
				}
				else {
					// Exception for Int ADC sensors 
					//TODO need to check HW version??
					if(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A1).mIsEnabled
						||mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A12).mIsEnabled
						||mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A13).mIsEnabled
						||mSensorMap.get(Configuration.Shimmer3.SensorMapKey.INT_EXP_ADC_A14).mIsEnabled){
						
					}
					else {
						mInternalExpPower = 0;
					}
				}
			}
		}
	}
	
	
	private void copySensorMapSensorDetails(int keyFrom, int keyTo) {
		if(mSensorMap!=null) {
			if(mSensorMap.containsKey(keyFrom)&&mSensorMap.containsKey(keyTo)) {
				mSensorMap.get(keyTo).mListOfCompatibleVersionInfo = mSensorMap.get(keyFrom).mListOfCompatibleVersionInfo;
				mSensorMap.get(keyTo).mListOfSensorMapKeysRequired = mSensorMap.get(keyFrom).mListOfSensorMapKeysRequired;
				mSensorMap.get(keyTo).mListOfSensorMapKeysConflicting = mSensorMap.get(keyFrom).mListOfSensorMapKeysConflicting;
//				mSensorMap.get(keyTo).mIntExpBoardPowerRequired = mSensorMap.get(keyFrom).mIntExpBoardPowerRequired;
				mSensorMap.get(keyTo).mListOfConfigOptionKeysAssociated = mSensorMap.get(keyFrom).mListOfConfigOptionKeysAssociated;
				
//				mSensorMap.get(keyTo).mSensorBitmapIDStreaming = mSensorMap.get(keyFrom).mSensorBitmapIDStreaming;
//				mSensorMap.get(keyTo).mSensorBitmapIDSDLogHeader = mSensorMap.get(keyFrom).mSensorBitmapIDSDLogHeader;
//				mSensorMap.get(keyTo).mDerivedChannelBitmapBitShift = mSensorMap.get(keyFrom).mDerivedChannelBitmapBitShift;
//				mSensorMap.get(keyTo).mDerivedChannelBitmapMask = mSensorMap.get(keyFrom).mDerivedChannelBitmapMask;
			}
		}		
	}
	
	//TODO improve this method - was changed at the last minute and is not fully operational
	public boolean checkIfVersionCompatible(List<ShimmerVerObject> listOfCompatibleVersionInfo) {
		if(listOfCompatibleVersionInfo == null) {
			return true;
		}
		
		for(ShimmerVerObject compatibleVersionInfo:listOfCompatibleVersionInfo) {

			boolean compatible = true;
			
			boolean checkHardwareVersion = false;
			boolean checkExpansionBoardId = false;
			boolean checkFirmwareIdentifier = false;
			boolean checkFirmwareVersionMajor = false;
			boolean checkFirmwareVersionMinor = false;
			boolean checkFirmwareVersionInternal = false;
			
			if(compatibleVersionInfo.mHardwareVersion!=ANY_VERSION) {
				checkHardwareVersion = true;
			}
			if(compatibleVersionInfo.mShimmerExpansionBoardId!=ANY_VERSION) {
				checkExpansionBoardId = true;
			}
			if(compatibleVersionInfo.mFirmwareIdentifier!=ANY_VERSION) {
				checkFirmwareIdentifier = true;
			}
			if(compatibleVersionInfo.mFirmwareVersionMajor!=ANY_VERSION) {
				checkFirmwareVersionMajor = true;
			}
			if(compatibleVersionInfo.mFirmwareVersionMinor!=ANY_VERSION) {
				checkFirmwareVersionMinor = true;
			}
			if(compatibleVersionInfo.mFirmwareVersionInternal!=ANY_VERSION) {
				checkFirmwareVersionInternal = true;
			}
			
			if((compatible)&&(checkHardwareVersion)) {
				if(mHardwareVersion != compatibleVersionInfo.mHardwareVersion) {
					compatible = false;
				}
			}
			if((compatible)&&(checkExpansionBoardId)) {
				if(mExpansionBoardId != compatibleVersionInfo.mShimmerExpansionBoardId) {
					compatible = false;
				}
			}
//			if((compatible)&&(checkFirmwareIdentifier)) {
//				if(mFirmwareIdentifier != compatibleVersionInfo.mFirmwareIdentifier) {
//					compatible = false;
//				}
//			}
			
//			if((compatible)&&(checkFirmwareVersionMajor)) {
//				if(mFirmwareVersionMajor < compatibleVersionInfo.mFirmwareVersionMajor) {
//					compatible = false;
//				}
//				if((compatible)&&(checkFirmwareVersionMinor)) {
//					if(mFirmwareVersionMinor < compatibleVersionInfo.mFirmwareVersionMinor) {
//						compatible = false;
//					}
//				}
//				if((compatible)&&(checkFirmwareVersionInternal)) {
//					if(mFirmwareVersionInternal < compatibleVersionInfo.mFirmwareVersionInternal) {
//						compatible = false;
//					}
//				}
//			}
//			else if((compatible)&&(checkFirmwareVersionMinor)) {
//				if(mFirmwareVersionMinor < compatibleVersionInfo.mFirmwareVersionMinor) {
//					compatible = false;
//				}
//				if((compatible)&&(checkFirmwareVersionInternal)) {
//					if(mFirmwareVersionInternal < compatibleVersionInfo.mFirmwareVersionInternal) {
//						compatible = false;
//					}
//				}
//			}
//			else if((compatible)&&(checkFirmwareVersionInternal)) {
//				if(mFirmwareVersionInternal < compatibleVersionInfo.mFirmwareVersionInternal) {
//					compatible = false;
//				}
//			}
			
			if(checkFirmwareVersionMajor){
				// Using the tree structure below each of the FW Major, Minor or Internal Release variables can be ignored
				if((compatible)&&(!Util.compareVersions(mFirmwareIdentifier, 
						mFirmwareVersionMajor, 
						mFirmwareVersionMinor, 
						mFirmwareVersionInternal, 
						compatibleVersionInfo.mFirmwareIdentifier, 
						compatibleVersionInfo.mFirmwareVersionMajor, 
						compatibleVersionInfo.mFirmwareVersionMinor, 
						compatibleVersionInfo.mFirmwareVersionInternal))){
					compatible = false;
				}
			}
			
			if(compatible) {
				return true;
			}
		}
		return false;
	}

	
	public List<Integer> sensorMapConflictCheck(Integer key){
		List<Integer> listOfChannelConflicts = new ArrayList<Integer>();
		
		//TODO: handle Shimmer2/r exceptions which involve get5VReg(), getPMux() and writePMux()
		
		if (mHardwareVersion == HW_ID.SHIMMER_3){
			if(mSensorMap.get(key).mListOfSensorMapKeysConflicting != null) {
				for(Integer sensorMapKey:mSensorMap.get(key).mListOfSensorMapKeysConflicting) {
					if(mSensorMap.get(sensorMapKey) != null) {
						if(mSensorMap.get(sensorMapKey).mIsEnabled == true) {
							listOfChannelConflicts.add(sensorMapKey);
						}
					}
				}
			}
		}
		
		if(listOfChannelConflicts.isEmpty()) {
			return null;
		}
		else {
			return listOfChannelConflicts;
		}
	}
	
	/**
	 * @param key This takes in a single sensor map key to check for conflicts and correct
	 * @return enabledSensors This returns the new set of enabled sensors, where any sensors which conflicts with sensorToCheck is disabled on the bitmap, so sensorToCheck can be accomodated (e.g. for Shimmer2 using ECG will disable EMG,GSR,..basically any daughter board)
	 * @return boolean 
	 *  
	 */
	public void sensorMapConflictCheckandCorrect(int key){
		
		if(mSensorMap.get(key) != null) {
			if(mSensorMap.get(key).mListOfSensorMapKeysConflicting != null) {
				for(Integer sensorMapKey:mSensorMap.get(key).mListOfSensorMapKeysConflicting) {
					if(mSensorMap.get(sensorMapKey) != null) {
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
						if(mSensorMap.get(sensorMapKey).isDerivedChannel()) {
							mDerivedSensors &= ~mSensorMap.get(sensorMapKey).mDerivedSensorBitmapID;
						}
						setDefaultConfigForSensor(sensorMapKey, mSensorMap.get(sensorMapKey).mIsEnabled);
					}
				}
			}
		}
		sensorMapCheckandCorrectSensorDependencies();
		sensorMapCheckandCorrectHwDependencies();
	}

	
	private void sensorMapCheckandCorrectSensorDependencies() {
		//Cycle through any required sensors and update sensorMap channel enable values
		for(Integer sensorMapKey:mSensorMap.keySet()) {
			if(mSensorMap.get(sensorMapKey).mListOfSensorMapKeysRequired != null) {
				for(Integer requiredSensorKey:mSensorMap.get(sensorMapKey).mListOfSensorMapKeysRequired) {
					if(mSensorMap.get(requiredSensorKey).mIsEnabled == false) {
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
						if(mSensorMap.get(sensorMapKey).isDerivedChannel()) {
							mDerivedSensors &= ~mSensorMap.get(sensorMapKey).mDerivedSensorBitmapID;
						}
						setDefaultConfigForSensor(sensorMapKey, mSensorMap.get(sensorMapKey).mIsEnabled);
						break;
					}
				}
			}
			
			if (mHardwareVersion == HW_ID.SHIMMER_3){
				//Exceptions for Shimmer3 ExG
				
				// If ECG or ExG_Test (i.e., two ExG chips)
				if(((sensorMapKey==Configuration.Shimmer3.SensorMapKey.ECG)||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_TEST)||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION))&&(mSensorMap.get(sensorMapKey).mIsEnabled)) {
					if(!(((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled)&&(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_16BIT).mIsEnabled))
							||((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled)&&(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG2_24BIT).mIsEnabled)))){
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
					}
					else {
						
					}
				}
				// Else if EMG (i.e., one ExG chip)
				else if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.EMG)&&(mSensorMap.get(sensorMapKey).mIsEnabled == true)) {
					if(!((mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_16BIT).mIsEnabled)||(mSensorMap.get(Configuration.Shimmer3.SensorMapKey.EXG1_24BIT).mIsEnabled))){
						mSensorMap.get(sensorMapKey).mIsEnabled = false;
					}
				}	
			}

		}
	}
	

	private void sensorMapCheckandCorrectHwDependencies() {
		for(Integer sensorMapKey:mSensorMap.keySet()) {
			if(mSensorMap.get(sensorMapKey).mListOfCompatibleVersionInfo != null) {
				if(!checkIfVersionCompatible(mSensorMap.get(sensorMapKey).mListOfCompatibleVersionInfo)) {
					mSensorMap.get(sensorMapKey).mIsEnabled = false;
					if(mSensorMap.get(sensorMapKey).isDerivedChannel()) {
						mDerivedSensors &= ~mSensorMap.get(sensorMapKey).mDerivedSensorBitmapID;
					}
					setDefaultConfigForSensor(sensorMapKey, mSensorMap.get(sensorMapKey).mIsEnabled);
				}
			}
		}
	}
	
//	private void setDefaultConfigForDisabledSensors() {
//		for(Integer sensorMapKey:mSensorMap.keySet()) {
//			if(mSensorMap.get(sensorMapKey).mIsEnabled == false) {
//				setDefaultConfigForSensor(sensorMapKey, mSensorMap.get(sensorMapKey).mIsEnabled);
//			}
//		}
//	}

	//TODO set defaults when ").mIsEnabled = false)" is set manually in the code
	private void setDefaultConfigForSensor(int sensorMapKey, boolean state) {
		if(sensorMapKey==Configuration.Shimmer3.SensorMapKey.LSM303DLHC_ACCEL) {
			setDefaultLsm303dlhcAccelSensorConfig(state);
		}
		else if(sensorMapKey==Configuration.Shimmer3.SensorMapKey.LSM303DLHC_MAG) {
			setDefaultLsm303dlhcMagSensorConfig(state);
		}
		else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.MPU9150_GYRO){
			setDefaultMpu9150GyroSensorConfig(state);
		}
		else if(sensorMapKey == Configuration.Shimmer3.SensorMapKey.MPU9150_ACCEL){
			setDefaultMpu9150AccelSensorConfig(state);
		}
		
		else if(sensorMapKey==Configuration.Shimmer3.SensorMapKey.BMP180_PRESSURE) {
			setDefaultBmp180PressureSensorConfig(state);
		}
		else if(sensorMapKey==Configuration.Shimmer3.SensorMapKey.GSR) {
			setDefaultGsrSensorConfig(state);
		}
		else if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.ECG)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_TEST)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EMG)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG1_16BIT)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG2_16BIT)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG1_24BIT)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG2_24BIT)) {
			if(!checkIfOtherExgChannelEnabled()) {
				setDefaultECGConfiguration();
			}
		}
		
		else if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG)
				||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW)) {
			if(!checkIfOtherMplChannelEnabled()) {
				setDefaultMpu9150MplSensorConfig(state);
			}
		}
		
	}
	
	private void setDefaultBmp180PressureSensorConfig(boolean state) {
		mPressureResolution = 0;
	}
	
	private void setDefaultLsm303dlhcAccelSensorConfig(boolean state) {
		if(state) {
			setLowPowerAccelWR(false);
		}
		else {
			mAccelRange = 0;
			setLowPowerAccelWR(true);
//			setLSM303AccelRateFromFreq(mShimmerSamplingRate);
		}
	}

	private void setDefaultLsm303dlhcMagSensorConfig(boolean state) {
		if(state) {
			setLowPowerMag(false);
		}
		else {
			mMagRange=1;
			setLowPowerMag(true);
//			setLSM303MagRateFromFreq(mShimmerSamplingRate);
		}
	}

	private void setDefaultGsrSensorConfig(boolean state) {
		if(state) {
		}
		else {
			mGSRRange=4;
		}
	}

	private void setDefaultMpu9150GyroSensorConfig(boolean state) {
		if(state) {
			setLowPowerGyro(false);
		}
		else {
			mGyroRange=1;
			setLowPowerGyro(true);
//			setMPU9150GyroAccelRateFromFreq(mShimmerSamplingRate);
		}
	}
	
	private void setDefaultMpu9150AccelSensorConfig(boolean state) {
		mMPU9150AccelRange=0;
		
	}
	
	private void setDefaultMpu9150MplSensorConfig(boolean state) {
		mMPU9150DMP = 0;
		mMPU9150LPF = 0;
		mMPU9150MotCalCfg = 0;
		mMPU9150MPLSamplingRate = 0;
		mMPU9150MagSamplingRate = 0;
		mMPLSensorFusion = 0;
		mMPLGyroCalTC = 0;
		mMPLVectCompCal = 0;
		mMPLMagDistCal = 0;
		mMPLEnable = 0;
	}
	
	private boolean checkIfOtherMplChannelEnabled() {
		for(Integer sensorMapKey:mSensorMap.keySet()) {
			if(mSensorMap.get(sensorMapKey).mIsEnabled) {
				if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_9DOF)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_6DOF)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_EULER_9DOF)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_HEADING)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_PEDOMETER)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_TAP)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MOTION_ORIENT)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_GYRO)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_ACCEL)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_MAG)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.MPU9150_MPL_QUAT_6DOF_RAW)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkIfOtherExgChannelEnabled() {
		for(Integer sensorMapKey:mSensorMap.keySet()) {
			if(mSensorMap.get(sensorMapKey).mIsEnabled) {
				if((sensorMapKey==Configuration.Shimmer3.SensorMapKey.ECG)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_TEST)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EMG)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG_RESPIRATION)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG1_16BIT)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG2_16BIT)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG1_24BIT)
					||(sensorMapKey==Configuration.Shimmer3.SensorMapKey.EXG2_24BIT)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	 /**
	 * @return the InfoMem byte size. HW and FW version needs to be set first for this to operate correctly.
	 */
	public int getExpectedInfoMemByteLength() {
		InfoMemLayout iML = new InfoMemLayout(mFirmwareIdentifier, mFirmwareVersionMajor, mFirmwareVersionMinor, mFirmwareVersionInternal);
		return iML.mInfoMemSize;
	}
	
	 /**
	 * @return the mConfigTime
	 */
	public long getConfigTime() {
		return mConfigTime;
	}

	 /**
	 * @return the mConfigTime in a parsed String format (yyyy-MM-dd hh:MM:ss)
	 */
	public String getConfigTimeParsed() {
		return Util.convertSecondsToDateString(mConfigTime);
	}
	
	/**
	 * @return the mBufferSize
	 */
	public int getBufferSize() {
		return mBufferSize;
	}
	
	/**
	 * @return the mMPU9150AccelRange
	 */
	public int getMPU9150AccelRange() {
		return mMPU9150AccelRange;
	}

	/**
	 * @return the mMPU9150GyroAccelRate
	 */
	public int getMPU9150GyroAccelRate() {
		return mMPU9150GyroAccelRate;
	}

	/**
	 * @return the mMPU9150MotCalCfg
	 */
	public int getMPU9150MotCalCfg() {
		return mMPU9150MotCalCfg;
	}
	/**
	 * @return the mMPU9150LPF
	 */
	public int getMPU9150LPF() {
		return mMPU9150LPF;
	}

	/**
	 * @return the mMPU9150MPLSamplingRate
	 */
	public int getMPU9150MPLSamplingRate() {
		return mMPU9150MPLSamplingRate;
	}

	/**
	 * @return the mMPU9150MagSamplingRate
	 */
	public int getMPU9150MagSamplingRate() {
		return mMPU9150MagSamplingRate;
	}

	/**
	 * @return the mMasterShimmer
	 */
	public int getMasterShimmer() {
		return mMasterShimmer;
	}

	/**
	 * @return the mSingleTouch
	 */
	public int getSingleTouch() {
		return mSingleTouch;
	}

	/**
	 * @return the mTCXO
	 */
	public int getTCXO() {
		return mTCXO;
	}

	/**
	 * @return the mExperimentName
	 */
	public String getExperimentName() {
		return mExperimentName;
	}

	/**
	 * @return the mExperimentNumberOfShimmers
	 */
	public int getExperimentNumberOfShimmers() {
		return mExperimentNumberOfShimmers;
	}
	
	/**
	 * @return the mExperimentDurationEstimated
	 */
	public int getExperimentDurationEstimated() {
		return mExperimentDurationEstimated;
	}

	/**
	 * @return the mExperimentDurationMaximum
	 */
	public int getExperimentDurationMaximum() {
		return mExperimentDurationMaximum;
	}

	/**
	 * @return the mLSM303DigitalAccelLPM
	 */
	public boolean isLSM303DigitalAccelLPM() {
		return mLowPowerAccelWR;
	}
	
	/**
	 * @return true if ExG respiration detection frequency is 32kHz and false if 64kHz
	 */
	public boolean isExgRespirationDetectFreq32kHz() {
		if(mEXG2RespirationDetectFreq==0)
			return true;
		else
			return false;
	}

	/**
	 * @return the mLSM303DigitalAccelHRM
	 */
	public boolean isLSM303DigitalAccelHRM() {
		return mHighResAccelWR;
	}
	
//	/**
//	 * @return the mLowPowerMag
//	 */
//	public boolean ismLowPowerMag() {
//		return mLowPowerMag;
//	}
//
//	/**
//	 * @return the mLowPowerGyro
//	 */
//	public boolean ismLowPowerGyro() {
//		return mLowPowerGyro;
//	}

	/**
	 * @return the mShimmerUserAssignedName
	 */
	public String getShimmerUserAssignedName() {
		return mShimmerUserAssignedName;
	}

	/**
	 * @return the mLSM303MagRate
	 */
	public int getLSM303MagRate() {
		return mLSM303MagRate;
	}

	/**
	 * @return the mLSM303DigitalAccelRate
	 */
	public int getLSM303DigitalAccelRate() {
		return mLSM303DigitalAccelRate;
	}

	/**
	 * @return the mMacIdFromInfoMem
	 */
	public String getMacIdFromInfoMem() {
		return mMacIdFromInfoMem;
	}

	/**
	 * @return the mExperimentId
	 */
	public int getExperimentId() {
		return mExperimentId;
	}

	/**
	 * @return the syncNodesList
	 */
	public List<String> getSyncNodesList() {
		return syncNodesList;
	}


	/**
	 * @return the mSyncBroadcastInterval
	 */
	public int getSyncBroadcastInterval() {
		return mSyncBroadcastInterval;
	}


	/**
	 * @return the mSyncWhenLogging
	 */
	public int getSyncWhenLogging() {
		return mSyncWhenLogging;
	}


	/**
	 * @return the mBluetoothBaudRate
	 */
	public int getBluetoothBaudRate() {
		return mBluetoothBaudRate;
	}


	/**
	 * @return the mExGResolution
	 */
	public int getExGResolution() {
		return mExGResolution;
	}

	/**
	 * @return the mShimmerVersion
	 */
	public int getHardwareVersion() {
		return mHardwareVersion;
	}

	/**
	 * @return the mSensorMap
	 */
	public TreeMap<Integer,SensorDetails> getSensorMap() {
		return mSensorMap;
	}

	/**
	 * @return the mConfigOptionsMap
	 */
	public HashMap<String, SensorConfigOptionDetails> getConfigOptionsMap() {
		return mConfigOptionsMap;
	}

	/**
	 * @return the mChannelTileMap
	 */
	public LinkedHashMap<String, SensorTileDetails> getChannelTileMap() {
		return mSensorTileMap;
	}


	/**
	 * @return the mEXG1RateSetting
	 */
	public int getEXG1RateSetting() {
		return mEXG1RateSetting;
	}


	/**
	 * @return the mEXG1CH1GainSetting
	 */
	public int getEXG1CH1GainSetting() {
		return mEXG1CH1GainSetting;
	}


	/**
	 * @return the mEXG1CH2GainSetting
	 */
	public int getEXG1CH2GainSetting() {
		return mEXG1CH2GainSetting;
	}



	/**
	 * @return the mEXG2RateSetting
	 */
	public int getEXG2RateSetting() {
		return mEXG2RateSetting;
	}


	/**
	 * @return the mEXG2CH1GainSetting
	 */
	public int getEXG2CH1GainSetting() {
		return mEXG2CH1GainSetting;
	}


	/**
	 * @return the mEXG2CH2GainSetting
	 */
	public int getEXG2CH2GainSetting() {
		return mEXG2CH2GainSetting;
	}


	/**
	 * @return the mEXGReferenceElectrode
	 */
	public int getEXGReferenceElectrode() {
		return mEXGReferenceElectrode;
	}


	/**
	 * @return the mEXG1LeadOffCurrentMode
	 */
	public int getEXG1LeadOffCurrentMode() {
		return mEXG1LeadOffCurrentMode;
	}


	/**
	 * @return the mEXG2LeadOffCurrentMode
	 */
	public int getEXG2LeadOffCurrentMode() {
		return mEXG2LeadOffCurrentMode;
	}


	/**
	 * @return the mEXG1Comparators
	 */
	public int getEXG1Comparators() {
		return mEXG1Comparators;
	}


	/**
	 * @return the mEXG2Comparators
	 */
	public int getEXG2Comparators() {
		return mEXG2Comparators;
	}


	/**
	 * @return the mEXGRLDSense
	 */
	public int getEXGRLDSense() {
		return mEXGRLDSense;
	}


	/**
	 * @return the mEXG1LeadOffSenseSelection
	 */
	public int getEXG1LeadOffSenseSelection() {
		return mEXG1LeadOffSenseSelection;
	}


	/**
	 * @return the mEXG2LeadOffSenseSelection
	 */
	public int getEXG2LeadOffSenseSelection() {
		return mEXG2LeadOffSenseSelection;
	}


	/**
	 * @return the mEXGLeadOffDetectionCurrent
	 */
	public int getEXGLeadOffDetectionCurrent() {
		return mEXGLeadOffDetectionCurrent;
	}


	/**
	 * @return the mEXGLeadOffComparatorTreshold
	 */
	public int getEXGLeadOffComparatorTreshold() {
		return mEXGLeadOffComparatorTreshold;
	}


	/**
	 * @return the mEXG2RespirationDetectState
	 */
	public int getEXG2RespirationDetectState() {
		return mEXG2RespirationDetectState;
	}


	/**
	 * @return the mEXG2RespirationDetectFreq
	 */
	public int getEXG2RespirationDetectFreq() {
		return mEXG2RespirationDetectFreq;
	}


	/**
	 * @return the mEXG2RespirationDetectPhase
	 */
	public int getEXG2RespirationDetectPhase() {
		return mEXG2RespirationDetectPhase;
	}

	public double getShimmerSamplingRate(){
		return mShimmerSamplingRate; 
	}


	/**
	 * @return the mFirmwareIdentifier
	 */
	public int getFirmwareIdentifier() {
		return mFirmwareIdentifier;
	}
	
	public double getPressTempAC1(){
		return AC1;
	}
	
	public double getPressTempAC2(){
		return AC2;
	}
	
	public double getPressTempAC3(){
		return AC3;
	}
	
	public double getPressTempAC4(){
		return AC4;
	}
	
	public double getPressTempAC5(){
		return AC5;
	}
	
	public double getPressTempAC6(){
		return AC6;
	}
	
	public double getPressTempB1(){
		return B1;
	}
	
	public double getPressTempB2(){
		return B2;
	}
	
	public double getPressTempMB(){
		return MB;
	}
	
	public double getPressTempMC(){
		return MC;
	}
	
	public double getPressTempMD(){
		return MD;
	}


	/**
	 * @return the mDerivedSensors
	 */
	public long getDerivedSensors() {
		return mDerivedSensors;
	}

	public boolean isUsingConfigFromInfoMem() {
		return mShimmerUsingConfigFromInfoMem;
	}

	/**
	 * @return the mPpgAdcSelectionGsrBoard
	 */
	public int getPpgAdcSelectionGsrBoard() {
		return mPpgAdcSelectionGsrBoard;
	}

	/**
	 * @return the mPpg1AdcSelectionProto3DeluxeBoard
	 */
	public int getPpg1AdcSelectionProto3DeluxeBoard() {
		return mPpg1AdcSelectionProto3DeluxeBoard;
	}

	/**
	 * @return the mPpg2AdcSelectionProto3DeluxeBoard
	 */
	public int getPpg2AdcSelectionProto3DeluxeBoard() {
		return mPpg2AdcSelectionProto3DeluxeBoard;
	}

	/**
	 * @return the mMacIdFromUart
	 */
	public String getMacIdFromUart() {
		return mMacIdFromUart;
	}

	/**
	 * @return the mMacIdFromUartParsed
	 */
	public String getMacIdFromUartParsed() {
		return mMacIdFromUartParsed;
	}

	/**
	 * @return the mSamplingDividerVBatt
	 */
	public int getSamplingDividerVBatt() {
		return mSamplingDividerVBatt;
	}

	/**
	 * @return the mSamplingDividerGsr
	 */
	public int getSamplingDividerGsr() {
		return mSamplingDividerGsr;
	}

	/**
	 * @return the mMPU9150GyroAccelRate in Hz
	 */
	public double getMPU9150GyroAccelRateInHz() {
		// Gyroscope Output Rate = 8kHz when the DLPF is disabled (DLPF_CFG = 0 or 7), and 1kHz when the DLPF is enabled
		double numerator = 1000.0;
		if(mMPU9150LPF == 0) {
			numerator = 8000.0;
		}
		
		if(mMPU9150GyroAccelRate == 0) {
			return numerator;
		}
		else {
			return (numerator / mMPU9150GyroAccelRate);
		}
	}
	
	
	/**
	 * @return the mSamplingDividerPpg
	 */
	public int getSamplingDividerPpg() {
		return mSamplingDividerPpg;
	}

	/**
	 * @return the mSamplingDividerLsm303dlhcAccel
	 */
	public int getSamplingDividerLsm303dlhcAccel() {
		return mSamplingDividerLsm303dlhcAccel;
	}

	/**
	 * @return the mHighResAccelWR
	 */
	public boolean isHighResAccelWR() {
		return mHighResAccelWR;
	}

	/**
	 * @return the mLowPowerAccelWR
	 */
	public boolean isLowPowerAccelWR() {
		return mLowPowerAccelWR;
	}


}
