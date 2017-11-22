/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2008 Hughes Systique Corporation, USA (http://www.hsc.com)
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

package com.nercms.schedule.sip.engine.sipua;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.R.raw;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

//rmv import com.nercms.schedule.R;
import com.nercms.schedule.rtsp.RTSPClient;
import com.nercms.schedule.sip.engine.net.KeepAliveSip;
import com.nercms.schedule.sip.engine.sipua.ui.LoopAlarm;
import com.nercms.schedule.sip.engine.sipua.ui.OnReRegister;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.engine.sipua.ui.Sipdroid;
import com.nercms.schedule.sip.stack.net.IpAddress;
import com.nercms.schedule.sip.stack.net.SocketAddress;
import com.nercms.schedule.sip.stack.sip.address.NameAddress;
import com.nercms.schedule.sip.stack.sip.provider.SipProvider;
import com.nercms.schedule.sip.stack.sip.provider.SipStack;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.Participant;
import com.nercms.schedule.network.NetworkStatus;
//rmv import com.nercms.schedule.ui.MessageHandlerManager;
//rmv import com.nercms.schedule.ui.ScheduleMap;

public class SipdroidEngine implements RegisterAgentListener
{
	private static final String LOG_TAG = "SIP";

	//public static final int LINES = 2;
	public int pref;

	public static final int UNINITIALIZED = 0x0;
	public static final int INITIALIZED = 0x2;

	/** User Agent */
	public UserAgent[] uas;
	public UserAgent ua;

	/** Register Agent */
	private RegisterAgent[] ras;

	private KeepAliveSip[] kas;

	/** UserAgentProfile */
	public UserAgentProfile[] user_profiles;

	public SipProvider[] sip_providers;

	public static PowerManager.WakeLock[] wl, pwl;

	public static void set_default_preference(Context context)
	{
		Log.v("UA", "set_default_preference");

		SP.set(context, "port", Integer.toString(GD.SIP_SERVER_PORT));
		// SipPreference.save_parameter(context, "speex_new", "never");
		SP.set(context, "stun_server_port", "3478");
		SP.set(context, "stun_server", "stun.ekiga.net");
		//SP.set(context, "username", Long.toString(GD.get_imsi(SipdroidReceiver.mContext)));
		//SP.set(context, "password", "123");
		SP.set(context, "wlan", true);
		SP.set(context, "3g", true);
		SP.set(context, "edge", true);
		SP.set(context, "GSM_new", "never");
		// SipPreference.save_parameter(context, "hmicgain", "1.0");
		SP.set(context, "registration", true);
		SP.set(context, "server", GD.DEFAULT_SCHEDULE_SERVER);
		SP.set(context, "location_server", GD.DEFAULT_SCHEDULE_SERVER);
		SP.set(context, "MWI_enabled", true);
		SP.set(context, "pref", "SIP");
		// 导致注册失败 SipPreference.save_parameter(context, "dns0",
		// "220.250.64.18");
	}

	UserAgentProfile getUserAgentProfile(int index)
	{
		Log.v("UA", "getUserAgentProfile");

		UserAgentProfile user_profile = new UserAgentProfile(null);

		//UA用户名为根据IMSI号计算得出的ID
		user_profile.username = Long.toString(GD.get_unique_id(getUIContext()));
		user_profile.username += (0 == (index % 2)) ? "T" : "U";//TCP及UDP方式各注册一个UA

		//UA密码
		user_profile.passwd = SP.get(getUIContext(), SP.PREF_PASSWORD, SP.DEFAULT_PASSWORD);

		if(0 == SP.get(getUIContext(), SP.PREF_DOMAIN, SP.DEFAULT_DOMAIN).length())
		{
			user_profile.realm = GD.DEFAULT_SCHEDULE_SERVER;//SP.get(getUIContext(), SP.PREF_SCHEDULE_SERVER, "");
		}
		else
		{
			user_profile.realm = SP.get(getUIContext(), SP.PREF_DOMAIN, SP.DEFAULT_DOMAIN);
		}
		
		user_profile.realm_orig = user_profile.realm;
		
		if(0 == SP.get(getUIContext(), SP.PREF_FROMUSER, SP.DEFAULT_FROMUSER).length())
		{
			user_profile.from_url = user_profile.username;
		}
		else
		{
			user_profile.from_url = SP.get(getUIContext(), SP.PREF_FROMUSER, SP.DEFAULT_FROMUSER);
		}

		// MMTel configuration (added by mandrajg)
		user_profile.qvalue = SP.get(getUIContext(), SP.PREF_MMTEL_QVALUE, SP.DEFAULT_MMTEL_QVALUE);
		user_profile.mmtel = SP.get(getUIContext(), SP.PREF_MMTEL, SP.DEFAULT_MMTEL);

		/*
		 * fym user_profile.pub = SipPreference.get_parameter(getUIContext(),
		 * SipPreference.PREF_EDGE, SipPreference.DEFAULT_EDGE) ||
		 * SipPreference.get_parameter(getUIContext(), SipPreference.PREF_3G,
		 * SipPreference.DEFAULT_3G);
		 */
		user_profile.pub = true;

		Log.v(LOG_TAG, "from_url " + user_profile.from_url);// fym
		Log.v(LOG_TAG, "contact_url " + user_profile.contact_url);// fym
		Log.v(LOG_TAG, "username " + user_profile.username);// fym
		Log.v(LOG_TAG, "passwd " + user_profile.passwd);// fym
		Log.v(LOG_TAG, "realm " + user_profile.realm);// fym
		Log.v(LOG_TAG, "qvalue " + user_profile.qvalue);// fym
		Log.v(LOG_TAG, "mmtel " + user_profile.mmtel);// fym
		Log.v(LOG_TAG, "pub " + user_profile.pub);// fym

		return user_profile;
	}

	public boolean StartEngine()
	{
		Log.v("Baidu", "StartEngine");
		
		//如果会议服务器或地图服务器地址为空，则设置默认地址
		/*if (SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, "").equals("")
			|| SP.get(SipdroidReceiver.mContext, SP.PREF_LOCATION_SERVER, "").equals(""))*/
		
		if(GD.DEFAULT_SCHEDULE_SERVER/*SP.get(getUIContext(), SP.PREF_SCHEDULE_SERVER, "")*/.equals("")
				|| SP.get(getUIContext(), SP.PREF_LOCATION_SERVER, "").equals(""))
		{
			SipdroidEngine.set_default_preference(getUIContext());
		}
		
		/*if(null == getUIContext())
			Log.e("Baidu", "getUIContext null");
		else
			Log.e("Baidu", "getUIContext is not null");
		
		if(null == GD.get_global_context())
			Log.e("Baidu", "get_global_context null");
		else
			Log.e("Baidu", "get_global_context is not null");
		
		String schedule_server = SP.get(getUIContext(), SP.PREF_SCHEDULE_SERVER, "");
		String location_server = SP.get(getUIContext(), SP.PREF_LOCATION_SERVER, "");
		
		if(null == schedule_server || true == schedule_server.equalsIgnoreCase("") || null == location_server || true == location_server.equalsIgnoreCase(""))
		{
			SipdroidEngine.set_default_preference(getUIContext());
		}*/

		// 启动后默认选择接收视频模式
		GD._i_am_video_source = false;//SP.set(SipdroidReceiver.mContext, SP.PREF_IS_VIDEO_SOURCE, false);

		//启动PowerManager，创建屏幕锁
		PowerManager pm = (PowerManager) getUIContext().getSystemService(Context.POWER_SERVICE);
		if(null == wl)
		{
			wl = new PowerManager.WakeLock[GD.UA_LINES/*fym LINES*/];
			pwl = new PowerManager.WakeLock[GD.UA_LINES/*fym LINES*/];
		}

		//创建UserAgent、RegisterAgent和SipProvider
		uas = new UserAgent[GD.UA_LINES/*fym LINES*/];		
		ras = new RegisterAgent[GD.UA_LINES/*fym LINES*/];		
		kas = new KeepAliveSip[GD.UA_LINES/*fym LINES*/];
		lastmsgs = new String[GD.UA_LINES/*fym LINES*/];
		sip_providers = new SipProvider[GD.UA_LINES/*fym LINES*/];		
		user_profiles = new UserAgentProfile[GD.UA_LINES/*fym LINES*/];

		//读取每个UA的设置
		for (int i = 0; i < GD.UA_LINES/*fym LINES*/; i++)
		{
			user_profiles[i] = getUserAgentProfile(i);
		}

		//根据配置文件初始化SipStack
		SipStack.init(null);

		int i = 0;
		for (UserAgentProfile user_profile : user_profiles)
		{
			if (wl[i] == null)
			{
				wl[i] = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sipdroid.SipdroidEngine");
				
				if (SP .get(getUIContext(), SP.PREF_KEEPON, SP.DEFAULT_KEEPON))
					pwl[i] = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.SipdroidEngine");
			}

			try
			{
				SipStack.debug_level = 0;
				// SipStack.log_path =
				// "/data/data/com.nercms.schedule.sip.engine.sipua";
				SipStack.max_retransmission_timeout = 10000;//4000;
				SipStack.default_transport_protocols = new String[1];
				//SipStack.default_transport_protocols[0] = GD.SIP_PROTOCOL;//"tcp";//"udp"

				String version = "schedule ua";
				SipStack.ua_info = version;
				SipStack.server_info = version;

				//获取本机IP地址
				IpAddress.setLocalIpAddress();
				//fym Log.v("Debug", "local ip: " + IpAddress.localIpAddress);
				
				//fym sip_providers[i] = new SipProvider(IpAddress.localIpAddress, 0);//fym
				sip_providers[i] = new SipProvider(IpAddress.localIpAddress, 0, i);

				Log.v(LOG_TAG, "localIpAddress " + IpAddress.localIpAddress);// fym

				user_profile.contact_url = getContactURL(user_profile.username, sip_providers[i]);

				if (user_profile.from_url.indexOf("@") < 0)
				{
					user_profile.from_url += "@" + user_profile.realm;
					//如此则MESSAGE信令不通 user_profile.from_url += "@" + ((false == GeneralDefine.OVER_VPDN) ? user_profile.realm : GeneralDefine.SIP_SERVER_LAN_IP_IN_VPN);//fym					
				}

				//创建SIP服务器地址
				CheckEngine();

				// added by mandrajg
				String icsi = null;
				if (user_profile.mmtel == true)
				{
					icsi = "\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"";
				}
				//Log.v("Debug", "icsi: " + icsi);

				Log.v(LOG_TAG, "after from_url " + user_profile.from_url);// fym
				Log.v(LOG_TAG, "after contact_url " + user_profile.from_url);// fym
				Log.v(LOG_TAG, "after username " + user_profile.username);// fym
				Log.v(LOG_TAG, "after passwd " + user_profile.passwd);// fym
				Log.v(LOG_TAG, "after realm " + user_profile.realm);// fym
				Log.v(LOG_TAG, "after qvalue " + user_profile.qvalue);// fym
				Log.v(LOG_TAG, "after mmtel " + user_profile.mmtel);// fym
				Log.v(LOG_TAG, "after pub " + user_profile.pub);// fym

				uas[i] = ua = new UserAgent(sip_providers[i], user_profile);
				ras[i] = new RegisterAgent(
						sip_providers[i],
						user_profile.from_url, // modified
						user_profile.contact_url, user_profile.username,
						user_profile.realm, user_profile.passwd, this,
						user_profile, user_profile.qvalue, icsi,
						user_profile.pub); // added by mandrajg
				kas[i] = new KeepAliveSip(sip_providers[i], 100000);
			} catch (Exception E) {
			}

			i++;
		}

		register();

		//android.util.Log.v("UA", "listen 2");

		listen();

		// 初始化与会人员列表
		synchronized(GD._participants_lock)
		{
			GD._participants.clear();
		}

		return true;
	}

	private String getContactURL(String username, SipProvider sip_provider) {
		Log.v(LOG_TAG, "getContactURL");

		int i = username.indexOf("@");
		if (i != -1) {
			// if the username already contains a @
			// strip it and everthing following it
			username = username.substring(0, i);
		}

		return username
				+ "@"
				+ IpAddress.localIpAddress
				+ (sip_provider.getPort() != 0 ? ":" + sip_provider.getPort()
						: "") + ";transport="
				+ sip_provider.getDefaultTransport();
	}

	void setOutboundProxy(SipProvider sip_provider, int i)
	{
		Log.v(LOG_TAG, "setOutboundProxy " + GD.DEFAULT_SCHEDULE_SERVER);//SP.get(getUIContext(), SP.PREF_SCHEDULE_SERVER, ""));

		try
		{
			if (sip_provider != null)
			{
				/*sip_provider.setOutboundProxy(new SocketAddress(IpAddress.getByName(SP.get(getUIContext(),
																SP.PREF_SCHEDULE_SERVER,//fym SipPreference.PREF_DNS + i,
																"")),//fym SipPreference.DEFAULT_DNS)),
											Integer.valueOf(SP.get(getUIContext(),
															SP.PREF_PORT,//fym SipPreference.PREF_PORT + (i != 0 ? i : ""),
															SP.DEFAULT_PORT))));*/
				sip_provider.setOutboundProxy(new SocketAddress(IpAddress.getByName(GD.DEFAULT_SCHEDULE_SERVER),//SP.get(getUIContext(), SP.PREF_SCHEDULE_SERVER, "")),
											GD.SIP_SERVER_PORT));//GD.SIP_SERVER_PORT生效
			}
		} catch (Exception e) {
		}
	}

	public void CheckEngine()
	{
		//Log.v(LOG_TAG, "CheckEngine");

		int i = 0;
		for(SipProvider sip_provider : sip_providers)
		{
			if (sip_provider != null && !sip_provider.hasOutboundProxy())
				setOutboundProxy(sip_provider, i);
			i++;
		}
	}

	public Context getUIContext()
	{
		// Log.v(LOG_TAG, "getUIContext");

		return GD.get_global_context();//SipdroidReceiver.mContext;
	}

	public int getRemoteVideo() {
		Log.v(LOG_TAG, "getRemoteVideo");

		return ua.remote_video_port;
	}

	public int getLocalVideo() {
		Log.v(LOG_TAG, "getLocalVideo");

		return ua.local_video_port;
	}

	public String getRemoteAddr() {
		Log.v(LOG_TAG, "getRemoteAddr");

		return ua.remote_media_address;
	}

	//UA注册接口
	public void expire()
	{
		Log.v("Baidu", "expire");

		SipdroidReceiver.rereg_expire_time = 0;
		int i = 0;
		
		for(RegisterAgent ra : ras)
		{
			//Log.v(LOG_TAG, "expire 2 " + i);
			
			if (ra != null && ra.CurrentState == RegisterAgent.REGISTERED)
			{
				ra.CurrentState = RegisterAgent.UNREGISTERED;
				SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION + i, null, 0, 0);
			}
			i++;
			
			//Log.v(LOG_TAG, "expire 3 " + i);
		}
		
		Log.v(LOG_TAG, "before register");
		
		register();
	}

	public void unregister(int i)
	{
		Log.v(LOG_TAG, "unregister");
		
		Log.i("Temp", "shutdown 1.131");

		if (user_profiles[i] == null || user_profiles[i].username.equals("") || user_profiles[i].realm.equals(""))
			return;
		
		Log.i("Temp", "shutdown 1.132");

		RegisterAgent ra = ras[i];
		
		Log.i("Temp", "shutdown 1.133 " + ra.username);
		if (ra != null && ra.unregister())
		{
			Log.i("Temp", "shutdown 1.135");
			// fym 注销成功后提示
			SipdroidReceiver.alarm(0, OnReRegister.class);//fym SipdroidReceiver.alarm(0, LoopAlarm.class);
			// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
			//fym wl[i].acquire();
			
			Log.i("Temp", "shutdown 1.136");
		}
		else
		{
			Log.i("Temp", "shutdown 1.137");
			SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, null, 0, 0);
			
			Log.i("Temp", "shutdown 1.138");
		}
		
		Log.i("Temp", "shutdown 1.139");
	}

	public void registerMore()
	{
		Log.v(LOG_TAG, "registerMore");

		IpAddress.setLocalIpAddress();
		int i = 0;
		for (RegisterAgent ra : ras) {
			try {
				if (user_profiles[i] == null
						|| user_profiles[i].username.equals("")
						|| user_profiles[i].realm.equals("")) {
					i++;
					continue;
				}
				user_profiles[i].contact_url = getContactURL(
						user_profiles[i].from_url, sip_providers[i]);

				//fym if(ra != null && !ra.isRegistered() && Receiver.isFast(i) && ra.register())
				if(ra != null && !ra.isRegistered() && ra.register())
				{
					// fym
					// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
					//fym wl[i].acquire();
				}
			} catch (Exception ex) {

			}
			i++;
		}
	}

	public void register()
	{
		android.util.Log.v("SIP", "register()");

		IpAddress.setLocalIpAddress();
		
		int i = 0;
		for (RegisterAgent ra : ras)
		{
			try
			{
				//用户名为空则不注册
				if(user_profiles[i] == null || user_profiles[i].username.equals("") || user_profiles[i].realm.equals(""))
				{
					i++;
					continue;
				}
				
				user_profiles[i].contact_url = getContactURL(user_profiles[i].from_url, sip_providers[i]);

				/*if(false)//fym !Receiver.isFast(i))
				{
					unregister(i);
				}
				else*/
				{
					if(ra != null && ra.register())
					{
						// fym 注册成功后提示
						// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
						//fym wl[i].acquire();
					}
				}
			}
			catch (Exception ex)
			{
			}
			i++;
		}
	}

	public void registerUdp() {
		Log.v(LOG_TAG, "registerUdp");

		IpAddress.setLocalIpAddress();
		int i = 0;
		for (RegisterAgent ra : ras) {
			try {
				if (user_profiles[i] == null
						|| user_profiles[i].username.equals("")
						|| user_profiles[i].realm.equals("")
						|| sip_providers[i] == null
						|| sip_providers[i].getDefaultTransport() == null
						|| sip_providers[i].getDefaultTransport().equals("tcp")) {
					i++;
					continue;
				}
				user_profiles[i].contact_url = getContactURL(
						user_profiles[i].from_url, sip_providers[i]);

				/*if(false)//fym !Receiver.isFast(i))
				{
					unregister(i);
				}
				else*/
				{
					if(ra != null && ra.register())
					{
						// fym
						// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(R.string.reg),R.drawable.sym_presence_idle,0);
						//fym wl[i].acquire();
					}
				}
			}
			catch (Exception ex)
			{
			}
			i++;
		}
	}

	public void halt()
	{
		Log.v("Temp", "halt");
		
		Log.i("Temp", "shutdown 1.2.1");
		
		//Log.i("Temp", "shutdown 1.1");

		// 清空与会人员列表
		synchronized(GD._participants_lock)
		{
			GD._participants.clear();
		}
		
		Log.i("Temp", "shutdown 1.2.2");
		
		//Log.i("Temp", "shutdown 1.2");

		long time = SystemClock.elapsedRealtime();

		int i = 0;
		for (RegisterAgent ra : ras)
		{
			//Log.i("Temp", "shutdown 1.3 " + i);
			
			Log.i("Temp", "shutdown 1.2.3 " + ra.username);
			unregister(i);
			Log.i("Temp", "shutdown 1.2.5");
			
			//Log.i("Temp", "shutdown 1.5");
			
			while (ra != null
					&& ra.CurrentState != RegisterAgent.UNREGISTERED
					//&& SystemClock.elapsedRealtime() - time < 2000)
					&& SystemClock.elapsedRealtime() - time < 200)
			{
				//Log.i("Temp", "shutdown 1.6");
				
				try {
					Thread.sleep(10);//Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
			}
			
			Log.i("Temp", "shutdown 1.2.6");
			
			//Log.i("Temp", "shutdown 1.7");
			
			if (wl[i].isHeld())
			{
				wl[i].release();
				if (pwl[i] != null && pwl[i].isHeld())
					pwl[i].release();
			}
			
			Log.i("Temp", "shutdown 1.2.7");
			
			//Log.i("Temp", "shutdown 1.8");
			
			if (kas[i] != null)
			{
				SipdroidReceiver.alarm(0, OnReRegister.class);//fym SipdroidReceiver.alarm(0, LoopAlarm.class);
				kas[i].halt();
			}
			
			Log.i("Temp", "shutdown 1.2.8");
			
			//Log.i("Temp", "shutdown 1.9");
			
			SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, null, 0, 0);
			
			if (ra != null)
				ra.halt();
			
			Log.i("Temp", "shutdown 1.2.9");
			
			//Log.i("Temp", "shutdown 1.10");
			
			if (uas[i] != null)
				uas[i].hangup();
			
			Log.i("Temp", "shutdown 1.2.10");
			
			//Log.i("Temp", "shutdown 1.11");
			
			if (sip_providers[i] != null)
				sip_providers[i].halt();
			
			Log.i("Temp", "shutdown 1.2.11");
			
			//Log.i("Temp", "shutdown 1.12");
			
			i++;
		}
		
		Log.i("Temp", "shutdown 1.2.12");
		
		//Log.i("Temp", "shutdown 1.5");
	}

	public boolean isRegistered() {
		Log.v(LOG_TAG, "isRegistered");

		for (RegisterAgent ra : ras)
			if (ra != null && ra.isRegistered())
				return true;
		return false;
	}

	boolean isRegistered(int i) {
		Log.v(LOG_TAG, "isRegistered");

		if (ras[i] == null) {
			return false;
		}
		return ras[i].isRegistered();
	}

	//注册成功后回调
	public void onUaRegistrationSuccess(RegisterAgent reg_ra, NameAddress target, NameAddress contact, String result)
	{
		android.util.Log.v("register", "------ Registration Success");

		int i = 0;
		for (RegisterAgent ra : ras) {
			if (ra == reg_ra)
				break;
			i++;
		}
		if (isRegistered(i))
		{
			if (SipdroidReceiver.on_wlan)
				;//fym SipdroidReceiver.alarm(60000, LoopAlarm.class);
			// fym
			// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(i
			// ==
			// pref?R.string.regpref:R.string.regclick),R.drawable.sym_presence_available,0);
			android.util.Log.i("Sipdroid", "onText");
			//rmv SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, getUIContext().getString(R.string.regsuccess), R.drawable.sym_presence_available, 0);
			
			GD._latest_sip_register_success = System.currentTimeMillis();
			
			reg_ra.subattempts = 0;
			// fym 不发送订阅请求 reg_ra.startMWI();
			SipdroidReceiver.registered();
		} else
		{
			// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i, null, 0,0);
		}

		if (wl[i].isHeld()) {
			wl[i].release();
			if (pwl[i] != null && pwl[i].isHeld())
				pwl[i].release();
		}
	}

	String[] lastmsgs;

	public void onMWIUpdate(RegisterAgent mwi_ra, boolean voicemail, int number, String vmacc)
	{
		Log.v(LOG_TAG, "onMWIUpdate");
	}

	static long lasthalt, lastpwl;

	/** When a UA failed on (un)registering. */
	//注册失败通知回调
	public void onUaRegistrationFailure(RegisterAgent reg_ra, NameAddress target, NameAddress contact, String result)
	{
		// Log.v(LOG_TAG, "onUaRegistrationFailure");
		android.util.Log.v("SIP", "++++++ Registration Failure");
		boolean retry = false;
		int i = 0;
		
		for(RegisterAgent ra : ras)
		{
			if (ra == reg_ra)
				break;
			i++;
		}
		
		if (isRegistered(i))
		{
			reg_ra.CurrentState = RegisterAgent.UNREGISTERED;
			// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i, null, 0, 0);
		}
		else
		{
			retry = true;
			
			//rmv SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, getUIContext().getString(R.string.regfailed) + " (" + result + ")", R.drawable.sym_presence_away, 0);
			
			//注册失败超过90秒时重启SIP及媒体服务
			//强制重启间隔至少30秒
			if(90000 <= (System.currentTimeMillis() - GD._latest_sip_register_success))
			{
				NetworkStatus.force_restart_sip_media_service(6);
			}
			// fym
			// Receiver.onText(Receiver.REGISTER_NOTIFICATION+i,getUIContext().getString(R.string.regfailed)+" ("+result+")",R.drawable.sym_presence_away,0);
		}
		
		android.util.Log.i("Sipdroid", "retry" + retry + "; "
						+ (SystemClock.uptimeMillis() > lastpwl + 45000) + "; "
						+ (pwl[i] != null) + "; " + (!pwl[i].isHeld()) + "; "
						+ (SipdroidReceiver.on_wlan));
		
		if (retry && SystemClock.uptimeMillis() > lastpwl + 45000
				&& pwl[i] != null && !pwl[i].isHeld() && SipdroidReceiver.on_wlan)
		{
			lastpwl = SystemClock.uptimeMillis();
			if (wl[i].isHeld())
				wl[i].release();
			//fym pwl[i].acquire(); 
			//register(); 
			if(!wl[i].isHeld() && pwl[i].isHeld())
				pwl[i].release();
		}
		else if(wl[i].isHeld())
		{
			wl[i].release();
			if (pwl[i] != null && pwl[i].isHeld())
				pwl[i].release();
		}

		if (SystemClock.uptimeMillis() > lasthalt + 45000)
		{
			lasthalt = SystemClock.uptimeMillis();
			sip_providers[i].haltConnections();
		}
		//fym updateDNS();
		reg_ra.stopMWI();
		WifiManager wm = (WifiManager) SipdroidReceiver.mContext.getSystemService(Context.WIFI_SERVICE);
		wm.startScan();

		android.util.Log.i("Sipdroid", "retry register");
		SipdroidReceiver.alarm(2000, OnReRegister.class); //注册失败后，15秒重新注册
	}

	/*public void updateDNS() {
		Log.v(LOG_TAG, "updateDNS");

		int i = 0;
		for (SipProvider sip_provider : sip_providers) {
			try {
				SipPreference.save_parameter(
						getUIContext(),
						SipPreference.PREF_DNS + i,
						IpAddress.getByName(
								SipPreference.get_parameter(getUIContext(),
										SipPreference.PREF_SERVER
												+ (i != 0 ? i : ""), ""))
								.toString());
			} catch (UnknownHostException e1) {
				i++;
				continue;
			}

			setOutboundProxy(sip_provider, i);
			i++;
		}
	}*/

	/** Receives incoming calls (auto accept) */
	public void listen()
	{
		Log.v(LOG_TAG, "listen");

		for (UserAgent ua : uas) {
			if (ua != null) {
				ua.printLog("UAS: WAITING FOR INCOMING CALL");

				if (!ua.user_profile.audio && !ua.user_profile.video) {
					ua.printLog("ONLY SIGNALING, NO MEDIA");
				}

				android.util.Log.v("UA", "listen 3");
				ua.listen();
			}
		}
	}

	public void info(char c, int duration) {
		Log.v(LOG_TAG, "info");

		ua.info(c, duration);
	}

	/** Makes a new call */
	public boolean call(String target_url, boolean force) {
		Log.v(LOG_TAG, "call");

		int p = 0;// fym pref;
		boolean found = false;

		if (isRegistered(p))//fym && Receiver.isFast(p))
			found = true;
		else {
			for (p = 0; p < GD.UA_LINES/*fym LINES*/; p++)
				if (isRegistered(p))//fym && Receiver.isFast(p))
				{
					found = true;
					break;
				}
			if (!found && force) {
				p = pref;
				if(true)//fym Receiver.isFast(p))
					found = true;
				else
					for (p = 0; p < GD.UA_LINES/*fym LINES*/; p++)
						if(true)//fym Receiver.isFast(p))
						{
							found = true;
							break;
						}
			}
		}

		if (!found || (ua = uas[p]) == null) {
			if (SP
					.get(getUIContext(), SP.PREF_CALLBACK,
							SP.DEFAULT_CALLBACK)
					&& SP.get(getUIContext(),
							SP.PREF_POSURL,
							SP.DEFAULT_POSURL).length() > 0) {
				SipdroidReceiver.url("n=" + Uri.decode(target_url));
				return true;
			}
			return false;
		}

		ua.printLog("UAC: CALLING " + target_url);

		if (!ua.user_profile.audio && !ua.user_profile.video) {
			ua.printLog("ONLY SIGNALING, NO MEDIA");
		}
		return ua.call(target_url, false);
	}

	public void answercall()
	{
		Log.v("SIP", "====== answer call");

		SipdroidReceiver.stopRingtone();
		ua.accept();
	}

	//拒绝接听呼叫
	public void rejectcall()
	{
		Log.v("SIP", "====== reject call");
		
		//Log.v("Media", "set_schedule_state IDLE 3");
		GD.set_schedule_state(GD.SCHEDULE_STATE.idle);//GeneralDefine.setConferenceOff();
		//关闭RTSP连接
		//Log.v("Call", "close rtsp session");
		//RTSPClient.get_instance().disconnect();
		//RTSPClient.get_instance().stop();

		ua.printLog("UA: HANGUP");
		ua.hangup();
	}
	
	public void restart()//fym
	{
		Log.v("Temp", "restart");
		
		long time = SystemClock.elapsedRealtime();

		int i = 0;
		for (RegisterAgent ra : ras)
		{
			Log.i("Temp", "shutdown 1.2.3 " + ra.username);
			unregister(i);
			Log.i("Temp", "shutdown 1.2.5");
			
			//Log.i("Temp", "shutdown 1.5");
			
			while (ra != null
					&& ra.CurrentState != RegisterAgent.UNREGISTERED
					//&& SystemClock.elapsedRealtime() - time < 2000)
					&& SystemClock.elapsedRealtime() - time < 200)
			{
				try {
					Thread.sleep(10);//Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
			}
			
			i++;
		}
		
		expire();
	}

	public void togglehold() {
		Log.v(LOG_TAG, "togglehold");

		ua.reInvite(null, 0);
	}

	public void transfer(String number) {
		Log.v(LOG_TAG, "transfer");

		ua.callTransfer(number, 0);
	}

	public void togglemute() {
		Log.v(LOG_TAG, "togglemute");

		if (ua.muteMediaApplication())
			;// fym Receiver.onText(Receiver.CALL_NOTIFICATION,
				// getUIContext().getString(R.string.menu_mute),
				// android.R.drawable.stat_notify_call_mute,Receiver.ccCall.base);
		else
			;//fym Receiver.progress();
	}

	public void togglebluetooth() {
		Log.v(LOG_TAG, "togglebluetooth");

		ua.bluetoothMediaApplication();
		//fym Receiver.progress();
	}

	public int speaker(int mode) {
		Log.v(LOG_TAG, "speaker");

		int ret = ua.speakerMediaApplication(mode);
		//fym Receiver.progress();
		return ret;
	}

	public void keepAlive()
	{
		if(true)
		{
			return;//RegisterService定期注册，无须KeepAlive，且注册失败时KeepAlive会报错
		}
		
		Log.v(LOG_TAG, "keepAlive");

		int i = 0;
		for (KeepAliveSip ka : kas) {
			if (ka != null && SipdroidReceiver.on_wlan && isRegistered(i))
				try {
					ka.sendToken();
					;//fym SipdroidReceiver.alarm(60000, LoopAlarm.class);
				} catch (IOException e) {
					if (!Sipdroid.release)
						e.printStackTrace();
				}
			i++;
		}
	}

	// fym
	public void send_message(String ua_id, String message_content)
	{
		//Log.v(LOG_TAG, "test");
		Log.v("Baidu", "Send: " + message_content);

		for(int i = 0; i < GD.UA_LINES; ++i)
		{
			ras[i].send_message(ua_id, message_content);
		}
	}
}
