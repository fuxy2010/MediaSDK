package com.nercms.schedule.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.nercms.schedule.R;
import com.nercms.schedule.audio.AudioRecvDecPlay;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.misc.Participant;
import com.nercms.schedule.network.MQTT;
import com.nercms.schedule.network.NetworkStatus;
import com.nercms.schedule.network.OnCall;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.task.ApplicationExitTask;
import com.nercms.schedule.video.VideoRender;

//地图界面用于显示警员在地图上
public class MediaInstance
{	
	private HashMap<String, Integer> _participants_status = new HashMap<String,Integer>(); //参与调度人员的状态，是否响应与是否是视频源
		
	public static Handler _message_handler = null; // 界面消息处理句柄
	
	private boolean _is_schedule_sponsor = false;//是否是调度的发起者
	
	//private ImageView _image = null; // 警徽图片控件
	//private TextView _textview_sos = null; // 显示SOS的文本控
		
	//用于调度控制
	private String _delete_imsis = "",_speaker_imsis = "";
	private boolean _speaker_status_changed = false;
	
	private boolean _i_am_speaker = false; //本人是发言人
		
	private int _network_status = 2; //默认网络状态
	
	private int _role = -1; //0-听众非视频源，1-发言人 非视频源，2-听众兼视频源，3-发言人兼视频源
	
	//private boolean _handle_cancel_msg = false; //是否处理cancel消息
	
	private VideoRender _video_render = new VideoRender();
	
