/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.nercms.schedule.sip.engine.sipua.ui;

//fym import com.nercms.schedule.sip.engine.media.RtpStreamReceiver;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nercms.schedule.sip.engine.net.RtpSocket;
import com.nercms.schedule.network.MQTT;
import com.nercms.schedule.network.NetworkStatus;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaSocketManager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class RegisterService
{
	//public static Object _exit_flag = new Object();
	
	SipdroidReceiver m_receiver;

	//private boolean _register_thread_available = true;// fym
	
	//上次向会议服务器注册（SIP注册及NAT探测）的时戳
	private static long _latest_register_to_schedule_server_timestamp = 0;	
	
	//上次向地图服务器注册的时戳
	public static long _last_register_to_location_server_timestamp = 0;			
	
	//上次更新下属机构人员位置的时戳
	//private static long _last_update_resource_position_timestamp = 0;
	
	//更新下属机构人员位置的时间间隔
	//private static long _update_resource_position_interval = 0;
	
	//上次检查程序更新的时戳
	//private static long _latest_check_app_version_timestamp = 0;
	
	private static ScheduledExecutorService _register_service = null;
	
	//单键处理
  	private volatile static RegisterService _unique_instance = null;
	public static RegisterService instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_instance)
		{
			// 对其进行锁,防止两个线程同时进入同步代码区
			synchronized(RegisterService.class)
			{
				//必须双重检查
				if(null == _unique_instance)
				{
					_unique_instance = new RegisterService();
				}
			}
		}
		
		return _unique_instance;
	}
		
	private static void register_all()
	{
		//Log.v("Baidu", "register all " + GD.DEFAULT_SCHEDULE_SERVER);//SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, ""));
		
		try
		{
			//每隔SCHEDULE_REGISTER_INTERVAL向会议服务器发送一次NAT探测包（即注册）
			if(GD.SCHEDULE_REGISTER_INTERVAL < (System.currentTimeMillis() - _latest_register_to_schedule_server_timestamp))
			{
				Log.v("register", "register_to_schedule_server_by_nat_probing");
				register_and_nat_probing();
				_latest_register_to_schedule_server_timestamp = System.currentTimeMillis();
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	//立即向调度服务器注册
	public static void register_to_schedule_server_at_once()
	{
		_latest_register_to_schedule_server_timestamp = 0;
	}

	private static String get_nat_content(String type)
	{
		/*{
			"t":"A",//必选项，A-音频UDP探测包，V-视频UDP探测包, REG-TCP探测包
			"i":"XXXXXX",//必选项，终端IMSI号
			"c":"X",//必选项，终端是否与会，"0"-不与会，"1"-与会
			"n":"X",//可选项，网络类型
			"s":"X",//可选项，网络状态
			"b":"XXX",//可选项，终端品牌
			"m":"XXX",//可选项，终端型号
			"p":"XXX"//可选项，传输协议三位字符串，第0、1、2位代表SIP、AUDIO、VIDEO，每位1-UDP，2-TCP, 3-都支持, 5-MQTT
		}*/		
		//调度信息
		int conference_flag = (true == GD.is_in_schedule()) ? 1 : 0;
		String content = "{ \"t\":\"" + type + "\",\"i\":\"" + GD.get_unique_id(GD.get_global_context()) + "\""
						  + ",\"c\":\"" + conference_flag + "\"";
		
		//网络类型
		content += ",\"n\":\"" + NetworkStatus.get_network_type() + "\"";
				
		//网络状态
		content += ",\"s\":\"2\"";//流畅演示 content += ",\"s\":\"" + NetworkStatus.get_network_status() + "\"";
		
		//终端品牌和型号
		content += ",\"b\":\"" + Build.BRAND + "\",\"m\":\"" + Build.MODEL + "\"";
		
		//传输协议
		content += ",\"p\":\"";
		if(false == GD.MQTT_ON) content += (true == GD.sip_audio_over_udp()) ? "3" : "2";//"1";//fym (true == GD.SIP_PROTOCOL.equalsIgnoreCase("tcp")) ? "1" : "0";
		else content += "5";//MQTT
		content += (true == GD.sip_audio_over_udp()) ? "3" : "2";//(true == GD.AUDIO_PROTOCOL.equalsIgnoreCase("tcp")) ? "1" : "0";
		content += (true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp")) ? "2" : "1";
		content += "\"";
				
		content += "}";
		
		return content;
	}
			
	//向会议服务器后台发送NAT探测包
	private static void register_and_nat_probing()
	{
		try
		{
			boolean nat_probed = false;
			
			//如视频传输协议为UDP，则发送NAT探测包
			if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("udp"))
			{
				RtpSocket socket = MediaSocketManager.get_instance().get_video_recv_socket();
				if(null != socket)
				{
					String nat_content = get_nat_content("V");
					Log.v("Baidu", "Video: " + nat_content);
							
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0) + 10);
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0) + 10);
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0) + 10);
					
					//socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT + 10);
					//socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT + 10);
					socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT + 10);
					
					nat_probed = true;
				}
				else
				{
					_latest_register_to_schedule_server_timestamp = 0;
				}
			}
			
			//如音频传输协议为UDP，则发送NAT探测包
			if(true == GD.sip_audio_over_udp() && false == GD.TCP_AUDIO_RECV)//if(true == GD.AUDIO_PROTOCOL.equalsIgnoreCase("udp"))
			{
				//发送音频探测包
				RtpSocket socket = MediaSocketManager.get_instance().get_audio_recv_socket();
				if(null != socket)
				{
					String nat_content = get_nat_content("A");				
					Log.v("Baidu", "Audio: " + nat_content);
					
					//一次连发三个探测包
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0));
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0));
					//socket.send(nat_content.getBytes(), nat_content.length(), SP.get( GD.get_global_context(), SP.PREF_SERVER_NAT_PORT, 0));
					
					//socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT);
					//socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT);
					socket.send(nat_content.getBytes(), nat_content.length(), GD.SERVER_NAT_PORT);
					
					nat_probed = true;
				}
				else
				{
					_latest_register_to_schedule_server_timestamp = 0;
				}
			}
			
			//如SIP信令传输协议为TCP，则发送基于MESSAGE信令的探测包
			//if(false == nat_probed || true == GD.SIP_PROTOCOL.equalsIgnoreCase("tcp"))
			if(false == nat_probed)
			{
				String msg = get_nat_content("REG");//此时媒体类型无意义				
				Log.v("Baidu", "TCP: " + msg);
				
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", msg);
				else MQTT.instance().publish_message_to_server(msg, 1);
				Log.v("Baidu", "tcp nat register success!");
			}
			
			//keep alive
			{
				Log.v("Baidu", "sip ua keep alive by message hahaha");
				if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).send_message("VUA", "hahaha");
			}
			
			//向SIP服务器注册
			if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).expire();
			
			Log.v("Baidu", "register & nat probing");
		}
		catch(Exception e)
		{
		}
	}
	
	public void start(Context context)
	{
		android.util.Log.v("Baidu", "RegisterService onCreate 0");
		
		//context.startService(new Intent(context, RegisterService.class));
		android.util.Log.v("Baidu", "RegisterService onCreate 1");
		
		
		if(SipdroidReceiver.mContext == null)
		{
			SipdroidReceiver.mContext = GD.get_global_context();
		}
		
		android.util.Log.v("Baidu", "RegisterService onCreate 2");
		
		if(null == m_receiver)
		{
			android.util.Log.v("Baidu", "RegisterService onCreate 3");
			
			IntentFilter intentfilter = new IntentFilter();
			intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentfilter.addAction(SipdroidReceiver.ACTION_DATA_STATE_CHANGED);
			intentfilter.addAction(SipdroidReceiver.ACTION_PHONE_STATE_CHANGED);
			intentfilter.addAction(SipdroidReceiver.ACTION_DOCK_EVENT);
			intentfilter.addAction(Intent.ACTION_HEADSET_PLUG);
			intentfilter.addAction(Intent.ACTION_USER_PRESENT);
			intentfilter.addAction(Intent.ACTION_SCREEN_OFF);
			intentfilter.addAction(Intent.ACTION_SCREEN_ON);
			intentfilter.addAction(SipdroidReceiver.ACTION_VPN_CONNECTIVITY);
			intentfilter.addAction(SipdroidReceiver.ACTION_SCO_AUDIO_STATE_CHANGED);
			intentfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
			intentfilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			GD.get_global_context().registerReceiver(m_receiver = new SipdroidReceiver(), intentfilter);
			//intentfilter = new IntentFilter();
		}
		
		if(false == GD.MQTT_ON)
		{
			SipdroidReceiver.engine(GD.get_global_context()).isRegistered();
			System.out.println("RegisterService");
			// fym RtpStreamReceiver.restoreSettings();
		}		

		android.util.Log.v("Baidu", "RegisterService onCreate");
		
		android.util.Log.v("Baidu", "RegisterService onCreate 1.1");
		
		_latest_register_to_schedule_server_timestamp = 0;	
		_last_register_to_location_server_timestamp = 0;
		
		Thread _register_thread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);//优先级无需太高
					
					if(GD.SCHEDULE_STATE.closing != GD.get_scheduel_state())
					{
						synchronized(GD._restart_lock)//避免重启时因RegisterService导致ANR错误
						{
							//Log.v("Baidu", "register_all() scheduleAtFixedRate 500");
							//rmv update_app();
							register_all();
						}
					}
				}
				catch(Exception e)
				{				
				}
			}
		};
		
		_register_service = Executors.newScheduledThreadPool(1);
		_register_service.scheduleAtFixedRate(_register_thread, 500, 500, TimeUnit.MILLISECONDS);
	}
	
	public void stop(Context context)
	{
		_register_service.shutdownNow();
		Log.v("Temp", "register service shutdown: " + _register_service.isShutdown() + ", terminate: " + _register_service.isTerminated());
		
		if (m_receiver != null)
		{
			GD.get_global_context().unregisterReceiver(m_receiver);
			m_receiver = null;
		}
		
		//Receiver.alarm(0, OneShotAlarm2.class);
		android.util.Log.v("Baidu", "RegisterService onDestroy");
	}
	
}
