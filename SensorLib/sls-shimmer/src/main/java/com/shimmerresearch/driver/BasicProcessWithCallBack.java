package com.shimmerresearch.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class BasicProcessWithCallBack {

	protected Callable mThread = null;
	//protected BlockingQueue<ShimmerMSG> mQueue = new ArrayBlockingQueue<ShimmerMSG>(1024);
	protected LinkedBlockingDeque<ShimmerMsg> mQueue = new LinkedBlockingDeque<ShimmerMsg>(1024);
	protected ConsumerThread mGUIConsumerThread = null;
	WaitForData mWaitForData = null;
	List<Callable> mListOfThreads = new ArrayList<Callable>();
	List<WaitForData> mListWaitForData = new ArrayList<WaitForData>();
	public BasicProcessWithCallBack(){
		
	}
	
	public void queueMethod(int i,Object ojc){
		try {
			mQueue.put(new ShimmerMsg(i,ojc));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**This is a seperate thread running on the callback msgs from lower layer
	 * @param shimmerMSG
	 */
	protected abstract void processMsgFromCallback(ShimmerMsg shimmerMSG);
	
	
	
	
	
	public class ConsumerThread extends Thread {
		public boolean stop = false;
		public void run() {
			while (!stop) {
				try {
					ShimmerMsg shimmerMSG = mQueue.take();
					processMsgFromCallback(shimmerMSG);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.print("QUE BLOCKED");
					e.printStackTrace();
				}
			}
		};
		
	}
	
	
	
	public BasicProcessWithCallBack(BasicProcessWithCallBack b){
		mWaitForData = new WaitForData(b);
	}
	
	public void setWaitForData(BasicProcessWithCallBack b){
		if (mGUIConsumerThread==null){
			mGUIConsumerThread = new ConsumerThread();
			mGUIConsumerThread.start();
		}
		if (mWaitForData!=null){
			mListWaitForData.add(new WaitForData(b));
		} else {
			mWaitForData = new WaitForData(b);
		}
	};
	
	public void passCallback(Callable c) {
		// TODO Auto-generated method stub
		if (mThread!=null){
			mListOfThreads.add(c);
		} else {
			mThread = c;
		}
	}
	
	
    public void sendCallBackMsg(ShimmerMsg s){
    	if (mThread!=null){
    		mThread.callBackMethod(s);
    	} 
    	for (Callable c:mListOfThreads){
    		c.callBackMethod(s);
    	}
    }
    
    public void sendCallBackMsg(int i, Object ojc){
    	if (mThread!=null){
    		mThread.callBackMethod( i, ojc);
    	}
    	for (Callable c:mListOfThreads){
    		c.callBackMethod(i,ojc);
    	}
    }
    
    //this is for the upper layer
	public class WaitForData implements com.shimmerresearch.driver.Callable  
	{

		public WaitForData(BasicProcessWithCallBack bpwcb)  
		{  
			bpwcb.passCallback(this);
		} 
		
		@Override
		public void callBackMethod(ShimmerMsg s) {
			try {
				mQueue.put(s);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}

		@Override
		public void callBackMethod(int i, Object ojc) {
			try {
				mQueue.put(new ShimmerMsg(i,ojc));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}		
	
	}

	
}
