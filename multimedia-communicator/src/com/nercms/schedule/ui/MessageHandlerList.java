package com.nercms.schedule.ui;

import java.util.ArrayList;

import com.nercms.schedule.misc.GD;

import android.os.Handler;
import android.util.Log;

public class MessageHandlerList
{
	private ArrayList<MessageHandler> _message_handler_list = new ArrayList<MessageHandler>();

	public synchronized void print_handler_num()
	{
		Log.i(GD.LOG_TAG,"handler nums:" + _message_handler_list.size());
	}
	
	//对应MessageHandler是否已添加
	protected synchronized boolean exist(Handler handle, int what, String name)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			if(true == _message_handler_list.get(i).value_equal(handle, what, name))
				return true;
		}
		
		return false;
	}
	
	//对应MessageHandler是否已添加
	protected synchronized boolean exist(MessageHandler message_handler)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			if(true == _message_handler_list.get(i).value_equal(message_handler))
				return true;
		}
		
		return false;
	}
	
	// 添加成员
	public synchronized void add(Handler handle, int what, String class_name)
	{
		if(false == exist(handle, what, class_name))
		{
			_message_handler_list.add(new MessageHandler(handle, what, class_name));
		}
	}

	// 根据what 删除成员
	public synchronized void remove(int what)
	{
		remove(what, "");
	}
	
	// 根据what和类名 删除成员
	public synchronized void remove(int what, String class_name)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			MessageHandler message_handler = _message_handler_list.get(i);
			
			if(what == message_handler.get_msg_what()
				&& ((class_name.equals("") || null == class_name) ? true : message_handler.get_class_name().equals(class_name)))
			{
				message_handler.clear();
				_message_handler_list.remove(message_handler);
				
				i = 0;
			}
		}
	}

	// 删除所有成员
	public synchronized void clear()
	{
		while (!_message_handler_list.isEmpty())
		{
			_message_handler_list.get(0).clear();
			_message_handler_list.remove(0);
		}
	}
	
	//根据类名class_name和消息标识what，通知相应的类处理消息，如果class_name为空，则只匹配what
	public synchronized void handle_message(int what,int arg1,int arg2,Object obj,String class_name)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			MessageHandler message_handler = _message_handler_list.get(i);
			
			if(what == message_handler.get_msg_what() && ((class_name.equals("") || null == class_name) ? true : message_handler.get_class_name().equals(class_name)))
			{
				message_handler.send_message(arg1, arg2, obj);
			}
		}
	}
}
