package com.nercms.schedule.misc;

import java.io.IOException;
import java.net.UnknownHostException;

import android.util.Log;

public class Participant
{
	private static final int UA_TYPE_MOBILE_PHONE = 0;//�����ն�1
	private static final int UA_TYPE_WalkieTalkie = 1;//�����ն�2
	private static final int UA_TYPE_FIXED_PHONE = 2;//�����ն�3
	
	public long _id;//���UA��ID
	
	public int _type;//���UA������
	
	public String _name;//���UA������
	
	public boolean _has_answered;//�Ƿ��ѽ���
	
	public boolean _is_speaker;//�Ƿ�����
	
	public boolean _is_video_source;//�Ƿ���ƵԴ
	
}