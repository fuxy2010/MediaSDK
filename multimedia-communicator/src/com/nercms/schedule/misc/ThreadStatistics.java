package com.nercms.schedule.misc;

import java.text.DecimalFormat;

import com.nercms.schedule.network.NetworkStatus;

import android.util.Log;

public class ThreadStatistics
{
	private final long THREAD_STATISTICS_INTERVAL = 15000;//15秒统计一次
	
	private long _latest_enter_timestamp;//最近一次进入时戳
	private long _latest_leave_timestamp;//最近一次离开时戳

	private long _accumulative_run_interval_interval;//本次统计累计运行(从进入到离开)时间
	private long _accumulative_schedule_interval_interval;//本次统计累计调度（从离开到下次进入)时间

	private long _run_interval_statistics_count;//本次统计运行时间累计运行次数
	private long _schedule_interval_statistics_count;//本次统计调度时间累计运行次数
	
	private long _latest_schedule_statistics_timestamp;//最近一次统计调度时间时戳
	private long _latest_run_statistics_timestamp;//最近一次统计运行时间时戳

	private double _average_run_interval;//线程平均运行时间
	private double _average_schedule_interval;//线程平均调度时间
	
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

		//更新
		_accumulative_schedule_interval_interval += (timestamp - _latest_leave_timestamp);
		_schedule_interval_statistics_count++;

		//统计
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

		//更新
		_accumulative_run_interval_interval += (timestamp - _latest_enter_timestamp);
		_run_interval_statistics_count++;

		//统计
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
