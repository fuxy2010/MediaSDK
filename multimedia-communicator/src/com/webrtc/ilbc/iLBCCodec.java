package com.webrtc.ilbc;

public class iLBCCodec
{
	/*
	 * �����������,����ֵΪ��Ӧ���
	 */
	public native static int iLBC_EncoderCreate();
	public native static int iLBC_DecoderCreate();
	
	/*
	 * ���������ʼ����processModeΪ����ģʽ20/30
	 */
	public native static short iLBC_EncoderInit(int encodeHandler, short processMode);
	public native static short iLBC_DecoderInit(int decodeHandler, short processMode);
	
	/*
	 * ���������������
	 * numOfSamplesΪ���������������20*8����30*8
	 * noOfLostFramesΪ��ʧ��֡����һ��Ϊ1
	 */
	public native static short iLBC_Encode(int encodeHandler, byte[] speechIn, short numOfSamples,  byte[] encoded);
	public native static short iLBC_Decode(int decodeHandler, byte[] encoded, short lenInByte, byte[] decoded);
	public native static short iLBC_DecodePlc(int decodeHandler, byte[] decoded, short noOfLostFrames);
	
	/*
	 * ��������ͷ�
	 */
	public native static short iLBC_EncoderFree(int encodeHandler);
	public native static short iLBC_DecoderFree(int decodeHandler);
	
	/*static
	{
		System.loadLibrary("iLBCModule");
	}*/
	
	private int _encoder = 0;
	private int _decoder = 0;
	private short ILBC_MODE = 20;//30;
	
	private iLBCCodec()
    {
		System.loadLibrary("iLBCModule");
		
		synchronized(iLBCCodec.class)
		{
			if(0 == _encoder)//��������ʱ��������codec
			{
				_encoder = iLBC_EncoderCreate();
				iLBC_EncoderInit(_encoder, ILBC_MODE);
			}
			
			if(0 == _decoder)//��������ʱ��������codec
			{
				_decoder = iLBC_DecoderCreate();
				iLBC_DecoderInit(_decoder, ILBC_MODE);
			}
		}
    }
    
	private volatile static iLBCCodec _unique_instance = null;
    public static iLBCCodec instance()
	{
		if(null == _unique_instance)
		{
			synchronized(iLBCCodec.class)
			{
				if(null == _unique_instance)
				{
					_unique_instance = new iLBCCodec();
				}
			}
		}			
		return _unique_instance;
	}
    
    public void free_encoder()
    {
    	synchronized(iLBCCodec.class)
		{
    		if(0 < _encoder)
    		{
    			//����ģʽ�¸����᳹�׹ر�codec 
    			//��������ʱ��������codec iLBC_EncoderFree(_encoder);
    		}
		}
    }
    
    public void free_decoder()
    {
    	synchronized(iLBCCodec.class)
		{
    		if(0 < _decoder)
    		{
    			//����ģʽ�¸����᳹�׹ر�codec 
    			//��������ʱ��������codec iLBC_EncoderFree(_decoder);
    		}
		}
    }
    
    public int encode(byte[] frame, int length, byte[] encoded)
    {
    	if(0 >= _encoder)
    		return 0;
    	
    	return (int)iLBC_Encode(_encoder, frame, (short)(8 * ILBC_MODE), encoded);
    	
    }
    
    public int decode(byte[] encoded, int length, byte[] decoded)
    {
    	if(0 >= _decoder)
    		return 0;
    	
    	iLBC_Decode(_decoder, encoded, (short)((20 == ILBC_MODE) ? 38 : 50), decoded);
    	
    	return (20 == ILBC_MODE) ? 38 : 50;
    }
    
    //���ز���������
    public boolean predictive_decode(byte[] frame, short lost_frame)
    {
    	if(0 >= _decoder || null == frame)
    		return false;
    	
    	return (8 * ILBC_MODE * lost_frame == iLBC_DecodePlc(_decoder, frame, lost_frame));//iLBC_DecodePlc���ز���������
    }

}
