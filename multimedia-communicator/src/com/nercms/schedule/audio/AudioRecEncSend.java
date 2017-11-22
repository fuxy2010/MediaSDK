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
	private AudioRecord _audio_record = null; //��Ƶ�ɼ�������
	
	protected byte[] _raw_audio_buffer = new byte[320];//�ɼ�ԭʼ��������������СΪ��_audio_sample_buffer_size
	protected byte[] _raw_audio_buffer_10ms = new byte[160];
	protected byte[] _raw_audio_buffer_aec = new byte[320];
	protected byte[] _raw_audio_buffer_aec_10ms = new byte[160];
	
	//protected byte[][] _frame_for_vad = new byte[3][320];//֡���壬��VAD����
	private LinkedList<byte[]> _frame_buffer_for_vad = new LinkedList<byte[]>();//֡���壬��VAD����
	private int _continuous_slient_frame_count = 0;//��ǰ��������֡��
	private final int RESERVED_SLIENT_FRAME = 2;//ǰ�������������֡
	
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
			AudioRecord.STATE_INITIALIZED == _audio_record.getState() &&//�ѳ�ʼ��
			AudioRecord.RECORDSTATE_RECORDING == _audio_record.getRecordingState())//���ڲɼ�
		{
			return;
		}
		
		stop_audio_record();
		
		int buffer_size = AudioRecord.getMinBufferSize(8000,
													AudioFormat.CHANNEL_CONFIGURATION_MONO,
													AudioFormat.ENCODING_PCM_16BIT);
		
		Log.i("Media", "Sample buf " + buffer_size);
		
		//ͳһ�ֻ���С����ߴ�
		//ȷ���ײ�Ӳ���ɼ�����Ĵ�СΪһ֡ԭʼ������320�ֽڣ���������
		buffer_size = ((buffer_size / 320) + (0 == (buffer_size % 320) ? 0 : 1)) * 320;
		Log.v("Baidu", "AudioRecord Buffer " + buffer_size);
		
		//��ʼ����Ƶ�ɼ�������
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
			//�޷��������ʼ��AudioRecord,��ʾ�û���������
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
		
		//NS��һ��ֻ�ܴ���10ms����
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
		//�������ʹAEC����ʧЧ
		/*if(true)//GD.AECType.NONE ==  GD.AEC_TYPE)
		{
			if(1 != VADModule.VAD_Process(VADModule._handler, 8000, _raw_audio_buffer, 160))
			{
				//Log.v("Baidu", "APM " + (System.currentTimeMillis() - t));
				//Log.v("Baidu", "mute");
				
				//����������ⳤʱ�䲻�������������1500ms�ڲ�����
				if(2000 >= (System.currentTimeMillis() - _last_available_audio_frame_timestamp))
					return false;//����֡������
			}
		}
		
		//Log.v("Baidu", "APV " + (System.currentTimeMillis() - t));
		
		_last_available_audio_frame_timestamp = System.currentTimeMillis();
		//Log.v("Baidu", "volume");*/
		
		//if(true) return true;//���������
		
		//��������֡
		if(1 == VADModule.VAD_Process(VADModule._handler, 8000, _raw_audio_buffer, 160))//�Ǿ���֡
		{
			byte[] frame = new byte[320];
			System.arraycopy(_raw_audio_buffer, 0, frame, 0, 320);
			_frame_buffer_for_vad.add(frame);
			
			_continuous_slient_frame_count = 0;
			
			//Log.v("Baidu", "A");
		}
		else//����֡
		{
			if((RESERVED_SLIENT_FRAME << 1) <= _continuous_slient_frame_count
				&& 2000 >= (System.currentTimeMillis() - _last_available_audio_frame_timestamp))//�������2000ms�ڲ���������֡
			{
				//���г�������RESERVED_SLIENT_FRAME * 2������֡������ɾ����RESERVED_SLIENT_FRAME + 1������֡�ٲ���
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
		
		//ȡ������֡
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
		
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);//�����߳����ȼ�
		
		try
		{
			int frame_count = 0;//�ϴη������ۼƵ�����֡��AMR��PCM������ÿ3֡д��һ��������һ��
			
			byte[] encoded_frame_buffer = new byte[128];//һ�����������֡�Ļ���
			int encoded_frame_size = 0;//һ��AMR����֡�ĳ���
			
			byte[] packet_buffer = new byte[256];///һ������������֡���Ļ���
			int packet_buffer_index = 0;//��ǰ��������д��λ������������д��amr_packet_buffer�е�����AMR����֡���ȣ�
			
			byte[] redundant_audio_packet = new byte[256];
			int first_audio_packet_size = 0;
			
			byte[] pcm_packet_buffer = new byte[1024];//�����RTP��ͷ��RTSP��ͷ [960];////һ��PCM����������֡���Ļ���
			
			int sequence = 0;
			
			final int rtp_header_length = 12;
			final int rtsp_header_length = 4;
			final long imsi = GD.get_unique_id(GD.get_global_context());
			
			//���෢��
			byte[] packet_1 = new byte[128];
			int packet_length_1 = 0;
			
			byte[] packet_2 = new byte[128];
			int packet_length_2 = 0;
			
			int first_audio_packet_index = 1;//�´η���������Ƶ��ʱ�������ĸ�����ͷ
			
			//���RTP over TCP��ͷ�̶�����							
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
			
			//��Ƶ��amr_packet_buffer��ʽΪ
			//0~3�ֽڣ�RTSP��ͷ
			//4~15�ֽڣ�RTP��ͷ
			//16�ֽ��Ժ󣺾���
			//������TCP(RTSP)����ʱ��ʹ�����������ֶ�
			//������UDP����ʱ��ֻ�����������ֶθ��Ƶ�udp_socket._rtp_packet���ɣ��Ҳ���udp_socket._rtp_packet�Դ�RTP��ͷ			
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
				
				//�����뷢����Ƶ���ת
				if(true == MediaThreadManager.get_instance()._audio_recorder_idle)
				{
					stop_audio_record();
					
					Thread.sleep(10);
					
					_thread_statistics.leave();
					continue;
				}
				
				start_audio_record();
				
				long timestamp = System.currentTimeMillis();
				int raw_frame_size = _audio_record.read(_raw_audio_buffer, 0, 320);//��Ƶ�ɼ�
				GD.AEC_READ_BLOCK = System.currentTimeMillis() - timestamp;
				
				if(320 != raw_frame_size) //�ײ�ɼ�������320�ֽڵ��������������ܲɼ�������320�ֽڵ����
				{
					Log.i("Audio", "audio Capture exception.");
					_thread_statistics.leave();
					
					Thread.sleep(5);					
					continue;
				}
				
				//AEC				
				//����֡�����뷢��
				if(false == audio_process())
				{
					continue;
				}
				
				try 
				{
					{//�������
						//long timestamp = System.currentTimeMillis();
						//amr_frame_size = doEncoder(_audio_encoder, _raw_audio_buffer, amr_frame_buffer); //����һ֡����֡Ϊ��λ���б���
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
						
						//ƴ��������
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
						
						//�����ɳ���Ϊarm_packet_buffer_index
						//���к�֡��Ϊaudio_frame_count
						
						if(3 == frame_count)//��3֡����һ��
						{
							if(true == GD.REDUNDANT_AUDIO_SEND)//���෢��
							{
								//���ոմ����3֡��Ƶ�����Ƶ�amr_packet_1��amr_packet_2
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
								
								//ƴ�������
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
									//�˷�֧���ڿ�ʼֻ����һ����Ƶ��ʱ����
									//Log.i("Baidu", "no redundant audio packet");
									
									packet_buffer_index = 0;
									frame_count = 0;
									
									_thread_statistics.leave();
									continue;
								}
							}
							
							//ʱ���ֶ�ǰ2�ֽ����ڴ�ŵ�һ����Ƶ���ĳ���
							
							//TCP,��ʱ�����෢��
							if(false && false == GD.REDUNDANT_AUDIO_SEND)//if(true && false == GD.REDUNDANT_AUDIO_SEND)
							{
								//Log.v("Baidu", "audio send tcp");
								//���RTP��ͷ��̬����
								//sequence
								RtpPacket.setInt(sequence, packet_buffer, 2 + rtsp_header_length, 4 + rtsp_header_length);
								
								//timestamp
								//��ǰ2�ֽ���0���������෢��ʱ���
								RtpPacket.setLong((0xFFFF & System.currentTimeMillis()), packet_buffer, 4 + rtsp_header_length, 8 + rtsp_header_length);
								
								//���RTSP��ͷ
								int length = packet_buffer_index + rtp_header_length;							
								packet_buffer[0] = '$';
								packet_buffer[1] = 2;
								packet_buffer[2] = (byte)((length >> 8) & 0xff);
								packet_buffer[3] = (byte)(length & 0xff);
								
								//����
								RTSPClient.get_instance().send_data(packet_buffer,  length + 4);
							}
							
							//UDP
							if(true)
							{
								//Log.v("Baidu", "audio send udp");
								System.arraycopy(packet_buffer, rtsp_header_length + rtp_header_length, udp_socket._rtp_packet.getPacket(), udp_socket._rtp_packet.getHeaderLength(), packet_buffer_index);
								
								udp_socket._rtp_packet.setSequenceNumber(sequence);
								udp_socket._rtp_packet.setPayloadLength(packet_buffer_index);
								udp_socket._rtp_packet.setTimestamp(0xFFFF & System.currentTimeMillis());//��ǰ2�ֽ���0���������෢��ʱ���
								
								//���෢��,timestamp�ֶε�ǰ2�ֽ�д���һ����Ƶ������
								if(true == GD.REDUNDANT_AUDIO_SEND)
								{
									udp_socket._rtp_packet.setTimestamp(udp_socket._rtp_packet.getTimestamp() |
																	((1 == first_audio_packet_index) ? packet_length_1 : packet_length_2) << 16);
								}
								
								//if(0 != sequence % 10)//��������
								{
									udp_socket.send(GD.SERVER_AUDIO_RECV_PORT);//UDP��ʽ����
								}
							}
							
							sequence += 1;
							
							packet_buffer_index = 0;
							frame_count = 0;
							
							//ѡ����һ�η���ʱ���׸���Ƶ�������������
							if(true == GD.REDUNDANT_AUDIO_SEND)//���෢��
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

	//��Ƶ�����߳�
	public void run()
	{
		tcp_udp_run();
	}
	
	/*
	 * ���ܣ��ͷ���Դ���˳���Ƶ�ɼ��������뷢��
	 */
	public void free()
	{
		_running = false;
	}
}