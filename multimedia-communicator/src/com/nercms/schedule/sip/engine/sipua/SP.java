package com.nercms.schedule.sip.engine.sipua;

import com.nercms.schedule.sip.stack.sip.provider.SipStack;

import android.content.Context;
import android.util.Log;

public class SP
{
	private static final String LOG_TAG = "SIP";
	public static final String PREFERENCE_NAME = "com.nercms.schedule.sip.engine.sipua_preferences";

	// ����������
	/*
	 * admin_server ����ע�ᣩ������������IP�����ȡ������Ϊ��������Զ��������ý��� admin_server_port
	 * ����ע�ᣩ������������IP location_server ��λ������������IP location_server_port ��λ�������˿�
	 * ftp_server ȡ֤�ļ�FTP������������IP
	 */
	/*
	 * public static String _admin_server = null;//Ĭ��ֵ����Ϊ�գ������ȡ�����˲�����ǿ�ƽ������ý���
	 * public static int _admin_server_port = 80; public static String
	 * _location_server = "http://192.168.101.38"; public static int
	 * _location_server_port = 80;
	 * 
	 * public static void update_server_config(Context context) { _admin_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getString("admin_server", null);//Ĭ��ֵ����Ϊ�գ������ȡ�����˲�����ǿ�ƽ������ý���
	 * _admin_server_port = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("admin_server_port", 80); _location_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getString("location_server", "http://nercms.gicp.net");
	 * _location_server_port = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("location_server_port", 80); // _ftp_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) //
	 * .getString("ftp_server", "ftp://nercms.gicp.net"); }
	 * 
	 * //GPS���� /* gps_sample_interval GPS�������¼�������룩 2 000�� gps_min_distance
	 * GPS�������������ף� 20�� gps_max_accuracy GPS������󾫶����ף� 35�� gps_try_time
	 * GPS��������ʱ�������룩�����ڴ�ʱ����δ�ɼ���GPS�ź���תΪAGPS��ʽ 60 000���� gps_switch_time
	 * GPS������ʽ�л�ʱ�������룩����AGPS�����ڼ�ÿ���ڴ�ʱ������һ��GPS��������ɹ���ת��GPS��ʽ 600 000��
	 * gps_validity_period GPS���ݱ���ʱ�����죩�����ݿ��г�����ʱ�������ϴ���GPS���ݽ����Զ�ɾ�� 1��
	 * gps_upload_interval GPS�ϴ�����webservice��� 60 000����
	 */
	/*
	 * public static long _gps_sample_interval = 2000;; public static long
	 * _gps_min_distance = 20; public static float _gps_max_accuracy = 50;
	 * public static long _gps_try_time = 60000; public static long
	 * _gps_switch_time = 600000; public static int _gps_validity_period = 1;
	 * public static int _gps_upload_interval = 60000;
	 * 
	 * public static void update_gps_sample_config(Context context) {
	 * _gps_sample_interval = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getLong("gps_sample_interval", 2000);
	 * _gps_min_distance = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getLong("gps_min_distance", 20); _gps_max_accuracy
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getFloat("gps_max_accuracy", 50); _gps_try_time =
	 * context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getLong("gps_try_time", 60000); _gps_switch_time =
	 * context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getLong("gps_switch_time", 600000);
	 * 
	 * //Log.v(LOG_TAG, "update_gps_sample_config"); }
	 * 
	 * public static void update_gps_upload_config(Context context) {
	 * _gps_validity_period = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("gps_validity_period", 1);
	 * _gps_upload_interval = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("gps_upload_interval", 60000);
	 * 
	 * //Log.v(LOG_TAG, "update_gps_upload_config"); }
	 * 
	 * // FTP���� /*
	 * 
	 * ftp_server FTP ��������ַ ftp_server_port FTP �������˿ں� 21 ftp_server_user FTP
	 * ��½�û��� ftp_server_pass FTP ��½���� file_auto_delete �Զ�ɾ�� false
	 * file_expire_time ����ʱ�䣨�룩 86400
	 */
	/*
	 * public static String _ftp_server = "ftp://nercms.gicp.net"; public static
	 * int _ftp_server_port = 21; public static String _ftp_server_user = null;
	 * public static String _ftp_server_pass = null; public static Boolean
	 * _file_auto_delete = false; public static int _file_expire_time = 86400;
	 * 
	 * public static void update_ftp_config(Context context) { _ftp_server =
	 * context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getString("ftp_server", "ftp://nercms.gicp.net");
	 * _ftp_server_port = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getInt("ftp_server_port", 21); _ftp_server_user =
	 * context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getString("ftp_server_user", ""); _ftp_server_pass
	 * = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getString("ftp_server_pass", ""); _file_auto_delete
	 * = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getBoolean("file_auto_delete", false);
	 * _file_expire_time = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE).getInt("file_expire_time", 86400);
	 * 
	 * }
	 */

