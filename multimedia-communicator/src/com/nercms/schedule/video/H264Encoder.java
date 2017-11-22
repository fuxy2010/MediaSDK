package com.nercms.schedule.video;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import android.util.Log;

import com.nercms.schedule.misc.CPUStypeDistinct;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaSocketManager;
import com.nercms.schedule.misc.ThreadStatistics;
import com.nercms.schedule.rtsp.RTSPClient;
import com.nercms.schedule.sensor.SensorHelper;
import com.nercms.schedule.sip.engine.net.RtpPacket;
import com.nercms.schedule.sip.engine.net.RtpSocket;
import com.nercms.schedule.sip.engine.net.SipdroidSocket;
import com.nercms.schedule.sip.engine.sipua.SP;

public class H264Encoder extends Thread
{
	private long _h264_endcoder = -1;         //���������
	
	public boolean _is_running = false; //��Ƶ�������б�־
	
	private byte[] _frame_buffer = null; //�����һ֡��Ƶ������
	private LinkedList<byte[]> _frame_buffer_list = new LinkedList<byte[]>();
	private final int _MAX_FRAME_BUFFER_NUM = 50;//_frame_buffer_list��󳤶�
	
	private int [] _packet_sizes_in_frame = null;  //һ����Ƶ֡�����Ƶ���ĳ���
	private LinkedList<int[]> _packet_sizes_in_frame_list = new LinkedList<int[]>();
	
	private int _packet_count_in_frame = 0;//һ����Ƶ֡����Ƶ������
	private LinkedList<Integer> _packet_count_in_frame_list = new LinkedList<Integer>();
	
	private long SEND_INTERVAL = 0;//��Ƶ�����ͼ����СΪ20ms
	
	private long _latest_sample_timestamp = 0;//���һ�β����������ʱ��
	
	/**
	 * ��ʼ��������
	 * @param width�� ��Ƶ֡�Ŀ��
	 * @param height����Ƶ֡�ĸ߶�
	 * @param fps������֡��
	 * @param qp��������������10~51֮��ȡֵ��
	 * @param packSize����Ƶ���ݰ�������ֽ���Ŀ��Ĭ��Ϊ1000��
	 * @param minIDR��IDR֡����С�����Ĭ��Ϊ15��
	 * @param maxIDR��IDR֡���������Ĭ��Ϊ150��
	 * @param adap������ӦI֡������ֵ��Ϊ0ʱȡ������ӦIDR֡��Ĭ��ֵΪ40��
	 * @return������ֵΪ�������ľ��
	 */
	private native long CompressBegin(int width, int height, int fps, int qp, int packSize, int minIDR, int maxIDR, int adap);//�ײ�⺯����������Ƶ������
	
	/**
	 * ��ʼ��������
	 * @param width�� ��Ƶ֡�Ŀ��
	 * @param height����Ƶ֡�ĸ߶�
	 * @param fps������֡��
	 * @param qp��������������10~51֮��ȡֵ��
	 * @param packSize����Ƶ���ݰ�������ֽ���Ŀ��Ĭ��Ϊ1000��
	 * @param minIDR��IDR֡����С�����Ĭ��Ϊ15��
	 * @param maxIDR��IDR֡���������Ĭ��Ϊ150��
	 * @param adap������ӦI֡������ֵ��Ϊ0ʱȡ������ӦIDR֡��Ĭ��ֵΪ40��
	 * @param bitrate�����ʣ���λ��Kbit/s�� Ϊ0ʱ�ر����ʿ��ƣ�
	 * @return������ֵΪ�������ľ��
	 */
	//private native long CompressBeginBitctrl(int width, int height, int fps, int bitrate, int packetsize, int IPeroid, int unit);
	
	/**
	 * ����һ֡
	 * @param encoder�����������
	 * @param type���ѵ�ǰͼ��ǿ�Ʊ���Ϊָ�����͵���Ƶ֡��Ĭ��ֵΪ-1��-1��Auto  0��P֡   1��IDR֡    2��I֡��
	 * @param in���������һ֡ͼ��
	 * @param insize��ͼ��ĳߴ�
	 * @param out����������������
	 * @param size��������Ϊ������ݰ��������������¼ÿ�����ݰ��ĳߴ�
	 * @return������ֵΪһ֡ͼ������������ݰ�����Ŀ
	 */
	private native int CompressBuffer(long encoder, int type, byte[] in, int insize, byte[] out ,int [] size); //�ײ�⺯��������һ֡ͼ��
	
