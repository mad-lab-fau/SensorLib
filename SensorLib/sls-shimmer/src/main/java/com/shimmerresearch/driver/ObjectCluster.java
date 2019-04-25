/*Rev 0.3
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
 * @date   October, 2013
 * 
 * Changes since 0.2
 * - SDLog support
 * 
 * Changes since 0.1
 * - Added method to remove a format 
 * 
 */
package com.shimmerresearch.driver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

final public class ObjectCluster implements Cloneable,Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7601464501144773539L;
	public Multimap<String, FormatCluster> mPropertyCluster = HashMultimap.create();
	public String mMyName;
	public String mBluetoothAddress;
	public byte[] mRawData;
	public double[] mUncalData;
	public double[] mCalData;
	public String[] mSensorNames;
	public String[] mUnitCal;
	public String[] mUnitUncal;
	
	
	String[] mSensorFormats;
	String[] mSensorUnits;
	
	
	public byte[] mSystemTimeStamp = new byte[8];
	
	public ObjectCluster(){
	}
	
	public ObjectCluster(String myName){
		mMyName = myName;
	}

	public ObjectCluster(String myName, String myBlueAdd){
		mMyName = myName;
		mBluetoothAddress=myBlueAdd;
	}

	/**
	 * Takes in a collection of Format Clusters and returns the Format Cluster specified by the string format
	 * @param collectionFormatCluster
	 * @param format 
	 * @return FormatCluster
	 */
	public static FormatCluster returnFormatCluster(Collection<FormatCluster> collectionFormatCluster, String format){
		Iterator<FormatCluster> iFormatCluster=collectionFormatCluster.iterator();
		FormatCluster formatCluster;
		FormatCluster returnFormatCluster = null;

		while(iFormatCluster.hasNext()){
			formatCluster=(FormatCluster)iFormatCluster.next();
			if (formatCluster.mFormat.equals(format)){
				returnFormatCluster=formatCluster;
			}
		}
		return returnFormatCluster;
	}

	/**
	 * Users should note that a property has to be removed before it is replaced
	 * @param propertyname Property name you want to delete
	 * @param formatname Format you want to delete
	 */
	public void removePropertyFormat(String propertyname, String formatname){
		Collection<FormatCluster> colFormats = mPropertyCluster.get(propertyname);  // first retrieve all the possible formats for the current sensor device
		FormatCluster formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(colFormats,formatname)); // retrieve format;
		mPropertyCluster.remove(propertyname, formatCluster);
	}
	
	/**Serializes the object cluster into an array of bytes
	 * @return byte[] an array of bytes
	 * @see java.io.Serializable
	 */
	public byte[] serialize() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private List<String[]> getListofEnabledSensorSignalsandFormats(){
		List<String[]> listofSignals = new ArrayList<String[]>();
		for (int i=0;i<mSensorNames.length;i++){
			String[] channel = new String[]{mMyName,mSensorNames[i],mSensorFormats[i],mSensorUnits[i]};
			listofSignals.add(channel);
		}
		
		return listofSignals;
	}
	
	public List<String[]> generateArrayOfChannels(){
		//First retrieve all the unique keys from the objectClusterLog
		Multimap<String, FormatCluster> m = mPropertyCluster;

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
		return getListofEnabledSensorSignalsandFormats();
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
}
