package com.nercms.schedule.rtsp;

import java.net.InetSocketAddress;

import com.nercms.schedule.misc.AppManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TestActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	AppManager.get_instance().add_activity(this);
    	
        //fym super.onCreate(savedInstanceState);
        //fym setContentView(R.layout.main);
    	
        AsyncRequest request = new AsyncRequest(new InetSocketAddress("10.0.2.2",8042));
        try {
        	request.setConNumPerNIOThread(5);
        	AsyncRequest.setThreadPoolNum(2, 5, 2);
        	request.setUDP(false);
        	request.setBody("hello world".getBytes());
			request.startAsyn(0);
			
			if(!request.isCurrRequestFull())
			{
	        	request.setBody("My name is dongfengsun".getBytes());
				request.startAsyn(1);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("onCreate", " = "+e.getMessage());
			e.printStackTrace();
		}
    }
}