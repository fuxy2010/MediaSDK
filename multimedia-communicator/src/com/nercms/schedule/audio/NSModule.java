package com.nercms.schedule.audio;

public class NSModule
{
	public static native int NS_Create();
	
	//fs为采样率
	public static native int NS_Init(int NS_inst, int fs);
	public static native int NS_Free(int NS_inst);
	
	//mode参数是噪音抑制参数设置，范围从0到2
	//0: Mild, 1: Medium , 2: Aggressive
	public static native int NS_set_policy(int NS_inst, int mode);
	
	//spframe_H和outframe_H两个数组设置为null，
	public static native int NS_Process(int NS_inst, byte spframe[], byte spframe_H[], byte outframe[], byte outframe_H[]);
	//此接口可以不用
	public static native float NS_speech_probability(int NS_inst);
	
	static
	{
		System.loadLibrary("AudioProcess_NS");
	}
	
	public static int _handler = -1;
}
