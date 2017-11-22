package com.nercms.schedule.misc;

import java.io.IOException;
import java.net.UnknownHostException;

import android.util.Log;

public class Participant
{
	private static final int UA_TYPE_MOBILE_PHONE = 0;//警务终端1
	private static final int UA_TYPE_WalkieTalkie = 1;//警务终端2
	private static final int UA_TYPE_FIXED_PHONE = 2;//警务终端3
	
	public long _id;//与会UA的ID
	
	public int _type;//与会UA的类型
	
	public String _name;//与会UA的名称
	
	public boolean _has_answered;//是否已接听
	
	public boolean _is_speaker;//是否发言人
	
	public boolean _is_video_source;//是否视频源
	
}