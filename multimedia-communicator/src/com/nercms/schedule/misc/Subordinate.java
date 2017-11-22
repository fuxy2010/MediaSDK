package com.nercms.schedule.misc;

import java.util.HashMap;

//本机用户下属信息
public class Subordinate
{
	//下属集合HashMap<IMSI, Subordinate>
	public static HashMap<String, Subordinate> _subordinates = new HashMap<String, Subordinate>();
	
	public String imsi = "";
	public String name = "";
	public double latitude = 0.0;
	public double longitude = 0.0;
	public int status = 0;//0-空闲， 1-未应答非视频源， 2-已应答非视频源， 3-未应答视频源，4-已应答视频源
	public boolean gps_available = true;
}
