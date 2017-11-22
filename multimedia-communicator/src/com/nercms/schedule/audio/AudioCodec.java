package com.nercms.schedule.audio;

public abstract class AudioCodec
{
	public abstract void free_encoder();
    
    public abstract void free_decoder();
    
    //返回值为编码后字节长度
    public abstract int encode(byte[] frame, int length, byte[] encoded);
    
    //返回值为本次解码的音频包字节长度
    public abstract int decode(byte[] encoded, int length, byte[] decoded);
}
