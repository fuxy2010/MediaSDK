package com.nercms.schedule.rtsp;

import android.util.Log;

public class LocklessBuffer
{
	private int write_pos;//元素位置，非以字节为单位
	private int read_pos;//元素位置，非以字节为单位
	private int capacity;//buffer容量，非以字节为单位
	private int element_size;//buffer中元素大小	
	private byte[] buffer = null;
	
	public Object lock = new Object();
	
	//size为buffer最多容纳element的个数
	public LocklessBuffer(int count, int size)
	{
		capacity = count;
		element_size = size;
		
		buffer = new byte[capacity * element_size];
		
		write_pos = 0;
		read_pos = 0;
	}
	
	@Override
	 protected void finalize()
	 {
		buffer = null;
		lock = null;
	 }
	
	public boolean write(byte[] data, int pos, int length)
	{
		if(null == buffer || length > capacity || 0 != length % element_size)
		{
			//Log.v("RTSP", "write fail");
			return false;
		}
		
		if(read_pos > write_pos)
		{
			//Log.v("RTSP", "write too slow r" + read_pos + " w" + write_pos);
			return false;
		}
		
		System.arraycopy(data, pos, buffer, (write_pos % capacity) * element_size, length);
		
		write_pos += (length / element_size);
		
		//Log.v("RTSP", "write " + write_pos);
		
		return true;
	}
	
	public boolean read(byte[] buf, int pos)
	{
		if(null == buffer || null == buf || element_size > buf.length - pos)
		{
			//Log.v("RTSP", "read fail");
			return false;
		}
		
		if(read_pos >= write_pos || capacity <= (write_pos - read_pos))
		{
			//Log.v("RTSP", "read too fast r" + read_pos + " w" + write_pos);
			return false;
		}
		
		System.arraycopy(buffer, (read_pos % capacity) * element_size, buf, pos, element_size);
		
		++read_pos;
		
		//Log.v("RTSP", "read " + read_pos);
		//Log.v("RTSP", "read " + (write_pos - read_pos));
		
		return true;
	}
	
	
}
