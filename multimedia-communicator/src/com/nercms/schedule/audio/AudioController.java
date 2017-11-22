package com.nercms.schedule.audio;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;

public class AudioController extends Activity{
	AudioManager _audio_manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
	
	//通话音量(Phone Call Volume)
	/*public void setPhoneCallVolum(int value)
	{
		int max_PhoneCall_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		if(value>max_PhoneCall_volum) value=max_PhoneCall_volum;
		_audio_manager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, value, 0);
	}
	public int getCurrentPhoneCallVolum()
	{
		return _audio_manager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
	}
	public void setMaxPhoneCallVolum()
	{
		int max_PhoneCall_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		_audio_manager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, max_PhoneCall_volum, 0);
	}
	public int getMaxPhoneCallVolum()
	{
		return _audio_manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
	}
	
	//系统音量(System Volume)
	public void setSystemVolum(int value)
	{
		int max_System_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
		if(value>max_System_volum) value=max_System_volum;
		_audio_manager.setStreamVolume(AudioManager.STREAM_SYSTEM, value, 0);
	}
	public int getCurrentSystemVolum()
	{
		return _audio_manager.getStreamVolume(AudioManager.STREAM_SYSTEM);
	}
	public void setMaxSystemVolum()
	{
		int max_System_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
		_audio_manager.setStreamVolume(AudioManager.STREAM_SYSTEM, max_System_volum, 0);
	}
	public int getMaxSystemVolum()
	{
		return _audio_manager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
	}
	
	//音乐音量(Music Volume)(Media Volume)
	public void setMusicVolum(int value)
	{
		int max_Music_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if(value>max_Music_volum) value=max_Music_volum;
		_audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
	}
	public int getCurrentMusicVolum()
	{
		return _audio_manager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}
	public void setMaxMusicVolum()
	{
		int max_Music_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		_audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, max_Music_volum, 0);
	}
	public int getMaxMusicVolum()
	{
		return _audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}
	
	//铃声音量(Ring Volume)
	public void setRingVolum(int value)
	{
		int max_Ring_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_RING);
		if(value>max_Ring_volum) value=max_Ring_volum;
		_audio_manager.setStreamVolume(AudioManager.STREAM_RING, value, 0);
	}
	public int getCurrentRingVolum()
	{
		return _audio_manager.getStreamVolume(AudioManager.STREAM_RING);
	}
	public void setMaxRingVolum()
	{
		int max_Ring_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_RING);
		_audio_manager.setStreamVolume(AudioManager.STREAM_RING, max_Ring_volum, 0);
	}
	public int getMaxRingVolum()
	{
		return _audio_manager.getStreamMaxVolume(AudioManager.STREAM_RING);
	}
	
	//提示音音量(Alarm Volume)
	public void setAlarmVolum(int value)
	{
		int max_Alarm_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		if(value>max_Alarm_volum) value=max_Alarm_volum;
		_audio_manager.setStreamVolume(AudioManager.STREAM_ALARM, value, 0);
	}
	public int getCurrentAlarmVolum()
	{
		return _audio_manager.getStreamVolume(AudioManager.STREAM_ALARM);
	}
	public void setMaxAlarmVolum()
	{
		int max_Alarm_volum=_audio_manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		_audio_manager.setStreamVolume(AudioManager.STREAM_ALARM, max_Alarm_volum, 0);
	}
	public int getMaxAlarmVolum()
	{
		return _audio_manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
	}*/
}