package com.shimmerresearch.driver;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds Shimmer Hardware and Firmware version details.
 * 
 * @author Mark Nolan
 *
 */
public class ShimmerVerDetails implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7940733886215010795L;
	
	public final static int EXP_BRD_NONE_ID = 255;
	public static final String EXP_BRD_NONE = "None";
	
	public final static class HW_ID {
		public final static int SHIMMER_1 = 0;
		public final static int SHIMMER_2 = 1;
		public final static int SHIMMER_2R = 2;
		public final static int SHIMMER_3 = 3;
		public final static int SHIMMER_SR30 = 4;
//		public final static int DCU_SWEATSENSOR = 4;
		public final static int SHIMMER_GQ = 5;
	}
	
	public static final Map<Integer, String> mMapOfShimmerRevisions;
    static {
        Map<Integer, String> aMap = new TreeMap<Integer,String>();
        aMap.put(HW_ID.SHIMMER_1, "Shimmer1");
        aMap.put(HW_ID.SHIMMER_2, "Shimmer2");
        aMap.put(HW_ID.SHIMMER_2R, "Shimmer2r");
        aMap.put(HW_ID.SHIMMER_3, "Shimmer3");
        aMap.put(HW_ID.SHIMMER_SR30, "Shimmer SR30");
        aMap.put(HW_ID.SHIMMER_GQ, "ShimmerGQ");
//        aMap.put(HW_ID.DCU_SWEATSENSOR, "DCU_SWEATSENSOR");
        mMapOfShimmerRevisions = Collections.unmodifiableMap(aMap);
    }
    
	public static final class HW_ID_SR_CODES {
		public final static int EXP_BRD_BR_AMP = 8;
		public final static int EXP_BRD_BR_AMP_UNIFIED = 49;
		public final static int EXP_BRD_GSR = 14;
		public final static int EXP_BRD_GSR_UNIFIED = 48;
		public final static int EXP_BRD_PROTO3_MINI = 36;
		public final static int EXP_BRD_EXG = 37;
		public final static int EXP_BRD_EXG_UNIFIED = 47;
		public final static int EXP_BRD_PROTO3_DELUXE = 38;
		public final static int EXP_BRD_HIGH_G_ACCEL = 44;
		public final static int EXP_BRD_GPS = 46;
		public final static int BASE15U = 41;
		public final static int BASE6U = 42;
	}
	
	public static final Map<Integer, String> mMapOfShimmmerHardware;
    static {
        Map<Integer, String> aMap = new TreeMap<Integer,String>();
        aMap.put(HW_ID_SR_CODES.EXP_BRD_BR_AMP, "Bridge Amplifier+");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_BR_AMP_UNIFIED, "Bridge Amplifier+");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_GSR, "GSR+");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_GSR_UNIFIED, "GSR+");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_PROTO3_MINI, "PROTO3 Mini");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_EXG, "ECG/EMG");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_EXG_UNIFIED, "ECG/EMG/Resp");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_PROTO3_DELUXE, "PROTO3 Deluxe");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_HIGH_G_ACCEL, "High-g Accel");
        aMap.put(HW_ID_SR_CODES.EXP_BRD_GPS, "GPS");
        aMap.put(HW_ID_SR_CODES.BASE15U, "Base15U");
        aMap.put(HW_ID_SR_CODES.BASE6U, "Base6U");
        mMapOfShimmmerHardware = Collections.unmodifiableMap(aMap);
    }

	public final static class FW_ID {
		public final static class SHIMMER3 {
			public final static int BOILER_PLATE = 0;
			public final static int BTSTREAM = 1;
			public final static int SDLOG = 2;
			public final static int LOGANDSTREAM = 3;
			public final static int DCU_SWEATSENSOR = 4;
			public final static int GQ_GSR = 5;
			public final static int GPIO_TEST = 6;
		}
		
		public final static class BASES {
			public final static int BASE15U_REV2 = 0;
			public final static int BASE15U_REV4 = 1;
			public final static int BASE6U = 2;
		}
		
		public final static class SHIMMER_GQ {
			public final static int GQ_GSR = 0;
		}
	}

}
