package com.shimmerresearch.driver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Holds all information related individual sensor channels for dynamic GUI and
 * configuration purposes. Currently used in Consensys only.
 * 
 * @author Mark Nolan
 *
 */
public class SensorDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4567211941610864326L;
	
	/**
	 * Indicates if sensors channel is enabled.
	 */
	public boolean mIsEnabled = false;
	/**
	 * Used for the BtStream and LogAndStream firmware to indicate enabled sensors when connected over Bluetooth. 
	 */
	public long mSensorBitmapIDStreaming = 0;
	/**
	 * Used in the configuration header in RAW data logged to the Shimmer's on-board SD-card. 
	 */
	public long mSensorBitmapIDSDLogHeader = 0;
	
	public long mDerivedSensorBitmapID = 0;
	
	public String mLabel = "";
	public List<Integer> mListOfSensorMapKeysRequired = null;
	public List<Integer> mListOfSensorMapKeysConflicting = null;
	public boolean mIntExpBoardPowerRequired = false;
	public List<String> mListOfConfigOptionKeysAssociated = null;
	public List<ShimmerVerObject> mListOfCompatibleVersionInfo = null;  
	
	//Testing for GQ
	public String mHeaderFileLabel = "";
	public int mHeaderByteMask = 0;
	public int mNumChannels = 0;
	//public LinkedHashMap<String,ChannelDetails> mMapOfChannels = new LinkedHashMap<String,ChannelDetails>();
	public List<ChannelDetails> mListOfChannels = new ArrayList<ChannelDetails>();

	/**
	 * Holds all information related individual sensor channels for dynamic GUI
	 * and configuration purposes. Currently used in Consensys only.
	 * 
	 * This constructor is used for standard Shimmer3 firmware (SDLog,
	 * LogAndStream and BtStream)
	 * 
	 * @param isChannelEnabled
	 * @param sensorBitmapIDStreaming
	 * @param sensorBitmapIDSDLogHeader
	 * @param label
	 */
	public SensorDetails(boolean isChannelEnabled, long sensorBitmapIDStreaming, long sensorBitmapIDSDLogHeader, String label) {
		mIsEnabled = isChannelEnabled;
		mSensorBitmapIDStreaming = sensorBitmapIDStreaming;
		mSensorBitmapIDSDLogHeader = sensorBitmapIDSDLogHeader;
		mLabel = label;
		mIntExpBoardPowerRequired = false;
		mListOfCompatibleVersionInfo = null;
	}

	/**
	 * Holds all information related individual sensor channels for dynamic GUI
	 * and configuration purposes. Currently used in Consensys only.
	 * 
	 * This constructor is used for Shimmer3 GQ firmware
	 * 
	 * @param isChannelEnabled
	 * @param label
	 */
	public SensorDetails(boolean isChannelEnabled, String label) {
		mIsEnabled = isChannelEnabled;
		mLabel = label;
		mIntExpBoardPowerRequired = false;
		mListOfCompatibleVersionInfo = null;
	}

	public void setEnabledState(boolean state) {
		mIsEnabled = state;
	}
	
	public boolean isDerivedChannel() {
		if(mDerivedSensorBitmapID>0) {
			return true;
		}
		return false;
	}

	public void resetMapKeyLists() {
		mListOfSensorMapKeysRequired = null;
		mListOfSensorMapKeysConflicting = null;
		mListOfConfigOptionKeysAssociated = null;
	}

}
