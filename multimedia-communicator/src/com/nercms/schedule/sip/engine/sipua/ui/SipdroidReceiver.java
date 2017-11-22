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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

//rmv import com.nercms.schedule.R;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.SipdroidEngine;
import com.nercms.schedule.sip.engine.sipua.UserAgent;
import com.nercms.schedule.sip.engine.sipua.phone.Call;
import com.nercms.schedule.sip.engine.sipua.phone.Connection;
import com.nercms.schedule.ui.MessageHandlerManager;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
//rmv import com.nercms.schedule.ui.ScheduleMap;

public class SipdroidReceiver extends BroadcastReceiver
{
	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	final static String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
	final static String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
	final static String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
	final static String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
	final static String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
	final static String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
	final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
	final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
	final static String ACTION_DEVICE_IDLE = "com.android.server.WifiManager.action.DEVICE_IDLE";
	final static String ACTION_VPN_CONNECTIVITY = "vpn.connectivity";
	final static String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
	final static String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
	final static String METADATA_DOCK_HOME = "android.dock_home";
	final static String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
	final static String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
	final static int EXTRA_DOCK_STATE_DESK = 1;
	final static int EXTRA_DOCK_STATE_CAR = 2;

	final int MSG_SCAN = 1;
	final int MSG_ENABLE = 2;

	final static long[] vibratePattern = { 0, 1000, 1000 };

	public static int docked = -1, headset = -1, bluetooth = -1;

	public static Context mContext;
	public static SipdroidListener listener_video;
	public static Call ccCall;
	public static Connection ccConn;
	public static int call_state;
	public static int call_end_reason = -1;

	public static String pstn_state;
	public static long pstn_time;
	public static String MWI_account;
	private static String laststate, lastnumber;
	
	//singleton
	public static SipdroidEngine _unique_sipdroid_engine = null;

	public static synchronized SipdroidEngine engine(Context context)
	{
		mContext = GD.get_global_context();//context;
		
		if(null == _unique_sipdroid_engine)
		{
			//创建SipdroidEngine
			// Log.v("SipdroidEngine", "new SipdroidEngine");
			android.util.Log.i("Baidu", "new SipdroidEngine");
			
			if(null != mContext)
			{
				//SipdroidReceiver.onText(SipdroidReceiver.REGISTER_NOTIFICATION, mContext.getString(R.string.reg), R.drawable.sym_presence_idle, 0);
			}
			
			_unique_sipdroid_engine = new SipdroidEngine();
			_unique_sipdroid_engine.StartEngine();
			
			if(Integer.parseInt(Build.VERSION.SDK) >= 8)
				;//fym Bluetooth.init();
		}
		else
		{
			// Log.v("SipdroidEngine", "exist SipdroidEngine");
			_unique_sipdroid_engine.CheckEngine();
		}
		
		return _unique_sipdroid_engine;
	}
	
	public static Ringtone oRingtone;
	static PowerManager.WakeLock wl;
	static android.os.Vibrator v;

	/**
	 * 关闭铃声
	 */
	public static void stopRingtone() {
		if (v != null)
			v.cancel();
		if (SipdroidReceiver.oRingtone != null) {
			Ringtone ringtone = SipdroidReceiver.oRingtone;
			oRingtone = null;
			ringtone.stop();
		}
	}

