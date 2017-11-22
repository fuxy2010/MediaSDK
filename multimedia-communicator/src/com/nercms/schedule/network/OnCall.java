package com.nercms.schedule.network;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

public class OnCall
{
	private volatile static OnCall _unique_instance = null;
	
	public static OnCall instance()
	{
		if(null == _unique_instance)
		{
			synchronized(OnCall.class)
			{
				if(null == _unique_instance)
				{
					_unique_instance = new OnCall();
				}
			}
		}
		
		return _unique_instance;
	}
	
	private OnCall()
	{
	}
	
	public static Ringtone _ringtone;
	private android.os.Vibrator _vibrator;
	final static long[] vibratePattern = { 0, 1000, 1000 };
	
	public void play()
	{
		Context ctx = GD.get_global_context();
		
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		int rm = am.getRingerMode();
		int vs = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
		KeyguardManager mKeyguardManager = (KeyguardManager)ctx.getSystemService(Context.KEYGUARD_SERVICE);

		//Log.i("SIP", "====== incoming call 5.2");
		
		if (_vibrator == null) _vibrator = (Vibrator)ctx.getSystemService(Context.VIBRATOR_SERVICE);
		
		if(true)//if(SP.get(ctx, SP.PREF_AUTO_ON, SP.DEFAULT_AUTO_ON) && !mKeyguardManager.inKeyguardRestrictedInputMode())
		{
			Log.i("MQTT", "incoming: viberate");
			_vibrator.vibrate(vibratePattern, 1);
		}
		else
		{
			Log.i("MQTT", "incoming: ringtone 1");
			
			//if(rm == AudioManager.RINGER_MODE_VIBRATE || (rm == AudioManager.RINGER_MODE_NORMAL && vs == AudioManager.VIBRATE_SETTING_ON))
			{
				Log.i("MQTT", "incoming: ringtone 2");
				_vibrator.vibrate(vibratePattern, 1);
			}
			
			if (am.getStreamVolume(AudioManager.STREAM_RING) > 0)
			{
				Log.i("MQTT", "incoming: ringtone 3");
				
				//Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		        RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)).play();
				
				/*String sUriSipRingtone = SP.get(ctx,
								SP.PREF_SIPRINGTONE,
								Settings.System.DEFAULT_RINGTONE_URI.toString());
				
				if (!TextUtils.isEmpty(sUriSipRingtone))
				{
					Log.i("MQTT", "incoming: ringtone 5 " + sUriSipRingtone);
					oRingtone = RingtoneManager.getRingtone(ctx, Uri.parse(sUriSipRingtone));
					if (oRingtone != null)
					{
						Log.i("MQTT", "incoming: ringtone 6");
						oRingtone.play(); // ¿ªÊ¼ÏìÁå
					}
				}*/
			}
		}
		
		//Log.i("SIP", "====== incoming call 5.3");
		
		//show_incoming_call(caller);// fym moveTop();		
		
		PowerManager _pm = (PowerManager)GD.get_global_context().getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock _wl = _pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "PersonalIncomingCall");
		_wl.acquire();
		
		GD.disable_keyguard(GD.get_global_context());
		
	}
	
	public void stop()
	{
		if (_vibrator != null)
			_vibrator.cancel();
	}
	
}
