package com.nercms.schedule.audio;

public class VADModule
{
	static
	{
		System.loadLibrary("AudioProcess_VAD");
	}
	
	public static native int VAD_Create();
	public static native int VAD_Free(int vadInst);
	public static native int VAD_Init(int vadInst);
	
	/*
	 * 设置VAD操作模式。越高阶（高级模式）的VAD对语音报告的严格程度越高。换句话说，随着模式值（mode）的增加，
	 * 数据被判断为语音的可能性越大。然而误判率也会随着增加。
	 * mode 范围（0, 1， 2， 3）
	 */
	public static native int VAD_SetMode(int vadInst, int mode);
	
	/*
	 * 采样率：支持8000, 16000， 32000
	 * 帧    长： 支持10ms, 20ms, 30ms
	 * 返回值：1--语音信号； 0--静音信号； -1--错误
	 */
	public static native int VAD_Process(int vadInst, int fs, byte[] audio_frame, int frame_length);
	
	/*
	 * 检测无效的采样率和帧长组合，我们支持10ms, 20ms, 30ms的帧长以及8000, 16000, 32000Hz的采样率
	 * 返回值： 0--有效组合； -1--无效组合
	 */
	
	public static native int VAD_ValidRateAndFrameLength(int rate, int frame_length);
	
	public static int _handler = -1;

}
 