package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

import android.util.Log;

public abstract class ConnectionImp implements IConnection, INIOHandler {

	/** 发送缓冲区 */
	protected ByteBuffer sendBuf;
	/** 接收缓冲区 */
	protected ByteBuffer receiveBuf;
	/** 发送队列 */
	protected Queue<OutPacket> sendQueue;
	/** 接收队列 */
	protected Queue<InPacket> inQueue;
	/** 端口名称 */
	protected String name;
	/** 远程地址 */
	protected InetSocketAddress remoteAddress;

	/**
	 * 构造函数
	 */
	public ConnectionImp(String connectionID) {
		this.name = connectionID;
		sendQueue = new LinkedList<OutPacket>();
		inQueue = new LinkedList<InPacket>();
		sendBuf = ByteBuffer.allocateDirect(EnginConst.QQ_MAX_PACKET_SIZE);
		receiveBuf = ByteBuffer.allocateDirect(EnginConst.QQ_MAX_PACKET_SIZE);
	}

	public String getId() {
		return name;
	}

	public synchronized void clearSendQueue() {
		sendQueue.clear();
	}
	
	public synchronized Queue<InPacket> getInQueue() {
		return inQueue;
	}

	public synchronized void add(OutPacket packet) {
		sendQueue.offer(packet);
	}
	
	public synchronized boolean isEmpty() {
		return sendQueue.isEmpty();
	}

	public synchronized OutPacket remove() {
		return sendQueue.poll();
	}

	public INIOHandler getNIOHandler() {
		return this;
	}

	public void processError(Exception e) {
		//Log.d("debug","网络出错，关闭连接, id: " + getId());;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}
}
