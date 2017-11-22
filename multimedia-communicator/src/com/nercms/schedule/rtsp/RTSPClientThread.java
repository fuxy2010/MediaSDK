package com.nercms.schedule.rtsp;

import android.util.Log;
import com.nercms.schedule.ui.MessageHandlerManager;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;

public class RTSPClientThread extends Thread 
{
	private boolean _running = true;
	public static Object _event = new Object();
	
	public RTSPClientThread()
	{
		super();
	}
	
	@Override
	protected void finalize()
	{
	}
	
	@Override
	public void run()
	{
		try
		{
			while(true == _running)
			{
				synchronized(RTSPClientThread._event)
				{
					_event.wait(300);
				}
				
				//if(false == GeneralDefine.is_in_schedule())
				if(GD.SCHEDULE_STATE.idle == GD.get_scheduel_state())
				{
					//Log.i("RTSP", "stop 1");
					RTSPClient.get_instance().stop();
					
					RTSPClient.get_instance().temporary_tcp_connection();
				}
				else if(GD.SCHEDULE_STATE.closing != GD.get_scheduel_state())
				{
					if(true == RTSPClient.get_instance().is_playing())
					{
						if((true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp")
							&& false == GD._i_am_video_source
							&& GD.RTSP_RECONNECT_INTERVAL < (System.currentTimeMillis() - GD._latest_rtsp_video_data_timestamp))
							&&//||
							(/*双UA true == GD.AUDIO_PROTOCOL.equalsIgnoreCase("tcp")
							&& */GD.RTSP_RECONNECT_INTERVAL < (System.currentTimeMillis() - GD._latest_rtsp_audio_data_timestamp)))
						{
							Log.i("RTSP", "reconnect rtsp session.");
							
							MessageHandlerManager.get_instance().handle_message(GID.MSG_RTSP_SESSION_RECONNECT, 0, GD.MEDIA_INSTANCE);
							
							//Log.i("RTSP", "stop 2");
							RTSPClient.get_instance().stop();//会话中超过5秒钟未收到媒体数据则先断开在重连
						}
					}
					
					//Log.i("RTSP", "start 1");
					RTSPClient.get_instance().start();
					//Log.i("RTSP", "start 2");
				}
				
				sleep(20);
			}
			
			//Log.i("RTSP", "stop 3");
			RTSPClient.get_instance().stop();
		}
		catch(Exception e)
		{
		}
	}
	
	public void free()
	{
		_running = false;
		
		synchronized(RTSPClientThread._event)
		{
			_event.notify();
		}
	}
}
