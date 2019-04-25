package com.shimmerresearch.driver;

import java.io.Serializable;

import com.shimmerresearch.driver.ShimmerVerDetails.HW_ID;
import com.shimmerresearch.driver.ShimmerVerDetails.FW_ID;

/**
 * Holds HW, FW and expansion board infomation. Used for docked Shimmers current
 * info and also for the purposes of compatible version checking.
 * 
 * @author Mark Nolan
 *
 */
public class ShimmerVerObject implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1966526754185423783L;
	
	public int mHardwareVersion = 0;
	public String mHardwareVersionParsed = "";
	
	public int mFirmwareIdentifier = 0;
	public int mFirmwareVersionMajor = 0;
	public int mFirmwareVersionMinor = 0;
	public int mFirmwareVersionInternal = 0;
	public int mFirmwareVersionCode = 0;
	public String mFirmwareIdentifierParsed = "";
	public String mFirmwareVersionParsed = "";

	public int mShimmerExpansionBoardId = 0;
	
	/**
	 * Used specifically for compatible version checking
	 * 
	 * @param hardwareVersion
	 * @param firmwareIdentifier
	 * @param firmwareVersionMajor
	 * @param firmwareVersionMinor
	 * @param firmwareVersionInternal
	 * @param shimmerExpansionBoardId
	 */
	public ShimmerVerObject(int hardwareVersion, 
			int firmwareIdentifier,
			int firmwareVersionMajor, 
			int firmwareVersionMinor, 
			int firmwareVersionInternal,
			int shimmerExpansionBoardId) {
		
		mHardwareVersion = hardwareVersion;
		mFirmwareIdentifier = firmwareIdentifier;
		mFirmwareVersionMajor = firmwareVersionMajor;
		mFirmwareVersionMinor = firmwareVersionMinor;
		mFirmwareVersionInternal = firmwareVersionInternal;
		mShimmerExpansionBoardId = shimmerExpansionBoardId;
		
		parseVerDetails();
	}
	
	/**
	 * Used specifically when finding the current information from a docked
	 * Shimmer through the dock's UART communication channel.

	 * @param hardwareVersion
	 * @param firmwareIdentifier
	 * @param firmwareVersionMajor
	 * @param firmwareVersionMinor
	 * @param firmwareVersionInternal
	 */
	public ShimmerVerObject(
			int hardwareVersion, 
			int firmwareIdentifier,
			int firmwareVersionMajor, 
			int firmwareVersionMinor,
			int firmwareVersionInternal) {

		mHardwareVersion = hardwareVersion;
		mFirmwareIdentifier = firmwareIdentifier;
		mFirmwareVersionMajor = firmwareVersionMajor;
		mFirmwareVersionMinor = firmwareVersionMinor;
		mFirmwareVersionInternal = firmwareVersionInternal;

		parseVerDetails();
	}
	
	
	/**
	 * Empty constructor used when finding the current information from a docked
	 * Shimmer through the dock's UART communication channel.
	 * 
	 */
	public ShimmerVerObject() {
		// TODO Auto-generated constructor stub
	}
	

	private void parseVerDetails() {
		if (ShimmerVerDetails.mMapOfShimmerRevisions.containsKey(mHardwareVersion)) {
			mHardwareVersionParsed = ShimmerVerDetails.mMapOfShimmerRevisions.get(mHardwareVersion);
		} else {
			mHardwareVersionParsed = "Unknown";
		}

		if (mFirmwareIdentifier == FW_ID.SHIMMER3.BOILER_PLATE) {
			mFirmwareIdentifierParsed = "Boilerplate";
		} else if (mFirmwareIdentifier == FW_ID.SHIMMER3.BTSTREAM) {
			mFirmwareIdentifierParsed = "BtStream";

			if ((mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 1)
					|| (mFirmwareVersionMajor == 1 && mFirmwareVersionMinor == 2 && mHardwareVersion == HW_ID.SHIMMER_2R))
				mFirmwareVersionCode = 1;
			else if (mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 2)
				mFirmwareVersionCode = 2;
			else if (mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 3)
				mFirmwareVersionCode = 3;
			else if (mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 4)
				mFirmwareVersionCode = 4;
			else
				// if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==5)
				mFirmwareVersionCode = 5;

		} else if (mFirmwareIdentifier == FW_ID.SHIMMER3.SDLOG) {
			mFirmwareIdentifierParsed = "SDLog";

			// TODO
			mFirmwareVersionCode = 6;

			// if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==1)
			// mFirmwareVersionCode = 3;
			// else if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==2)
			// mFirmwareVersionCode = 4;
			// else if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==3)
			// mFirmwareVersionCode = 5;

		} else if (mFirmwareIdentifier == FW_ID.SHIMMER3.LOGANDSTREAM) {
			mFirmwareIdentifierParsed = "LogAndStream";

			if (mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 1)
				mFirmwareVersionCode = 3;
			else if (mFirmwareVersionMajor == 0 && mFirmwareVersionMinor == 2)
				mFirmwareVersionCode = 4;
			else
				// if(mFirmwareVersionMajor==0 && mFirmwareVersionMinor==3)
				mFirmwareVersionCode = 5;
		} else if (mFirmwareIdentifier == FW_ID.SHIMMER3.GQ_GSR) {
			mFirmwareIdentifierParsed = "GQ GSR";

			// TODO
			mFirmwareVersionCode = 7;
		} else {
			mFirmwareIdentifierParsed = "Unknown";
		}

		mFirmwareVersionParsed = mFirmwareIdentifierParsed + " v"
				+ mFirmwareVersionMajor + "." + mFirmwareVersionMinor + "."
				+ mFirmwareVersionInternal;		
	}

}
