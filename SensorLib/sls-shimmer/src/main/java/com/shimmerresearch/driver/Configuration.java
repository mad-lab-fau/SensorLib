/*Rev 2.6
 * 
 * 
 *  Copyright (c) 2010, Shimmer Research, Ltd.
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
 * @date   May, 2014
 * 
 * The purpose of this code is to maintain the configurations of BTSTREAM
 * 
 * 
 * Changes since 2.5 (RM first revision)
 * - Addition of Strain Gauge for Shimmer3
 * 
 * Changes since 2.2
 * - Changed list of compatible sensors to public
 * 
 */


package com.shimmerresearch.driver;

public class Configuration {
	//Channel Contents
	public static class Shimmer3Gq{
		public class GuiLabelConfig{
			public static final String SAMPLING_RATE_DIVIDER_VBATT = "VBATT Divider";
			public static final String SAMPLING_RATE_DIVIDER_GSR = "GSR Divider";
			public static final String SAMPLING_RATE_DIVIDER_PPG = "PPG Divider";
			public static final String SAMPLING_RATE_DIVIDER_LSM303DLHC_ACCEL = "LSM3030DLHC Divider";
		}
		
		public final static String[] ListofSamplingRateDividers={"0.75Hz","1.5Hz","3Hz","7.5Hz","15Hz","30Hz","75Hz","220Hz"};
		public final static Integer[] ListofSamplingRateDividersValues={0,1,2,3,4,5,6,7};

	}
	public static class CHANNEL_UNITS{
		//Sensors units
		public static final String NO_UNITS = "no units";
		public static final String MILLISECONDS = "ms";
		public static final String METER_PER_SECOND_SQUARE = "m/(s^2)";
		public static final String DEGREES_PER_SECOND = "deg/s";
		public static final String LOCAL_FLUX = "local_flux";
		public static final String KOHMS = "kOhms";
		public static final String MILLIVOLTS = "mV";
		public static final String BEATS_PER_MINUTE = "BPM";
		public static final String KPASCAL = "kPa";
		public static final String DEGREES_CELSUIS = "Degrees Celsius";
		public static final String DEGREES = "Degrees";
		public static final String U_TESLA = "uT";
		public static final String DATE_FORMAT = "yyyy/mm/dd hh:mm:ss.000";
		public static final String GRAVITY = "g";
		public static final String CLOCK_UNIT = "Ticks";

		public static final String ACCEL_CAL_UNIT = METER_PER_SECOND_SQUARE;
		public static final String ACCEL_DEFAULT_CAL_UNIT = METER_PER_SECOND_SQUARE+"*";
		public static final String GYRO_CAL_UNIT = DEGREES_PER_SECOND;
		public static final String GYRO_DEFAULT_CAL_UNIT = DEGREES_PER_SECOND+"*";
		public static final String MAG_CAL_UNIT = LOCAL_FLUX;
		public static final String MAG_DEFAULT_CAL_UNIT = LOCAL_FLUX+"*";
		
		public static final String LOCAL = "local"; //used for axis-angle and madgewick quaternions
	}	
	
	public static class CHANNEL_TYPE{
		public static final String CAL = "CAL";
		public static final String UNCAL = "UNCAL";
		public static final String RAW = "RAW";
	}
	
	public static class Shimmer3{
		public class Channel{
			public final static int XAAccel     			 = 0x00;
			public final static int YAAccel    				 = 0x01;
			public final static int ZAAccel     			 = 0x02;
			public final static int VBatt       			 = 0x03;
			public final static int XDAccel     			 = 0x04;
			public final static int YDAccel     			 = 0x05;
			public final static int ZDAccel     			 = 0x06;
			public final static int XMag        			 = 0x07;
			public final static int YMag        			 = 0x08;
			public final static int ZMag        			 = 0x09;
			public final static int XGyro       			 = 0x0A;
			public final static int YGyro       			 = 0x0B;
			public final static int ZGyro       			 = 0x0C;
			public final static int ExtAdc7					 = 0x0D;
			public final static int ExtAdc6					 = 0x0E;
			public final static int ExtAdc15 				 = 0x0F;
			public final static int IntAdc1					 = 0x10;
			public final static int IntAdc12 				 = 0x11;
			public final static int IntAdc13 				 = 0x12;
			public final static int IntAdc14 				 = 0x13;
			public final static int XAlterAccel      		 = 0x14; //Alternative Accelerometer
			public final static int YAlterAccel     		 = 0x15;
			public final static int ZAlterAccel     		 = 0x16;
			public final static int XAlterMag        		 = 0x17; //Alternative Magnetometer
			public final static int YAlterMag        		 = 0x18;
			public final static int ZAlterMag        		 = 0x19;
			public final static int Temperature 			 = 0x1A;
			public final static int Pressure 				 = 0x1B;
			public final static int GsrRaw 					 = 0x1C;
			public final static int EXG_ADS1292R_1_STATUS 	 = 0x1D;
			public final static int EXG_ADS1292R_1_CH1_24BIT = 0x1E;
			public final static int EXG_ADS1292R_1_CH2_24BIT = 0x1F;
			public final static int EXG_ADS1292R_2_STATUS 	 = 0x20;
			public final static int EXG_ADS1292R_2_CH1_24BIT = 0x21;
			public final static int EXG_ADS1292R_2_CH2_24BIT = 0x22;
			public final static int EXG_ADS1292R_1_CH1_16BIT = 0x23;
			public final static int EXG_ADS1292R_1_CH2_16BIT = 0x24;
			public final static int EXG_ADS1292R_2_CH1_16BIT = 0x25;
			public final static int EXG_ADS1292R_2_CH2_16BIT = 0x26;
			public final static int BridgeAmpHigh  			 = 0x27;
			public final static int BridgeAmpLow   			 = 0x28;
		}

		public class SensorBitmap{
			//Sensor Bitmap for Shimmer 3
			public static final int SENSOR_A_ACCEL			   = 0x80;
			public static final int SENSOR_GYRO			   	   = 0x40;
			public static final int SENSOR_MAG				   = 0x20;
			public static final int SENSOR_EXG1_24BIT			   = 0x10;
			public static final int SENSOR_EXG2_24BIT			   = 0x08;
			public static final int SENSOR_GSR					   = 0x04;
			public static final int SENSOR_EXT_A7				   = 0x02;
			public static final int SENSOR_EXT_A6				   = 0x01;
			public static final int SENSOR_VBATT				   = 0x2000;
			public static final int SENSOR_D_ACCEL			   = 0x1000;
			public static final int SENSOR_EXT_A15				   = 0x0800;
			public static final int SENSOR_INT_A1				   = 0x0400;
			public static final int SENSOR_INT_A12				   = 0x0200;
			public static final int SENSOR_INT_A13				   = 0x0100;
			public static final int SENSOR_INT_A14				   = 0x800000;
			public static final int SENSOR_BMP180				   = 0x40000;
			public static final int SENSOR_EXG1_16BIT			   = 0x100000;
			public static final int SENSOR_EXG2_16BIT			   = 0x080000;
			public static final int SENSOR_BRIDGE_AMP			   = 0x8000;
		}

