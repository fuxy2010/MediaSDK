package com.nercms.schedule.misc;

import android.content.Intent;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.engine.sipua.ui.RegisterService;
import com.nercms.schedule.ui.MessageHandlerManager;

/**
 * @author Administrator
 *
 */
public class SIPMediaServiceManager
{
	private static Object _lock = new Object();
	
	public static void start(boolean with_registerservice)
	{
		//synchronized(_lock)
		{
			//����Socket
			android.util.Log.v("Baidu", "SIPMediaServiceManager::start 1");
			MediaSocketManager.get_instance().create_socket();
			
			//��������Ƶ�߳�
			android.util.Log.v("Baidu", "SIPMediaServiceManager::start 2");
			MediaThreadManager.get_instance().start_media_thread();
			
			// ����Sipע���Լ������������̨��ע��Service
			android.util.Log.v("Baidu", "SIPMediaServiceManager::start 3");
			if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).isRegistered();
			
			if(true == with_registerservice)
			{
				android.util.Log.v("Baidu", "SIPMediaServiceManager::start 5");
				RegisterService.instance().start(GD.get_global_context());
			}
			
		}
	}
	
	public static void shutdown(boolean with_registerservice)
	{
		android.util.Log.i("Temp", "shutdown 1.0");
		//synchronized(_lock)
		{
			if(GD.is_in_schedule())
			{
				//�ڵ����У���رյ���
				MessageHandlerManager.get_instance().handle_message(GID.MSG_STOP_SCHEDULE,GD.MEDIA_INSTANCE);
			}
			
			android.util.Log.i("Temp", "shutdown 1.1");
			
			//�ر�RegisterService
			if(true == with_registerservice)
			{
				RegisterService.instance().stop(GD.get_global_context());
				android.util.Log.i("Temp", "shutdown 1.2");
			}
			
			//ע��Sipע��
			//fym SipdroidReceiver.pos(true);
			if(false == GD.MQTT_ON) SipdroidReceiver.engine(GD.get_global_context()).halt();
			SipdroidReceiver._unique_sipdroid_engine = null;
			//SipdroidReceiver.reRegister(0);
			android.util.Log.i("Temp", "shutdown 1.3");
			
			//�ر�����Ƶ�߳�
			MediaThreadManager.get_instance().stop_media_thread();
			android.util.Log.i("Temp", "shutdown 1.5");
			
			//�ر�Socket
			MediaSocketManager.get_instance().close_socket();
			android.util.Log.i("Temp", "shutdown 1.6");
		}		
	}

}
