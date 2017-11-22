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
	private long _h264_endcoder = -1;         //定义编码器
	
	public boolean _is_running = false; //视频编码运行标志
	
	private byte[] _frame_buffer = null; //编码后一帧视频缓冲区
	private LinkedList<byte[]> _frame_buffer_list = new LinkedList<byte[]>();
	private final int _MAX_FRAME_BUFFER_NUM = 50;//_frame_buffer_list最大长度
	
	private int [] _packet_sizes_in_frame = null;  //一个视频帧里各视频包的长度
	private LinkedList<int[]> _packet_sizes_in_frame_list = new LinkedList<int[]>();
	
	private int _packet_count_in_frame = 0;//一个视频帧里视频包个数
	private LinkedList<Integer> _packet_count_in_frame_list = new LinkedList<Integer>();
	
	private long SEND_INTERVAL = 0;//视频包发送间隔最小为20ms
	
	private long _latest_sample_timestamp = 0;//最近一次采样并编码的时戳
	
	/**
	 * 初始化编码器
	 * @param width： 视频帧的宽度
	 * @param height：视频帧的高度
	 * @param fps：编码帧率
	 * @param qp：量化步长（在10~51之间取值）
	 * @param packSize：视频数据包的最大字节数目（默认为1000）
	 * @param minIDR：IDR帧的最小间隔（默认为15）
	 * @param maxIDR：IDR帧的最大间隔（默认为150）
	 * @param adap：自适应I帧的门限值（为0时取消自适应IDR帧，默认值为40）
	 * @return：返回值为编码器的句柄
	 */
	private native long CompressBegin(int width, int height, int fps, int qp, int packSize, int minIDR, int maxIDR, int adap);//底层库函数：创建视频编码器
	
	/**
	 * 初始化编码器
	 * @param width： 视频帧的宽度
	 * @param height：视频帧的高度
	 * @param fps：编码帧率
	 * @param qp：量化步长（在10~51之间取值）
	 * @param packSize：视频数据包的最大字节数目（默认为1000）
	 * @param minIDR：IDR帧的最小间隔（默认为15）
	 * @param maxIDR：IDR帧的最大间隔（默认为150）
	 * @param adap：自适应I帧的门限值（为0时取消自适应IDR帧，默认值为40）
	 * @param bitrate：码率（单位：Kbit/s， 为0时关闭码率控制）
	 * @return：返回值为编码器的句柄
	 */
	//private native long CompressBeginBitctrl(int width, int height, int fps, int bitrate, int packetsize, int IPeroid, int unit);
	
	/**
	 * 编码一帧
	 * @param encoder：编码器句柄
	 * @param type：把当前图像强制编码为指定类型的视频帧，默认值为-1（-1：Auto  0：P帧   1：IDR帧    2：I帧）
	 * @param in：待编码的一帧图像
	 * @param insize：图像的尺寸
	 * @param out：编码后输出的码流
	 * @param size：码流分为多个数据包，该整形数组记录每个数据包的尺寸
	 * @return：返回值为一帧图像编码后包含数据包的数目
	 */
	private native int CompressBuffer(long encoder, int type, byte[] in, int insize, byte[] out ,int [] size); //底层库函数：编码一帧图像
	
	/**
	 * 销毁编码器
	 * @param encoder：编码器句柄
	 * @return：返回值为1表明编码器已经成功销毁
	 */
	private native int CompressEnd(long encoder); //底层库函数：退出视频编码器，结束编码
	
	//private native long CompressBegin(int width, int height, int fps, int qp, int packetsize, int minIDR, int maxIDR, int apdI);
	//private native int CompressBuffer(long encoder, int type, byte[] in, int insize, byte[] out ,int [] size);
	//private native int CompressEnd(long encoder);
	
	public H264Encoder()
	{
		//根据CPU架构加载相应的编码库
		/*CPUStypeDistinct cpu = new CPUStypeDistinct();
		switch(cpu.GetCPUStype())
		{
			case 1://ARM V9及更早版本
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
		int IDR_interval = 3000 / GD.VIDEO_SAMPLE_INTERVAL;//每隔3秒一个I帧
		
		int FPS = 1000 / GD.VIDEO_SAMPLE_INTERVAL;//(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp")) ? 6 : 12;//编码帧率
		
		int adaptive_IDR_interval = 0;
		
		//初始化编码器，指定编码缓存大小
		//量化步长值域为[23, 51], 数值越小越清晰，设为31
		
		//_h264_endcoder = CompressBeginBitctrl(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, FPS, 1000, GD.MAX_VIDEO_PACKET_SIZE, IDR_interval, 5);
		_h264_endcoder = CompressBegin(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, FPS, QP, GD.MAX_VIDEO_PACKET_SIZE, IDR_interval, IDR_interval, 0);
		
		_frame_buffer = new byte[65536];
		_packet_sizes_in_frame = new int[64];
		
		_is_running=true;
	}
	
	//对YUV420帧做180度翻转
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
	
	//编码一帧原始视频
	public void encode(byte[] raw_video_frame, boolean rotate)
	{
		GD.VIDEO_ENCODE_STRATEGY = -1;//流畅演示
		
		//正常情况下视频采样编码间隔至少VIDEO_SAMPLE_INTERVAL毫秒
		//只编IDR帧时相邻两IDR帧至少间隔ONLY_IDR_FRAME_INTERVAL毫秒
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
			//队列满时不编码
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
			//编码一帧图像并返回该帧内视频包个数
			_packet_count_in_frame = CompressBuffer(_h264_endcoder, GD.VIDEO_ENCODE_STRATEGY,
													rotated_frame, rotated_frame.length,
													_frame_buffer, _packet_sizes_in_frame);
			Log.v("Video", "enc 1: " + (System.currentTimeMillis() - t));
		}
		else
		{
			long t = System.currentTimeMillis();
			//编码一帧图像并返回该帧内视频包个数
			_packet_count_in_frame = CompressBuffer(_h264_endcoder, GD.VIDEO_ENCODE_STRATEGY,
													raw_video_frame, raw_video_frame.length,
													_frame_buffer, _packet_sizes_in_frame);
			Log.v("Video", "enc 2: " + (System.currentTimeMillis() - t));
		}
		
		//Log.v("Video", "video enc: " + _packet_count_in_frame);
		
		if(0 == _packet_count_in_frame)
			return;
		
		//将编码后得到的数据写入队列
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
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);//设置线程优先级
			
			byte[] frame_buffer = null;//编码后帧缓冲区
			int[] packet_sizes_in_frame = null;//一帧中各包长度
			int packet_count_in_frame = 0;//一帧中包总数
			long packet_pos_in_frame = 0;//首字节为数据包在帧中的序号，从0计数，次字节为帧内包总数，其余填1
			
			int send_bytes_in_frame = 0;//一帧视频中已发送的数据长度
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
				
				//取数据包
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
					
					//取数据包失败则
					if(null == frame_buffer || 0 == packet_count_in_frame || null == packet_sizes_in_frame)
					{
						H264Encoder.class.wait(60);
						
						_thread_statistics.leave();
						continue; 
					}
				}
				
				//依次发送一帧视频中的各数据包
				timestamp = System.currentTimeMillis();//一帧内的各数据包应采用同样的时戳
				
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
					//修改timestamp
					//首字节为帧内包序号，从0开始
					//次字节后四位为帧内包总数
					//后两字节为原timestamp内容
					packet_pos_in_frame = 0;
					packet_pos_in_frame |= (i << 24);//首字节为帧内包序号
					packet_pos_in_frame |= (packet_count_in_frame << 16);//次节后四位为帧内包总数
					//packet_pos_in_frame |= 0xFFFF;//后两字节填1					
					
					//将packet_pos_in_frame置于timestamp中
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
						sleep(SEND_INTERVAL);//平缓发包频率，避免拥塞
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
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);//设置线程优先级
			
			byte[] frame_buffer = null;//编码后帧缓冲区
			int[] packet_sizes_in_frame = null;//一帧中各包长度
			int packet_count_in_frame = 0;//一帧中包总数
			int packet_pos_in_frame = 0;//首字节为数据包在帧中的序号，从0计数，次字节为帧内包总数，其余填1
			
			int send_bytes_in_frame = 0;//一帧视频中已发送的数据长度
			int sequence = 0;
			long timestamp = 0;
			
			byte[] video_packet_buffer = new byte[1024];//须包含RTP包头和RTSP包头
			
			final int rtp_header_length = 12;
			final int rtsp_header_length = 4;
			final long imsi = GD.get_unique_id(GD.get_global_context());
			
			//添加RTP包头固定部分							
			//payloadtype-2(H.264)
			video_packet_buffer[1 + rtsp_header_length] = (byte)((video_packet_buffer[1 + rtsp_header_length] & 0x80) | (2 & 0x7F));
			
			//ssrc
			RtpPacket.setLong(imsi, video_packet_buffer, 8 + rtsp_header_length, 12 + rtsp_header_length);
			
			while(_is_running)
			{
				_thread_statistics.enter();
				
				//取数据包
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
					
					//取数据包失败则
					if(null == frame_buffer || 0 == packet_count_in_frame || null == packet_sizes_in_frame)
					{
						H264Encoder.class.wait(60);
						
						_thread_statistics.leave();
						continue; 
					}
				}				
				
				//依次发送一帧视频中的各数据包
				timestamp = System.currentTimeMillis();//一帧内的各数据包应采用同样的时戳
				
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
					packet_pos_in_frame |= (i << 28);//第一字节前四位为帧内包序号					
					packet_pos_in_frame |= (packet_count_in_frame << 24);//第一字节后四位为帧内包总数					
					packet_pos_in_frame |= 0xFFFFFF;//后三字节填1
					
					//将packet_pos_in_frame头字节置于timestamp中
					timestamp &= 0xFFFFFF;
					timestamp |= packet_pos_in_frame;*/
					
					packet_pos_in_frame = 0;
					packet_pos_in_frame |= (i << 24);//首字节为帧内包序号
					packet_pos_in_frame |= (packet_count_in_frame << 16);//次节后四位为帧内包总数
					packet_pos_in_frame |= 0xFFFF;//后两字节填1					
					
					//将packet_pos_in_frame置于timestamp中
					timestamp &= 0xFFFF;//兼容web播放暂时关闭 timestamp &= 0xFFFF;
					timestamp |= packet_pos_in_frame;//兼容web播放暂时关闭 timestamp |= packet_pos_in_frame;
					
					//添加RTP包头动态部分
					//sequence
					RtpPacket.setInt(sequence++, video_packet_buffer, 2 + rtsp_header_length, 4 + rtsp_header_length);
					
					//timestamp
					//一帧内的各数据包应采用同样的时戳
					RtpPacket.setLong(timestamp, video_packet_buffer, 4 + rtsp_header_length, 8 + rtsp_header_length);
					
					//mark
					video_packet_buffer[1 + rtsp_header_length] = RtpPacket.setBit(((i == packet_count_in_frame - 1) ? true : false), video_packet_buffer[1 + rtsp_header_length], 7);
					
					//添加RTSP包头
					int length = packet_sizes_in_frame[i] + rtp_header_length;							
					video_packet_buffer[0] = '$';
					video_packet_buffer[1] = 0;
					video_packet_buffer[2] = (byte)((length >> 8) & 0xff);
					video_packet_buffer[3] = (byte)(length & 0xff);
					
					send_bytes_in_frame += packet_sizes_in_frame[i];
					
					//发送
					RTSPClient.get_instance().send_data(video_packet_buffer,  length + 4);
					
					if(0 != SEND_INTERVAL)
					{
						sleep(SEND_INTERVAL);//平缓发包频率，避免拥塞
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
	 * free函数功能：关闭rtp发送，释放解码器资源
	 */
	public void free() 
	{
		_is_running = false;
		
		Log.i("H264Encoder","H264Encoder finish");
	}
}