		public final static String[] ListofCompatibleSensors={"Low Noise Accelerometer","Wide Range Accelerometer","Gyroscope","Magnetometer","Battery Voltage","External ADC A7","External ADC A6","External ADC A15","Internal ADC A1","Internal ADC A12","Internal ADC A13","Internal ADC A14","Pressure","GSR","EXG1","EXG2","EXG1 16Bit","EXG2 16Bit", "Bridge Amplifier"}; 
		public final static String[] ListofAccelRange={"+/- 2g","+/- 4g","+/- 8g","+/- 16g"};
		public final static String[] ListofGyroRange={"+/- 250 dps","+/- 500 dps","+/- 1000 dps","+/- 2000 dps"}; 
		public final static String[] ListofMagRange={"+/- 1.3 Ga","+/- 1.9 Ga","+/- 2.5 Ga","+/- 4.0 Ga","+/- 4.7 Ga","+/- 5.6 Ga","+/- 8.1 Ga"}; 
		public final static String[] ListofPressureResolution={"Low","Standard","High","Very High"};
		public final static String[] ListofGSRRange={"10k\u2126 to 56k\u2126","56k\u2126 to 220k\u2126","220k\u2126 to 680k\u2126","680k\u2126 to 4.7M\u2126","Auto"};
		public final static String[] ListofDefaultEXG={"ECG","EMG","Test Signal"};
		public final static String[] ListOfExGGain={"6","1","2","3","4","8","12"};
		public final static String[] ListOfECGReferenceElectrode={"Inverse Wilson CT","Fixed Potential"};
		public final static String[] ListOfEMGReferenceElectrode={"Fixed Potential", "Inverse of Ch1"};
		public final static String[] ListOfExGLeadOffDetection={"Off","DC Current"};
		public final static String[] ListOfExGLeadOffCurrent={"6 nA","22 nA", "6 uA", "22 uA"};
		public final static String[] ListOfExGLeadOffComparator={"Pos:95%-Neg:5%","Pos:92.5%-Neg:7.5%","Pos:90%-Neg:10%","Pos:87.5%-Neg:12.5%","Pos:85%-Neg:15%","Pos:80%-Neg:20%","Pos:75%-Neg:25%","Pos:70%-Neg:30%"};
		public final static String[] ListofMPU9150AccelRange={"+/- 2g","+/- 4g","+/- 8g","+/- 16g"};
		public final static String[] ListofBluetoothBaudRates={"115200","1200","2400","4800","9600","19200","38400","57600","230400","460800","921600"};


		//TODO: From here onwards is is Mark TESTING - not finished
		//TODO: check all indexes below
		public final static Integer[] ListofBluetoothBaudRatesConfigValues={0,1,2,3,4,5,6,7,8,9,10};

		public final static String[] ListofMPU9150MplCalibrationOptions={"No Cal","Fast Cal","1s no motion","2s no motion","5s no motion","10s no motion","30s no motion","60s no motion"};
		public final static String[] ListofMPU9150MplLpfOptions={"No LPF","188Hz","98Hz","42Hz","20Hz","10Hz","5Hz"};

		//		public final static String[] ListofLSM303DLHCAccelRate={"Power-down","1Hz","10Hz","25Hz","50Hz","100Hz","200Hz","400Hz","1620Hz","1344Hz","5376Hz"}; // 1620Hz and 5376Hz are only available in low-power mode, 1344Hz only available in full power mode
		//		public final static Integer[] ListofLSM303DLHCAccelRateConfigValues={0,1,2,3,4,5,6,7,8,9,9};
		public final static String[] ListofLSM303DLHCAccelRate={"Power-down","1Hz","10Hz","25Hz","50Hz","100Hz","200Hz","400Hz","1344Hz"};
		public final static Integer[] ListofLSM303DLHCAccelRateConfigValues={0,1,2,3,4,5,6,7,9};
		public final static String[] ListofLSM303DLHCAccelRateLpm={"Power-down","1Hz","10Hz","25Hz","50Hz","100Hz","200Hz","400Hz","1620Hz","5376Hz"}; // 1620Hz and 5376Hz are only available in low-power mode
		public final static Integer[] ListofLSM303DLHCAccelRateLpmConfigValues={0,1,2,3,4,5,6,7,8,9};

		public final static String[] ListofLSM303DLHCMagRate={"0.75Hz","1.5Hz","3Hz","7.5Hz","15Hz","30Hz","75Hz","220Hz"};
		public final static Integer[] ListofLSM303DLHCMagRateConfigValues={0,1,2,3,4,5,6,7};
		public final static String[] ListofMPU9150MplRate={"10Hz","20Hz","40Hz","50Hz","100Hz"};
		public final static Integer[] ListofMPU9150MplRateConfigValues={0,1,2,3,4};
		public final static String[] ListofMPU9150MagRate={"10Hz","20Hz","40Hz","50Hz","100Hz"};
		public final static Integer[] ListofMPU9150MagRateConfigValues={0,1,2,3,4};

		public final static Integer[] ListofLSM303DLHCAccelRangeConfigValues={0,1,2,3};
		public final static Integer[] ListofMPU9150GyroRangeConfigValues={0,1,2,3};

		public final static Integer[] ListofPressureResolutionConfigValues={0,1,2,3};
		public final static Integer[] ListofGSRRangeConfigValues={0,1,2,3,4};
		public final static Integer[] ListofMagRangeConfigValues={1,2,3,4,5,6,7}; // no '0' option

		public final static Integer[] ListofMPU9150AccelRangeConfigValues={0,1,2,3};
		public final static Integer[] ListofMPU9150MplCalibrationOptionsConfigValues={0,1,2,3,4,5,6,7};
		public final static Integer[] ListofMPU9150MplLpfOptionsConfigValues={0,1,2,3,4,5,6};

		public final static Integer[] ListOfExGGainConfigValues={0,1,2,3,4,5,6};
		public final static String[] ListOfExGResolutions={"16-bit","24-bit"};
		public final static Integer[] ListOfExGResolutionsConfigValues={0,1};

		public final static Integer[] ListOfECGReferenceElectrodeConfigValues={13,0};
		public final static Integer[] ListOfEMGReferenceElectrodeConfigValues={0,3};
		public final static Integer[] ListOfExGLeadOffDetectionConfigValues={-1,0};
		public final static Integer[] ListOfExGLeadOffCurrentConfigValues={0,1,2,3};
		public final static Integer[] ListOfExGLeadOffComparatorConfigValues={0,1,2,3,4,5,6,7};

