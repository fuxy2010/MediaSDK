package com.nercms.schedule.network;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.sip.engine.sipua.SP;
import com.nercms.schedule.sip.engine.sipua.ui.SipdroidReceiver;
import com.nercms.schedule.sip.stack.sdp.MediaDescriptor;
import com.nercms.schedule.ui.MessageHandlerManager;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class OnRecvMQTTMsg
{
	private volatile static OnRecvMQTTMsg _unique_instance = null;
	
	private static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
	private static JsonParser parser = new JsonParser();
	
	public static OnRecvMQTTMsg instance()
	{
		if(null == _unique_instance)
		{
			synchronized(OnRecvMQTTMsg.class)
			{
				if(null == _unique_instance)
				{
					_unique_instance = new OnRecvMQTTMsg();
				}
			}
		}
		
		return _unique_instance;
	}
	
	private OnRecvMQTTMsg()
	{
	}
	
	public void on_recv_msg(String topic, String msg)
	{
		Log.i("MQTT", "Recv <-: " + msg + " on topic: " + topic.toString());
		
		if(false == topic.contains(MQTT.SUBSCRIBE_TOPIC_PREFIX + GD.get_unique_id(GD.get_global_context()))) return;
		
		if(true == msg.contains("INVITE"))
		{
			//JsonArray array = parser.parse(message.toString()).getAsJsonArray();
			//for(JsonElement obj : array )
			{
				MQTTMsg.INVITE j = gson.fromJson(msg, MQTTMsg.INVITE.class);
				Log.i("MQTT", "INVITE: " + j.c + ", " + j.vp + ", " + j.ap);
				
				OnCall.instance().play();
				
				//fym
				//从服务器发送的INVITE消息体中获取服务器音视频接收端口
				if(null != j.ap && false == j.ap.isEmpty())
				{
					GD.SERVER_AUDIO_RECV_PORT = Integer.parseInt(j.ap);
				}
				
				if(null != j.vp && false == j.vp.isEmpty())
				{
					GD.SERVER_VIDEO_RECV_PORT = Integer.parseInt(j.ap);
				}
				
				MessageHandlerManager.get_instance().handle_message(GID.MSG_INCOMING_CALL, "CallerID", GD.MEDIA_INSTANCE);
			}
		}
		else if(true == msg.contains("BYE"))
		{
			MQTTMsg.BYE j = gson.fromJson(msg, MQTTMsg.BYE.class);
			Log.i("MQTT", "BYE: " + j.c);
			
			OnCall.instance().stop();
			
			MessageHandlerManager.get_instance().handle_message(GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
		}
		else if(msg.contains("roger"))//调度服务器对UA NAT注册的响应
		{
			
		}
		else if(msg.contains("{\"t\":"))//会议控制
		{
			//android.util.Log.v("SIP", "MESSAGE JSON: " + message.getBody());//message.toString());
			GD.parse_schedule_notifty_message(msg);
		}
		else if(msg.contains("{\"r\":"))//角色更新
		{
			//android.util.Log.v("UA", "Role MESSAGE JSON: " + message.getBody());//message.toString());
			parse_role_notifty(msg);
		}
	}
	
	private void parse_role_notifty(String json)
	{
	    //"r":"X"//"0"-听众非视频源，"1"-发言人 非视频源，"2"-听众兼视频源，"3"-发言人兼视频源 , "9"表示关闭当前调度
		try
		{
			JSONObject root = new JSONObject(json);
			
			//参数校验
			if(false == root.has("r"))
			{
				return;
			}
			else
			{
				int role = Integer.parseInt(root.getString("r"));
				
				android.util.Log.v("SIP", "Role: " + role);//message.toString());
				
				if(9 != role)
				{ 
					MessageHandlerManager.get_instance().handle_message(GID.MSG_CHANGE_ROLE,role,GD.MEDIA_INSTANCE);
				}
				else if(9 == role)
				{
					/*if(UA_STATE_IDLE != SipdroidReceiver.call_state)
					{
						android.util.Log.v("SIP", "role 9 1");
						hangup();
					}*/
					
					if(true == GD.is_in_schedule())
					{
						android.util.Log.v("Media", "hang up 2 " + json);
						MessageHandlerManager.get_instance().handle_message(GID.MSG_HANG_UP, GD.MEDIA_INSTANCE);
					}
					else
					{
						MessageHandlerManager.get_instance().handle_message(GID.MSG_RECV_CANCEL, GD.MEDIA_INSTANCE);
					}
				}
				else if(10 == role)
				{
					/*if(UA_STATE_IDLE != SipdroidReceiver.call_state)
					{
						hangup();
					}*/
					
					MessageHandlerManager.get_instance().handle_message(GID.MSG_SCHEDULE_REJECTED, GD.MEDIA_INSTANCE);
				}
			}
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			android.util.Log.v("JSON", e.toString());
			e.printStackTrace();
		}
	}
}
