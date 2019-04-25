package com.shimmerresearch.driver;

import java.io.Serializable;
import java.util.List;

/**
 * Used in Consensys to hold Shimmer configuration GUI information for each
 * configuration option to allow for dynamic GUI creation based on compatible
 * HW&FW version checking.
 * 
 * @author Mark Nolan
 */
public class SensorConfigOptionDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8894717489924237791L;

	public static enum GUI_COMPONENT_TYPE {
		COMBOBOX,
		CHECKBOX,
		TEXTFIELD
	};
	
	public String[] mGuiValues;
	public Integer[] mConfigValues;
	public GUI_COMPONENT_TYPE mGuiComponentType;
	
	public List<ShimmerVerObject> mCompatibleVersionInfo = null;  
	
	/**
	 * Used in Consensys to hold Shimmer configuration GUI information for
	 * each configuration option to allow for dynamic GUI creation based on
	 * compatible HW&FW version checking.
	 * 
	 * This constructor = ComboBox (compatible with all HW, FW and Expansion Boards)
	 * 
	 * @param guiValues array of configuration values to show in the GUI
	 * @param configValues bit/bytes values written to the Shimmer corresponding to the shown GUI options.
	 * @param guiComponentType
	 */
	public SensorConfigOptionDetails(String[] guiValues, Integer[] configValues, GUI_COMPONENT_TYPE guiComponentType) {
		mGuiValues = guiValues;
		mConfigValues = configValues;
		mGuiComponentType = guiComponentType;
		
		mCompatibleVersionInfo = null;
	}
	
	/**
	 * Used in Consensys to hold Shimmer configuration GUI information for
	 * each configuration option to allow for dynamic GUI creation based on
	 * compatible HW&FW version checking.
	 * 
	 * This constructor = ComboBox (with compatible HW, FW, and Expansion Board information)
	 * 
	 * @param guiValues
	 * @param configValues
	 * @param guiComponentType
	 */
	public SensorConfigOptionDetails(String[] guiValues, Integer[] configValues, GUI_COMPONENT_TYPE guiComponentType, List<ShimmerVerObject> compatibleVersionInfo) {
		mGuiValues = guiValues;
		mConfigValues = configValues;
		mGuiComponentType = guiComponentType;
		
		mCompatibleVersionInfo = compatibleVersionInfo;
	}

	
	/**
	 * Used in Consensys to hold Shimmer configuration GUI information for
	 * each configuration option to allow for dynamic GUI creation based on
	 * compatible HW&FW version checking.
	 * 
	 * This constructor = CheckBox (compatible with all HW, FW and Expansion Boards)
	 * 
	 * @param guiValues
	 * @param configValues
	 * @param guiComponentType
	 */
	public SensorConfigOptionDetails(GUI_COMPONENT_TYPE guiComponentType) {
		mGuiComponentType = guiComponentType;
		
		mCompatibleVersionInfo = null;
	}
	
	/**
	 * Used in Consensys to hold Shimmer configuration GUI information for
	 * each configuration option to allow for dynamic GUI creation based on
	 * compatible HW&FW version checking.
	 * 
	 * This constructor = CheckBox (with compatible HW, FW, and Expansion Board information)
	 * 
	 * @param guiValues
	 * @param configValues
	 * @param guiComponentType
	 */
	public SensorConfigOptionDetails(GUI_COMPONENT_TYPE guiComponentType, List<ShimmerVerObject> compatibleVersionInfo) {
		mGuiComponentType = guiComponentType;
		
		mCompatibleVersionInfo = compatibleVersionInfo;
	}
	
	
}
