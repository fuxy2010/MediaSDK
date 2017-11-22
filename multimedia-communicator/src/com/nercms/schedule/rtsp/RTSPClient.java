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

    /** * ����ͨ�� */
    private SocketChannel _socket_channel = null;

    /** ���ͻ����� */
    private final ByteBuffer _send_signal_buf;
    private final ByteBuffer _send_data_buf;

    /** ���ջ����� */
    private final ByteBuffer _recv_buf;

    //private static final int BUFFER_SIZE = 1024;//1024;//2048;//4096;

    /** �˿�ѡ���� */
    private Selector _selector = null;

    private String _remote_url;

    private Status _session_status;

    private String _session_id = null;

    /** �߳��Ƿ�����ı�־ */
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
    
    //��������
  	private volatile static RTSPClient _unique_instance = null;
  	
  	public static RTSPClient get_instance()
	{
		// ���ʵ��,���ǲ����ھͽ���ͬ��������
		if(null == _unique_instance)
		{
			// ���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(RTSPClient.class)
			{
				//����˫�ؼ��
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
    	
    	//��ʼ��������
        _send_signal_buf = ByteBuffer.allocateDirect(GD.RTSP_SEND_BUFFER_SIZE);//BUFFER_SIZE);
        _send_data_buf = ByteBuffer.allocateDirect(GD.RTSP_SEND_BUFFER_SIZE);//BUFFER_SIZE);
        _recv_buf = ByteBuffer.allocateDirect(GD.RTSP_RECV_BUFFER_SIZE);//BUFFER_SIZE);
    }
    
    public static void connection_test(String remote_ip, int remote_port)
    {
    	try
    	{
    		Log.i("RTSP", "TCP���Ӳ��Կ�ʼ");
    		
    		long durance = System.currentTimeMillis();
    		
    		InetSocketAddress addr = new InetSocketAddress(remote_ip, remote_port);
    		
    		SocketChannel socket_channel = SocketChannel.open();
    		
    		socket_channel.configureBlocking(false);
    		socket_channel.connect(addr);
    		
    		while(false == socket_channel.finishConnect())
    		{
    			Thread.sleep(50);
    		}
    		
    		Log.i("RTSP", "TCP���Ӳ��Գɹ�, ��ʱ" + (System.currentTimeMillis() - durance) + "ms");
    		
    		socket_channel.close();
    		socket_channel = null;
    	}
    	catch(Exception e)
    	{
    		Log.i("RTSP", "TCP���Ӳ��Դ���: " + e.toString());
    	}
    }
    
    private boolean create_tcp_connection()
    {
    	try
    	{
    		long durance = System.currentTimeMillis();
    		
    		//���»�ȡ������ַ,��ӦIP��ַ���ܷ����仯�����
    		IpAddress.setLocalIpAddress();
    		Log.i("RTSP", "Local IP: " + IpAddress.localIpAddress);
    		
    		//��ͨ��
    		synchronized(SocketChannel.class)
    		{
    			_socket_channel = SocketChannel.open();
    			_socket_channel.configureBlocking(false);//������
    		}
    		
    		_socket_channel.socket().setSoTimeout(GD.RTSP_SOCKET_CHANNEL_TIME_OUT);//���ö���ʱʱ��
    		_socket_channel.socket().setReceiveBufferSize(GD.RTSP_RECV_BUFFER_SIZE);//���ý��ջ���
    		_socket_channel.socket().setSendBufferSize(GD.RTSP_SEND_BUFFER_SIZE);//���÷��ͻ���
    		_socket_channel.socket().setTcpNoDelay(true);//����Nagle�㷨������������С���ݰ�ƴ��Ϊ�����ݰ����ͣ�ֻ��һ��ACK��ʡ����������������������
    		//_socket_channel.socket().bind(new InetSocketAddress(IpAddress.localIpAddress, 0));//�󶨱��ص�ַ
    		Log.i("RTSP", "Local Port: " + _socket_channel.socket().getLocalPort());
    		
    		Log.i("RTSP", "��ʼ�������� " + GD.DEFAULT_SCHEDULE_SERVER/*SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER)*/ + ":" + GD.RTSP_SERVER_PORT);
    		
    		_socket_channel.connect(new InetSocketAddress(GD.DEFAULT_SCHEDULE_SERVER/*SP.get(SipdroidReceiver.mContext, SP.PREF_SCHEDULE_SERVER, GD.DEFAULT_SCHEDULE_SERVER)*/, GD.RTSP_SERVER_PORT));
    		
    		while(false == _socket_channel.finishConnect())
    		{
    			Thread.sleep(10);
    		}
            
            Log.i("RTSP", "�������ӳɹ� " + (System.currentTimeMillis() - durance) + "ms");
            
            return true;
    	}
    	catch(Exception e)
    	{
    		Log.i("RTSP", "������ʱ����ʧ��1");
    	}
    	
    	return false;
    }
    
    //����ʱ��ʱ����TCP���ӣ��Լӿ�RTSP�Ựʱ����TCP���ӵ��ٶ�
    private long _latest_temporary_tcp_connection_timestamp = 0;
    public void temporary_tcp_connection()
    {
    	//45�����һ��
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
    		
    		//�����ظ������Ự
    		if(true == is_connected() && Status.idle != _session_status)
    			return;
    		
    		Log.i("RTSP", "========= new rtsp session =========");
    		
    		if(false == create_tcp_connection())
    			return;
    		
    		//��տ��ܴ��ڵĲ�������
    		_send_signal_buf.clear();
    		_send_data_buf.clear();
    		_recv_buf.clear();
    		
    		//����selector
        	if(_selector == null)
        	{
        		_selector = Selector.open();
            }
        	
        	_socket_channel.register(_selector,
            						SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE,
            						this);
            
        	Log.i("RTSP", "ע��selector�ɹ� ");
            
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
        	_is_sent = false;//ʹrun�������Խ���״̬��
        	
        	//�����ݴ����������
        	_track_id = -1;//������յ����ݰ�ͷ�Ĺ��ֵ
            _remain_packet_length = 0;//������յ����ݰ�ʣ��δ���Ƶľ��ɳ���
            //byte[] _incomplete_rtsp_header = new byte[4];//���ڴ�ȡ��ȱ��TCP���ݰ�ͷ
            _incomplete_header_length = 0;//δ�����TCP���ݰ�ͷ����
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
    		
    		//�����ظ��Ҷ�
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
    			_session_thread.join();//�ȴ�run����ִ�����
        	
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
        		
        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);//�����߳����ȼ�
            	
                // ������ѭ������
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
                        			//��ʱ���ٽ���RTSP���ӣ�ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
                                	_session_status = Status.play_done;
                        		}
                        		_session_id = null;//�״η���SETUP�����Session�ֶ�
                        		do_setup(TRACK_VIDEO);
                        		break;
                        		
                        	case setup_video_done:
                        		if(4000 < (System.currentTimeMillis() - _describe_timestamp))
                        		{
                        			//��ʱ���ٽ���RTSP���ӣ�ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
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
                        			//��ʱ���ٽ���RTSP���ӣ�ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
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
                        			//��ʱ���ٽ���RTSP���ӣ�ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
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
	                    	//��ʱ�������ʱ��ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
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
	��ʼ�����mark��ָ���һ��Ԫ��֮ǰ�ĵļ�-1��postionΪָ���һ��Ԫ��Ϊ0����Limit�Ǳ���ֵΪbyte[]�ĳ��ȡ�
	���ַ�����Դ��
	public final Buffer clear() {   
	    position = 0;     //����Ϊ0
	    limit = capacity;    //���޺�������ͬ
	    mark = -1;   //ȡ�����
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
	 flip��rewind��������flip���ƶ����޺�λ����ͬ
	 ��������д����ʱ���಻������
	 ��clear����ջ�����*/
    
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
            	_send_data_buf.limit(length);//����bufʵ����Ч����
            }
            
            if(true == is_connected())
            {
                try
                {
                	//Log.i("RTSP", "send data 5");                	
                	SelectionKey key = _socket_channel.keyFor(_selector);
                	
                	int send_len = 0;
                	long timestamp = System.currentTimeMillis();
                	
                	//500���볬ʱ
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

    private void send_signal(byte[] buf)//����
    {
    	if (buf == null || buf.length < 1) {
            return;
        }
        
        synchronized (_send_signal_buf)
        {
            _send_signal_buf.clear();
            _send_signal_buf.put(buf);
            _send_signal_buf.flip();
            _send_signal_buf.limit(buf.length);//����bufʵ����Ч����
        }

        // ���ͳ�ȥ
        try
        {
        	//handle_write();
        	if(true == is_connected())
            {
        		SelectionKey key = _socket_channel.keyFor(_selector);
            	
            	int send_len = 0;
            	long timestamp = System.currentTimeMillis();
            	
            	//2�볬ʱ
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
            	Log.i("RTSP", "ͨ��Ϊ�ջ���δ���� 1");
            }
        	
            _is_sent = true;//ʹrun����������״̬����ר�ĵȴ�������Ӧ�������ظ���������
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
        	Log.i("RTSP", "ͨ��Ϊ�ջ���δ���� 1");
        }
    }
    
    @Override
    public void handle_connect(SelectionKey key) throws IOException
    {
        if(true == is_connected())
        {
            return;
        }
        
        //���SocketChannel������
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

    private long _latest_receive_timestamp = 0;//�ϴε���SocketChannel���յ�ʱ��
    @Override
    public void handle_read(SelectionKey key) throws IOException
    {
        // ������Ϣ
        final byte[] msg = recieve();
        
        _latest_receive_timestamp = System.currentTimeMillis();
        
        if(msg != null)
        {
            handle_rtsp_msg(msg);
        }
        else
        {
            //��߽���Ƶ�ʺ󲻱�cancel key.cancel();
            
            //Log.i("RTSP", "_is_thread_shutdown.set(true) 3");
            //��������ֹͣ�̣߳���RTSPClientThread���� _is_thread_shutdown.set(true);
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
                    	//len��ֵΪ�������Է����ӶϿ�
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
                    	//Log.i("RTSP", "��������Ϊ��");
                    	//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "RTSP�Ự֮TCP�����ж�");
                        return null;
                    }
                }
            }
            catch (final Exception e)
            {
            	Log.i("RTSP", "������Ϣ����:");
            }
        }
        else
        {
        	Log.i("RTSP", "δ����");
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

        // ���select���ش���0�������¼�
        if(0 < n)
        {
        	final Iterator<SelectionKey> iter = _selector.selectedKeys().iterator();
        	
            while(true == iter.hasNext())
            {
                // �õ���һ��Key
                final SelectionKey selection_key = iter.next();
                
                //SelectionKey�е�ready���ϱ�ʾ��selector����Ȥ��ʱ�䣬ֻ����select��ʱ���ɵײ��޸�
                //Ϊ�˱�ʾ�Ѿ�����ready���ϣ�ֻ�ܽ���SelectionKey����ѡ���Key�������Ƴ���remove��
                //�������ͨ���رգ���SelectionKey��ʾͨ����selector�Ĺ�ϵ��������Զ���ᷢ�����رա�ʱ�䣬����ͨ����selector���Ƴ���key.cancel()��
                iter.remove();
                
                handle_selection_key(selection_key);
            }
        }
    }
    
    private void handle_selection_key(SelectionKey key)
    {
    	//��ͨ��selection_key.channel()��ȡ��Ӧ��SocketChannel
    	
    	// ������Ƿ���Ч
        if(false == key.isValid())
        	return;

        // �����¼�
        final IEvent handler = (IEvent) key.attachment();
        
        try
        {
            if (key.isConnectable())
            {
            	handler.handle_connect(key);
            	Log.i("RTSP", "reconnect " + _session_status);
            	
            	if(Status.idle != _session_status && Status.send_teardown != _session_status && Status.teardown_done != _session_status)
            	{
            		//��ʱ���ٽ���RTSP���ӣ�ֱ�ӽ�״̬��Ϊplay_done���Եȴ���RTSPClientThread��һ��ѭ���н���RTSP����
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
            key.cancel();//�Ͽ�����
            
            Log.i("RTSP", "_is_thread_shutdown.set(true) 1");
            
            _is_thread_shutdown.set(true);//��ʱ������������ر����ӣ�����ֹͣ�߳����У�����
        }
    }

    private void shutdown()
    {
        if (is_connected())
        {
            try
            {
                _socket_channel.close();
                Log.i("RTSP", "�ر�ͨ���ɹ�");
            }
            catch (final IOException e)
            {
            	Log.i("RTSP", "�ر�ͨ������:");
            }
            //finally
            {
                _socket_channel = null;
            }
        }
        else
        {
            Log.i("RTSP", "ͨ��Ϊ�ջ���δ���� 2");
        }
    }
    
    private void handle_rtsp_msg(byte[] msg)
    {
    	//Log.i("RTSP", "VR " + (System.currentTimeMillis() - GD._latest_rtsp_video_data_timestamp) + " " + msg.length);
    	//Log.i("RTSP", "AR " + (System.currentTimeMillis() - GD._latest_rtsp_audio_data_timestamp) + " " + msg.length);
    	
    	if('R' == msg[0]
    		&& 'T' == msg[1]
    		&& 'S' == msg[2]
    		&& 'P' == msg[3])//RTSP����
    	{
    		String tmp = new String(msg);        
            //Log.i("RTSP", "RTSP Ack��"+tmp); 
            
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
                _is_sent=false;//���ղ����������ݺ�ʹrun��������״̬��
            }
            else//RTSP��������
            {
                Log.i("RTSP", "RTSP Ack error��" + tmp);
                _session_status = Status.idle;//���½�������
            }
    	}
    	else//ý������
    	{
    		//Log.i("RTSP", "Data " + msg.length);
        	//long timestamp = System.currentTimeMillis();
    		
    		handle_media_data(msg);
        	//Log.i("RTSP", "new packet " + msg.length);        	
        	//Log.i("RTSP", "h " + (System.currentTimeMillis() - timestamp) + "ms " + msg.length);
    	}
    }
    
    //���ݰ��ṹ
    //BYTE0: '$'
    //BYTE1: track_id, 0-video, 2-audio, 4-whiteboard
    //BYTE2: �����ɳ����ֵĸ�8λ
    //BYTE3: �����ɳ����ֵĵ�8λ
    //12 BYTEs: RTSP Server��ӵ�RTP��ͷ
    //12 BYTEs: ��������RTSP Serverǰ��ӵ�RTP��ͷ
    private int _track_id = -1;//������յ����ݰ�ͷ�Ĺ��ֵ
    private int _remain_packet_length = 0;//������յ����ݰ�ʣ��δ���Ƶľ��ɳ���
    private byte[] _incomplete_rtsp_header = new byte[4];//���ڴ�ȡ��ȱ��TCP���ݰ�ͷ
    private int _incomplete_header_length = 0;//δ�����TCP���ݰ�ͷ����
    
    private void handle_media_data(byte[] data)
    {
    	if(null == data || 1 > data.length)
    	{
    		Log.i("RTSP", "error packet " + data.length);
    		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", "�����ش�����" + data.length);
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
    				//�յ����ݰ�ͷ
        			_track_id = GD.byte_2_int(data[pos + 1]);
        			_remain_packet_length = (GD.byte_2_int(data[pos + 2]) << 8) + GD.byte_2_int(data[pos + 3]);
        			//Log.i("RTSP", "1 track " + _track_id + " length " + _remain_packet_length);
        			
        			pos += 4;
        			
        			_incomplete_header_length = 0;
    			}
    			else
    			{
    				//TCP��ͷ��ȫ,�ȸ���һ����
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
    		
    		//�����ȱ��TCP���ݰ�ͷ
    		if(0 < _incomplete_header_length)
    		{
    			System.arraycopy(data, pos, _incomplete_rtsp_header, 4 - _incomplete_header_length, _incomplete_header_length);
    			
    			_track_id = GD.byte_2_int(_incomplete_rtsp_header[1]);
    			_remain_packet_length = (GD.byte_2_int(_incomplete_rtsp_header[2]) << 8) + GD.byte_2_int(_incomplete_rtsp_header[3]);    			    			
    			//Log.i("RTSP", "2 track " + _track_id + " length " + _remain_packet_length);
    			
    			pos += _incomplete_header_length;
    			
    			_incomplete_header_length = 0;
    		}
    		
    		//��������
    		//���θ��Ƶĳ���
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
    byte[][] _recv_media_buf = new byte[3][2048];//ý�建����
    int[] _media_buf_pos = new int[] {0, 0, 0};//ý�建����λ��
    
    private boolean handle_media_data(byte[] data, int pos, int length, int type)
	{
    	if(2 < type || 0 > type)//�������
    		return false;
    	
    	if(length > _recv_media_buf[type].length - _media_buf_pos[type])//���������ɲ���
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
    		
    		//��ȡ��һ�����������ݰ�
    		//���Ƶ����뻺����...
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
        
        //�׸�TRACK��Ӧ��SETUP�����Session�ֶ�
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
