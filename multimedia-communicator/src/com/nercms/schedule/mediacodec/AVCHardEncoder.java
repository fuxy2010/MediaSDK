package com.nercms.schedule.mediacodec;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.nercms.schedule.mediacodec.NalTypes;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaSocketManager;
import com.nercms.schedule.misc.ThreadStatistics;
import com.nercms.schedule.rtsp.RTSPClient;
import com.nercms.schedule.sip.engine.net.RtpPacket;
import com.nercms.schedule.sip.engine.net.RtpSocket;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

public class AVCHardEncoder extends Thread
{
	private MediaCodec _media_codec = null;
	private MediaCodecInfo _video_codec_info = null;
	private int _color_format = -1;
	
	private byte[] _sps_pps = null;
	
	private int _IDR_interval = 5000 / GD.VIDEO_SAMPLE_INTERVAL;//ÿ��5��һ��I֡
	private int _FPS = 1000 / GD.VIDEO_SAMPLE_INTERVAL;
	
	public AVCHardEncoder() 
	{
		_IDR_interval = 5;//5000 / GD.VIDEO_SAMPLE_INTERVAL;//ÿ��5��һ��I֡
		_FPS = 1000 / GD.VIDEO_SAMPLE_INTERVAL;
		
		//init_codec(bit_rate, frame_rate, i_frame_interval);
		init_codec(125000, _FPS, _IDR_interval);
		
		_is_running = true;
	}
	
	private void get_support_color_format()
	{
		int codec_counts = MediaCodecList.getCodecCount();
		
		for (int i = 0; i < codec_counts; i++)
		{
			MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
			if(false == info.isEncoder())
			{
				continue;
			}
			
			String[] types = info.getSupportedTypes();
			boolean found = false;
			for (int j = 0; j < types.length; j++)
			{
				Log.v("Baidu", "encoder type: " + types[j]);
				if(true == types[j].equals("video/avc"))
				{
					found = true;
				}
			}
			
			if(true == found)
			{
				_video_codec_info = info;
				break;
			}
		}
		
		Log.v("Baidu", "found video encoder: " + _video_codec_info.getName());
		
		//int color_format = 0;
		MediaCodecInfo.CodecCapabilities capabilities = _video_codec_info.getCapabilitiesForType("video/avc");
		
		Log.v("Baidu", "MediaCodecInfo.CodecCapabilities " + capabilities.colorFormats.length);
		
		for (int i = 0; i < capabilities.colorFormats.length/* && color_format == 0*/; i++)
		{
			int format = capabilities.colorFormats[i];
			Log.i("Baidu", i + " ====== Using color format " + format);
			
			switch(format)
			{
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
					Log.v("Baidu", "COLOR_FormatYUV420Planar");
					_color_format = format;
					break;
					
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
					Log.v("Baidu", "COLOR_FormatYUV420PackedPlanar");
					_color_format = format;
					break;
					
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
					Log.v("Baidu", "COLOR_FormatYUV420SemiPlanar");
					_color_format = format;
					break;
					
				case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
					Log.v("Baidu", "COLOR_FormatYUV420PackedSemiPlanar");
					_color_format = format;
					break;
					
				case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
					Log.v("Baidu", "COLOR_TI_FormatYUV420PackedSemiPlanar");
					_color_format = format;
					break;
					
				default:
					Log.v("Baidu", "color unknown");
					break;
			}
		}
	}
	
