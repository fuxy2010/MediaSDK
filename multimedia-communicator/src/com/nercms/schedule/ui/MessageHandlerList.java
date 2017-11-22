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
	
	//��ӦMessageHandler�Ƿ������
	protected synchronized boolean exist(Handler handle, int what, String name)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			if(true == _message_handler_list.get(i).value_equal(handle, what, name))
				return true;
		}
		
		return false;
	}
	
	//��ӦMessageHandler�Ƿ������
	protected synchronized boolean exist(MessageHandler message_handler)
	{
		for(int i = 0; i < _message_handler_list.size(); ++i)
		{
			if(true == _message_handler_list.get(i).value_equal(message_handler))
				return true;
		}
		
		return false;
	}
	
	// ��ӳ�Ա
	public synchronized void add(Handler handle, int what, String class_name)
	{
		if(false == exist(handle, what, class_name))
		{
			_message_handler_list.add(new MessageHandler(handle, what, class_name));
		}
	}

	// ����what ɾ����Ա
	public synchronized void remove(int what)
	{
		remove(what, "");
	}
	
	// ����what������ ɾ����Ա
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

	// ɾ�����г�Ա
	public synchronized void clear()
	{
		while (!_message_handler_list.isEmpty())
		{
			_message_handler_list.get(0).clear();
			_message_handler_list.remove(0);
		}
	}
	
	//��������class_name����Ϣ��ʶwhat��֪ͨ��Ӧ���ദ����Ϣ�����class_nameΪ�գ���ֻƥ��what
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
