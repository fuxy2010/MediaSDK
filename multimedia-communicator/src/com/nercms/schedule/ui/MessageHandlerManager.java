package com.nercms.schedule.ui;

import android.os.Handler;

public class MessageHandlerManager
{
	//��������
	private volatile static MessageHandlerManager _unique_handler_manager = null;
	private MessageHandlerList _unique_message_handlers = null;
		
	//��ȡHandlerManager�ĵ���ʵ��
	public static MessageHandlerManager get_instance()
	{
		// ���ʵ��,���ǲ����ھͽ���ͬ��������
		if(null == _unique_handler_manager)
		{
			//���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(MessageHandlerManager.class)
			{
				//����˫�ؼ��
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
		//����RegistrantList�ĵ���ʵ��
		if(null == _unique_message_handlers)
		{
			//���������,��ֹ�����߳�ͬʱ����ͬ��������
			synchronized(MessageHandlerList.class)
			{
				//����˫�ؼ��
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
	
	//ע�ᣬ��ע��ʱ������ṩ����
	public void register(Handler h, int what, String class_name)
	{
		_unique_message_handlers.add(h, what, class_name);
	}
	
	//ע����whatΪ��Ϣ��ʶ��Registrant����
	public void unregister(int what)
	{
		_unique_message_handlers.remove(what);
	}
	
	//ע����whatΪ��Ϣ��ʶ��ͬʱ����Ϊclass_name��Registrant����
	public void unregister(int what,String class_name)
	{
		_unique_message_handlers.remove(what,class_name);
	}
	
	//ע������handler
	public void unregister_all()
	{
		_unique_message_handlers.clear();
	}
	
	//��Ϣ����ɷ�Ϊwhat����Ϣ��ʶ������������arg1��arg2��obj
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ����
	public void handle_message(int what)
	{
		_unique_message_handlers.handle_message(what,0,0,null,"");
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1����
	public void handle_message(int what,int arg1)
	{
		_unique_message_handlers.handle_message(what,arg1,0,null,"");
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������obj����
	public void handle_message(int what,Object obj)
	{
		_unique_message_handlers.handle_message(what,0,0,obj,"");
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1��arg2����
	public void handle_message(int what,int arg1,int arg2)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,null,"");
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1��arg2��obj����
	public void handle_message(int what,int arg1,int arg2,int obj)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,obj,"");
	}

	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ�����������Ϣֻ������Ϊclass_name���ദ��
	public void handle_message(int what,String class_name)
	{
		_unique_message_handlers.handle_message(what,0,0,null,class_name);
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1���ݣ������Ϣֻ������Ϊclass_name���ദ��
	public void handle_message(int what,int arg1,String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,0,null,class_name);
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������obj���ݣ������Ϣֻ������Ϊclass_name���ദ��
	public void handle_message(int what,Object obj, String class_name)
	{
		_unique_message_handlers.handle_message(what,0,0,obj,class_name);
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1��arg2���ݣ������Ϣֻ������Ϊclass_name���ദ��
	public void handle_message(int what,int arg1,int arg2, String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,null,class_name);
	}
	
	//֪ͨ��WhatΪ��Ϣ��ʶ����Ϣ������ͬʱ������arg1��arg2��obj���ݣ������Ϣֻ������Ϊclass_name���ദ��
	public void handle_message(int what,int arg1,int arg2,int obj,String class_name)
	{
		_unique_message_handlers.handle_message(what,arg1,arg2,obj,class_name);
	}
}
