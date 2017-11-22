package com.nercms.schedule.audio;

public class AECMModule
{
	static
    {
    	System.loadLibrary("AudioProcess_AECM");
    }
	
	public native static long AECM_Create();
	public native static int AECM_Free(long aecmInst);
	public native static int AECM_Init(long aecmInst, int samFreq);
    public native static int AECM_BufferFarend(long aecmInst, byte farend[], short nrOfSamples);
    public native static int AECM_Process(long aecmInst, byte nearendNoisy[], byte nearendClean[], 
   			byte out[], short nrOfSamples, short msInSndCardBuf);
    
    public static long _handler = -1;
}
