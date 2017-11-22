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
	 * ����VAD����ģʽ��Խ�߽ף��߼�ģʽ����VAD������������ϸ�̶�Խ�ߡ����仰˵������ģʽֵ��mode�������ӣ�
	 * ���ݱ��ж�Ϊ�����Ŀ�����Խ��Ȼ��������Ҳ���������ӡ�
	 * mode ��Χ��0, 1�� 2�� 3��
	 */
	public static native int VAD_SetMode(int vadInst, int mode);
	
	/*
	 * �����ʣ�֧��8000, 16000�� 32000
	 * ֡    ���� ֧��10ms, 20ms, 30ms
	 * ����ֵ��1--�����źţ� 0--�����źţ� -1--����
	 */
	public static native int VAD_Process(int vadInst, int fs, byte[] audio_frame, int frame_length);
	
	/*
	 * �����Ч�Ĳ����ʺ�֡����ϣ�����֧��10ms, 20ms, 30ms��֡���Լ�8000, 16000, 32000Hz�Ĳ�����
	 * ����ֵ�� 0--��Ч��ϣ� -1--��Ч���
	 */
	
	public static native int VAD_ValidRateAndFrameLength(int rate, int frame_length);
	
	public static int _handler = -1;

}
 