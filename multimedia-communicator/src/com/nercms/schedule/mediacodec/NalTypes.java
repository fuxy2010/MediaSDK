package com.nercms.schedule.mediacodec;

public class NalTypes
{
	public static final int NAL_SLICE = 1;
	public static final int NAL_SLICE_DPA = 2;
	public static final int NAL_SLICE_DPB = 3;
	public static final int NAL_SLICE_DPC = 4;
	public static final int NAL_SLICE_IDR = 5;
	public static final int NAL_SEI = 6;
	public static final int NAL_SPS = 7;
	public static final int NAL_PPS = 8;
	public static final int NAL_AUD = 9;
	public static final int NAL_FILLER = 12;
	public static final int NAL_INVALID = -1;
	
	public static int get_video_packet_type(byte[] data, int length)
	{
		int offset = 0;

		if(3 > length)
			return NAL_INVALID;

		if(0 != data[0] || 0 != data[1])
			return NAL_INVALID;

		if(1 == data[2])
		{
			offset = 3;
		}
		else if(0 == data[2] && 1 == data[3])
		{
			offset = 4;
		}
		else
		{
			return NAL_INVALID;
		}

		return (int)(data[offset] & 0x1f);
	}
}
