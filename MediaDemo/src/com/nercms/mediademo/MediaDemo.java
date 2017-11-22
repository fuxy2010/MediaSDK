package com.nercms.mediademo;

import java.text.DateFormat.Field;
import java.util.ArrayList;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.ui.MediaInstance;
import com.nercms.schedule.ui.OnMsgCallback;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MediaDemo extends Activity implements OnMsgCallback
{
	//public static String server_ip_wan = "120.26.78.7";//调度服务器IP
	public static String server_ip_wan = "120.26.46.170";
	//public static String server_ip_wan = "192.168.3.42";
	//public static String server_ip_wan = "120.76.159.147";
	//public static String server_ip_wan = "172.16.25.178";//调度服务器IP
	//public static String server_ip_wan = "172.16.24.155";//调度服务器IP
	//public static String server_ip_wan = "192.168.3.5";//调度服务器IP
	//public static String server_ip_wan = "192.168.1.101";//调度服务器IP
	//public static String server_ip_wan = "58.50.28.139";//调度服务器IP
	public static boolean server_in_lan = false;
	//public static String server_ip_lan = "120.26.78.7";
	public static String server_ip_lan = "120.26.46.170";
	//public static String server_ip_lan = "120.76.159.147";
	//public static String server_ip_lan = "192.168.3.5";
	//public static String server_ip_lan = "192.168.0.208";
	public static int server_port = 5060;//调度服务器通信端口
	public static String self_id = "222";//本机注册ID
	public static String encrypt_info = "JEO!FGL#GGG)GG$G$HIG((^&%$FJEF";
	//public static String remote_id1 = "111";//"4294967295";//被叫终端ID，可有多个
	public static String remote_id1 = "4294967295";//被叫终端ID，可有多个
	//public static String remote_id2 = "333";
	//public static String remote_id2 = "333";//"4294967295";
	public static String remote_id2 = "4519065";
	public static String remote_id3 = "";
	public static String video_source = remote_id1;//视频源ID
	//public static String video_source = self_id;//remote_id2;//视频源ID
	
	private Button call_button;
	private Button answer_button;
	private Button reject_button;
	private Button close_button;
	
	private Button test_button0;
	private Button test_button1;
	private Button test_button2;
	private Button test_button3;
	
	SurfaceView video_render_view;
	SurfaceView video_capture_view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.i("Demo", "MediaDemo::onCreate()");
		
		Log.v("MQTT", "timeout: " + (System.currentTimeMillis() / 1000 - 1467164333));
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_media_demo);
		
		video_render_view = (SurfaceView)findViewById(R.id.videorenderview);
		video_capture_view = (SurfaceView)findViewById(R.id.videocaptureview);
		
		
		MediaInstance.instance().api_set_msg_callback(this);
		MediaInstance.instance().api_set_video_view(video_render_view, video_capture_view);//layout_inflater.inflate(R.layout.videorender, null));
		
		call_button = (Button)findViewById(R.id.call_button);
		answer_button = (Button)findViewById(R.id.answer_button);
		reject_button = (Button)findViewById(R.id.reject_button);
		close_button = (Button)findViewById(R.id.close_button);
		
		test_button0 = (Button)findViewById(R.id.test_button0);
		test_button1 = (Button)findViewById(R.id.test_button1);
		test_button2 = (Button)findViewById(R.id.test_button2);
		test_button3 = (Button)findViewById(R.id.test_button3);
		
		call_button.setEnabled(true);
		answer_button.setEnabled(false);
		reject_button.setEnabled(false);
		close_button.setEnabled(false);
		
		AudioManager am = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
		am.setSpeakerphoneOn(false);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);  
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
		
		call_button.setOnClickListener(new Button.OnClickListener()
        								{
											public void onClick(View view)
											{
												//一个调度最多可包含4个终端（含本机）
												ArrayList<String> ids = new ArrayList<String>();
												if(null != remote_id1 && false == remote_id1.isEmpty() && self_id != remote_id1) ids.add(remote_id1);
												if(null != remote_id2 && false == remote_id2.isEmpty() && self_id != remote_id2) ids.add(remote_id2);
												if(null != remote_id3 && false == remote_id3.isEmpty() && self_id != remote_id3) ids.add(remote_id3);
												
												Log.i("Demo", "id: " + ids.size());
												
												if(0 == ids.size())
												{
													Toast.makeText(MediaDemo.this, "请填写被叫终端ID", Toast.LENGTH_SHORT).show();
												}
												else
												{
													boolean ret = MediaInstance.instance().api_start_schedule(ids, video_source);
													if(true == ret)
													{
														call_button.setEnabled(false);
														answer_button.setEnabled(false);
														reject_button.setEnabled(false);
														close_button.setEnabled(true);
													}
													else
													{
														Toast.makeText(MediaDemo.this, "参数错误，或含发起者在内调度总人数超过四个", Toast.LENGTH_LONG).show();
													}
												}
												
												refresh_view();//Toast.makeText(MediaDemo.this, "呼叫", Toast.LENGTH_SHORT).show();
											}
										});
        
		answer_button.setOnClickListener(new Button.OnClickListener()
        								{
											public void onClick(View view)
											{
												MediaInstance.instance().api_accept_schedule_invite();
												
												call_button.setEnabled(false);
												answer_button.setEnabled(false);
												reject_button.setEnabled(false);
												close_button.setEnabled(true);
												
												refresh_view();//Toast.makeText(MediaDemo.this, "接听", Toast.LENGTH_SHORT).show();
											}
										});
        
		reject_button.setOnClickListener(new Button.OnClickListener()
        								{
											public void onClick(View view)
											{
												MediaInstance.instance().api_reject_schedule_invite();
												call_button.setEnabled(true);
												answer_button.setEnabled(false);
												reject_button.setEnabled(false);
												close_button.setEnabled(false);
												
												onBackPressed();//refresh_view();//Toast.makeText(MediaDemo.this, "拒绝", Toast.LENGTH_SHORT).show();
											}
										});
        
        
		close_button.setOnClickListener(new Button.OnClickListener()
        								{
											public void onClick(View view)
											{
												MediaInstance.instance().api_shutdown_schedule();
												
												call_button.setEnabled(true);
												answer_button.setEnabled(false);
												reject_button.setEnabled(false);
												close_button.setEnabled(false);
												
												//onBackPressed();//refresh_view();//Toast.makeText(MediaDemo.this, "挂断", Toast.LENGTH_SHORT).show();
											}
										});
		
		test_button0.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				//MediaInstance.instance().api_hand_free_switch();
			}
		});
		test_button0.setEnabled(true);
		test_button0.setText("免提");
		
		test_button1.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				//MediaInstance.instance().api_test();
				
			}
		});
		test_button1.setEnabled(false);
		
		test_button2.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				//start_play();
				
			}
		});
		test_button2.setEnabled(true);
		
		test_button3.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				//stop_play();
				Log.i("Baidu", "play ringtone");
				//play_ringtone();
				
			}
		});
		test_button3.setEnabled(true);
		
		video_capture_view.setOnLongClickListener
		(
			new OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View v)
				{
					if(true == GD._i_am_video_source)
					{
						MediaInstance.instance().api_reverse_camera();
					}
					return true;
				}
			}
		);
		
		//test
		//Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//getApplicationContext().startActivity(intent);
		//moveTaskToBack(true);
		
		
	}
	
	private void play_ringtone()
	{
		//Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		// Allow user to pick 'Default'
		//intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		// Show only ringtones
		//intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
		//set the default Notification value
		//intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		// Don't show 'Silent'
		//intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		//startActivityForResult(intent, 1);    
        
        
		//RingtoneManager.getRingtone(MediaDemo.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)).play();
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Ringtone ringtone = RingtoneManager.getRingtone(this, notification);
        //ringtone.play();
        
        Class<Ringtone> clazz =Ringtone.class;
		try
		{
			java.lang.reflect.Field field = clazz.getDeclaredField("mLocalPlayer");//返回一个 Field 对象，它反映此 Class 对象所表示的类或接口的指定公共成员字段（※这里要进源码查看属性字段）
			field.setAccessible(true);
			MediaPlayer target = (MediaPlayer) field.get(ringtone);//返回指定对象上此 Field 表示的字段的值
			target.setLooping(true);//设置循环
		}
		catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		
		ringtone.setStreamType(AudioManager.STREAM_RING);//因为rt.stop()使得MediaPlayer置null,所以要重新创建（具体看源码）
		setRingtoneRepeat(ringtone);//设置重复提醒
		ringtone.play();
	}
	
	/////////
	public void onCreate()
	{
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		Ringtone rt = RingtoneManager.getRingtone(getApplicationContext(), uri);
	}


	//反射设置闹铃重复播放
	private void setRingtoneRepeat(Ringtone ringtone)
	{
		Class<Ringtone> clazz =Ringtone.class;
		try
		{
			java.lang.reflect.Field field = clazz.getDeclaredField("mLocalPlayer");//返回一个 Field 对象，它反映此 Class 对象所表示的类或接口的指定公共成员字段（※这里要进源码查看属性字段）
			field.setAccessible(true);
			MediaPlayer target = (MediaPlayer) field.get(ringtone);//返回指定对象上此 Field 表示的字段的值
			target.setLooping(true);//设置循环
		}
		catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
	}

	public static void playRingtone()
	{
		//rt.setStreamType(AudioManager.STREAM_RING);//因为rt.stop()使得MediaPlayer置null,所以要重新创建（具体看源码）
		//setRingtoneRepeat(rt);//设置重复提醒
		//rt.play();
	}
	
	public static void stopRingtone()
	{
		//rt.stop();
	}
	/////////
	
	private void start_play()
	{
		MediaInstance.instance().api_start_play_tone(this, "tone.mp3");
	}
	
	private void stop_play()
	{
		MediaInstance.instance().api_stop_play_tone();
	}
	
	public static void wake_up(Context context, Bundle extras)
	{
		//将页面调至前台
		Intent intent = new Intent(context, MediaDemo.class);
		if(null != extras) intent.putExtras(extras);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(intent);
	}
	
	@Override
	public void on_msg_callback(int what, Object content)
	{
		handler.sendMessage(handler.obtainMessage(what, content));
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		Log.i("Demo", "MediaDemo::onNewIntent()");
		super.onNewIntent(intent);
	}
	
	private Handler handler = new Handler()
	{
		// 回调处理
		@Override
		public void handleMessage(Message msg)
		{
			if(GID.MSG_PING_DELAY != msg.what) MediaDemo.wake_up(getApplicationContext(), null);
			
			refresh_view();
			
			super.handleMessage(msg);
			switch (msg.what)
			{
				case GID.MSG_INCOMING_CALL:
					Toast.makeText(MediaDemo.this, "收到调度邀请 " + (String)(msg.obj), Toast.LENGTH_SHORT).show();
					call_button.setEnabled(false);
					answer_button.setEnabled(true);
					reject_button.setEnabled(true);
					close_button.setEnabled(false);
					break;
					
				case GID.MSG_HANG_UP:
					Toast.makeText(MediaDemo.this, "调度结束", Toast.LENGTH_SHORT).show();
					call_button.setEnabled(true);
					answer_button.setEnabled(false);
					reject_button.setEnabled(false);
					close_button.setEnabled(false);
					//onBackPressed();
					break;
					
				case GID.MSG_RECV_CANCEL:
					Toast.makeText(MediaDemo.this, "主叫方放弃调度", Toast.LENGTH_SHORT).show();
					call_button.setEnabled(true);
					answer_button.setEnabled(false);
					reject_button.setEnabled(false);
					close_button.setEnabled(false);
					//onBackPressed();
					break;
					
				case GID.MSG_WAKE_UP:
					Toast.makeText(MediaDemo.this, "wake up!", Toast.LENGTH_SHORT).show();
					break;
					
				case GID.MSG_REFRESH_VIDEO_VIEW:
					Toast.makeText(MediaDemo.this, "refresh video view!", Toast.LENGTH_SHORT).show();
					break;
					
				case GID.MSG_PING_DELAY:
					Toast.makeText(MediaDemo.this, "ping server delay: " + (Integer)(msg.obj) + "ms", Toast.LENGTH_SHORT).show();
					//Log.v("Temp", "ping server delay: " + (Integer)(msg.obj));
					break;
					
			default:
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.media_demo, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		Log.i("Demo", "MediaDemo::onPause()");
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.i("Demo", "MediaDemo::onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i("Demo", "MediaDemo::onDestroy()");
		super.onDestroy();
	}
	
	private void refresh_view()
	{
		video_render_view = (SurfaceView)findViewById(R.id.videorenderview);
		video_capture_view = (SurfaceView)findViewById(R.id.videocaptureview);
		
		MediaInstance.instance().api_set_msg_callback(this);
		MediaInstance.instance().api_set_video_view(video_render_view, video_capture_view);//layout_inflater.inflate(R.layout.videorender, null));
		
		if(false)
		{
			if(true == GD.is_in_schedule())
			{
				if(false == GD._i_am_video_source)
				{
					Log.v("Demo", "not video source");
					video_capture_view.getHolder().setFormat(PixelFormat.TRANSPARENT);
					video_capture_view.setZOrderOnTop(false);
					video_capture_view.setZOrderMediaOverlay(false);
					video_render_view.setZOrderOnTop(true);
					video_render_view.setZOrderMediaOverlay(true);
				}
				else
				{
					Log.v("Demo", "video source");
					video_render_view.getHolder().setFormat(PixelFormat.TRANSPARENT);
					video_render_view.setZOrderOnTop(false);
					video_render_view.setZOrderMediaOverlay(false);
					video_capture_view.setZOrderOnTop(true);
					video_capture_view.setZOrderMediaOverlay(true);
				}
			}
			else
			{
				video_render_view.setZOrderOnTop(false);
				video_render_view.setZOrderMediaOverlay(false);
				video_capture_view.setZOrderOnTop(true);
				video_capture_view.setZOrderMediaOverlay(true);
			}
		}		
	}

	@Override
	protected void onResume() {
		Log.i("Demo", "MediaDemo::onResume()");
		super.onResume();
		
		refresh_view();
	}
	
	@Override
	protected void onStart() {
		Log.i("Demo", "MediaDemo::onStart()");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		Log.i("Demo", "MediaDemo::onRestart()");
		super.onRestart();
	}
	
	@Override
	public void onBackPressed()
	{
		Log.i("Demo", "MediaDemo::onBackPressed()");
		
		if(true == GD.is_in_schedule())
		{
			Toast.makeText(MediaDemo.this, "请关闭调度后再返回", Toast.LENGTH_SHORT).show();
			return;
		}
		
		super.onBackPressed();
	}
	
	public void finish()
	{
		Log.i("Demo", "MediaDemo::finish()");
		
		moveTaskToBack(true); //设置该activity永不过期，即不执行onDestroy()
		//super.finish();
	}
}