	public static void onState(int state, String caller)
	{
		android.util.Log.i("SIP", "onState: " + state + " caller: " + caller);
		
		// caller为来电号码
		if (ccCall == null)
		{
			ccCall = new Call();
			ccConn = new Connection();
			ccCall.setConn(ccConn);
			ccConn.setCall(ccCall);
		}
		
		Log.i("SIP", "====== incoming call 5.0.1");

		if (call_state != state)
		{
			Log.i("SIP", "====== incoming call 5.0.2");
			
			if (state != UserAgent.UA_STATE_IDLE)
				call_end_reason = -1;

			call_state = state;
			
			Log.i("SIP", "====== incoming call 5.0.3");

			switch (call_state) {
			case UserAgent.UA_STATE_INCOMING_CALL:
				//GD.hang_up_phone_call(mContext);// 挂断正在进行的语音电话
				Log.i("SIP", "====== incoming call 5.0.5");
				enable_wifi(true);
				Log.i("SIP", "====== incoming call 5.0.6");
				bluetooth = -1;
				String text = caller.toString();
				
				if (text.indexOf("<sip:") >= 0 && text.indexOf("@") >= 0)
					text = text.substring(text.indexOf("<sip:") + 5, text.indexOf("@"));
				
				//Log.i("SIP", "====== incoming call 5.0.7");
				
				String text2 = caller.toString();
				
				if (text2.indexOf("\"") >= 0)
					text2 = text2.substring(text2.indexOf("\"") + 1, text2.lastIndexOf("\""));
				
				//Log.i("SIP", "====== incoming call 5.0.8");
				
				broadcastCallStateChanged("RINGING", caller);
				
				//Log.i("SIP", "====== incoming call 5.0.8.1");
				
				mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
				
				//Log.i("SIP", "====== incoming call 5.0.9");
				
				ccCall.setState(Call.State.INCOMING);
				ccConn.setUserData(null);
				ccConn.setAddress(text, text2);
				ccConn.setIncoming(true);
				ccConn.date = System.currentTimeMillis();
				ccCall.base = 0;
				
				//Log.i("SIP", "====== incoming call 5.1");

				AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
				int rm = am.getRingerMode();
				int vs = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
				KeyguardManager mKeyguardManager = (KeyguardManager)mContext.getSystemService(Context.KEYGUARD_SERVICE);
		
				//Log.i("SIP", "====== incoming call 5.2");
				
				if (v == null)
					v = (Vibrator) mContext
							.getSystemService(Context.VIBRATOR_SERVICE);
				if ((pstn_state == null || pstn_state.equals("IDLE"))
						&& SP.get(mContext,
								SP.PREF_AUTO_ON,
								SP.DEFAULT_AUTO_ON)
						&& !mKeyguardManager.inKeyguardRestrictedInputMode())
					v.vibrate(vibratePattern, 1);
				else {
					if ((pstn_state == null || pstn_state.equals("IDLE"))
							&& (rm == AudioManager.RINGER_MODE_VIBRATE || (rm == AudioManager.RINGER_MODE_NORMAL && vs == AudioManager.VIBRATE_SETTING_ON)))
						v.vibrate(vibratePattern, 1);
					if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {
						String sUriSipRingtone = SP
								.get(mContext,
										SP.PREF_SIPRINGTONE,
										Settings.System.DEFAULT_RINGTONE_URI
												.toString());
						if (!TextUtils.isEmpty(sUriSipRingtone)) {
							oRingtone = RingtoneManager.getRingtone(mContext,
									Uri.parse(sUriSipRingtone));
							if (oRingtone != null)
								oRingtone.play(); // 开始响铃
						}
					}
				}
				
				//Log.i("SIP", "====== incoming call 5.3");
				
				show_incoming_call(caller);// fym moveTop();
				
				if(wl == null)
				{
					PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
					wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.onState");
				}
				wl.acquire();
				Log.i("SIP", "acquire 1");
				// fym Checkin.checkin(true);
				break;
			case UserAgent.UA_STATE_OUTGOING_CALL:
				bluetooth = -1;
				onText(MISSED_CALL_NOTIFICATION, null, 0, 0);
				// fym engine(mContext).registerUdp();
				broadcastCallStateChanged("OFFHOOK", caller);
				ccCall.setState(Call.State.DIALING);
				ccConn.setUserData(null);
				ccConn.setAddress(caller, caller);
				ccConn.setIncoming(false);
				ccConn.date = System.currentTimeMillis();
				ccCall.base = 0;
				//fym show_outgoing_call(caller);// fym moveTop();
				break;
			case UserAgent.UA_STATE_IDLE:
				broadcastCallStateChanged("IDLE", null);
				onText(CALL_NOTIFICATION, null, 0, 0);
				ccCall.setState(Call.State.DISCONNECTED);
				if (listener_video != null)
					listener_video.onHangup();
				stopRingtone();
				if (wl != null && wl.isHeld())
					wl.release();
				// fym mContext.startActivity(createIntent(InCallScreen.class));
				ccConn.log(ccCall.base);
				ccConn.date = 0;
				android.util.Log.v("UA", "listen 1");
				engine(mContext).listen();
				System.out.println("onstate");
				break;
			case UserAgent.UA_STATE_INCALL:
				broadcastCallStateChanged("OFFHOOK", null);
				if (ccCall.base == 0) {
					ccCall.base = SystemClock.elapsedRealtime();
				}
				//fym progress();
				ccCall.setState(Call.State.ACTIVE);
				stopRingtone();
				if (wl != null && wl.isHeld())
					wl.release();
				// fym mContext.startActivity(createIntent(InCallScreen.class));
				break;
			case UserAgent.UA_STATE_HOLD:
				//rmv onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_on_hold), android.R.drawable.stat_sys_phone_call_on_hold, ccCall.base);
				ccCall.setState(Call.State.HOLDING);
				break;
			}
		}
	}

