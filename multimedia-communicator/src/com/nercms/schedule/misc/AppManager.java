package com.nercms.schedule.misc;

import java.util.Stack;

import com.nercms.schedule.rtsp.RTSPClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class AppManager
{
	
	private static Stack<Activity> _activity_stack;
	private static AppManager _unique_instance;
	
	private AppManager(){}
	/**
	 * 单一实例
	 */
	
	public static AppManager get_instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_instance)
		{
			// 对其进行锁,防止两个线程同时进入同步代码区
			synchronized(AppManager.class)
			{
				//必须双重检查
				if(null == _unique_instance)
				{
					_unique_instance = new AppManager();
				}
			}
		}
		
		return _unique_instance;
	}
	
	
	/**
	 * 添加Activity到堆栈
	 */
	public void add_activity(Activity activity)
	{
		synchronized(AppManager.class)
		{
			if(_activity_stack==null)
			{
				_activity_stack=new Stack<Activity>();
			}
			_activity_stack.add(activity);
		}
	}
	/**
	 * 获取当前Activity（堆栈中最后一个压入的）
	 */
	public Activity get_current_activity()
	{
		synchronized(AppManager.class)
		{
			Activity activity=_activity_stack.lastElement();
			return activity;
		}
	}
	/**
	 * 结束当前Activity（堆栈中最后一个压入的）
	 */
	public void finish_activity()
	{
		synchronized(AppManager.class)
		{
			Activity activity=_activity_stack.lastElement();
			if(activity!=null)
			{
				activity.finish();
				activity = null;
			}
		}
	}
	/**
	 * 结束指定的Activity
	 */
	public void finish_activity(Activity activity)
	{
		synchronized(AppManager.class)
		{
			if(activity!=null){
				_activity_stack.remove(activity);
				activity.finish();
				activity=null;
			}
		}
	}
	/**
	 * 结束指定类名的Activity
	 */
	public void finish_activity(Class<?> cls)
	{
		synchronized(AppManager.class)
		{
			for (Activity activity : _activity_stack)
			{
				if(activity.getClass().equals(cls))
				{
					finish_activity(activity);
				}
			}
		}
	}
	/**
	 * 结束所有Activity
	 */
	public void finish_all_activity()
	{
		synchronized(AppManager.class)
		{
			for (int i = 0, size = _activity_stack.size(); i < size; i++)
			{
	            if (null != _activity_stack.get(i))
	            {
	            	android.util.Log.v("Schedule", "finish: " + _activity_stack.get(i).getLocalClassName());
	            	
	            	_activity_stack.get(i).finish();
	            }
		    }
			_activity_stack.clear();
		}
	}
	/**
	 * 退出应用程序
	 */
	public void app_exit(Context context)
	{
		try
		{
			android.util.Log.v("Schedule", "app_exit");
			
			finish_all_activity();
			
			ActivityManager activityMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			activityMgr.killBackgroundProcesses(context.getPackageName());
			System.exit(0);
		}
		catch (Exception e){}
	}
}