	/**
	 * ���ٱ�����
	 * @param encoder�����������
	 * @return������ֵΪ1�����������Ѿ��ɹ�����
	 */
	private native int CompressEnd(long encoder); //�ײ�⺯�����˳���Ƶ����������������
	
	//private native long CompressBegin(int width, int height, int fps, int qp, int packetsize, int minIDR, int maxIDR, int apdI);
	//private native int CompressBuffer(long encoder, int type, byte[] in, int insize, byte[] out ,int [] size);
	//private native int CompressEnd(long encoder);
	
	public H264Encoder()
	{
		//����CPU�ܹ�������Ӧ�ı����
		/*CPUStypeDistinct cpu = new CPUStypeDistinct();
		switch(cpu.GetCPUStype())
		{
			case 1://ARM V9������汾
			default:
				System.loadLibrary("H264Encoder_c");
				break;
				
			case 2://ARM V11
				System.loadLibrary("H264Encoder_armv4");
				break;
			
			case 3://Cortex A8/A9/A15
				System.loadLibrary("H264Encoder_neon");
				break;
		}*/
		System.loadLibrary("H264Encoder_neon");
		
		int QP = 28;//36;//31;
		int IDR_interval = 3000 / GD.VIDEO_SAMPLE_INTERVAL;//ÿ��3��һ��I֡
		
		int FPS = 1000 / GD.VIDEO_SAMPLE_INTERVAL;//(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp")) ? 6 : 12;//����֡��
		
		int adaptive_IDR_interval = 0;
		
		//��ʼ����������ָ�����뻺���С
		//��������ֵ��Ϊ[23, 51], ��ֵԽСԽ��������Ϊ31
		
		//_h264_endcoder = CompressBeginBitctrl(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, FPS, 1000, GD.MAX_VIDEO_PACKET_SIZE, IDR_interval, 5);
		_h264_endcoder = CompressBegin(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, FPS, QP, GD.MAX_VIDEO_PACKET_SIZE, IDR_interval, IDR_interval, 0);
		
		_frame_buffer = new byte[65536];
		_packet_sizes_in_frame = new int[64];
		
		_is_running=true;
	}
	
	//��YUV420֡��180�ȷ�ת
	//private final int cif_frame_size = 352 * 288;
	//private final int qcif_frame_size = 176 * 144;
	//private byte[] rotated_frame = new byte[352 * 288 * 3 / 2];
	private byte[] rotated_frame = new byte[GD.VIDEO_WIDTH * GD.VIDEO_HEIGHT * 3 / 2];
	private boolean rotate_video(byte[] frame)
	{
		int HW = GD.VIDEO_WIDTH / 2;
		int HH = GD.VIDEO_HEIGHT / 2;
		int cif_frame_size = GD.VIDEO_WIDTH * GD.VIDEO_HEIGHT;
		int qcif_frame_size = HW * HH;
		
		if(null == frame || cif_frame_size * 3 / 2 != frame.length)
			return false;
		
		int base = 0;
		
		//Y
		for(int i = 0; i < GD.VIDEO_HEIGHT; ++i)
		{
			for(int j = 0; j < GD.VIDEO_WIDTH; ++j)
			{
				rotated_frame[base + GD.VIDEO_WIDTH - 1 - j] = frame[base + j];
			}
			
			base += GD.VIDEO_WIDTH;
		}
		
		base = 0;
		for(int i = 0; i < HH; ++i)
		{
			for(int j = 0; j < HW; ++j)
			{
				//V
				//rotated_frame[cif_frame_size + 2 * (base + 175 - j)] = frame[cif_frame_size +  2 * (base + j)];
				rotated_frame[cif_frame_size + (base + HW - 1 - j) + (base + HW - 1 - j)] = frame[cif_frame_size +  (base + j) + (base + j)];
				
				//U
				//rotated_frame[cif_frame_size + 2 * (base + 175 - j) + 1] = frame[cif_frame_size + 2 * (base + j) + 1];
				rotated_frame[cif_frame_size + (base + HW - 1 - j) + (base + HW - 1 - j) + 1] = frame[cif_frame_size + (base + j) + (base + j) + 1];
			}
			
			base += HW;
		}
		
		//Log.i("Video", "R " + (System.currentTimeMillis() - t));
		
		return true;
	}
	
	private long _enc_times = 0;
	private long _enc_durance = 0;
	
