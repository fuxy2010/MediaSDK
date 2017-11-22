package com.nercms.schedule.audio;

import java.util.LinkedList;

import com.googlecode.androidilbc.Codec;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaSocketManager;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.misc.ThreadStatistics;
import com.nercms.schedule.network.MQTT;
import com.nercms.schedule.rtsp.RTSPClient;
import com.nercms.schedule.sip.engine.net.RtpPacket;
import com.nercms.schedule.sip.engine.net.RtpSocket;
import com.webrtc.ilbc.iLBCCodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecEncSend extends Thread
{
	private AudioRecord _audio_record = null; //音频采集控制类
	
	protected byte[] _raw_audio_buffer = new byte[320];//采集原始语音缓冲区，大小为：_audio_sample_buffer_size
	protected byte[] _raw_audio_buffer_10ms = new byte[160];
	protected byte[] _raw_audio_buffer_aec = new byte[320];
	protected byte[] _raw_audio_buffer_aec_10ms = new byte[160];
	
	//protected byte[][] _frame_for_vad = new byte[3][320];//帧缓冲，因VAD设立
	private LinkedList<byte[]> _frame_buffer_for_vad = new LinkedList<byte[]>();//帧缓冲，因VAD设立
	private int _continuous_slient_frame_count = 0;//当前连续静音帧数
	private final int RESERVED_SLIENT_FRAME = 2;//前后各留两个静音帧
	
	protected boolean _running = false;
	
	private int _aes_index = 0;
	
	public AudioRecEncSend()
	{
		super();
		
		_running = true;
	}
	
	private void start_audio_record()
	{
		if(null != _audio_record)
		{
			//Log.v("RTSP", "Audio " + _audio_record.getState() + ", " + _audio_record.getRecordingState());
		}
		
		if(null != _audio_record &&
			AudioRecord.STATE_INITIALIZED == _audio_record.getState() &&//已初始化
			AudioRecord.RECORDSTATE_RECORDING == _audio_record.getRecordingState())//正在采集
		{
			return;
		}
		
		stop_audio_record();
		
		int buffer_size = AudioRecord.getMinBufferSize(8000,
													AudioFormat.CHANNEL_CONFIGURATION_MONO,
													AudioFormat.ENCODING_PCM_16BIT);
		
		Log.i("Media", "Sample buf " + buffer_size);
		
		//统一手机最小缓存尺寸
		//确保底层硬件采集缓存的大小为一帧原始语音（320字节）的整数倍
		buffer_size = ((buffer_size / 320) + (0 == (buffer_size % 320) ? 0 : 1)) * 320;
		Log.v("Baidu", "AudioRecord Buffer " + buffer_size);
		
		//初始化音频采集控制类
		try
		{
			_audio_record = new AudioRecord(MediaRecorder.AudioSource.MIC,
											8000,
											AudioFormat.CHANNEL_CONFIGURATION_MONO,
											AudioFormat.ENCODING_PCM_16BIT,
											buffer_size);
		}
		catch(Exception e)
		{
			Log.i("Audio", "create AudioRecord error: " + e.toString());
		}
		
		if(null != _audio_record && AudioRecord.STATE_INITIALIZED == _audio_record.getState())
		{
			try
			{
				_audio_record.startRecording();
				System.gc();
				Log.i("Audio", "====== start audio recording!");
			}
			catch(Exception e)
			{
				Log.i("Audio", "start audio recording error: " + e.toString());
			}
		}
		else
		{
			//无法创建或初始化AudioRecord,提示用户重启机器
			//......
			
			if(null == _audio_record)
			{
				Log.i("Audio", "fail in creating AudioRecord");
			}
			else
			{
				Log.i("Audio", "AudioRecord cannot be initialized");
			}
		}
	}
	
	private void stop_audio_record()
	{
		if(null != _audio_record)
		{
			if(true)//AudioRecord.RECORDSTATE_RECORDING == _audio_record.getRecordingState())
			{
				_audio_record.stop();
			}
			
			_audio_record.release();
			
			_audio_record = null;
			
			Log.i("Audio", "====== stop audio recording!");
		}
	}
	
	private ThreadStatistics _thread_statistics = new ThreadStatistics("Audio Encode Thread");
	
	public void thread_statistics_record()
	{
		if(null != _thread_statistics)
		{
			_thread_statistics.record();
		}
	}
	
	private long _last_available_audio_frame_timestamp = 0;
	private boolean audio_process()
	{
		//if(true) return true;
		
		//long t = System.currentTimeMillis();
		
		//NS，一次只能处理10ms语音
		if(true)
		{
			//10ms
			System.arraycopy(_raw_audio_buffer, 0, _raw_audio_buffer_10ms, 0, 160);
			NSModule.NS_Process(NSModule._handler, _raw_audio_buffer_10ms, null, _raw_audio_buffer_aec_10ms, null);
			System.arraycopy(_raw_audio_buffer_aec_10ms, 0, _raw_audio_buffer, 0, 160);
			
			//10ms
			System.arraycopy(_raw_audio_buffer, 160, _raw_audio_buffer_10ms, 0, 160);
			NSModule.NS_Process(NSModule._handler, _raw_audio_buffer_10ms, null, _raw_audio_buffer_aec_10ms, null);
			System.arraycopy(_raw_audio_buffer_aec_10ms, 0, _raw_audio_buffer, 160, 160);
		}
		
		//AEC				
		if(GD.AECType.AEC == GD.AEC_TYPE)
		{
			//10ms
			System.arraycopy(_raw_audio_buffer, 0, _raw_audio_buffer_10ms, 0, 160);
			AECModule.AEC_Process(AECModule._handler, _raw_audio_buffer_10ms, null, _raw_audio_buffer_aec_10ms, null, (short)80, (short)(GD.AEC_DELAY + GD.AEC_WRITE_BLOCK + GD.AEC_READ_BLOCK / 2), 0);
			System.arraycopy(_raw_audio_buffer_aec_10ms, 0, _raw_audio_buffer, 0, 160);
			
			//10ms
			System.arraycopy(_raw_audio_buffer, 160, _raw_audio_buffer_10ms, 0, 160);
			AECModule.AEC_Process(AECModule._handler, _raw_audio_buffer_10ms, null, _raw_audio_buffer_aec_10ms, null, (short)80, (short)(GD.AEC_DELAY + GD.AEC_WRITE_BLOCK + GD.AEC_READ_BLOCK / 2), 0);
			System.arraycopy(_raw_audio_buffer_aec_10ms, 0, _raw_audio_buffer, 160, 160);
			
		}
		//AECM
		else if(GD.AECType.AECM == GD.AEC_TYPE)
		{
			AECMModule.AECM_Process(AECMModule._handler, _raw_audio_buffer, null, _raw_audio_buffer_aec, (short)160, (short)(GD.AEC_DELAY + GD.AEC_WRITE_BLOCK + GD.AEC_READ_BLOCK));
			System.arraycopy(_raw_audio_buffer_aec, 0, _raw_audio_buffer, 0, 320);
		}
		
		//VAD
		//静音检测使AEC流程失效
		/*if(true)//GD.AECType.NONE ==  GD.AEC_TYPE)
		{
			if(1 != VADModule.VAD_Process(VADModule._handler, 8000, _raw_audio_buffer, 160))
			{
				//Log.v("Baidu", "APM " + (System.currentTimeMillis() - t));
				//Log.v("Baidu", "mute");
				
				//避免因静音检测长时间不发包，最多允许1500ms内不编码
				if(2000 >= (System.currentTimeMillis() - _last_available_audio_frame_timestamp))
					return false;//静音帧不发送
			}
		}
		
		//Log.v("Baidu", "APV " + (System.currentTimeMillis() - t));
		
		_last_available_audio_frame_timestamp = System.currentTimeMillis();
		//Log.v("Baidu", "volume");*/
		
		//if(true) return true;//不静音检测
		
		//插入语音帧
		if(1 == VADModule.VAD_Process(VADModule._handler, 8000, _raw_audio_buffer, 160))//非静音帧
		{
			byte[] frame = new byte[320];
			System.arraycopy(_raw_audio_buffer, 0, frame, 0, 320);
			_frame_buffer_for_vad.add(frame);
			
			_continuous_slient_frame_count = 0;
			
			//Log.v("Baidu", "A");
		}
		else//静音帧
		{
			if((RESERVED_SLIENT_FRAME << 1) <= _continuous_slient_frame_count
				&& 2000 >= (System.currentTimeMillis() - _last_available_audio_frame_timestamp))//最多允许2000ms内不发送语音帧
			{
				//如有超过连续RESERVED_SLIENT_FRAME * 2个静音帧，则先删掉第RESERVED_SLIENT_FRAME + 1个静音帧再插入
				byte[] temp = _frame_buffer_for_vad.remove(_frame_buffer_for_vad.size() - RESERVED_SLIENT_FRAME);
				temp = null;
				
				//Log.v("Baidu", "SD");
			}
			
			byte[] frame = new byte[320];
			System.arraycopy(_raw_audio_buffer, 0, frame, 0, 320);
			_frame_buffer_for_vad.add(frame);
			
			//Log.v("Baidu", "SA");
			
			_continuous_slient_frame_count++;
		}
		
		//取出语音帧
		if(RESERVED_SLIENT_FRAME >= _frame_buffer_for_vad.size())
			return false;
		
		byte[] frame = _frame_buffer_for_vad.removeFirst();
		System.arraycopy(frame, 0, _raw_audio_buffer, 0, 320);
		
		_last_available_audio_frame_timestamp = System.currentTimeMillis();
		
		//Log.v("Baidu", "S");
		
		return true;
	}
	
	private void tcp_udp_run()
	{
		Log.i("RTSP", "Audio Record TCP & UDP");
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);//设置线程优先级
		
		try
		{
			int frame_count = 0;//上次发包后累计的语音帧（AMR或PCM）数，每3帧写入一个包发送一次
			
			byte[] encoded_frame_buffer = new byte[128];//一个编码后语音帧的缓存
			int encoded_frame_size = 0;//一个AMR语音帧的长度
			
			byte[] packet_buffer = new byte[256];///一个语音包（多帧）的缓存
			int packet_buffer_index = 0;//当前语音包的写入位置索引（即已写入amr_packet_buffer中的所有AMR语音帧长度）
			
			byte[] redundant_audio_packet = new byte[256];
			int first_audio_packet_size = 0;
			
			byte[] pcm_packet_buffer = new byte[1024];//须包含RTP包头和RTSP包头 [960];////一个PCM语音包（三帧）的缓存
			
			int sequence = 0;
			
			final int rtp_header_length = 12;
			final int rtsp_header_length = 4;
			final long imsi = GD.get_unique_id(GD.get_global_context());
			
			//冗余发送
			byte[] packet_1 = new byte[128];
			int packet_length_1 = 0;
			
			byte[] packet_2 = new byte[128];
			int packet_length_2 = 0;
			
			int first_audio_packet_index = 1;//下次发送冗余音频包时，上述哪个包打头
			
			//添加RTP over TCP包头固定部分							
			//payloadtype-0(AMR)
			pcm_packet_buffer[1 + rtsp_header_length] = (byte)((pcm_packet_buffer[1 + rtsp_header_length] & 0x80) | (0 & 0x7F));
			packet_buffer[1 + rtsp_header_length] = (byte)((packet_buffer[1 + rtsp_header_length] & 0x80) | (0 & 0x7F));
			
			//ssrc
			RtpPacket.setLong(imsi, pcm_packet_buffer, 8 + rtsp_header_length, 12 + rtsp_header_length);
			RtpPacket.setLong(imsi, packet_buffer, 8 + rtsp_header_length, 12 + rtsp_header_length);
			
			//mark
			pcm_packet_buffer[1 + rtsp_header_length] = RtpPacket.setBit(true, pcm_packet_buffer[1 + rtsp_header_length], 7);
			packet_buffer[1 + rtsp_header_length] = RtpPacket.setBit(true, packet_buffer[1 + rtsp_header_length], 7);
			
			RtpSocket udp_socket = null;
			if(true == GD.sip_audio_over_udp())
			{
				udp_socket = MediaSocketManager.get_instance().get_audio_send_socket();
			}
			
			//音频包amr_packet_buffer格式为
			//0~3字节：RTSP包头
			//4~15字节：RTP包头
			//16字节以后：净荷
			//当采用TCP(RTSP)发送时，使用上述所有字段
			//当采用UDP发送时，只将上述净荷字段复制到udp_socket._rtp_packet净荷，且采用udp_socket._rtp_packet自带RTP包头			
			while(true == _running) 
			{
				_thread_statistics.enter();
				
				if(true == GD.sip_audio_over_udp() && null == udp_socket)
				{
					Thread.sleep(10);
					
					udp_socket = MediaSocketManager.get_instance().get_audio_send_socket();
					
					_thread_statistics.leave();
					continue;
				}
				
				//如毋须发送音频则空转
				if(true == MediaThreadManager.get_instance()._audio_recorder_idle)
				{
					stop_audio_record();
					
					Thread.sleep(10);
					
					_thread_statistics.leave();
					continue;
				}
				
				start_audio_record();
				
				long timestamp = System.currentTimeMillis();
				int raw_frame_size = _audio_record.read(_raw_audio_buffer, 0, 320);//音频采集
				GD.AEC_READ_BLOCK = System.currentTimeMillis() - timestamp;
				
				if(320 != raw_frame_size) //底层采集缓存是320字节的整数倍，不可能采集到低于320字节的情况
				{
					Log.i("Audio", "audio Capture exception.");
					_thread_statistics.leave();
					
					Thread.sleep(5);					
					continue;
				}
				
				//AEC				
				//静音帧不编码发送
				if(false == audio_process())
				{
					continue;
				}
				
				try 
				{
					{//编码后发送
						//long timestamp = System.currentTimeMillis();
						//amr_frame_size = doEncoder(_audio_encoder, _raw_audio_buffer, amr_frame_buffer); //编码一帧，以帧为单位进行编码
						//encoded_frame_size = Codec.instance().encode(_raw_audio_buffer, 0, 320, encoded_frame_buffer, 0);//iLBC
						//Log.i("Audio", "AE " + (System.currentTimeMillis() - timestamp));
						
						//Log.v("Audio", "AE " + amr_frame_size);
						
						//if(0 == amr_frame_size || 13 < amr_frame_size)
						//if(38 != amr_frame_size)//iLBC
						
						encoded_frame_size = 0;
						
						if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc-webrtc"))
						{
							encoded_frame_size = iLBCCodec.instance().encode(_raw_audio_buffer, 0, encoded_frame_buffer);
						}
						else if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc"))
						{
							encoded_frame_size = Codec.instance().encode(_raw_audio_buffer, 0, encoded_frame_buffer);
						}
						
						//Log.v("Baidu", "encoded frame " + encoded_frame_size);
						
						if(0 == encoded_frame_size)
						{
							Log.v("Baidu", "audio enc fail");
							
							Thread.sleep(5);
							_thread_statistics.leave();
							continue;
						}
						
						//拼包待发送
						System.arraycopy(encoded_frame_buffer, 0,
										packet_buffer, packet_buffer_index + rtp_header_length + rtsp_header_length,
										encoded_frame_size);
						
						packet_buffer_index += encoded_frame_size;
						++frame_count;
						
						//AES
						if(true == GD.USE_AES)
						{
							if(3 == frame_count)
							{
								//OSD.instance().encrypt(_aes_index, packet_buffer, rtp_header_length + rtsp_header_length, 16);
							}
						}
						
						//包净荷长度为arm_packet_buffer_index
						//包中含帧数为audio_frame_count
						
						if(3 == frame_count)//满3帧发送一次
						{
							if(true == GD.REDUNDANT_AUDIO_SEND)//冗余发送
							{
								//将刚刚凑齐的3帧音频包复制到amr_packet_1或amr_packet_2
								if(0 == packet_length_1)
								{
									System.arraycopy(packet_buffer, rtp_header_length + rtsp_header_length, packet_1, 0, packet_buffer_index);
									
									packet_length_1 = packet_buffer_index;
									
									//Log.i("Baidu", "W1 " + amr_packet_length_1);
								}
								else if(0 == packet_length_2)
								{
									System.arraycopy(packet_buffer, rtp_header_length + rtsp_header_length,
													packet_2, 0, packet_buffer_index);
									
									packet_length_2 = packet_buffer_index;
									//Log.i("Baidu", "W2 " + amr_packet_length_2);
								}
								
								//拼接冗余包
								if(0 != packet_length_1 && 0!= packet_length_2)
								{
									if(1 == first_audio_packet_index)
									{
										System.arraycopy(packet_1, 0,
														packet_buffer, rtp_header_length + rtsp_header_length,
														packet_length_1);
										
										System.arraycopy(packet_2, 0,
														packet_buffer, rtp_header_length + rtsp_header_length + packet_length_1,
														packet_length_2);
										
										//Log.i("Baidu", "S12 " + amr_packet_length_1 + ", " + amr_packet_length_2);
									}
									else if(2 == first_audio_packet_index)
									{
										System.arraycopy(packet_2, 0,
														packet_buffer, rtp_header_length + rtsp_header_length,
														packet_length_2);
								
										System.arraycopy(packet_1, 0,
														packet_buffer, rtp_header_length + rtsp_header_length + packet_length_2,
														packet_length_1);
										
										//Log.i("Baidu", "S21 " + amr_packet_length_2 + ", " + amr_packet_length_1);
									}
									
									packet_buffer_index = packet_length_1 + packet_length_2;
								}
								else
								{
									//此分支仅在开始只生成一个音频包时出现
									//Log.i("Baidu", "no redundant audio packet");
									
									packet_buffer_index = 0;
									frame_count = 0;
									
									_thread_statistics.leave();
									continue;
								}
							}
							
							//时戳字段前2字节用于存放第一个音频包的长度
							
							//TCP,此时不冗余发送
							if(false && false == GD.REDUNDANT_AUDIO_SEND)//if(true && false == GD.REDUNDANT_AUDIO_SEND)
							{
								//Log.v("Baidu", "audio send tcp");
								//添加RTP包头动态部分
								//sequence
								RtpPacket.setInt(sequence, packet_buffer, 2 + rtsp_header_length, 4 + rtsp_header_length);
								
								//timestamp
								//将前2字节置0，用于冗余发送时存放
								RtpPacket.setLong((0xFFFF & System.currentTimeMillis()), packet_buffer, 4 + rtsp_header_length, 8 + rtsp_header_length);
								
								//添加RTSP包头
								int length = packet_buffer_index + rtp_header_length;							
								packet_buffer[0] = '$';
								packet_buffer[1] = 2;
								packet_buffer[2] = (byte)((length >> 8) & 0xff);
								packet_buffer[3] = (byte)(length & 0xff);
								
								//发送
								RTSPClient.get_instance().send_data(packet_buffer,  length + 4);
							}
							
							//UDP
							if(true)
							{
								//Log.v("Baidu", "audio send udp");
								System.arraycopy(packet_buffer, rtsp_header_length + rtp_header_length, udp_socket._rtp_packet.getPacket(), udp_socket._rtp_packet.getHeaderLength(), packet_buffer_index);
								
								udp_socket._rtp_packet.setSequenceNumber(sequence);
								udp_socket._rtp_packet.setPayloadLength(packet_buffer_index);
								udp_socket._rtp_packet.setTimestamp(0xFFFF & System.currentTimeMillis());//将前2字节置0，用于冗余发送时存放
								
								//冗余发送,timestamp字段的前2字节写入第一个音频包长度
								if(true == GD.REDUNDANT_AUDIO_SEND)
								{
									udp_socket._rtp_packet.setTimestamp(udp_socket._rtp_packet.getTimestamp() |
																	((1 == first_audio_packet_index) ? packet_length_1 : packet_length_2) << 16);
								}
								
								//if(0 != sequence % 10)//主动丢包
								{
									udp_socket.send(GD.SERVER_AUDIO_RECV_PORT);//UDP方式发送
								}
							}
							
							sequence += 1;
							
							packet_buffer_index = 0;
							frame_count = 0;
							
							//选择下一次发送时的首个音频包及更新冗余包
							if(true == GD.REDUNDANT_AUDIO_SEND)//冗余发送
							{
								//first_audio_packet_index = (1 == first_audio_packet_index) ? 2 : 1;
								if(1 == first_audio_packet_index)
								{
									first_audio_packet_index = 2;
									packet_length_1 = 0;
								}
								else if(2 == first_audio_packet_index)
								{
									first_audio_packet_index = 1;
									packet_length_2 = 0;
								}
							}
						}
					}
				} 
				catch (Exception ex) 
				{
					Log.w("AudioPlayer", ex.toString());
				}
				
				_thread_statistics.leave();
			}
			
			_thread_statistics = null;
			
			encoded_frame_buffer = null;
			packet_buffer = null;
			pcm_packet_buffer = null;
			
			stop_audio_record();
			
			_raw_audio_buffer = null;
			
			if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc-webrtc"))
			{
				iLBCCodec.instance().free_encoder();
			}
			else if(true == GD.AUDIO_CODEC.equalsIgnoreCase("ilbc"))
			{
				Codec.instance().free_encoder();
			}

			Log.i("Audio", "audio encode and send finish.");
		} 
		catch (Exception e) 
		{
			Log.i("Temp", "audio send exception. " + e.toString());
			e.printStackTrace();
					
			_raw_audio_buffer = null;
		}
	}

	//音频发送线程
	public void run()
	{
		tcp_udp_run();
	}
	
	/*
	 * 功能：释放资源，退出音频采集、编码与发送
	 */
	public void free()
	{
		_running = false;
	}
}