	static String cache_text;
	static int cache_res;
	
	public final static int MWI_NOTIFICATION = 1;
	public final static int CALL_NOTIFICATION = 2;
	public final static int MISSED_CALL_NOTIFICATION = 3;
	public final static int AUTO_ANSWER_NOTIFICATION = 4;
	public final static int REGISTER_NOTIFICATION = 5;

	//根据状态在标题栏显示相应的图标
	public static void onText(int type, String text, int mInCallResId, long base)
	{
		if(GD.is_in_schedule())
		{
			return;
		}
		
		/*if (type >= REGISTER_NOTIFICATION
				&& mInCallResId == R.drawable.sym_presence_available
				&& !SP.get(SipdroidReceiver.mContext,
						SP.PREF_REGISTRATION,
						SP.DEFAULT_REGISTRATION))
			;//Log.v("UA", "onText 11");// fym;//fym text = null;
		
		Log.v("UA", "onText: " + text + ", type: " + type + " id: " + mInCallResId + " base: " + base);
		
		NotificationManager mNotificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		
		if(text != null)
		{
			//Log.v("UA", "onText 2");// fym
			
			Notification notification = new Notification();
			notification.icon = mInCallResId;//在标题栏选用何种图标显示状态，在线/离线/空闲
			
			if(type == MISSED_CALL_NOTIFICATION)
			{
				//Log.v("UA", "onText 3");// fym
				
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
				notification.setLatestEventInfo(mContext, text, mContext.getString(R.string.app_name), PendingIntent.getActivity(mContext, 0, createCallLogIntent(), 0));
				
				if (SP.get(SipdroidReceiver.mContext, SP.PREF_NOTIFY, SP.DEFAULT_NOTIFY))
				{
					notification.flags |= Notification.FLAG_SHOW_LIGHTS;
					notification.ledARGB = 0xff0000ff; // blue
					notification.ledOnMS = 125;
					notification.ledOffMS = 2875;
				}
			}
			else
			{
				//Log.v("UA", "onText 5");// fym
				
				//点击标题栏中的图标返回地图界面
				notification.contentIntent = PendingIntent.getActivity(mContext, 0, createIntent(ScheduleMap.class, "", ""), 0);
				
				if (mInCallResId == R.drawable.sym_presence_away)//图标为离线状态
				{
					Log.v("UA", "onText 6");// fym
					notification.flags |= Notification.FLAG_SHOW_LIGHTS;
					notification.ledARGB = 0xffff0000; // red
					notification.ledOnMS = 125;
					notification.ledOffMS = 2875;
				}
				
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification);
				
				contentView.setImageViewResource(R.id.icon, notification.icon);
				contentView.setTextViewText(R.id.text1, "点击进入欢迎界面");
				contentView.setTextViewText(R.id.text2, text);
				notification.contentView = contentView;
			}
			mNotificationMgr.notify(GID.MSG_REGISTER_ICON, notification);
		}
		else
		{
			//Log.v("UA", "onText 7");// fym
			
			mNotificationMgr.cancel(type);
		}*/
	}
	