	//单键处理
  	private volatile static MediaInstance _unique_instance = null;
	public static MediaInstance instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_instance)
		{
			// 对其进行锁,防止两个线程同时进入同步代码区
			synchronized(MediaInstance.class)
			{
				//必须双重检查
				if(null == _unique_instance)
				{
					_unique_instance = new MediaInstance();
				}
			}
		}
		
		return _unique_instance;
	}
	
	public void MediaInstance()
	{
		Log.v("Baidu", "MediaInstance::onCreate");
		
		GD._start_timestamp = System.currentTimeMillis();
	}
	
	public int api_get_ping_delay(String server)
	{
		return (int)(NetworkStatus.ping_delay(server));
	}
	
	//初始化
	public boolean _available = false;
	public boolean api_start(Context globla_context, int video_width, int video_height, String server_ip_wan, String server_ip_lan, boolean server_in_lan, int server_port, String self_id, String encrypt_info)
	{
		//init
		GD.set_global_context(globla_context);
		
		GD.VIDEO_WIDTH = video_width;
		GD.VIDEO_HEIGHT = video_height;
		GD.VideoBit = Bitmap.createBitmap(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, Config.RGB_565);//解码后图像缓存
		
		//获取屏幕锁，屏幕保持常亮
		PowerManager _pm = (PowerManager)GD.get_global_context().getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = _pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PersonalIncomingCall");
		wl.acquire();
		
		//self
		GD.set_unique_id(Long.parseLong(self_id));
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.idle);
		
		//service
		register_message();//注册地图处理消息,必须在init_view之前调用
		
		GD.DEFAULT_SCHEDULE_SERVER = server_ip_wan;
		GD.OVER_VPDN = server_in_lan;
		GD.SIP_SERVER_LAN_IP_IN_VPN = (false == GD.OVER_VPDN) ? server_ip_wan : server_ip_lan;//SIP服务器内网地址
		//SP.set(GD.get_global_context(), SP.PREF_SCHEDULE_SERVER, server_ip);
		//SP.set(GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 12580);
		GD.SERVER_NAT_PORT = 12580;
		
		GD.SIP_SERVER_PORT = server_port;
		
		//MQTT初始化
		MQTT.SERVER_URL = "tcp://" + server_ip_wan + ":1883";
		MQTT.CLIENT_ID = GD.get_unique_id(GD.get_global_context()) + "";
		MQTT.SCHEDULE_SERVER_ID = "VUA";
		Log.i("MQTT", "start mqtt: " + MQTT.SERVER_URL);
		MQTT.instance().keep_alive();
		
		GD.start_sip_media_service(); //启动全部完成后，开启相应SIP及媒体服务
		
		_available = true;
		return true;
	}
	
	public void api_set_msg_callback(OnMsgCallback callback)
	{
		if(false == _available) return;
		
		GD._msg_callback = callback;
	}
	
	public void api_set_video_view(SurfaceView video_render_view, SurfaceView video_capture_view)
	{
		if(false == _available) return;
		
		_video_render.set_video_view(video_render_view, video_capture_view);
	}
	
	public void api_msg_test(int what, String content)
	{
		if(false == _available) return;
		
		MessageHandlerManager.get_instance().handle_message(GID.MSG_DESTORY);
		
		if(null == GD._msg_callback)
			return;
		
		if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(what, content);
	}
	
	public void api_shutdown()
	{
		if(false == _available) return;
		
		Log.v("Schedule", "on_destroy");
		
		//_video_layout.removeAllViews();
		api_shutdown_schedule();
		
		//关闭音视频服务和Sip服务
		//GD.shutdown_sip_media_service();
		//ApplicationExitTask._activity_context = GD.get_global_context();
		new ApplicationExitTask().execute();
		
		//通知还存在的界面销毁
		//在ApplicationExitTask中执行 MessageHandlerManager.get_instance().handle_message(GID.MSG_DESTORY);
				
		//在界面销毁时，取消Handler的注册
		unregister_message();
		
		//_wl.release();
		
		_available = false;
		
		//退出后台线程,以及销毁静态变量,必须调用
		System.exit(0);
	}
	
	//呼叫
	public boolean api_start_schedule(ArrayList<String> participants, String video_source_id)
	{
		if(false == _available)
		{
			Log.i("Baidu", "api_start_schedule available false");
			return false;
		}
		
		if(4 < participants.size())
			return false;
		
		start_normal_schedule(participants, video_source_id);
		
		//if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_START_SCHEDULE, null);
		
		return true;
	}
	
	//挂断
	public void api_shutdown_schedule()
	{
		/*if(false == _available) return;
		
		if(GD.is_in_schedule())
		{
			//在调度中，则关闭调度
			MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE,GD.MEDIA_INSTANCE);
		}*/
		
		Log.v("Baidu", "quit_schedule");
		
		api_reject_schedule_invite();
		
		Log.v("Baidu", "quit_schedule -> shutdown_schedule");
		close_outgoing_schedule();
		
		_is_schedule_sponsor = false;
	}
	
	//接受调度邀请
	public void api_accept_schedule_invite()
	{
		if(false == _available) return;
		
		Log.v("Baidu", "off hook");
		
		(new Thread()
		{
			public void run()
			{
				Log.v("SIP", "thread answercall");
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).answercall();
				else OnCall.instance().stop();
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
		
		//hide_image_and_sos(); //隐藏图片和SOS控件
		
		//find_answer_image_control(); //先初始化调度接受后的图片控件
		
		on_start_schedule();//接听后才开始采集发送语音

		//延迟1秒显示视频源
		_message_handler.sendMessageDelayed(_message_handler.obtainMessage(GID.MSG_SHOW_VIDEO), 1000);

		//start_refresh_overlays_time_task();
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.in_schedule);//GeneralDefine.setConferenceOn();
	}
	
	//拒绝调度邀请
	public void api_reject_schedule_invite()
	{
		if(false == _available) return;
		
		new Thread()
    	{
			@Override
			public void run()
			{
				Log.v("SIP", "thread rejectcall");
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).rejectcall();
				else
				{
					OnCall.instance().stop();
					
					Log.v("MQTT", "====== reject call");
					
					GD.set_schedule_state(GD.SCHEDULE_STATE.idle);
					
				}
				
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    	}.start();
	}
	
	//免提
	public void api_hand_free_switch()
	{
		AudioRecvDecPlay._hands_free = !AudioRecvDecPlay._hands_free;
		GD.hands_free_volume_switch();
	}
	
	//切换摄像头
	public void api_reverse_camera()
	{
		if(null == _video_render)
			return;
		
		_video_render.reverse();
	}
	
	public void api_set_video_render_scale(float scale)
	{
		GD._video_render_scale = scale;
	}
	
	public void api_set_video_resolution(int width, int height)
	{
		GD.VIDEO_WIDTH = width;
		GD.VIDEO_HEIGHT = height;
	}
	
	private MediaPlayer _player = null;
	private Object _tone_mutex = new Object();
	private int _audio_mode = AudioManager.MODE_CURRENT;
	public void api_start_play_tone(Context ctx, String file)
	{
		Log.i("Temp", "api_start_play_tone");		
		synchronized(_tone_mutex)
		{
			AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
			am.setSpeakerphoneOn(false);
			
			_audio_mode = am.getMode();
			//setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);  
			am.setMode(AudioManager.MODE_IN_COMMUNICATION);
			
			if(false)//raw
			{
				//_player = MediaPlayer.create(GD.get_global_context(), R.raw.tone);
				//_player.setLooping(true);
				//_player.start();
			}
			
			if(true)
			{
				try
				{
					AssetManager assetManager = ctx.getAssets();
					AssetFileDescriptor afd = assetManager.openFd(file);
					_player = new MediaPlayer();
					_player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
					_player.setLooping(true);
					_player.prepare();
					_player.start();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void api_stop_play_tone()
	{
		synchronized(_tone_mutex)
		{
			//Log.i("Temp", "api_stop_play_tone 1");
			if(null != _player && true == _player.isPlaying())
			{
				Log.i("Temp", "api_stop_play_tone 2");		
				_player.stop();
				_player.release();
				_player = null;
				
				AudioManager am = (AudioManager)GD.get_global_context().getSystemService(Context.AUDIO_SERVICE);
				
				//am.setSpeakerphoneOn(false);			
				//am.setMode(AudioManager.MODE_NORMAL);
				am.setMode(_audio_mode);
				am.setSpeakerphoneOn(AudioRecvDecPlay._hands_free);
			}
			
		}		
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//调度相关函数
	//发起调度
	private void start_normal_schedule(ArrayList<String> participants, String video_source_id)
	{
		Log.v("Baidu", "start new schedule");
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.in_schedule);//GeneralDefine.setConferenceOn();		
			
		send_convene_message(participants, video_source_id); //给服务器发送调度的消息
			
		_is_schedule_sponsor = true;
		
		on_start_schedule(); //程序处于调度状态
	}
	
	//发送发起调度的消息给服务器
	private void send_convene_message(ArrayList<String> participants, String video_source_id)
	{
		/*
		{
			"t":"convene",
			"l":"2",
			"id":["s1111","s2222","s3333","4444","5555"],
			"vs":"1111"//视频源
		}*/
		//String msg = "{\"t\":\"convene\","+ "\"l\":\""+ SipPreference.get_parameter(ScheduleMap.this,SipPreference.PREF_SCHEDULE_LEVEL, 0) + "\",\"id\":[";
		//String msg = "{\"t\":\"convene\","+ "\"l\":\"1\",\"id\":[";//将所有调度级别均设为1，高于所有终端调度级别
		String msg = "{\"t\":\"convene\"," + "\"i\":\"" + GD.get_unique_id(GD.get_global_context()) + "\"," +  "\"l\":\"1\",\"id\":[";//将所有调度级别均设为1，高于所有终端调度级别
		
		//得到message中关于id字段的内容--所有参与调度人员的IMSI
		String ids = "";
		int size = participants.size();//_participant_imsis.size();
		int participants_num = 0;
		
		Log.v("Baidu", "schedule participants " + size);
		
		//调度初始即对GD._participants赋初值
		synchronized(GD._participants_lock)
		{
			GD._participants.clear();
			
			for(int i = 0; i < size; i++)
			{
				/*if(false == _participant_checkbox_status.get(i))
				{
					continue;
				}*/
				
				Log.v("Baidu", "participant: " + participants.get(i));
				
				ids = ids + "\"sa" + participants.get(i) + "\",";
				participants_num++;
				
				Participant ua = new Participant();
				ua._id = Long.parseLong(participants.get(i));
				ua._type = 0;
				ua._name = "unknown";//_participant_names.get(i);
				ua._has_answered = false;
				ua._is_speaker = true;
				ua._is_video_source = (video_source_id == participants.get(i)) ? true : false;
				
				GD._participants.put(participants.get(i), ua);
			}
		}
		
		GD._video_source_imsi = video_source_id;
		GD._i_am_video_source = (GD.get_unique_id(GD.get_global_context()) == Long.parseLong(video_source_id)) ? true : false;
		
		Log.v("Baidu", "i am video source " + GD._i_am_video_source + ", " + GD._video_source_imsi);
		
		msg += ids.substring(0, ids.length() - 1);//去掉末尾的逗号
		msg += ("],\"vs\":\"a" + video_source_id +  "\"}");
		
		SP.set(GD.get_global_context(), SP.PREF_LAST_VIDEO_SOURCE, video_source_id);
		
		Log.i("Baidu", "convene msg: " + msg);
		
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2);
			
	}
	
	//将程序设置处于调度状态
	private void on_start_schedule()
	{
		Log.v("Baidu", "on_start_schedule");
		//默认开始调度时不锁定屏幕
		//_screen_locked = false;
		//_imageview_lock.setVisibility(View.GONE);
		
		//开启加速感应器
		//_sensor_helper.register(GD.get_global_context(), _message_handler, Sensor.TYPE_ACCELEROMETER);
		
		//init_video_view(); //初始化视频控件的容器控件
		
		//设置参数
		//_video_source_imsi = "";
		GD._video_source_imsi = "";
		//_is_video_visible = true;
		
		register_schedule_message();
		
		//在状态栏上显示图标，提示用户在调度中
		//show_notification();
		
		//显示视频控件和时间控件
		//show_time_control();

		GD.set_conference_volume(GD.get_global_context()); // 设置通话音量
		
		//_i_am_speaker = true; //默认是发言人
		_network_status = NetworkStatus.get_network_status();
		
		//接通后无论是否发言人均开启音频录制和播放模块
		//一旦发言人改变只在调度服务器中处理
		start_audio_record(); // 启动音频录制的模块
		
		start_audio_play(); //开启播放音频数据的模块

		_video_render.set_video_state(); //设置视频状态
		
		//_schedule_answer_info = "";
	}
	
	//关闭调度
	private void close_outgoing_schedule()
	{
		Log.i("Baidu", "close schedule");
		
		//Log.v("Media", "set_schedule_state IDLE 2");
		GD.set_schedule_state(GD.SCHEDULE_STATE.idle);//GeneralDefine.setConferenceOff();
		//关闭RTSP连接
		//Log.v("Call", "close rtsp session");
		//RTSPClient.get_instance().disconnect();
		//RTSPClient.get_instance().stop();
		
		//GD.reset_volumn_change(); //在调度关闭时，修改音量变化时比较的基准时间
		
		GD.set_conference_volume(GD.get_global_context()); //设置通话音量
		
		//不锁屏幕
		//_screen_locked = false;
		//_imageview_lock.setVisibility(View.GONE);
		
		//关闭加速感应器
		//_sensor_helper.unregister();
		
		if(true == _is_schedule_sponsor)
		{
			Log.i("Baidu", "close schedule 2");
			//给服务器发送消息，通知调度结束
			send_close_message();
			
			// 关闭会议时会议发起者要记得清除SipdroidEngine
			GD._participants.clear();
			
			GD._conference_id = "";  //初始化会议的ID
		}

		_role = -1;
		
		//rmv _notification_manager.cancel(_notification_id);//清除提示用户调度中的图标
		
		//SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, "注册成功", R.drawable.sym_presence_available , 0); //设置状态栏的图标为注册成功
		
		stop_audio_play();
		stop_audio_record();

		_video_render.stop_video_render();
		_video_render.stop_video_capture();

		unregister_schedule_message(); //注销与调度有关的注册消息
		
		//将Prefreference中的is_video_source改为false
		GD._i_am_video_source = false;

		//保险起见
		//SP.set(this,SP.PREF_ANSWER_RECV_VIDEO, false);
		//SP.set(this,SP.PREF_ANSWER_SEND_VIDEO, false);
		
		unregister_schedule_message(); //注销与调度有关的注册消息

		//重置参数
		GD._i_am_video_source = false;
		//_video_source_imsi = "";
		GD._video_source_imsi = "";
		//_handle_receive_video_msg = false;
		
		_participants_status.clear(); //清空状态
		
		//回复默认免提状态
		AudioRecvDecPlay._hands_free = GD.DEFAULT_HANDS_FREE;//_is_handfree_on = false;
		GD.hands_free_volume_switch();//(ScheduleMap.this, _is_handfree_on);
		
		//关闭暂停
		GD.MEDIA_PAUSE_IN_SCHEDULE = false;
		
		restart_sip_service();
	}
	
	//给服务器发送消息，通知调度结束
	private void send_close_message()
	{
		String msg = "{\"t\":\"close\",\"c\":\"" + GD._conference_id + "\"}";
		Log.i("Baidu", "close conference: " + msg);
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);		
		else MQTT.instance().publish_message_to_server(msg, 2);

	}
		
	//注册地图处理消息
	private void register_message()
	{
		_message_handler = new MapMessageHandler(GD.get_global_context().getMainLooper());
		
		Log.v("Baidu", "message handler 1: " + _message_handler);
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_DESTORY, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_UPDATE_NETWORK_STATUS, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_NOTIFICATION, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_SOCKET_ERROR, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_ADJUST_LOCAL_TIME, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_UPDATE_SYSTEM_TIPS, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_UPDATE_APPLICATION, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_FORCE_EXIT, GD.MEDIA_INSTANCE);
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_INCOMING_CALL, GD.MEDIA_INSTANCE);
	}
	
	//注销地图处理消息
	private void unregister_message()
	{
		MessageHandlerManager.get_instance().unregister(GID.MSG_DESTORY, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_UPDATE_NETWORK_STATUS, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_NOTIFICATION, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_SOCKET_ERROR, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_ADJUST_LOCAL_TIME, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_UPDATE_SYSTEM_TIPS, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_UPDATE_APPLICATION, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_FORCE_EXIT, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_INCOMING_CALL, GD.MEDIA_INSTANCE);
	}
	
	//socket未创建成功时的提示消息
	void show_socket_error_tip()
	{
		new AlertDialog.Builder(GD.get_global_context()).setTitle("提示!").setMessage("程序异常，请重启程序！")
			.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog,int whichButton)
				{
					dialog.dismiss();
					Log.v("Media", "shutdown_schedule 2");
					close_outgoing_schedule();
				}
			}).show();
	}
	
	void api()
	{
		//退出
		start_exit_task();
		
		//发起新调度
		//start_normal_schedule();//_message_handler.sendEmptyMessage(GID.MSG_NEW_SCHEDULE);
		
		//调度控制
		//check_participant_status();
		
		//发送消息更新下属状态
		//_message_handler.sendEmptyMessageDelayed(GID.MSG_REFRESH_MAP, 500);
		
		//关闭主叫调度
		close_outgoing_schedule();
		
		//免提
		AudioRecvDecPlay._hands_free = !AudioRecvDecPlay._hands_free;
		GD.hands_free_volume_switch();
		
		//接听
		api_accept_schedule_invite();
		
		//拒绝
		api_reject_schedule_invite();
	
		//关闭被叫调度
		close_incoming_schedule();
		
		//保存定位服务器的ip变量
		SP.set(GD.get_global_context(), SP.PREF_LOCATION_SERVER, "xxx.xxx.xxx.xxx");
		GD.change_register_time(GD.get_global_context());
	}
	
	private void start_exit_task()
	{
		//ApplicationExitTask._activity_context = GD.get_global_context();
		//new ApplicationExitTask().execute();
	}

	//退出调度
	private void close_incoming_schedule()
	{
		Log.v("Baidu", "hang up");
		
		api_reject_schedule_invite();
		
		Log.v("Baidu", "shutdown_schedule 6");
		close_outgoing_schedule();
	}
	
	//删除人员的消息
	private void send_delete_message()
	{
		_delete_imsis = _delete_imsis.substring(0, _delete_imsis.length()-1);
		
		//检测删除人员中是否有人是视频源
		/*if(_delete_imsis.contains(_video_source_imsi_dialog))
		{
			_video_source_imsi_dialog = "";
		}*/
		
		/**
		 * { "t":"remove", "c":"1111",
		 * "id":["a3333","a4444","a5555"]//该字段格式见2.3.1.1 }
		 */
		String msg = "{\"t\":\"remove\",\"c\":\"" + GD._conference_id + "\",\"id\":[" + _delete_imsis + "]}";
		Log.i(GD.LOG_TAG,"delete msg: " + msg);
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2); 
	}
	
	//发言人改变的消息
	private void send_speaker_change_message(){
		_speaker_imsis += ("\"a" + GD.get_unique_id(GD.get_global_context()) + "\"");
		
		String msg = "{\"t\":\"speaker\",\"c\":\"" + GD._conference_id + "\",\"id\":[";
	
		//改变发言人时，一定要将自己的id加上
		msg += _speaker_imsis;
		msg += "]}";
		
		Log.i(GD.LOG_TAG,"speaker msg: " + msg);
		
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2);
	}
	
	//是否有人员添加，有则向服务器发送消息
	private void check_have_participant_add()
	{
		/*String add_imsis = "";
		for(int i = _participant_nums; i < _participant_imsis.size()-1; i++){
			if(_participant_checkbox_status.get(i))
			{
				add_imsis = add_imsis + (_participant_role.get(i) ? "\"sa" : "\"aa") + _participant_imsis.get(i) + "\",";
			}
		}
		if(!add_imsis.equals("")){
			add_imsis = add_imsis.substring(0,add_imsis.length()-1);
			String msg = "{\"t\":\"add\",\"c\":\"" + GD._conference_id + "\",\"id\":[";
			msg += add_imsis;
			msg += "]}";
			Log.i(GD.LOG_TAG,"add msg: " + msg);
			if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
			else MQTT.get_instance().publish_message_to_server(msg, 2);
		}*/
	}
	
	//检测发言人是否改变、是否有人员添加、视频源是否改变
	private void check_others()
	{
		if(_speaker_status_changed)
		{
			send_speaker_change_message();
		}
		//检查是否有人员添加
		check_have_participant_add();
		
		//检查视频源是否有改变
		check_video_source_changed();
	}
	
	//视频源是否有改变
	private void check_video_source_changed()
	{
		/*if(false == _video_source_imsi_dialog.equals(_video_source_imsi_dialog_bak))
		{
			String msg = "{\"t\":\"video\",\"c\":\"" + GD._conference_id
					+ "\",\"id\":\"a" + _video_source_imsi_dialog + "\"}";
			
			Log.i(GD.LOG_TAG,"video change msg: " + msg);
			
			if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
			else MQTT.get_instance().publish_message_to_server(msg, 2);
			
			SP.set(GD.get_global_context(), SP.PREF_LAST_VIDEO_SOURCE, _video_source_imsi_dialog);
			
			GD._video_source_imsi = "";
		}*/
	}

	//注册与调度有关的消息
	private void register_schedule_message()
	{
		Log.v("Baidu", "message handler 2: " + _message_handler);
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_SEND_VIDEO, GD.MEDIA_INSTANCE); //发送视频
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RECV_VIDEO, GD.MEDIA_INSTANCE); //接受视频
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_CHANGE_ROLE, GD.MEDIA_INSTANCE); //角色改变
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_STOP_SCHEDULE, GD.MEDIA_INSTANCE); //关闭SOS
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RTSP_SESSION_RECONNECT, GD.MEDIA_INSTANCE); //RTSP会话重连
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
		
		//MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_INCOMING_CALL, GD.MEDIA_INSTANCE);
	}
	
	//注销与调度有关的消息
	private void unregister_schedule_message()
	{
		MessageHandlerManager.get_instance().unregister(GID.MSG_SEND_VIDEO,GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_RECV_VIDEO,GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_CHANGE_ROLE,GD.MEDIA_INSTANCE);		
		
		MessageHandlerManager.get_instance().unregister(GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().unregister(GID.MSG_RTSP_SESSION_RECONNECT,GD.MEDIA_INSTANCE);
		
		MessageHandlerManager.get_instance().unregister(GID.MSG_CHANGE_ROLE,GD.MEDIA_INSTANCE);
		
		//MessageHandlerManager.get_instance().unregister(GID.MSG_INCOMING_CALL, GD.MEDIA_INSTANCE);
	}
	
	/**与音频有关的函数
	 * */
	// 启动音频录制的模块
	private void start_audio_record()
	{
		// false 表明不发送音频
		 if (false == SP.get(GD.get_global_context(),SP.PREF_AUDIO_SEND_ON, true))
		 {
			 return;
		 }

		MediaThreadManager.get_instance()._audio_recorder_idle = false;
		
		Log.v("Audio", "start_audio_record");		
	}
	
	//关闭音频录制的模块
	private void stop_audio_record()
	{
		MediaThreadManager.get_instance()._audio_recorder_idle = true;
		
		Log.v("Audio", "stop_audio_record");
	}
	
	//开启播放音频
	private void start_audio_play()
	{
		MediaThreadManager.get_instance()._audio_play_idle = false;
	}
		
	//关闭音频播放
	private void stop_audio_play()
	{
		MediaThreadManager.get_instance()._audio_play_idle = true;
	}
	
	//设置调度角色
	private void set_participant_role()
	{
	//"0"-听众非视频源，"1"-发言人 非视频源，"2"-听众兼视频源，"3"-发言人兼视频源
		Log.i("Audio","role: " + _role);
		switch(_role){
			case 0:
			case 2:
				_i_am_speaker = false;
				break;
			case 1:
			case 3:
				_i_am_speaker = true;
				break;
		}
	}
	
	private void restart_sip_service()
	{
		//new Thread()
    	{
			//@Override
			//public void run()
			{
				if(GD.is_in_schedule())
				{
					//在调度中，则关闭调度
					MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE,GD.MEDIA_INSTANCE);
				}
				//注销Sip注册
				//fym SipdroidReceiver.pos(true);
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).halt();
				SipdroidReceiver._unique_sipdroid_engine = null;
				
				
				try{Thread.sleep(200);}
				catch(InterruptedException e) {e.printStackTrace();}
				
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).isRegistered();
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).expire();
			}
    	}
    	//.start();
	}
	
	//地图消息处理句柄
	private class MapMessageHandler extends Handler
	{
		MapMessageHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what) {
				case GID.MSG_STOP_SCHEDULE: //关闭调度
					//Log.i("Message","stop schedule");
					if(GD.is_in_schedule())
					{
						if(true == _is_schedule_sponsor)
						{
							Log.v("Media", "shutdown_schedule 7");
							close_outgoing_schedule();
						}
						else
						{
							close_incoming_schedule();
						}
					}
					break;

				case GID.MSG_DESTORY: //处理界面销毁的消息
					//rmv on_destroy();//防止finish()不调用onDestroy()
					Log.v("Baidu", "MSG_DESTORY");
					//finish();
					break;
				
				case GID.MSG_RECV_VIDEO:
				case GID.MSG_SEND_VIDEO:
					//rmv reset_control_visible();
					_video_render.set_video_state();
					break;
					
				case GID.MSG_SCHEDULE_REJECTED://调度被拒绝
					if(GD.is_in_schedule())
					{
						Log.i("Baidu", "hang_up");
						Log.v("Media", "shutdown_schedule 9");
						close_outgoing_schedule();
					}
					Toast.makeText(GD.get_global_context(), "并发调度数已到达上限，请稍候再试。", Toast.LENGTH_LONG).show();
					break;
					
				case GID.MSG_GET_SELF_LOCATION_SUCCESS: //获取用户位置信息成功
					//rv after_self_location(true);
					break;
					
				case GID.MSG_GET_SELF_LOCATION_FAIL:
					//rmv after_self_location(false);
					break;

				case GID.MSG_SELECTED_VIDEO_SOURCE_CHANGE: //视频源改变的消息
					/*int video_source_index = msg.arg1;
					_video_source_imsi_dialog = (-1 == msg.arg1) ? "" : _participant_imsis.get(video_source_index);
					Log.i(GD.LOG_TAG,"video source: " + _video_source_imsi_dialog);*/
					break;
					
				case GID.MSG_SOCKET_ERROR: //socket出现异常的情况
					show_socket_error_tip(); //弹出提示对话框 
					break;
				
				case GID.MSG_INVALID_CHANGE_ROLE: //调度控制时，在没有选中其参加调度的情况下，改变其视频，发言状态
					Toast.makeText(GD.get_global_context(), "该警员未参与调度，不能指定状态", 1).show();
					break;
					
				case GID.MSG_RECV_CANCEL: //接听前收到cancel消息
					//会议服务器后台可能改变，回归空闲状态后应重置此两参数
					//GeneralDefine.SERVER_AUDIO_RECV_PORT = 30000;
					//GeneralDefine.SERVER_VIDEO_RECV_PORT = 30010;
					
					Log.v("SIP", "Cancel");
					
					//Log.v("Media", "set_schedule_state IDLE 1");
					GD.set_schedule_state(GD.SCHEDULE_STATE.idle);//GeneralDefine.setConferenceOff();
					//关闭RTSP连接
					//Log.v("Call", "close rtsp session");
					//RTSPClient.get_instance().disconnect();
					//RTSPClient.get_instance().stop();
					
					//if(false == _handle_cancel_msg)
					{
						//hide_image_and_sos();
						//rmv layout_control_by_flag(LAYOUT_IDLE);
						//_handle_cancel_msg = true;
						
						MessageHandlerManager.get_instance().unregister(GID.MSG_RECV_CANCEL,GD.MEDIA_INSTANCE);
					}
					if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_HANG_UP, (String)(msg.obj));
					break;
					
				case GID.MSG_NOTIFICATION://浮动于地图上的Toast
					if(GD.is_in_schedule())
					{
						//show_toast((String)msg.obj);
					}
					break;
					
				case GID.MSG_UPDATE_NETWORK_STATUS://网络状态更新时
					_network_status = msg.arg1;
					//Log.v("Temp", "NetworkStatus " + _network_status);
					//update_system_tips(_network_status, msg.arg2);					
					break;
					
				case GID.MSG_UPDATE_SYSTEM_TIPS://强制更新系统提示
					//update_system_tips(_network_status, 0);
					break;
						
				case GID.MSG_CHANGE_ROLE: //调度角色发生了变化
					if(GD.is_in_schedule())
					{
						if(msg.arg1 != _role)
						{
							_role = msg.arg1;
							set_participant_role();
						}
					}
					break;
						
				case GID.MSG_REFRESH_MAP://刷新地图数据
					break;
					
				case GID.MSG_SHOW_VIDEO://显示视频源
					//rmv refresh_video_source_on_map();
					break;
						
				case GID.MSG_REFRESH_STATISTIC: //刷新地图统计数据
					//Log.v("Baidu", "refresh_statistic_information");
					//rmv refresh_statistic_information();
					break;
						
				case GID.MSG_FORCE_EXIT:
					Toast.makeText(GD.get_global_context(), "根据服务器配置，本机强制下线!", Toast.LENGTH_LONG).show();
					start_exit_task();
					break;
					
				case GID.MSG_HANG_UP: //收到BYE信令后界面跳转
					Log.v("SIP", "MSG_HANG_UP " + GD.get_scheduel_state() + ", " + GD._conference_id);
					//if(GD.is_in_schedule())
					if(GD.is_in_schedule() && false == _is_schedule_sponsor)
					{
						Log.v("SIP", "hang_up");
						Log.v("Media", "shutdown_schedule 8");
						close_outgoing_schedule();
						
						if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_HANG_UP, (String)(msg.obj));
					}
					break;
					
				case GID.MSG_INCOMING_CALL://收到调度呼叫
					{
						Log.v("Baidu", "incoming call " + (String)(msg.obj));
						
						if(GD.is_in_schedule())
						{
							Log.v("Media", "shutdown_schedule 3");
							close_outgoing_schedule();
						}
						
						GD.set_schedule_state(GD.SCHEDULE_STATE.incoming_call);
						//创建RTSP连接
						//Log.v("Call", "create rtsp session");
						//RTSPClient.get_instance().start();
						//RTSPClient.get_instance().connect();
	
						MessageHandlerManager.get_instance().register(_message_handler,GID.MSG_HANG_UP, GD.MEDIA_INSTANCE); //hang up消息
						MessageHandlerManager.get_instance().register(_message_handler,GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE); //cancel 消息
						MessageHandlerManager.get_instance().register(_message_handler,GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
					
						GD._i_am_video_source = false;
						
						_is_schedule_sponsor = false;
						
						//_handle_cancel_msg = false;
						
						if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_INCOMING_CALL, (String)(msg.obj));
					}
					break;
			}
		}
	}
	
}
