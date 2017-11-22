package com.nercms.schedule.video;

public class PacketFrames 
{
	long handle = 0;
	static 
	{
		System.loadLibrary("PacketFrame");
	}
	
	//����ֵΪ1����ʾ��ǰ���ݰ�����I֡
	//����ֵΪ0����ʾ��ǰ���ݰ�����P֡
	public native int IsIntraFrame(byte[] pPayload, int payloadlen);
	public native long CreateH264Packer();
	public native void DestroyH264Packer(long handle);
	public native int PackH264Frame(long handle, byte[] pPayload, int payloadlen, int bMark, int pts, int sequence, byte[] frmbuf);
}