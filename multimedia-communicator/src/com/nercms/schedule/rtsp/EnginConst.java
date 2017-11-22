package com.nercms.schedule.rtsp;

public interface EnginConst {
	/** 包最大大小 */
	public static final int QQ_MAX_PACKET_SIZE = 65535;
	/** 客户端版本号 - QQ2006 */
	public static final char QQ_CLIENT_VERSION_0F5F = 0x0F5F;
	/** 不需要确认的包的发送次数，这个值应该是随便的，由于QQ Logout包发了4次，所以我选4 */ 
    public static final int QQ_SEND_TIME_NOACK_PACKET = 4;
	/** 单位: ms */
	public static final long QQ_TIMEOUT_SEND = 5000;
}
