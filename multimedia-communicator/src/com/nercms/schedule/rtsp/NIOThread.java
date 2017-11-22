package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

import android.util.Log;

public class NIOThread implements Runnable {
	/** 线程是否结束的标志 */
	protected boolean shutdown = false;
	/** 端口选择器 */
	protected Selector selector;
	
	// Connection列表
	private List<IConnection> ports;
	
	// 连接释放请求
	private Queue<Object> disposeQueue;
	
	// 新连接请求
	private List<Object> newConnections;
	
	/**
	 * 构造一个Porter.
	 */
	public NIOThread() {
	    ports = new ArrayList<IConnection>();
	    newConnections = new Vector<Object>();
	    disposeQueue = new LinkedList<Object>();
	    
		registry = new HashMap<String, IConnection>();
		references = new HashMap<IConnection, Integer>();

	    // 创建新的Selector
		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 注册一个IConnection到NIOThread中
	 * 
	 * @param port
	 * 		IConnection实现
	 * @throws ClosedChannelException
	 * 		如果注册失败
	 */
	public void register(IConnection port) throws ClosedChannelException {
	    SelectableChannel channel = port.channel();
	    if(channel instanceof SocketChannel)
	    {
		    channel.register(selector, SelectionKey.OP_CONNECT, port.getNIOHandler());
	    }
	    else if(channel instanceof DatagramChannel)
	    {
		    channel.register(selector, SelectionKey.OP_READ, port.getNIOHandler());
	    }
	    if(!ports.contains(port))
	        ports.add(port);
	}
	
	/**
	 * 以指定的操作注册channel
	 * 
	 * @param port
	 * @param ops
	 * @throws ClosedChannelException
	 */
	public void register(IConnection port, int ops) throws ClosedChannelException {
	    SelectableChannel channel = port.channel();
	    if(channel instanceof SocketChannel)
		    channel.register(selector, ops, port.getNIOHandler());
	    else if(channel instanceof DatagramChannel)
		    channel.register(selector, ops, port.getNIOHandler());
	    if(!ports.contains(port))
	        ports.add(port);
	}
	
	private void deregister(IConnection port) {
		if(port == null)
			return;
		
	    if(!ports.remove(port))
	    	return;
    	SelectionKey key = port.channel().keyFor(selector);
    	if(key != null)
    		key.cancel();
        port.dispose();
	}
	
	/**
	 * 发送错误事件到所有IConnection
	 * 
	 * @param e
	 * 		包含错误信息的Exception
	 */
	private void dispatchErrorToAll(Exception e) {
		for(IConnection port : ports)
			port.getNIOHandler().processError(e);
	}
	
	/**
	 * 通知所有IConnection发送包
	 * @throws IOException
	 */
	private void notifySend() {
	    int size = ports.size();
	    for(int i = 0; i < size; i++) {
	        INIOHandler handler = null;
	        try {
		        handler = (ports.get(i)).getNIOHandler();
                handler.processWrite();
            } catch (IOException e) {
	            //handler.processError(e);
            } catch (IndexOutOfBoundsException e) {                
            }
	    }
	}
	
	/**
	 * 不断运转维护所有注册的IConnection对象.
	 * 通过调用它们的几个函数分别做到清空发送队列/填充接收队列/维护队列的功能.
	 * @see IConnection#send(ByteBuffer)
	 * @see IConnection#receive(ByteBuffer)
	 * @see IConnection#maintain()
	 */
	public void run()
	{
		int n = 0;
		while(!shutdown)
		{	
	        // do select
            try
            {
            	if(selector != null)
            		n = selector.select(3000);
            	
                // 如果要shutdown，关闭selector退出
                if (shutdown)
            	{
                    selector.close();
                	break;			        
                }
            }
            catch (IOException e)
            {
	            dispatchErrorToAll(e);
            } 
            
            // 处理连接释放请求
            processDisposeQueue();
            
		    // 如果select返回大于0，处理事件
		    if(n > 0) {
		        for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
					// 得到下一个Key
					SelectionKey sk = i.next();
					i.remove();
					// 检查其是否还有效
	                if(!sk.isValid())
	                    continue;

					// 处理
					INIOHandler handler = (INIOHandler)sk.attachment();
		            try {
                        if(sk.isConnectable())
                        {
                            handler.processConnect(sk);
                        }
                        else if (sk.isReadable())
                        {
                            handler.processRead(sk);
                        }
                    } catch (IOException e) {
        	            handler.processError(e);
                    } catch (RuntimeException e) {
    	            }
		        }
		        
		        n = 0;
		    }
		    		
		    checkNewConnection();
		    notifySend();		    
		}
	    
        selector = null;
        shutdown = false;
	}
	
