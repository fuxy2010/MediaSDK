package com.nercms.schedule.video;

public class PacketFrames 
{
	long handle = 0;
	static 
	{
		System.loadLibrary("PacketFrame");
	}
	
	//返回值为1：表示当前数据包属于I帧
	//返回值为0：表示当前数据包属于P帧
	public native int IsIntraFrame(byte[] pPayload, int payloadlen);
	public native long CreateH264Packer();
	public native void DestroyH264Packer(long handle);
	public native int PackH264Frame(long handle, byte[] pPayload, int payloadlen, int bMark, int pts, int sequence, byte[] frmbuf);
}