	public static void registered()
	{
		//fym pos(true);开启或关闭GPS
	}

	//开启或关闭WIFI
	static void enable_wifi(boolean enable)
	{
		if (!SP.get(mContext, SP.PREF_OWNWIFI, SP.DEFAULT_OWNWIFI))
			return;

		if (enable && !SP.get(mContext,
						SP.PREF_WIFI_DISABLED,
						SP.DEFAULT_WIFI_DISABLED))
			return;
		
		WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		
		ContentResolver cr = SipdroidReceiver.mContext.getContentResolver();
		
		if (!enable && Settings.Secure.getInt(cr, Settings.Secure.WIFI_ON, 0) == 0)
			return;

		SP.set(SipdroidReceiver.mContext, SP.PREF_WIFI_DISABLED, !enable);

		if (enable)
		{
			Intent intent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
			intent.putExtra(WifiManager.EXTRA_NEW_STATE, wm.getWifiState());
			mContext.sendBroadcast(intent);
		}
		
		wm.setWifiEnabled(enable);
	}

	public static void url(final String opt) {
		(new Thread() {
			public void run() {
				try {
					URL url = new URL(SP.get(mContext,
							SP.PREF_POSURL,
							SP.DEFAULT_POSURL)
							+ "?" + opt);
					BufferedReader in;
					in = new BufferedReader(new InputStreamReader(
							url.openStream()));
					in.close();
				} catch (IOException e) {
					if (!Sipdroid.release)
						e.printStackTrace();
				}

			}
		}).start();
	}

	static boolean was_playing;

	static void broadcastCallStateChanged(String state, String number)
	{
		//Log.i("SIP", "====== incoming call 5.0.8.1, " + state + ", " + number);
		
		if (state == null)
		{
			state = laststate;
			number = lastnumber;
		}
		
		//Log.i("SIP", "====== incoming call 5.0.8.2 " + android.os.Build.BRAND);
		
		//此段阻止被叫振铃
		if(false)// == android.os.Build.BRAND.equalsIgnoreCase("huawei"))//华为P7在此阻塞
		{
			Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
			intent.putExtra("state", state);
			if(number != null)
				intent.putExtra("incoming_number", number);
			intent.putExtra("schedule"/* fym mContext.getString(R.string.app_name) */, true);
			mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
		}
		
		//Log.i("SIP", "====== incoming call 5.0.8.3");
		
		if(state.equals("IDLE"))
		{
			if (was_playing)
			{
				if (pstn_state == null || pstn_state.equals("IDLE"))
					mContext.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
				was_playing = false;
			}
			
			//Log.i("SIP", "====== incoming call 5.0.8.3.1");
		}
		else
		{
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			if ((laststate == null || laststate.equals("IDLE")) && (was_playing = am.isMusicActive()))
				mContext.sendBroadcast(new Intent(PAUSE_ACTION));
			
			//Log.i("SIP", "====== incoming call 5.0.8.3.2");
		}
		laststate = state;
		lastnumber = number;
	}

