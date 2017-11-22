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
	public static long _start_timestamp = 0;//�������һ������ʱ�䣬��λ����
	
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
	
	public static boolean REDUNDANT_AUDIO_SEND = false;//true;//true-��UDP���෢����Ƶ��(����12, 23...), false-TCP/UDP˫·�����෢��
	public static boolean TCP_AUDIO_RECV = false;
	
	//WCDMA
	public static final int UA_LINES = 2;//2;//1--SIP��Audioֻ֧��TCP��2--SIP��Audioͬʱ֧��TCP��UDP
	public static boolean OVER_VPDN = false;
	//public static String SIP_PROTOCOL = "tcp";
	//public static String AUDIO_PROTOCOL = "tcp";
	public static String VIDEO_PROTOCOL = "udp";	
	public static int LOCATION_SERVER_PORT = 8080;//��λ�������˿�
	public static int SIP_SERVER_PORT = 5060;//SIP�������˿�
	//public static String DEFAULT_SCHEDULE_SERVER = "220.249.112.22";//SIP������������ַ
	public static String DEFAULT_SCHEDULE_SERVER = "120.26.78.7";//SIP������������ַ
	public static int  SERVER_NAT_PORT = 12580;
	//public static String DEFAULT_SCHEDULE_SERVER = "121.40.214.71";//SIP������������ַ��������
	public static String SIP_SERVER_LAN_IP_IN_VPN = DEFAULT_SCHEDULE_SERVER;//SIP������������ַ
	public static final int SHORT_LOCATE_INTERVAL = 15000;//�̶�λʱ����3s
	//public static final int LONG_LOCATE_INTERVAL = 30000;//����λʱ����60s
	public static int RTSP_SERVER_PORT = 1554;//RTSP�������˿�
	public static boolean MEDIA_STATISTICS = true;
	public static boolean THREAD_STATISTICS = false;
	public static boolean DEFAULT_HANDS_FREE = false;
	public static int DEFAULT_CAMERA = 0;//0-���ã�1-ǰ��
	public static long CHECK_VERSION_INTERVAL = 120000L;//���汾���¼������λ����
	public static final int VIDEO_SAMPLE_INTERVAL = 67;//100;//67;//125;//200;//200;//200;//333;//��Ƶ����������С���(ms)
	public static final long RTSP_RECONNECT_INTERVAL = 8000;//��δ�յ�����ƵRTSP����ǰ�ȴ�ʱ������λ����
	public static boolean TOLERANCE_FOR_VIDEO_MOSAIC = true;//�Ƿ�������Ƶ������
	public static int VIDEO_WIDTH = 352;//1280;//352;
	public static int VIDEO_HEIGHT = 288;//720;//288;
	
	public static boolean sip_audio_over_udp()
	{
		return (1 < UA_LINES);
	}
	
	//��TCP��ʽ��������Ƶ����ʱ
	public static int RTSP_SEND_BUFFER_SIZE = 10240;//8192;//4096;//2048;
	public static int RTSP_RECV_BUFFER_SIZE = 5120;//4096;//2048;
	
	//RTSP socketchannel��ʱʱ�䣨���룩
	public static int RTSP_SOCKET_CHANNEL_TIME_OUT = 20;//ms
	
	//��UDP��ʽ����Ƶ����ʱ
	public static int NIO_UDP_AUDIO_RECV_BUFFER_SIZE = 1536;
	public static int NIO_UDP_VIDEO_RECV_BUFFER_SIZE = 10240;
	
	public static int UDP_AUDIO_RECV_SOCKET_TIME_OUT = 200;//20;//20;//ms
	public static int UDP_VIDEO_RECV_SOCKET_TIME_OUT = 200;//20;//ms
	
	public static boolean FAKE_COMMUNICATION_WITH_LOCATION_SERVER = false;
	public static boolean NO_RENDERING_DECODE_VIDEO = false;
	
	//���ͼ�����������ӷ�ʽ
	public static String LOCATION_SERVER_INTERFACE = "http";
	//public static String LOCATION_SERVER_INTERFACE = "webservice";
	
	//����������
	public static int SERVER_AUDIO_RECV_PORT = 30000;//��Ƶ�������Ľ��ն˿ڳ�ʼֵ
	public static int SERVER_VIDEO_RECV_PORT = 30010;//��Ƶ�������Ľ��ն˿ڳ�ʼֵ
	
	//����ȷ���������NAT̽�����ʱ���������룩
	public static long SCHEDULE_REGISTER_INTERVAL = 20000;
	
	//���ͼ������ע���ʱ���������룩
	public static final long LOCATION_REGISTER_INTERVAL = 30000;
	
	//����������Դ�����ʱ���������룩
	public static final long MAX_UPDATE_RESOURCE_POSITION_INTERVAL = 5000;
	
	//�ϴ�����GPS���ݵ����ʱ���������룩
	public static final long MAX_UPLOAD_GPS_DATA_INTERVAL = 15000;
	
	//�������ʱ�䳬������ֵ���룩�������û���������ʱ��
	public static final long MAX_TIME_DIFF_BETWEEN_SERVER_AND_LOCAL = 180;
	
	//��ǰҳ���Ƿ�Ϊ��ͼҳ��
	public static boolean _is_rendering_map = true;
	
	//ÿ��1Сʱɾ��һ��GPS���ݣ����룩
	public static final long GPS_DATA_DELETE_INTERVAL = 3600000;
	
	//ÿ������ϴ�50����¼
	public static final int GPS_UPLOAD_MAX_COUNT_ONCE = 30;//50;
	
	public static String LOG_TAG = "Schedule";
	
	//private static boolean _control_available = true; //��ʶ�ڵ��ȵ�����£��ؼ��Ƿ����
	
	public static final String SCHEDULE = "com.nercms.schedule.schedule"; //��Ϊ���ȹ㲥��Action
	
	//���һ��SIPע��ɹ���ʱ��
	public static long _latest_sip_register_success = 0;
	
	private static long _schedule_timestamp = 0;//�������ʱ��,���ڿ���״̬ʱ��ʱ��Ϊ0
	private static long _rtsp_session_start_timestamp = 0;//������Ⱥ�RTSP�Ự�����ɹ���ʱ��
	private static long _rtsp_session_complete_timestamp = 0;//������Ⱥ�RTSP�Ự�����ɹ���ʱ��
	private static long _recv_audio_timestamp = 0;//������Ⱥ��յ���һ����Ƶ����ʱ��
	private static long _play_audio_timestamp = 0;//������Ⱥ󲥷ŵ�һ����Ƶ����ʱ��
	private static long _recv_video_timestamp = 0;//������Ⱥ��յ���һ����Ƶ����ʱ��
	private static long _render_video_timestamp = 0;//������Ⱥ󲥷ŵ�һ����Ƶ����ʱ��
	
	public static float _video_render_scale = 1.0f;
	
	//��¼���Ⱥ��״��յ���Ƶ����ʱ��
	public static void update_recv_audio_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
			return;
		
		if(0 == GD._recv_audio_timestamp)
		{
			GD._recv_audio_timestamp = System.currentTimeMillis();
		}
	}
	
	//��¼���Ⱥ��״��յ���Ƶ����ʱ��
	public static void update_recv_video_timestamp()
	{
		if(SCHEDULE_STATE.in_schedule != _schedule_state)
			return;
		
		if(0 == GD._recv_video_timestamp)
		{
			GD._recv_video_timestamp = System.currentTimeMillis();
		}
	}
	
	//��¼���Ⱥ��״β�����Ƶ����ʱ��
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
		
	//��¼���Ⱥ��״β�����Ƶ����ʱ��
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
		
		//������ָտ�ʼRTSP�Ự�����������
        GD._latest_rtsp_video_data_timestamp = System.currentTimeMillis();
        GD._latest_rtsp_audio_data_timestamp = System.currentTimeMillis();
	}
	
	//����״̬
	public static enum SCHEDULE_STATE
	{
		idle,//������
		incoming_call,//�յ����л�δ����
		in_schedule,//�����У����ѷ�����Ȼ��ѽ�������
		closing,//�رճ�����
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
		
		//��Ѹ��������ر�RTSP����
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
		
	//�Ƿ��ڵ�����
	public static boolean is_in_schedule()
	{
		return (_schedule_state == SCHEDULE_STATE.in_schedule);
	}
	
	public static String _conference_id = "";// Ŀǰ����������ID
	
	public static HashMap<String, Participant> _participants = new HashMap<String, Participant>();;// Ŀǰ���������ĳ�Ա<��ԱID����Ա����>
	public static Object _participants_lock = new Object();
	
	public static final int MAX_SPEAKER_NUM = 4; //���������Ŀ 
	
	public static boolean MEDIA_PAUSE_IN_SCHEDULE = false;//�������Ƿ���ͣ����Ƶ����
	
	/** ���� */
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
	 
	public static final int IDLE = 0; //����״̬
	public static final int TALK = 1; //ͨ��״̬
	public static final int VIDEO = 2; //������Ƶ״̬
	
	private static Context _global_context = null; //ȫ�ֵ������ı���
	public static OnMsgCallback _msg_callback = null;
	
	private static int _business_type = 0; //1:��ʾ��Դ���� 2:��ʾͳһͨ��  3����ʾ���ܵ���
	
	public static boolean _i_am_video_source = false; //�����Ƿ���ƵԴ
	public static String _video_source_imsi = ""; //��ƵԴ��imsi
	
	public static int VIDEO_ENCODE_STRATEGY = -1; //��Ƶ֡����������-1���Զ�  0����P֡      1����IDR֡       2����I֡��
	public static int ONLY_IDR_FRAME_INTERVAL = 2000; //ֻ��IDR֡ʱ������IDR֡����С���
	public static int MAX_VIDEO_PACKET_SIZE = 800;  //��Ƶ���ݰ������ߴ�	
	//public static final int VIDEO_SAMPLE_INTERVAL = 333;//333;//��Ƶ����������С���(ms)
	
	public static long _last_shut_schedule_time = 0L; // �ϴιرյ��ȵ�ʱ��
	
	public static final int START_GPS = 1; //����GPS��requestCode
	
	//��Ƶ
	public static ByteBuffer _video_render_buffer = null;//���������������
	public static boolean video_is_coming = false;//���ε������Ƿ��յ���Ƶ����
	public static Bitmap VideoBit = null;//Bitmap.createBitmap(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, Config.RGB_565);//�����ͼ�񻺴�
	public static Object _video_lock = new Object();
	
	public static long _latest_rtsp_video_data_timestamp = 0;//��Ƶ�Ự���һ���յ�RTSP���������͵����ݵ�ʱ��
	public static long _latest_rtsp_audio_data_timestamp = 0;//��Ƶ�Ự���һ���յ�RTSP���������͵����ݵ�ʱ��
	
	//����ȫ�ֵ������ı���
	public static void set_global_context(Context context)
	{
		_global_context = context;
	}
	
	//��ȡȫ�ֵ������ı���
	public static Context get_global_context()
	{
		return _global_context;//��Descriptionҳ���Context��Ϊȫ��Context
	}
	
	public static boolean is_numeric(String str)
	{
		Pattern pattern = Pattern.compile("[0-9]*");
		return pattern.matcher(str).matches();
	}
	
	//��ȡ����IMSI
	private static long _local_imsi = 0;// ����IMSI�������ID��
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
	/*private static long _last_volumn_change_timestamp = 0;//�ϴ������仯��ʱ��
	private static long _continued_volumn_change_time = 0;//���������仯������δ�ɿ���ʱ��
	private static boolean _release_volumn_button_after_trigger = true;//�ϴ������仯�¼��������Ƿ��ɿ���������
	private static final int UNAVAILABLE_VOLUMN_CHANGE_INTERVAL = 1000;//���������仯�����˼������Ϊ�����ڴ��ڼ����ɿ�
	private static final int AVAILABLE_VOLUMN_CHANGE_INTERVAL = 2000;//���������仯ʱ�������˼��������SOS����
	public static void on_volumn_change()
	{
		//Log.v("Temp", "volumn change.");
		
		//if(true) return;
			
		long current = System.currentTimeMillis();
		
		if(UNAVAILABLE_VOLUMN_CHANGE_INTERVAL < (current - _last_volumn_change_timestamp))
		{
			reset_volumn_change();
			return;//�ϴ������仯��������������̧���
		}
			
		//�ϴ������仯������������δ��̧��
		_continued_volumn_change_time += current - _last_volumn_change_timestamp;
		_last_volumn_change_timestamp = current;
			
		if(AVAILABLE_VOLUMN_CHANGE_INTERVAL <= _continued_volumn_change_time && true == _release_volumn_button_after_trigger)
		{
			Log.v("Temp", "volumn change trigger.");
			
			if(GD.is_in_schedule())
			{
				//�ڵ����У��ر�SOS����
				Log.i(GD.LOG_TAG,"shut sos");
				//rmv MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE, GD.SCHEDULEMAP);
			}
			else if(false == NetworkStatus._is_restarting_media_and_sip_service)
			{
				//ͨ��SOS������SOS����
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
	
	//����־д�����ݿ�
	/*public static void log_to_db(Context context, int type, String tag, String content)
	{
		if(tag.equals("Statistics"))
		{
			Log.v("Network", content);
		}
		
		if(true) return;
	}*/
	
	//�ַ�������
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
	
	//�Ҷ����ڽ��е������绰
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

	// ����ͨ������
	public static void set_conference_volume(Context context)
	{
		AudioManager audio_manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		audio_manager.setMode(AudioManager.MODE_NORMAL);
		// ��ȡ���ý������ֵ
		int max_media_volum = audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int max_call_volum = audio_manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

		// ��ý����������3/5
		audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, max_media_volum/* * 3 / 5*/, 0);
		// ��ͨ����������˷��������������
		audio_manager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, max_call_volum/* * 4 / 5*/, 0);
	}
	
	//���޸�AMRPlay._hands_free�ٵ��ô˺���
	public static void hands_free_volume_switch()
	{
		if(10 < android.os.Build.VERSION.SDK_INT)//Android 4.0����ͨ��AudioManager����������Ч
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

	// �Ƿ�������Ƶ�л���enableΪtrue������
	public static void enable_lock_screen(Context context, boolean enable)
	{
		android.provider.Settings.System.putInt(context.getContentResolver(),
												android.provider.Settings.System.LOCK_PATTERN_ENABLED,
												(true == enable) ? 1 : 0);
	}
	
	/*�ն����ɼ���GPS���ݰ�����
	1�����ȣ����㣬����114.35407���������ֲ�����140��С�����5λ��������0.99999
	2��γ�ȣ����㣬����30.53536, �������ֲ�����60��С�����5λ��������0.99999
	3�����Σ����Σ�����800��������20000����λ��
	4�����ȣ����Σ�����50�� ������1000����λ��
	5���ٶȣ����Σ�����13��������63����λ��/��
	6��ʱ�������Σ����ɼ�������ʱ��1970��1��1��0��0�������������1294367860��������3000000000
	7���ɼ���ʽ��GPS��AGPS��ʽ

	GPS����ѹ��ʱ����64���ƣ���0��63������0��9��a��z����СдӢ����ĸ����������l��o����A��Z������дӢ����ĸ����������I��O����$��%��*��#��&��@��64���ַ���ʾ��һ��GPS���ݹ���20�ַ�������Ϊ��
	1����1��4���ַ�����ʾ���ȳ�100000�õ�����ֵ��ֵ��0��14099999��
	2����5��8���ַ�����ʾγ�ȳ�100000�õ�����ֵ��ֵ��0��130999999������С��7000000��ΪGPS��ʽ������ΪAGPS��ʽ����ʱ��ȥ7000000��Ϊʵ��γ��ֵ��
	3����9��11���ַ�����ʾ���Σ�ֵ��0��20000��
	4����12��13���ַ�����ʾ���ȣ�ֵ��0��1000��
	5����14���ַ�����ʾ�ٶȣ�ֵ��0��63��
	6����15��20���ַ�����ʾʱ����ֵ��0��3000000000��

	����һ��GPS����Ϊ������114.35407��γ��30.53536������100�ף� ����50���ٶ�8��/�룬ʱ��1294367860�룬GPS��ʽ�ɼ������Ӧ��ѹ���ַ���ΪKFWfbK%Q01C0S81d9FRU.

	��ЧGPS���ݵľ��ȷ�ΧΪ[72.0, 140.0],γ�ȷ�Χ[10.0, 60.0].*/
	
	//64�����ַ���
	//Сд��ĸ��l��o����д�ַ���I,O
	private static String char64 = "0123456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ$%*~^@";
	
	//fym ����ת��Ϊ64�����ַ���
	private static String convert_num_to_str(long number, short radix, short length_inneed)
	{
		String str = "";
		
		while(0 < number)
		{
			str = char64.charAt((int)(number % radix)) + str;
			number /= radix;
		}
		
		//����λ��0
		while(str.length() < length_inneed)
		{
			str = '0' + str;
		}
		
		return str;
	}
	
	//fym 64�����ַ���ת��Ϊ����
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
	
	//fym ����GPS����ѹ��
	public static String compress_gps_data(double longitude, double latitude, short altitude, short accuracy, byte speed, long timestamp, boolean is_gps)
	{
		String gps = "";
		
		if(72.0 > longitude || 140.0 < longitude || 10.0 > latitude || 140.0 < latitude)//γ�Ȱ���APGS���
		{
			gps += "00000000000000000000";			
			return gps;
		}
		
		//����
		gps += convert_num_to_str((long) (longitude * 1E5), (short) 64, (short) 4);
		
		//γ��
		gps += convert_num_to_str((long)(latitude * 1E5) + ((true == is_gps) ? 0 : 7000000L), (short) 64, (short) 4);
		
		//�߶�
		altitude = (20000 < altitude) ? 20000 : altitude;
		gps += convert_num_to_str((long) altitude, (short) 64, (short) 3);
		
		//����
		accuracy = (1000 < accuracy) ? 1000 : accuracy;
		gps += convert_num_to_str((long) accuracy, (short) 64, (short) 2);
		
		//�ٶ�
		speed = (63 < speed) ? 63 : speed;
		gps += convert_num_to_str((long) speed, (short) 64, (short) 1);
		
		//ʱ��
		//Log.v("GPS", "timestamp 1: " + timestamp);
		timestamp = (3000000000L < timestamp) ?  3000000000L : timestamp;
		//Log.v("GPS", "timestamp 2: " + timestamp);
		
		gps += convert_num_to_str((long) timestamp, (short) 64, (short) 6);

		return gps;
	}
	
	/*�ӵ�ͼ���������ص�GPS����ѹ��ʱ����64���ƣ���0��63������0��9��a��z����СдӢ����ĸ����������l��o����A��Z������дӢ����ĸ����������I��O����$��%��*��#��&��@��64���ַ���ʾ��һ��GPS���ݹ���20�ַ�������Ϊ��
	1����1��4���ַ�����ʾ���ȳ�100000�õ�����ֵ��ֵ��0��14099999��
	2����5��8���ַ�����ʾγ�ȳ�100000�õ�����ֵ��ֵ��0��130999999������С��7000000��ΪGPS��ʽ������ΪAGPS��ʽ����ʱ��ȥ7000000��Ϊʵ��γ��ֵ��
	3����9��14���ַ�����ʾʱ����ֵ��0��3000000000��*/
	
	//fym ����ѹ����GPS����
	//���������0Ԫ��Ϊ���ȣ���1Ԫ��Ϊγ�ȣ���2Ԫ��Ϊʱ��
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
	
	public static int byte_2_int(byte b)//���з���byte��Ϊ�޷���byte��תΪint��
	{
		return ((0 > b) ? (256 + b) : b);
	}
	
	// ���������������ڼ������Ŀ�����ر�
	private static KeyguardManager _keyguard_manager;
	private static KeyguardManager.KeyguardLock _keyguard_lock;
	private static boolean _keyguard_enabled = false;
	
	//�ü�����ʧЧ
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
		
	//�ü�����������Ч
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
	
	//ϵͳ���������RegisterService��NetworkStatus, SOSService��GPSSampler
	//ǰ������start_sip_media_service��shutdown_sip_media_service�����͹ر�
	//SOSService��������������set_volumn_change_time����
	
	//����SIP��ý�����
	public static void start_sip_media_service()
	{
		//����Sip��������Ƶ�����Լ�RegisterSerivice
		android.util.Log.v("RTSP", "SIPMediaServiceManager.start 1");
		SIPMediaServiceManager.start(true);
		
		//��������״̬��⼰����Ӧ�������
		NetworkStatus.start(GD.get_global_context());
	}
	
	//�ر�SIP��ý�����
	public static void shutdown_sip_media_service()
	{
		long t = System.currentTimeMillis();
		Log.v("Temp", "shutdown 0");
		//�ر�����״̬��⼰����Ӧ�������
		NetworkStatus.stop(GD.get_global_context());
		
		Log.v("Temp", "shutdown 1: " + (System.currentTimeMillis() - t));
				
		//�ر�Sip��������Ƶ�����Լ�RegisterSerivice
		SIPMediaServiceManager.shutdown(true);
		
		Log.v("Temp", "shutdown 2: " + (System.currentTimeMillis() - t));
		
		SipdroidReceiver.alarm(0, OnReRegister.class);
		
		Log.v("Temp", "shutdown 3: " + (System.currentTimeMillis() - t));
	}
	
	//����SIP��ý�����
	//������NetworkStatus����
	//�ú���ֻ����NetworkStatus�߳��е���
	public static Object _restart_lock = new Object();
	public static void restart_sip_media_service()
	{
		try
		{
			Log.e("Temp", "restart_sip_media_service shutdown");
			//�ر�GPS���������������ڼ������ݿ�д��GPS���ݱ���
			//shutdown_gps_sampler();
			
			//Log.v("Temp", "restart 1");
			//�ر�Sip��������Ƶ�����Լ�RegisterSerivice
			SIPMediaServiceManager.shutdown(false);
			
			Thread.sleep(1000);
			
			//�����ж�������
			IpAddress.setLocalIpAddress();
			if(true == IpAddress.localIpAddress.equalsIgnoreCase("") || null == IpAddress.localIpAddress || true == IpAddress.localIpAddress.equalsIgnoreCase("127.0.0.1"))
			{
				return;
			}
			
			//Log.v("Temp", "ping_delay() 1");
			int ping_delay = (int)(NetworkStatus.ping_delay(GD.DEFAULT_SCHEDULE_SERVER));//ping��ͨҲ���ж�
			if(0 == ping_delay)
			{
				return;
			}
			
			Log.e("Temp", "restart_sip_media_service start");
			//Log.v("Temp", "restart 2");
			//����Sip��������Ƶ�����Լ�RegisterSerivice
			android.util.Log.v("RTSP", "SIPMediaServiceManager.start 2");
			SIPMediaServiceManager.start(false);
			
			//start_gps_sampler();
			
			//Log.v("Temp", "restart 3");
		}
		catch(Exception e)
		{
		}
	}
	
	//ɾ��json�ַ�����ֵΪ""�ļ�ֵ��
	public static String remove_empty_value_in_json(String json)
	{
		//��ʽ��ͼ����������ֵ��������"key":null�Ĵ���
		json = json.replace("\":null", "\":\"\"");
		
		String pair = "";//��ɾ����ֵΪ""�ļ�ֵ���ַ���������֮ǰ�Ķ��ţ�����ڣ�
		int pos = 0;
		int head = 0;//��ֵ���׵�ַ
		int tail = 0;//��ֵ��ĩ��ַ
			
		while(true == json.contains("\"\""))
		{
			pos = json.indexOf("\"\"");
			
			tail = pos + 1;
			
			pos = json.lastIndexOf("\"", pos - 1);//�ҵ����ĺ�һ������
			pos = json.lastIndexOf("\"", pos - 1);//�ҵ����ĵ�һ������
				
			head = (',' == json.charAt(tail + 1)) ? (pos - 1) : pos;//���ֵ��ǰ�޶�����ɾ��
				
			pair = json.substring(head, tail + 1);			
			//Log.v("Video", "pair " + pair);
			
			json = json.replace(pair, "");
			//Log.v("Video", "json " + json);			
		}
		
		//ȥ����������JSON�ַ����Զ��Ž�β������
		if(2 < json.length())
		{
			json = json.replace(",}", "}");
		}
			
		return json;
	}
	
	//�����յ���MESSAGE����
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
					"n":"����",
					"s":"1"//1--������, 0--����
					"t":"0"//0--�����ն�1
					"a":"1"//1--�ѽ�����0--δ����
				},
				{
					"id":"2222",
					"n":"����",
					"s":"1"//1--������, 0--����
					"t":"0"//0--�����ն�1
					"a":"1"//1--�ѽ�����0--δ����
				},
				{
					"id":"3333",
					"n":"����",
					"s":"0"//1--������, 0--����
					"t":"0"//0--�����ն�1
					"a":"1"//1--�ѽ�����0--δ����
				},
			],
			"v":"1111",
		}*/
		
		try
		{
			JSONObject root = new JSONObject(json);
			
			//����У��
			if(false == root.has("t"))
				return;
			
			if(true == root.getString("t").equalsIgnoreCase("notify"))//����״̬֪ͨ
			{
				android.util.Log.v("UserAgent", json);
				
				//����У��
				if(false == root.has("c") ||
					false == root.has("p") ||
					false == root.has("v"))
					return;
				
				//����ID
				GD._conference_id = root.getString("c");
				
				//��ƵԴID
				long video_source = Long.parseLong(root.getString("v"));
				
				synchronized(GD._participants_lock)
				{
					//����Ա
					JSONArray p = root.getJSONArray("p");
					
					//����ǰ�������GD._participants
					GD._participants.clear();
					
					android.util.Log.v("Temp", "=========");
					
					for(int i = 0; i < p.length(); ++i)
					{
						/*public int _id;//���UA��ID
						public int _type;//���UA������
						public String _name;//���UA������
						public boolean _has_answered;//�Ƿ��ѽ���				
						public boolean _is_speaker;//�Ƿ�����
						public boolean _is_video_source;//�Ƿ���ƵԴ*/
						
						JSONObject ua_obj = p.getJSONObject(i);
						
						//����У��
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
								+ ((ua._has_answered) ? "�ѽ���" : "δ����") + " " + ((ua._is_speaker) ? "������" : "����") + " " + ((ua._is_video_source) ? "��ƵԴ" : "����ƵԴ"));
						
						GD._participants.put(ua_obj.getString("id"), ua);
					}
				}
			}
			else if(true == root.getString("t").equalsIgnoreCase("send_video"))//���뷢����Ƶ״̬
			{
				/*{
					"t":"send_video"
				}*/
				android.util.Log.v("UA", "MESSAGE JSON: send_video");
				GD._i_am_video_source = true;//SP.set(SipdroidReceiver.mContext, SP.PREF_IS_VIDEO_SOURCE, true);
				MessageHandlerManager.get_instance().handle_message(GID.MSG_SEND_VIDEO, GD.MEDIA_INSTANCE);
			}
			else if(true == root.getString("t").equalsIgnoreCase("recv_video"))//���������Ƶ״̬
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
