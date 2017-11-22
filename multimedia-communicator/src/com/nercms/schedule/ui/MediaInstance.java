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

//��ͼ����������ʾ��Ա�ڵ�ͼ��
public class MediaInstance
{	
	private HashMap<String, Integer> _participants_status = new HashMap<String,Integer>(); //���������Ա��״̬���Ƿ���Ӧ���Ƿ�����ƵԴ
		
	public static Handler _message_handler = null; // ������Ϣ������
	
	private boolean _is_schedule_sponsor = false;//�Ƿ��ǵ��ȵķ�����
	
	//private ImageView _image = null; // ����ͼƬ�ؼ�
	//private TextView _textview_sos = null; // ��ʾSOS���ı���
		
	//���ڵ��ȿ���
	private String _delete_imsis = "",_speaker_imsis = "";
	private boolean _speaker_status_changed = false;
	
	private boolean _i_am_speaker = false; //�����Ƿ�����
		
	private int _network_status = 2; //Ĭ������״̬
	
	private int _role = -1; //0-���ڷ���ƵԴ��1-������ ����ƵԴ��2-���ڼ���ƵԴ��3-�����˼���ƵԴ
	
	//private boolean _handle_cancel_msg = false; //�Ƿ���cancel��Ϣ
	
	private VideoRender _video_render = new VideoRender();
	
	//��������
  	private volatile static MediaInstance _unique_instance = null;
	public static MediaInstance instance()
	{
		// ���ʵ��,���ǲ����ھͽ���ͬ��������
		if(null == _unique_instance)
		{
			// ���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(MediaInstance.class)
			{
				//����˫�ؼ��
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
	
	//��ʼ��
	public boolean _available = false;
	public boolean api_start(Context globla_context, int video_width, int video_height, String server_ip_wan, String server_ip_lan, boolean server_in_lan, int server_port, String self_id, String encrypt_info)
	{
		//init
		GD.set_global_context(globla_context);
		
		GD.VIDEO_WIDTH = video_width;
		GD.VIDEO_HEIGHT = video_height;
		GD.VideoBit = Bitmap.createBitmap(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, Config.RGB_565);//�����ͼ�񻺴�
		
		//��ȡ��Ļ������Ļ���ֳ���
		PowerManager _pm = (PowerManager)GD.get_global_context().getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = _pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PersonalIncomingCall");
		wl.acquire();
		
		//self
		GD.set_unique_id(Long.parseLong(self_id));
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.idle);
		
		//service
		register_message();//ע���ͼ������Ϣ,������init_view֮ǰ����
		
		GD.DEFAULT_SCHEDULE_SERVER = server_ip_wan;
		GD.OVER_VPDN = server_in_lan;
		GD.SIP_SERVER_LAN_IP_IN_VPN = (false == GD.OVER_VPDN) ? server_ip_wan : server_ip_lan;//SIP������������ַ
		//SP.set(GD.get_global_context(), SP.PREF_SCHEDULE_SERVER, server_ip);
		//SP.set(GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 12580);
		GD.SERVER_NAT_PORT = 12580;
		
		GD.SIP_SERVER_PORT = server_port;
		
		//MQTT��ʼ��
		MQTT.SERVER_URL = "tcp://" + server_ip_wan + ":1883";
		MQTT.CLIENT_ID = GD.get_unique_id(GD.get_global_context()) + "";
		MQTT.SCHEDULE_SERVER_ID = "VUA";
		Log.i("MQTT", "start mqtt: " + MQTT.SERVER_URL);
		MQTT.instance().keep_alive();
		
		GD.start_sip_media_service(); //����ȫ����ɺ󣬿�����ӦSIP��ý�����
		
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
		
		//�ر�����Ƶ�����Sip����
		//GD.shutdown_sip_media_service();
		//ApplicationExitTask._activity_context = GD.get_global_context();
		new ApplicationExitTask().execute();
		
		//֪ͨ�����ڵĽ�������
		//��ApplicationExitTask��ִ�� MessageHandlerManager.get_instance().handle_message(GID.MSG_DESTORY);
				
		//�ڽ�������ʱ��ȡ��Handler��ע��
		unregister_message();
		
		//_wl.release();
		
		_available = false;
		
		//�˳���̨�߳�,�Լ����پ�̬����,�������
		System.exit(0);
	}
	
	//����
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
	
	//�Ҷ�
	public void api_shutdown_schedule()
	{
		/*if(false == _available) return;
		
		if(GD.is_in_schedule())
		{
			//�ڵ����У���رյ���
			MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE,GD.MEDIA_INSTANCE);
		}*/
		
		Log.v("Baidu", "quit_schedule");
		
		api_reject_schedule_invite();
		
		Log.v("Baidu", "quit_schedule -> shutdown_schedule");
		close_outgoing_schedule();
		
		_is_schedule_sponsor = false;
	}
	
	//���ܵ�������
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
		
		//hide_image_and_sos(); //����ͼƬ��SOS�ؼ�
		
		//find_answer_image_control(); //�ȳ�ʼ�����Ƚ��ܺ��ͼƬ�ؼ�
		
		on_start_schedule();//������ſ�ʼ�ɼ���������

		//�ӳ�1����ʾ��ƵԴ
		_message_handler.sendMessageDelayed(_message_handler.obtainMessage(GID.MSG_SHOW_VIDEO), 1000);

		//start_refresh_overlays_time_task();
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.in_schedule);//GeneralDefine.setConferenceOn();
	}
	
