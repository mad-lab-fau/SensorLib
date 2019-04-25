/*Rev 0.2
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
 * @author Jong Chern Lim
 * 
 * Changes since 0.1
 * - Added method to get outputFile
 * 
 */


package com.shimmerresearch.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import android.os.Environment;
import android.util.Log;

import com.google.common.collect.Multimap;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;


public class Logging {
	boolean mFirstWrite=true;
	String[] mSensorNames;
	String[] mSensorFormats;
	String[] mSensorUnits;
	String mFileName="";
	BufferedWriter writer=null;
	File outputFile;
	String mDelimiter=","; //default is comma
	
	/**
	 * @param myName is the file name which will be used
	 */
	public Logging(String myName){
		mFileName=myName;
		File root = Environment.getExternalStorageDirectory();
		Log.d("AbsolutePath", root.getAbsolutePath());
		outputFile = new File(root, mFileName+".dat");
	}
	
	public Logging(String myName,String delimiter){
		mFileName=myName;
		mDelimiter=delimiter;
		File root = Environment.getExternalStorageDirectory();
		Log.d("AbsolutePath", root.getAbsolutePath());
		outputFile = new File(root, mFileName+".dat");
	}
	
	/**
	 * @param myName
	 * @param delimiter
	 * @param folderName will create a new folder if it does not exist
	 */
	public Logging(String myName,String delimiter, String folderName){
		mFileName=myName;
		mDelimiter=delimiter;

		 File root = new File(Environment.getExternalStorageDirectory() + "/"+folderName);

		   if(!root.exists())
		    {
		        if(root.mkdir()); //directory is created;
		    }
		   outputFile = new File(root, mFileName+".dat");
	}
	
	
	/**
	 * This function takes an object cluster and logs all the data within it. User should note that the function will write over prior files with the same name.
	 * @param objectClusterLog data which will be written into the file
	 */
	public void logData(ObjectCluster objectCluster){
		ObjectCluster objectClusterLog = objectCluster;
		try {
			
    		
				

			if (mFirstWrite==true) {
				writer = new BufferedWriter(new FileWriter(outputFile,true));
				
	    		//First retrieve all the unique keys from the objectClusterLog
				Multimap<String, FormatCluster> m = objectClusterLog.mPropertyCluster;

				int size = m.size();
				System.out.print(size);
				mSensorNames=new String[size];
				mSensorFormats=new String[size];
				mSensorUnits=new String[size];
				int i=0;
				int p=0;
				 for(String key : m.keys()) {
					 //first check that there are no repeat entries
					 
					 if(compareStringArray(mSensorNames, key) == true) {
						 for(FormatCluster formatCluster : m.get(key)) {
							 mSensorFormats[p]=formatCluster.mFormat;
							 mSensorUnits[p]=formatCluster.mUnits;
							 //Log.d("Shimmer",key + " " + mSensorFormats[p] + " " + mSensorUnits[p]);
							 p++;
						 }
						 
					 }	
					 
					 mSensorNames[i]=key;
					 i++;				 
				 	}
			 	
			
			// write header to a file
			
			writer = new BufferedWriter(new FileWriter(outputFile,false));
			
			for (int k=0;k<mSensorNames.length;k++) { 
                writer.write(objectClusterLog.mMyName);
            	writer.write(mDelimiter);
            }
			writer.newLine(); // notepad recognized new lines as \r\n
			
			for (int k=0;k<mSensorNames.length;k++) { 
                writer.write(mSensorNames[k]);
            	writer.write(mDelimiter);
            }
			writer.newLine();
			
			for (int k=0;k<mSensorFormats.length;k++) { 
                writer.write(mSensorFormats[k]);
                
            	writer.write(mDelimiter);
            }
			writer.newLine();
			
			for (int k=0;k<mSensorUnits.length;k++) { 
				if (mSensorUnits[k]=="u8"){writer.write("");}
		        else if (mSensorUnits[k]=="i8"){writer.write("");} 
		        else if (mSensorUnits[k]=="u12"){writer.write("");}
		        else if (mSensorUnits[k]=="u16"){writer.write("");}
		        else if (mSensorUnits[k]=="i16"){writer.write("");}   
		        else {
		        	writer.write(mSensorUnits[k]);
		        }
            	writer.write(mDelimiter);
            }
			writer.newLine();
//			Log.d("Shimmer","Data Written");
			mFirstWrite=false;
			}
			
			//now write data
			for (int r=0;r<mSensorNames.length;r++) {
				Collection<FormatCluster> dataFormats = objectClusterLog.mPropertyCluster.get(mSensorNames[r]);  
				FormatCluster formatCluster = (FormatCluster) returnFormatCluster(dataFormats,mSensorFormats[r],mSensorUnits[r]);  // retrieve the calibrated data
//				Log.d("Shimmer","Data : " +mSensorNames[r] + formatCluster.mData + " "+ formatCluster.mUnits);
				writer.write(Double.toString(formatCluster.mData));
            	writer.write(mDelimiter);
			}
			writer.newLine();
			
			
		}
	catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		Log.d("Shimmer","Error with bufferedwriter");
	}
	}
	
	public void closeFile(){
		if (writer != null){
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String getName(){
		return mFileName;
	}
	
	public File getOutputFile(){
		return outputFile;
	}
	
	public String getAbsoluteName(){
		return outputFile.getAbsolutePath();
	}
	
	private boolean compareStringArray(String[] stringArray, String string){
		boolean uniqueString=true;
		int size = stringArray.length;
		for (int i=0;i<size;i++){
			if (stringArray[i]==string){
				uniqueString=false;
			}	
					
		}
		return uniqueString;
	}
	
	 private FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format, String units){
	    	Iterator<FormatCluster> iFormatCluster=collectionFormatCluster.iterator();
	    	FormatCluster formatCluster;
	    	FormatCluster returnFormatCluster = null;
	    	
	    	while(iFormatCluster.hasNext()){
	    		formatCluster=(FormatCluster)iFormatCluster.next();
	    		if (formatCluster.mFormat==format && formatCluster.mUnits==units){
	    			returnFormatCluster=formatCluster;
	    		}
	    	}
			return returnFormatCluster;
	    }
	
}


