package com.nercms.schedule.audio;

import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.googlecode.androidilbc.Codec;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.misc.MediaSocketManager;
import com.nercms.schedule.misc.MediaStatistics;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.misc.ThreadStatistics;
import com.nercms.schedule.sip.engine.net.RtpSocket;
import com.nercms.schedule.ui.MediaInstance;
import com.webrtc.ilbc.iLBCCodec;

public class AudioRecvDecPlay  extends Thread
{
	public MediaStatistics _media_statistics = null;//音频数据接收统计
	public static boolean _hands_free = GD.DEFAULT_HANDS_FREE;
	
	boolean _is_pcm = false;// 待解码的音频流格式控制开关（AMR_NB或者PCM）
	protected boolean _playing = false;// 音频播放标志
	
	private AudioTrack _audio_track = null;
	
	byte[] _payload = new byte[128];//用于暂存收到的音频包净荷
	int _payload_length = 0;//语音包净荷长度
	int _frame_index = 0;//一个音频包内的音频帧索引
	int _decoded_len = 0;//一音频包内已解码的长度
	byte[] _frame = new byte[320];//一帧缓冲
	byte[] _aec_frame_10ms = new byte[160];
	byte[] _frame_aec = new byte[320];
	boolean _audio_initialized = false;//AudioTrack是否已初始化
	int _next_fetch_audio_sequence = 0;//下一次将要取的音频包
	byte[] _predictive_audio_frame = new byte[3072];//预测补偿丢包，最多补偿3个包（9帧）
	
	private int _aes_index = 1;
	
