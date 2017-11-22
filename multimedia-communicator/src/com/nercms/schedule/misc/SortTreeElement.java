package com.nercms.schedule.misc;

//用于RTP包排序
public class SortTreeElement implements Comparable<SortTreeElement>
{
	public int pos;//SortTreeElement插入排序树时的位置
	public int sequence;
	public int bMark;
	public int len;
	public long timestamp;

	public SortTreeElement(int pos, int sequence, int bMark, int len, long timestamp)
	{
		super();
		this.sequence = sequence;
		this.pos = pos;
		this.bMark = bMark;
		this.len=len;
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if (!(o instanceof SortTreeElement))
			return false;
		SortTreeElement r = (SortTreeElement) o;
		return sequence == r.sequence;
	}

	@Override
	public int compareTo(SortTreeElement another) {
		// TODO Auto-generated method stub
		return sequence - another.sequence;
	}

}