	private void init_codec(int bit_rate, int frame_rate, int i_frame_interval)
	{
		Log.v("Baidu", "init_media_codec W: " + GD.VIDEO_WIDTH + " H: " + GD.VIDEO_HEIGHT);
		
		get_support_color_format();
		
		_media_codec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat media_format = MediaFormat.createVideoFormat("video/avc", GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT);
		media_format.setInteger(MediaFormat.KEY_BIT_RATE, bit_rate);//125000);//����
		media_format.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);//15);//֡��
		media_format.setInteger(MediaFormat.KEY_COLOR_FORMAT, _color_format);//MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);//COLOR_FormatYUV420Planar);
		media_format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, i_frame_interval);//5);
		_media_codec.configure(media_format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		
		_media_codec.start();
	}
	
	public void close_codec()
	{
		try
		{
			if(null != _media_codec)
			{
				_media_codec.stop();
				_media_codec.release();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static byte[] YV12toYUV420PackedSemiPlanar(byte[] input, byte[] output, final int width, final int height)
	{
		//COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
		//We convert by putting the corresponding U and V bytes together (interleaved).
		final int frameSize = width * height;
		final int qFrameSize = frameSize/4;
		
		System.arraycopy(input, 0, output, 0, frameSize);//Y
		
		for(int i = 0; i < qFrameSize; i++)
		{
			output[frameSize + i*2] = input[frameSize + i + qFrameSize];//Cb(U)
			output[frameSize + i*2 + 1] = input[frameSize + i];//Cr(V)
		}
		return output;
	}
	
	public static byte[] YV12toYUV420Planar(byte[] input, byte[] output, int width, int height)
	{
		//COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
		//So we just have to reverse U and V.
		final int frameSize = width * height;
		final int qFrameSize = frameSize/4;
		
		System.arraycopy(input, 0, output, 0, frameSize); // Y
		System.arraycopy(input, frameSize, output, frameSize + qFrameSize, qFrameSize); // Cr (V)
		System.arraycopy(input, frameSize + qFrameSize, output, frameSize, qFrameSize); // Cb (U)
		
		return output;
	}
	
	public static byte[] swapYV12toI420(byte[] yv12bytes, byte [] i420bytes, int width, int height)
	{
		//byte[] i420bytes = new byte[yv12bytes.length];
		
		for(int i = 0; i < width*height; i++)
	        i420bytes[i] = yv12bytes[i];
		
		for(int i = width*height; i < width*height + (width/2*height/2); i++)
			i420bytes[i] = yv12bytes[i + (width/2*height/2)];
		
		for(int i = width*height + (width/2*height/2); i < width*height + 2*(width/2*height/2); i++)
			i420bytes[i] = yv12bytes[i - (width/2*height/2)];
		
		return i420bytes;
	}
	
	private void YV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height)   
    {        
        System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);  
        System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);  
        System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);    
    }
	
	private void on_recv_sps_pps(byte[] sps_pps, int sps_pps_len)
	{
		byte[] b = new byte[4];
		int sps_head = 0;
		int sps_tail = 0;
		int pps_head = 0;
		
		/*for(int i = 0; i < sps_pps_len; ++i)
		{
			Log.i("Baidu", "SP " + sps_pps[i]);
		}*/
		
		for(int i = 0; i < sps_pps_len - 3; ++i)
		{
			b[0] = sps_pps[i];
			b[1] = sps_pps[i + 1];
			b[2] = sps_pps[i + 2];
			b[3] = sps_pps[i + 3];
			
			if(0 == b[0] && 0 == b[1] && 0 == b[2] && 1 == b[3])
			{
				if(NalTypes.NAL_SPS == (sps_pps[i + 4] & 0x1f))
				{
					sps_head = i + 4;
				}
				else if(NalTypes.NAL_PPS == (sps_pps[i + 4] & 0x1f))
				{
					sps_tail = i - 1;
					pps_head = i + 4;
					
					break;//����00 00 00 01Ҳ����00 00 01�����
				}
			}
			else if(0 == b[0] && 0 == b[1] && 1 == b[2])
			{
				if(0x67 == sps_pps[i + 3])
				{
					sps_head = i + 3;
				}
				else if(0x68 == sps_pps[i + 3])
				{
					sps_tail = i - 1;
					pps_head = i + 3;
					
					break;//����00 00 00 01Ҳ����00 00 01�����
				}
			}
		}
		
		Log.i("Baidu", sps_pps_len + " sps_pps: " + sps_head + ", " + sps_tail + ", " + pps_head);
		
		int sps_len = sps_tail - sps_head + 1;
		byte[] sps = new byte[sps_len];
		System.arraycopy(sps_pps, sps_head, sps, 0, sps_len);
		
		Log.i("Baidu", sps_len + " SPS: " + sps[0] + ", " + sps[1] + ", " + sps[2] + ", " + sps[3] + ", " + sps[4] + ", " + sps[5]);
		
		int pps_len = sps_pps_len - pps_head;
		byte[] pps = new byte[pps_len];
		System.arraycopy(sps_pps, pps_head, pps, 0, pps_len);
		
		Log.i("Baidu", pps_len + " PPS: " + pps[0] + ", " + pps[1] + ", " + pps[2] + ", " + sps[3] + ", " + sps[4] + ", " + sps[5]);
		
		/*if(null != G._mp4_creator)
		{
			long timestamp = System.currentTimeMillis();
			String path = "/sdcard/Download/";
			path += new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date((timestamp)));// - TimeZone.getDefault().getOffset(timestamp)));
			path += ".mp4";
			
			int ret = G._mp4_creator.mp4_create(path, sps, sps_len, pps, pps_len, G.VIDEO_WIDTH, G.VIDEO_HEIGHT, 25, G.AAC_SAMPLE_RATE, 1);
			Log.v("Baidu", "mp4_create " + ret);
		}*/
	}
	
	private long _frame_count = 0;
	public void encode(byte[] data, boolean rotate)
	//public void media_encode(byte[] data, int offset, int length)
	{
		Log.v("Baidu", "enc " + data.length + ", " + GD.VIDEO_WIDTH +  ", " + GD.VIDEO_HEIGHT);
		//if(true) return;
		
		ByteBuffer[] input_buffers = _media_codec.getInputBuffers();
		ByteBuffer[] output_buffers = _media_codec.getOutputBuffers();

		//Log.v("Baidu", "input_bufers " + input_buffers.length + ", " + output_buffers.length);
		
		int input_index = _media_codec.dequeueInputBuffer(-1);
		
		//Log.i("Baidu", "media_encode 2 " + input_index + ", " + length);
		if(0 <= input_index)
		{
			ByteBuffer input_buf = input_buffers[input_index];
			input_buf.clear();
			input_buf.put(data, 0, data.length);
			_media_codec.queueInputBuffer(input_index, 0, data.length, _frame_count++ * 1000000 / _FPS, 0);
		}
		
		data = null;
		
		MediaCodec.BufferInfo buffer_info = new MediaCodec.BufferInfo();
		
		int output_index = _media_codec.dequeueOutputBuffer(buffer_info, 0);//����
		
		//Log.i("Baidu", "media_encode 5 index " + output_index);
		while(0 <= output_index)
		{
			//Log.i("Baidu", "media_encode 6 " + buffer_info.size);
			ByteBuffer output_buf = output_buffers[output_index];
			
			//////////////////////////////////////////////////////
			byte[] frame = new byte[buffer_info.size];
			output_buf.get(frame);
			Log.i("Baidu", buffer_info.size + " bytes " + frame[0] + ", " + frame[1] + ", " + frame[2] + ", " + frame[3] + ", " + frame[4] + ", T:" + NalTypes.get_video_packet_type(frame, frame.length));
			//������������////////////////////////////////////////////////////
			{
				int frame_length = buffer_info.size;
				
				int packet_count = frame_length / GD.MAX_VIDEO_PACKET_SIZE;
				packet_count += (0 == (frame_length % GD.MAX_VIDEO_PACKET_SIZE)) ? 0 : 1;
				
				int[] packet_sizes = new int[packet_count];
				for(int i = 0; i < packet_count; ++i)
				{
					packet_sizes[i] = (frame_length > GD.MAX_VIDEO_PACKET_SIZE) ? GD.MAX_VIDEO_PACKET_SIZE : frame_length;
					frame_length -= packet_sizes[i];
				}
				
				frame_length = buffer_info.size;
				
				if(false)
				{
					Log.v("Baidu", "================== " + frame_length + ", " + packet_count);
					Log.i("Baidu", frame[0] + ", " + frame[1] + ", " + frame[2] + ", " + frame[3] + ", " + (0x1F & frame[4]) + ", " + frame[5]);
				}
				
				//�������õ�������д�����
				synchronized(AVCHardEncoder.class)
				{
					_frame_buffer_list.add(frame);
					_packet_count_in_frame_list.add(packet_count);
					_packet_sizes_in_frame_list.add(packet_sizes);
					
					AVCHardEncoder.class.notify();
				}
				
				_latest_sample_timestamp = System.currentTimeMillis();
			}
			//////////////////////////////////////////////////////
			output_buf.clear();
			//////////////////////////////////////////////////////
			
			_media_codec.releaseOutputBuffer(output_index, false);
			output_index = _media_codec.dequeueOutputBuffer(buffer_info, 0);
		}
	}
	
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
				
				synchronized(AVCHardEncoder.class)
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
						AVCHardEncoder.class.wait(60);
						
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
			
			close_codec();
			
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
				
				synchronized(AVCHardEncoder.class)
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
						AVCHardEncoder.class.wait(60);
						
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
			
			close_codec();
			
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
