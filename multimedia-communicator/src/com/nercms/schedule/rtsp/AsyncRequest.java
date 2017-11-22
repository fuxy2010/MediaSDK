package com.nercms.schedule.rtsp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class AsyncRequest {
	private InetSocketAddress sockAddress;
	private static ThreadPoolExecutor threadPool = null;
	private int ConNumPerNIOThread;
	private byte[] requestBody;
	private boolean isUDP;
	private NIOThread nioThreadArg;
	
	public AsyncRequest()
	{
		nioThreadArg = new NIOThread();
	}
	
	public void setBody(byte[] body)
	{
		this.requestBody = body;
	}
	
	public void setUDP(boolean aIsUDP)
	{
		this.isUDP = aIsUDP;
	}
	
	public AsyncRequest(InetSocketAddress sockAddress)
	{
		nioThreadArg = new NIOThread();
		this.sockAddress = sockAddress;
	}
	
	public static synchronized ThreadPoolExecutor setThreadPoolNum(int aThreadPoolMinNum,int aThreadPoolMaxNum,long keepAliveTime)
	{
		/*ThreadPoolExecutor(int corePoolSize,//线程池维护线程的最少数量
		 					int maximumPoolSize,//线程池维护线程的最大数量
		  					long keepAliveTime,//线程池维护线程所允许的空闲时间
		  					TimeUnit unit,//线程池维护线程所允许的空闲时间的单位
		  					BlockingQueue<Runnable> workQueue,//线程池所使用的缓冲队列
		  					RejectedExecutionHandler handler)//线程池对拒绝任务的处理策略*/
		  					
		if(threadPool == null)
		{
			threadPool = new ThreadPoolExecutor(aThreadPoolMinNum,aThreadPoolMaxNum,keepAliveTime,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(3),new ThreadPoolExecutor.DiscardOldestPolicy()); 
		}
		
		return threadPool;
	}
	
	public void setConNumPerNIOThread(int aConNumPerNIOThread)
	{
		this.ConNumPerNIOThread = aConNumPerNIOThread;
	}
	
	public boolean isCurrRequestFull()
	{
		return ConNumPerNIOThread == nioThreadArg.getConnectionNum();
	}
	
	public InPacket getResponse(int connectionID)
	{
		IConnection connection;
		if(isUDP)
		{
			connection = nioThreadArg.getConnection("UDPConnection"+connectionID);
			
			return ((UDPConnection)connection).getInQueue().remove();
		}else
		{
			connection = nioThreadArg.getConnection("TCPConnection"+connectionID);
			return ((TCPConnection)connection).getInQueue().remove();
		}
	}
	
	public void startAsyn(int connectionID) throws Exception
	{
		ByteBuffer buffer = null;
		OutPacket out = null;
		
		if(requestBody != null)
		{
				buffer = ByteBuffer.wrap(requestBody);
				out = new OutPacket(buffer);
				if(isUDP)
				{
					String id = "UDPConnection"+(connectionID);
					Log.e("startAsyn TCPConnection id", " = "+id);
					IConnection connection = nioThreadArg.newUDPConnection(id, sockAddress, false);
					AsyncRequest.threadPool.execute(nioThreadArg);//将一个NIOThread线程加入线程池
					connection.start();
					
					nioThreadArg.send(id,out,false);
				}else
				{
					String id = "TCPConnection"+(connectionID);
					Log.e("startAsyn TCPConnection id", " = "+id);
					IConnection connection = nioThreadArg.newTCPConnection(id, sockAddress, false);
					AsyncRequest.threadPool.execute(nioThreadArg);//将一个NIOThread线程加入线程池
					connection.start();
					
					nioThreadArg.send(id,out,false);
				}
		}
	}
}