	//����һ֡ԭʼ��Ƶ
	public void encode(byte[] raw_video_frame, boolean rotate)
	{
		GD.VIDEO_ENCODE_STRATEGY = -1;//������ʾ
		
		//�����������Ƶ��������������VIDEO_SAMPLE_INTERVAL����
		//ֻ��IDR֡ʱ������IDR֡���ټ��ONLY_IDR_FRAME_INTERVAL����
		//if(((GD.VIDEO_ENCODE_STRATEGY == 1) ? GD.ONLY_IDR_FRAME_INTERVAL : GD.VIDEO_SAMPLE_INTERVAL) > (System.currentTimeMillis() - _latest_sample_timestamp))
		if(GD.VIDEO_SAMPLE_INTERVAL > (System.currentTimeMillis() - _latest_sample_timestamp))
			 return;
		
		//Log.i("Temp", "frame: " + raw_video_frame.length);
		
		_latest_sample_timestamp = System.currentTimeMillis();
		
		if(true == SensorHelper._is_shocking)
		{
			//Log.v("Video", "shocking");
			//return;
		}
		
		synchronized(H264Encoder.class) 
		{
			//������ʱ������
			if(_MAX_FRAME_BUFFER_NUM <= _frame_buffer_list.size())
			{
				Log.v("RTSP", "send video buffer overflow");
				return;
			}
		}
		
		if(true == rotate)
		{
			rotate_video(raw_video_frame);
			
			long t = System.currentTimeMillis();
			//����һ֡ͼ�񲢷��ظ�֡����Ƶ������
			_packet_count_in_frame = CompressBuffer(_h264_endcoder, GD.VIDEO_ENCODE_STRATEGY,
													rotated_frame, rotated_frame.length,
													_frame_buffer, _packet_sizes_in_frame);
			Log.v("Video", "enc 1: " + (System.currentTimeMillis() - t));
		}
		else
		{
			long t = System.currentTimeMillis();
			//����һ֡ͼ�񲢷��ظ�֡����Ƶ������
			_packet_count_in_frame = CompressBuffer(_h264_endcoder, GD.VIDEO_ENCODE_STRATEGY,
													raw_video_frame, raw_video_frame.length,
													_frame_buffer, _packet_sizes_in_frame);
			Log.v("Video", "enc 2: " + (System.currentTimeMillis() - t));
		}
		
		//Log.v("Video", "video enc: " + _packet_count_in_frame);
		
		if(0 == _packet_count_in_frame)
			return;
		
		//�������õ�������д�����
		synchronized(H264Encoder.class)
		{
			_frame_buffer_list.add(_frame_buffer);
			_packet_count_in_frame_list.add(_packet_count_in_frame);
			_packet_sizes_in_frame_list.add(_packet_sizes_in_frame);
			
			H264Encoder.class.notify();
		}
	}
	
	private ThreadStatistics _thread_statistics = new ThreadStatistics("Video Send Thread");
	
	public void thread_statistics_record()
	{
		if(null != _thread_statistics)
		{
			_thread_statistics.record();
		}
	}
	