		public final static String[] ListOfExGRespirationDetectFreq={"32 kHz","64 kHz"};
		public final static Integer[] ListOfExGRespirationDetectFreqConfigValues={0,1};
		public final static String[] ListOfExGRespirationDetectPhase32khz={"0°","11.25°","22.5°","33.75°","45°","56.25°","67.5°","78.75°","90°","101.25°","112.5°","123.75°","135°","146.25°","157.5°","168.75°"};
		public final static Integer[] ListOfExGRespirationDetectPhase32khzConfigValues={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
		public final static String[] ListOfExGRespirationDetectPhase64khz={"0°","22.5°","45°","67.5°","90°","112.5°","135°","157.5°"};
		public final static Integer[] ListOfExGRespirationDetectPhase64khzConfigValues={0,1,2,3,4,5,6,7};

		public final static String[] ListOfExGRate={"125 Hz","250 Hz","500 Hz","1 kHz","2 kHz","4 kHz","8 kHz"};
		public final static Integer[] ListOfExGRateConfigValues={0,1,2,3,4,5,6};

		public final static String[] ListOfOnOff={"On","Off"};
		public final static Integer[] ListOfOnOffConfigValues={0x01,0x00};
		
		
		public final static String[] ListOfPpgAdcSelection={"Int A13","Int A12"};
		public final static Integer[] ListOfPpgAdcSelectionConfigValues={0,1};
		public final static String[] ListOfPpg1AdcSelection={"Int A13","Int A12"};
		public final static Integer[] ListOfPpg1AdcSelectionConfigValues={0,1};
		public final static String[] ListOfPpg2AdcSelection={"Int A1","Int A14"};
		public final static Integer[] ListOfPpg2AdcSelectionConfigValues={0,1};

		
		public class SensorMapKey{
			/**
			 * Shimmer3 Low-noise analog accelerometer
			 */
			public final static int A_ACCEL = 0;
			/**
			 * Shimmer3 Gyroscope
			 */
			public final static int MPU9150_GYRO = 1;
			/**
			 * Shimmer3 Primary magnetometer
			 */
			public final static int LSM303DLHC_MAG = 2;
			public final static int EXG1_24BIT = 3;
			public final static int EXG2_24BIT = 4;
			public final static int GSR = 5;
			public final static int EXT_EXP_ADC_A6 = 6;
			public final static int EXT_EXP_ADC_A7 = 7;
			public final static int BRIDGE_AMP = 8;
			public final static int RESISTANCE_AMP = 9;
			//public final static int HR = 9;
			public final static int VBATT = 10;
			/**
			 * Shimmer3 Wide-range digital accelerometer
			 */
			public final static int LSM303DLHC_ACCEL = 11;
			public final static int EXT_EXP_ADC_A15 = 12;
			public final static int INT_EXP_ADC_A1 = 13;
			public final static int INT_EXP_ADC_A12 = 14;
			public final static int INT_EXP_ADC_A13 = 15;
			public final static int INT_EXP_ADC_A14 = 16;
			/**
			 * Shimmer3 Alternative accelerometer
			 */
			public final static int MPU9150_ACCEL = 17;
			/**
			 * Shimmer3 Alternative magnetometer
			 */
			public final static int MPU9150_MAG = 18;
			public final static int EXG1_16BIT = 19;
			public final static int EXG2_16BIT = 21;
			public final static int BMP180_PRESSURE = 22;
			//public final static int BMP180_TEMPERATURE = 23; // not yet implemented
			//public final static int MSP430_TEMPERATURE = 24; // not yet implemented
			public final static int MPU9150_TEMP = 25;
			//public final static int LSM303DLHC_TEMPERATURE = 26; // not yet implemented
			//public final static int MPU9150_MPL_TEMPERATURE = 1<<17; // same as SENSOR_SHIMMER3_MPU9150_TEMP 
			public final static int MPU9150_MPL_QUAT_6DOF = 27;
			public final static int MPU9150_MPL_QUAT_9DOF = 28;
			public final static int MPU9150_MPL_EULER_6DOF = 29;
			public final static int MPU9150_MPL_EULER_9DOF = 30;
			public final static int MPU9150_MPL_HEADING = 31;
			public final static int MPU9150_MPL_PEDOMETER = 32;
			public final static int MPU9150_MPL_TAP = 33;
			public final static int MPU9150_MPL_MOTION_ORIENT = 34;
			public final static int MPU9150_MPL_GYRO = 35;
			public final static int MPU9150_MPL_ACCEL = 36;
			public final static int MPU9150_MPL_MAG = 37;
			public final static int MPU9150_MPL_QUAT_6DOF_RAW = 38;
	
			// Combination Channels
			public final static int ECG = 100;
			public final static int EMG = 101;
			public final static int EXG_TEST = 102;
			
			// Derived Channels
			public final static int EXG_RESPIRATION = 103;
			public final static int SKIN_TEMP_PROBE = 104;
	
			// Derived Channels - GSR Board
			public final static int PPG_DUMMY = 105;
			public final static int PPG_A12 = 106;
			public final static int PPG_A13 = 107;
			
			// Derived Channels - Proto3 Deluxe Board
			
			public final static int PPG1_DUMMY = 110;
			public final static int PPG1_A12 = 111;
			public final static int PPG1_A13 = 112;
			public final static int PPG2_DUMMY = 113;
			public final static int PPG2_A1 = 114;
			public final static int PPG2_A14 = 115;
		}

		// Sensor Options Map
		public class GuiLabelConfig{
			public static final String SHIMMER_USER_ASSIGNED_NAME = "Shimmer Name";
			public static final String EXPERIMENT_NAME = "Experiment Name";
			public static final String SHIMMER_SAMPLING_RATE = "Sampling Rate";
			public static final String BUFFER_SIZE = "Buffer Size";
			public static final String CONFIG_TIME = "Config Time";
			public static final String EXPERIMENT_NUMBER_OF_SHIMMERS = "Number Of Shimmers";
			public static final String SHIMMER_MAC_FROM_INFOMEM = "InfoMem MAC";
			public static final String EXPERIMENT_ID = "Experiment ID";
			public static final String EXPERIMENT_DURATION_ESTIMATED = "Estimated Duration";
			public static final String EXPERIMENT_DURATION_MAXIMUM = "Maximum Duration";
			public static final String BROADCAST_INTERVAL = "Broadcast Interval";
			public static final String BLUETOOTH_BAUD_RATE = "Bluetooth Baud Rate";

			public static final String USER_BUTTON_START = "User Button";
			public static final String UNDOCK_START = "Undock/Dock";
			public static final String SINGLE_TOUCH_START = "Single Touch Start";
			public static final String EXPERIMENT_MASTER_SHIMMER = "Master Shimmer";
			public static final String EXPERIMENT_SYNC_WHEN_LOGGING = "Sync When Logging";

			public static final String LSM303DLHC_ACCEL_RATE = "Wide Range Accel Rate";
			public static final String LSM303DLHC_ACCEL_RANGE = "Wide Range Accel Range";
			public static final String MPU9150_GYRO_RANGE = "Gyro Range";
			public static final String MPU9150_GYRO_RATE = "Gyro Sampling Rate";
			public static final String LSM303DLHC_MAG_RANGE = "Mag Range";
			public static final String LSM303DLHC_MAG_RATE = "Mag Rate";
			public static final String PRESSURE_RESOLUTION = "Pressure Resolution";
			public static final String GSR_RANGE = "GSR Range";
			public static final String EXG_RESOLUTION = "Resolution";
			public static final String EXG_GAIN = "Gain";

			public static final String EXG_RATE = "Rate";
			public static final String EXG_REFERENCE_ELECTRODE = "Reference Electrode";
			public static final String EXG_LEAD_OFF_DETECTION = "Lead-Off Detection";
			public static final String EXG_LEAD_OFF_CURRENT = "Lead-Off Current";
			public static final String EXG_LEAD_OFF_COMPARATOR = "Lead-Off Compartor Threshold";
			public static final String EXG_RESPIRATION_DETECT_FREQ = "Respiration Detection Freq.";
			public static final String EXG_RESPIRATION_DETECT_PHASE = "Respiration Detection Phase";

			public static final String MPU9150_ACCEL_RANGE = "MPU Accel Range";
			public static final String MPU9150_DMP_GYRO_CAL = "MPU Gyro Cal";
			public static final String MPU9150_LPF = "MPU LPF";
			public static final String MPU9150_MPL_RATE = "MPL Rate";
			public static final String MPU9150_MAG_RATE = "MPU Mag Rate";

			public static final String MPU9150_DMP = "DMP";
			public static final String MPU9150_MPL = "MPL";
			public static final String MPU9150_MPL_9DOF_SENSOR_FUSION = "9DOF Sensor Fusion";
			public static final String MPU9150_MPL_GYRO_CAL = "Gyro Calibration";
			public static final String MPU9150_MPL_VECTOR_CAL = "Vector Compensation Calibration";
			public static final String MPU9150_MPL_MAG_CAL = "Magnetic Disturbance Calibration";

			public static final String KINEMATIC_LPM = "Kinematic Sensors Low-Power Mode";
			public static final String LSM303DLHC_ACCEL_LPM = "Wide Range Accel Low-Power Mode";
			public static final String MPU9150_GYRO_LPM = "Gyro Low-Power Mode";
			public static final String LSM303DLHC_MAG_LPM = "Mag Low-Power Mode";
			public static final String TCX0 = "TCX0";
			public static final String INT_EXP_BRD_POWER_BOOLEAN = "Internal Expansion Board Power";
			public static final String INT_EXP_BRD_POWER_INTEGER = "Int Exp Power";
			
			public static final String PPG_ADC_SELECTION = "PPG Channel";
			public static final String PPG1_ADC_SELECTION = "PPG1 Channel";
			public static final String PPG2_ADC_SELECTION = "PPG2 Channel";

		}

		// GUI Sensor Tiles
		public class GuiLabelSensorTiles{
			public static final String LOW_NOISE_ACCEL = Configuration.Shimmer3.GuiLabelSensors.ACCEL_LN;
			public static final String GYRO = Configuration.Shimmer3.GuiLabelSensors.GYRO;
			public static final String MAG = Configuration.Shimmer3.GuiLabelSensors.MAG;
			public static final String BATTERY_MONITORING = Configuration.Shimmer3.GuiLabelSensors.BATTERY;
			public static final String WIDE_RANGE_ACCEL = Configuration.Shimmer3.GuiLabelSensors.ACCEL_WR;
			public static final String PRESSURE_TEMPERATURE = Configuration.Shimmer3.GuiLabelSensors.PRESS_TEMP_BMP180;
			public static final String EXTERNAL_EXPANSION_ADC = "External Expansion ADCs";
			public static final String GSR = "GSR+";
			public static final String EXG = "ECG/EMG";
			public static final String PROTO3_MINI = "Proto Mini";
			public static final String PROTO3_DELUXE = "Proto Deluxe";
			public static final String PROTO3_DELUXE_SUPP = "PPG";
			public static final String BRIDGE_AMPLIFIER = "Bridge Amplifier+";
			public static final String BRIDGE_AMPLIFIER_SUPP = Configuration.Shimmer3.GuiLabelSensors.SKIN_TEMP_PROBE;
			public static final String HIGH_G_ACCEL = Configuration.Shimmer3.GuiLabelSensors.HIGH_G_ACCEL;
			public static final String INTERNAL_EXPANSION_ADC = "Internal Expansion ADCs";
			//public static final String GPS = "GPS";
		}
		
		//GUI SENSORS
		public class GuiLabelSensors{
			public static final String ACCEL_LN = "Low-Noise Accelerometer";
			public static final String BATTERY = "Battery Voltage";
			public static final String EXT_EXP_A7 = "Ext A7";
			public static final String EXT_EXP_A6 = "Ext A6";
			public static final String EXT_EXP_A15 = "Ext A15";
			public static final String INT_EXP_A12 = "Int A12";
			public static final String INT_EXP_A13 = "Int A13";
			public static final String INT_EXP_A14 = "Int A14";
			public static final String BRIDGE_AMPLIFIER = "Bridge Amp";
			public static final String GSR = "GSR";
			public static final String INT_EXP_A1 = "Int A1";
			public static final String RESISTANCE_AMP = "Resistance Amp";
			public static final String GYRO = "Gyroscope";
			public static final String ACCEL_WR = "Wide-Range Accelerometer";
			public static final String MAG = "Magnetometer";
			public static final String ACCEL_MPU = "Alternative Accel";
			public static final String MAG_MPU = "Alternative Mag";
			public static final String PRESS_TEMP_BMP180 = "Pressure & Temperature";
			public static final String EMG = "EMG";
			public static final String ECG = "ECG";
			public static final String EXG_TEST = "Test";
			public static final String EXT_EXP_ADC = "External Expansion";
			public static final String QUAT_MPL_6DOF = "MPU Quat 6DOF";
			public static final String QUAT_MPL_9DOF = "MPU Quat 9DOF";
			public static final String EULER_MPL_6DOF = "MPU Euler 6DOF";
			public static final String EULER_MPL_9DOF = "MPU Euler 9DOF";
			public static final String MPL_HEADING = "MPU Heading";
			public static final String MPL_TEMPERATURE = "MPU Temp";
			public static final String MPL_PEDOM_CNT = "MPL_Pedom_cnt"; // not currently supported
			public static final String MPL_PEDOM_TIME = "MPL_Pedom_Time"; // not currently supported
			public static final String MPL_TAPDIRANDTAPCNT = "TapDirAndTapCnt"; // not currently supported
			public static final String MPL_MOTIONANDORIENT = "MotionAndOrient"; // not currently supported
			public static final String GYRO_MPU_MPL = "MPU Gyro";
			public static final String ACCEL_MPU_MPL = "MPU Accel";
			public static final String MAG_MPU_MPL = "MPU Mag";
			public static final String QUAT_DMP_6DOF = "MPU Quat 6DOF (from DMP)";
			public static final String ECG_TO_HR = "ECG To HR";
			public static final String PPG_TO_HR = "PPG To HR";
			public static final String ORIENTATION_3D_6DOF = "3D Orientation (6DOF)";
			public static final String ORIENTATION_3D_9DOF = "3D Orientation (9DOF)";
			public static final String EULER_ANGLES_6DOF = "Euler Angles (6DOF)";
			public static final String EULER_ANGLES_9DOF = "Euler Angles (9DOF)";

			public static final String HIGH_G_ACCEL = "200g Accel";

			public static final String PPG_DUMMY = "PPG";
			public static final String PPG_A12 = "PPG A12";
			public static final String PPG_A13 = "PPG A13";
			public static final String PPG1_DUMMY = "PPG1";
			public static final String PPG1_A12 = "PPG1 A12";
			public static final String PPG1_A13 = "PPG1 A13";
			public static final String PPG2_DUMMY = "PPG2";
			public static final String PPG2_A1 = "PPG2 A1";
			public static final String PPG2_A14 = "PPG2 A14";
			public static final String EXG_RESPIRATION = "Respiration";
			public static final String SKIN_TEMP_PROBE = "Temperature Probe";
			public static final String BRAMP_HIGHGAIN = "High Gain";
			public static final String BRAMP_LOWGAIN = "Low Gain";
	
			public static final String EXG1_24BIT = "EXG1 24BIT";
			public static final String EXG2_24BIT = "EXG2 24BIT";
			public static final String EXG1_16BIT = "EXG1 16BIT";
			public static final String EXG2_16BIT = "EXG2 16BIT";
		}

		//DATABASE NAMES
		//GUI AND EXPORT CHANNELS
		public static class ObjectClusterSensorName{
			public static  String TIMESTAMP = "Timestamp";
			public static  String REAL_TIME_CLOCK = "RealTime";
			public static  String ACCEL_LN_X = "Accel_LN_X";
			public static  String ACCEL_LN_Y = "Accel_LN_Y";
			public static  String ACCEL_LN_Z = "Accel_LN_Z";
			public static  String BATTERY = "Battery";
			public static  String EXT_EXP_A7 = "Ext_Exp_A7";
			public static  String EXT_EXP_A6 = "Ext_Exp_A6";
			public static  String EXT_EXP_A15 = "Ext_Exp_A15";
			public static  String INT_EXP_A12 = "Int_Exp_A12";
			public static  String INT_EXP_A13 = "Int_Exp_A13";
			public static  String INT_EXP_A14 = "Int_Exp_A14";
			public static  String BRIDGE_AMP_HIGH = "Bridge_Amp_High";
			public static  String BRIDGE_AMP_LOW = "Bridge_Amp_Low";
			public static  String GSR = "GSR";
			public static  String GSR_CONDUCTANCE = "GSR_Conductance";
			public static  String INT_EXP_A1 = "Int_Exp_A1";
			public static  String RESISTANCE_AMP = "Resistance_Amp";
			public static  String GYRO_X = "Gyro_X";
			public static  String GYRO_Y = "Gyro_Y";
			public static  String GYRO_Z = "Gyro_Z";
			public static  String ACCEL_WR_X = "Accel_WR_X";
			public static  String ACCEL_WR_Y = "Accel_WR_Y";
			public static  String ACCEL_WR_Z= "Accel_WR_Z";
			public static  String MAG_X = "Mag_X";
			public static  String MAG_Y = "Mag_Y";
			public static  String MAG_Z = "Mag_Z";
			public static  String ACCEL_MPU_X = "Accel_MPU_X";
			public static  String ACCEL_MPU_Y = "Accel_MPU_Y";
			public static  String ACCEL_MPU_Z = "Accel_MPU_Z";
			public static  String MAG_MPU_X = "Mag_MPU_X";
			public static  String MAG_MPU_Y = "Mag_MPU_Y";
			public static  String MAG_MPU_Z = "Mag_MPU_Z";
			public static  String TEMPERATURE_BMP180 = "Temperature_BMP180";
			public static  String PRESSURE_BMP180 = "Pressure_BMP180";
			public static  String EMG_CH1_24BIT = "EMG_CH1_24BIT";
			public static  String EMG_CH2_24BIT = "EMG_CH2_24BIT";
			public static  String EMG_CH1_16BIT = "EMG_CH1_16BIT";
			public static  String EMG_CH2_16BIT = "EMG_CH2_16BIT";
			public static  String ECG_LL_RA_24BIT = "ECG_LL-RA_24BIT";
			public static  String ECG_LA_RA_24BIT = "ECG_LA-RA_24BIT";
			public static  String ECG_LL_RA_16BIT = "ECG_LL-RA_16BIT";
			public static  String ECG_LA_RA_16BIT = "ECG_LA-RA_16BIT";
			public static  String TEST_CHIP1_CH1_24BIT = "Test_CHIP1_CH1_24BIT";
			public static  String TEST_CHIP1_CH2_24BIT = "Test_CHIP1_CH2_24BIT";
			public static  String TEST_CHIP2_CH1_24BIT = "Test_CHIP2_CH1_24BIT";
			public static  String TEST_CHIP2_CH2_24BIT = "Test_CHIP2_CH2_24BIT";
			public static  String TEST_CHIP1_CH1_16BIT = "Test_CHIP1_CH1_16BIT";
			public static  String TEST_CHIP1_CH2_16BIT = "Test_CHIP1_CH2_16BIT";
			public static  String TEST_CHIP2_CH1_16BIT = "Test_CHIP2_CH1_16BIT";
			public static  String TEST_CHIP2_CH2_16BIT = "Test_CHIP2_CH2_16BIT";
			public static  String EXG1_STATUS = "ECG_EMG_Status1";
			public static  String ECG_RESP_24BIT = "ECG_RESP_24BIT";
			public static  String ECG_VX_RL_24BIT = "ECG_Vx-RL_24BIT";
			public static  String ECG_RESP_16BIT = "ECG_RESP_16BIT";
			public static  String ECG_VX_RL_16BIT = "ECG_Vx-RL_16BIT";
			public static  String EXG1_CH1_24BIT = "ExG1_CH1_24BIT";
			public static  String EXG1_CH2_24BIT = "ExG1_CH2_24BIT";
			public static  String EXG1_CH1_16BIT = "ExG1_CH1_16BIT";
			public static  String EXG1_CH2_16BIT = "ExG1_CH2_16BIT";
			public static  String EXG2_CH1_24BIT = "ExG2_CH1_24BIT";
			public static  String EXG2_CH2_24BIT = "ExG2_CH2_24BIT";
			public static  String EXG2_CH1_16BIT = "ExG2_CH1_16BIT";
			public static  String EXG2_CH2_16BIT = "ExG2_CH2_16BIT";
			public static  String EXG2_STATUS = "ECG_EMG_Status2";
			public static  String QUAT_MPL_6DOF_W = "Quat_MPL_6DOF_W";
			public static  String QUAT_MPL_6DOF_X = "Quat_MPL_6DOF_X";
			public static  String QUAT_MPL_6DOF_Y = "Quat_MPL_6DOF_Y";
			public static  String QUAT_MPL_6DOF_Z = "Quat_MPL_6DOF_Z";
			public static  String QUAT_MPL_9DOF_W = "Quat_MPL_9DOF_W";
			public static  String QUAT_MPL_9DOF_X = "Quat_MPL_9DOF_X";
			public static  String QUAT_MPL_9DOF_Y = "Quat_MPL_9DOF_Y";
			public static  String QUAT_MPL_9DOF_Z = "Quat_MPL_9DOF_Z";
			public static  String EULER_MPL_6DOF_X = "Euler_MPL_6DOF_X";
			public static  String EULER_MPL_6DOF_Y = "Euler_MPL_6DOF_Y";
			public static  String EULER_MPL_6DOF_Z = "Euler_MPL_6DOF_Z";
			public static  String EULER_MPL_9DOF_X = "Euler_MPL_9DOF_X";
			public static  String EULER_MPL_9DOF_Y = "Euler_MPL_9DOF_Y";
			public static  String EULER_MPL_9DOF_Z = "Euler_MPL_9DOF_Z";
			public static  String MPL_HEADING = "MPL_heading";
			public static  String MPL_TEMPERATURE = "MPL_Temperature";
			public static  String MPL_PEDOM_CNT = "MPL_Pedom_cnt";
			public static  String MPL_PEDOM_TIME = "MPL_Pedom_Time";
			public static  String TAPDIRANDTAPCNT = "TapDirAndTapCnt";
			public static  String MOTIONANDORIENT = "MotionAndOrient";
			public static  String GYRO_MPU_MPL_X = "Gyro_MPU_MPL_X";
			public static  String GYRO_MPU_MPL_Y = "Gyro_MPU_MPL_Y";
			public static  String GYRO_MPU_MPL_Z = "Gyro_MPU_MPL_Z";
			public static  String ACCEL_MPU_MPL_X = "Accel_MPU_MPL_X";
			public static  String ACCEL_MPU_MPL_Y = "Accel_MPU_MPL_Y";
			public static  String ACCEL_MPU_MPL_Z = "Accel_MPU_MPL_Z";
			public static  String MAG_MPU_MPL_X = "Mag_MPU_MPL_X";
			public static  String MAG_MPU_MPL_Y = "Mag_MPU_MPL_Y";
			public static  String MAG_MPU_MPL_Z = "Mag_MPU_MPL_Z";
			public static  String QUAT_DMP_6DOF_W = "Quat_DMP_6DOF_W";
			public static  String QUAT_DMP_6DOF_X = "Quat_DMP_6DOF_X";
			public static  String QUAT_DMP_6DOF_Y = "Quat_DMP_6DOF_Y";
			public static  String QUAT_DMP_6DOF_Z = "Quat_DMP_6DOF_Z";
			public static  String ECG_TO_HR = "ECGtoHR";
			public static  String PPG_TO_HR = "PPGtoHR";
			public static  String QUAT_MADGE_6DOF_W = "Quat_Madge_6DOF_W";
			public static  String QUAT_MADGE_6DOF_X = "Quat_Madge_6DOF_X";
			public static  String QUAT_MADGE_6DOF_Y = "Quat_Madge_6DOF_Y";
			public static  String QUAT_MADGE_6DOF_Z = "Quat_Madge_6DOF_Z";
			public static  String QUAT_MADGE_9DOF_W = "Quat_Madge_9DOF_W";
			public static  String QUAT_MADGE_9DOF_X = "Quat_Madge_9DOF_X";
			public static  String QUAT_MADGE_9DOF_Y = "Quat_Madge_9DOF_Y";
			public static  String QUAT_MADGE_9DOF_Z = "Quat_Madge_9DOF_Z";
			public static  String EULER_6DOF_A = "Euler_6DOF_A";
			public static  String EULER_6DOF_X = "Euler_6DOF_X";
			public static  String EULER_6DOF_Y = "Euler_6DOF_Y";
			public static  String EULER_6DOF_Z = "Euler_6DOF_Z";
			public static  String EULER_9DOF_A = "Euler_9DOF_A";
			public static  String EULER_9DOF_X = "Euler_9DOF_X";
			public static  String EULER_9DOF_Y = "Euler_9DOF_Y";
			public static  String EULER_9DOF_Z = "Euler_9DOF_Z";
			public static  String AXIS_ANGLE_A = "Axis_Angle_A";
			public static  String AXIS_ANGLE_X = "Axis_Angle_X";
			public static  String AXIS_ANGLE_Y = "Axis_Angle_Y";
			public static  String AXIS_ANGLE_Z = "Axis_Angle_Z";
			public static  String PPG_A12 = "PPG_A12";
			public static  String PPG_A13 = "PPG_A13";
			public static  String PPG1_A12 = "PPG1_A12";
			public static  String PPG1_A13 = "PPG1_A13";
			public static  String PPG2_A1 = "PPG2_A1";
			public static  String PPG2_A14 = "PPG2_A14";
			public static  String REAL_TIME_CLOCK_SYNC = "RealTime_Sync";
			public static  String TIMESTAMP_SYNC = "Timestamp_Sync";
		}
		
		
		
		//Names used for parsing the GQ configuration header file 
		public class HeaderFileSensorName{
			public static final String SHIMMER3 = "shimmer3";
			public static final String VBATT = "VBATT";
			public static final String GSR = "GSR";
			public static final String LSM303DLHC_ACCEL = "LSM303DLHC_ACCEL";
		}
	}

	
	