	//如renew_time为0则取消该intent
	//否则则在renew_time毫秒后发送intent启动cls	
	public static void alarm(long renew_time, Class<?> cls)
	{
		//如get_global被销毁代表程序已关闭
		if(null == mContext)
			return;
		
		Intent intent = new Intent(mContext, cls);
		
		PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		
		AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		
		am.cancel(sender);
		
		if(renew_time > 0)
		{
			am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + renew_time, sender);
		}

	}

	public static long rereg_expire_time;

	public static void reRegister(int renew_time)
	{
		if (renew_time == 0)
		{
			rereg_expire_time = 0;
		}
		else
		{
			if(rereg_expire_time != 0 && renew_time * 1000 + SystemClock.elapsedRealtime() > rereg_expire_time)
				return;
			
			rereg_expire_time = renew_time * 1000 + SystemClock.elapsedRealtime();
		}
		
		alarm((renew_time - 15) * 1000, OnReRegister.class);
	}

	static Intent createIntent(Class<?> cls, String key, String value)
	{
		//普通会议 号码以S结尾，SOS会议 号码以E结尾
		//boolean is_sos = value.contains("E");
		Intent startActivity = new Intent();
		//startActivity.setAction(Intent.ACTION_MAIN);
		//startActivity.addCategory(Intent.CATEGORY_LAUNCHER);
		startActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity.setClass(mContext, cls);
		return startActivity;
	}

	public static Intent createCallLogIntent() {
		Intent intent = new Intent(Intent.ACTION_VIEW, null);
		intent.setType("vnd.android.cursor.dir/calls");
		return intent;
	}

	static Intent createHomeDockIntent() {
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		if (docked == EXTRA_DOCK_STATE_CAR) {
			intent.addCategory(CATEGORY_CAR_DOCK);
		} else if (docked == EXTRA_DOCK_STATE_DESK) {
			intent.addCategory(CATEGORY_DESK_DOCK);
		} else {
			return null;
		}

		ActivityInfo ai = intent.resolveActivityInfo(
				mContext.getPackageManager(), PackageManager.GET_META_DATA);
		if (ai == null) {
			return null;
		}

		if (ai.metaData != null && ai.metaData.getBoolean(METADATA_DOCK_HOME)) {
			intent.setClassName(ai.packageName, ai.name);
			return intent;
		}

		return null;
	}

	public static Intent createHomeIntent() {
		Intent intent = createHomeDockIntent();
		if (intent != null) {
			try {
				return intent;
			} catch (ActivityNotFoundException e) {
			}
		}
		intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_HOME);
		return intent;
	}

	static Intent createMWIIntent() {
		Intent intent;

		if (MWI_account != null)
			intent = new Intent(Intent.ACTION_CALL, Uri.parse(MWI_account));
		else
			intent = new Intent(Intent.ACTION_DIAL);
		return intent;
	}
	
	//收到SIP呼叫后显示被叫界面
	public static void show_incoming_call(String caller)
	{
		Log.i("Baidu", "Incoming call from " + caller);
		
		MessageHandlerManager.get_instance().handle_message(GID.MSG_INCOMING_CALL, caller, GD.MEDIA_INSTANCE);
		
		if(true) return;
		else
		{
			Intent intent = new Intent();
			//rmv intent.setClass(mContext, ScheduleMap.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("intent_flag", 2);
			intent.putExtra("is_sos", caller.contains("E"));//SOS调度呼叫
			intent.putExtra("from_pc", caller.contains("P"));//PC调度呼叫
			mContext.startActivity(intent);
		}
	}

	public static boolean on_wlan = true;//fym

	public static int speakermode()
	{
		if (docked > 0 && headset <= 0)
			return AudioManager.MODE_NORMAL;
		else
			return AudioManager.MODE_IN_CALL;
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(true)//fym
			return;
		
		//暂毋须调用//////////////////////////////////////////////////////////////////////
		
		String action = intent.getAction();
		
		if(!Sipdroid.on(context))
			return;
		
		if(!Sipdroid.release)
			Log.i("SipUA:", action);
		
		if(mContext == null)
			mContext = context;
		
		Log.v("Debug", "action: " + action);
		
		if(action.equals(Intent.ACTION_BOOT_COMPLETED))//启动完毕
		{
			//on_vpn(false);
			//engine(context).register();
		}
	}
}
