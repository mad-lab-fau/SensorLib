package com.shimmerresearch.driver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/** Utility class with commonly useful methods
 * 
 * @author Mark Nolan
 *
 */
public class Util {
	
	public String mParentClassName = "UpdateCheck";
	
	public Boolean mVerboseMode = true;
	
	public Util(String parentClassName, Boolean verboseMode){
		this.mParentClassName = parentClassName;
		this.mVerboseMode = verboseMode;
	}
	
	public void consolePrintLn(String message) {
		if(mVerboseMode) {
			Calendar rightNow = Calendar.getInstance();
			String rightNowString = "[" + String.format("%02d",rightNow.get(Calendar.HOUR_OF_DAY)) 
					+ ":" + String.format("%02d",rightNow.get(Calendar.MINUTE)) 
					+ ":" + String.format("%02d",rightNow.get(Calendar.SECOND)) 
					+ ":" + String.format("%03d",rightNow.get(Calendar.MILLISECOND)) + "]";
			System.out.println(rightNowString + " " + mParentClassName + ": " + message);
		}		
	}
	public void consolePrint(String message) {
		if(mVerboseMode) {
			System.out.print(message);
		}		
	}
	
	public void setVerboseMode(boolean verboseMode) {
		mVerboseMode = verboseMode;
	}

	public static String convertSecondsToDateString(long seconds) {
		return convertMilliSecondsToDateString(seconds * 1000);
	}
	
	public static String convertMilliSecondsToDateString(long milliSeconds) {
		Calendar cal = Calendar.getInstance();
		
		cal.setTimeInMillis(milliSeconds);
		int dayIndex = cal.get(Calendar.DAY_OF_MONTH);
		String dayString = getDayOfMonthSuffix(dayIndex);

		int monthIndex = cal.get(Calendar.MONTH);
		String monthString = "";

    	switch(monthIndex){
		
			case(0):
				monthString = "Jan";
            	break;
			case(1):
				monthString = "Feb";
            	break;
			case(2):
				monthString = "Mar";
            	break;
			case(3):
				monthString = "Apr";
            	break;
			case(4):
				monthString = "May";
            	break;
			case(5):
				monthString = "June";
            	break;
			case(6):
				monthString = "July";
            	break;
			case(7):
				monthString = "Aug";
            	break;
			case(8):
				monthString = "Sept";
            	break;
			case(9):
				monthString = "Oct";
            	break;
			case(10):
				monthString = "Nov";
            	break;
			case(11):
				monthString = "Dec";
            	break;
            default:
            	break;
    	}
    	DateFormat dfLocal = new SimpleDateFormat("//yyyy HH:mm:ss");
    	String timeString = dfLocal.format(new Date(milliSeconds));
    	timeString = timeString.replaceFirst("//", dayIndex + dayString + " " + monthString + " ");
		return timeString;
	}
	
	private static String getDayOfMonthSuffix(final int n) {
	    if (n >= 11 && n <= 13) {
	        return "th";
	    }
	    switch (n % 10) {
	        case 1:  return "st";
	        case 2:  return "nd";
	        case 3:  return "rd";
	        default: return "th";
	    }
	}
	
	
	public static String convertBytes(double bytes){
		
		bytes = (double)((bytes) / 1024 / 1024 / 1024);
	    
	    String mFormattedBytesTxt = " GB";
	    
    	if(bytes < 0.001){
    		bytes = bytes * 1024 * 1024;
    		mFormattedBytesTxt = " KB";
    	}	
	    
    	else if (bytes < 1.0) {
	    	bytes = bytes * 1024;
	    	mFormattedBytesTxt = " MB";
	    }
	    mFormattedBytesTxt = String.format("%.2f", bytes) + mFormattedBytesTxt;
	    return mFormattedBytesTxt;
	    
	}
	
	public static boolean isNumeric(String str){
		if(str==null) {
			return false;
		}
		if(str.isEmpty()) {
			return false;
		}
		
	    for (char c : str.toCharArray()){
	        if (!Character.isDigit(c)) return false;
	    }
	    return true;
	}
	
	
	public static boolean isAsciiPrintable(char ch) {
	      return ch >= 32 && ch < 127;
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	
	/**Returns true if FW ID is the same and "this" version is greater or equal then comparison version
	 * @param thisFwIdent
	 * @param thisMajor
	 * @param thisMinor
	 * @param thisInternal
	 * @param compFwIdent
	 * @param compMajor
	 * @param compMinor
	 * @param compInternal
	 * @return
	 */
	public static boolean compareVersions(int thisFwIdent, int thisMajor, int thisMinor, int thisInternal,
			int compFwIdent, int compMajor, int compMinor, int compInternal) {

		if (thisFwIdent==compFwIdent){
			if ((thisMajor>compMajor)
					||(thisMajor==compMajor && thisMinor>compMinor)
					||(thisMajor==compMajor && thisMinor==compMinor && thisInternal>=compInternal)){
				return true; // if FW ID is the same and version is greater or equal 
			}
		}
		return false; // if less or not the same FW ID
	}
	
	public static String convertDuration(int duration){
		
		double totalSecs = duration/1000; //convert from miliseconds to seconds
		int hours = (int) (totalSecs / 3600);
		int minutes = (int) ((totalSecs % 3600) / 60);
		int seconds = (int) (totalSecs % 60);
		String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
		
		return timeString;
	}
	
	
	public String fromMilToDate(double miliseconds){
		
		long mili = (long) miliseconds;
		Date date = new Date(mili);		
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		return formatter.format(date);
	}
	
	
	public String fromSecondsToDate(String seconds){
		
		double miliseconds = 1000*Double.valueOf(seconds);
		long mili = (long) miliseconds;
		Date date = new Date(mili);		
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		return formatter.format(date);
	}
	
}
