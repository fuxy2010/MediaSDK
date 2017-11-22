package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import android.util.Log;

public class TCPConnection extends ConnectionImp{

	/** 用于通信的channel */
	private final SocketChannel channel;
	/**
	 * true表示远程已经关闭了这个连接
	 */
	private boolean remoteClosed;
	
	/**
	 * 构造一个连接到指定地址的TCPPort. 
	 * 
	 * @param address 连接到的地址.
	 * @throws IOException 端口打开/端口配置/连接到地址出错.
	 */
	public TCPConnection(String id, InetSocketAddress address) throws IOException {
	    super(id);
		channel = SocketChannel.open();
		channel.configureBlocking(false);
        this.remoteAddress = address;
        remoteClosed = false;
	}

    /* (non-Javadoc)
     */
    public void start() {
		try {
            channel.connect(remoteAddress);
		} catch(UnknownHostException e) {
			processError(new Exception("Unknown Host"));
		} catch(UnresolvedAddressException e) {
			processError(new Exception("Unable to resolve server address"));
        } catch (IOException e) {
            processError(e);
        } 
    }
    
	/* (non-Javadoc)
	 */
	public SelectableChannel channel() {
		return channel;
	}

	/* (non-Javadoc)
	 */
	public void receive() throws IOException {
		if(remoteClosed)
			return;
		//接收数据	    
	    int oldPos = receiveBuf.position();
		for (int r = channel.read(receiveBuf); r > 0; r = channel.read(receiveBuf))
		    ;
		
		byte[] tempBuffer = new byte[1024];
		receiveBuf.get(tempBuffer, 0, receiveBuf.position());
		Log.e("receive", " = "+new String(tempBuffer,"UTF-8"));
		
		// 得到当前位置
		int pos = receiveBuf.position();		
		receiveBuf.flip();
		// 检查是否读了0字节，这种情况一般表示远程已经关闭了这个连接		
		if(oldPos == pos) {
		    remoteClosed = true;
		    return;
		}
	    InPacket packet = new InPacket(receiveBuf);
	    inQueue.add(packet);
	    
	    adjustBuffer(pos);
	}
	
	/**
	 * 调整buffer
	 * 
	 * @param pos
	 */
	private void adjustBuffer(int pos) {
	    // 如果0不等于当前pos，说明至少分析了一个包
	    if(receiveBuf.position() > 0) {
	        receiveBuf.compact();  
	        receiveBuf.limit(receiveBuf.capacity());    
	    } else {
	        receiveBuf.limit(receiveBuf.capacity());    
	        receiveBuf.position(pos);
	    }
	}

	/* (non-Javadoc)
	 */
	public void send() throws IOException {
		while (!isEmpty()) {
			sendBuf.clear();
			OutPacket packet = remove();
			channel.write(ByteBuffer.wrap(packet.getBody()));
			// 添加到重发队列
			packet.setTimeout(System.currentTimeMillis() + EnginConst.QQ_TIMEOUT_SEND);
			Log.e("debug","have sended packet - " + packet.toString());	
			}
		}
	
    /* (non-Javadoc)
     */
    public void send(OutPacket packet) {
		try {
			sendBuf.clear();
			channel.write(ByteBuffer.wrap(packet.getBody()));
            Log.d("debug","have sended packet - " + packet.toString());			    
        } catch (Exception e) {
        }
    }
    
    /* (non-Javadoc)
     */
    public void send(ByteBuffer buffer) {
        try {
            channel.write(buffer);
        } catch (IOException e) {
        }
    }
    
    /* (non-Javadoc)
     */
    public void dispose() {
        try {
			channel.close();
		} catch(IOException e) {
		}
    }
    
    /* (non-Javadoc)
     */
    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }
    
    /* (non-Javadoc)
     */
    public void processConnect(SelectionKey sk) throws IOException {
        //完成SocketChannel的连接
        channel.finishConnect();
        while(!channel.isConnected()) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                // 没有什么要做的
            }            
            channel.finishConnect();
        }
    	sk.interestOps(SelectionKey.OP_READ);
    	Log.e("debug","hava connected to server");
    }

    /* (non-Javadoc)
     */
    public void processRead(SelectionKey sk) throws IOException {
        receive();
    }

    /* (non-Javadoc)
     */
    public void processWrite() throws IOException {
        if(isConnected())
            send();
    }

}
