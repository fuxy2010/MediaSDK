package com.nercms.mediademo;

import java.util.List;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.ui.MediaInstance;
import com.tencent.bugly.crashreport.CrashReport;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity
{
	private Button login_button;
	private Button logout_button;
	
	public static final String PREFERENCE_NAME = "com.nercms.mediademo_preferences";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.i("Demo", "onCreate()");
		
		//int ping_delay = MediaInstance.instance().api_get_ping_delay(MediaDemo.server_ip_wan);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		//CrashReport.testJavaCrash();
		
		login_button = (Button)findViewById(R.id.login_button);
		logout_button = (Button)findViewById(R.id.logout_button);
		
		if(true == MediaInstance.instance()._available)
		{
			login_button.setEnabled(false);
			logout_button.setEnabled(true);
		}
		else
		{
			login_button.setEnabled(true);
			logout_button.setEnabled(false);
		}
		
		{
			Log.i("Baidu", "package");
			final PackageManager packageManager = getPackageManager();   
			List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
			Log.i("Baidu", "package size: " + pinfo.size());
			if(pinfo != null)
			{
				for(int i = 0; i < pinfo.size(); i++)
				{
					Log.i("Baidu", "Package: " + pinfo.get(i).packageName);
				}
			}
		}
		
		{
			PackageInfo packageInfo = null;
			
			try
			{
				packageInfo = this.getPackageManager().getPackageInfo("com.baidu.navi", 0);
			}
			catch (NameNotFoundException e)
			{
				packageInfo = null;
			}
			
			if (packageInfo == null)
				Log.i("Baidu", "not found baidu navi.");
			else
				Log.i("Baidu", "has found baidu navi.");
		}
		
		login_button.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				//NetworkStatus.start(getApplicationContext());
				Intent intent = new Intent(getApplicationContext(), MediaDemo.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				getApplicationContext().startActivity(intent);
				
				MediaDemo.server_ip_wan = ((EditText)findViewById(R.id.server_ip)).getText().toString();
				MediaDemo.server_ip_lan = ((EditText)findViewById(R.id.server_ip)).getText().toString();
				MediaDemo.server_in_lan = false;
				MediaDemo.self_id = ((EditText)findViewById(R.id.self_id)).getText().toString();
				MediaDemo.remote_id1 = ((EditText)findViewById(R.id.remote_id1)).getText().toString();
				MediaDemo.remote_id2 = ((EditText)findViewById(R.id.remote_id2)).getText().toString();
				MediaDemo.remote_id3 = ((EditText)findViewById(R.id.remote_id3)).getText().toString();
				MediaDemo.video_source = ((EditText)findViewById(R.id.video_source)).getText().toString();
				
				if(false == MediaInstance.instance()._available)
				{
					if(true)
					{
						MediaInstance.instance().api_start(getApplicationContext(),
													352, 288,
													MediaDemo.server_ip_wan, MediaDemo.server_ip_lan, MediaDemo.server_in_lan,
													MediaDemo.server_port, MediaDemo.self_id, MediaDemo.encrypt_info);
						MediaInstance.instance().api_set_video_render_scale(1.8f);
					}
					
					login_button.setEnabled(false);
					logout_button.setEnabled(true);
				}
				
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("server", MediaDemo.server_ip_wan).commit();
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("self_id", MediaDemo.self_id).commit();
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("remote_id1", MediaDemo.remote_id1).commit();
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("remote_id2", MediaDemo.remote_id2).commit();
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("remote_id3", MediaDemo.remote_id3).commit();
				getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().putString("video_source", MediaDemo.video_source).commit();
			}
		});
		
		logout_button.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				if(true == MediaInstance.instance()._available)
				{
					MediaInstance.instance().api_shutdown();
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		
		MediaDemo.server_ip_wan = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("server", "120.26.46.170");
		MediaDemo.self_id = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("self_id", "1");
		MediaDemo.remote_id1 = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("remote_id1", "2");
		MediaDemo.remote_id2 = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("remote_id2", "3");
		MediaDemo.remote_id3 = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("remote_id3", "5");
		MediaDemo.video_source = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString("video_source", "2");
		
		((EditText)findViewById(R.id.server_ip)).setText(MediaDemo.server_ip_wan);
		((EditText)findViewById(R.id.self_id)).setText(MediaDemo.self_id);
		((EditText)findViewById(R.id.remote_id1)).setText(MediaDemo.remote_id1);
		((EditText)findViewById(R.id.remote_id2)).setText(MediaDemo.remote_id2);
		((EditText)findViewById(R.id.remote_id3)).setText(MediaDemo.remote_id3);
		((EditText)findViewById(R.id.video_source)).setText(MediaDemo.video_source);
		
	}
	
	@Override
	public void onBackPressed()
	{
		MediaInstance.instance().api_shutdown();
		
		finish();
		
		//退出后台线程,以及销毁静态变量
		//System.exit(0);
	}

}
