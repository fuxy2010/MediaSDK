package com.nercms.mediademo;

import com.nercms.schedule.misc.GID;
import com.nercms.schedule.ui.MediaInstance;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity
{
	private Button test_button0;
	private Button test_button1;
	private Button test_button2;
	private Button test_button3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		test_button0 = (Button)findViewById(R.id.test_button0);
		test_button1 = (Button)findViewById(R.id.test_button1);
		
		test_button0.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				MediaDemo.wake_up(getApplicationContext(), null);
			}
		});
		
		test_button1.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				MediaInstance.instance().api_msg_test(GID.MSG_WAKE_UP, "wake up");
			}
		});
		
	}
	
	@Override
	public void onBackPressed()
	{
		MediaInstance.instance().api_shutdown();
		
		finish();
		
		System.exit(0);
	}
}
