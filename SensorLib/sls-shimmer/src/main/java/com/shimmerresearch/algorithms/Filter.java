/*Rev 0.4
 * 

 This is a BlackMan-Windowed-Sinc Filter. Algorithm for calculating 
 filter coefficients from "The Scientist and Engineer's Guide to Digital Signal Processing",
 copyright ©1997-1998 by Steven W. Smith. 
 For more information visit the book's website at: www.DSPguide.com.
 
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
 * @author Alejandro Saez 
 * 
 * @date   July, 2014
 * 
 * Changes since 0.3
 * - Updated to coefficients[(nTaps/2)] = coefficients[(nTaps/2)] +1;
 * 
 * Changes since 0.2
 * - Changed the way the filtered data is calculated.
 * - Changed the buffer implementation. Now is a LIFO queue. More efficient.
 * - Possibility of filter either one data or an array of data
 * 
 * 
 * Changes since 0.1
 * - Handle an error case - when array of data to filter (number of samples) is bigger than the array of data to filter from the previous iteration
 * - Added resetBuffers() function 
*/
package com.shimmerresearch.algorithms;

import java.util.Arrays;
import java.util.List;

/**
 *
 This is a BlackMan-Windowed-Sinc Filter. Algorithm for calculating 
 filter coefficients from "The Scientist and Engineer's Guide to Digital Signal Processing",
 copyright ©1997-1998 by Steven W. Smith. 
 For more information visit the book's website at: www.DSPguide.com.
 *
 */

public class Filter {
	
	public static int LOW_PASS = 0;
    public static int HIGH_PASS = 1;
    public static int BAND_PASS = 2;
    public static int BAND_STOP = 3;

    // filter parameters   
    private int filterType;
    private double samplingRate = Double.NaN;
    private double[] cornerFrequency;
    private int nTaps;
    private double minCornerFrequency, maxCornerFrequency;

    // buffered data (for filtering streamed data)
    private double[] bufferedX;

    // filter coefficients {h}
    private double[] coefficients;

    // input parameters are invalid
    private boolean validparameters = false;

    // default parameters
    private double defaultSamplingRate = 512;
    private double[] defaultCornerFrequency = { 0.5 };
    private int defaultNTaps = 200;
    
    public Filter() throws Exception{
    	filterType=LOW_PASS;

    	SetFilterParameters(LOW_PASS, defaultSamplingRate, defaultCornerFrequency, defaultNTaps);
    }
     
    public Filter(int filterType) throws Exception{
    	
    	this.filterType=filterType;

    	SetFilterParameters(filterType, defaultSamplingRate, defaultCornerFrequency, defaultNTaps);
    }
    
    public Filter(int filterType, double samplingRate, double[] cornerFrequency) throws Exception{
    	
    	this.filterType=filterType;
    	
        SetFilterParameters(filterType, samplingRate, cornerFrequency, defaultNTaps);
    }

    public Filter(int filterType, double samplingRate, double[] cornerFrequency, int nTaps) throws Exception{
    	
    	this.filterType=filterType;
    	
        SetFilterParameters(filterType, samplingRate, cornerFrequency, nTaps);
    }
    
    
    public void SetFilterParameters(int LoHi, double samplingRate, double[] cornerFrequency, int nTaps) throws Exception{
    	
    	//reset the buffers
    	this.bufferedX = null; 
    	
    	if(cornerFrequency.length!=1){
    		if(cornerFrequency[0] > cornerFrequency[1]){
    			minCornerFrequency = cornerFrequency[1];
    			maxCornerFrequency = cornerFrequency[0];
    		}
    		else{
    			minCornerFrequency = cornerFrequency[0];
    			maxCornerFrequency = cornerFrequency[1];
    		}
    	}
    	else
    		minCornerFrequency = maxCornerFrequency = cornerFrequency[0];
    	
    	
    	if (maxCornerFrequency > samplingRate / 2)
        {
            this.validparameters = false;
            throw new Exception("Error: cornerFrequency is greater than Nyquist frequency. Please choose valid parameters.");
        }
        else
        {
            if (nTaps % 2 != 0)
            {
                nTaps--;
            }

            if (LoHi == LOW_PASS || LoHi == HIGH_PASS) // High pass or Low pass filter
            {
                this.samplingRate = samplingRate;
                this.cornerFrequency = cornerFrequency;
                this.nTaps = nTaps;

                double fc = (cornerFrequency[0] / samplingRate);
                // calculate filter coefficients
                coefficients = new double[nTaps];
                coefficients = calculateCoefficients(fc, LoHi, nTaps);
                this.validparameters = true;
            }
            else if (LoHi == BAND_PASS || LoHi == BAND_STOP)
            {
                if (cornerFrequency.length != 2)
                	throw new Exception("Error. Bandpass or bandstop filter requires two corner frequencies to be specified");
                
                this.samplingRate = samplingRate;
                this.nTaps = nTaps;
                
                double fcHigh = maxCornerFrequency / samplingRate;
                double fcLow = minCornerFrequency / samplingRate;

                // calculate filter coefficients
                double[] coefficientHighPass = calculateCoefficients(fcHigh, HIGH_PASS, nTaps);
                double[] coefficientLowPass = calculateCoefficients(fcLow, LOW_PASS, nTaps);
                
                coefficients = new double[coefficientHighPass.length];
                for(int i=0; i<coefficientHighPass.length;i++){
                	if(LoHi == BAND_PASS)
                		coefficients[i] = - (coefficientHighPass[i] + coefficientLowPass[i]); //sum of HPF and LPF for bandstop filter, spectral inversion for bandpass filter
                	else
                		coefficients[i] = coefficientHighPass[i] + coefficientLowPass[i]; //sum of HPF and LPF for bandstop filter
                }
                
                if(LoHi == BAND_PASS){
                	coefficients[(nTaps/2)] = coefficients[(nTaps/2)] +1;
                }
                
                this.validparameters = true;
            }
            else
            	throw new Exception("Error. Undefined filter type: use 0 - lowpass, 1 - highpass, 2- bandpass, or 3- bandstop");
        }
    }
    