	public static class Shimmer2{
		public class Channel{
			public final static int XAccel      = 0x00;
			public final static int YAccel      = 0x01;
			public final static int ZAccel      = 0x02;
			public final static int XGyro       = 0x03;
			public final static int YGyro       = 0x04;
			public final static int ZGyro       = 0x05;
			public final static int XMag        = 0x06;
			public final static int YMag        = 0x07;
			public final static int ZMag        = 0x08;
			public final static int EcgRaLl     = 0x09;
			public final static int EcgLaLl     = 0x0A;
			public final static int GsrRaw      = 0x0B;
			public final static int GsrRes      = 0x0C;
			public final static int Emg         = 0x0D;
			public final static int AnExA0      = 0x0E;
			public final static int AnExA7      = 0x0F;
			public final static int BridgeAmpHigh  = 0x10;
			public final static int BridgeAmpLow   = 0x11;
			public final static int HeartRate   = 0x12;
		}
		public class SensorBitmap{
			public static final int SENSOR_ACCEL				   = 0x80;
			public static final int SENSOR_GYRO				   	   = 0x40;
			public static final int SENSOR_MAG					   = 0x20;
			public static final int SENSOR_ECG					   = 0x10;
			public static final int SENSOR_EMG					   = 0x08;
			public static final int SENSOR_GSR					   = 0x04;
			public static final int SENSOR_EXP_BOARD_A7		       = 0x02;
			public static final int SENSOR_EXP_BOARD_A0		       = 0x01;
			public static final int SENSOR_BRIDGE_AMP			   = 0x8000;
			public static final int SENSOR_HEART				   = 0x4000;
		}
		
