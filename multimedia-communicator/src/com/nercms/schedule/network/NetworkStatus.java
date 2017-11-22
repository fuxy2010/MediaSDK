package com.nercms.schedule.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.misc.MediaStatistics;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.RegisterService;
import com.nercms.schedule.sip.stack.net.IpAddress;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.ui.MessageHandlerManager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class NetworkStatus//rmv extends Service
{
	public static Object _exit_flag = new Object();
	
	/**
	 * 网络为GPRS时，级别为0；
	 * 网络为EDGE时，级别为1；
     * 网络为3G或HSDPA/HSUPA时，级别为2；
     * 网络为WIFI时，级别为2；
	 */
	private static int _network_type = 2; //网络类型，2-H/3G/WIFI, 1-EDGE, 0-GPRS, -1-未知
	private static int _network_status = 2; //网络状态, 2-正常， 1-较差， 0-极差, -1-网络未知或初始状态, -2-网络断开, -3-IP变更
	
	//private boolean _available = true;
	
	private static final String LOG_TAG = "Network";
	
	private static DecimalFormat _df = new DecimalFormat( "0.0000");
	
	//private static Timer _network_status_timer = null;
	//private static TimerTask _network_status_task = null;
	
	//每隔15秒检测一次网络等级
	private final static long SCHEDULE_CHECK_INTERVAL = 15000;
	
	//调度中每隔5秒检测一次CPU利用率，空闲中则间隔增大10倍
	private final static long PERFORMANCE_CHECK_INTERVAl = 5000;

	private static ScheduledExecutorService _network_status_service = null;
	
	//获得网络类型
	public static int get_network_type()
	{
		return _network_type;
	}
	
	//获得网络状态
	public static int get_network_status()
	{
		return _network_status;
	}
	
	private static int get_access_network_type()
	{
		ConnectivityManager connectivity_manager = (ConnectivityManager)GD.get_global_context().getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if(null == connectivity_manager)
			return -1;
				
		NetworkInfo network_info = connectivity_manager.getActiveNetworkInfo();
			 	
	 	if(null == network_info)
	 		return -1;
			 	
	 	return network_info.getType();
	}
	
	//获取网络等级
	//返回值2-HSPA/3G/WIFI, 1-EDGE, 0-GPRS, -1-unknown
	public static int get_current_network_type()
	{
		ConnectivityManager connectivity_manager = (ConnectivityManager) GD.get_global_context().getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if(null == connectivity_manager)
			return -1;
				
		NetworkInfo network_info = connectivity_manager.getActiveNetworkInfo();
			 	
	 	if(null == network_info)
	 		return -1;
			 	
	 	if(ConnectivityManager.TYPE_WIFI == network_info.getType())//WIFI网络
	 	{
	 		return 2;
	 	}
	 	else if(ConnectivityManager.TYPE_MOBILE == network_info.getType())//Mobile网络
	 	{
	 		//Mobile
	 		switch(network_info.getSubtype())
	 		{
	 			case GD.NETWORK_TYPE_UNKNOWN:
		 			return -1;
		 			
		 		//G
		 		case GD.NETWORK_TYPE_GPRS:
		 			return 0;
	 		
		 		//E
		 		case GD.NETWORK_TYPE_EDGE:
		 		case GD.NETWORK_TYPE_CDMA:
		 		case GD.NETWORK_TYPE_1xRTT:
		 		case GD.NETWORK_TYPE_IDEN:
		 			return 1;
			 			
		 		//H
		 		case GD.NETWORK_TYPE_UMTS:
		 		case GD.NETWORK_TYPE_HSDPA:
		 		case GD.NETWORK_TYPE_HSUPA:
		 		case GD.NETWORK_TYPE_HSPA:
		 		case GD.NETWORK_TYPE_EVDO_0:
		 		case GD.NETWORK_TYPE_EVDO_A:
		 			return 2;
		 			
		 		default:
		 			return 2;
	 		}
	 	}
			 	
	 	return -1;
	}
	
	//网络等级发生改变时
	private static void on_network_type_change()
	{
		Log.i(GD.LOG_TAG, "NetworkAdaptive: network change");
			
		//根据网络登记修改NAT探测间隔
		switch(_network_type)
		{
		case 0:
		default:
			GD.SCHEDULE_REGISTER_INTERVAL = 60000;
			//SipPreference.save_parameter(context, SipPreference.PREF_GPS_UPLOAD_INTERVAL, Integer.parseInt(value));
			break;
		
		case 1:
			GD.SCHEDULE_REGISTER_INTERVAL = 45000;
			//SipPreference.save_parameter(context, SipPreference.PREF_GPS_UPLOAD_INTERVAL, Integer.parseInt(value));
			break;
		
		case 2:
			GD.SCHEDULE_REGISTER_INTERVAL = 45000;
			//SipPreference.save_parameter(context, SipPreference.PREF_GPS_UPLOAD_INTERVAL, Integer.parseInt(value));
			break;
		}
				
		//MessageHandlerManager.get_instance().handle_message(GeneralDefine.MSG_NETWORK_LEVEL_VARIETY,_network_type,GeneralDefine.SCHEDULEMAP);
		
		//立即通知调度服务器
		//网络类型改变状态一般也会改变 RegisterService.register_to_schedule_server_at_once();
	}
	
	//获取当前网络状态
	public static int get_current_network_status()
	{
		if(true)
		{
			if(2 == _network_type)//H/3G/WIFI
			{
				if(1000.0 >= _average_ping_delay)
					return 2;
				else if(2000.0 >= _average_ping_delay && 1000.0 < _average_ping_delay)
					return 1;
				else
					return 0;
			}
			else//E/G
			{
				if(1000.0 >= _average_ping_delay)
					return 1;
				else
					return 0;
			}
		}
		
		//根据网络状态及丢包率调整编码策略
	 	/*1. 网络类型为H/3G/WIFI
		    1.1 视频源手机
		        音频接收丢包率[0%, 1%)时，则发送正常视频（自适应I帧或定时I帧），发送正常音频（如果是发言人，下同），通知服务器网络类型且状态正常；
		        音频接收丢包率[1%, 2%)时，则只发送I帧（自适应I帧或定时I帧），发送正常音频，界面提示网络较差，通知服务器网络类型且状态较差；
		        音频接收丢包率[2%, 100%)时，则每隔3秒发送一次I帧，发送正常音频，界面提示网络极差，通知服务器网络类型且状态极差；

		    1.2 非视频源手机
		        音频及视频都接收丢包率都小于1%时，则发送正常音频（如果是发言人，下同），通知服务器网络类型且状态正常；
		        音频丢包率[1%, 2%)或视频丢包率[2%, 5%)，则发送正常音频，界面提示网络较差，通知服务器网络类型且状态较差；
		        音频丢包率[2%, 100%)或视频丢包率[5%, 100%)，则发送正常音频，界面提示网络极差，通知服务器网络类型且状态极差；

		2. 网络类型为E/G
		    2.1 视频源手机
		        音频接收丢包率[0%, 1%)时，则每隔2秒发送一次I帧，发送正常音频（如果是发言人，下同），界面提示网络较差，通知服务器网络类型且状态较差；
		       音频接收丢包率[1%, 100%)时，则每隔3秒发送一次I帧，发送正常音频，，界面提示网络极差，通知服务器网络类型且状态极差；

		    2.2 非视频源手机
		        音频及视频都接收丢包率都小于1%时，通知服务器网络较差，发送正常音频（如果是发言人，下同）；
		        音频丢包率[1%, 100%)或视频丢包率[1%, 100%)，通知服务器网络极差，发送正常音频；

		服务器策略
		1.如按调度控制需向该手机发送音频，仅当该手机为E/G网络且网络极差时不发送音频；
		2.如按调度控制需向该手机发送视频，无论手机网络类型如何，当网络状态正常则全部发送，如网络较差则只发送I帧，如网络极差 则只发送I帧且确保相邻两I帧间隔不得小于3秒（需主动丢弃部分I帧）。*/
		
		MediaStatistics statistics = MediaThreadManager.get_instance().get_media_statistics(0);
		
		/*//先根据一定间隔内（15秒）收到的音频包数判断网络状态
		//15秒内正常应收到15 * (1000 / 60) = 250个音频包
		long packet_count = statistics.get_packet_count();
		if(0 <= packet_count && 240 >= packet_count)
		{
			return 0;
		}
		else if(240 < packet_count && 245 >= packet_count)
		{
			return 1;
		}
		//只有15秒内收到超过240个包才再根据丢包率判断网路状态*/ 
		
		//获取音频丢包率
		double audio_packet_lost_rate = (null != statistics) ? statistics.get_packet_lost_rate() : 0.0;		
		
		//获取视频丢包率
		statistics = MediaThreadManager.get_instance().get_media_statistics(1);
		double video_packet_lost_rate = (null != statistics) ? statistics.get_packet_lost_rate() : 0.0;
		
		if(true == GD.NO_RENDERING_DECODE_VIDEO)
			video_packet_lost_rate = 0.0;
		
		int network_status = 1;
		
		//获取网络状态
		if(2 == _network_type)//H/3G/WIFI
		{
			//要考虑网络状态为1或0时因服务器不对其发送I帧导致统计意义上的丢包率很高
			//if(0.7 > audio_packet_lost_rate && ((2.0 > video_packet_lost_rate && 2 == _network_status) || 2 != _network_status))
			//if(0.7 > audio_packet_lost_rate)
			if(0.7 > audio_packet_lost_rate && 2.0 > video_packet_lost_rate)
			{
				//Log.v("Network", "网络正常 AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
				network_status = 2;//网络正常
			}
			/*else if((2.0 > audio_packet_lost_rate && 0.7 <= audio_packet_lost_rate) ||
					(5.0 > video_packet_lost_rate && 2.0 <= video_packet_lost_rate))
			{
				network_status = 1;//网络较差
			}*/
			//else if(2.0 <= audio_packet_lost_rate || (5.0 <= video_packet_lost_rate && 2 == _network_status))
			else if(2.0 <= audio_packet_lost_rate || 5.0 <= video_packet_lost_rate)
			{
				network_status = 0;//网络极差
				//Log.v("Network", "网络极差 AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
			else
			{
				network_status = 1;//网络较差
				//Log.v("Network", "网络较差 AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
		}
		else//E/G
		{
			//if(1.0 > audio_packet_lost_rate)//网络类型为E/G时服务器只向其发送I帧故不考虑视频丢包率 && 1.0 > video_packet_lost_rate)
			if(1.0 > audio_packet_lost_rate && 5.0 > video_packet_lost_rate)
			{
				network_status = 1;//网络较差
				Log.v("Network", "网络较差 AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
			//else if(1.0 <= audio_packet_lost_rate)//网络类型为E/G时服务器只向其发送I帧故不考虑视频丢包率 || 1.0 <= video_packet_lost_rate)
			else if(1.0 <= audio_packet_lost_rate || 5.0 <= video_packet_lost_rate)
			{
				network_status = 0;//网络极差
				Log.v("Network", "网络极差 AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
		}
		
		//Log.v("Network", "AL: " + audio_packet_lost_rate + "%, VL: " + video_packet_lost_rate + "%, S: " + _network_status);
		
		return network_status;
	}
	
	//音视频编码策略调整
	private static void adjust_codec_strategy()
	{
		if(2 == _network_status)
		{
			//视频策略调整
			GD.VIDEO_ENCODE_STRATEGY = -1;//自动
			GD.ONLY_IDR_FRAME_INTERVAL = 2000;
		}
		else if(1 == _network_status)
		{
			GD.VIDEO_ENCODE_STRATEGY = 1;//只编IDR帧
			GD.ONLY_IDR_FRAME_INTERVAL = 3000;
		}
		else if(0 == _network_status)
		{
			GD.VIDEO_ENCODE_STRATEGY = 1;//只编IDR帧
			GD.ONLY_IDR_FRAME_INTERVAL = 4500;
		}
		
		//fym
		GD.VIDEO_ENCODE_STRATEGY = -1;//
		GD.ONLY_IDR_FRAME_INTERVAL = 100;//2000;
	}
	
	private static long _latest_record_media_and_network_log = 0;
	private static void record_media_and_network_log()
	{
		//15秒记录一次
		if(15000 > (System.currentTimeMillis() - _latest_record_media_and_network_log))
			return;
		
		_latest_record_media_and_network_log = System.currentTimeMillis();
		
		//记录网络状态
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "网络类型  " + _network_type + ", 状态 " + _network_status);
		
		//记录媒体统计信息
		String statistics_snapshot = "";
		MediaStatistics statistics = null;
		
		//音频
		if(false == MediaThreadManager.get_instance()._audio_play_idle)
		{
			Log.v("Baidu", "audio statistics");
			statistics = MediaThreadManager.get_instance().get_media_statistics(0);
			if(null != statistics)
			{
				//statistics_snapshot = "Audio: L " + statistics.get_packet_lost_rate() + "%, D " + statistics.get_packet_delay() + "ms, B " + statistics.get_bitrate() + "kbps";
				statistics_snapshot += "音频丢包率" + _df.format(statistics.get_packet_lost_rate())
									+ "%, 总计" + statistics.get_packet_count()
									+ ", 相对延迟" + _df.format(statistics.get_packet_relative_delay())
									+ "ms, 绝对延迟" + _df.format(statistics.get_packet_absolute_delay())
									+ "ms, 码率" + _df.format(statistics.get_bitrate())
									+ "kbps, 间隔" + _df.format(statistics.get_averate_packet_timestamp_interval()) + "ms";
			}
		}
		//视频
		if(false == MediaThreadManager.get_instance()._video_receive_idle)
		{
			Log.v("Baidu", "video statistics");
			statistics = MediaThreadManager.get_instance().get_media_statistics(1);
			if(null != statistics)
			{
				//statistics_snapshot = "Video: L " + statistics.get_packet_lost_rate() + "%, D " + statistics.get_packet_delay() + "ms, B " + statistics.get_bitrate() + "kbps";
				statistics_snapshot += "\r\n视频丢包率" + _df.format(statistics.get_packet_lost_rate())
									+ "%, 总计" + statistics.get_packet_count()
									+ ", 相对延迟" + _df.format(statistics.get_packet_relative_delay())
									+ "ms, 绝对延迟" + _df.format(statistics.get_packet_absolute_delay())
									+ "ms, 码率" + _df.format(statistics.get_bitrate()) + "kbps";
				
			}
		}
		
		if(0 != statistics_snapshot.length())
		{
			statistics_snapshot += "\r\n";
			
			//Log.v("Media", statistics_snapshot);
			//GD.log_to_db(NetworkStatus.this, 0, "Statistics", statistics_snapshot);
		}
		
		//记录线程统计信息
		MediaThreadManager.get_instance().thread_statistics();
	}
	
	//ping服务器获取
	//PING 220.249.112.22 (220.249.112.22) 56(84) bytes of data.
	//64 bytes from 220.249.112.22: icmp_seq=1 ttl=122 time=221 ms
	//--- 220.249.112.22 ping statistics ---
	//1 packets transmitted, 1 received, 0% packet loss, time 0ms
	//rtt min/avg/max/mdev = 221.447/221.447/221.447/0.000 ms
	public static double ping_delay(String server)
	{
		try
		{
			//String server = "220.249.112.22";
			//Process p = Runtime.getRuntime().exec("ping -c 1 " + server);
			//Process p = Runtime.getRuntime().exec("ping -c 1 -w 10 " + server);
			//Process p = Runtime.getRuntime().exec("ping -c 1 -s 32 " + server);
			//Log.v("Temp", "ping before");
			long t = System.currentTimeMillis();
			
			//Process p = Runtime.getRuntime().exec("ping -c 1 -w 8000 " + SP.get(GD.get_global_context(),SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER));
			Process p = Runtime.getRuntime().exec("ping -c 1 -w 8000 " + server);
			Log.v("Temp", "ping……");
			int status = p.waitFor();
			
			//Log.v("Temp", "ping after " + (System.currentTimeMillis() - t));
			
			//读取输出流http://blog.csdn.net/dancen/article/details/7969328
			double delay = 0;
			
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    //StringBuffer buffer = new StringBuffer();
		    String line = null;
		    while(null != (line = in.readLine()))
		    {
		    	Log.v("Temp", "ping: " + line);
		    	if(0 == status)
		    	{
		    		//buffer.append(line);
			    	//Log.v("Baidu", "ping: " + line);
			    	if(true == line.contains("bytes from"))
			    	{
			    		//Log.v("Baidu", "ping: " + line);
			    		//line = line.substring(line.indexOf("time=") + 5, line.lastIndexOf(" ms"));
			    		//Log.v("Baidu", "ping delay: --" + line + "--");
			    		
			    		delay = Double.parseDouble(line.substring(line.indexOf("time=") + 5, line.lastIndexOf(" ms")));
			    	}
		    	}
		    	
		    }

		    in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    line = null;
		    while(null != (line = in.readLine()))
		    {
		    }
		    
		    //Log.w("Temp", "ping delay " + (int)delay);
		    _cur_ping_delay = (int)delay;
		    
		    //if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_PING_DELAY, _cur_ping_delay);
		    
		    return delay;
		}
		catch(IOException e)
		{
			//Log.w("Temp", "ping delay 0(1)");
			_cur_ping_delay = 0;
			//if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_PING_DELAY, 0);
			return 0;
		}
		catch(InterruptedException e)
		{
			//Log.w("Temp", "ping delay 0(2)");
			_cur_ping_delay = 0;
			//if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_PING_DELAY, null);
			return 0;
		}
	}
	
	public static double _average_ping_delay = 0.0;
	private static double _total_ping_delay = 0.0;
	private static int _ping_delay_counter = 0;
	private static long _last_ping_delay_timestamp = 0;
	private static void update_ping_delay()
	{
		//空闲时5秒ping，调度中2秒ping
		if((true == GD.is_in_schedule() ? 2000 : 5000) < (System.currentTimeMillis() - _last_ping_delay_timestamp))
		{
			//Log.w("Temp", "update_ping_delay");
			//Log.v("Temp", "ping_delay() 2");
			_total_ping_delay += ping_delay(GD.DEFAULT_SCHEDULE_SERVER);
			++_ping_delay_counter;
			
			_last_ping_delay_timestamp = System.currentTimeMillis();
		}
		
		if(5 <= _ping_delay_counter)
		{
			_average_ping_delay = _total_ping_delay / 5.0;
			
			Log.v("Baidu", "ping delay " + _average_ping_delay);
			
			_total_ping_delay = 0;
			_ping_delay_counter = 0;
		}
	}
	
	private static int _force_restart_sip_media_service = 0;//是否强制重启服务
	
	public static boolean _is_restarting_media_and_sip_service = false;//是否正在重启服务
	
	private static int _cur_ping_delay = 0;
	
	private Thread _network_check_thread = new Thread()
	{
		//每隔15秒检测一次网络等级
		private final long SCHEDULE_CHECK_INTERVAL = 15000;
		
		//调度中每隔5秒检测一次CPU利用率，空闲中则间隔增大10倍
		private final long PERFORMANCE_CHECK_INTERVAl = 5000;
		
		@Override
		public void run()
		{
			
		}
	};
	
	//最近一次强制重启服务的时戳
	private static long _latest_force_restart_sip_media_service = 0;
	
	//强制重启SIP及媒体服务
	public static void force_restart_sip_media_service(int flag)
	{
		Log.v("Temp", "force_restart_sip_media_service " + flag);
		
		if(30000 <= (System.currentTimeMillis() - _latest_force_restart_sip_media_service))
		{
			_force_restart_sip_media_service = flag;
			_latest_force_restart_sip_media_service = System.currentTimeMillis();
		}
	}
	
	private static long _latest_check_network_status_timestamp = 0;
	private static long latest_check_cpu_timestamp = 0;
	
	private static String last_local_ip = "unknown";//上次获取的本机IP，初始值不能为null避免误判
	private static int last_access_network_type = -1;//上次获取的接入网络类型
	private static String current_local_ip = "";//本次获取的本机IP
	private static int current_access_network_type = -1;//本次获取的接入网络类型
	
	public static void start(Context context)
	{
		//context.startService(new Intent(context, NetworkStatus.class));
		
		/*if(null == _network_status_timer)
			_network_status_timer = new Timer();
	
		if(null != _network_status_task)
		{
			_network_status_task.cancel();
			_network_status_task = null;
		}
		
		//Log.v("Baidu", "start_realtime_update_task 2");
		
		_network_status_timer.purge();
		
		//Log.v("Baidu", "start_realtime_update_task 3");*/
		
		Thread network_status_thread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					//while(true == _available)
					{
						//Thread.sleep(100);
						
						//Log.v("Baidu", "NetworkStatus 1");
						//////////////////////////////////////////////////////////////////////////////////////////
						//Log.v("Baidu", "NetworkStatus Task run!");
						
						//通过ping感知网络状态
						//有最高8秒的延迟
						update_ping_delay();//Log.v("Baidu", "ping delay: " + ping_delay());
						
						//Log.v("Baidu", "NetworkStatus 2");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						if(0 != _force_restart_sip_media_service)//强制重启服务
						{
							_network_status = -2;
							
							//主界面提示网络状态并处理
							MessageHandlerManager.get_instance().handle_message(GID.MSG_UPDATE_NETWORK_STATUS, _network_status, _force_restart_sip_media_service, GD.MEDIA_INSTANCE);
							
							_force_restart_sip_media_service = 0;
							
							Log.v("Temp", "_force_restart_sip_media_service");
						}
						else
						{
							//主界面提示网络状态并处理
							MessageHandlerManager.get_instance().handle_message(GID.MSG_UPDATE_NETWORK_STATUS, _network_status, GD.MEDIA_INSTANCE);
						}
						
						//Log.v("Baidu", "NetworkStatus 3");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						if(-1 > _network_status)
						{
							//网络中断及IP变化时需重启网络
							Log.v("Temp", "restart_sip_media_service " + _network_status);
							
							_is_restarting_media_and_sip_service = true;
							Thread.sleep(100);						
							
							synchronized(GD._restart_lock)//避免重启时因RegisterService导致ANR错误
							{
								GD.restart_sip_media_service();
							}
							
							Thread.sleep(100);
							_is_restarting_media_and_sip_service = false;
						}
						
						//Log.v("Baidu", "NetworkStatus 5");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						/*//进程资源占用统计
						//调度中间隔为PERFORMANCE_CHECK_INTERVAl
						//空闲中间隔为10倍PERFORMANCE_CHECK_INTERVAl
						if(((true == GD.is_in_schedule()) ? PERFORMANCE_CHECK_INTERVAl : (10 * PERFORMANCE_CHECK_INTERVAl)) < (System.currentTimeMillis() - latest_check_cpu_timestamp))
						{
							PerformanceStatistics.get_cpu_usage();
							PerformanceStatistics.get_process_memory();
							
							latest_check_cpu_timestamp = System.currentTimeMillis();
						}*/
						
						//Log.v("Baidu", "NetworkStatus 6");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//获取本机IP和接入网络类型
						IpAddress.setLocalIpAddress();//适应IP地址可能发生变化的情况
						current_local_ip = IpAddress.localIpAddress;
						
						//如获取为127.0.0.1，则实为无法获取IP
						current_local_ip = (true == current_local_ip.equalsIgnoreCase("127.0.0.1") ? "" : current_local_ip);
						
						//获取接入网络类型
						current_access_network_type = get_access_network_type();
						
						//Log.v("Baidu", current_local_ip + ", " + current_access_network_type);
						
						if(null == current_local_ip || current_local_ip.equals(""))//无法获取IP则判断为网络中断
						{
							_network_status = -2;						
							return;//continue;
						}
						else//IP获取正常
						{
							if(0 == _cur_ping_delay)//ping不通也是中断
							{
								_network_status = -2;						
								return;//continue;
							}
							
							//对last_local_ip赋初值
							if(true == last_local_ip.equalsIgnoreCase("unknown"))
							{
								last_local_ip = current_local_ip;
								last_access_network_type = current_access_network_type;
							}
							
							//IP变动
							if(false == last_local_ip.equalsIgnoreCase(current_local_ip))
							{
								//Log.v("Baidu", last_local_ip + ", " + current_local_ip);
								
								//WIFI网络下可能会检测到3G网络的IP，故在WIFI网络下IP变动不做处理
								if(ConnectivityManager.TYPE_WIFI == current_access_network_type &&
									last_access_network_type == current_access_network_type)
								{
									return;//continue;
								}
								
								last_local_ip = current_local_ip;
								last_access_network_type = current_access_network_type;
								
								_network_status = -3;
								return;//continue;
							}
						}
						
						//Log.v("Baidu", "NetworkStatus 7");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//获取网络类型
						int current_network_type = get_current_network_type();
						
						if(_network_type != current_network_type)
						{
							//网络类型变化时的处理
							on_network_type_change();
							_network_type = current_network_type;
						}
						
						//网络类型未知
						if(-1 == _network_type)
						{
							_network_status = -1;
							return;//continue;
						}
						
						//网络重启后且未到计算时间则以网络类型作为网络状态
						if(0 > _network_status)
						{
							_network_status = _network_type;
						}
						
						//Log.v("Baidu", "NetworkStatus 8");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//结合网络类型及ping delay计算网络状态
						if(SCHEDULE_CHECK_INTERVAL > (System.currentTimeMillis() - _latest_check_network_status_timestamp))
						{
							return;//continue;
						}
						
						//根据ping delay获取网络状态
						int current_network_status = get_current_network_status();
						
						if(_network_status != current_network_status)
						{
							_network_status = current_network_status;
							
							//网络类型变化时立即通知调度服务器
							RegisterService.register_to_schedule_server_at_once();
						}
						
						//Log.v("Baidu", "NetworkStatus 9");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//调整编码策略
						adjust_codec_strategy();
						
						//Log.v("Baidu", "NetworkStatus 10");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//记录网络及音视频统计日志
						//record_media_and_network_log();
					}
				}
				catch(Exception e)
				{
				}
			}
		};
		
		_network_status_service = Executors.newScheduledThreadPool(1);
		_network_status_service.scheduleAtFixedRate(network_status_thread, 100, 3000, TimeUnit.MILLISECONDS);
	}
	
	public static void stop(Context context)
	{
		_network_status_service.shutdownNow();
	}
	
	
	
}