	// boolean
	public static void set(Context context, String key, boolean value) {
		synchronized (SP.class) {
			context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
					.edit().putBoolean(key, value).commit();
		}
	}

	public static boolean get(Context context, String key,
			boolean default_value) {
		return context.getSharedPreferences(PREFERENCE_NAME,
				Context.MODE_PRIVATE).getBoolean(key, default_value);
	}

	// int
	public static void set(Context context, String key, int value) {
		synchronized (SP.class) {
			context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
					.edit().putInt(key, value).commit();
		}
	}

	public static int get(Context context, String key,
			int default_value) {
		return context.getSharedPreferences(PREFERENCE_NAME,
				Context.MODE_PRIVATE).getInt(key, default_value);
	}

	// long
	public static void set(Context context, String key, long value) {
		synchronized (SP.class) {
			context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
					.edit().putLong(key, value).commit();
		}
	}

	public static long get(Context context, String key,
			long default_value) {
		return context.getSharedPreferences(PREFERENCE_NAME,
				Context.MODE_PRIVATE).getLong(key, default_value);
	}

	// float
	public static void set(Context context, String key, float value) {
		synchronized (SP.class) {
			context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
					.edit().putFloat(key, value).commit();
		}
	}

	public static float get(Context context, String key,
			float default_value) {
		return context.getSharedPreferences(PREFERENCE_NAME,
				Context.MODE_PRIVATE).getFloat(key, default_value);
	}

	// String
	public static void set(Context context, String key, String value) {
		synchronized (SP.class) {
			context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
					.edit().putString(key, value).commit();
		}
	}

