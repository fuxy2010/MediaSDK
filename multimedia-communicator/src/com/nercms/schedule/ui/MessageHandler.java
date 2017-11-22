package com.nercms.schedule.ui;

import com.nercms.schedule.misc.GD;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

//RegistrantList��Registrant�Լ�HandlerTool����������
//��һ��ʹ�õ� ��Ҫ�Ƿ������֮����Ϣ�Ĵ���

public class MessageHandler
{
    private Handler  _msg_handler; //������Ϣ�ľ��
    private int      _msg_what;//��ϢID    
    private String   _class_name;//������Ϣ���������
    
	MessageHandler(Handler handle, int what, String name)
	{
		this._msg_handler = handle;
		this._msg_what = what;
		this._class_name = name;
	}
	
	@Override
    protected void finalize()
	{
		clear();
    }
	
	//ֵ����ж�
	public boolean value_equal(Handler handle, int what, String name)
	{
		//Log.v("Baidu", _msg_handler + ", " + handle + ", " + name + ", "+ _class_name);
		if(_msg_handler.equals(handle) && what == _msg_what && _class_name.equals(name))
			return true;
		
		return false;
	}
	
	//ֵ����ж�
	public boolean value_equal(MessageHandler message_handler)
	{
		return value_equal(message_handler._msg_handler, message_handler._msg_what, message_handler._class_name);
	}
	
	//֪ͨhandler������ĵ����鷢�� ��Ҫ��Ӧ�Ľ��洦��
	void send_message(int arg1,int arg2,Object obj)
	{
		if(null == _msg_handler)
			return;
		
		Message msg = Message.obtain();
		msg.what = _msg_what;
		msg.obj = obj;
		msg.arg1 = arg1;
		msg.arg2 = arg2;
		_msg_handler.sendMessage(msg);
		
		//Log.i(GD.LOG_TAG,"message what: " + _msg_what);
	}
		
	//���
	void clear()
	{
		_msg_handler = null;
		_class_name = null;
	}
	
	//�õ���ϢID
	int get_msg_what()
	{
		return _msg_what;
	}
	
	//������Ϣ���������
	String get_class_name()
	{
		return (null == _class_name) ? "" : _class_name;
	}
}
