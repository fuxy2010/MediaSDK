package com.nercms.schedule.ui;

import com.nercms.schedule.misc.GD;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

//RegistrantList、Registrant以及HandlerTool三个类联合
//在一起使用的 主要是方便界面之间消息的传递

public class MessageHandler
{
    private Handler  _msg_handler; //处理消息的句柄
    private int      _msg_what;//消息ID    
    private String   _class_name;//处理消息的类的名称
    
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
	
	//值相等判断
	public boolean value_equal(Handler handle, int what, String name)
	{
		//Log.v("Baidu", _msg_handler + ", " + handle + ", " + name + ", "+ _class_name);
		if(_msg_handler.equals(handle) && what == _msg_what && _class_name.equals(name))
			return true;
		
		return false;
	}
	
	//值相等判断
	public boolean value_equal(MessageHandler message_handler)
	{
		return value_equal(message_handler._msg_handler, message_handler._msg_what, message_handler._class_name);
	}
	
	//通知handler，其关心的事情发生 需要相应的界面处理
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
		
	//清除
	void clear()
	{
		_msg_handler = null;
		_class_name = null;
	}
	
	//得到消息ID
	int get_msg_what()
	{
		return _msg_what;
	}
	
	//处理消息的类的名称
	String get_class_name()
	{
		return (null == _class_name) ? "" : _class_name;
	}
}
