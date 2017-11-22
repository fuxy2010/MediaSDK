package com.nercms.schedule.sip.engine.sipua;

import com.nercms.schedule.sip.stack.sip.provider.SipStack;

import android.content.Context;
import android.util.Log;

public class SP
{
	private static final String LOG_TAG = "SIP";
	public static final String PREFERENCE_NAME = "com.nercms.schedule.sip.engine.sipua_preferences";

	// 服务器设置
	/*
	 * admin_server 管理（注册）服务器域名或IP，如读取此项结果为空则程序自动弹出设置界面 admin_server_port
	 * 管理（注册）服务器域名或IP location_server 定位服务器域名或IP location_server_port 定位服务器端口
	 * ftp_server 取证文件FTP服务器域名或IP
	 */
	/*
	 * public static String _admin_server = null;//默认值必须为空，程序获取不到此参数则强制进入设置界面
	 * public static int _admin_server_port = 80; public static String
	 * _location_server = "http://192.168.101.38"; public static int
	 * _location_server_port = 80;
	 * 
	 * public static void update_server_config(Context context) { _admin_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getString("admin_server", null);//默认值必须为空，程序获取不到此参数则强制进入设置界面
	 * _admin_server_port = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("admin_server_port", 80); _location_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
	 * .getString("location_server", "http://nercms.gicp.net");
	 * _location_server_port = context.getSharedPreferences(PREFERENCE_NAME,
	 * Context.MODE_PRIVATE) .getInt("location_server_port", 80); // _ftp_server
	 * = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) //
	 * .getString("ftp_server", "ftp://nercms.gicp.net"); }
	 * 
	 * //GPS设置 /* gps_sample_interval GPS采样更新间隔（毫秒） 2 000秒 gps_min_distance
	 * GPS采样距离间隔（米） 20米 gps_max_accuracy GPS采样最大精度误差（米） 35米 gps_try_time
	 * GPS采样请求时长（毫秒），如在此时间内未采集到GPS信号则转为AGPS方式 60 000毫秒 gps_switch_time
	 * GPS采样方式切换时长（毫秒），在AGPS采样期间每隔在此时长则尝试一次GPS采样，如成功则转入GPS方式 600 000秒
	 * gps_validity_period GPS数据保存时长（天），数据库中超过此时长且已上传的GPS数据将被自动删除 1天
	 * gps_upload_interval GPS上传调用webservice间隔 60 000毫秒
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
	 * // FTP设置 /*
	 * 
	 * ftp_server FTP 服务器地址 ftp_server_port FTP 服务器端口号 21 ftp_server_user FTP
	 * 登陆用户名 ftp_server_pass FTP 登陆密码 file_auto_delete 自动删除 false
	 * file_expire_time 超期时间（秒） 86400
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
	public static final String PREF_LOCATION_SERVER = "location_server";//定位服务器IP
	public static final String PREF_PORT = "port";
	public static final String PREF_PROTOCOL = "protocol";
	public static final String PREF_WLAN = "wlan";
	public static final String PREF_3G = "3g";
	public static final String PREF_EDGE = "edge";
	//public static final String PREF_IS_VIDEO_SOURCE = "is_video_source";
	public static final String PREF_ID_BY_IMSI = "id_by_imsi";
	//public static final String PREF_ANSWER_RECV_VIDEO = "answer_rec_video"; // 是否响应接收视频的消息
	//public static final String PREF_ANSWER_SEND_VIDEO = "answer_send_video"; // 是否响应发送视频的消息
	//public static final String PREF_SERVER_NAT_PORT = "server_nat_port"; // 服务器接受udp数据包的端口
	public static final String RUNNING = "running"; //程序是否启动 boolean
	public static final String LAST_UPDATA_CONFIGURE_TIME = "last_updata_configure_time"; //上次更新配置项的时间
	public static final String LAST_UPDATE_ORGNIZATION_TIMESTAMP = "last_update_orgnization_timestamp";//上次更新下属组织机构人员的时戳
	public static final String LAST_UPDATE_GROUP_TIMESTAMP = "last_update_group_timestamp";//上次更新下属分组人员的时戳
	public static final String LAST_UPDATE_TASK_TIMESTAMP = "last_update_task_timestamp";//上次更新任务状态人员的时戳
	
	//版本信息
	public static final String VERSION = "version";
	public static final String VERSION_TIPS = "version_tips";
	public static final String VERSION_URL = "version_url";
	
	//业务控制参数
	//public static final String PREF_SCHEDULE_SERVER = "schedule_server"; //调度服务器IP 程序重启后生效
	public static final String PREF_SERVICE_ON = "service_on";//所有调度按服务是否开启, "true" or "false" 程序重启后生效
	public static final String PREF_AUDIO_SEND_ON = "audio_send_on";//是否发送音频, "true" or "false" 重开会议后生效
	public static final String PREF_AUDIO_RECV_ON = "audio_recv_on";//是否接收音频, "true" or "false" 重开会议时生效
	public static final String PREF_VIDEO_SEND_ON = "video_send_on";//是否发送视频, "true" or "false" 重开会议时生效
	public static final String PREF_VIDEO_RECV_ON = "video_recv_on";//是否接收视频, "true" or "false" 重开会议时生效
	public static final String PREF_GPS_RECORD_ON = "gps_record_on";//是否将采集到的GPS数据写入数据库, "true" or "false" 立即生效
	public static final String PREF_GPS_UPLOAD_ON = "gps_upload_on";//是否上传GPS数据, "true" or "false" 立即生效
	public static final String PREF_FORCE_PLUG_ON = "force_plug_on";//是否开启强插/拆
	public static final String PREF_TXT_SCHEDUEL_ON = "txt_scheduel_on";//是否开启文本调度
	public static final String PREF_SCHEDULE_LEVEL = "schedule_level";//调度级别
	public static final String PREF_SUPER_PHONE_NUMBER = "super_phone_number";//强插电话号码（手机号码或ID）字符串 立即生效 
	public static final String PREF_GPS_UPLOAD_INTERVAL = "gps_upload_interval";//GPS上传时间间隔
	public static final String PREF_GPS_MIN_DISTANCE = "gps_min_distance";//GPS最小采样距离间隔
	//警务终端1业务单项开关，形如"000000000"，每一个字符代表一项业务的开启（为"1"）和关闭（为"0"），目前共8项业务依次为资源发现、统一通信、智能调度、警务通、电子巡更、执法监督、现场取证、应急处理和一键拨号
	public static final String PREF_SINGLE_SERVICE = "single_service_swtich";
	public static final String PREF_LAST_VIDEO_SOURCE = "last_video_source";//上一次调度中选择的视频源ID
	
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
	public static final boolean DEFAULT_KEEPON = true;  //找图标红点时修改
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
