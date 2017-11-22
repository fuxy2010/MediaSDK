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
	 * ��һʵ��
	 */
	
	public static AppManager get_instance()
	{
		// ���ʵ��,���ǲ����ھͽ���ͬ��������
		if(null == _unique_instance)
		{
			// ���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(AppManager.class)
			{
				//����˫�ؼ��
				if(null == _unique_instance)
				{
					_unique_instance = new AppManager();
				}
			}
		}
		
		return _unique_instance;
	}
	
	
	/**
	 * ���Activity����ջ
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
	 * ��ȡ��ǰActivity����ջ�����һ��ѹ��ģ�
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
	 * ������ǰActivity����ջ�����һ��ѹ��ģ�
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
	 * ����ָ����Activity
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
	 * ����ָ��������Activity
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
	 * ��������Activity
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
	 * �˳�Ӧ�ó���
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