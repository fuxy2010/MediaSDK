package com.nercms.schedule;

import android.app.Application;
import android.util.Log;
import com.nercms.schedule.misc.GD;

public class MultimediaCommunicatorApplication extends Application
{

	@Override
	public void onCreate()
	{
		super.onCreate();
		
		//GD.set_global_context(getApplicationContext()); //����ȫ�ֵ�context
		
		//Log.v("Baidu", "ScheduleApplication");
	}

}