package com.nercms.schedule.misc;

import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.nercms.schedule.audio.AECMModule;
import com.nercms.schedule.audio.AECModule;
import com.nercms.schedule.audio.AudioRecvDecPlay;
import com.nercms.schedule.audio.AudioRecEncSend;
import com.nercms.schedule.audio.NSModule;
import com.nercms.schedule.audio.VADModule;
import com.nercms.schedule.mediacodec.AVCHardEncoder;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaStatistics;
import com.nercms.schedule.video.H264Decoder;
import com.nercms.schedule.video.H264Encoder;
import com.nercms.schedule.rtsp.RTSPClientThread;

public class MediaThreadManager
{
	//��������
	private volatile static MediaThreadManager _unique_instance = null;
		
	//��Ƶ�ɱ෢
	private AudioRecEncSend _audio_recorder = null;//��Ƶ�ɱ෢�߳�
	public boolean _audio_recorder_idle = true;//��Ƶ�ɱ෢�߳��Ƿ��ת
	
	//��Ƶ�սⲥ
	private AudioRecvDecPlay _audio_player = null;//��Ƶ�ս⼰���߳�
	public boolean _audio_play_idle = true;//��Ƶ�ս⼰���߳��Ƿ��ת
	
	//��Ƶ�ɱ෢
	private H264Encoder _video_send = null;//��Ƶ���߳�
	//private AVCHardEncoder _video_send = null;//��Ƶ���߳�

	//��Ƶ�սⲥ
	private H264Decoder _video_receive = null;//��Ƶ�ռ��ⲥ�߳�
	public boolean _video_receive_idle = true;//��Ƶ�ռ��ⲥ�߳��Ƿ��ת
	
	private RTSPClientThread _rtsp_client_thread = null;
	
	//��ȡsingletonʵ��
	public static MediaThreadManager get_instance()
	{
		// ���ʵ��,���ǲ����ھͽ���ͬ��������
		if(null == _unique_instance)
		{
			//���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(MediaThreadManager.class)
			{
				//����˫�ؼ��
				if(null == _unique_instance)
				{
					_unique_instance = new MediaThreadManager();
				}
			}
		}
		
		return _unique_instance;
	}
	
	private MediaThreadManager()
	{
		MediaSocketManager.get_instance();//����ý���շ�socket
	}
	
	public void start_media_thread()
	{
		if(false)//if(true == GD.AUDIO_PROTOCOL.equalsIgnoreCase("tcp") || true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
		{
			_rtsp_client_thread = new RTSPClientThread();
			_rtsp_client_thread.start();
			Log.v("RTSP", "start rtsp client thread!");
		}
		
		//AEC
		if(GD.AECType.AEC == GD.AEC_TYPE)
		{
			AECModule._handler = AECModule.AEC_Create();
			AECModule.AEC_Init(AECModule._handler, 8000, 8000);
		}
		else if(GD.AECType.AECM == GD.AEC_TYPE)
		{
			AECMModule._handler = AECMModule.AECM_Create();
			AECMModule.AECM_Init(AECMModule._handler, 8000);
		}
		
		//if(GD.AECType.NONE != GD.AEC_TYPE)
		{
			if(-1 == NSModule._handler)
			{
				NSModule._handler = NSModule.NS_Create();
				NSModule.NS_Init(NSModule._handler, 8000);
				NSModule.NS_set_policy(NSModule._handler, 1);
			}
			
			if(-1 == VADModule._handler)
			{
				VADModule._handler = VADModule.VAD_Create();
				VADModule.VAD_Init(VADModule._handler);
				VADModule.VAD_SetMode(VADModule._handler, 2);
				VADModule.VAD_ValidRateAndFrameLength(8000, 160);
			}
		}
		
		//������Ƶ�ɱ෢�߳�
		if(_audio_recorder == null) 
		{
			if(true == GD.AUDIO_CODEC.equalsIgnoreCase("amr-wbp"))
			{
				//_audio_recorder = new AMREncoder();
			}
			else
			{
				_audio_recorder = new AudioRecEncSend();
			}
			
			_audio_recorder.start();
		}
		
		//������Ƶ�ռ��ⲥ�߳�
		if(_audio_player == null)
		{
			if(true == GD.AUDIO_CODEC.equalsIgnoreCase("amr-wbp"))
			{
				//_audio_player = new AMRDecoder();
			}
			else
			{
				_audio_player = new AudioRecvDecPlay(false);
			}
			
			_audio_player.start();
			Log.v("Audio", "start audio_play_thread!");
		}
		
		//������Ƶ�ɱ෢�߳�
		if(null == _video_send)
		{
			_video_send = new H264Encoder();
			//_video_send = new AVCHardEncoder();
			_video_send._is_running = true;
			_video_send.start();
		}
		
		//������Ƶ�ռ��ⲥ�߳�
		if(null == _video_receive)
		{
			_video_receive = new H264Decoder();
			_video_receive._receiving = true;
			_video_receive.start();
		}
	}
	