	/**
	 * 添加释放请求
	 * 
	 * @param p
	 */
	public void addDisposeRequest(IConnection p) {
		synchronized(disposeQueue) {
			disposeQueue.offer(p);
		}
	}
	
    /**
     * 检查是否有新连接要加入
     */
    private void checkNewConnection() {	
        while(!newConnections.isEmpty()) {
            Object handler = newConnections.remove(0);
            if(handler instanceof IConnection) {
		        try {
	                register((IConnection)handler);
	            } catch (ClosedChannelException e1) {
	            }
	        }            
        }
    }
    
    public int getConnectionNum()
    {
      return newConnections.size();
    }
    /**
     * 处理连接释放请求
     */
    private void processDisposeQueue() {
    	synchronized(disposeQueue) {
    		while(!disposeQueue.isEmpty()) {
    			Object obj = disposeQueue.poll();
    			if(obj instanceof IConnection)
    				deregister((IConnection)obj);
    		}
    	}
    }

    /**
     * 关闭IConnection
     */
    public void shutdown() {
	    if(selector != null) {
		    shutdown = true;
	        selector.wakeup();	        
	    }
    }
    
    /**
     * 唤醒selector
     */
    public void wakeup() {
        selector.wakeup();
    }
    
    /**
     * 唤醒selector然后注册这个proxy
     * 
     * @param proxy
     */
    public void wakeup(Object handler) {
        newConnections.add(handler);
        selector.wakeup();
    }
    
    /************************************************************************/

	// IPort映射器，key是名称，value是IPort对象
	private Map<String, IConnection> registry;
	// 引用计数
	private Map<IConnection, Integer> references;
	
	public void start() {
		this.start();
	}
	
	public synchronized List<IConnection> getConnections() {
		List<IConnection> ret = new ArrayList<IConnection>();
		ret.addAll(registry.values());
		return ret;
	}
	
	public synchronized boolean hasConnection(String id) {
		return registry.containsKey(id);
	}
	
	public synchronized void release(IConnection con) {
		if(con == null)
			return;
		
		Integer reference = references.get(con);
		if(reference == null)
			return;
		reference--;
		if(reference <= 0) {
			references.remove(con);
			registry.remove(con.getId());
			this.addDisposeRequest((IConnection)con);
		} else
			references.put(con, reference);
	}
	
	public synchronized void release(String id) {
		IConnection con = getConnection(id);
		release(con);
	}
	
	public synchronized IConnection getConnection(String id) {
		return registry.get(id);
	}
	
	/**
	 * 检查是否存在一个到指定远程地址的连接
	 * 
	 * @param address
	 * @return
	 */
	public synchronized IConnection getConnection(InetSocketAddress address) {
		for(IConnection port : registry.values()) {
	        if(port.getRemoteAddress().equals(address))
	            return port;
		}
	    return null;
	}
	
	/**
	 * 增加连接的引用计数
	 * 
	 * @param id
	 */
	private synchronized void increaseReference(IConnection con) {
		Integer i = references.get(con);
		if(i != null) {
			i++;
			references.put(con, i);			
		}
	}

	/**
	 * 使用指定的port发送一个包
	 * 
	 * @param name
	 * 		port name
	 * @param packet
	 * 		OutPacket子类
	 * @param keepSent
	 * 		true表示保存发出的包，这种需要是因为有些协议的返回包没有什么可用信息，需要使用发出包来
	 *      触发事件
	 * @return
	 * 		true表示包发送成功，false表示失败
	 */
	public void send(String id, OutPacket packet, boolean keepSent) {
		IConnection port = getConnection(id);
		if(port != null) {
			port.add(packet);
		} 
	}

	public synchronized IConnection newUDPConnection(String id, InetSocketAddress server, boolean start) {
        if(hasConnection(id)) {
        	IConnection con = getConnection(id);
        	increaseReference(con);
        }
        
        UDPConnection port;
		try {
			port = new UDPConnection(id, server);
		} catch(IOException e) {
			return null;
		}

        registry.put(id, port);
        references.put(port, 1);
        wakeup(port);
        if(start)
            port.start();
        return port;
	}

	public synchronized IConnection newTCPConnection(String id, InetSocketAddress server, boolean start) {
        if(hasConnection(id)) {
        	IConnection con = getConnection(id);
        	increaseReference(con);
        }
        
        TCPConnection port;
		try {
			port = new TCPConnection(id, server);
		} catch(IOException e) {
			return null;
		}
        registry.put(id, port);
        references.put(port, 1);
        wakeup(port);
        if(start)
            port.start();
        return port;
	}

	/* (non-Javadoc)
	 */
	public synchronized void dispose() {
		new Thread() {
			@Override
			public void run() {
				shutdown();
				registry.clear();
				references.clear();
				registry = null;
				references = null;
			}
		}.start();
	}

	public void flush() {
		wakeup();
	}
	/***********************************************************************************/
}
