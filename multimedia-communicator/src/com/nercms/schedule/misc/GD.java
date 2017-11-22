package com.nercms.schedule.misc;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.android.internal.telephony.ITelephony;
import com.nercms.schedule.R;
import com.nercms.schedule.audio.AudioRecvDecPlay;
import com.nercms.schedule.network.NetworkStatus;
import com.nercms.schedule.rtsp.RTSPClientThread;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.OnReRegister;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.stack.net.IpAddress;
import com.nercms.schedule.ui.MessageHandlerManager;
import com.nercms.schedule.ui.OnMsgCallback;
//import com.nercms.schedule.ui.ScheduleMap;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class GD
{
	public static long _start_timestamp = 0;//程序最近一次启动时间，单位毫秒
	
	//AEC
	public enum AECType
    {
		NONE,
		AEC,
		AECM
    }
	public static AECType AEC_TYPE = AECType.NONE;
	public static short AEC_DELAY = 200;
	public static long AEC_READ_BLOCK = 0;
	public static long AEC_WRITE_BLOCK = 0;
	
	public static boolean MQTT_ON = true;
	
	//public static String AUDIO_CODEC="ilbc-webrtc";
	public static String AUDIO_CODEC="ilbc";
	//public static String AUDIO_CODEC="amr-nb";
	//public static String AUDIO_CODEC="amr-wbp";
	
	public static boolean USE_AES = false;//false;//true;
	
	public static boolean REDUNDANT_AUDIO_SEND = false;//true;//true-仅UDP冗余发送音频包(形如12, 23...), false-TCP/UDP双路非冗余发包
	public static boolean TCP_AUDIO_RECV = false;
	
	//WCDMA
	public static final int UA_LINES = 2;//2;//1--SIP及Audio只支持TCP，2--SIP及Audio同时支持TCP及UDP
	public static boolean OVER_VPDN = false;
	//public static String SIP_PROTOCOL = "tcp";
	//public static String AUDIO_PROTOCOL = "tcp";
	public static String VIDEO_PROTOCOL = "udp";	
	public static int LOCATION_SERVER_PORT = 8080;//定位服务器端口
	public static int SIP_SERVER_PORT = 5060;//SIP服务器端口
	//public static String DEFAULT_SCHEDULE_SERVER = "220.249.112.22";//SIP服务器公网地址
	public static String DEFAULT_SCHEDULE_SERVER = "120.26.78.7";//SIP服务器公网地址
	public static int  SERVER_NAT_PORT = 12580;
	//public static String DEFAULT_SCHEDULE_SERVER = "121.40.214.71";//SIP服务器公网地址（国安）
	public static String SIP_SERVER_LAN_IP_IN_VPN = DEFAULT_SCHEDULE_SERVER;//SIP服务器内网地址
	public static final int SHORT_LOCATE_INTERVAL = 15000;//短定位时间间隔3s
	//public static final int LONG_LOCATE_INTERVAL = 30000;//长定位时间间隔60s
	public static int RTSP_SERVER_PORT = 1554;//RTSP服务器端口
	public static boolean MEDIA_STATISTICS = true;
	public static boolean THREAD_STATISTICS = false;
	public static boolean DEFAULT_HANDS_FREE = false;
	public static int DEFAULT_CAMERA = 0;//0-后置，1-前置
	public static long CHECK_VERSION_INTERVAL = 120000L;//检查版本更新间隔，单位毫秒
	public static final int VIDEO_SAMPLE_INTERVAL = 67;//100;//67;//125;//200;//200;//200;//333;//视频采样编码最小间隔(ms)
	public static final long RTSP_RECONNECT_INTERVAL = 8000;//因未收到音视频RTSP重连前等待时长，单位毫秒
	public static boolean TOLERANCE_FOR_VIDEO_MOSAIC = true;//是否容忍视频马赛克
	public static int VIDEO_WIDTH = 352;//1280;//352;
	public static int VIDEO_HEIGHT = 288;//720;//288;
	
	public static boolean sip_audio_over_udp()
	{
		return (1 < UA_LINES);
	}
	
	//以TCP方式传输音视频数据时
	public static int RTSP_SEND_BUFFER_SIZE = 10240;//8192;//4096;//2048;
	public static int RTSP_RECV_BUFFER_SIZE = 5120;//4096;//2048;
	
	//RTSP socketchannel超时时间（毫秒）
	public static int RTSP_SOCKET_CHANNEL_TIME_OUT = 20;//ms
	
	//以UDP方式音视频数据时
	public static int NIO_UDP_AUDIO_RECV_BUFFER_SIZE = 1536;
	public static int NIO_UDP_VIDEO_RECV_BUFFER_SIZE = 10240;
	
	public static int UDP_AUDIO_RECV_SOCKET_TIME_OUT = 200;//20;//20;//ms
	public static int UDP_VIDEO_RECV_SOCKET_TIME_OUT = 200;//20;//ms
	
	public static boolean FAKE_COMMUNICATION_WITH_LOCATION_SERVER = false;
	public static boolean NO_RENDERING_DECODE_VIDEO = false;
	
	//与地图服务器的连接方式
	public static String LOCATION_SERVER_INTERFACE = "http";
	//public static String LOCATION_SERVER_INTERFACE = "webservice";
	
	//服务器参数
	public static int SERVER_AUDIO_RECV_PORT = 30000;//音频服务器的接收端口初始值
	public static int SERVER_VIDEO_RECV_PORT = 30010;//视频服务器的接收端口初始值
	
	//向调度服务器发送NAT探测包的时间间隔（毫秒）
	public static long SCHEDULE_REGISTER_INTERVAL = 20000;
	
	//向地图服务器注册的时间间隔（毫秒）
	public static final long LOCATION_REGISTER_INTERVAL = 30000;
	
	//更新下属资源的最大时间间隔（毫秒）
	public static final long MAX_UPDATE_RESOURCE_POSITION_INTERVAL = 5000;
	
	//上传本机GPS数据的最大时间间隔（毫秒）
	public static final long MAX_UPLOAD_GPS_DATA_INTERVAL = 15000;
	
	//与服务器时间超过该阈值（秒）即提醒用户调整本机时间
	public static final long MAX_TIME_DIFF_BETWEEN_SERVER_AND_LOCAL = 180;
	
	//当前页面是否为地图页面
	public static boolean _is_rendering_map = true;
	
	//每隔1小时删除一次GPS数据（毫秒）
	public static final long GPS_DATA_DELETE_INTERVAL = 3600000;
	
	//每次最多上传50条记录
	public static final int GPS_UPLOAD_MAX_COUNT_ONCE = 30;//50;
	
	public static String LOG_TAG = "Schedule";
	
	//private static boolean _control_available = true; //标识在调度的情况下，控件是否可用
	
	public static final String SCHEDULE = "com.nercms.schedule.schedule"; //作为调度广播的Action
	
	//最近一次SIP注册成功的时戳
	public static long _latest_sip_register_success = 0;
	
	private static long _schedule_timestamp = 0;//参与调度时戳,如在空闲状态时该时戳为0
	private static long _rtsp_session_start_timestamp = 0;//参与调度后RTSP会话建立成功的时戳
	private static long _rtsp_session_complete_timestamp = 0;//参与调度后RTSP会话建立成功的时戳
	private static long _recv_audio_timestamp = 0;//参与调度后收到第一个音频包的时戳
	private static long _play_audio_timestamp = 0;//参与调度后播放第一个音频包的时戳
	private static long _recv_video_timestamp = 0;//参与调度后收到第一个视频包的时戳
	private static long _render_video_timestamp = 0;//参与调度后播放第一个视频包的时戳
	
	public static float _video_render_scale = 1.0f;
	
	//记录调度后首次收到音频包的时戳
	public static void update_recv_audio_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
			return;
		
		if(0 == GD._recv_audio_timestamp)
		{
			GD._recv_audio_timestamp = System.currentTimeMillis();
		}
	}
	
	//记录调度后首次收到视频包的时戳
	public static void update_recv_video_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
			return;
		
		if(0 == GD._recv_video_timestamp)
		{
			GD._recv_video_timestamp = System.currentTimeMillis();
		}
	}
	
	//记录调度后首次播放音频包的时戳
	public static void update_play_audio_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
			return;
		
		if(0 == GD._play_audio_timestamp)
		{
			GD._play_audio_timestamp = System.currentTimeMillis();
			
			//Log.v("Video", "Play Audio " + (GD._recv_audio_timestamp - GD._schedule_timestamp) + ", " + (GD._play_audio_timestamp - GD._schedule_timestamp));
			Log.v("Video", "Play Audio " + (GD._recv_audio_timestamp - GD._rtsp_session_complete_timestamp) + ", " + (GD._play_audio_timestamp - GD._rtsp_session_complete_timestamp));
		}
	}
		
	//记录调度后首次播放视频包的时戳
	public static void update_render_video_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
		{
			//Log.v("Video", "preliminary render.");
			return;
		}
		
		if(0 == GD._render_video_timestamp)
		{
			GD._render_video_timestamp = System.currentTimeMillis();
			
			//Log.v("Video", "Render Video " + (GD._recv_video_timestamp - GD._schedule_timestamp) + ", " + (GD._render_video_timestamp - GD._schedule_timestamp));
			Log.v("Video", "Render Video " + (GD._recv_video_timestamp - GD._rtsp_session_complete_timestamp) + ", " + (GD._render_video_timestamp - GD._rtsp_session_complete_timestamp));
		}
	}
	
	public static void update_rtsp_session_start_timestamp()
	{
		_rtsp_session_start_timestamp = System.currentTimeMillis();
		
		Log.v("Video", "Start RTSP Session " + (GD._rtsp_session_start_timestamp - GD._schedule_timestamp));
	}
	
	public static void update_rtsp_session_complete_timestamp()
	{
		_rtsp_session_complete_timestamp = System.currentTimeMillis();
		
		//避免出现刚开始RTSP会话即重连的情况
        GD._latest_rtsp_video_data_timestamp = System.currentTimeMillis();
        GD._latest_rtsp_audio_data_timestamp = System.currentTimeMillis();
	}
	
	//调度状态
	public static enum SCHEDULE_STATE
	{
		idle,//空闲中
		incoming_call,//收到呼叫还未接听
		in_schedule,//调度中，即已发起调度或已接听呼叫
		closing,//关闭程序中
	}	
	private static SCHEDULE_STATE _schedule_state;
		
	public static void set_schedule_state(SCHEDULE_STATE state)
	{
		//Log.v("Media", "set_schedule_state " + state);
		
		_schedule_state = state;
		
		switch(state)
		{
			case idle:
			case incoming_call:
			case closing:
				_schedule_timestamp = 0;				
				break;
				
			case in_schedule:
				_schedule_timestamp = System.currentTimeMillis();
				break;
		}
		
		_recv_audio_timestamp = 0;
		_play_audio_timestamp = 0;
		_recv_video_timestamp = 0;
		_render_video_timestamp = 0;
		
		//令迅速启动或关闭RTSP连接
		synchronized(RTSPClientThread._event)
		{
			RTSPClientThread._event.notify();
		}
		
		MessageHandlerManager.get_instance().handle_message(GID.MSG_UPDATE_SYSTEM_TIPS, GD.MEDIA_INSTANCE);
	}
		
	public static SCHEDULE_STATE get_scheduel_state()
	{
		return _schedule_state;
	}
		
	//是否在调度中
	public static boolean is_in_schedule()
	{
		return (_schedule_state == SCHEDULE_STATE.in_schedule);
	}
	
	public static String _conference_id = "";// 目前所参与会议的ID
	
	public static HashMap<String, Participant> _participants = new HashMap<String, Participant>();;// 目前所参与会议的成员<成员ID，成员对象>
	public static Object _participants_lock = new Object();
	
	public static final int MAX_SPEAKER_NUM = 4; //最大发言人数目 
	
	public static boolean MEDIA_PAUSE_IN_SCHEDULE = false;//调度中是否暂停音视频传输
	
	/** 类名 */
	public static final String MEDIA_INSTANCE="MediaInstance";
	
	 /** Network type is unknown */
	public static final int NETWORK_TYPE_UNKNOWN = 0;
   /** Current network is GPRS */
	public static final int NETWORK_TYPE_GPRS = 1;
   /** Current network is EDGE */
	public static final int NETWORK_TYPE_EDGE = 2;
   /** Current network is UMTS */
	public static final int NETWORK_TYPE_UMTS = 3;
   /** Current network is CDMA: Either IS95A or IS95B*/
	public static final int NETWORK_TYPE_CDMA = 4;
   /** Current network is EVDO revision 0*/
	public static final int NETWORK_TYPE_EVDO_0 = 5;
   /** Current network is EVDO revision A*/
	public static final int NETWORK_TYPE_EVDO_A = 6;
   /** Current network is 1xRTT*/
	public static final int NETWORK_TYPE_1xRTT = 7;
   /** Current network is HSDPA */
	public static final int NETWORK_TYPE_HSDPA = 8;
   /** Current network is HSUPA */
	public static final int NETWORK_TYPE_HSUPA = 9;
   /** Current network is HSPA */
	public static final int NETWORK_TYPE_HSPA = 10;
   /** Current network is iDen */
	public static final int NETWORK_TYPE_IDEN = 11;
	 
	public static final int IDLE = 0; //待机状态
	public static final int TALK = 1; //通话状态
	public static final int VIDEO = 2; //发送视频状态
	
	private static Context _global_context = null; //全局的上下文变量
	public static OnMsgCallback _msg_callback = null;
	
	private static int _business_type = 0; //1:表示资源发现 2:表示统一通信  3：表示智能调度
	
	public static boolean _i_am_video_source = false; //本人是否视频源
	public static String _video_source_imsi = ""; //视频源的imsi
	
	public static int VIDEO_ENCODE_STRATEGY = -1; //视频帧类型索引（-1：自动  0：编P帧      1：编IDR帧       2：编I帧）
	public static int ONLY_IDR_FRAME_INTERVAL = 2000; //只编IDR帧时相邻两IDR帧的最小间隔
	public static int MAX_VIDEO_PACKET_SIZE = 800;  //视频数据包的最大尺寸	
	//public static final int VIDEO_SAMPLE_INTERVAL = 333;//333;//视频采样编码最小间隔(ms)
	
	public static long _last_shut_schedule_time = 0L; // 上次关闭调度的时间
	
	public static final int START_GPS = 1; //开启GPS的requestCode
	
	//视频
	public static ByteBuffer _video_render_buffer = null;//解码后码流缓冲区
	public static boolean video_is_coming = false;//本次调度中是否收到视频数据
	public static Bitmap VideoBit = null;//Bitmap.createBitmap(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, Config.RGB_565);//解码后图像缓存
	public static Object _video_lock = new Object();
	
	public static long _latest_rtsp_video_data_timestamp = 0;//视频会话最近一次收到RTSP服务器发送的数据的时戳
	public static long _latest_rtsp_audio_data_timestamp = 0;//音频会话最近一次收到RTSP服务器发送的数据的时戳
	
	//设置全局的上下文变量
	public static void set_global_context(Context context)
	{
		_global_context = context;
	}
	
	//获取全局的上下文变量
	public static Context get_global_context()
	{
		return _global_context;//以Description页面的Context作为全局Context
	}
	
	public static boolean is_numeric(String str)
	{
		Pattern pattern = Pattern.compile("[0-9]*");
		return pattern.matcher(str).matches();
	}
	
	//获取本机IMSI
	private static long _local_imsi = 0;// 根据IMSI计算出的ID号
	public static void set_unique_id(long id)
	{
		_local_imsi = id;
	}
	
	public static synchronized long get_unique_id(Context context)
	{
		if (0 != _local_imsi)
			return _local_imsi;		

		return _local_imsi;
	}
	
	//////////////////////////////////////////////////////	
	/*private static long _last_volumn_change_timestamp = 0;//上次音量变化的时间
	private static long _continued_volumn_change_time = 0;//音量连续变化（按键未松开）时长
	private static boolean _release_volumn_button_after_trigger = true;//上次音量变化事件触发后是否松开了音量键
	private static final int UNAVAILABLE_VOLUMN_CHANGE_INTERVAL = 1000;//两次音量变化超过此间隔即认为按键在此期间曾松开
	private static final int AVAILABLE_VOLUMN_CHANGE_INTERVAL = 2000;//音量连续变化时长超过此间隔则启动SOS呼叫
	public static void on_volumn_change()
	{
		//Log.v("Temp", "volumn change.");
		
		//if(true) return;
			
		long current = System.currentTimeMillis();
		
		if(UNAVAILABLE_VOLUMN_CHANGE_INTERVAL < (current - _last_volumn_change_timestamp))
		{
			reset_volumn_change();
			return;//上次音量变化至今音量按键曾抬起过
		}
			
		//上次音量变化至今音量按键未曾抬起
		_continued_volumn_change_time += current - _last_volumn_change_timestamp;
		_last_volumn_change_timestamp = current;
			
		if(AVAILABLE_VOLUMN_CHANGE_INTERVAL <= _continued_volumn_change_time && true == _release_volumn_button_after_trigger)
		{
			Log.v("Temp", "volumn change trigger.");
			
			if(GD.is_in_schedule())
			{
				//在调度中，关闭SOS调度
				Log.i(GD.LOG_TAG,"shut sos");
				//rmv MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE, GD.SCHEDULEMAP);
			}
			else if(false == NetworkStatus._is_restarting_media_and_sip_service)
			{
				//通过SOS服务发起SOS调度
				Log.i(GD.LOG_TAG,"start sos");
				Intent sosintent = new Intent();
				sosintent.setComponent(new ComponentName("com.nercms.schedule", "com.nercms.schedule.ui.SOSService"));
				GD.get_global_context().startService(sosintent);
			}
			
			_release_volumn_button_after_trigger = false;
		}
	}
		
	public static void reset_volumn_change()
	{
		Log.v("Temp", "restart volumn change.");
		
		_last_volumn_change_timestamp = System.currentTimeMillis();
		_continued_volumn_change_time = 0;
		_release_volumn_button_after_trigger = true;
	}*/
	
	//将日志写入数据库
	/*public static void log_to_db(Context context, int type, String tag, String content)
	{
		if(tag.equals("Statistics"))
		{
			Log.v("Network", content);
		}
		
		if(true) return;
	}*/
	
	//字符串加密
	public static String encrypt_string(String data, String algorithm)
	{
		if (0 != data.length()) {
			try {
				MessageDigest message_digest = MessageDigest
						.getInstance(algorithm);
				message_digest.update(data.getBytes());

				byte[] byteArray = message_digest.digest();

				StringBuffer md5StrBuff = new StringBuffer();

				for (int i = 0; i < byteArray.length; i++) {
					if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
						md5StrBuff.append("0").append(
								Integer.toHexString(0xFF & byteArray[i]));
					else
						md5StrBuff.append(Integer
								.toHexString(0xFF & byteArray[i]));
				}

				return md5StrBuff.toString().toUpperCase();

			} catch (NoSuchAlgorithmException e) {
				Log.v(LOG_TAG, "encrypt error: " + e.toString());
			}

		}

		return data;
	}
	
	//挂断正在进行的语音电话
	public static void hang_up_phone_call(Context context)
	{
		/*try
		{
			TelephonyManager telephony_manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			ITelephony telephony = null;
			
			Class<TelephonyManager> telephony_class = TelephonyManager.class;
			Method method = telephony_class.getDeclaredMethod("getITelephony", (Class[]) null);
			method.setAccessible(true);
			telephony = (ITelephony)method.invoke(telephony_manager, (Object[]) null);
			
			telephony.endCall();

		}
		catch (Exception e)
		{
			Log.v(LOG_TAG, e.toString());
		}*/
	}

	// 设置通话音量
	public static void set_conference_volume(Context context)
	{
		AudioManager audio_manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		audio_manager.setMode(AudioManager.MODE_NORMAL);
		// 获取最大媒体音量值
		int max_media_volum = audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int max_call_volum = audio_manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

		// 将媒体音量调至3/5
		audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, max_media_volum/* * 3 / 5*/, 0);
		// 将通话音量（麦克风音量）调至最大
		audio_manager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, max_call_volum/* * 4 / 5*/, 0);
	}
	
	//先修改AMRPlay._hands_free再调用此函数
	public static void hands_free_volume_switch()
	{
		if(10 < android.os.Build.VERSION.SDK_INT)//Android 4.0以上通过AudioManager控制免提无效
		{
			//AMRPlay._hands_free = is_hands_free;//for Android 4.0 or higher version
		}
		else
		{
			AudioManager audio_manager = (AudioManager)get_global_context().getSystemService(Context.AUDIO_SERVICE);
			audio_manager.setMode(AudioManager.MODE_NORMAL);		
			//audio_manager.setMode(AudioManager.MODE_IN_CALL);
			audio_manager.setSpeakerphoneOn(AudioRecvDecPlay._hands_free);
		}
	}

	// 是否允许锁频切换，enable为true则运行
	public static void enable_lock_screen(Context context, boolean enable)
	{
		android.provider.Settings.System.putInt(context.getContentResolver(),
												android.provider.Settings.System.LOCK_PATTERN_ENABLED,
												(true == enable) ? 1 : 0);
	}
	
	/*终端所采集的GPS数据包括：
	1）经度：浮点，形如114.35407，整数部分不超过140，小数点后5位，不超过0.99999
	2）纬度：浮点，形如30.53536, 整数部分不超过60，小数点后5位，不超过0.99999
	3）海拔：整形，形如800，不超过20000，单位米
	4）精度：整形，形如50， 不超过1000，单位米
	5）速度：整形，形如13，不超过63，单位米/秒
	6）时戳，整形，即采集该数据时距1970年1月1日0分0秒的秒数，形如1294367860，不超过3000000000
	7）采集方式：GPS或AGPS方式

	GPS数据压缩时采用64进制，即0～63依次由0～9，a～z（即小写英文字母，但不包括l和o），A～Z（即大写英文字母，但不包括I和O），$，%，*，#，&，@共64个字符表示，一条GPS数据共个20字符，依次为：
	1）第1～4个字符，表示经度乘100000得到整数值，值域0～14099999；
	2）第5～8个字符，表示纬度乘100000得到整数值，值域0～130999999，超过小于7000000则为GPS方式，否则为AGPS方式，此时减去7000000则为实际纬度值；
	3）第9～11个字符，表示海拔，值域0～20000；
	4）第12～13个字符，表示精度，值域0～1000；
	5）第14个字符，表示速度，值域0～63；
	6）第15～20个字符，表示时戳，值域0～3000000000；

	例如一条GPS数据为：经度114.35407，纬度30.53536，海拔100米， 精度50，速度8米/秒，时戳1294367860秒，GPS方式采集。则对应的压缩字符串为KFWfbK%Q01C0S81d9FRU.

	有效GPS数据的精度范围为[72.0, 140.0],纬度范围[10.0, 60.0].*/
	
	//64进制字符串
	//小写字母无l，o，大写字符无I,O
	private static String char64 = "0123456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ$%*~^@";
	
	//fym 数字转换为64进制字符串
	private static String convert_num_to_str(long number, short radix, short length_inneed)
	{
		String str = "";
		
		while(0 < number)
		{
			str = char64.charAt((int)(number % radix)) + str;
			number /= radix;
		}
		
		//不足位补0
		while(str.length() < length_inneed)
		{
			str = '0' + str;
		}
		
		return str;
	}
	
	//fym 64进制字符串转换为数字
	private static long convert_str_to_num(String str)
	{
		long num = 0;
		
		for(int i = 0; i < str.length(); ++i)
		{
			long pos = char64.indexOf(str.charAt(i));
			
			if(-1 != pos)
			{
				num *= 64;
				num += pos;
			}
		}
		
		return num;
	}
	
	//fym 单条GPS数据压缩
	public static String compress_gps_data(double longitude, double latitude, short altitude, short accuracy, byte speed, long timestamp, boolean is_gps)
	{
		String gps = "";
		
		if(72.0 > longitude || 140.0 < longitude || 10.0 > latitude || 140.0 < latitude)//纬度包含APGS情况
		{
			gps += "00000000000000000000";			
			return gps;
		}
		
		//经度
		gps += convert_num_to_str((long) (longitude * 1E5), (short) 64, (short) 4);
		
		//纬度
		gps += convert_num_to_str((long)(latitude * 1E5) + ((true == is_gps) ? 0 : 7000000L), (short) 64, (short) 4);
		
		//高度
		altitude = (20000 < altitude) ? 20000 : altitude;
		gps += convert_num_to_str((long) altitude, (short) 64, (short) 3);
		
		//精度
		accuracy = (1000 < accuracy) ? 1000 : accuracy;
		gps += convert_num_to_str((long) accuracy, (short) 64, (short) 2);
		
		//速度
		speed = (63 < speed) ? 63 : speed;
		gps += convert_num_to_str((long) speed, (short) 64, (short) 1);
		
		//时戳
		//Log.v("GPS", "timestamp 1: " + timestamp);
		timestamp = (3000000000L < timestamp) ?  3000000000L : timestamp;
		//Log.v("GPS", "timestamp 2: " + timestamp);
		
		gps += convert_num_to_str((long) timestamp, (short) 64, (short) 6);

		return gps;
	}
	
	/*从地图服务器下载的GPS数据压缩时采用64进制，即0～63依次由0～9，a～z（即小写英文字母，但不包括l和o），A～Z（即大写英文字母，但不包括I和O），$，%，*，#，&，@共64个字符表示，一条GPS数据共个20字符，依次为：
	1）第1～4个字符，表示经度乘100000得到整数值，值域0～14099999；
	2）第5～8个字符，表示纬度乘100000得到整数值，值域0～130999999，超过小于7000000则为GPS方式，否则为AGPS方式，此时减去7000000则为实际纬度值；
	3）第9～14个字符，表示时戳，值域0～3000000000；*/
	
	//fym 解析压缩的GPS数据
	//返回数组第0元素为经度，第1元素为纬度，第2元素为时戳
	public static ArrayList<Object> decompress_gps_data(String data)
	{
		if(14 != data.length())
			return null;
		
		ArrayList<Object> result = new ArrayList<Object>();
		
		double longitude = (double)(convert_str_to_num(data.substring(0, 4)) / 1E5);
		result.add(longitude);
		
		double latitude = (double)(((convert_str_to_num(data.substring(4, 8))) % 7000000L) / 1E5);
		result.add(latitude);
		
		Long timestamp = convert_str_to_num(data.substring(8, 14));
		result.add(timestamp);
		
		return result;		
	}
	
	public static void change_register_time(Context context)
	{
		SP.set(context, SP.LAST_UPDATA_CONFIGURE_TIME, "1970-01-01 00:00:00");
		SP.set(context, SP.LAST_UPDATE_ORGNIZATION_TIMESTAMP, "1970-01-01 00:00:00");
		SP.set(context, SP.LAST_UPDATE_GROUP_TIMESTAMP, "1970-01-01 00:00:00");
		SP.set(context, SP.LAST_UPDATE_TASK_TIMESTAMP, "1970-01-01 00:00:00");
				
		SP.set(context,SP.PREF_PASSWORD,null);
	}
	
	public static int byte_2_int(byte b)//将有符号byte作为无符号byte并转为int型
	{
		return ((0 > b) ? (256 + b) : b);
	}
	
	// 以下三个变量用于键盘锁的开启或关闭
	private static KeyguardManager _keyguard_manager;
	private static KeyguardManager.KeyguardLock _keyguard_lock;
	private static boolean _keyguard_enabled = false;
	
	//让键盘锁失效
	public static void disable_keyguard(Context context)
	{
		if (_keyguard_manager == null)
		{
			_keyguard_manager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
			_keyguard_lock = _keyguard_manager.newKeyguardLock("com.nercms.schedule");
			_keyguard_enabled = true;
		}
		if(true == _keyguard_enabled)
		{
			_keyguard_lock.disableKeyguard();
			_keyguard_enabled = false;
		}
	}
		
	//让键盘锁重新生效
	public static void reenable_keyguard()
	{
		if(false == _keyguard_enabled)
		{
			try
			{
				if (Integer.parseInt(Build.VERSION.SDK) < 5)
					Thread.sleep(1000);
			}
			catch (InterruptedException e) {}
			
			_keyguard_lock.reenableKeyguard();
			_keyguard_enabled = true;
		}
	}
	
	//系统中四类服务：RegisterService，NetworkStatus, SOSService和GPSSampler
	//前两者由start_sip_media_service和shutdown_sip_media_service启动和关闭
	//SOSService由设置音量函数set_volumn_change_time启动
	
	//开启SIP及媒体服务
	public static void start_sip_media_service()
	{
		//开启Sip服务、音视频服务以及RegisterSerivice
		android.util.Log.v("RTSP", "SIPMediaServiceManager.start 1");
		SIPMediaServiceManager.start(true);
		
		//开启网络状态监测及自适应处理服务
		NetworkStatus.start(GD.get_global_context());
	}
	
	//关闭SIP及媒体服务
	public static void shutdown_sip_media_service()
	{
		long t = System.currentTimeMillis();
		Log.v("Temp", "shutdown 0");
		//关闭网络状态监测及自适应处理服务
		NetworkStatus.stop(GD.get_global_context());
		
		Log.v("Temp", "shutdown 1: " + (System.currentTimeMillis() - t));
				
		//关闭Sip服务、音视频服务以及RegisterSerivice
		SIPMediaServiceManager.shutdown(true);
		
		Log.v("Temp", "shutdown 2: " + (System.currentTimeMillis() - t));
		
		SipdroidReceiver.alarm(0, OnReRegister.class);
		
		Log.v("Temp", "shutdown 3: " + (System.currentTimeMillis() - t));
	}
	
	//重启SIP及媒体服务
	//不重启NetworkStatus服务
	//该函数只能在NetworkStatus线程中调用
	public static Object _restart_lock = new Object();
	public static void restart_sip_media_service()
	{
		try
		{
			Log.e("Temp", "restart_sip_media_service shutdown");
			//关闭GPS采样，避免重启期间向数据库写入GPS数据报错
			//shutdown_gps_sampler();
			
			//Log.v("Temp", "restart 1");
			//关闭Sip服务、音视频服务以及RegisterSerivice
			SIPMediaServiceManager.shutdown(false);
			
			Thread.sleep(1000);
			
			//网络中断则不启动
			IpAddress.setLocalIpAddress();
			if(true == IpAddress.localIpAddress.equalsIgnoreCase("") || null == IpAddress.localIpAddress || true == IpAddress.localIpAddress.equalsIgnoreCase("127.0.0.1"))
			{
				return;
			}
			
			//Log.v("Temp", "ping_delay() 1");
			int ping_delay = (int)(NetworkStatus.ping_delay(GD.DEFAULT_SCHEDULE_SERVER));//ping不通也是中断
			if(0 == ping_delay)
			{
				return;
			}
			
			Log.e("Temp", "restart_sip_media_service start");
			//Log.v("Temp", "restart 2");
			//开启Sip服务、音视频服务以及RegisterSerivice
			android.util.Log.v("RTSP", "SIPMediaServiceManager.start 2");
			SIPMediaServiceManager.start(false);
			
			//start_gps_sampler();
			
			//Log.v("Temp", "restart 3");
		}
		catch(Exception e)
		{
		}
	}
	
	//删除json字符串中值为""的键值对
	public static String remove_empty_value_in_json(String json)
	{
		//方式地图服务器返回值出现形如"key":null的错误
		json = json.replace("\":null", "\":\"\"");
		
		String pair = "";//待删除的值为""的键值对字符串，包括之前的逗号（如存在）
		int pos = 0;
		int head = 0;//键值对首地址
		int tail = 0;//键值对末地址
			
		while(true == json.contains("\"\""))
		{
			pos = json.indexOf("\"\"");
			
			tail = pos + 1;
			
			pos = json.lastIndexOf("\"", pos - 1);//找到键的后一个引号
			pos = json.lastIndexOf("\"", pos - 1);//找到键的第一个引号
				
			head = (',' == json.charAt(tail + 1)) ? (pos - 1) : pos;//如键值对前无逗号则不删除
				
			pair = json.substring(head, tail + 1);			
			//Log.v("Video", "pair " + pair);
			
			json = json.replace(pair, "");
			//Log.v("Video", "json " + json);			
		}
		
		//去除大括号内JSON字符串以逗号结尾的现象
		if(2 < json.length())
		{
			json = json.replace(",}", "}");
		}
			
		return json;
	}
	
	//处理收到的MESSAGE信令
	public static void parse_schedule_notifty_message(String json)
	{
		Log.i("JSON","notify: " + json);
		/*{
			"t":"notify",
			"c":"1111",
			"p":
			[
				{
					"id":"1111",
					"n":"张三",
					"s":"1"//1--发言人, 0--听众
					"t":"0"//0--警务终端1
					"a":"1"//1--已接听，0--未接听
				},
				{
					"id":"2222",
					"n":"李四",
					"s":"1"//1--发言人, 0--听众
					"t":"0"//0--警务终端1
					"a":"1"//1--已接听，0--未接听
				},
				{
					"id":"3333",
					"n":"王五",
					"s":"0"//1--发言人, 0--听众
					"t":"0"//0--警务终端1
					"a":"1"//1--已接听，0--未接听
				},
			],
			"v":"1111",
		}*/
		
		try
		{
			JSONObject root = new JSONObject(json);
			
			//参数校验
			if(false == root.has("t"))
				return;
			
			if(true == root.getString("t").equalsIgnoreCase("notify"))//会议状态通知
			{
				android.util.Log.v("UserAgent", json);
				
				//参数校验
				if(false == root.has("c") ||
					false == root.has("p") ||
					false == root.has("v"))
					return;
				
				//会议ID
				GD._conference_id = root.getString("c");
				
				//视频源ID
				long video_source = Long.parseLong(root.getString("v"));
				
				synchronized(GD._participants_lock)
				{
					//与会成员
					JSONArray p = root.getJSONArray("p");
					
					//更新前必须清空GD._participants
					GD._participants.clear();
					
					android.util.Log.v("Temp", "=========");
					
					for(int i = 0; i < p.length(); ++i)
					{
						/*public int _id;//与会UA的ID
						public int _type;//与会UA的类型
						public String _name;//与会UA的名称
						public boolean _has_answered;//是否已接听				
						public boolean _is_speaker;//是否发言人
						public boolean _is_video_source;//是否视频源*/
						
						JSONObject ua_obj = p.getJSONObject(i);
						
						//参数校验
						if(false == ua_obj.has("id") ||
							false == ua_obj.has("n") ||
							false == ua_obj.has("s") ||
							false == ua_obj.has("t") ||
							false == ua_obj.has("a"))
							continue;
						
						//android.util.Log.v("UserAgent", "json "+ ua_obj.getString("id"));
						
						Participant ua = new Participant();
						ua._id = Long.parseLong(ua_obj.getString("id"));
						ua._type = Integer.parseInt(ua_obj.getString("t"));
						ua._name = ua_obj.getString("n");
						ua._has_answered = (true == ua_obj.getString("a").equals("1")) ? true : false;
						ua._is_speaker = (true == ua_obj.getString("s").equals("1")) ? true : false;
						ua._is_video_source = (ua._id == video_source) ? true : false;
						
						android.util.Log.v("Temp", "id " + ua._id + " " + ua._name + " "
								+ ((ua._has_answered) ? "已接听" : "未接听") + " " + ((ua._is_speaker) ? "发言人" : "听众") + " " + ((ua._is_video_source) ? "视频源" : "非视频源"));
						
						GD._participants.put(ua_obj.getString("id"), ua);
					}
				}
			}
			else if(true == root.getString("t").equalsIgnoreCase("send_video"))//进入发送视频状态
			{
				/*{
					"t":"send_video"
				}*/
				android.util.Log.v("UA", "MESSAGE JSON: send_video");
				GD._i_am_video_source = true;//SP.set(SipdroidReceiver.mContext, SP.PREF_IS_VIDEO_SOURCE, true);
				MessageHandlerManager.get_instance().handle_message(GID.MSG_SEND_VIDEO, GD.MEDIA_INSTANCE);
			}
			else if(true == root.getString("t").equalsIgnoreCase("recv_video"))//进入接收视频状态
			{
				/*{
					"t":"recv_video"
				}*/
				
				android.util.Log.v("UA", "MESSAGE JSON: recv_video");
				GD._i_am_video_source = false;//SP.set(SipdroidReceiver.mContext, SP.PREF_IS_VIDEO_SOURCE, false);
				MessageHandlerManager.get_instance().handle_message(GID.MSG_RECV_VIDEO, GD.MEDIA_INSTANCE);
			}
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			Log.v("JSON", e.toString());
			e.printStackTrace();
		}
	}
}
