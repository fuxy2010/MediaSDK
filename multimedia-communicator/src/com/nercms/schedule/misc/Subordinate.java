package com.nercms.schedule.misc;

import java.util.HashMap;

//�����û�������Ϣ
public class Subordinate
{
	//��������HashMap<IMSI, Subordinate>
	public static HashMap<String, Subordinate> _subordinates = new HashMap<String, Subordinate>();
	
	public String imsi = "";
	public String name = "";
	public double latitude = 0.0;
	public double longitude = 0.0;
	public int status = 0;//0-���У� 1-δӦ�����ƵԴ�� 2-��Ӧ�����ƵԴ�� 3-δӦ����ƵԴ��4-��Ӧ����ƵԴ
	public boolean gps_available = true;
}
