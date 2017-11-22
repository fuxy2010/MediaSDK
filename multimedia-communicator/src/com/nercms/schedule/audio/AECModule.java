package com.nercms.schedule.audio;

public class AECModule
{
	static
	{
		System.loadLibrary("AudioProcess_AEC");
	}
	
	public native static long AEC_Create();
	public native static int AEC_Free(long aecInst);
	public native static int AEC_Init(long aecInst, int samFreq, int scSampFreq);
	public native static int AEC_BufferFarend(long aecInst, byte[] farend, short nrOfSamples);
	//nearendH和outH设置为null，skew设置为0
	public native static int AEC_Process(long aecInst, byte[] nearend, byte[] nearendH, byte[] out, byte[] outH, 
			short nrOfSamples, short msInSndCardBuf, int skew);
	
	public static long _handler = -1;
	
	
	/*long startTime = System.currentTimeMillis();
	readSize = audioRecord.read(audioData, 0, ReadLengthByte);
	recordBlock = (short)(System.currentTimeMillis() - startTime);
	
	long startTime = System.currentTimeMillis();
	if(inStream.read(inputDataByte, 0, ReadLengthByte) != ReadLengthByte)
	isProcessing = false;
	playBlock = (short)(System.currentTimeMillis() - startTime);*/
}
