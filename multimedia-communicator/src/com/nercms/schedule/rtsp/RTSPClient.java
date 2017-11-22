package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaThreadManager;
import com.nercms.schedule.network.NetworkStatus;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.stack.net.IpAddress;

import android.util.Log;

public class RTSPClient implements IEvent
{

    private static final String VERSION = " RTSP/1.0\r\n";

    /** * 连接通道 */
    private SocketChannel _socket_channel = null;

    /** 发送缓冲区 */
    private final ByteBuffer _send_signal_buf;
    private final ByteBuffer _send_data_buf;

    /** 接收缓冲区 */
    private final ByteBuffer _recv_buf;

    //private static final int BUFFER_SIZE = 1024;//1024;//2048;//4096;

    /** 端口选择器 */
    private Selector _selector = null;

    private String _remote_url;

    private Status _session_status;

    private String _session_id = null;

    /** 线程是否结束的标志 */
    private AtomicBoolean _is_thread_shutdown = null;
    
    private int _cseq = 1;
    
    private boolean _is_sent;
    
    //private String _track_info;
    private final int TRACK_VIDEO = 0;
    private final int TRACK_AUDIO = 1;
    private final int TRACK_WHITEBOARD = 2;
    
    private long _describe_timestamp = 0;
    private long _setup_timestamp = 0;
    private long _play_timestamp = 0;
        
    private RTSPSessionThread _session_thread = null;

    private enum Status
    {
        //init, options, describe, setup, play, pause, teardown
    	idle,
    	send_describe,
    	describe_done,
    	setup_video_done,
    	setup_audio_done,
    	setup_whiteboard_done,
    	play_done,
    	//pause_done,
    	send_teardown,
    	teardown_done,
    }
    
    //单键处理
  	private volatile static RTSPClient _unique_instance = null;
  	