		//DATABASE NAMES
				//GUI AND EXPORT CHANNELS
				public static class ObjectClusterSensorName{
					public static String TIMESTAMP = "Timestamp";
					public static String REAL_TIME_CLOCK = "RealTime";
					public static String ACCEL_X = "Accel_X";
					public static String ACCEL_Y = "Accel_Y";
					public static String ACCEL_Z = "Accel_Z";
					public static String BATTERY = "Battery";
					public static String REG = "Reg";
					public static String EXT_EXP_A7 = "Ext_Exp_A7";
					public static String EXT_EXP_A6 = "Ext_Exp_A6";
					public static String EXT_EXP_A15 = "Ext_Exp_A15";
					public static String INT_EXP_A12 = "Int_Exp_A12";
					public static String INT_EXP_A13 = "Int_Exp_A13";
					public static String INT_EXP_A14 = "Int_Exp_A14";
					public static String BRIDGE_AMP_HIGH = "Bridge_Amp_High";
					public static String BRIDGE_AMP_LOW = "Bridge_Amp_Low";
					public static String GSR = "GSR";
					//public static String GSR_RAW = "GSR Raw";
					public static String GSR_RES = "GSR Res";
					public static String INT_EXP_A1 = "Int_Exp_A1";
					public static String EXP_BOARD_A0 = "Exp_Board_A0";
					public static String EXP_BOARD_A7 = "Exp_Board_A7";
					public static String GYRO_X = "Gyro_X";
					public static String GYRO_Y = "Gyro_Y";
					public static String GYRO_Z = "Gyro_Z";
					public static String MAG_X = "Mag_X";
					public static String MAG_Y = "Mag_Y";
					public static String MAG_Z = "Mag_Z";
					public static String EMG = "EMG";
					public static String ECG_RA_LL = "ECG_RA-LL";
					public static String ECG_LA_LL = "ECG_LA-LL";
					public static String ECG_TO_HR = "ECGtoHR";
					public static String QUAT_MADGE_6DOF_W = "Quat_Madge_6DOF_W";
					public static String QUAT_MADGE_6DOF_X = "Quat_Madge_6DOF_X";
					public static String QUAT_MADGE_6DOF_Y = "Quat_Madge_6DOF_Y";
					public static String QUAT_MADGE_6DOF_Z = "Quat_Madge_6DOF_Z";
					public static String QUAT_MADGE_9DOF_W = "Quat_Madge_9DOF_W";
					public static String QUAT_MADGE_9DOF_X = "Quat_Madge_9DOF_X";
					public static String QUAT_MADGE_9DOF_Y = "Quat_Madge_9DOF_Y";
					public static String QUAT_MADGE_9DOF_Z = "Quat_Madge_9DOF_Z";
					public static String EULER_6DOF_A = "Euler_6DOF_A";
					public static String EULER_6DOF_X = "Euler_6DOF_X";
					public static String EULER_6DOF_Y = "Euler_6DOF_Y";
					public static String EULER_6DOF_Z = "Euler_6DOF_Z";
					public static String EULER_9DOF_A = "Euler_9DOF_A";
					public static String EULER_9DOF_X = "Euler_9DOF_X";
					public static String EULER_9DOF_Y = "Euler_9DOF_Y";
					public static String EULER_9DOF_Z = "Euler_9DOF_Z";
					public static String HEART_RATE = "Heart_Rate"; //for the heart rate strap now no longer sold
					public static String AXIS_ANGLE_A = "Axis_Angle_A";
					public static String AXIS_ANGLE_X = "Axis_Angle_X";
					public static String AXIS_ANGLE_Y = "Axis_Angle_Y";
					public static String AXIS_ANGLE_Z = "Axis_Angle_Z";
					public static String VOLT_REG = "VSenseReg";
				}
		