    public double filterData(double data) throws Exception
    {
    	double dataFiltered = Double.NaN;
    	
    	 if (!this.validparameters)
         	throw new Exception("Error. Filter parameters are invalid. Please set filter parameters before filtering data.");
         else
         {
        	 int nSamples = 1;
             int bufferSize = this.nTaps; 
        	 if(bufferedX==null){
        		 bufferedX = new double[bufferSize+nSamples]; // buffers are initiliazed to 0 by default
        		 Arrays.fill(bufferedX, data); // fill the buffer X with the first data        		 
        	 }
        	 else{
        		 System.arraycopy(bufferedX, 1, bufferedX, 0, bufferedX.length-1); //all the elements in the buffer are shifted one position to the left
        		 bufferedX[bufferedX.length-1] = data;
        	 }
        	 
        	 double Y = filter(bufferedX);
        	 dataFiltered = Y;
        	 
         }
    	
    	return dataFiltered;    
    }
    
    public double[] filterData(double[] data) throws Exception
    {
    	if (!this.validparameters)
         	throw new Exception("Error. Filter parameters are invalid. Please set filter parameters before filtering data.");
         else
         {
        	 double[] dataFiltered = new double[data.length];
        	 
        	 for(int i=0; i<data.length; i++){
        		 double individualDataFiltered = filterData(data[i]);
        		 dataFiltered[i] = individualDataFiltered;
        	 }
        	 
        	 return dataFiltered;
         }
    }
    
    private double filter(double[] X)
    {
    	
    	int nTaps = coefficients.length;
    	double Y = 0;
    	
    	for(int i=0; i<nTaps; i++)
    		Y += X[nTaps-i]*coefficients[i];
    	
    	return Y;
    }
    
    private double[] calculateCoefficients(double fc, int LoHi, int nTaps) throws Exception
    {
        if (!(LoHi == LOW_PASS || LoHi == HIGH_PASS))
        	throw new Exception("Error: the function calculateCoefficients() can only be called for LPF or HPF.");

        //Initialization
        int M = nTaps;
        double[] h = new double[M];
        for(int i=0;i<M; i++)
        	h[i] = 0;
        
        for(int i=0;i<M;i++){
        	h[i] = 0.42 - 0.5 * Math.cos((2*Math.PI*i)/M) + 0.08*Math.cos((4*Math.PI*i)/M);
        	if(i!=M/2)
        		h[i] = h[i] * (Math.sin(2*Math.PI*fc*(i-(M/2))))/(i-(M/2));
        	else
        		h[i] = h[i] * (2*Math.PI*fc);
        }
         
        double gain = 0;
        for(int i=0;i<h.length;i++)
        	gain += h[i];
        
        for(int i=0;i<h.length;i++){
        	if(LoHi == HIGH_PASS){
        		h[i] = - h[i]/gain;
        	}
        	else
        		h[i] = h[i]/gain;
        }

        if(LoHi == HIGH_PASS){
        	h[M/2] = h[M/2] + 1;
        }
        
        return h;
    }
    
    
    public double GetSamplingRate(){
        return samplingRate;
    }
    
    protected void SamplingRate(double samplingrate){
        samplingRate = samplingrate;
    }
    
    
    public double[] GetCornerFrequency(){
        return cornerFrequency;
    }
    
    protected void SetCornerFrequency(double[] cf){
        cornerFrequency = cf;
    }
    
    public static double[] fromListToArray(List<Double> list){
		
		double [] array = new double[list.size()];
		for(int i=0;i<list.size();i++)
			array[i] = list.get(i);
		
		return array;
	}
    
    public void resetBuffers(){
    	
    	bufferedX = null;
    }
    
	public int getFilterType() {
		return filterType;
	}    
    
}