	/*
	 * 构造函数 功能：初始化套接字、初始化音频流格式
	 */
	public AudioRecvDecPlay(boolean is_pcm)
	{
		super();
		
		try
		{
			_playing = true;//打开音频流播放标志
			
			System.gc();
			
			_media_statistics = new MediaStatistics(0);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void finalize()
	{
		_media_statistics = null;
	}
	 
	public double get_packet_lost_rate()
	{
		if(null == _media_statistics)
			return 0.0;
		
		return _media_statistics.get_packet_lost_rate();
	}
	
	private ThreadStatistics _recv_thread_statistics = new ThreadStatistics("Audio Recv Thread");
	private ThreadStatistics sa = new ThreadStatistics("Write");
	
	public void thread_statistics_record()
	{
		if(null != _recv_thread_statistics)
		{
			_recv_thread_statistics.record();
			sa.record();
		}
		
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "Discard " + get_discard_count() + " audio frame\r\n");
	}
	
	//主动丢弃语音帧统计
	private long _discard_count = 0;//主动丢弃的语音帧数
	private Object _discard_count_lock = new Object();
	private void add_discard_count()
	{
		synchronized(_discard_count_lock)
		{
			++_discard_count;
		}
	}
	private long get_discard_count()
	{
		long count = 0;
		
		synchronized(_discard_count_lock)
		{
			count = _discard_count;
			_discard_count = 0;
		}
		
		return count;
	}
	
	private void stop_audio_track()
	{
		if(null != _audio_track)
		{
			if(true)//AudioTrack.PLAYSTATE_STOPPED != _audio_track.getPlayState())
				_audio_track.stop();
			
			_audio_track.release();
			_audio_track = null;
			
			Log.i("Audio", "------ close audio track");
		}
	}
	
	private void start_audio_track(boolean hands_free)
	{
		if(null != _audio_track &&
			AudioTrack.STATE_INITIALIZED == _audio_track.getState() &&//已初始化
			AudioTrack.PLAYSTATE_PLAYING == _audio_track.getPlayState() &&//正在播放
			((true == hands_free && AudioManager.STREAM_MUSIC == _audio_track.getStreamType()) ||//免提且扬声器
			(false == hands_free && AudioManager.STREAM_VOICE_CALL == _audio_track.getStreamType())))//非免提且听筒
		{
			return;
		}
		
		stop_audio_track();
		
		//根据采样率，采样精度，单双声道获取语音帧大小
		int buffer_size = AudioTrack.getMinBufferSize(8000,//采样频率，每秒8k个样点
													AudioFormat.CHANNEL_CONFIGURATION_MONO,//单声道
													AudioFormat.ENCODING_PCM_16BIT);//采样精度，每个样点16bit
		
		Log.i("Media", "Play buf " + buffer_size);
		
		//统一手机最小缓存尺寸
		//应适当加大避免播放write函数耗时抖动导致主动丢包
		//buffer_size = (4096 > buffer_size) ? 4096 : buffer_size;//适当增加缓冲，减少语音咔嗒声
		buffer_size = (2048 > buffer_size) ? 2048 : buffer_size;//适当增加缓冲，减少语音咔嗒声
		//并确保底层硬件采集缓存的大小为一帧原始语音（320字节）的整数倍
		buffer_size = ((buffer_size / 320) + (0 == (buffer_size % 320) ? 0 : 1)) * 320;
		//buffer_size *= 4;
		Log.v("Baidu", "AudioTrack Buffer " + buffer_size);
		
		_audio_track = new AudioTrack((true == hands_free)? AudioManager.STREAM_MUSIC : AudioManager.STREAM_VOICE_CALL,
									8000,
									AudioFormat.CHANNEL_CONFIGURATION_MONO,
									AudioFormat.ENCODING_PCM_16BIT,
									buffer_size,
									AudioTrack.MODE_STREAM);
		
		_audio_track.pause();	
		
		//将数组转换成java.util.ArrayList类型asList()
		//数组的排序sort()
		//数组的二分查找binarySearch()
		//两个数组的比较equals()
		//给数组赋初值fill()
		java.util.Arrays.fill(_frame, (byte)0);
		for(int i = 0; i <= (buffer_size / 320); ++i)
		{
			_audio_track.write(_frame, 0, 320);
		}
		
		_audio_track.play();
		System.gc();//开始播放音频则清空系统底层音频数据缓存					
		
		Log.i("Audio", "------ create audio track " + ((true == _hands_free) ? "hands free" : "normal"));
	}
	
	int _latest_sequence = -1;
	private int recv_audio_statistics(byte[] data, int length, int rtp_header_offset)
	{
		//语音包含24个字节的双RTP包头		
		if(null == data || (rtp_header_offset + 12) >= data.length || (rtp_header_offset + 12) >= length)
		{
			Log.i("Audio", "wrong audio packet " + length);
			return -1;
		}
		
		/*typedef struct tagRTPHeader//fym struct RTPHeader
		{
			unsigned char csrccount:4;
			unsigned char extension:1;
			unsigned char padding:1;
			unsigned char version:2;

			unsigned char payloadtype:7;
			unsigned char marker:1;

			unsigned short sequencenumber;
			unsigned long timestamp;
			unsigned long ssrc;
		}
		RTPHeader, *RTPHeaderPtr;//fym*/
		
		//更新统计信息		
		int sequence = (GD.byte_2_int(data[2 + rtp_header_offset]) << 8)
					+ GD.byte_2_int(data[3 + rtp_header_offset]);
		
		long timestamp = (GD.byte_2_int(data[4 + rtp_header_offset]) << 24)
						+ (GD.byte_2_int(data[5 + rtp_header_offset]) << 16)
						+ (GD.byte_2_int(data[6 + rtp_header_offset]) << 8)
						+ GD.byte_2_int(data[7 + rtp_header_offset]);
		
		int lost_packet_count = _media_statistics.update_packet_statistics(sequence, timestamp, true, length);
		
		//Log.i("Audio", "as " + sequence);
		
		if(-1 == _latest_sequence)
		{
			_latest_sequence = sequence;
		}
		else
		{
			if(sequence != _latest_sequence + 1)
			{
				Log.i("Baidu", "lost audio packet " + _latest_sequence + " " + sequence);
			}
			
			_latest_sequence = sequence;
		}
		
		return lost_packet_count;
	}
	
	public void self_loop_audio_decode(byte[] data, int length)
	{
		if(null == data || length <= 24)
			return;
		
		//数据包统计
		/*if(false == recv_audio_statistics(data, length, 12))
			return;*/
		
		RtpSocket send_socket = MediaSocketManager.get_instance().get_internal_audio_send_socket();
		RtpSocket recv_socket = MediaSocketManager.get_instance().get_audio_recv_socket();
		
		if(null == send_socket || null == recv_socket)
			return;
		
		System.arraycopy(data, 12, send_socket._rtp_packet.getPacket(), 0, length - 12);
		send_socket._rtp_packet.setPayloadLength(length - 24);//必须的
		
		try
		{
			//Log.v("Baidu", "AD " + length);
			//发送给本机socket进行接收解码播放
			send_socket.send(recv_socket.get_sipdroid_socket().getLocalPort());
			//Log.i("Audio", "SA++++++");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void play_with_aec()
	{
		if(GD.AECType.AEC == GD.AEC_TYPE)
		{
			System.arraycopy(_frame, 0, _aec_frame_10ms, 0, 160);
			AECModule.AEC_BufferFarend(AECModule._handler, _aec_frame_10ms, (short)80);
			
			long timestamp = System.currentTimeMillis();
			_audio_track.write(_aec_frame_10ms, 0, _aec_frame_10ms.length);
			GD.AEC_WRITE_BLOCK = System.currentTimeMillis() - timestamp;
			
			System.arraycopy(_frame, 160, _aec_frame_10ms, 0, 160);
			AECModule.AEC_BufferFarend(AECModule._handler, _aec_frame_10ms, (short)80);
			
			timestamp = System.currentTimeMillis();
			_audio_track.write(_aec_frame_10ms, 0, _aec_frame_10ms.length);
			GD.AEC_WRITE_BLOCK = System.currentTimeMillis() - timestamp;
			
		}
		else if(GD.AECType.AECM == GD.AEC_TYPE)
		{
			AECMModule.AECM_BufferFarend(AECMModule._handler, _frame, (short)160);
			
			long timestamp = System.currentTimeMillis();
			_audio_track.write(_frame, 0, _frame.length);
			GD.AEC_WRITE_BLOCK = System.currentTimeMillis() - timestamp;
		}
		else
		{
			_audio_track.write(_frame, 0, _frame.length);
		}
		
		MediaInstance.instance().api_stop_play_tone();
	}
	
	private void udp_run()
	{
		try
		{
			Log.i("Audio", "Audio Recv & Play UDP");
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);//设置线程优先级
			 
			RtpSocket socket = MediaSocketManager.get_instance().get_audio_recv_socket();
			 
			boolean stop_statistics = true;//是否停止统计
			 
			_media_statistics.reset();
			
			long next_receive_timestamp = System.currentTimeMillis();
			
			while (_playing)
			{
				//if(true) { Thread.sleep(30); continue; }
				
				_recv_thread_statistics.enter();
				
				//数据统计处理
				if(true == MediaThreadManager.get_instance()._audio_play_idle)
				{
					if(false == stop_statistics)
					 {
						 _media_statistics.pause();//暂停统计
						 socket.empty(GD.UDP_AUDIO_RECV_SOCKET_TIME_OUT);
					 }
					 stop_statistics = true;
					 
					 _next_fetch_audio_sequence = 0;
					 
					 sleep(60);//音频接受socket的超时较短，空闲时需在此sleep
				}
				else
				{
					if(true == stop_statistics)
					{
						_media_statistics.resume();//重启统计
					}
					stop_statistics = false;
				}
				 
				if(null == socket)//获取socket失败
				{
					Thread.sleep(10);
					
					socket = MediaSocketManager.get_instance().get_audio_recv_socket();
					socket.empty(GD.UDP_AUDIO_RECV_SOCKET_TIME_OUT);
					 
					_recv_thread_statistics.leave();
					continue;
				}
				
				//如毋须接收及解码音频
				if(true == MediaThreadManager.get_instance()._audio_play_idle)
				{
					stop_audio_track();
					
					Thread.sleep(10);
					
					_recv_thread_statistics.leave();
					continue;
				}
				
				//需要接收解码播放音频				
				start_audio_track(_hands_free);
				
				////////////////////////////////////
				//平滑接收音频数据间隔
				if(next_receive_timestamp > System.currentTimeMillis())
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				next_receive_timestamp += 60;
				////////////////////////////////////
				
				//接收音频数据
				if(false == socket.receive())
				{
					_recv_thread_statistics.leave();
					//System.gc();
					continue;
				}
				
				//Log.v("Baidu", "RA 3 " + socket._rtp_packet.getLength());
				
				//空闲状态不处理收到的音频包
				if(true == MediaThreadManager.get_instance()._audio_play_idle)
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				GD.update_recv_audio_timestamp();
				
				/*if(true)
				{
					long timestamp = socket._rtp_packet.getTimestamp();
					long first_audio_packet_length = timestamp >> 16;
						
						if(first_audio_packet_length > length - sizeof(RTPHeader))
						{
							cout << "?";
							return;
						}

						unsigned long second_audio_packet_length = length - sizeof(RTPHeader) - first_audio_packet_length;
				}*/
				
				//不解码重复接收的音频包
				/*if(true)
				{
					if(_next_fetch_audio_sequence > socket._rtp_packet.getSequenceNumber())
					{
						Log.v("Baidu", "AD " + socket._rtp_packet.getSequenceNumber());
						_recv_thread_statistics.leave();
						continue;
					}
					
					_next_fetch_audio_sequence = socket._rtp_packet.getSequenceNumber() + 1;
				}*/
				
				//数据包统计
				int lost_packet_count = recv_audio_statistics(socket._rtp_packet.getPacket(), socket._rtp_packet.getLength(), 0);
				if(0 > lost_packet_count)
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//if(0 < lost_packet_count Log.v("Baidu", "lost " + lost_packet_count);
				
				if(true)//iLBC补偿
				{
					if(0 < lost_packet_count && true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc-webrtc"))
					{
						//Log.v("Baidu", "P");
						lost_packet_count %= 3;//最多补偿3个包（9帧）
						lost_packet_count *= 3;//9帧
						
						if(true == iLBCCodec.instance().predictive_decode(_predictive_audio_frame, (short)lost_packet_count))
						{
							for(int i = 0; i < lost_packet_count; ++i)
							{
								System.arraycopy(_predictive_audio_frame, 320 * i, _frame, 0, 320);
								play_with_aec();
							}
						}
					}
				}
				
				//解码
				/*if (_is_pcm == true)
				{
					//复制语音包
					//sa.enter();
					_audio_track.write(socket._rtp_packet.getPacket(), socket._rtp_packet.getHeaderLength(), socket._rtp_packet.getPayloadLength());
					//sa.leave();
				}
				else*/
				{
					//复制语音包
					_payload_length = socket._rtp_packet.getPayloadLength();				
					if(0 == _payload_length)
					{
						_recv_thread_statistics.leave();
						continue;
					}
					
					System.arraycopy(socket._rtp_packet.getPacket(), socket._rtp_packet.getHeaderLength(), _payload, 0, _payload_length);
					//audio_track.flush();
					
					//AES
					//Log.v("Temp", "AES 1");
					if(true == GD.USE_AES)
					{
						//OSD.instance().decrypt(_aes_index, _payload, 0, 16);
						//Log.v("Temp", "AES 2");
					}
					
					for(_frame_index = 0; _frame_index < 3; _frame_index++) 
					{
						//解码一帧语音
						//sa.enter();
						//decoded_len = doDecoder(_audio_decoder, payload, frame, DECODE_FACTOR); // 解码一帧（以帧为单位进行解码）
						//decoded_len = Codec.instance().decode(payload, 0, 38, frame, 0);//iLBC
						//decoded_len = 38;//iLBC
						//Log.v("Baidu", "decoded_len " + decoded_len);
						//sa.leave();
						
						_decoded_len = 0;
						
						if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc-webrtc"))
						{
							_decoded_len = iLBCCodec.instance().decode(_payload, 0, _frame);
						}
						else if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc"))
						{
							_decoded_len = Codec.instance().decode(_payload, 0, _frame);
						}
						
						GD.update_play_audio_timestamp();
						
						//caculate_frame_energy(frame);
						
						//播放一帧语音
						//sa.enter();
						
						long r = System.currentTimeMillis();
						
						//AEC
						play_with_aec();
						
						r = System.currentTimeMillis() - r;
						if(100 < r) Log.i("Audio", "DA~~~~~~ " + r);
						
						//sa.leave();
						
						//将下一帧的数据复制到fetched_payload头部
						//Log.v("Baidu", "D " + _decoded_len);
						_payload_length -= _decoded_len;
						
						if(0 < _payload_length)
						{
							System.arraycopy(_payload, _decoded_len, _payload, 0, _payload_length);
						}
					}
				}
				
				_recv_thread_statistics.leave();
			}
			
			stop_audio_track();
			
			if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc-webrtc"))
			{
				iLBCCodec.instance().free_decoder();
			}
			else if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc"))
			{
				Codec.instance().free_decoder();
			}
			
			_payload = null;		
			_frame = null;
		}
		catch(Exception e)
		{
			
		}
	}
	
	//音频接收线程
	public void run()
	{
		udp_run();
	}

	/*
	 * 功能：释放资源，退出音频接收、解码与播放
	 */
	public void free() 
	{
		_playing = false;
	}
}
