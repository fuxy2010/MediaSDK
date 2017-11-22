package com.nercms.schedule.sensor;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class SensorHelper
{
	//判断是否摇晃手机锁屏的阈值
	private static final float ACCELEROMETER_THRESHOLD_ON_LOCK = 12.0f;//9.0f;
		
	//判断是否视频采样的阈值
	private static final float ACCELEROMETER_THRESHOLD_ON_VIDEO_SAMPLE = 12.0f;//5.0f;
	//是否正在摇晃手机，如在摇晃则不采样视频
	public static boolean _is_shocking = false;
		
	private SensorManager _sensor_manager = null;
	private Sensor _sensor = null;
	
	private final float alpha = 0.1f;//值越小越敏感？
	
	private float _accelerometer_x;
	private float _accelerometer_y;
	private float _accelerometer_z;
	
	private long _latest_accelerometer_variety_timestamp = 0L;
	
	private Handler _message_handler = null;
	
	
	
	public SensorHelper()
	{
	}
	
	public void register(Context context,Handler handler,int type)
	{
		_sensor_manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		_sensor = _sensor_manager.getDefaultSensor(type);
		_message_handler = handler;
		_sensor_manager.registerListener(_sensor_listener, _sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void unregister()
	{
		_sensor_manager.unregisterListener(_sensor_listener);
		_message_handler = null;
	}
	
	SensorEventListener _sensor_listener = new SensorEventListener()
	{
		@Override
		public void onSensorChanged(SensorEvent e)
		{
			//TODO Auto-generated method stub
			if(Sensor.TYPE_ACCELEROMETER == e.sensor.getType())
			{
			 	float x = e.values[SensorManager.DATA_X];
		        float y = e.values[SensorManager.DATA_Y];
		        float z = e.values[SensorManager.DATA_Z];
		        
		        //Log.v("Video", "X1 " + x + ", Y1 " + y + ", Z1 " +z);
		        
		        //用低通滤波器分离出重力加速度
		        //Isolate the force of gravity with the low-pass filter.
		        _accelerometer_x = x * alpha + _accelerometer_x * (1.0f - alpha);
		        _accelerometer_y = y * alpha + _accelerometer_y * (1.0f - alpha);
		        _accelerometer_z = z * alpha + _accelerometer_z * (1.0f - alpha);
		        
		        //用高通滤波器剔除重力干扰
		        //Remove the gravity contribution with the high-pass filter.
		        x  -= _accelerometer_x;
		        y  -= _accelerometer_y;
		        z  -= _accelerometer_z;
		        
		        //Log.v("Video", "X2 " + x + ", Y2 " + y + ", Z2 " +z);
		
		        if(Math.abs(x) > ACCELEROMETER_THRESHOLD_ON_LOCK || Math.abs(y) > ACCELEROMETER_THRESHOLD_ON_LOCK)
		        {
		        	long time = SystemClock.uptimeMillis();
		        	
		        	if(null != _message_handler && 1000 < (time - _latest_accelerometer_variety_timestamp))
		        	{
		        		_message_handler.sendEmptyMessage(GID.MSG_ACCELERATION_VARIETY);//锁屏
		        		_latest_accelerometer_variety_timestamp = time;
		        	}
		        }
		        
		        if(ACCELEROMETER_THRESHOLD_ON_VIDEO_SAMPLE < Math.abs(x)
		        	|| ACCELEROMETER_THRESHOLD_ON_VIDEO_SAMPLE < Math.abs(y)
		        	|| ACCELEROMETER_THRESHOLD_ON_VIDEO_SAMPLE < Math.abs(z))
		        {
		        	_is_shocking = true;
		        	//Log.v("Video", "shocking X: " + x + ", Y: " + y + ", Z: " + z);
		        	//Log.v("Video", "shocking X: " + (int)x + ", Y: " + (int)y + ", Z: " + (int)z);
		        }
		        else
		        {
		        	_is_shocking = false;
		        }
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy)
		{
			//TODO Auto-generated method stub
		}
	};
}
