package com.nercms.schedule.misc;

import java.text.DecimalFormat;

import com.nercms.schedule.network.NetworkStatus;

import android.util.Log;

public class ThreadStatistics
{
	private final long THREAD_STATISTICS_INTERVAL = 15000;//15��ͳ��һ��
	
	private long _latest_enter_timestamp;//���һ�ν���ʱ��
	private long _latest_leave_timestamp;//���һ���뿪ʱ��

	private long _accumulative_run_interval_interval;//����ͳ���ۼ�����(�ӽ��뵽�뿪)ʱ��
	private long _accumulative_schedule_interval_interval;//����ͳ���ۼƵ��ȣ����뿪���´ν���)ʱ��

	private long _run_interval_statistics_count;//����ͳ������ʱ���ۼ����д���
	private long _schedule_interval_statistics_count;//����ͳ�Ƶ���ʱ���ۼ����д���
	
	private long _latest_schedule_statistics_timestamp;//���һ��ͳ�Ƶ���ʱ��ʱ��
	private long _latest_run_statistics_timestamp;//���һ��ͳ������ʱ��ʱ��

	private double _average_run_interval;//�߳�ƽ������ʱ��
	private double _average_schedule_interval;//�߳�ƽ������ʱ��
	
	private String _tag = "";
	
	private DecimalFormat _df = new DecimalFormat( "0.0000");
	
	public ThreadStatistics(String tag)
	{
		_latest_enter_timestamp = System.currentTimeMillis();
		_latest_leave_timestamp = System.currentTimeMillis();
		_accumulative_run_interval_interval = 0;
		_accumulative_schedule_interval_interval = 0;
		_run_interval_statistics_count = 0;
		_schedule_interval_statistics_count = 0;
		_average_run_interval = 0.0;
		_average_schedule_interval = 0.0;
		_latest_schedule_statistics_timestamp = System.currentTimeMillis();
		_latest_run_statistics_timestamp = System.currentTimeMillis();
		
		_tag = tag;
	}
	
	public double get_run_interval()
	{
		return _average_run_interval;
	}

	public double get_schedule_interval()
	{
		return _average_schedule_interval;
	}
	
	public void enter()
	{
		if(false == GD.THREAD_STATISTICS)
			return;
		
		long timestamp = System.currentTimeMillis();

		//����
		_accumulative_schedule_interval_interval += (timestamp - _latest_leave_timestamp);
		_schedule_interval_statistics_count++;

		//ͳ��
		//if(THREAD_STATISTICS_INTERVAL <= _schedule_interval_statistics_count)
		if(THREAD_STATISTICS_INTERVAL <= (timestamp - _latest_schedule_statistics_timestamp))
		{
			_average_schedule_interval = (double)_accumulative_schedule_interval_interval / (double)_schedule_interval_statistics_count;

			_accumulative_schedule_interval_interval = 0;
			_schedule_interval_statistics_count = 0;
			_latest_schedule_statistics_timestamp = timestamp;
		}

		_latest_enter_timestamp = System.currentTimeMillis();
	}

	public void leave()
	{
		if(false == GD.THREAD_STATISTICS)
			return;
		
		long timestamp = System.currentTimeMillis();

		//����
		_accumulative_run_interval_interval += (timestamp - _latest_enter_timestamp);
		_run_interval_statistics_count++;

		//ͳ��
		//if(THREAD_STATISTICS_INTERVAL <= _run_interval_statistics_count)
		if(THREAD_STATISTICS_INTERVAL <= (timestamp - _latest_run_statistics_timestamp))
		{
			_average_run_interval = (double)_accumulative_run_interval_interval / (double)_run_interval_statistics_count;

			_accumulative_run_interval_interval = 0;
			_run_interval_statistics_count = 0;
			_latest_run_statistics_timestamp = timestamp;
		}

		_latest_leave_timestamp = System.currentTimeMillis();
	}
	
	public void record()
	{
		if(false == GD.THREAD_STATISTICS)
			return;
		
		//if(true == _tag.equals("Decode") || true == _tag.equals("Write"))
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", _tag + " Schedule " + _df.format(_average_schedule_interval) + "ms\r\n");
		
		//if(true == _tag.equals("Decode") || true == _tag.equals("Write"))
		//GD.log_to_db(GD.get_global_context(), 0, "Statistics", _tag + " Run " + _df.format(_average_run_interval) + "ms\r\n");
	}
}
