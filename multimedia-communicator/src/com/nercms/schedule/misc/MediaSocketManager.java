package com.nercms.schedule.misc;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.util.Log;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.sip.engine.net.RtpSocket;
import com.nercms.schedule.sip.engine.net.SipdroidSocket;
import com.nercms.schedule.sip.engine.sipua.SP;

public class MediaSocketManager
{
	//单键处理
	private volatile static MediaSocketManager _unique_instance = null;
	
	//SOCKET
	private RtpSocket _audio_send_socket = null;//发送音频流socket
	private RtpSocket _internal_audio_send_socket = null;//发送音频流socket，RTSP内环专用
	private RtpSocket _audio_recv_socket = null;//接收音频流socket
	private RtpSocket _video_send_socket = null;//发送视频流socket
	private RtpSocket _video_recv_socket = null;//接收视频流socket
	
	//获取singleton实例
	public static MediaSocketManager get_instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_instance)
		{
			//对其进行锁,防止两个线程同时进入同步代码区
			synchronized(MediaThreadManager.class)
			{
				//必须双重检查
				if(null == _unique_instance)
				{
					_unique_instance = new MediaSocketManager();
				}
			}
		}
		
		return _unique_instance;
	}
	
	private MediaSocketManager()
	{
	}
	
	public void create_socket()
	{
		String server_ip = GD.DEFAULT_SCHEDULE_SERVER;//SP.get(GD.get_global_context(),SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER);
		
		if(true == GD.sip_audio_over_udp())//if(true == GD.AUDIO_PROTOCOL.equalsIgnoreCase("udp"))
		{
			if(null == _audio_send_socket)//audio_send
			{
				//Log.i("SOCKET","_audio_send_socket null");
				try 
				{
					//自动分配端口
					_audio_send_socket = new RtpSocket(/*new SipdroidSocket(0), */server_ip, GD.NIO_UDP_AUDIO_RECV_BUFFER_SIZE);//InetAddress.getByName(server_ip));
					//_audio_send_socket.get_sipdroid_socket().setSendBufferSize(GD.UDP_AUDIO_SEND_BUFFER_SIZE);
					
					//提前设置RTP包头的固定值
					_audio_send_socket._rtp_packet.setPayloadType(0);//数据类型为AMR
					_audio_send_socket._rtp_packet.setSscr(GD.get_unique_id(GD.get_global_context()));
					_audio_send_socket._rtp_packet.setMarker(true);
				} 
				catch(Exception e)//(SocketException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _audio_send_socket error: SocketException " + e.toString());
				}
				/*catch (UnknownHostException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _audio_send_socket error: UnknownHostException " + e.toString());
				}*/
			}
		}
		
		//TCP接收需要使用_internal_audio_send_socket
		//服务器双路发送需要使用_audio_recv_socket
		if(true)
		{
			if(null == _internal_audio_send_socket)//audio_send
			{
				//Log.i("SOCKET","_audio_send_socket null");
				try 
				{
					//自动分配端口
					_internal_audio_send_socket = new RtpSocket(/*new SipdroidSocket(0), */"127.0.0.1", GD.NIO_UDP_AUDIO_RECV_BUFFER_SIZE);//InetAddress.getByName("127.0.0.1"));
					//_internal_audio_send_socket.get_sipdroid_socket().setSendBufferSize(GD.UDP_AUDIO_SEND_BUFFER_SIZE);
					
					//提前设置RTP包头的固定值
					//_internal_audio_send_socket._rtp_packet.setPayloadType(0);//数据类型为AMR
					//_internal_audio_send_socket._rtp_packet.setSscr(GD.get_imsi(GD.get_global_context()));
					//_internal_audio_send_socket._rtp_packet.setMarker(true);
				} 
				catch(Exception e)//(SocketException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _internal_audio_send_socket error: SocketException " + e.toString());
				}
				/*catch (UnknownHostException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _internal_audio_send_socket error: UnknownHostException " + e.toString());
				}*/
			}
			
			if(null == _audio_recv_socket)//audio_recv
			{
				//Log.i("SOCKET","_audio_recv_socket null");
				try 
				{
					//自动分配端口
					_audio_recv_socket = new RtpSocket(/*new SipdroidSocket(0), */server_ip, GD.NIO_UDP_AUDIO_RECV_BUFFER_SIZE);//InetAddress.getByName(server_ip));
					//_audio_recv_socket.get_sipdroid_socket().setReceiveBufferSize(GD.UDP_AUDIO_RECV_BUFFER_SIZE);
					_audio_recv_socket.set_timeout(GD.UDP_AUDIO_RECV_SOCKET_TIME_OUT);//设置超时时间
				} 
				catch(Exception e)//(SocketException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _audio_recv_socket error: SocketException " + e.toString());
				}
				/*catch (UnknownHostException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _audio_recv_socket error: UnknownHostException " + e.toString());
				}*/
			}
		}
		
		if(true == GD.VIDEO_PROTOCOL.equalsIgnoreCase("udp"))
		{
			if(null == _video_send_socket)//video_send
			{
				//Log.i("SOCKET","_video_send_socket null");
				try 
				{
					//自动分配端口
					_video_send_socket = new RtpSocket(/*new SipdroidSocket(0), */server_ip, GD.NIO_UDP_VIDEO_RECV_BUFFER_SIZE);//InetAddress.getByName(server_ip));
					//_video_send_socket.get_sipdroid_socket().setSendBufferSize(GD.UDP_VIDEO_SEND_BUFFER_SIZE);
					
					_video_send_socket._rtp_packet.setPayloadType(2);//数据类型为H.264
					_video_send_socket._rtp_packet.setSscr(GD.get_unique_id(GD.get_global_context()));//SP.get(GD.get_global_context(),SP.PREF_ID_BY_IMSI, 0L));
				} 
				catch(Exception e)//(SocketException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _video_send_socket error: SocketException " + e.toString());
				}
				/*catch (UnknownHostException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _video_send_socket error: UnknownHostException " + e.toString());
				}*/
			}
			
			if(null == _video_recv_socket)//video_recv
			{
				//Log.i("SOCKET","_video_recv_socket null");
				try 
				{
					//自动分配端口
					_video_recv_socket = new RtpSocket(/*new SipdroidSocket(0), */server_ip, GD.NIO_UDP_VIDEO_RECV_BUFFER_SIZE);//InetAddress.getByName(server_ip));
					//_video_recv_socket.get_sipdroid_socket().setReceiveBufferSize(GD.UDP_VIDEO_RECV_BUFFER_SIZE);
					_video_recv_socket.set_timeout(GD.UDP_VIDEO_RECV_SOCKET_TIME_OUT);//设置超时时间
				} 
				catch(Exception e)//(SocketException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _video_recv_socket error: SocketException " + e.toString());
				}
				/*catch (UnknownHostException e) 
				{
					//e.printStackTrace();
					Log.v("SOCKET", "create _video_recv_socket error: UnknownHostException " + e.toString());
				}*/
			}
		}
	}
	
	public void close_socket()
	{
		if(null != _audio_send_socket)
		{
			_audio_send_socket.close();
		}
		
		if(null != _internal_audio_send_socket)
		{
			_internal_audio_send_socket.close();
		}
		
		if(null != _audio_recv_socket)
		{
			_audio_recv_socket.close();
		}
		
		if(null != _video_send_socket)
		{
			_video_send_socket.close();
		}
		
		if(null != _video_recv_socket)
		{
			_video_recv_socket.close();
		}
		
		_audio_send_socket = null;
		_internal_audio_send_socket = null;
		_audio_recv_socket = null;
		_video_send_socket = null;
		_video_recv_socket = null;
	}
	
	public RtpSocket get_audio_send_socket()
	{
		return _audio_send_socket;
	}
	
	public RtpSocket get_internal_audio_send_socket()
	{
		return _internal_audio_send_socket;
	}
	
	public RtpSocket get_audio_recv_socket()
	{
		return _audio_recv_socket;
	}
	
	public RtpSocket get_video_send_socket()
	{
		return _video_send_socket;
	}
	
	public RtpSocket get_video_recv_socket()
	{
		return _video_recv_socket;
	}

}
