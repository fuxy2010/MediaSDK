package com.nercms.schedule.network;

import java.util.Date;

public class MQTTMsg
{
	/*{
		"c":"INVITE",
		"ap":"xxx",
		"vp":"xxx"
	}*/
	public static class INVITE
	{
		public String c;
		public String ap;
		public String vp;
	}
	
	/*{
		"c":"BYE"
	}*/
	public static class BYE
	{
		public String c;
	}
}
