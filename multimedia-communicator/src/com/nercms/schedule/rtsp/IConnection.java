package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

public interface IConnection
{
	public void add(OutPacket out);
	
	public void clearSendQueue();
	
	public void start();
	
	public String getId();
	
	public void dispose();
	
	public InetSocketAddress getRemoteAddress();
	
	public SelectableChannel channel();
	
	public INIOHandler getNIOHandler();
	
	public boolean isEmpty();

	public void receive() throws IOException;

	public void send() throws IOException;
	
	public void send(ByteBuffer buffer);
    
    public boolean isConnected();
}
