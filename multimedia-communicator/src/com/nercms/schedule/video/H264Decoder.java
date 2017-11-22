package com.nercms.schedule.video;

import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.nercms.schedule.misc.CPUStypeDistinct;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaSocketManager;
import com.nercms.schedule.misc.MediaStatistics;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.misc.SortTreeElement;
import com.nercms.schedule.misc.ThreadStatistics;
import com.nercms.schedule.sip.engine.net.RtpPacket;
import com.nercms.schedule.sip.engine.net.RtpSocket;

public class H264Decoder extends Thread 
{
	public boolean _receiving = true;   //视频接收运行标志
	public boolean _decoding = true;  //视频解码运行标志
	
	VideoDecode _video_decode_thread = null;//视频解码线程	
	
	//视频数据包队列
	private LinkedList<byte[]> _video_packet_list; //视频数据包(净荷)队列
	private LinkedList<int[]> _mark_sequence_list; //数组元素1：MARK  2：序列号
	private LinkedList<Long> _timestamp_list; //数组元素1：时戳
	
	//private VideoRenderView _video_render_view = null;
	private SurfaceView _video_render_view = null;
	
	private final int _MAX_LIST_SIZE = 100;//30;//接收队列最大长度
	
	private final static int _MAX_SORT_PACKET_NUM = 8;//最多参与排序的数据包个数
	private final static int _SORT_PACKET_NUM = 0;//实际只对最近3个数据包排序
	
	public MediaStatistics _statistics = null;//视频数据接收统计
	
	//视频解码JNI函数
	public native int InitDecoder(int width, int height); // 底层库函数：创建解码器
	public native int DecoderNal(byte[] in, int insize, byte[] out); // 底层库函数：解码得到一帧图像
	public native int UninitDecoder(); // 底层库函数：退出解码器，释放资源
	
	private int _video_decoder_handle = -1;//解码器句柄
	private boolean _using_video_decoder = false;//解码器是否正在使用
	
	private Object _video_render_lock = new Object();
	
	void reset_video_decoder_when_idle()//空闲时重启解码器，否则残存视频数据影响下次调度解码
	{
		if(-1 == _video_decoder_handle)
		{
			//解码器还未创建则必须创建
			//取到数据包后立即创建解码器
			long t = System.currentTimeMillis();
			_video_decoder_handle = InitDecoder(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT);
			Log.v("Video", "InitDecoder 1 " + (System.currentTimeMillis() - t) + "ms, handle " + _video_decoder_handle);
		}
		else
		{
			//如解码器已创建且已使用则先关闭再创建，同时置为未使用
			if(true == _using_video_decoder)
			{
				long t = System.currentTimeMillis();
				UninitDecoder();
				Log.v("Video", "UninitDecoder " + (System.currentTimeMillis() - t) + "ms");
				
				t = System.currentTimeMillis();
				_video_decoder_handle = InitDecoder(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT);
				Log.v("Video", "InitDecoder 2 " + (System.currentTimeMillis() - t) + "ms, handle " + _video_decoder_handle);
				
				_using_video_decoder = false;
			}
		}
	}
	
	//public void set_render_view(VideoRenderView video_render_view)
	public void set_render_view(SurfaceView video_render_view)
	{
		synchronized(SurfaceView.class)
		{
			_video_render_view = video_render_view;
		}
	}
	
	public H264Decoder()
	{
		//根据CPU架构加载相应的解码库
		/*CPUStypeDistinct cpu = new CPUStypeDistinct();
		
		switch(cpu.GetCPUStype())
		{
			case 1://ARM V9及更早版本
			default:
				System.loadLibrary("H264Decoder_c");
				break;
						
			case 2://ARM V11
				System.loadLibrary("H264Decoder_armv4");
				break;
					
			case 3://Cortex A8/A9/A15
				System.loadLibrary("H264Decoder_neon");
				break;
		}*/
		System.loadLibrary("H264Decoder_neon");
		
		_video_packet_list = new LinkedList<byte[]>();
		_mark_sequence_list = new LinkedList<int[]>();
		_timestamp_list = new LinkedList<Long>();
		
		_video_decode_thread = new VideoDecode();
		_video_decode_thread.start();
		
		_statistics = new MediaStatistics(1);
	}
	
	@Override
	protected void finalize()
	{
		_video_packet_list = null;
		_mark_sequence_list = null;
		_timestamp_list = null;
		_statistics = null;
	}
	
	public double get_packet_lost_rate()
	{
		if(null == _statistics)
			return 0.0;
		
		return _statistics.get_packet_lost_rate();
	}
	