  	public static RTSPClient get_instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_instance)
		{
			// 对其进行锁,防止两个线程同时进入同步代码区
			synchronized(RTSPClient.class)
			{
				//必须双重检查
				if(null == _unique_instance)
				{
					_unique_instance = new RTSPClient();
				}
			}
		}
		
		return _unique_instance;
	}

    public RTSPClient()//(InetSocketAddress remoteAddress, InetSocketAddress localAddress, String address)
    {
    	Log.i("RTSP", "--------- new rtsp client instance ---------");
    	
    	//初始化缓冲区
        _send_signal_buf = ByteBuffer.allocateDirect(GD.RTSP_SEND_BUFFER_SIZE);//BUFFER_SIZE);
        _send_data_buf = ByteBuffer.allocateDirect(GD.RTSP_SEND_BUFFER_SIZE);//BUFFER_SIZE);
        _recv_buf = ByteBuffer.allocateDirect(GD.RTSP_RECV_BUFFER_SIZE);//BUFFER_SIZE);
    }
    
    public static void connection_test(String remote_ip, int remote_port)
    {
    	try
    	{
    		Log.i("RTSP", "TCP连接测试开始");
    		
    		long durance = System.currentTimeMillis();
    		
    		InetSocketAddress addr = new InetSocketAddress(remote_ip, remote_port);
    		
    		SocketChannel socket_channel = SocketChannel.open();
    		
    		socket_channel.configureBlocking(false);
    		socket_channel.connect(addr);
    		
    		while(false == socket_channel.finishConnect())
    		{
    			Thread.sleep(50);
    		}
    		
    		Log.i("RTSP", "TCP连接测试成功, 耗时" + (System.currentTimeMillis() - durance) + "ms");
    		
    		socket_channel.close();
    		socket_channel = null;
    	}
    	catch(Exception e)
    	{
    		Log.i("RTSP", "TCP连接测试错误: " + e.toString());
    	}
    }
    
    private boolean create_tcp_connection()
    {
    	try
    	{
    		long durance = System.currentTimeMillis();
    		
    		//重新获取本机地址,适应IP地址可能发生变化的情况
    		IpAddress.setLocalIpAddress();
    		Log.i("RTSP", "Local IP: " + IpAddress.localIpAddress);
    		
    		//打开通道
    		synchronized(SocketChannel.class)
    		{
    			_socket_channel = SocketChannel.open();
    			_socket_channel.configureBlocking(false);//非阻塞
    		}
    		
    		_socket_channel.socket().setSoTimeout(GD.RTSP_SOCKET_CHANNEL_TIME_OUT);//设置读超时时间
    		_socket_channel.socket().setReceiveBufferSize(GD.RTSP_RECV_BUFFER_SIZE);//设置接收缓存
    		_socket_channel.socket().setSendBufferSize(GD.RTSP_SEND_BUFFER_SIZE);//设置发送缓存
    		_socket_channel.socket().setTcpNoDelay(true);//禁用Nagle算法，即不将若干小数据包拼接为大数据包发送（只需一个ACK节省带宽），而是立即发送数据
    		//_socket_channel.socket().bind(new InetSocketAddress(IpAddress.localIpAddress, 0));//绑定本地地址
    		Log.i("RTSP", "Local Port: " + _socket_channel.socket().getLocalPort());
    		
    		Log.i("RTSP", "开始建立连接 " + GD.DEFAULT_SCHEDULE_SERVER/*SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER)*/ + ":" + GD.RTSP_SERVER_PORT);
    		
    		_socket_channel.connect(new InetSocketAddress(GD.DEFAULT_SCHEDULE_SERVER/*SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER)*/, GD.RTSP_SERVER_PORT));
    		
    		while(false == _socket_channel.finishConnect())
    		{
    			Thread.sleep(10);
    		}
            
            Log.i("RTSP", "建立连接成功 " + (System.currentTimeMillis() - durance) + "ms");
            
            return true;
    	}
    	catch(Exception e)
    	{
    		Log.i("RTSP", "建立临时连接失败1");
    	}
    	
    	return false;
    }
    
    //空闲时临时建立TCP连接，以加快RTSP会话时进行TCP连接的速度
    private long _latest_temporary_tcp_connection_timestamp = 0;
    public void temporary_tcp_connection()
    {
    	//45秒进行一次
    	if(45000 > (System.currentTimeMillis() - _latest_temporary_tcp_connection_timestamp))
    		return;
    	
    	if(true == create_tcp_connection())
    	{
    		try {
				_socket_channel.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            _socket_channel = null;
    	}
    	
    	_latest_temporary_tcp_connection_timestamp = System.currentTimeMillis();
    }
    
    public void start()
    {
    	try
    	{
    		if(false)//if(false == GD.AUDIO_PROTOCOL.equalsIgnoreCase("tcp") && false == GD.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
    		{
        		return;
    		}
    		
    		//避免重复建立会话
    		if(true == is_connected() && Status.idle != _session_status)
    			return;
    		
    		Log.i("RTSP", "========= new rtsp session =========");
    		
    		if(false == create_tcp_connection())
    			return;
    		
    		//清空可能存在的残余数据
    		_send_signal_buf.clear();
    		_send_data_buf.clear();
    		_recv_buf.clear();
    		
    		//创建selector
        	if(_selector == null)
        	{
        		_selector = Selector.open();
            }
        	
        	_socket_channel.register(_selector,
            						SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE,
            						this);
            
        	Log.i("RTSP", "注册selector成功 ");
            
            this._remote_url = "rtsp://";
            this._remote_url += GD.DEFAULT_SCHEDULE_SERVER;//SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER);
            this._remote_url += ":" + Integer.toString(GD.RTSP_SERVER_PORT) + "/";
            this._remote_url += Long.toString(0xFFFF & GD.get_unique_id(GD.get_global_context()));
            this._remote_url += ".sdp";
            Log.i("RTSP", "URL: " + this._remote_url);
            
            _session_status = Status.idle;
            
            _is_thread_shutdown = new AtomicBoolean(false);
            _is_sent = false;
            
            _session_thread = new RTSPSessionThread();        
            _session_thread.start();
            
            synchronized(_session_status)
        	{
        		_session_status.wait(2000);
        	}
        	
            _describe_timestamp = 0;
            _setup_timestamp = 0;
            _play_timestamp = 0;
            
        	_session_status = Status.send_describe;
        	_is_sent = false;//使run函数可以进入状态机
        	
        	//将数据处理参数归零
        	_track_id = -1;//最近接收的数据包头的轨道值
            _remain_packet_length = 0;//最近接收的数据包剩下未复制的净荷长度
            //byte[] _incomplete_rtsp_header = new byte[4];//用于存取残缺的TCP数据包头
            _incomplete_header_length = 0;//未补齐的TCP数据包头长度
    	}
    	catch(Exception e)
    	{
    	}
    }
    
    public void stop()
    {
    	try
    	{
    		/*if(false == GeneralDefine.AUDIO_PROTOCOL.equalsIgnoreCase("tcp") && false == GeneralDefine.VIDEO_PROTOCOL.equalsIgnoreCase("tcp"))
    		{
        		return;
    		}*/
    		
    		//避免重复挂断
    		if(false == is_connected() && Status.idle == _session_status)
    			return;
    		
    		if(null != _socket_channel
    			&& true == _socket_channel.isConnected()
    			&& true == is_playing())
    		{
        		do_teardown();
        		_session_status = Status.send_teardown;
    		}
        	
        	if(null != _is_thread_shutdown)
        		_is_thread_shutdown.set(true);
        	
    		if(null != _session_thread && true == _session_thread.isAlive())
    			_session_thread.join();//等待run函数执行完毕
        	
        	//_unique_instance = null;
        	_session_thread = null;
        	
        	_selector = null;
        	
        	synchronized(SocketChannel.class)
    		{
        		_socket_channel = null;
    		}
        	
        	_session_status = Status.idle;
    	}
    	catch(Exception e)
    	{
    	}
    }
    
    public boolean is_playing()
    {
    	return (Status.play_done == _session_status
    			&& Status.send_teardown != _session_status
    			&& Status.teardown_done != _session_status);
    }
    
    class RTSPSessionThread extends Thread 
	{
    	public RTSPSessionThread() 
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
    			Log.i("RTSP", "RTSP TCP");
        		
        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);//设置线程优先级
            	
                // 启动主循环流程
                while(false == _is_thread_shutdown.get())
                {
                	if(true == is_connected() && false == _is_sent)
                    {
                    	switch(_session_status)
                        {
                        	case idle:
                        		synchronized(_session_status)
                            	{
                            		_session_status.notify();
                            	}
                        		break;
                        		
                        	case send_describe:
                        		Log.i("RTSP", "start rtsp session");
                        		do_describe();
                        		break;
                        		
                        	case describe_done:
                        		if(3000 < (System.currentTimeMillis() - _describe_timestamp))
                        		{
                        			//此时不再进行RTSP链接，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
                                	_session_status = Status.play_done;
                        		}
                        		_session_id = null;//首次发送SETUP信令不含Session字段
                        		do_setup(TRACK_VIDEO);
                        		break;
                        		
                        	case setup_video_done:
                        		if(4000 < (System.currentTimeMillis() - _describe_timestamp))
                        		{
                        			//此时不再进行RTSP链接，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
                                	_session_status = Status.play_done;
                        		}
                        		
                        		if(null == _session_id || 0 == _session_id.length())
                        		{
                        			Log.i("RTSP", "video setup dose not complete");
                        		}
                        		else
                        		{
                        			do_setup(TRACK_AUDIO);
                        		}
                        		break;
                        		
                        	case setup_audio_done:
                        		if(5000 < (System.currentTimeMillis() - _describe_timestamp))
                        		{
                        			//此时不再进行RTSP链接，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
                                	_session_status = Status.play_done;
                        		}
                        		
                        		if(null == _session_id || 0 == _session_id.length())
                        		{
                        			Log.i("RTSP", "audio setup dose not complete");
                        		}
                        		else
                        		{
                        			do_setup(TRACK_WHITEBOARD);
                        		}
                        		break;
                        		
                        	case setup_whiteboard_done:
                        		if(6000 < (System.currentTimeMillis() - _describe_timestamp))
                        		{
                        			//此时不再进行RTSP链接，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
                                	_session_status = Status.play_done;
                        		}
                        		
                        		if(null == _session_id || 0 == _session_id.length())
                        		{
                        			Log.i("RTSP", "whiteboard setup dose not complete");
                        		}
                        		else
                        		{
                        			do_play();
                        		}
                        		break;
                            
                        	case play_done:
                        		break;
                        		
                        	case send_teardown:
                        		do_teardown();
                        		break;
                        		
                        	default:
                        		break;
                        }
                    	
                    	/*if((Status.send_describe == _session_status && 0 < _describe_timestamp && 3000 < (System.currentTimeMillis() - _describe_timestamp))
	                    	|| (Status.describe_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp))
	                    	|| (Status.setup_video_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp))
	                    	|| (Status.setup_audio_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp)))
	                    {
	                    	//此时信令交互超时，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
	                		Log.i("RTSP", "send signal timeout!");
	                    	_session_status = Status.play_done;
	                    }*/
	                	
	                	if((Status.send_describe == _session_status && 0 < _describe_timestamp && 3000 < (System.currentTimeMillis() - _describe_timestamp)))
	                	{
	                		Log.i("RTSP", "send describe timeout! " + (System.currentTimeMillis() - _describe_timestamp));
	                    	_session_status = Status.play_done;
	                	}
	                	else if ((Status.describe_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp)))
	                	{
	                		Log.i("RTSP", "send video setup timeout! " + (System.currentTimeMillis() - _setup_timestamp));
	                    	_session_status = Status.play_done;
	                	}
	                	else if((Status.setup_video_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp)))
	                	{
	                		Log.i("RTSP", "send audio setup timeout! " + (System.currentTimeMillis() - _setup_timestamp));
	                    	_session_status = Status.play_done;
	                	}
	                	else if((Status.setup_audio_done == _session_status && 0 < _setup_timestamp && 2000 < (System.currentTimeMillis() - _setup_timestamp)))
	                    {
	                		Log.i("RTSP", "send whiteboard setup timeout! " + (System.currentTimeMillis() - _setup_timestamp));
	                    	_session_status = Status.play_done;
	                    }
                    }
                	
                	select();
                	Thread.sleep(3);
                }                    
                shutdown();
    		}
    		catch(Exception e)
    		{
    		}
		}
	}
    
    /*m:mark;
	p:position;
	L:limit;
	初始情况下mark是指向第一个元素之前的的即-1，postion为指向第一个元素为0，而Limit是被赋值为byte[]的长度。
	三种方法的源码
	public final Buffer clear() {   
	    position = 0;     //设置为0
	    limit = capacity;    //极限和容量相同
	    mark = -1;   //取消标记
	    return this;   
	} 

	public final Buffer rewind() {   
	    position = 0;   
	    mark = -1;   
	    return this;   
	} 

	public final Buffer flip() {   
	     limit = position;    
	     position = 0;   
	     mark = -1;   
	     return this;   
	 }
	 flip和rewind的区别是flip会制定极限和位置相同
	 所以我们写数据时不多不少正好
	 而clear则清空缓冲区*/
    
    public void send_data(byte[] buf, int length)//type:0-video, 1-audio
    {
    	//Log.i("RTSP", "send data 1 " + length);
    	
    	synchronized(SocketChannel.class)
		{
    		//Log.i("RTSP", "send data 2");
    		
    		if(null == buf || 0 == buf.length
    			|| false == is_connected()
    			|| false == _session_thread.isAlive()
    			|| Status.play_done != _session_status)
        		return;
    		
    		//Log.i("RTSP", "send data 3");
        	
        	synchronized (_send_data_buf)
            {
            	_send_data_buf.clear();
            	_send_data_buf.put(buf);
            	_send_data_buf.flip();
            	_send_data_buf.limit(length);//设置buf实际有效长度
            }
            
            if(true == is_connected())
            {
                try
                {
                	//Log.i("RTSP", "send data 5");                	
                	SelectionKey key = _socket_channel.keyFor(_selector);
                	
                	int send_len = 0;
                	long timestamp = System.currentTimeMillis();
                	
                	//500毫秒超时
                	while(send_len < length && 500 > (System.currentTimeMillis() - timestamp))
                	{
                		int write_len = _socket_channel.write(_send_data_buf);
                		
                		if(0 == write_len)
                		{
                			//Log.i("RTSP", "write video fail " + length + " " + write_len);              	
                        	key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        	_selector.wakeup();
                		}
                		
                		send_len += write_len;
                		
                		//Log.i("Video", "D " + (System.currentTimeMillis() - timestamp));
                	}
                }
                catch (final IOException e)
                {
                }
            }
            else
            {
            	Log.i("RTSP", "Fail in sending data!");
            }
		}
    }

    private void send_signal(byte[] buf)//发送
    {
    	if (buf == null || buf.length < 1) {
            return;
        }
        
        synchronized (_send_signal_buf)
        {
            _send_signal_buf.clear();
            _send_signal_buf.put(buf);
            _send_signal_buf.flip();
            _send_signal_buf.limit(buf.length);//设置buf实际有效长度
        }

        // 发送出去
        try
        {
        	//handle_write();
        	if(true == is_connected())
            {
        		SelectionKey key = _socket_channel.keyFor(_selector);
            	
            	int send_len = 0;
            	long timestamp = System.currentTimeMillis();
            	
            	//2秒超时
            	while(send_len < buf.length && 2000 > (System.currentTimeMillis() - timestamp))
            	{
            		int write_len = _socket_channel.write(_send_signal_buf);
            		
            		if(0 == write_len)
            		{
            			//Log.i("Video", "write fail " + length + " " + write_len);              	
                    	key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    	_selector.wakeup();
            		}
            		
            		send_len += write_len;
            		
            		//Log.i("Video", "D " + (System.currentTimeMillis() - timestamp));
            	}
            }
        	else
            {
            	Log.i("RTSP", "通道为空或者未连接 1");
            }
        	
            _is_sent = true;//使run函数不进入状态机，专心等待信令相应，避免重复发送信令
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void handle_write() throws IOException
    {
        if(is_connected())
        {
            try
            {
                _socket_channel.write(_send_signal_buf);
                //Log.i("RTSP", "send signal: " + _send_signal_buf.toString());
            }
            catch (final IOException e)
            {
            }
        }
        else
        {
        	Log.i("RTSP", "通道为空或者未连接 1");
        }
    }
    
    @Override
    public void handle_connect(SelectionKey key) throws IOException
    {
        if(true == is_connected())
        {
            return;
        }
        
        //完成SocketChannel的连接
        while(false == _socket_channel.finishConnect())
		{
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }

    @Override
    public void handle_error(Exception e) {
        e.printStackTrace();
    }

    private long _latest_receive_timestamp = 0;//上次调用SocketChannel接收的时戳
    @Override
    public void handle_read(SelectionKey key) throws IOException
    {
        // 接收消息
        final byte[] msg = recieve();
        
        _latest_receive_timestamp = System.currentTimeMillis();
        
        if(msg != null)
        {
            handle_rtsp_msg(msg);
        }
        else
        {
            //提高接收频率后不必cancel key.cancel();
            
            //Log.i("RTSP", "_is_thread_shutdown.set(true) 3");
            //不必立即停止线程，由RTSPClientThread重连 _is_thread_shutdown.set(true);
        }
    }

    private byte[] recieve()
    {
        if(is_connected())
        {
            try
            {
                int len = 0;
                int readBytes = 0;

                synchronized (_recv_buf)
                {
                	_recv_buf.clear();
                	
                    try
                    {
                    	//len = _socket_channel.read(_recv_buf);
                    	//len的值为负表明对方连接断开
                    	while ((len = _socket_channel.read(_recv_buf)) > 0)
                    	{
                        	readBytes += len;
                        	//Log.i("RTSP", "receive len " + len);
                        }
                    }
                    finally
                    {
                    	//Log.i("RTSP", "receive all " + readBytes);
                    	_recv_buf.flip();
                    }
                    
                    if(readBytes > 0)
                    {
                        final byte[] tmp = new byte[readBytes];
                        _recv_buf.get(tmp);
                        return tmp;
                    }
                    else
                    {
                    	//Log.i("RTSP", "接收数据为空");
                    	//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "RTSP会话之TCP连接中断");
                        return null;
                    }
                }
            }
            catch (final Exception e)
            {
            	Log.i("RTSP", "接收消息错误:");
            }
        }
        else
        {
        	Log.i("RTSP", "未连接");
        }
        return null;
    }

    private boolean is_connected()
    {
        return _socket_channel != null && _socket_channel.isConnected();
    }

    private void select()
    {
    	int n = 0;
        try
        {
            if (_selector == null)
            {
                return;
            }
            
            n = _selector.select(100);//(2000);

        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }

        // 如果select返回大于0，处理事件
        if(0 < n)
        {
        	final Iterator<SelectionKey> iter = _selector.selectedKeys().iterator();
        	
            while(true == iter.hasNext())
            {
                // 得到下一个Key
                final SelectionKey selection_key = iter.next();
                
                //SelectionKey中的ready集合表示了selector感兴趣的时间，只有在select的时候由底层修改
                //为了表示已经处理ready集合，只能将该SelectionKey从已选择的Key集合中移除（remove）
                //另外如果通道关闭，因SelectionKey表示通道和selector的关系，所以永远都会发生“关闭”时间，除非通道从selector中移除（key.cancel()）
                iter.remove();
                
                handle_selection_key(selection_key);
            }
        }
    }
    
    private void handle_selection_key(SelectionKey key)
    {
    	//可通过selection_key.channel()获取对应的SocketChannel
    	
    	// 检查其是否还有效
        if(false == key.isValid())
        	return;

        // 处理事件
        final IEvent handler = (IEvent) key.attachment();
        
        try
        {
            if (key.isConnectable())
            {
            	handler.handle_connect(key);
            	Log.i("RTSP", "reconnect " + _session_status);
            	
            	if(Status.idle != _session_status && Status.send_teardown != _session_status && Status.teardown_done != _session_status)
            	{
            		//此时不再进行RTSP链接，直接将状态置为play_done，以等待在RTSPClientThread下一次循环中进行RTSP重连
            		_session_status = Status.play_done;
            	}
            }
            else if(key.isReadable())
            {
                handler.handle_read(key);
            }
            /* just for ServerSocketChannel else if(key.isAcceptable())
            {
            	ServerSocketChannel server = (ServerSocketChannel)key.channel();
            	SocketChannel channel = server.accept();
            	channel.configureBlocking(false);
            	channel.register(_selector, SelectionKey.OP_READ);
            }*/
            else
            {
            	/*if(GD.RTSP_SOCKET_CHANNEL_TIME_OUT <= (System.currentTimeMillis() - _latest_receive_timestamp))
            	{
            		handler.handle_read(selection_key);
            	}*/
            }
        }
        catch(final Exception e)
        {
            handler.handle_error(e);
            key.cancel();//断开连接
            
            Log.i("RTSP", "_is_thread_shutdown.set(true) 1");
            
            _is_thread_shutdown.set(true);//此时服务端已主动关闭连接，必须停止线程运行！！！
        }
    }

    private void shutdown()
    {
        if (is_connected())
        {
            try
            {
                _socket_channel.close();
                Log.i("RTSP", "关闭通道成功");
            }
            catch (final IOException e)
            {
            	Log.i("RTSP", "关闭通道错误:");
            }
            //finally
            {
                _socket_channel = null;
            }
        }
        else
        {
            Log.i("RTSP", "通道为空或者未连接 2");
        }
    }
    
    private void handle_rtsp_msg(byte[] msg)
    {
    	//Log.i("RTSP", "VR " + (System.currentTimeMillis() - GD._latest_rtsp_video_data_timestamp) + " " + msg.length);
    	//Log.i("RTSP", "AR " + (System.currentTimeMillis() - GD._latest_rtsp_audio_data_timestamp) + " " + msg.length);
    	
    	if('R' == msg[0]
    		&& 'T' == msg[1]
    		&& 'S' == msg[2]
    		&& 'P' == msg[3])//RTSP信令
    	{
    		String tmp = new String(msg);        
            //Log.i("RTSP", "RTSP Ack："+tmp); 
            
            if(true == tmp.startsWith("RTSP/1.0 200 OK"))
            {
            	switch (_session_status)
                {
            		case send_describe:
            			_session_status = Status.describe_done;
            			//_track_info=tmp.substring(tmp.indexOf("trackID"));
            			Log.i("RTSP", "describe ok " + (System.currentTimeMillis() - _describe_timestamp));
            			break;
            			
            		case describe_done:
            			//_session_id = tmp.substring(tmp.indexOf("Session: ") + 9, tmp.indexOf("Date:"));
            			int date_pos = tmp.indexOf("Date: ");        			
            			int cache_pos = tmp.indexOf("Cache-Control: ");
            			int session_head_pos = tmp.indexOf("Session: ");
            			int session_tail_pos = (cache_pos < session_head_pos) ? date_pos : ((date_pos < session_head_pos) ? cache_pos : ((date_pos < cache_pos) ? date_pos : cache_pos));
            			
            			_session_id = tmp.substring(session_head_pos + 9, session_tail_pos);//9--"Session: "
            			Log.i("RTSP", "Session id " + _session_id);
            			
            			if(null != _session_id && 0 < _session_id.length())
            			{
            				_session_status = Status.setup_video_done;
            				Log.i("RTSP", "video_setup ok " + (System.currentTimeMillis() - _setup_timestamp));
            			}
            			break;
            			
            		case setup_video_done:
            			//_session_id = tmp.substring(tmp.indexOf("Session: ") + 9, tmp.indexOf("Date:"));        			
            			//Log.i("RTSP", "Audio Setup Session id " + _session_id);
            			
            			if(null != _session_id && 0 < _session_id.length())
            			{
            				_session_status = Status.setup_audio_done;
            				Log.i("RTSP", "audio_setup ok " + (System.currentTimeMillis() - _setup_timestamp));
            			}
            			break;
            			
            		case setup_audio_done:
            			if(null != _session_id && 0 < _session_id.length())
            			{
            				_session_status = Status.setup_whiteboard_done;
            				Log.i("RTSP", "whiteboard_setup ok " + (System.currentTimeMillis() - _setup_timestamp));
            			}
            			break;
            			
            		case setup_whiteboard_done:
            			_session_status = Status.play_done;
            			Log.i("RTSP", "play ok " + (System.currentTimeMillis() - _play_timestamp));
            			Log.i("RTSP", "session " + (System.currentTimeMillis() - _describe_timestamp));
            			Log.i("Video", "Complete RTSP Session " + (System.currentTimeMillis() - _describe_timestamp));
            			GD.update_rtsp_session_complete_timestamp();
            			break;
            			
            		case play_done:
            			//_session_status = Status.pause_done;
            			//Log.i("RTSP", "pause ok");
            			break;
            			
            		case send_teardown:
            			_session_status = Status.idle;
            			Log.i("RTSP", "teardown ok");
            			Log.i("RTSP", "_is_thread_shutdown.set(true) 2");
            			_is_thread_shutdown.set(true);
            			break;
            			
            		default:
            			break;
                }
                _is_sent=false;//接收并处理完数据后使run函数进入状态机
            }
            else//RTSP报错信令
            {
                Log.i("RTSP", "RTSP Ack error：" + tmp);
                _session_status = Status.idle;//重新建立连接
            }
    	}
    	else//媒体数据
    	{
    		//Log.i("RTSP", "Data " + msg.length);
        	//long timestamp = System.currentTimeMillis();
    		
    		handle_media_data(msg);
        	//Log.i("RTSP", "new packet " + msg.length);        	
        	//Log.i("RTSP", "h " + (System.currentTimeMillis() - timestamp) + "ms " + msg.length);
    	}
    }
    
    //数据包结构
    //BYTE0: '$'
    //BYTE1: track_id, 0-video, 2-audio, 4-whiteboard
    //BYTE2: 包净荷长度字的高8位
    //BYTE3: 包净荷长度字的低8位
    //12 BYTEs: RTSP Server添加的RTP包头
    //12 BYTEs: 数据输入RTSP Server前添加的RTP包头
    private int _track_id = -1;//最近接收的数据包头的轨道值
    private int _remain_packet_length = 0;//最近接收的数据包剩下未复制的净荷长度
    private byte[] _incomplete_rtsp_header = new byte[4];//用于存取残缺的TCP数据包头
    private int _incomplete_header_length = 0;//未补齐的TCP数据包头长度
    
    private void handle_media_data(byte[] data)
    {
    	if(null == data || 1 > data.length)
    	{
    		Log.i("RTSP", "error packet " + data.length);
    		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "错误重传数据" + data.length);
    		return;
    	}
    	
    	//Log.i("RTSP", "data " + data.length + "---------");
    	//if(true) return;
    	
    	int pos = 0;
    	
    	//Log.i("RTSP", "process packet 1 " + pos + " " + data.length);
    	while(pos < data.length)
    	{
    		//Log.i("RTSP", "process packet 2 " + pos + " " + data.length);
    		if(0 == _remain_packet_length && '$' == data[pos])
        	{
    			if(4 <= data.length - pos)
    			{
    				//Log.i("RTSP", "process packet 3 " + pos + " " + data.length);
    				//收到数据包头
        			_track_id = GD.byte_2_int(data[pos + 1]);
        			_remain_packet_length = (GD.byte_2_int(data[pos + 2]) << 8) + GD.byte_2_int(data[pos + 3]);
        			//Log.i("RTSP", "1 track " + _track_id + " length " + _remain_packet_length);
        			
        			pos += 4;
        			
        			_incomplete_header_length = 0;
    			}
    			else
    			{
    				//TCP包头不全,先复制一部分
    				System.arraycopy(data, pos, _incomplete_rtsp_header, 0, data.length - pos);
    				_incomplete_header_length = 4 - (data.length - pos);
    				return;
    			}
    			//Log.i("RTSP", "process packet 5 " + pos + " " + data.length);
        	}
    		else if(-1 == _track_id)
    		{
    			//Log.i("RTSP", "process packet 6 " + pos + " " + data.length);
    			Log.i("RTSP", "error data " + data.length);
    			
    			_track_id = -1;
    			_remain_packet_length = 0;
    			_incomplete_header_length = 0;
    			
    			//Log.i("RTSP", "process packet 7 " + pos + " " + data.length);
    			return;
    		}
    		
    		//补齐残缺的TCP数据包头
    		if(0 < _incomplete_header_length)
    		{
    			System.arraycopy(data, pos, _incomplete_rtsp_header, 4 - _incomplete_header_length, _incomplete_header_length);
    			
    			_track_id = GD.byte_2_int(_incomplete_rtsp_header[1]);
    			_remain_packet_length = (GD.byte_2_int(_incomplete_rtsp_header[2]) << 8) + GD.byte_2_int(_incomplete_rtsp_header[3]);    			    			
    			//Log.i("RTSP", "2 track " + _track_id + " length " + _remain_packet_length);
    			
    			pos += _incomplete_header_length;
    			
    			_incomplete_header_length = 0;
    		}
    		
    		//复制数据
    		//本次复制的长度
    		int copy_length = (_remain_packet_length > data.length - pos) ? (data.length - pos) : _remain_packet_length;
    		
    		//Log.i("RTSP", "process packet 8 " + pos + " " + data.length);
    		
    		if(false == handle_media_data(data, pos, copy_length, (_track_id / 2)))
    		{
    			_track_id = -1;
    			Log.i("RTSP", "data copy fail");
    			//Log.i("RTSP", "process packet 9 " + pos + " " + data.length);
    			return;
    		}
    		
    		pos += copy_length;
    	}
    	//Log.i("RTSP", "process packet 10 " + pos + " " + data.length);
    }
    
    //0-video, 1-audio, 2-whiteboard
    byte[][] _recv_media_buf = new byte[3][2048];//媒体缓冲区
    int[] _media_buf_pos = new int[] {0, 0, 0};//媒体缓冲区位置
    
    private boolean handle_media_data(byte[] data, int pos, int length, int type)
	{
    	if(2 < type || 0 > type)//轨道错误
    		return false;
    	
    	if(length > _recv_media_buf[type].length - _media_buf_pos[type])//缓冲区容纳不下
    	{
    		return false;
    	}
    	
    	/*try {
			Log.i("RTSP", "RB " + _socket_channel.socket().getReceiveBufferSize() + ", SB " + _socket_channel.socket().getSendBufferSize());
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    	
    	System.arraycopy(data, pos, _recv_media_buf[type], _media_buf_pos[type], length);
    	
    	_media_buf_pos[type] += length;    	
    	_remain_packet_length -= length;
    	
    	if(0 >= _remain_packet_length)
    	{
    		if(0 == _media_buf_pos[type])
    			return false;
    		
    		//读取到一个完整的数据包
    		//复制到解码缓冲区...
    		//Log.i("RTSP", "data " + type + " " + _media_buf_pos[type]);
    		
    		//int sequence = (GD.byte2int(_recv_media_buf[type][2]) << 8) + GD.byte2int(_recv_media_buf[type][3]);
    		//Log.i("RTSP", "t " + type + " s " + sequence + " " + _media_buf_pos[type]);
    		
    		if(0 == type)
    		{
    			GD._latest_rtsp_video_data_timestamp = System.currentTimeMillis();
    			GD.update_recv_video_timestamp();
    			
    			//Log.v("Video", "V");
    			//Log.i("RTSP", "vs " + sequence + " " + _media_buf_pos[type]);
    			//Log.i("RTSP", "V " + _media_buf_pos[type]);
    			//Log.i("Video", "TR");
    			if(true == GD.NO_RENDERING_DECODE_VIDEO)
    				return true;
    			
    			MediaThreadManager.get_instance().add_video_packetx(_recv_media_buf[type],  _media_buf_pos[type]);
    		}
    		else if(1 == type)
    		{
    			GD._latest_rtsp_audio_data_timestamp = System.currentTimeMillis();
    			GD.update_recv_audio_timestamp();
    			
    			//Log.v("RTSP", "a " + _media_buf_pos[type]);
    			//Log.i("RTSP", "as " + sequence + " " + _media_buf_pos[type]);
    			//Log.v("RTSP", "RA 0 " + _media_buf_pos[type]);
    			if(true == GD.TCP_AUDIO_RECV)
    			{
    				MediaThreadManager.get_instance().self_loop_audio_decode(_recv_media_buf[type], _media_buf_pos[type]);
    			}
    		}
    		else if(2 == type)
    		{
    			String msg = new String(_recv_media_buf[type]);
    			msg = msg.substring(msg.indexOf("{\""), msg.lastIndexOf('}') + 1);
    			
    			//Log.i("Temp", "------ " + msg);
    			GD.parse_schedule_notifty_message(msg);
    		}
    		
    		_media_buf_pos[type] = 0;
    	}
    	
    	return true;
	}
    
    private void do_describe()
    {
    	/*
    	 DESCRIBE rtsp://10.10.10.15:1554/11.sdp RTSP/1.0
    	 CSeq: 1
    	 Accept: application/sdp
    	 User-agent: (null)
    	 */
    	_describe_timestamp = System.currentTimeMillis();
    	
    	GD.update_rtsp_session_start_timestamp();
    	
        StringBuilder sb = new StringBuilder();
        sb.append("DESCRIBE ");
        sb.append(this._remote_url);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\nAccept: application/sdp\r\nUser-agent: (null)\r\n\r\n");

        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send DESCRIBE: ");// + sb.toString());
    }
    
    private void do_setup(int track)
    {
    	/*SETUP rtsp://10.10.10.15:1554/11.sdp/trackID=0 RTSP/1.0
    	CSeq: 2
    	Transport: RTP/AVP/TCP;unicast;interleaved=0-1
    	User-agent: (null)*/
    		
    	_setup_timestamp = System.currentTimeMillis();
    	
        StringBuilder sb = new StringBuilder();
        sb.append("SETUP ");
        sb.append(this._remote_url);
        sb.append("/trackID=");
        sb.append(track);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\n");
        
        //首个TRACK对应的SETUP信令不含Session字段
        if(null != _session_id && 0 < _session_id.length())
        {
        	sb.append("Session: ");
            sb.append(_session_id);
        }
        
        sb.append("Transport: RTP/AVP/TCP;unicast;interleaved=");
        sb.append(2 * track);
        sb.append("-");
        sb.append(2 * track + 1);
        sb.append("\r\nUser-agent: (null)\r\n\r\n");
        
        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send SETUP: ");// + sb.toString());
    }
    
    private void do_play()
    {
    	/*PLAY rtsp://10.10.10.15:1554/11.sdp RTSP/1.0
    	CSeq: 5
    	Session: 35536559421637
    	Range: npt=0.0-
    	x-prebuffer: maxtime=3.0
    	User-agent: (null)*/
    		
    	_play_timestamp = System.currentTimeMillis();
    	
        StringBuilder sb = new StringBuilder();
        sb.append("PLAY ");
        sb.append(this._remote_url);
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\nSession: ");
        sb.append(_session_id);
        sb.append("Range: npt=0.0-\r\nx-prebuffer: maxtime=3.0\r\nUser-agent: (null)\r\n\r\n");
        
        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send PLAY");

    }

    private void do_teardown()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TEARDOWN ");
        sb.append(this._remote_url);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\n");
        sb.append("User-Agent: RealMedia Player HelixDNAClient/10.0.0.11279 (win32)\r\n");
        sb.append("Session: ");
        sb.append(_session_id);
        sb.append("\r\n");
        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send TEARDOWN: ");// + sb.toString());
    }
    
    private void do_option()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("OPTIONS ");
        sb.append(this._remote_url.substring(0, _remote_url.lastIndexOf("/")));
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\n");
        sb.append("\r\n");
        
        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send Option");
    }
    
    private void do_pause() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAUSE ");
        sb.append(this._remote_url);
        sb.append("/");
        sb.append(VERSION);
        sb.append("Cseq: ");
        sb.append(_cseq++);
        sb.append("\r\n");
        sb.append("Session: ");
        sb.append(_session_id);
        sb.append("\r\n");
        
        send_signal(sb.toString().getBytes());
        Log.i("RTSP", "Send Pause");
    }
}
