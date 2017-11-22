package com.nercms.schedule.ui;

import android.os.Handler;

public class MessageHandlerManager
{
	//单键处理
	private volatile static MessageHandlerManager _unique_handler_manager = null;
	private MessageHandlerList _unique_message_handlers = null;
		
	//获取HandlerManager的单键实例
	public static MessageHandlerManager get_instance()
	{
		// 检查实例,如是不存在就进入同步代码区
		if(null == _unique_handler_manager)
		{
			//对其进行锁,防止两个线程同时进入同步代码区
			synchronized(MessageHandlerManager.class)
			{
				//必须双重检查
				if(null == _unique_handler_manager)
				{
					_unique_handler_manager = new MessageHandlerManager();
				}
			}
		}
		
		return _unique_handler_manager;
	}
	
	private MessageHandlerManager()
	{
		//创建RegistrantList的单键实例
		if(null == _unique_message_handlers)
		{
			//对其进行锁,防止两个线程同时进入同步代码区
			synchronized(MessageHandlerList.class)
			{
				//必须双重检查
				if(null == _unique_message_handlers)
				{
					_unique_message_handlers = new MessageHandlerList();
				}
			}
		}
	}	
	
	public void print_handler_num()
	{
		_unique_message_handlers.print_handler_num();
	}
	
	//注册，在注册时，最后提供类名
	public void register(Handler h, int what, String class_name)
	{
		_unique_message_handlers.add(h, what, class_name);
	}
	
	//注销已what为消息标识的Registrant对象
	public void unregister(int what)
	{
		_unique_message_handlers.remove(what);
	}
	
	//注销已what为消息标识，同时类名为class_name的Registrant对象
	public void unregister(int what,String class_name)
	{
		_unique_message_handlers.remove(what,class_name);
	}
	
	//注销所有handler
	public void unregister_all()
	{
		_unique_message_handlers.clear();
	}
	
	//消息的组成分为what（消息标识），附带内容arg1，arg2，obj
	
	//通知以What为消息标识的消息发生
	public void handle_message(int what)
	{
		_unique_message_handlers.handle_message(what,0,0,null,"");
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1内容
	public void handle_message(int what,int arg1)
	{
		_unique_message_handlers.handle_message(what,arg1,0,null,"");
	}
	
	//通知以What为消息标识的消息发生，同时附带有obj内容
	public void handle_message(int what,Object obj)
	{
		_unique_message_handlers.handle_message(what,0,0,obj,"");
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1，arg2内容
	public void handle_message(int what,int arg1,int arg2)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,null,"");
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1，arg2，obj内容
	public void handle_message(int what,int arg1,int arg2,int obj)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,obj,"");
	}

	//通知以What为消息标识的消息发生，这个消息只由类名为class_name的类处理
	public void handle_message(int what,String class_name)
	{
		_unique_message_handlers.handle_message(what,0,0,null,class_name);
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1内容，这个消息只由类名为class_name的类处理
	public void handle_message(int what,int arg1,String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,0,null,class_name);
	}
	
	//通知以What为消息标识的消息发生，同时附带有obj内容，这个消息只由类名为class_name的类处理
	public void handle_message(int what,Object obj, String class_name)
	{
		_unique_message_handlers.handle_message(what,0,0,obj,class_name);
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1，arg2内容，这个消息只由类名为class_name的类处理
	public void handle_message(int what,int arg1,int arg2, String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,null,class_name);
	}
	
	//通知以What为消息标识的消息发生，同时附带有arg1，arg2，obj内容，这个消息只由类名为class_name的类处理
	public void handle_message(int what,int arg1,int arg2,int obj,String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,obj,class_name);
	}
}