	private ThreadStatistics _recv_thread_statistics = new ThreadStatistics("Video Recv Thread");
	private ThreadStatistics _decode_thread_statistics = new ThreadStatistics("Video Decode Thread");
	
	public void thread_statistics_record()
	{
		if(null != _recv_thread_statistics)
		{
			_recv_thread_statistics.record();
		}
		
		if(null != _decode_thread_statistics)
		{
			_decode_thread_statistics.record();
		}
		
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "Discard " + get_discard_count() + " video packet\r\n");
	}
	
	//主动丢弃视频包统计
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
	
	//模拟丢包
	private boolean lost_packet_simulation(int sequence)
	{
		return false;//不模拟丢包		
		//return (0 == (sequence % 10));//模拟每10个包丢一个包
	}
	
	long _latest_packet_timestamp = 0;//最近一次收到视频包的时戳
	
	int _latest_sequence = -1;
	public void add_video_packet(byte[] data, int length)
	{
		//视频包含24个字节的双RTP包头
		//前12字节RTP包头为RTSP协议栈所加
		//后12字节RTP包头为视频源所加
		if(null == data || 24 >= data.length || 24 >= length)
		{
			Log.v("RTSP", "wrong audio packet");
			return;
		}
		
		int rtp_header_offset = 12;//0;//RTSPServer额外增加了12个字节的RTP包头
		
		int sequence = (GD.byte_2_int(data[2 + rtp_header_offset]) << 8)
						+ GD.byte_2_int(data[3 + rtp_header_offset]);
		
		long timestamp = (GD.byte_2_int(data[4 + rtp_header_offset]) << 24)
						+ (GD.byte_2_int(data[5 + rtp_header_offset]) << 16)
						+ (GD.byte_2_int(data[6 + rtp_header_offset]) << 8)
						+ GD.byte_2_int(data[7 + rtp_header_offset]);
		
		long ssrc = (GD.byte_2_int(data[8 + rtp_header_offset]) << 24)
					+ (GD.byte_2_int(data[9 + rtp_header_offset]) << 16)
					+ (GD.byte_2_int(data[10 + rtp_header_offset]) << 8)
					+ GD.byte_2_int(data[11 + rtp_header_offset]);
		
		boolean mark = RtpPacket.getBit(data[1 + rtp_header_offset], 7);
		
		//Log.v("Baidu", "V " + sequence + ", " + ssrc + ", " + timestamp + ", " + mark);
		
		if(-1 == _latest_sequence)
		{
			_latest_sequence = sequence;
		}
		else
		{
			if(sequence != _latest_sequence + 1)
			{
				Log.v("Baidu", "lost video packet " + _latest_sequence + " " + sequence);
			}
			
			_latest_sequence = sequence;
		}
		
		//更新统计信息
		_statistics.update_packet_statistics(sequence, timestamp, mark, length);
		
		//根据RTP包SSRC字段获取当前的视频源ID
		//如视频源为MUSIC则ssrc值为-1
		GD._video_source_imsi = Long.toString(ssrc);
		GD._video_source_imsi = (true == GD._video_source_imsi.equalsIgnoreCase("-1")) ? "4294967295" : GD._video_source_imsi;
		//Log.v("Temp", "video source 3 " + GD._video_source_imsi + " " + ssrc);
		
		//Log.v("RTSP", "vs " + sequence);
		
		//if(true) return;
		
		//空闲状态不处理收到的视频包
		/*if(true == MediaThreadManager.get_instance()._video_receive_idle)
			return;*/
		
		//模拟丢包
		if(true == lost_packet_simulation(sequence))
		{
			return;
		}
		
		//添加视频包
		//获取数据包序列号及标志位
		int[] mark_seq = new int[2];
		mark_seq[0] = (true == mark) ? 1 : 0;
		mark_seq[1] = sequence;
				
		//获取数据包净荷
		byte[] payload = new byte[length - 24];
		
		System.arraycopy(data, 24, payload, 0, length - 24);
		
		//将视频数据写入都列
		synchronized(H264Decoder.class) 
		{
			if (_MAX_LIST_SIZE <= _video_packet_list.size()) 
			{
				//如队列超长则主动丢包					
				//GD.log_to_db(GeneralDefine.get_global_context(), 0, "Statistics", "Discard 1 video packet\r\n");
		  		Log.v("RTSP", "Discard 1 video packet");
				add_discard_count();
				
				try
				{
					_video_packet_list.removeFirst();
					_mark_sequence_list.removeFirst();
					_timestamp_list.removeFirst();
				}
				catch(NoSuchElementException e){}
			}
			
			_mark_sequence_list.add(mark_seq);
			_timestamp_list.add(timestamp);
			_video_packet_list.add(payload);
			
			H264Decoder.class.notify();
		}
	}
	
	private void recv_tcp_run()
	{
		Log.v("RTSP", "Video Recv TCP");
		
		try
		{
			boolean stop_statistics = true;//是否停止统计
			
			_statistics.reset();
			
			while (_receiving) 
			{
				_recv_thread_statistics.enter();
				
				//数据统计处理
				if(true == MediaThreadManager.get_instance()._video_receive_idle)
				{
					//不接收视频时视频源为空
					GD._video_source_imsi = "";
					
					if(false == stop_statistics)
					{
						_statistics.pause();//暂停统计
						
						synchronized(H264Decoder.class) 
						{
							_mark_sequence_list.clear();
							_timestamp_list.clear();
							_video_packet_list.clear();
						}
					}
					
					stop_statistics = true;				
				}
				else
				{
					if(true == stop_statistics)
					{
						_statistics.resume();//重启统计
					}
					
					stop_statistics = false;
				}
				
				Thread.sleep(100);
				
				_recv_thread_statistics.leave();
			}
			
			Log.d(GD.LOG_TAG, "video receive thread finish.");
		}
		catch(Exception e)
		{
			
		}
	}
	
	private void recv_udp_run()
	{
		Log.v("RTSP", "Video Recv UDP");
		
		try
		{
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);//设置线程优先级
			
			RtpSocket socket = MediaSocketManager.get_instance().get_video_recv_socket();
			socket.empty(GD.UDP_VIDEO_RECV_SOCKET_TIME_OUT);
			
			int payload_length_in_packet;
			
			//boolean idle_to_empty_socket = false;
			
			boolean stop_statistics = true;//是否停止统计
			
			_statistics.reset();
			
			///////////////////////////
			/*int sort_cache_index = 0;
			byte[][] sort_cache = new byte[_MAX_SORT_PACKET_NUM][2048];
			
			//RTP包排序比较函数
			@SuppressWarnings("rawtypes")
			Comparator comparator = new Comparator() 
			{
				public int compare(Object o1, Object o2) 
				{
					SortTreeElement e1 = (SortTreeElement) o1;
					SortTreeElement e2 = (SortTreeElement) o2;
					return e1.compareTo(e2);
				}
			};
			
			//创建RTP包排序比较树
			@SuppressWarnings("unchecked")
			TreeSet<SortTreeElement> sort_tree_set = new TreeSet<SortTreeElement>(comparator);
			
			synchronized(H264Decoder.class) 
			{
				_mark_sequence_list.clear();
				_timestamp_list.clear();
				_video_packet_list.clear();
			}
			
			sort_tree_set.clear();//清空排序树*/
			
			while(_receiving) 
			{
				_recv_thread_statistics.enter();
				
				//数据统计处理
				if(true == MediaThreadManager.get_instance()._video_receive_idle)
				{
					//不接收视频时视频源为空
					GD._video_source_imsi = "";
					
					if(false == stop_statistics)
					{
						_statistics.pause();//暂停统计
						
						socket.empty(GD.UDP_VIDEO_RECV_SOCKET_TIME_OUT);
						
						synchronized(H264Decoder.class) 
						{
							socket.empty(GD.UDP_VIDEO_RECV_SOCKET_TIME_OUT);
							
							Log.v("Video", "clear socket buffer");
							
							_mark_sequence_list.clear();
							_timestamp_list.clear();
							_video_packet_list.clear();
						}
					}
					
					stop_statistics = true;
					
					sleep(60);//视频接受socket的超时较短，空闲时需在此sleep
				}
				else
				{
					if(true == stop_statistics)
					{
						_statistics.resume();//重启统计
					}
					
					stop_statistics = false;
				}
				
				if(null == socket)
				{
					//获取socket失败
					Thread.sleep(10);
					
					socket = MediaSocketManager.get_instance().get_video_recv_socket();
					socket.empty(GD.UDP_VIDEO_RECV_SOCKET_TIME_OUT);
					
					_recv_thread_statistics.leave();
					continue;
				}
				
				//接收视频数据
				if(false == socket.receive())
				{
					if(10000 < (System.currentTimeMillis() - _latest_packet_timestamp))
					{
						//连续10秒收不到视频则认为视频源已消失
						GD._video_source_imsi = "";
					}
					
					_recv_thread_statistics.leave();
					continue;
				}
				
				//空闲或SOS调度中暂停时不处理收到的视频包
				//if(true == MediaThreadManager.get_instance()._video_receive_idle || true == GD.NO_RENDERING_DECODE_VIDEO)
				if(true == GD.NO_RENDERING_DECODE_VIDEO)
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//判断是否为H.264视频包
				if(2 != socket._rtp_packet.getPayloadType())
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//Log.v("Video", "VU " + (System.currentTimeMillis() - vut) + " " + socket._rtp_packet.getPayloadLength());
				//Log.v("Video", "RVP " + socket._rtp_packet.getSscr());
				
				GD.update_recv_video_timestamp();
				
				//更新用于判断视频源是否丢失
				_latest_packet_timestamp = System.currentTimeMillis();
				
				//更新统计信息
				_statistics.update_packet_statistics(socket._rtp_packet.getSequenceNumber(),
													socket._rtp_packet.getTimestamp(),
													socket._rtp_packet.hasMarker(),
													socket._rtp_packet.getLength());
				
				payload_length_in_packet = socket._rtp_packet.getPayloadLength();
				
				if(0 == payload_length_in_packet)
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//模拟丢包
				if(true == lost_packet_simulation(socket._rtp_packet.getSequenceNumber()))
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//根据RTP包SSRC字段获取当前的视频源ID
				GD._video_source_imsi = socket._rtp_packet.getSscr() + "";
				GD._video_source_imsi = (true == GD._video_source_imsi.equalsIgnoreCase("-1")) ? "4294967295" : GD._video_source_imsi;
				
				//获取数据包序列号及标志位
				int[] mark_seq = new int[2];
				mark_seq[0] = (true == socket._rtp_packet.hasMarker()) ? 1 : 0;
				mark_seq[1] = socket._rtp_packet.getSequenceNumber();
				
				//获取数据包时戳
				long timestamp = socket._rtp_packet.getTimestamp();
				
				//获取数据包净荷
				byte[] payload = new byte[payload_length_in_packet];
				
				if(null == payload || null == socket._rtp_packet || null == socket._rtp_packet.getPacket())
				{
					_recv_thread_statistics.leave();
					continue;
				}
				
				//Log.v("Video", "UR");
				
				System.arraycopy(socket._rtp_packet.getPacket(), socket._rtp_packet.getHeaderLength(), payload, 0, payload_length_in_packet);
				
				//Log.v("Video", "V " + socket._rtp_packet.getSequenceNumber() + ", " + payload_length_in_packet);
				
				//将视频数据写入队列
				synchronized(H264Decoder.class)
				{
					if (_MAX_LIST_SIZE <= _video_packet_list.size()) 
					{
						//如队列超长则主动丢包					
						//GD.log_to_db(GeneralDefine.get_global_context(), 0, "Statistics", "Discard 1 video packet\r\n");
				  		//Log.v("Network", "Discard 1 video packet");
						add_discard_count();
						
						_video_packet_list.removeFirst();
						_mark_sequence_list.removeFirst();
						_timestamp_list.removeFirst();
					}
					
					_mark_sequence_list.add(mark_seq);
					_timestamp_list.add(timestamp);
					_video_packet_list.add(payload);
					
					H264Decoder.class.notify();
				}
				
				_recv_thread_statistics.leave();
			}
			
			Log.d(GD.LOG_TAG, "video receive thread finish.");
		}
		catch(Exception e)
		{
			
		}
	}
	
	//视频接收线程
	@Override
	public void run() 
	{
		if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("udp"))
		{
			recv_udp_run();
		}
		else if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
		{
			recv_tcp_run();
		}
	}
	
	public void free()
	{
		_receiving = false;
		_decoding = false;
		
		if(null == _video_decode_thread)
			return;
		
		try {
			_video_decode_thread.join();
			_video_decode_thread = null;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		Log.d(GD.LOG_TAG, "free_VideoReceive");
	}
	
	//视频解码及播放线程
	class VideoDecode extends Thread 
	{
		public byte[] _decode_buffer = new byte[GD.VIDEO_WIDTH * GD.VIDEO_HEIGHT * 2];
		
		public VideoDecode() 
		{
			super();
			
			rest_render_buffer();
		}
		
		@Override
		protected void finalize()
		{
			_decode_buffer = null;
		}
		
		//重置显示缓存
		private void rest_render_buffer()
		{
			//Log.v("Video", "rest_render_buffer");
			/*for(int i = 0; i < _decode_buffer.length; i++)
			{
				_decode_buffer[i] = (byte) 0x00;
			}
			
			GD._video_render_buffer = null;
			GD._video_render_buffer = ByteBuffer.wrap(_decode_buffer);*/
		}
		
		//int packet_of_frame = 0;//帧内包序号
		//int packet_in_frame = 0;//帧内包总数
		int last_packet_sequence = 0;
		boolean waiting_for_I_frame = false;
		private int lost_packet_of_frame = 0;//当前帧内已丢包数，实际只用于I帧丢包统计
		private long last_packet_timestamp = 0;//上一帧内包的时戳
		//boolean is_I_frame = false;
		private final int THRESHOLD_OF_LOST_PACKET_IN_I_FRAME = 1;//I帧内丢1个以上的包即不解码
		
		//判断数据包是否需要解码
		private boolean need_to_be_decoded(boolean is_I_frame, long timestamp, int sequence)
		{
			if(true) return true;
			
			if(true == GD.TOLERANCE_FOR_VIDEO_MOSAIC) return true;//容忍马赛克则每包均解码
			
			//Log.v("Video", (packet_timestamp >> 24) + " / " + (0xff &(packet_timestamp >> 16)));
			//packet_of_frame = (int)(packet_timestamp >> 24);//帧内包序号
			//packet_in_frame = (int)((0xff &(packet_timestamp >> 16)));//帧内包总数
			
			//时戳有变意味着收到新一帧内数据包
			if(last_packet_timestamp != timestamp)
				lost_packet_of_frame = 0;
			
			last_packet_timestamp = timestamp;//更新最近的帧内包时戳
			
			//是否I帧
			if(true == is_I_frame)//I帧丢1个以上的包不解码
			{
				//Log.v("Video", "I");
				if((0 != last_packet_sequence//非序列号溢出归零时刻
					&& last_packet_sequence + 1 != sequence)//有丢包
					&& 0 != (timestamp >> 24))//包内帧序号非零，意味着如有丢包则丢的是帧内包
				{
					Log.v("Video", "LI");
					lost_packet_of_frame++;
				}
				
				last_packet_sequence = sequence;							
				
				if(THRESHOLD_OF_LOST_PACKET_IN_I_FRAME >= lost_packet_of_frame)
				{
					waiting_for_I_frame = false;
				}
				else//I帧丢THRESHOLD_OF_LOST_PACKET_IN_I_FRAME个以上不解码
				{
					//Log.v("Video", "LPOF " + lost_packet_of_frame);
					waiting_for_I_frame = true;//等待下一个I帧
					
					return false;
				}
			}
			else//非I帧一旦丢包则不解码，直至下一个I帧
			{
				if((0 != last_packet_sequence && last_packet_sequence + 1 != sequence)//非序列号溢出归零时刻有丢包
					|| true == waiting_for_I_frame)//等待I帧
				{
					//Log.v("Video", "P 1");
					//丢弃不解
					last_packet_sequence = sequence;
					waiting_for_I_frame = true;
					//Log.v("Temp", "not decode");
					
					return false;
				}
				else
				{
					//Log.v("Video", "P 2");
					//继续解码
					last_packet_sequence = sequence;
					waiting_for_I_frame = false;
				}
			}
			
			return true;
		}
		
		private void video_render()
		{
			GD._video_render_buffer.rewind();//for Android 4.2
			GD.VideoBit.copyPixelsFromBuffer(GD._video_render_buffer);
			
			int w = GD.VIDEO_WIDTH;
			int h = GD.VIDEO_HEIGHT;
			
			//缩放显示
			Matrix matrix = new Matrix();			
			//if(352 == w || 586 == w)
			{
			 	matrix.setRotate(90);
			}			
			
			matrix.postScale(w / GD.VIDEO_WIDTH, h / GD.VIDEO_HEIGHT);
			
			matrix.postScale(GD._video_render_scale, GD._video_render_scale);
			
			Canvas canvas = _video_render_view.getHolder().lockCanvas();
			canvas.drawBitmap(Bitmap.createBitmap(GD.VideoBit, 0, 0, GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT, matrix, true), 0, 0, null);
			_video_render_view.getHolder().unlockCanvasAndPost(canvas);
			//_video_render_view.setImageBitmap(Bitmap.createBitmap(GD.VideoBit, 0, 0, 352, 288, matrix, true));//, 0, 0, null);
		}
		
		private void play_udp_run()
		{
			Log.v("RTSP", "Video Play UDP");
			
			try
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY); //提高线程优先级
				
				int decode_result = 0;
				int sort_cache_index = 0;
				byte[][] sort_cache = new byte[_MAX_SORT_PACKET_NUM][2048];
				
				byte[] frame_buffer = new byte[65536]; // 64k 定义一帧完整码流的缓存
				int[] packet_mark_sequence = null;
				long packet_timestamp = 0;
				byte[] packet_payload= null;
				
				int frame_size = 0;
				
				//RTP包排序比较函数
				@SuppressWarnings("rawtypes")
				Comparator comparator = new Comparator() 
				{
					public int compare(Object o1, Object o2) 
					{
						SortTreeElement e1 = (SortTreeElement) o1;
						SortTreeElement e2 = (SortTreeElement) o2;
						return e1.compareTo(e2);
					}
				};
				
				//创建RTP包排序比较树
				@SuppressWarnings("unchecked")
				TreeSet<SortTreeElement> sort_tree_set = new TreeSet<SortTreeElement>(comparator);
				
				GD.video_is_coming = false;
				
				synchronized(H264Decoder.class) 
				{
					_mark_sequence_list.clear();
					_timestamp_list.clear();
					_video_packet_list.clear();
					
					rest_render_buffer();
				}
				
				//创建视频包拼帧器
				PacketFrames video_frame_combination = new PacketFrames();
				video_frame_combination.handle = video_frame_combination.CreateH264Packer();
				
				sort_tree_set.clear();//清空排序树
				
				while(_decoding)
				{
					_decode_thread_statistics.enter();
					
					if(true == MediaThreadManager.get_instance()._video_receive_idle)
					{
						reset_video_decoder_when_idle();
						
						rest_render_buffer();
						
						//Log.v("Video", "VP 1 " + _video_packet_list.size());
						
						GD.video_is_coming = false;
						
						last_packet_sequence = 0;
						waiting_for_I_frame = false;
						
						//Thread.sleep(10);
						
						//_decode_thread_statistics.leave();
						//continue;
					}
				
					packet_mark_sequence = null;
					packet_timestamp = 0;
					packet_payload= null;
					
					//int packet_list_size = 0;
					
					//从队列取视频数据包
					synchronized(H264Decoder.class) 
					{
						//Log.v("Video", "VP 2 " + _video_packet_list.size());
						
						if(0 < _video_packet_list.size())
						{
							//packet_list_size = _video_packet_list.size();
							
							packet_mark_sequence = _mark_sequence_list.removeFirst();
							packet_timestamp = _timestamp_list.removeFirst();
							packet_payload = _video_packet_list.removeFirst();
						}
						
						//取数据包失败
						if(null == packet_payload || null == packet_mark_sequence || 0 == packet_timestamp)
						{
							if(null != _video_render_view)
							{
								//Log.v("Baidu", "video render 1");
								_video_render_view.postInvalidate();//显示视频连接中
							}
							
							H264Decoder.class.wait(60);
							
							_decode_thread_statistics.leave();
							continue;
						}
						
						//Log.v("Video", "UF " + Long.toString(packet_timestamp) + " " + packet_mark_sequence[0] + " " + packet_payload.length);
					}
					
					//Log.v("Video", "UFetched");
					
					//将取出的包的序列号插入排序树，插入过程即进行排序
					{
						//sort_cache_index必须小于_MAX_SORT_PACKET_NUM
						/*if(_MAX_SORT_PACKET_NUM <= sort_cache_index)
						{
							continue;
						}*/
						sort_cache_index %= _MAX_SORT_PACKET_NUM;
						
						if(null == packet_payload || null == sort_cache[sort_cache_index])
						{
							_decode_thread_statistics.leave();
							continue;
						}
						
						System.arraycopy(packet_payload, 0, sort_cache[sort_cache_index], 0, packet_payload.length);
					
						SortTreeElement rtp_sort_stuff = new SortTreeElement(sort_cache_index/*不再排序 sort_cache_index++*/,
																	packet_mark_sequence[1],
																	packet_mark_sequence[0],
																	packet_payload.length,
																	packet_timestamp & 0xFFFF);
						
						sort_tree_set.add(rtp_sort_stuff);
					}
					
					//实际对最近三个数据包排序即可
					if(_SORT_PACKET_NUM <= sort_tree_set.size()) 
					{
						SortTreeElement rtp_sort_stuff = sort_tree_set.first();
						
						/////////////////////////////////////////////////////////////////////////////////
						//判断是否需要解码						
						if(false == need_to_be_decoded((1 == video_frame_combination.IsIntraFrame(sort_cache[rtp_sort_stuff.pos], rtp_sort_stuff.len)),
														packet_timestamp, rtp_sort_stuff.sequence))
						{
							sort_tree_set.remove(rtp_sort_stuff);
							rtp_sort_stuff = null;
							_decode_thread_statistics.leave();
							continue;
						}
						/////////////////////////////////////////////////////////////////////////////////
						
						
						//Log.v("Video", "p " + rtp_sort_stuff.len);
						//拼帧
						frame_size = video_frame_combination.PackH264Frame(video_frame_combination.handle,
																			sort_cache[rtp_sort_stuff.pos],
																			rtp_sort_stuff.len,
																			rtp_sort_stuff.bMark,
																			(int)(rtp_sort_stuff.timestamp & 0xFFFF),
																			rtp_sort_stuff.sequence,
																			frame_buffer);
						
						//删除数据包
						sort_tree_set.remove(rtp_sort_stuff);
						rtp_sort_stuff = null;
						
						if(frame_size <= 0)
						{
							//还未拼成完整一帧
							_decode_thread_statistics.leave();
							continue;
						}
						
						if(true == MediaThreadManager.get_instance()._video_receive_idle)
						{
							continue;
						}
						
						//Log.v("Video", packet_list_size + " UF " + ((System.currentTimeMillis() % 0x100000000L) - packet_timestamp));
						//Log.v("Video", "OK T " + (int)(rtp_sort_stuff.timestamp & 0xFFFF));
						
						//解码已拼成的完整一帧					
						synchronized(_video_render_lock)
						{
							//解码一帧，返回值大于0为解码成功
							decode_result = DecoderNal(frame_buffer, frame_size, _decode_buffer);
							
							Log.v("Video", "dec " + frame_size);
							
							_using_video_decoder = true;
							
							GD.update_render_video_timestamp();
							
							//显示一帧图像
							if(false == GD.MEDIA_PAUSE_IN_SCHEDULE && 0 < decode_result) 
							{
								GD._video_render_buffer = ByteBuffer.wrap(_decode_buffer);
								GD.video_is_coming = true;
								
								if(null != _video_render_view)
								{
									//Log.v("Baidu", "video render 2");
									//long t = System.currentTimeMillis();
									//双屏时无需此操作 _video_render_view.setZOrderOnTop(true);
									video_render();
									_video_render_view.postInvalidate();
									//Log.v("Baidu", "video render 2.1 " + (System.currentTimeMillis() - t));
								}
							}
						}
					}
					
					_decode_thread_statistics.leave();
				}
				
				if(-1 != _video_decoder_handle)
				{
					UninitDecoder();
					_video_decoder_handle = -1;
				}
				
				GD.video_is_coming = false;
				
				//清空视频缓存
				GD._video_render_buffer.clear();
				GD._video_render_buffer = null;
				_decode_buffer = null;
				
				//销毁拼帧器
				if(null != video_frame_combination) 
				{
					video_frame_combination.DestroyH264Packer(video_frame_combination.handle);
					video_frame_combination = null;
				}
				
				frame_buffer = null;
				packet_mark_sequence = null;
				packet_payload= null;			
				sort_cache = null;
				
				Log.d(GD.LOG_TAG, "video Decode thread finish.");
			}
			catch(Exception e)
			{
				
			}
			
		}
		
		private void play_tcp_run()
		{
			Log.v("RTSP", "Video Play TCP");
			
			try
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY); //提高线程优先级
				
				int decode_result = 0;
				
				byte[] frame_buffer = new byte[65536]; // 64k 定义一帧完整码流的缓存
				int[] packet_mark_sequence = null;
				long packet_timestamp = 0;
				byte[] packet_payload= null;
				
				int frame_size = 0;
				
				GD.video_is_coming = false;
				
				synchronized(H264Decoder.class) 
				{
					_mark_sequence_list.clear();
					_timestamp_list.clear();
					_video_packet_list.clear();
					
					rest_render_buffer();
				}
				
				//创建视频包拼帧器
				PacketFrames video_frame_combination = new PacketFrames();
				video_frame_combination.handle = video_frame_combination.CreateH264Packer();
				
				while(_decoding)
				{
					_decode_thread_statistics.enter();
					
					if(true == MediaThreadManager.get_instance()._video_receive_idle)
					{
						reset_video_decoder_when_idle();
						
						rest_render_buffer();
						
						GD.video_is_coming = false;
						
						last_packet_sequence = 0;
						waiting_for_I_frame = false;
						
						//Thread.sleep(10);
						
						//_decode_thread_statistics.leave();
						//continue;
					}
					
					packet_mark_sequence = null;
					packet_timestamp = 0;
					packet_payload= null;
					
					//int packet_list_size = 0;
					
					//从队列取视频数据包
					synchronized(H264Decoder.class) 
					{
						//packet_list_size = _video_packet_list.size();
						
						if(0 < _video_packet_list.size())
						{
							packet_mark_sequence = _mark_sequence_list.removeFirst();
							packet_timestamp = _timestamp_list.removeFirst();
							packet_payload = _video_packet_list.removeFirst();
						}
						
						//取数据包失败
						if(null == packet_payload || null == packet_mark_sequence || 0 == packet_timestamp)
						{
							if(null != _video_render_view)
								_video_render_view.postInvalidate();//显示视频连接中
							
							H264Decoder.class.wait(100);
							
							_decode_thread_statistics.leave();
							continue;
						}
						
						//Log.v("RTSP", "TF " + Long.toString(packet_timestamp) + " " + packet_mark_sequence[0] + " " + packet_payload.length);
					}
					
					//必须与0xF相与，否则认为首位为符号位
					//Log.v("Video", "P " + (0xF & (packet_timestamp >> 28)) + " / " + ((packet_timestamp >> 24) & 0xF ));
					
					//Log.v("Video", "P " + (0xFF & (packet_timestamp >> 24)) + " / " + ((packet_timestamp >> 16) & 0xFF ));
					
					/////////////////////////////////////////////////////////////////////////////////
					//判断是否需要解码
					if(false == need_to_be_decoded((1 == video_frame_combination.IsIntraFrame(packet_payload, packet_payload.length)),
													packet_timestamp, packet_mark_sequence[1]))
					{
						packet_mark_sequence = null;
						packet_timestamp = 0;
						packet_payload= null;
						
						_decode_thread_statistics.leave();
						continue;
					}
					/////////////////////////////////////////////////////////////////////////////////
					
					//拼帧
					frame_size = video_frame_combination.PackH264Frame(video_frame_combination.handle,
																		packet_payload,
																		packet_payload.length, packet_mark_sequence[0],
																		(int)(packet_timestamp & 0xFFFF), packet_mark_sequence[1],
																		frame_buffer);
					
					if(frame_size <= 0)
					{
						//还未拼成完整一帧
						_decode_thread_statistics.leave();
						continue;
					}
					
					if(true == MediaThreadManager.get_instance()._video_receive_idle)
					{
						continue;
					}
					
					//Log.v("Video", packet_list_size + " TF " + ((System.currentTimeMillis() % 0x100000000L) - packet_timestamp));
					
					//解码已拼成的完整一帧
					synchronized(_video_render_lock)
					{
						//long timestamp = System.currentTimeMillis();
						
						//解码一帧，返回值大于0为解码成功
						decode_result = DecoderNal(frame_buffer, frame_size, _decode_buffer);
						
						_using_video_decoder = true;
						
						GD.update_render_video_timestamp();
						
						//显示一帧图像
						if(false == GD.MEDIA_PAUSE_IN_SCHEDULE && 0 < decode_result) 
						{
							GD._video_render_buffer = ByteBuffer.wrap(_decode_buffer);
							GD.video_is_coming = true;
							
							if(null != _video_render_view)
							{
								_video_render_view.postInvalidate();
							}
						}
						//Log.v("Video", "Decode " + (System.currentTimeMillis() - timestamp));
					}
					
					_decode_thread_statistics.leave();
				}
				
				GD.video_is_coming = false;
				
				if(-1 != _video_decoder_handle)
				{
					UninitDecoder();
					_video_decoder_handle = -1;
				}
				
				//清空视频缓存
				GD._video_render_buffer.clear(); 
				GD._video_render_buffer = null;
				_decode_buffer = null;
				
				//销毁拼帧器
				if(null != video_frame_combination) 
				{
					video_frame_combination.DestroyH264Packer(video_frame_combination.handle);
					video_frame_combination = null;
				}
				
				frame_buffer = null;
				packet_mark_sequence = null;
				packet_payload= null;	
				
				Log.d(GD.LOG_TAG, "video Decode thread finish.");
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
				play_udp_run();
			}
			else if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
			{
				play_tcp_run();
			}
		}
	}
}