	//�ܾ���������
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
	
	//����
	public void api_hand_free_switch()
	{
		AudioRecvDecPlay._hands_free = !AudioRecvDecPlay._hands_free;
		GD.hands_free_volume_switch();
	}
	
	//�л�����ͷ
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
	//������غ���
	//�������
	private void start_normal_schedule(ArrayList<String> participants, String video_source_id)
	{
		Log.v("Baidu", "start new schedule");
		
		GD.set_schedule_state(GD.SCHEDULE_STATE.in_schedule);//GeneralDefine.setConferenceOn();		
			
		send_convene_message(participants, video_source_id); //�����������͵��ȵ���Ϣ
			
		_is_schedule_sponsor = true;
		
		on_start_schedule(); //�����ڵ���״̬
	}
	
	//���ͷ�����ȵ���Ϣ��������
	private void send_convene_message(ArrayList<String> participants, String video_source_id)
	{
		/*
		{
			"t":"convene",
			"l":"2",
			"id":["s1111","s2222","s3333","4444","5555"],
			"vs":"1111"//��ƵԴ
		}*/
		//String msg = "{\"t\":\"convene\","+ "\"l\":\""+ SipPreference.get_parameter(ScheduleMap.this,SipPreference.PREF_SCHEDULE_LEVEL, 0) + "\",\"id\":[";
		//String msg = "{\"t\":\"convene\","+ "\"l\":\"1\",\"id\":[";//�����е��ȼ������Ϊ1�����������ն˵��ȼ���
		String msg = "{\"t\":\"convene\"," + "\"i\":\"" + GD.get_unique_id(GD.get_global_context()) + "\"," +  "\"l\":\"1\",\"id\":[";//�����е��ȼ������Ϊ1�����������ն˵��ȼ���
		
		//�õ�message�й���id�ֶε�����--���в��������Ա��IMSI
		String ids = "";
		int size = participants.size();//_participant_imsis.size();
		int participants_num = 0;
		
		Log.v("Baidu", "schedule participants " + size);
		
		//���ȳ�ʼ����GD._participants����ֵ
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
		
		msg += ids.substring(0, ids.length() - 1);//ȥ��ĩβ�Ķ���
		msg += ("],\"vs\":\"a" + video_source_id +  "\"}");
		
		SP.set(GD.get_global_context(), SP.PREF_LAST_VIDEO_SOURCE, video_source_id);
		
		Log.i("Baidu", "convene msg: " + msg);
		
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2);
			
	}
	
	//���������ô��ڵ���״̬
	private void on_start_schedule()
	{
		Log.v("Baidu", "on_start_schedule");
		//Ĭ�Ͽ�ʼ����ʱ��������Ļ
		//_screen_locked = false;
		//_imageview_lock.setVisibility(View.GONE);
		
		//�������ٸ�Ӧ��
		//_sensor_helper.register(GD.get_global_context(), _message_handler, Sensor.TYPE_ACCELEROMETER);
		
		//init_video_view(); //��ʼ����Ƶ�ؼ��������ؼ�
		
		//���ò���
		//_video_source_imsi = "";
		GD._video_source_imsi = "";
		//_is_video_visible = true;
		
		register_schedule_message();
		
		//��״̬������ʾͼ�꣬��ʾ�û��ڵ�����
		//show_notification();
		
		//��ʾ��Ƶ�ؼ���ʱ��ؼ�
		//show_time_control();

		GD.set_conference_volume(GD.get_global_context()); // ����ͨ������
		
		//_i_am_speaker = true; //Ĭ���Ƿ�����
		_network_status = NetworkStatus.get_network_status();
		
		//��ͨ�������Ƿ����˾�������Ƶ¼�ƺͲ���ģ��
		//һ�������˸ı�ֻ�ڵ��ȷ������д���
		start_audio_record(); // ������Ƶ¼�Ƶ�ģ��
		
		start_audio_play(); //����������Ƶ���ݵ�ģ��

		_video_render.set_video_state(); //������Ƶ״̬
		
		//_schedule_answer_info = "";
	}
	
	//�رյ���
	private void close_outgoing_schedule()
	{
		Log.i("Baidu", "close schedule");
		
		//Log.v("Media", "set_schedule_state IDLE 2");
		GD.set_schedule_state(GD.SCHEDULE_STATE.idle);//GeneralDefine.setConferenceOff();
		//�ر�RTSP����
		//Log.v("Call", "close rtsp session");
		//RTSPClient.get_instance().disconnect();
		//RTSPClient.get_instance().stop();
		
		//GD.reset_volumn_change(); //�ڵ��ȹر�ʱ���޸������仯ʱ�ȽϵĻ�׼ʱ��
		
		GD.set_conference_volume(GD.get_global_context()); //����ͨ������
		
		//������Ļ
		//_screen_locked = false;
		//_imageview_lock.setVisibility(View.GONE);
		
		//�رռ��ٸ�Ӧ��
		//_sensor_helper.unregister();
		
		if(true == _is_schedule_sponsor)
		{
			Log.i("Baidu", "close schedule 2");
			//��������������Ϣ��֪ͨ���Ƚ���
			send_close_message();
			
			// �رջ���ʱ���鷢����Ҫ�ǵ����SipdroidEngine
			GD._participants.clear();
			
			GD._conference_id = "";  //��ʼ�������ID
		}

		_role = -1;
		
		//rmv _notification_manager.cancel(_notification_id);//�����ʾ�û������е�ͼ��
		
		//SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, "ע��ɹ�", R.drawable.sym_presence_available , 0); //����״̬����ͼ��Ϊע��ɹ�
		
		stop_audio_play();
		stop_audio_record();

		_video_render.stop_video_render();
		_video_render.stop_video_capture();

		unregister_schedule_message(); //ע��������йص�ע����Ϣ
		
		//��Prefreference�е�is_video_source��Ϊfalse
		GD._i_am_video_source = false;

		//�������
		//SP.set(this,SP.PREF_ANSWER_RECV_VIDEO, false);
		//SP.set(this,SP.PREF_ANSWER_SEND_VIDEO, false);
		
		unregister_schedule_message(); //ע��������йص�ע����Ϣ

		//���ò���
		GD._i_am_video_source = false;
		//_video_source_imsi = "";
		GD._video_source_imsi = "";
		//_handle_receive_video_msg = false;
		
		_participants_status.clear(); //���״̬
		
		//�ظ�Ĭ������״̬
		AudioRecvDecPlay._hands_free = GD.DEFAULT_HANDS_FREE;//_is_handfree_on = false;
		GD.hands_free_volume_switch();//(ScheduleMap.this, _is_handfree_on);
		
		//�ر���ͣ
		GD.MEDIA_PAUSE_IN_SCHEDULE = false;
		
		restart_sip_service();
	}
	
	//��������������Ϣ��֪ͨ���Ƚ���
	private void send_close_message()
	{
		String msg = "{\"t\":\"close\",\"c\":\"" + GD._conference_id + "\"}";
		Log.i("Baidu", "close conference: " + msg);
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);		
		else MQTT.instance().publish_message_to_server(msg, 2);

	}
		
	//ע���ͼ������Ϣ
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
	
	//ע����ͼ������Ϣ
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
	
	//socketδ�����ɹ�ʱ����ʾ��Ϣ
	void show_socket_error_tip()
	{
		new AlertDialog.Builder(GD.get_global_context()).setTitle("��ʾ!").setMessage("�����쳣������������")
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
		//�˳�
		start_exit_task();
		
		//�����µ���
		//start_normal_schedule();//_message_handler.sendEmptyMessage(GID.MSG_NEW_SCHEDULE);
		
		//���ȿ���
		//check_participant_status();
		
		//������Ϣ��������״̬
		//_message_handler.sendEmptyMessageDelayed(GID.MSG_REFRESH_MAP, 500);
		
		//�ر����е���
		close_outgoing_schedule();
		
		//����
		AudioRecvDecPlay._hands_free = !AudioRecvDecPlay._hands_free;
		GD.hands_free_volume_switch();
		
		//����
		api_accept_schedule_invite();
		
		//�ܾ�
		api_reject_schedule_invite();
	
		//�رձ��е���
		close_incoming_schedule();
		
		//���涨λ��������ip����
		SP.set(GD.get_global_context(), SP.PREF_LOCATION_SERVER, "xxx.xxx.xxx.xxx");
		GD.change_register_time(GD.get_global_context());
	}
	
	private void start_exit_task()
	{
		//ApplicationExitTask._activity_context = GD.get_global_context();
		//new ApplicationExitTask().execute();
	}

	//�˳�����
	private void close_incoming_schedule()
	{
		Log.v("Baidu", "hang up");
		
		api_reject_schedule_invite();
		
		Log.v("Baidu", "shutdown_schedule 6");
		close_outgoing_schedule();
	}
	
	//ɾ����Ա����Ϣ
	private void send_delete_message()
	{
		_delete_imsis = _delete_imsis.substring(0, _delete_imsis.length()-1);
		
		//���ɾ����Ա���Ƿ���������ƵԴ
		/*if(_delete_imsis.contains(_video_source_imsi_dialog))
		{
			_video_source_imsi_dialog = "";
		}*/
		
		/**
		 * { "t":"remove", "c":"1111",
		 * "id":["a3333","a4444","a5555"]//���ֶθ�ʽ��2.3.1.1 }
		 */
		String msg = "{\"t\":\"remove\",\"c\":\"" + GD._conference_id + "\",\"id\":[" + _delete_imsis + "]}";
		Log.i(GD.LOG_TAG,"delete msg: " + msg);
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2); 
	}
	
	//�����˸ı����Ϣ
	private void send_speaker_change_message(){
		_speaker_imsis += ("\"a" + GD.get_unique_id(GD.get_global_context()) + "\"");
		
		String msg = "{\"t\":\"speaker\",\"c\":\"" + GD._conference_id + "\",\"id\":[";
	
		//�ı䷢����ʱ��һ��Ҫ���Լ���id����
		msg += _speaker_imsis;
		msg += "]}";
		
		Log.i(GD.LOG_TAG,"speaker msg: " + msg);
		
		if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
		else MQTT.instance().publish_message_to_server(msg, 2);
	}
	
	//�Ƿ�����Ա��ӣ������������������Ϣ
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
	
	//��ⷢ�����Ƿ�ı䡢�Ƿ�����Ա��ӡ���ƵԴ�Ƿ�ı�
	private void check_others()
	{
		if(_speaker_status_changed)
		{
			send_speaker_change_message();
		}
		//����Ƿ�����Ա���
		check_have_participant_add();
		
		//�����ƵԴ�Ƿ��иı�
		check_video_source_changed();
	}
	
	//��ƵԴ�Ƿ��иı�
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

	//ע��������йص���Ϣ
	private void register_schedule_message()
	{
		Log.v("Baidu", "message handler 2: " + _message_handler);
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_SEND_VIDEO, GD.MEDIA_INSTANCE); //������Ƶ
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RECV_VIDEO, GD.MEDIA_INSTANCE); //������Ƶ
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_CHANGE_ROLE, GD.MEDIA_INSTANCE); //��ɫ�ı�
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_STOP_SCHEDULE, GD.MEDIA_INSTANCE); //�ر�SOS
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RTSP_SESSION_RECONNECT, GD.MEDIA_INSTANCE); //RTSP�Ự����
		
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE);
		MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
		
		//MessageHandlerManager.get_instance().register(_message_handler, GID.MSG_INCOMING_CALL, GD.MEDIA_INSTANCE);
	}
	
	//ע��������йص���Ϣ
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
	
	/**����Ƶ�йصĺ���
	 * */
	// ������Ƶ¼�Ƶ�ģ��
	private void start_audio_record()
	{
		// false ������������Ƶ
		 if (false == SP.get(GD.get_global_context(),SP.PREF_AUDIO_SEND_ON, true))
		 {
			 return;
		 }

		MediaThreadManager.get_instance()._audio_recorder_idle = false;
		
		Log.v("Audio", "start_audio_record");		
	}
	
	//�ر���Ƶ¼�Ƶ�ģ��
	private void stop_audio_record()
	{
		MediaThreadManager.get_instance()._audio_recorder_idle = true;
		
		Log.v("Audio", "stop_audio_record");
	}
	
	//����������Ƶ
	private void start_audio_play()
	{
		MediaThreadManager.get_instance()._audio_play_idle = false;
	}
		
	//�ر���Ƶ����
	private void stop_audio_play()
	{
		MediaThreadManager.get_instance()._audio_play_idle = true;
	}
	
	//���õ��Ƚ�ɫ
	private void set_participant_role()
	{
	//"0"-���ڷ���ƵԴ��"1"-������ ����ƵԴ��"2"-���ڼ���ƵԴ��"3"-�����˼���ƵԴ
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
					//�ڵ����У���رյ���
					MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE,GD.MEDIA_INSTANCE);
				}
				//ע��Sipע��
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
	
	//��ͼ��Ϣ������
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
				case GID.MSG_STOP_SCHEDULE: //�رյ���
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

				case GID.MSG_DESTORY: //����������ٵ���Ϣ
					//rmv on_destroy();//��ֹfinish()������onDestroy()
					Log.v("Baidu", "MSG_DESTORY");
					//finish();
					break;
				
				case GID.MSG_RECV_VIDEO:
				case GID.MSG_SEND_VIDEO:
					//rmv reset_control_visible();
					_video_render.set_video_state();
					break;
					
				case GID.MSG_SCHEDULE_REJECTED://���ȱ��ܾ�
					if(GD.is_in_schedule())
					{
						Log.i("Baidu", "hang_up");
						Log.v("Media", "shutdown_schedule 9");
						close_outgoing_schedule();
					}
					Toast.makeText(GD.get_global_context(), "�����������ѵ������ޣ����Ժ����ԡ�", Toast.LENGTH_LONG).show();
					break;
					
				case GID.MSG_GET_SELF_LOCATION_SUCCESS: //��ȡ�û�λ����Ϣ�ɹ�
					//rv after_self_location(true);
					break;
					
				case GID.MSG_GET_SELF_LOCATION_FAIL:
					//rmv after_self_location(false);
					break;

				case GID.MSG_SELECTED_VIDEO_SOURCE_CHANGE: //��ƵԴ�ı����Ϣ
					/*int video_source_index = msg.arg1;
					_video_source_imsi_dialog = (-1 == msg.arg1) ? "" : _participant_imsis.get(video_source_index);
					Log.i(GD.LOG_TAG,"video source: " + _video_source_imsi_dialog);*/
					break;
					
				case GID.MSG_SOCKET_ERROR: //socket�����쳣�����
					show_socket_error_tip(); //������ʾ�Ի��� 
					break;
				
				case GID.MSG_INVALID_CHANGE_ROLE: //���ȿ���ʱ����û��ѡ����μӵ��ȵ�����£��ı�����Ƶ������״̬
					Toast.makeText(GD.get_global_context(), "�þ�Աδ������ȣ�����ָ��״̬", 1).show();
					break;
					
				case GID.MSG_RECV_CANCEL: //����ǰ�յ�cancel��Ϣ
					//�����������̨���ܸı䣬�ع����״̬��Ӧ���ô�������
					//GeneralDefine.SERVER_AUDIO_RECV_PORT = 30000;
					//GeneralDefine.SERVER_VIDEO_RECV_PORT = 30010;
					
					Log.v("SIP", "Cancel");
					
					//Log.v("Media", "set_schedule_state IDLE 1");
					GD.set_schedule_state(GD.SCHEDULE_STATE.idle);//GeneralDefine.setConferenceOff();
					//�ر�RTSP����
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
					
				case GID.MSG_NOTIFICATION://�����ڵ�ͼ�ϵ�Toast
					if(GD.is_in_schedule())
					{
						//show_toast((String)msg.obj);
					}
					break;
					
				case GID.MSG_UPDATE_NETWORK_STATUS://����״̬����ʱ
					_network_status = msg.arg1;
					//Log.v("Temp", "NetworkStatus " + _network_status);
					//update_system_tips(_network_status, msg.arg2);					
					break;
					
				case GID.MSG_UPDATE_SYSTEM_TIPS://ǿ�Ƹ���ϵͳ��ʾ
					//update_system_tips(_network_status, 0);
					break;
						
				case GID.MSG_CHANGE_ROLE: //���Ƚ�ɫ�����˱仯
					if(GD.is_in_schedule())
					{
						if(msg.arg1 != _role)
						{
							_role = msg.arg1;
							set_participant_role();
						}
					}
					break;
						
				case GID.MSG_REFRESH_MAP://ˢ�µ�ͼ����
					break;
					
				case GID.MSG_SHOW_VIDEO://��ʾ��ƵԴ
					//rmv refresh_video_source_on_map();
					break;
						
				case GID.MSG_REFRESH_STATISTIC: //ˢ�µ�ͼͳ������
					//Log.v("Baidu", "refresh_statistic_information");
					//rmv refresh_statistic_information();
					break;
						
				case GID.MSG_FORCE_EXIT:
					Toast.makeText(GD.get_global_context(), "���ݷ��������ã�����ǿ������!", Toast.LENGTH_LONG).show();
					start_exit_task();
					break;
					
				case GID.MSG_HANG_UP: //�յ�BYE����������ת
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
					
				case GID.MSG_INCOMING_CALL://�յ����Ⱥ���
					{
						Log.v("Baidu", "incoming call " + (String)(msg.obj));
						
						if(GD.is_in_schedule())
						{
							Log.v("Media", "shutdown_schedule 3");
							close_outgoing_schedule();
						}
						
						GD.set_schedule_state(GD.SCHEDULE_STATE.incoming_call);
						//����RTSP����
						//Log.v("Call", "create rtsp session");
						//RTSPClient.get_instance().start();
						//RTSPClient.get_instance().connect();
	
						MessageHandlerManager.get_instance().register(_message_handler,GID.MSG_HANG_UP, GD.MEDIA_INSTANCE); //hang up��Ϣ
						MessageHandlerManager.get_instance().register(_message_handler,GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE); //cancel ��Ϣ
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
