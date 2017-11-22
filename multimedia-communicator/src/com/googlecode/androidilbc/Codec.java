package com.googlecode.androidilbc;

public class Codec
{
	public native int encode(byte[] data, int dataOffset, int dataLength, byte[] samples, int samplesOffset);
    public native int decode(byte[] samples, int samplesOffset, int samplesLength, byte[] data, int dataOffset);
    private native int init(int mode);
    
    private short ILBC_MODE = 20;//30;
    
    private Codec()
    {
        System.loadLibrary("ilbc-codec");
        init(ILBC_MODE);
    }
    
    private volatile static Codec _unique_instance = null;
    public static Codec instance()
	{
		if(null == _unique_instance)
		{
			synchronized(Codec.class)
			{
				if(null == _unique_instance)
				{
					_unique_instance = new Codec();
				}
			}
		}			
		return _unique_instance;
	}
    
    public void free_encoder()
    {
    }
    
    public void free_decoder()
    {
    }
    
    public int encode(byte[] frame, int length, byte[] encoded)
    {
    	return encode(frame, 0, (20 == ILBC_MODE) ? 320 : 480, encoded, 0);
    }
    
    public int decode(byte[] encoded, int length, byte[] decoded)
    {
    	decode(encoded, 0, (20 == ILBC_MODE) ? 38 : 50, decoded, 0);
    	
    	return (20 == ILBC_MODE) ? 38 : 50;
    }
}
