/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.nercms.schedule.sip.engine.net;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.io.IOException;

import android.util.Log;

/**
 * RtpSocket implements a RTP socket for receiving and sending RTP packets.
 * <p>
 * RtpSocket is associated to a DatagramSocket that is used to send and/or
 * receive RtpPackets.
 */
public class RtpSocket
{
	private Selector _selector = null;
	private DatagramChannel _channel = null;
	private ByteBuffer _recv_buffer = null;
	private String _remote_ip;
	private int _timeout = 0;
	
	public RtpPacket _rtp_packet = null;
	
	private long _init_timestamp = 0;		
	
	@Override
	protected void finalize()
	{
		_selector = null;
		_channel = null;
		
		_rtp_packet = null;
	}
	
	public DatagramSocket get_sipdroid_socket()
	{
		if(null == _channel)
			return null;
		
		return _channel.socket();
	}
	
	public void set_timeout(int timeout)
	{
		_timeout = timeout;
    	
		synchronized(RtpSocket.class) 
    	{
        	if(null == _channel || 0 == _timeout)
        		return;
        	
        	try
        	{
        		_channel.socket().setSoTimeout(_timeout);
        	}
        	catch (SocketException e2)
        	{
        		e2.printStackTrace();
        	}
    	}		
	}
	
	//Çå¿Õµ×²ã»º´æ
	public void empty(int timeout)
	{
		/*try
		{
			set_timeout(1);			
			while(true == receive())
				;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			set_timeout(timeout);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}*/
		
		while(true == receive());
		
		_recv_buffer.clear();
		
	}
	
	private void init_or_restart_channel()
	{
    	synchronized(RtpSocket.class) 
    	{
        	try
        	{
        		if(null != _channel && 3600000 <= (System.currentTimeMillis() - _init_timestamp))
            	{
            		_channel.close();
            		_channel = null;
            	}
        		
        		if(null == _channel)
            	{
            		_channel = DatagramChannel.open();			
            		_channel.configureBlocking(false);
            		_channel.socket().bind(new InetSocketAddress(0));
            		
            		if(0 != _timeout)
            			_channel.socket().setSoTimeout(_timeout);
            		
            		if(null != _selector)
            		{
            			_channel.register(_selector, SelectionKey.OP_READ);
            		}
            		
            		_recv_buffer.clear();
            		
            		_init_timestamp = System.currentTimeMillis();
            	}
            	
        	}
        	catch (IOException e)
        	{
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}
    	}
	}

	/** Creates a new RTP socket (sender and receiver) */
	public RtpSocket(/*SipdroidSocket datagram_socket, */String remote_ip, int recv_bufer_size)//InetAddress remote_address)
	{
		_recv_buffer = ByteBuffer.allocate(recv_bufer_size);
		_rtp_packet = new RtpPacket(new byte[2048], 0);
		_remote_ip = remote_ip;
		
		_recv_buffer.clear();
				
		try
		{
			_selector = Selector.open();			
			init_or_restart_channel();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//_remote_addr = remote_address;
	}
	
	public boolean receive()
	{
		init_or_restart_channel();
		
		if(null == _selector || null == _channel)
			return false;
		
		try
		{
			if(0 < _selector.select(100))
			{
				Iterator iterator = _selector.selectedKeys().iterator();
				
				//while(true == iterator.hasNext())
				if(true == iterator.hasNext())
				{
					SelectionKey selection_key = (SelectionKey)iterator.next();
					iterator.remove();
					
					if(true == selection_key.isReadable())
					{
						DatagramChannel channel = (DatagramChannel)selection_key.channel();
						
						if(null != channel.receive(_recv_buffer))
						{
							//buffer.flip() == buffer.limit(buffer.position()).position(0);
							_recv_buffer.flip();
							//Log.v("Baidu", "ByteBuffer " + _recv_buffer.limit());
							
							_recv_buffer.get(_rtp_packet.packet, 0, _recv_buffer.limit());
							_rtp_packet.setPacketLength(_recv_buffer.limit());
							
							_recv_buffer.clear();
							
							//Log.v("Baidu", "RS " + _rtp_packet.getSequenceNumber());
							return true;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.v("Baidu", "NUE " + e.toString());
		}
		
		return false;
	}
	
	public void send(int remote_port) throws IOException
	{
		init_or_restart_channel();
		
		if(null == _channel)
			return;
		
		int ret = _channel.send(ByteBuffer.wrap(_rtp_packet.packet, 0, _rtp_packet.getPayloadLength() + 12), new InetSocketAddress(_remote_ip, remote_port));
		
		//Log.v("Baidu", "send ret " + ret);
	}

	public void send(byte[] buf, int len, int remote_port) throws IOException
	{
		init_or_restart_channel();
		
		if(null == _channel)
			return;
		
		//Log.v("NAT", _remote_ip + " : " + remote_port);
		_channel.send(ByteBuffer.wrap(buf, 0, len), new InetSocketAddress(_remote_ip, remote_port));
	}

	/** Closes this socket */
	public void close()
	{
		try {
			_channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.v("Baidu", "rtp socket close");
	}
}