	private void udp_run()
	{
		Log.v("RTSP", "Video Encode UDP");
		
		try
		{
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);//�����߳����ȼ�
			
			byte[] frame_buffer = null;//�����֡������
			int[] packet_sizes_in_frame = null;//һ֡�и�������
			int packet_count_in_frame = 0;//һ֡�а�����
			long packet_pos_in_frame = 0;//���ֽ�Ϊ���ݰ���֡�е���ţ���0���������ֽ�Ϊ֡�ڰ�������������1
			
			int send_bytes_in_frame = 0;//һ֡��Ƶ���ѷ��͵����ݳ���
			int sequence = 0;
			long timestamp = 0;
			
			RtpSocket socket = MediaSocketManager.get_instance().get_video_send_socket();
			
			while(_is_running)
			{
				_thread_statistics.enter();
				
				if(null == socket)
				{
					Thread.sleep(10);
					
					socket = MediaSocketManager.get_instance().get_video_send_socket();
					
					_thread_statistics.leave();
					continue;
				}
				
				//ȡ���ݰ�
				frame_buffer = null;
				packet_count_in_frame = 0;
				packet_sizes_in_frame= null;
				
				synchronized(H264Encoder.class)
				{
					if(0 < _frame_buffer_list.size())
					{
						try 
						{
							frame_buffer = _frame_buffer_list.removeFirst(); 
							packet_count_in_frame = _packet_count_in_frame_list.removeFirst(); 
							packet_sizes_in_frame = _packet_sizes_in_frame_list.removeFirst();
						}                                                            
						catch(NoSuchElementException e){} 
					}
					
					//ȡ���ݰ�ʧ����
					if(null == frame_buffer || 0 == packet_count_in_frame || null == packet_sizes_in_frame)
					{
						H264Encoder.class.wait(60);
						
						_thread_statistics.leave();
						continue; 
					}
				}
				
				//���η���һ֡��Ƶ�еĸ����ݰ�
				timestamp = System.currentTimeMillis();//һ֡�ڵĸ����ݰ�Ӧ����ͬ����ʱ��
				
				send_bytes_in_frame = 0;
				
				for(int i = 0; i < packet_count_in_frame; ++i)
				{
					if(null == frame_buffer || null == socket._rtp_packet || null == socket._rtp_packet.getPacket())
					{
						_thread_statistics.leave();
						continue;
					}
					
					System.arraycopy(frame_buffer, send_bytes_in_frame, socket._rtp_packet.getPacket(), socket._rtp_packet.getHeaderLength(), packet_sizes_in_frame[i]);					
					
					////////////////////////////////////
					//�޸�timestamp
					//���ֽ�Ϊ֡�ڰ���ţ���0��ʼ
					//���ֽں���λΪ֡�ڰ�����
					//�����ֽ�Ϊԭtimestamp����
					packet_pos_in_frame = 0;
					packet_pos_in_frame |= (i << 24);//���ֽ�Ϊ֡�ڰ����
					packet_pos_in_frame |= (packet_count_in_frame << 16);//�νں���λΪ֡�ڰ�����
					//packet_pos_in_frame |= 0xFFFF;//�����ֽ���1					
					
					//��packet_pos_in_frame����timestamp��
					timestamp &= 0xFFFF;
					timestamp |= packet_pos_in_frame;
					////////////////////////////////////					
					
					socket._rtp_packet.setSequenceNumber(sequence++);
					socket._rtp_packet.setPayloadLength(packet_sizes_in_frame[i]);
					socket._rtp_packet.setTimestamp(timestamp);
					socket._rtp_packet.setMarker((i == packet_count_in_frame - 1) ? true : false);
					
					send_bytes_in_frame += packet_sizes_in_frame[i];
					
					//Log.v("Baidu", "send video " + send_bytes_in_frame);
					socket.send(GD.SERVER_VIDEO_RECV_PORT);
					
					if(0 != SEND_INTERVAL)
					{
						sleep(SEND_INTERVAL);//ƽ������Ƶ�ʣ�����ӵ��
					}
				}
				
				_thread_statistics.leave();
			}
			if(_h264_endcoder!=-1)
			{
				CompressEnd(_h264_endcoder);
			}
			
			frame_buffer = null;
			packet_count_in_frame = 0;
			packet_sizes_in_frame= null;
		}
		catch(Exception e)
		{
			
		}
	}
	
	private void tcp_run()
	{
		Log.v("RTSP", "Video Encode TCP");
		
		try
		{
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);//�����߳����ȼ�
			
			byte[] frame_buffer = null;//�����֡������
			int[] packet_sizes_in_frame = null;//һ֡�и�������
			int packet_count_in_frame = 0;//һ֡�а�����
			int packet_pos_in_frame = 0;//���ֽ�Ϊ���ݰ���֡�е���ţ���0���������ֽ�Ϊ֡�ڰ�������������1
			
			int send_bytes_in_frame = 0;//һ֡��Ƶ���ѷ��͵����ݳ���
			int sequence = 0;
			long timestamp = 0;
			
			byte[] video_packet_buffer = new byte[1024];//�����RTP��ͷ��RTSP��ͷ
			
			final int rtp_header_length = 12;
			final int rtsp_header_length = 4;
			final long imsi = GD.get_unique_id(GD.get_global_context());
			
			//���RTP��ͷ�̶�����							
			//payloadtype-2(H.264)
			video_packet_buffer[1 + rtsp_header_length] = (byte)((video_packet_buffer[1 + rtsp_header_length] & 0x80) | (2 & 0x7F));
			
			//ssrc
			RtpPacket.setLong(imsi, video_packet_buffer, 8 + rtsp_header_length, 12 + rtsp_header_length);
			
			while(_is_running)
			{
				_thread_statistics.enter();
				
				//ȡ���ݰ�
				frame_buffer = null;
				packet_count_in_frame = 0;
				packet_sizes_in_frame= null;
				
				synchronized(H264Encoder.class)
				{
					if(0 < _frame_buffer_list.size())
					{
						try 
						{
							frame_buffer = _frame_buffer_list.removeFirst(); 
							packet_count_in_frame = _packet_count_in_frame_list.removeFirst(); 
							packet_sizes_in_frame = _packet_sizes_in_frame_list.removeFirst();
						}                                                            
						catch(NoSuchElementException e){} 
					}
					
					//ȡ���ݰ�ʧ����
					if(null == frame_buffer || 0 == packet_count_in_frame || null == packet_sizes_in_frame)
					{
						H264Encoder.class.wait(60);
						
						_thread_statistics.leave();
						continue; 
					}
				}				
				
				//���η���һ֡��Ƶ�еĸ����ݰ�
				timestamp = System.currentTimeMillis();//һ֡�ڵĸ����ݰ�Ӧ����ͬ����ʱ��
				
				send_bytes_in_frame = 0;
				
				for(int i = 0; i < packet_count_in_frame; ++i)
				{
					if(null == frame_buffer)
					{
						_thread_statistics.leave();
						continue;
					}
					
					System.arraycopy(frame_buffer, send_bytes_in_frame,
									video_packet_buffer, rtp_header_length + rtsp_header_length,
									packet_sizes_in_frame[i]);
					
					/*packet_pos_in_frame = 0;
					packet_pos_in_frame |= (i << 28);//��һ�ֽ�ǰ��λΪ֡�ڰ����					
					packet_pos_in_frame |= (packet_count_in_frame << 24);//��һ�ֽں���λΪ֡�ڰ�����					
					packet_pos_in_frame |= 0xFFFFFF;//�����ֽ���1
					
					//��packet_pos_in_frameͷ�ֽ�����timestamp��
					timestamp &= 0xFFFFFF;
					timestamp |= packet_pos_in_frame;*/
					
					packet_pos_in_frame = 0;
					packet_pos_in_frame |= (i << 24);//���ֽ�Ϊ֡�ڰ����
					packet_pos_in_frame |= (packet_count_in_frame << 16);//�νں���λΪ֡�ڰ�����
					packet_pos_in_frame |= 0xFFFF;//�����ֽ���1					
					
					//��packet_pos_in_frame����timestamp��
					timestamp &= 0xFFFF;//����web������ʱ�ر� timestamp &= 0xFFFF;
					timestamp |= packet_pos_in_frame;//����web������ʱ�ر� timestamp |= packet_pos_in_frame;
					
					//���RTP��ͷ��̬����
					//sequence
					RtpPacket.setInt(sequence++, video_packet_buffer, 2 + rtsp_header_length, 4 + rtsp_header_length);
					
					//timestamp
					//һ֡�ڵĸ����ݰ�Ӧ����ͬ����ʱ��
					RtpPacket.setLong(timestamp, video_packet_buffer, 4 + rtsp_header_length, 8 + rtsp_header_length);
					
					//mark
					video_packet_buffer[1 + rtsp_header_length] = RtpPacket.setBit(((i == packet_count_in_frame - 1) ? true : false), video_packet_buffer[1 + rtsp_header_length], 7);
					
					//���RTSP��ͷ
					int length = packet_sizes_in_frame[i] + rtp_header_length;							
					video_packet_buffer[0] = '$';
					video_packet_buffer[1] = 0;
					video_packet_buffer[2] = (byte)((length >> 8) & 0xff);
					video_packet_buffer[3] = (byte)(length & 0xff);
					
					send_bytes_in_frame += packet_sizes_in_frame[i];
					
					//����
					RTSPClient.get_instance().send_data(video_packet_buffer,  length + 4);
					
					if(0 != SEND_INTERVAL)
					{
						sleep(SEND_INTERVAL);//ƽ������Ƶ�ʣ�����ӵ��
					}
				}
				
				_thread_statistics.leave();
			}
			if(_h264_endcoder!=-1)
			{
				CompressEnd(_h264_endcoder);
			}
			
			frame_buffer = null;
			packet_count_in_frame = 0;
			packet_sizes_in_frame= null;
		}
		catch(Exception e)
		{
			
		}
	}
	
	@Override
	public void run() 
	{
		if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("udp"))
		{
			udp_run();
		}
		else if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
		{
			tcp_run();
		}
	}
	
	/*
	 * free�������ܣ��ر�rtp���ͣ��ͷŽ�������Դ
	 */
	public void free() 
	{
		_is_running = false;
		
		Log.i("H264Encoder","H264Encoder finish");
	}
}