	public static String get(Context context, String key, String default_value)
	{
		return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).getString(key, default_value);
	}

	// important
	//public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWORD = "password";
	public static final String PREF_LOCATION_SERVER = "location_server";//��λ������IP
	public static final String PREF_PORT = "port";
	public static final String PREF_PROTOCOL = "protocol";
	public static final String PREF_WLAN = "wlan";
	public static final String PREF_3G = "3g";
	public static final String PREF_EDGE = "edge";
	//public static final String PREF_IS_VIDEO_SOURCE = "is_video_source";
	public static final String PREF_ID_BY_IMSI = "id_by_imsi";
	//public static final String PREF_ANSWER_RECV_VIDEO = "answer_rec_video"; // �Ƿ���Ӧ������Ƶ����Ϣ
	//public static final String PREF_ANSWER_SEND_VIDEO = "answer_send_video"; // �Ƿ���Ӧ������Ƶ����Ϣ
	//public static final String PREF_SERVER_NAT_PORT = "server_nat_port"; // ����������udp���ݰ��Ķ˿�
	public static final String RUNNING = "running"; //�����Ƿ����� boolean
	public static final String LAST_UPDATA_CONFIGURE_TIME = "last_updata_configure_time"; //�ϴθ����������ʱ��
	public static final String LAST_UPDATE_ORGNIZATION_TIMESTAMP = "last_update_orgnization_timestamp";//�ϴθ���������֯������Ա��ʱ��
	public static final String LAST_UPDATE_GROUP_TIMESTAMP = "last_update_group_timestamp";//�ϴθ�������������Ա��ʱ��
	public static final String LAST_UPDATE_TASK_TIMESTAMP = "last_update_task_timestamp";//�ϴθ�������״̬��Ա��ʱ��
	
	//�汾��Ϣ
	public static final String VERSION = "version";
	public static final String VERSION_TIPS = "version_tips";
	public static final String VERSION_URL = "version_url";
	
	//ҵ����Ʋ���
	//public static final String PREF_SCHEDULE_SERVER = "schedule_server"; //���ȷ�����IP ������������Ч
	public static final String PREF_SERVICE_ON = "service_on";//���е��Ȱ������Ƿ���, "true" or "false" ������������Ч
	public static final String PREF_AUDIO_SEND_ON = "audio_send_on";//�Ƿ�����Ƶ, "true" or "false" �ؿ��������Ч
	public static final String PREF_AUDIO_RECV_ON = "audio_recv_on";//�Ƿ������Ƶ, "true" or "false" �ؿ�����ʱ��Ч
	public static final String PREF_VIDEO_SEND_ON = "video_send_on";//�Ƿ�����Ƶ, "true" or "false" �ؿ�����ʱ��Ч
	public static final String PREF_VIDEO_RECV_ON = "video_recv_on";//�Ƿ������Ƶ, "true" or "false" �ؿ�����ʱ��Ч
	public static final String PREF_GPS_RECORD_ON = "gps_record_on";//�Ƿ񽫲ɼ�����GPS����д�����ݿ�, "true" or "false" ������Ч
	public static final String PREF_GPS_UPLOAD_ON = "gps_upload_on";//�Ƿ��ϴ�GPS����, "true" or "false" ������Ч
	public static final String PREF_FORCE_PLUG_ON = "force_plug_on";//�Ƿ���ǿ��/��
	public static final String PREF_TXT_SCHEDUEL_ON = "txt_scheduel_on";//�Ƿ����ı�����
	public static final String PREF_SCHEDULE_LEVEL = "schedule_level";//���ȼ���
	public static final String PREF_SUPER_PHONE_NUMBER = "super_phone_number";//ǿ��绰���루�ֻ������ID���ַ��� ������Ч 
	public static final String PREF_GPS_UPLOAD_INTERVAL = "gps_upload_interval";//GPS�ϴ�ʱ����
	public static final String PREF_GPS_MIN_DISTANCE = "gps_min_distance";//GPS��С����������
	//�����ն�1ҵ����أ�����"000000000"��ÿһ���ַ�����һ��ҵ��Ŀ�����Ϊ"1"���͹رգ�Ϊ"0"����Ŀǰ��8��ҵ������Ϊ��Դ���֡�ͳһͨ�š����ܵ��ȡ�����ͨ������Ѳ����ִ���ල���ֳ�ȡ֤��Ӧ�������һ������
	public static final String PREF_SINGLE_SERVICE = "single_service_swtich";
	public static final String PREF_LAST_VIDEO_SOURCE = "last_video_source";//��һ�ε�����ѡ�����ƵԴID
	
	// All possible values of the PREF_PREF preference (see bellow)
	public static final String VAL_PREF_PSTN = "PSTN";
	public static final String VAL_PREF_SIP = "SIP";
	public static final String VAL_PREF_SIPONLY = "SIPONLY";
	public static final String VAL_PREF_ASK = "ASK";

	// Name of the keys in the Preferences XML file
	public static final String PREF_DOMAIN = "domain";
	public static final String PREF_FROMUSER = "fromuser";
	public static final String PREF_VPN = "vpn";
	public static final String PREF_PREF = "pref";
	public static final String PREF_AUTO_ON = "auto_on";
	public static final String PREF_AUTO_ONDEMAND = "auto_on_demand";
	public static final String PREF_AUTO_HEADSET = "auto_headset";
	public static final String PREF_MWI_ENABLED = "MWI_enabled";
	public static final String PREF_REGISTRATION = "registration";
	public static final String PREF_NOTIFY = "notify";
	public static final String PREF_NODATA = "nodata";
	public static final String PREF_SIPRINGTONE = "sipringtone";
	public static final String PREF_SEARCH = "search";
	public static final String PREF_EXCLUDEPAT = "excludepat";
	public static final String PREF_EARGAIN = "eargain";
	public static final String PREF_MICGAIN = "micgain";
	public static final String PREF_HEARGAIN = "heargain";
	public static final String PREF_HMICGAIN = "hmicgain";
	public static final String PREF_OWNWIFI = "ownwifi";
	public static final String PREF_STUN = "stun";
	public static final String PREF_STUN_SERVER = "stun_server";
	public static final String PREF_STUN_SERVER_PORT = "stun_server_port";

	// MMTel configurations (added by mandrajg)
	public static final String PREF_MMTEL = "mmtel";
	public static final String PREF_MMTEL_QVALUE = "mmtel_qvalue";

	public static final String PREF_PAR = "par";
	public static final String PREF_IMPROVE = "improve";
	public static final String PREF_POSURL = "posurl";
	public static final String PREF_POS = "pos";
	public static final String PREF_CALLBACK = "callback";
	public static final String PREF_CALLTHRU = "callthru";
	public static final String PREF_CALLTHRU2 = "callthru2";
	public static final String PREF_CODECS = "codecs_new";
	//fym public static final String PREF_DNS = "dns";
	public static final String PREF_VQUALITY = "vquality";
	public static final String PREF_MESSAGE = "vmessage";
	public static final String PREF_BLUETOOTH = "bluetooth";
	public static final String PREF_KEEPON = "keepon";
	public static final String PREF_SELECTWIFI = "selectwifi";
	public static final String PREF_ACCOUNT = "account";

	// Default values of the preferences
	public static final String DEFAULT_USERNAME = "";
	public static final String DEFAULT_PASSWORD = "";
	//fympublic static final String DEFAULT_SERVER = "pbxes.org";
	public static final String DEFAULT_DOMAIN = "";
	public static final String DEFAULT_FROMUSER = "";
	public static final String DEFAULT_PORT = "" + SipStack.default_port;
	public static final String DEFAULT_PROTOCOL = "tcp";
	public static final boolean DEFAULT_WLAN = true;
	public static final boolean DEFAULT_3G = false;
	public static final boolean DEFAULT_EDGE = false;
	public static final boolean DEFAULT_VPN = false;
	public static final String DEFAULT_PREF = VAL_PREF_SIP;
	public static final boolean DEFAULT_AUTO_ON = false;
	public static final boolean DEFAULT_AUTO_ONDEMAND = false;
	public static final boolean DEFAULT_AUTO_HEADSET = false;
	public static final boolean DEFAULT_MWI_ENABLED = true;
	public static final boolean DEFAULT_REGISTRATION = true;
	public static final boolean DEFAULT_NOTIFY = false;
	public static final boolean DEFAULT_NODATA = false;
	public static final String DEFAULT_SIPRINGTONE = "";
	public static final String DEFAULT_SEARCH = "";
	public static final String DEFAULT_EXCLUDEPAT = "";
	public static final float DEFAULT_EARGAIN = (float) 0.25;
	public static final float DEFAULT_MICGAIN = (float) 0.25;
	public static final float DEFAULT_HEARGAIN = (float) 0.25;
	public static final float DEFAULT_HMICGAIN = (float) 1.0;
	public static final boolean DEFAULT_OWNWIFI = false;
	public static final boolean DEFAULT_STUN = false;
	public static final String DEFAULT_STUN_SERVER = "stun.ekiga.net";
	public static final String DEFAULT_STUN_SERVER_PORT = "3478";

	// MMTel configuration (added by mandrajg)
	public static final boolean DEFAULT_MMTEL = false;
	public static final String DEFAULT_MMTEL_QVALUE = "1.00";

	public static final boolean DEFAULT_PAR = false;
	public static final boolean DEFAULT_IMPROVE = false;
	public static final String DEFAULT_POSURL = "";
	public static final boolean DEFAULT_POS = false;
	public static final boolean DEFAULT_CALLBACK = false;
	public static final boolean DEFAULT_CALLTHRU = false;
	public static final String DEFAULT_CALLTHRU2 = "";
	public static final String DEFAULT_CODECS = null;
	public static final String DEFAULT_DNS = "";
	public static final String DEFAULT_VQUALITY = "low";
	public static final boolean DEFAULT_MESSAGE = false;
	public static final boolean DEFAULT_BLUETOOTH = false;
	public static final boolean DEFAULT_KEEPON = true;  //��ͼ����ʱ�޸�
  	public static final boolean DEFAULT_SELECTWIFI = false;
	public static final int DEFAULT_ACCOUNT = 0;

	// An other preference keys (not in the Preferences XML file)
	public static final String PREF_OLDVALID = "oldvalid";
	public static final String PREF_SETMODE = "setmode";
	public static final String PREF_OLDVIBRATE = "oldvibrate";
	public static final String PREF_OLDVIBRATE2 = "oldvibrate2";
	public static final String PREF_OLDPOLICY = "oldpolicy";
	public static final String PREF_OLDRING = "oldring";
	public static final String PREF_AUTO_DEMAND = "auto_demand";
	public static final String PREF_WIFI_DISABLED = "wifi_disabled";
	public static final String PREF_ON_VPN = "on_vpn";
	public static final String PREF_NODEFAULT = "nodefault";
	public static final String PREF_ON = "on";
	public static final String PREF_PREFIX = "prefix";
	public static final String PREF_COMPRESSION = "compression";
	// public static final String PREF_RINGMODEx = "ringmodeX";
	// public static final String PREF_VOLUMEx = "volumeX";

	// Default values of the other preferences
	public static final boolean DEFAULT_OLDVALID = false;
	public static final boolean DEFAULT_SETMODE = false;
	public static final int DEFAULT_OLDVIBRATE = 0;
	public static final int DEFAULT_OLDVIBRATE2 = 0;
	public static final int DEFAULT_OLDPOLICY = 0;
	public static final int DEFAULT_OLDRING = 0;
	public static final boolean DEFAULT_AUTO_DEMAND = false;
	public static final boolean DEFAULT_WIFI_DISABLED = false;
	public static final boolean DEFAULT_ON_VPN = false;
	public static final boolean DEFAULT_NODEFAULT = false;
	public static final boolean DEFAULT_ON = false;
	public static final String DEFAULT_PREFIX = "";
	public static final String DEFAULT_COMPRESSION = null;
	// public static final String DEFAULT_RINGTONEx = "";
	// public static final String DEFAULT_VOLUMEx = "";
}
