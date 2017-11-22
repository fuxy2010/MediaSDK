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
	 * ����ΪGPRSʱ������Ϊ0��
	 * ����ΪEDGEʱ������Ϊ1��
     * ����Ϊ3G��HSDPA/HSUPAʱ������Ϊ2��
     * ����ΪWIFIʱ������Ϊ2��
	 */
	private static int _network_type = 2; //�������ͣ�2-H/3G/WIFI, 1-EDGE, 0-GPRS, -1-δ֪
	private static int _network_status = 2; //����״̬, 2-������ 1-�ϲ 0-����, -1-����δ֪���ʼ״̬, -2-����Ͽ�, -3-IP���
	
	//private boolean _available = true;
	
	private static final String LOG_TAG = "Network";
	
	private static DecimalFormat _df = new DecimalFormat( "0.0000");
	
	//private static Timer _network_status_timer = null;
	//private static TimerTask _network_status_task = null;
	
	//ÿ��15����һ������ȼ�
	private final static long SCHEDULE_CHECK_INTERVAL = 15000;
	
	//������ÿ��5����һ��CPU�����ʣ���������������10��
	private final static long PERFORMANCE_CHECK_INTERVAl = 5000;

	private static ScheduledExecutorService _network_status_service = null;
	
	//�����������
	public static int get_network_type()
	{
		return _network_type;
	}
	
	//�������״̬
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
	
	//��ȡ����ȼ�
	//����ֵ2-HSPA/3G/WIFI, 1-EDGE, 0-GPRS, -1-unknown
	public static int get_current_network_type()
	{
		ConnectivityManager connectivity_manager = (ConnectivityManager) GD.get_global_context().getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if(null == connectivity_manager)
			return -1;
				
		NetworkInfo network_info = connectivity_manager.getActiveNetworkInfo();
			 	
	 	if(null == network_info)
	 		return -1;
			 	
	 	if(ConnectivityManager.TYPE_WIFI == network_info.getType())//WIFI����
	 	{
	 		return 2;
	 	}
	 	else if(ConnectivityManager.TYPE_MOBILE == network_info.getType())//Mobile����
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
	
	//����ȼ������ı�ʱ
	private static void on_network_type_change()
	{
		Log.i(GD.LOG_TAG, "NetworkAdaptive: network change");
			
		//��������Ǽ��޸�NAT̽����
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
		
		//����֪ͨ���ȷ�����
		//�������͸ı�״̬һ��Ҳ��ı� RegisterService.register_to_schedule_server_at_once();
	}
	
	//��ȡ��ǰ����״̬
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
		
		//��������״̬�������ʵ����������
	 	/*1. ��������ΪH/3G/WIFI
		    1.1 ��ƵԴ�ֻ�
		        ��Ƶ���ն�����[0%, 1%)ʱ������������Ƶ������ӦI֡��ʱI֡��������������Ƶ������Ƿ����ˣ���ͬ����֪ͨ����������������״̬������
		        ��Ƶ���ն�����[1%, 2%)ʱ����ֻ����I֡������ӦI֡��ʱI֡��������������Ƶ��������ʾ����ϲ֪ͨ����������������״̬�ϲ
		        ��Ƶ���ն�����[2%, 100%)ʱ����ÿ��3�뷢��һ��I֡������������Ƶ��������ʾ���缫�֪ͨ����������������״̬���

		    1.2 ����ƵԴ�ֻ�
		        ��Ƶ����Ƶ�����ն����ʶ�С��1%ʱ������������Ƶ������Ƿ����ˣ���ͬ����֪ͨ����������������״̬������
		        ��Ƶ������[1%, 2%)����Ƶ������[2%, 5%)������������Ƶ��������ʾ����ϲ֪ͨ����������������״̬�ϲ
		        ��Ƶ������[2%, 100%)����Ƶ������[5%, 100%)������������Ƶ��������ʾ���缫�֪ͨ����������������״̬���

		2. ��������ΪE/G
		    2.1 ��ƵԴ�ֻ�
		        ��Ƶ���ն�����[0%, 1%)ʱ����ÿ��2�뷢��һ��I֡������������Ƶ������Ƿ����ˣ���ͬ����������ʾ����ϲ֪ͨ����������������״̬�ϲ
		       ��Ƶ���ն�����[1%, 100%)ʱ����ÿ��3�뷢��һ��I֡������������Ƶ����������ʾ���缫�֪ͨ����������������״̬���

		    2.2 ����ƵԴ�ֻ�
		        ��Ƶ����Ƶ�����ն����ʶ�С��1%ʱ��֪ͨ����������ϲ����������Ƶ������Ƿ����ˣ���ͬ����
		        ��Ƶ������[1%, 100%)����Ƶ������[1%, 100%)��֪ͨ���������缫�����������Ƶ��

		����������
		1.�簴���ȿ���������ֻ�������Ƶ���������ֻ�ΪE/G���������缫��ʱ��������Ƶ��
		2.�簴���ȿ���������ֻ�������Ƶ�������ֻ�����������Σ�������״̬������ȫ�����ͣ�������ϲ���ֻ����I֡�������缫�� ��ֻ����I֡��ȷ��������I֡�������С��3�루��������������I֡����*/
		
		MediaStatistics statistics = MediaThreadManager.get_instance().get_media_statistics(0);
		
		/*//�ȸ���һ������ڣ�15�룩�յ�����Ƶ�����ж�����״̬
		//15��������Ӧ�յ�15 * (1000 / 60) = 250����Ƶ��
		long packet_count = statistics.get_packet_count();
		if(0 <= packet_count && 240 >= packet_count)
		{
			return 0;
		}
		else if(240 < packet_count && 245 >= packet_count)
		{
			return 1;
		}
		//ֻ��15�����յ�����240�������ٸ��ݶ������ж���·״̬*/ 
		
		//��ȡ��Ƶ������
		double audio_packet_lost_rate = (null != statistics) ? statistics.get_packet_lost_rate() : 0.0;		
		
		//��ȡ��Ƶ������
		statistics = MediaThreadManager.get_instance().get_media_statistics(1);
		double video_packet_lost_rate = (null != statistics) ? statistics.get_packet_lost_rate() : 0.0;
		
		if(true == GD.NO_RENDERING_DECODE_VIDEO)
			video_packet_lost_rate = 0.0;
		
		int network_status = 1;
		
		//��ȡ����״̬
		if(2 == _network_type)//H/3G/WIFI
		{
			//Ҫ��������״̬Ϊ1��0ʱ������������䷢��I֡����ͳ�������ϵĶ����ʺܸ�
			//if(0.7 > audio_packet_lost_rate && ((2.0 > video_packet_lost_rate && 2 == _network_status) || 2 != _network_status))
			//if(0.7 > audio_packet_lost_rate)
			if(0.7 > audio_packet_lost_rate && 2.0 > video_packet_lost_rate)
			{
				//Log.v("Network", "�������� AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
				network_status = 2;//��������
			}
			/*else if((2.0 > audio_packet_lost_rate && 0.7 <= audio_packet_lost_rate) ||
					(5.0 > video_packet_lost_rate && 2.0 <= video_packet_lost_rate))
			{
				network_status = 1;//����ϲ�
			}*/
			//else if(2.0 <= audio_packet_lost_rate || (5.0 <= video_packet_lost_rate && 2 == _network_status))
			else if(2.0 <= audio_packet_lost_rate || 5.0 <= video_packet_lost_rate)
			{
				network_status = 0;//���缫��
				//Log.v("Network", "���缫�� AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
			else
			{
				network_status = 1;//����ϲ�
				//Log.v("Network", "����ϲ� AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
		}
		else//E/G
		{
			//if(1.0 > audio_packet_lost_rate)//��������ΪE/Gʱ������ֻ���䷢��I֡�ʲ�������Ƶ������ && 1.0 > video_packet_lost_rate)
			if(1.0 > audio_packet_lost_rate && 5.0 > video_packet_lost_rate)
			{
				network_status = 1;//����ϲ�
				Log.v("Network", "����ϲ� AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
			//else if(1.0 <= audio_packet_lost_rate)//��������ΪE/Gʱ������ֻ���䷢��I֡�ʲ�������Ƶ������ || 1.0 <= video_packet_lost_rate)
			else if(1.0 <= audio_packet_lost_rate || 5.0 <= video_packet_lost_rate)
			{
				network_status = 0;//���缫��
				Log.v("Network", "���缫�� AL " + audio_packet_lost_rate + "%, VL " + video_packet_lost_rate + "%");
			}
		}
		
		//Log.v("Network", "AL: " + audio_packet_lost_rate + "%, VL: " + video_packet_lost_rate + "%, S: " + _network_status);
		
		return network_status;
	}
	
	//����Ƶ������Ե���
	private static void adjust_codec_strategy()
	{
		if(2 == _network_status)
		{
			//��Ƶ���Ե���
			GD.VIDEO_ENCODE_STRATEGY = -1;//�Զ�
			GD.ONLY_IDR_FRAME_INTERVAL = 2000;
		}
		else if(1 == _network_status)
		{
			GD.VIDEO_ENCODE_STRATEGY = 1;//ֻ��IDR֡
			GD.ONLY_IDR_FRAME_INTERVAL = 3000;
		}
		else if(0 == _network_status)
		{
			GD.VIDEO_ENCODE_STRATEGY = 1;//ֻ��IDR֡
			GD.ONLY_IDR_FRAME_INTERVAL = 4500;
		}
		
		//fym
		GD.VIDEO_ENCODE_STRATEGY = -1;//
		GD.ONLY_IDR_FRAME_INTERVAL = 100;//2000;
	}
	
	private static long _latest_record_media_and_network_log = 0;
	private static void record_media_and_network_log()
	{
		//15���¼һ��
		if(15000 > (System.currentTimeMillis() - _latest_record_media_and_network_log))
			return;
		
		_latest_record_media_and_network_log = System.currentTimeMillis();
		
		//��¼����״̬
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "��������  " + _network_type + ", ״̬ " + _network_status);
		
		//��¼ý��ͳ����Ϣ
		String statistics_snapshot = "";
		MediaStatistics statistics = null;
		
		//��Ƶ
		if(false == MediaThreadManager.get_instance()._audio_play_idle)
		{
			Log.v("Baidu", "audio statistics");
			statistics = MediaThreadManager.get_instance().get_media_statistics(0);
			if(null != statistics)
			{
				//statistics_snapshot = "Audio: L " + statistics.get_packet_lost_rate() + "%, D " + statistics.get_packet_delay() + "ms, B " + statistics.get_bitrate() + "kbps";
				statistics_snapshot += "��Ƶ������" + _df.format(statistics.get_packet_lost_rate())
									+ "%, �ܼ�" + statistics.get_packet_count()
									+ ", ����ӳ�" + _df.format(statistics.get_packet_relative_delay())
									+ "ms, �����ӳ�" + _df.format(statistics.get_packet_absolute_delay())
									+ "ms, ����" + _df.format(statistics.get_bitrate())
									+ "kbps, ���" + _df.format(statistics.get_averate_packet_timestamp_interval()) + "ms";
			}
		}
		//��Ƶ
		if(false == MediaThreadManager.get_instance()._video_receive_idle)
		{
			Log.v("Baidu", "video statistics");
			statistics = MediaThreadManager.get_instance().get_media_statistics(1);
			if(null != statistics)
			{
				//statistics_snapshot = "Video: L " + statistics.get_packet_lost_rate() + "%, D " + statistics.get_packet_delay() + "ms, B " + statistics.get_bitrate() + "kbps";
				statistics_snapshot += "\r\n��Ƶ������" + _df.format(statistics.get_packet_lost_rate())
									+ "%, �ܼ�" + statistics.get_packet_count()
									+ ", ����ӳ�" + _df.format(statistics.get_packet_relative_delay())
									+ "ms, �����ӳ�" + _df.format(statistics.get_packet_absolute_delay())
									+ "ms, ����" + _df.format(statistics.get_bitrate()) + "kbps";
				
			}
		}
		
		if(0 != statistics_snapshot.length())
		{
			statistics_snapshot += "\r\n";
			
			//Log.v("Media", statistics_snapshot);
			//GD.log_to_db(NetworkStatus.this, 0, "Statistics", statistics_snapshot);
		}
		
		//��¼�߳�ͳ����Ϣ
		MediaThreadManager.get_instance().thread_statistics();
	}
	
	//ping��������ȡ
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
			Log.v("Temp", "ping����");
			int status = p.waitFor();
			
			//Log.v("Temp", "ping after " + (System.currentTimeMillis() - t));
			
			//��ȡ�����http://blog.csdn.net/dancen/article/details/7969328
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
		//����ʱ5��ping��������2��ping
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
	
	private static int _force_restart_sip_media_service = 0;//�Ƿ�ǿ����������
	
	public static boolean _is_restarting_media_and_sip_service = false;//�Ƿ�������������
	
	private static int _cur_ping_delay = 0;
	
	private Thread _network_check_thread = new Thread()
	{
		//ÿ��15����һ������ȼ�
		private final long SCHEDULE_CHECK_INTERVAL = 15000;
		
		//������ÿ��5����һ��CPU�����ʣ���������������10��
		private final long PERFORMANCE_CHECK_INTERVAl = 5000;
		
		@Override
		public void run()
		{
			
		}
	};
	
	//���һ��ǿ�����������ʱ��
	private static long _latest_force_restart_sip_media_service = 0;
	
	//ǿ������SIP��ý�����
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
	
	private static String last_local_ip = "unknown";//�ϴλ�ȡ�ı���IP����ʼֵ����Ϊnull��������
	private static int last_access_network_type = -1;//�ϴλ�ȡ�Ľ�����������
	private static String current_local_ip = "";//���λ�ȡ�ı���IP
	private static int current_access_network_type = -1;//���λ�ȡ�Ľ�����������
	
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
						
						//ͨ��ping��֪����״̬
						//�����8����ӳ�
						update_ping_delay();//Log.v("Baidu", "ping delay: " + ping_delay());
						
						//Log.v("Baidu", "NetworkStatus 2");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						if(0 != _force_restart_sip_media_service)//ǿ����������
						{
							_network_status = -2;
							
							//��������ʾ����״̬������
							MessageHandlerManager.get_instance().handle_message(GID.MSG_UPDATE_NETWORK_STATUS, _network_status, _force_restart_sip_media_service, GD.MEDIA_INSTANCE);
							
							_force_restart_sip_media_service = 0;
							
							Log.v("Temp", "_force_restart_sip_media_service");
						}
						else
						{
							//��������ʾ����״̬������
							MessageHandlerManager.get_instance().handle_message(GID.MSG_UPDATE_NETWORK_STATUS, _network_status, GD.MEDIA_INSTANCE);
						}
						
						//Log.v("Baidu", "NetworkStatus 3");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						if(-1 > _network_status)
						{
							//�����жϼ�IP�仯ʱ����������
							Log.v("Temp", "restart_sip_media_service " + _network_status);
							
							_is_restarting_media_and_sip_service = true;
							Thread.sleep(100);						
							
							synchronized(GD._restart_lock)//��������ʱ��RegisterService����ANR����
							{
								GD.restart_sip_media_service();
							}
							
							Thread.sleep(100);
							_is_restarting_media_and_sip_service = false;
						}
						
						//Log.v("Baidu", "NetworkStatus 5");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						/*//������Դռ��ͳ��
						//�����м��ΪPERFORMANCE_CHECK_INTERVAl
						//�����м��Ϊ10��PERFORMANCE_CHECK_INTERVAl
						if(((true == GD.is_in_schedule()) ? PERFORMANCE_CHECK_INTERVAl : (10 * PERFORMANCE_CHECK_INTERVAl)) < (System.currentTimeMillis() - latest_check_cpu_timestamp))
						{
							PerformanceStatistics.get_cpu_usage();
							PerformanceStatistics.get_process_memory();
							
							latest_check_cpu_timestamp = System.currentTimeMillis();
						}*/
						
						//Log.v("Baidu", "NetworkStatus 6");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//��ȡ����IP�ͽ�����������
						IpAddress.setLocalIpAddress();//��ӦIP��ַ���ܷ����仯�����
						current_local_ip = IpAddress.localIpAddress;
						
						//���ȡΪ127.0.0.1����ʵΪ�޷���ȡIP
						current_local_ip = (true == current_local_ip.equalsIgnoreCase("127.0.0.1") ? "" : current_local_ip);
						
						//��ȡ������������
						current_access_network_type = get_access_network_type();
						
						//Log.v("Baidu", current_local_ip + ", " + current_access_network_type);
						
						if(null == current_local_ip || current_local_ip.equals(""))//�޷���ȡIP���ж�Ϊ�����ж�
						{
							_network_status = -2;						
							return;//continue;
						}
						else//IP��ȡ����
						{
							if(0 == _cur_ping_delay)//ping��ͨҲ���ж�
							{
								_network_status = -2;						
								return;//continue;
							}
							
							//��last_local_ip����ֵ
							if(true == last_local_ip.equalsIgnoreCase("unknown"))
							{
								last_local_ip = current_local_ip;
								last_access_network_type = current_access_network_type;
							}
							
							//IP�䶯
							if(false == last_local_ip.equalsIgnoreCase(current_local_ip))
							{
								//Log.v("Baidu", last_local_ip + ", " + current_local_ip);
								
								//WIFI�����¿��ܻ��⵽3G�����IP������WIFI������IP�䶯��������
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
						//��ȡ��������
						int current_network_type = get_current_network_type();
						
						if(_network_type != current_network_type)
						{
							//�������ͱ仯ʱ�Ĵ���
							on_network_type_change();
							_network_type = current_network_type;
						}
						
						//��������δ֪
						if(-1 == _network_type)
						{
							_network_status = -1;
							return;//continue;
						}
						
						//������������δ������ʱ����������������Ϊ����״̬
						if(0 > _network_status)
						{
							_network_status = _network_type;
						}
						
						//Log.v("Baidu", "NetworkStatus 8");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//����������ͼ�ping delay��������״̬
						if(SCHEDULE_CHECK_INTERVAL > (System.currentTimeMillis() - _latest_check_network_status_timestamp))
						{
							return;//continue;
						}
						
						//����ping delay��ȡ����״̬
						int current_network_status = get_current_network_status();
						
						if(_network_status != current_network_status)
						{
							_network_status = current_network_status;
							
							//�������ͱ仯ʱ����֪ͨ���ȷ�����
							RegisterService.register_to_schedule_server_at_once();
						}
						
						//Log.v("Baidu", "NetworkStatus 9");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//�����������
						adjust_codec_strategy();
						
						//Log.v("Baidu", "NetworkStatus 10");
						//////////////////////////////////////////////////////////////////////////////////////////
						
						///////////////////////////
						//��¼���缰����Ƶͳ����־
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
