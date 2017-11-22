package com.nercms.schedule.audio;

public abstract class AudioCodec
{
	public abstract void free_encoder();
    
    public abstract void free_decoder();
    
    //����ֵΪ������ֽڳ���
    public abstract int encode(byte[] frame, int length, byte[] encoded);
    
    //����ֵΪ���ν������Ƶ���ֽڳ���
    public abstract int decode(byte[] encoded, int length, byte[] decoded);
}