		public final static String[] ListofCompatibleSensors={"Accelerometer","Gyroscope","Magnetometer","Battery Voltage","ECG","EMG","GSR","Exp Board","Bridge Amplifier","Heart Rate"};
		public final static String[] ListofAccelRange={"+/- 1.5g","+/- 6g"};
		public final static String[] ListofMagRange={"+/- 0.8Ga","+/- 1.3Ga","+/- 1.9Ga","+/- 2.5Ga","+/- 4.0Ga","+/- 4.7Ga","+/- 5.6Ga","+/- 8.1Ga"};
		public final static String[] ListofGSRRange={"10kOhm to 56kOhm","56kOhm to 220kOhm","220kOhm to 680kOhm","680kOhm to 4.7MOhm","Auto Range"};

		public class SensorMapKey{
			public final static int ACCEL = 0;
			public final static int GYRO = 1;
			public final static int MAG = 2;
			public final static int EMG = 3;
			public final static int ECG = 4;
			public final static int GSR = 5;
			public final static int EXP_BOARD_A7 = 6;
			public final static int EXP_BOARD_A0 = 7;
			public final static int EXP_BOARD = 8;
			public final static int BRIDGE_AMP = 9;
			public final static int HEART = 10;
			public final static int BATT = 11;
			public final static int EXT_ADC_A15 = 12;
			public final static int INT_ADC_A1 = 13;
			public final static int INT_ADC_A12 = 14;
			public final static int INT_ADC_A13 = 15;
			public final static int INT_ADC_A14 = 16;
		}
	}

	
	public static void setTooLegacyObjectClusterSensorNames(){

		Shimmer3.ObjectClusterSensorName.TIMESTAMP = "Timestamp";
		Shimmer3.ObjectClusterSensorName.REAL_TIME_CLOCK = "RealTime";
		Shimmer3.ObjectClusterSensorName.ACCEL_LN_X = "Low Noise Accelerometer X";
		Shimmer3.ObjectClusterSensorName.ACCEL_LN_Y = "Low Noise Accelerometer Y";
		Shimmer3.ObjectClusterSensorName.ACCEL_LN_Z = "Low Noise Accelerometer Z";
		Shimmer3.ObjectClusterSensorName.BATTERY = "VSenseBatt";
		Shimmer3.ObjectClusterSensorName.EXT_EXP_A7 = "ExpBoard A7";
		Shimmer3.ObjectClusterSensorName.EXT_EXP_A6 = "ExpBoard A6";
		Shimmer3.ObjectClusterSensorName.EXT_EXP_A15 = "External ADC A15";
		Shimmer3.ObjectClusterSensorName.INT_EXP_A12 = "Internal ADC A12";
		Shimmer3.ObjectClusterSensorName.INT_EXP_A13 = "Internal ADC A13";
		Shimmer3.ObjectClusterSensorName.INT_EXP_A14 = "Internal ADC A14";
		Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_HIGH = "Bridge Amplifier High";
		Shimmer3.ObjectClusterSensorName.BRIDGE_AMP_LOW = "Bridge Amplifier Low";
		Shimmer3.ObjectClusterSensorName.GSR = "GSR";
		Shimmer3.ObjectClusterSensorName.INT_EXP_A1 = "Internal ADC A1";
		Shimmer3.ObjectClusterSensorName.RESISTANCE_AMP = "Resistance Amp";
		Shimmer3.ObjectClusterSensorName.GYRO_X = "Gyroscope X";
		Shimmer3.ObjectClusterSensorName.GYRO_Y = "Gyroscope Y";
		Shimmer3.ObjectClusterSensorName.GYRO_Z = "Gyroscope Z";
		Shimmer3.ObjectClusterSensorName.ACCEL_WR_X = "Wide Range Accelerometer X";
		Shimmer3.ObjectClusterSensorName.ACCEL_WR_Y = "Wide Range Accelerometer Y";
		Shimmer3.ObjectClusterSensorName.ACCEL_WR_Z= "Wide Range Accelerometer Z";
		Shimmer3.ObjectClusterSensorName.MAG_X = "Magnetometer X";
		Shimmer3.ObjectClusterSensorName.MAG_Y = "Magnetometer Y";
		Shimmer3.ObjectClusterSensorName.MAG_Z = "Magnetometer Z";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_X = "Accel_MPU_X";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_Y = "Accel_MPU_Y";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_Z = "Accel_MPU_Z";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_X = "Mag_MPU_X";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_Y = "Mag_MPU_Y";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_Z = "Mag_MPU_Z";
		Shimmer3.ObjectClusterSensorName.TEMPERATURE_BMP180 = "Temperature";
		Shimmer3.ObjectClusterSensorName.PRESSURE_BMP180 = "Pressure";
		Shimmer3.ObjectClusterSensorName.EMG_CH1_24BIT = "EMG CH1";
		Shimmer3.ObjectClusterSensorName.EMG_CH2_24BIT = "EMG CH2";
		Shimmer3.ObjectClusterSensorName.EMG_CH1_16BIT = "EMG CH1";
		Shimmer3.ObjectClusterSensorName.EMG_CH2_16BIT = "EMG CH2";
		Shimmer3.ObjectClusterSensorName.ECG_LL_RA_24BIT = "ECG LL-RA";
		Shimmer3.ObjectClusterSensorName.ECG_LA_RA_24BIT = "ECG LA-RA";
		Shimmer3.ObjectClusterSensorName.ECG_LL_RA_16BIT = "ECG LL-RA";
		Shimmer3.ObjectClusterSensorName.ECG_LA_RA_16BIT = "ECG LA-RA";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP1_CH1_24BIT = "EXG1 CH1";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP2_CH1_24BIT = "EXG1 CH1";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP1_CH2_24BIT = "EXG1 CH2";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP2_CH2_24BIT = "EXG1 CH2";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP1_CH1_16BIT = "EXG1 CH1 16BIT";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP2_CH1_16BIT = "EXG1 CH1 16BIT";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP1_CH2_16BIT = "EXG1 CH2 16BIT";
		Shimmer3.ObjectClusterSensorName.TEST_CHIP2_CH2_16BIT = "EXG1 CH2 16BIT";
		Shimmer3.ObjectClusterSensorName.EXG1_STATUS = "EXG1 Status";
		Shimmer3.ObjectClusterSensorName.ECG_RESP_24BIT = "ECG RESP";
		Shimmer3.ObjectClusterSensorName.ECG_VX_RL_24BIT = "ECG Vx-RL";
		Shimmer3.ObjectClusterSensorName.ECG_RESP_16BIT = "ECG RESP";
		Shimmer3.ObjectClusterSensorName.ECG_VX_RL_16BIT = "ECG Vx-RL";
		Shimmer3.ObjectClusterSensorName.EXG1_CH1_24BIT = "ExG1 CH1";
		Shimmer3.ObjectClusterSensorName.EXG1_CH2_24BIT = "ExG1 CH2";
		Shimmer3.ObjectClusterSensorName.EXG1_CH1_16BIT = "ExG1 CH1 16Bit";
		Shimmer3.ObjectClusterSensorName.EXG1_CH2_16BIT = "ExG1 CH2 16Bit";
		Shimmer3.ObjectClusterSensorName.EXG2_CH1_24BIT = "ExG2 CH1";
		Shimmer3.ObjectClusterSensorName.EXG2_CH2_24BIT = "ExG2 CH2";
		Shimmer3.ObjectClusterSensorName.EXG2_CH1_16BIT = "ExG2 CH1 16Bit";
		Shimmer3.ObjectClusterSensorName.EXG2_CH2_16BIT = "ExG2 CH2 16Bit";
		Shimmer3.ObjectClusterSensorName.EXG2_STATUS = "EXG2 Status";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_W = "Quat_MPL_6DOF_W";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_X = "Quat_MPL_6DOF_X";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Y = "Quat_MPL_6DOF_Y";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_6DOF_Z = "Quat_MPL_6DOF_Z";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_W = "Quat_MPL_9DOF_W";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_X = "Quat_MPL_9DOF_X";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_Y = "Quat_MPL_9DOF_Y";
		Shimmer3.ObjectClusterSensorName.QUAT_MPL_9DOF_Z = "Quat_MPL_9DOF_Z";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_X = "Euler_MPL_6DOF_X";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_Y = "Euler_MPL_6DOF_Y";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_6DOF_Z = "Euler_MPL_6DOF_Z";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_X = "Euler_MPL_9DOF_X";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_Y = "Euler_MPL_9DOF_Y";
		Shimmer3.ObjectClusterSensorName.EULER_MPL_9DOF_Z = "Euler_MPL_9DOF_Z";
		Shimmer3.ObjectClusterSensorName.MPL_HEADING = "MPL_heading";
		Shimmer3.ObjectClusterSensorName.MPL_TEMPERATURE = "MPL_Temperature";
		Shimmer3.ObjectClusterSensorName.MPL_PEDOM_CNT = "MPL_Pedom_cnt";
		Shimmer3.ObjectClusterSensorName.MPL_PEDOM_TIME = "MPL_Pedom_Time";
		Shimmer3.ObjectClusterSensorName.TAPDIRANDTAPCNT = "TapDirAndTapCnt";
		Shimmer3.ObjectClusterSensorName.MOTIONANDORIENT = "MotionAndOrient";
		Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_X = "Gyro_MPU_MPL_X";
		Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Y = "Gyro_MPU_MPL_Y";
		Shimmer3.ObjectClusterSensorName.GYRO_MPU_MPL_Z = "Gyro_MPU_MPL_Z";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_X = "Accel_MPU_MPL_X";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Y = "Accel_MPU_MPL_Y";
		Shimmer3.ObjectClusterSensorName.ACCEL_MPU_MPL_Z = "Accel_MPU_MPL_Z";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_X = "Mag_MPU_MPL_X";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Y = "Mag_MPU_MPL_Y";
		Shimmer3.ObjectClusterSensorName.MAG_MPU_MPL_Z = "Mag_MPU_MPL_Z";
		Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_W = "Quat_DMP_6DOF_W";
		Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_X = "Quat_DMP_6DOF_X";
		Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_Y = "Quat_DMP_6DOF_Y";
		Shimmer3.ObjectClusterSensorName.QUAT_DMP_6DOF_Z = "Quat_DMP_6DOF_Z";
		Shimmer3.ObjectClusterSensorName.ECG_TO_HR = "ECGtoHR";
		Shimmer3.ObjectClusterSensorName.PPG_TO_HR = "PPGtoHR";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_6DOF_W = "Quaternion 0";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_6DOF_X = "Quaternion 1";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_6DOF_Y = "Quaternion 2";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_6DOF_Z = "Quaternion 3";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_W = "Quaternion 0";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_X = "Quaternion 1";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y = "Quaternion 2";
		Shimmer3.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z = "Quaternion 3";
		Shimmer3.ObjectClusterSensorName.EULER_6DOF_A = "Euler_6DOF_A";
		Shimmer3.ObjectClusterSensorName.EULER_6DOF_X = "Euler_6DOF_X";
		Shimmer3.ObjectClusterSensorName.EULER_6DOF_Y = "Euler_6DOF_Y";
		Shimmer3.ObjectClusterSensorName.EULER_6DOF_Z = "Euler_6DOF_Z";
		Shimmer3.ObjectClusterSensorName.EULER_9DOF_A = "Euler_9DOF_A";
		Shimmer3.ObjectClusterSensorName.EULER_9DOF_X = "Euler_9DOF_X";
		Shimmer3.ObjectClusterSensorName.EULER_9DOF_Y = "Euler_9DOF_Y";
		Shimmer3.ObjectClusterSensorName.EULER_9DOF_Z = "Euler_9DOF_Z";
		Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_A = "Axis Angle A";
		Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_X = "Axis Angle X";
		Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Y = "Axis Angle Y";
		Shimmer3.ObjectClusterSensorName.AXIS_ANGLE_Z = "Axis Angle Z";
		Shimmer3.ObjectClusterSensorName.PPG_A12 = "PPG_A12";
		Shimmer3.ObjectClusterSensorName.PPG_A13 = "PPG_A13";
		Shimmer3.ObjectClusterSensorName.PPG1_A12 = "PPG1_A12";
		Shimmer3.ObjectClusterSensorName.PPG1_A13 = "PPG1_A13";
		Shimmer3.ObjectClusterSensorName.PPG2_A1 = "PPG2_A1";
		Shimmer3.ObjectClusterSensorName.PPG2_A14 = "PPG2_A14";
		Shimmer3.ObjectClusterSensorName.REAL_TIME_CLOCK_SYNC = "RealTime_Sync";
		Shimmer3.ObjectClusterSensorName.TIMESTAMP_SYNC = "Timestamp_Sync";
	

		Shimmer2.ObjectClusterSensorName.TIMESTAMP = "Timestamp";
		Shimmer2.ObjectClusterSensorName.REAL_TIME_CLOCK = "RealTime";
		Shimmer2.ObjectClusterSensorName.ACCEL_X = "Accelerometer X";
		Shimmer2.ObjectClusterSensorName.ACCEL_Y = "Accelerometer Y";
		Shimmer2.ObjectClusterSensorName.ACCEL_Z = "Accelerometer Z";
		Shimmer2.ObjectClusterSensorName.BATTERY = "VSenseBatt";
		Shimmer2.ObjectClusterSensorName.VOLT_REG = "VSenseReg";
		Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_HIGH = "Bridge Amplifier High";
		Shimmer2.ObjectClusterSensorName.BRIDGE_AMP_LOW = "Bridge Amplifier Low";
		Shimmer2.ObjectClusterSensorName.GSR = "GSR";
		//Shimmer2.ObjectClusterSensorName.GSR_RAW = "GSR Raw";
		Shimmer2.ObjectClusterSensorName.GSR_RES = "GSR Res";
		Shimmer2.ObjectClusterSensorName.EXP_BOARD_A0 = "ExpBoard A0";
		Shimmer2.ObjectClusterSensorName.EXP_BOARD_A7 = "ExpBoard A7";
		Shimmer2.ObjectClusterSensorName.GYRO_X = "Gyroscope X";
		Shimmer2.ObjectClusterSensorName.GYRO_Y = "Gyroscope Y";
		Shimmer2.ObjectClusterSensorName.GYRO_Z = "Gyroscope Z";
		Shimmer2.ObjectClusterSensorName.MAG_X = "Magnetometer X";
		Shimmer2.ObjectClusterSensorName.MAG_Y = "Magnetometer Y";
		Shimmer2.ObjectClusterSensorName.MAG_Z = "Magnetometer Z";
		Shimmer2.ObjectClusterSensorName.EMG = "EMG";
		Shimmer2.ObjectClusterSensorName.ECG_RA_LL = "ECG RA-LL";
		Shimmer2.ObjectClusterSensorName.ECG_LA_LL = "ECG LA-LL";
		Shimmer2.ObjectClusterSensorName.ECG_TO_HR = "ECGtoHR";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_6DOF_W = "Quaternion 0";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_6DOF_X = "Quaternion 1";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_6DOF_Y = "Quaternion 2";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_6DOF_Z = "Quaternion 3";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_W = "Quaternion 0";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_X = "Quaternion 1";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Y = "Quaternion 2";
		Shimmer2.ObjectClusterSensorName.QUAT_MADGE_9DOF_Z = "Quaternion 3";
		Shimmer2.ObjectClusterSensorName.EULER_6DOF_A = "Euler_6DOF_A";
		Shimmer2.ObjectClusterSensorName.EULER_6DOF_X = "Euler_6DOF_X";
		Shimmer2.ObjectClusterSensorName.EULER_6DOF_Y = "Euler_6DOF_Y";
		Shimmer2.ObjectClusterSensorName.EULER_6DOF_Z = "Euler_6DOF_Z";
		Shimmer2.ObjectClusterSensorName.EULER_9DOF_A = "Euler_9DOF_A";
		Shimmer2.ObjectClusterSensorName.EULER_9DOF_X = "Euler_9DOF_X";
		Shimmer2.ObjectClusterSensorName.EULER_9DOF_Y = "Euler_9DOF_Y";
		Shimmer2.ObjectClusterSensorName.EULER_9DOF_Z = "Euler_9DOF_Z";
		Shimmer2.ObjectClusterSensorName.HEART_RATE = "Heart_Rate"; //for the heart rate strap now no longer sold
		Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_A = "Axis Angle A";
		Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_X = "Axis Angle X";
		Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Y = "Axis Angle Y";
		Shimmer2.ObjectClusterSensorName.AXIS_ANGLE_Z = "Axis Angle Z";
		
	
		
		
	}
	
}