	public void stop_media_thread()
	{
		try
		{
			if(null != _rtsp_client_thread)
			{
				_rtsp_client_thread.free();
				_rtsp_client_thread.join();
				_rtsp_client_thread = null;
			}
			
			if(null != _audio_recorder)
			{
				_audio_recorder.free();				
				_audio_recorder.join();				
				_audio_recorder = null;
			}
			
			if(null != _audio_player)
			{
				_audio_player.free();				
				_audio_player.join();				
				_audio_player = null;
			}
			
			if(null != _video_send)
			{
				_video_send.free();				
				_video_send.join();				
				_video_send = null;
			}
			
			if(null != _video_receive)
			{
				_video_receive.free();				
				_video_receive.join();				
				_video_receive = null;
			}
			
			//AEC
			if(GD.AECType.AEC == GD.AEC_TYPE)
			{
				if(-1 != AECModule._handler)
				{
					AECModule.AEC_Free(AECModule._handler);
					AECModule._handler = -1;
				}
			}
			else if(GD.AECType.AECM == GD.AEC_TYPE)
			{
				if(-1 != AECMModule._handler)
				{
					AECMModule.AECM_Free(AECMModule._handler);
					AECMModule._handler = -1;
				}
			}
			
			if(false)//if(GD.AECType.NONE != GD.AEC_TYPE)
			{
				Log.v("Baidu", "NS_Free " + NSModule._handler + ", VAD_Free " + VADModule._handler);
				
				if(-1 != NSModule._handler)
				{
					NSModule.NS_Free(NSModule._handler);
					NSModule._handler = -1;
				}
				
				if(-1 != VADModule._handler)
				{
					VADModule.VAD_Free(VADModule._handler);
					VADModule._handler = -1;
				}
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	public void video_encode(byte[] raw_video_frame, boolean rotate)
	{
		if(null != _video_send)
		{
			_video_send.encode(raw_video_frame, rotate);
		}
	}
	
	//public void set_video_render_view(VideoRenderView video_render_view)
	public void set_video_render_view(SurfaceView video_render_view)
	{
		if(null != _video_receive)
		{
			_video_receive.set_render_view(video_render_view);
		}
	}
	
	public MediaStatistics get_media_statistics(int media_type)
	{
		if(0 == media_type)
		{
			if(null != _audio_player)
			{
				return _audio_player._media_statistics;
			} 
		}
		else if(1 == media_type)
		{
			if(null != _video_receive)
			{
				return _video_receive._statistics;
			}
		}
		
		return null;
	}
	
	public void thread_statistics()
	{
		if(null != _audio_player)
		{
			_audio_player.thread_statistics_record();
		}
		
		if(null != _audio_recorder)
		{
			_audio_recorder.thread_statistics_record();
		}
		
		if(null != _video_send)
		{
			_video_send.thread_statistics_record();
		}
		
		if(null != _video_receive)
		{
			_video_receive.thread_statistics_record();
		}
	}
	
	public void self_loop_audio_decode(byte[] packet, int length)
	{
		if(null == _audio_player)
			return;
		
		_audio_player.self_loop_audio_decode(packet, length);
	}
	
	public void add_video_packetx(byte[] packet, int length)
	{
		if(null == _video_receive)
			return;
		
		_video_receive.add_video_packet(packet, length);
	}
}
