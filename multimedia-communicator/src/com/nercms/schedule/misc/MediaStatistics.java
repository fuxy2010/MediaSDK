package com.nercms.schedule.misc;

import android.util.Log;

public class MediaStatistics
{
	//统计数据是否未初始化
	private boolean _data_initialized = false;

	//是否暂停统计
	private boolean _pause = true;
	
	//统计数据类型
	private int _media_type = 0;//0-audio, 1-video
	
	private long _resume_timestamp = 0;
	
	public MediaStatistics(int media_type)
	{
		_media_type = media_type;
	}
	
	public void pause()
	{
		_pause = true;
	}

	public void resume()
	{
		_pause = false;
		
		_latest_packet_arrival_interval_statistics_timestamp = _latest_packet_arrival_interval_statistics_timestamp = _latest_lost_rate_statistics_timestamp = System.currentTimeMillis();

		reset();
		
		_resume_timestamp = System.currentTimeMillis();
		
		//Log.v("Media", ((0 == _media_type) ? "A" : "V") + " resume statistics.");
	}

	public void reset()
	{
		_data_initialized = false;
		
		initialize(0);
		
		//不将_data_initialized置为true，留待收到数据包后更新sequence
	}

	public double get_packet_lost_rate()
	{
		return (false == _pause) ? _packet_lost_rate : 0.0;
	}
	
	public double get_averate_packet_timestamp_interval()
	{
		return (false == _pause) ? _average_packet_arrival_timestamp_interval : 0.0;
	}
	
	public double get_packet_relative_delay()
	{
		return (false == _pause) ? _average_packet_relative_delay : 0.0;
	}
	
	public double get_packet_absolute_delay()
	{
		return (false == _pause) ? _average_packet_absolute_delay : 0.0;
	}
	
	public double get_bitrate()
	{
		return (false == _pause) ? _average_bitrate : 0.0;
	}
	
	public long get_packet_count()
	{
		//不在调度中不统计数据包总数，不取零避免在判断网络状态时误判
		if(true == _pause)
			return -2;
		
		//如距离回复统计（开始调度）还不足15秒则无法获取收包数，不取零避免在判断网络状态时误判
		if(System.currentTimeMillis() < _resume_timestamp + _PACKET_LOST_RATE_STATISTICS_INTERVAL)
			return -1;
		
		//如_packet_count长期未更新则实际无数据包到达
		return (System.currentTimeMillis() > (_packet_count_update_timestamp + _PACKET_LOST_RATE_STATISTICS_INTERVAL)) ? 0 : _packet_count;
	}
	
	//丢包统计
	private final int _PACKET_LOST_RATE_STATISTICS_INTERVAL = 15000;//每10秒统计一次丢包率
	private int _latest_packet_sequence = -1;//最近一次收到的数据包的序号
	private long _lost_packet_count = 0;//本轮统计中累计丢包数
	private long _packet_count_for_lost_rate_statistics = 0;//本轮统计中收到的包数
	private double _packet_lost_rate = 0.0;//本轮统计的丢包率，百分制
	private long _latest_lost_rate_statistics_timestamp = 0;//上次统计时戳
	
	private long _packet_count = 0;//最近15秒内收到的数据包数
	private long _packet_count_update_timestamp = 0;//最近一次更新_packet_count的时戳
	
	private int packet_lost_rate_statistics(int sequence)
	{
		//short s1 = 0;
		//short s2 = (short) 0xFFFF;
		//short s3 = (short) (s2 - s1);
		//Log.v("Baidu", "seq " + s3);
				
		//更新累计丢包数//////////////////////////////////////////////////////////////////////////
		int seq_diff = 0;
		if(-1 != _latest_packet_sequence)
		{
			seq_diff = (int) ((int)sequence - _latest_packet_sequence);
			if(1 < seq_diff)//不考虑乱序的情况
			{
				_lost_packet_count += seq_diff - 1;
				//Log.v("Baidu", "AL " + _latest_packet_sequence + " -> " + sequence);
			}
			else
			{
				seq_diff = 1;
			}
		}

		_latest_packet_sequence = (int) sequence;
		_packet_count_for_lost_rate_statistics++;
		
		//计算丢包率//////////////////////////////////////////////////////////////////////////
		//if(PACKET_LOST_RATE_INTERVAL <= _packet_count_for_lost)
		if(_PACKET_LOST_RATE_STATISTICS_INTERVAL <= (System.currentTimeMillis() - _latest_lost_rate_statistics_timestamp))
		{
			_packet_count = _packet_count_for_lost_rate_statistics;
			_packet_count_update_timestamp = System.currentTimeMillis();
			
			if(0 != _packet_count_for_lost_rate_statistics)
			{
				_packet_lost_rate = 100.0 * ((double)_lost_packet_count / (double)(_packet_count_for_lost_rate_statistics + _lost_packet_count));
				//Log.v("RTSP", "PLR " + _packet_lost_rate + "%");
			}

			_lost_packet_count = 0;
			_packet_count_for_lost_rate_statistics = 0;
					
			_latest_lost_rate_statistics_timestamp = System.currentTimeMillis();
		}
		
		return (seq_diff - 1);
	}
	
	//包到达间隔统计
	private final int _PACKET_ARRIVAL_TIMESTAMP_INTERVAL_STATISTICS_INTERVAL = 5000;//每60秒统计一次包到达间隔平均值
	private long _latest_packet_arrival_timestamp = 0;//最近一次收到的数据包到达的时戳
	private long _accumulative_packet_arrival_timestamp_interval = 0;//本轮统计中累计数据包包到达间隔
	private long _packet_count_for_packet_arrival_timestamp_interval = 0;//本轮统计中收到的包数
	private double _average_packet_arrival_timestamp_interval = 0.0;//本轮统计的包到达时戳间隔平均值
	private long _latest_packet_arrival_interval_statistics_timestamp = 0;//上次统计完毕时戳
	private void packet_arrival_interval_statistics()
	{
		if(0 != _media_type)
			return;
		
		//重新开始统计时
		if(0 == _latest_packet_arrival_interval_statistics_timestamp)
		{
			_latest_packet_arrival_interval_statistics_timestamp = System.currentTimeMillis();
			_packet_count_for_packet_arrival_timestamp_interval = 0;
			_accumulative_packet_arrival_timestamp_interval = 0;
		}
		
		//更新累计包到达时间间隔//////////////////////////////////////////////////////////////////////////
		long timestamp = System.currentTimeMillis();

		//不考虑时戳到达值域上限的包
		if(timestamp >= _latest_packet_arrival_timestamp)
		{
			_accumulative_packet_arrival_timestamp_interval += timestamp - _latest_packet_arrival_timestamp;

			_packet_count_for_packet_arrival_timestamp_interval++;
		}

		_latest_packet_arrival_timestamp = timestamp;		

		//计算包到达时间平均值//////////////////////////////////////////////////////////////////////////
		//if(PACKET_AVERAGE_TIMESTAMP_INTERVAL <= _packet_count_for_interval)
		if(_PACKET_ARRIVAL_TIMESTAMP_INTERVAL_STATISTICS_INTERVAL <= (System.currentTimeMillis() - _latest_packet_arrival_interval_statistics_timestamp))
		{
			if(0 != _packet_count_for_packet_arrival_timestamp_interval)
			{
				_average_packet_arrival_timestamp_interval = ((double)_accumulative_packet_arrival_timestamp_interval / (double)(_packet_count_for_packet_arrival_timestamp_interval));
			}

			_accumulative_packet_arrival_timestamp_interval = 0;
			_packet_count_for_packet_arrival_timestamp_interval = 0;
					
			_latest_packet_arrival_interval_statistics_timestamp = System.currentTimeMillis();
		}
	}
	
	//延时统计
	private final int _PACKET_DELAY_STATISTICS_INTERVAL = 15000;//每10秒统计一次包到达延迟
	private long _latest_packet_timestamp = 0;//最近一次收到的数据包的时戳
	private long _latest_packet_relative_timestamp = 0;//最近一次数据包的相对时戳（即到达时戳)
	private long _accumulative_packet_timestamp_relative_delay = 0;//本轮统计中总计数据包相对延迟
	private long _accumulative_packet_timestamp_absolute_delay = 0;//本轮统计中总计数据包绝对延迟
	private long _packet_count_for_packet_delay_statistics = 0;//本轮统计中收到的包数
	private double _average_packet_relative_delay = 0.0;//本轮统计的包相对延迟平均值（毫秒）
	private double _average_packet_absolute_delay = 0.0;//本轮统计的包绝对延迟平均值（毫秒）
	private long _latest_packet_delay_statistics_timestamp = 0;//上次统计完毕时戳	
	private void packet_delay_statistics(long packet_timestamp, boolean mark)
	{
		//只计算mark为true的视频包
		//音频包全部计算
		if(1 == _media_type && false == mark)
		{
			return;
		}
		
		long timestamp = System.currentTimeMillis();
		
		//重新开始统计时
		if(0 == _latest_packet_timestamp && 0 == _latest_packet_relative_timestamp)
		{
			_latest_packet_timestamp = packet_timestamp;
			_latest_packet_relative_timestamp = timestamp;			
			_latest_packet_delay_statistics_timestamp = timestamp;
			_packet_count_for_packet_delay_statistics = 0;
			
			return;
		}
		
		if(packet_timestamp <= _latest_packet_timestamp && 200 > packet_timestamp)
		{
			//当packet_timestamp到达值域上限回转时重新计算
			_latest_packet_timestamp = packet_timestamp;
			_latest_packet_relative_timestamp = timestamp;			
			_latest_packet_delay_statistics_timestamp = timestamp;
			_packet_count_for_packet_delay_statistics = 0;
			
			return;
		}
		
		//更新总计数据包相对延迟
		_accumulative_packet_timestamp_relative_delay += (timestamp - _latest_packet_relative_timestamp) - (packet_timestamp - _latest_packet_timestamp);
		
		//更新视频数据包绝对延迟
		if(1 == _media_type)
		{
			//_accumulative_packet_timestamp_absolute_delay += ((timestamp % 0x100000000L) - packet_timestamp);
			_accumulative_packet_timestamp_absolute_delay += ((timestamp & 0xFFFFFFFFL) - packet_timestamp);
		}
		
		_latest_packet_timestamp = packet_timestamp;
		_latest_packet_relative_timestamp = timestamp;
		
		_packet_count_for_packet_delay_statistics++;
		
		//计算延迟//////////////////////////////////////////////////////////////////////////
		if(_PACKET_DELAY_STATISTICS_INTERVAL <= (System.currentTimeMillis() - _latest_packet_delay_statistics_timestamp))
		{
			if(0 != _packet_count_for_packet_delay_statistics)
			{
				_average_packet_relative_delay = ((double)_accumulative_packet_timestamp_relative_delay / (double)_packet_count_for_packet_delay_statistics);
				
				if(1 == _media_type)
				{
					_average_packet_absolute_delay = ((double)_accumulative_packet_timestamp_absolute_delay / (double)_packet_count_for_packet_delay_statistics);
				}
				
				//Log.v("Media", ((0 == _media_type) ? "A" : "V") + "D: " + _average_packet_delay + " C: " + _packet_count_for_packet_delay_statistics);
			}
			
			_latest_packet_delay_statistics_timestamp = System.currentTimeMillis();
			
			_accumulative_packet_timestamp_relative_delay = 0;
			_accumulative_packet_timestamp_absolute_delay = 0;
			_latest_packet_relative_timestamp = 0;
			_latest_packet_timestamp = 0;
			_packet_count_for_packet_delay_statistics = 0;
		}
	}
	
	//流量统计
	private final int _BITRATE_STATISTICS_INTERVAL = 15000;//每10秒统计一次流量
	private long _accumulative_packet_length = 0;//本轮统计中总计数据包长度
	private double _average_bitrate = 0.0;//本轮统计的码率(kbps)
	private long _latest_bitreate_statistics_timestamp = 0;//上次统计完毕时戳
	private void bitrate_statistics(int packet_length)
	{
		//重新开始统计时
		if(0 == _latest_bitreate_statistics_timestamp)
		{
			_latest_bitreate_statistics_timestamp = System.currentTimeMillis();
			_accumulative_packet_length = 0;
		}
		
		//更新数据包总计长度
		_accumulative_packet_length += packet_length;
			
		//计算流量//////////////////////////////////////////////////////////////////////////
		if(_BITRATE_STATISTICS_INTERVAL <= (System.currentTimeMillis() - _latest_bitreate_statistics_timestamp))
		{
			//_average_bitrate = ((double)(_accumulative_packet_length * 1000) / (double)(128 * (System.currentTimeMillis() - _latest_bitreate_statistics_timestamp)));
			_average_bitrate = ((double)(_accumulative_packet_length * 125) / (double)(16 * _BITRATE_STATISTICS_INTERVAL));
				
			//Log.v("Media", ((0 == _media_type) ? "A" : "V") + "B: " + _average_bitrate + "kbps");
			
			_accumulative_packet_length = 0;
			_latest_bitreate_statistics_timestamp = System.currentTimeMillis();
		}
	}
	
	private void initialize(int sequence)
	{
		//丢包率相关
		_latest_packet_sequence = sequence;
		_packet_count_for_lost_rate_statistics = 1;//必须从1开始
		_lost_packet_count = 0;
		_latest_lost_rate_statistics_timestamp = System.currentTimeMillis();//上次统计时戳
				

		//包到达间隔相关
		_latest_packet_arrival_timestamp = System.currentTimeMillis();
		_packet_count_for_packet_arrival_timestamp_interval = 0;//必须从0开始
		_latest_packet_arrival_interval_statistics_timestamp = 0;
		_accumulative_packet_arrival_timestamp_interval = 0;			
				
		//包延迟相关
		_latest_packet_timestamp = 0;
		_latest_packet_relative_timestamp = 0;
		_accumulative_packet_timestamp_relative_delay = 0;
		_accumulative_packet_timestamp_absolute_delay = 0;
		_packet_count_for_packet_delay_statistics = 0;//必须从0开始
		_latest_packet_delay_statistics_timestamp = 0;
				
		//流量相关
		_accumulative_packet_length = 0;
		_latest_bitreate_statistics_timestamp = 0;
	}

	//收到数据包后更新
	public int update_packet_statistics(int sequence, long packet_timestamp, boolean mark, int packet_length)
	{
		if(false == GD.MEDIA_STATISTICS)
			return 0;
		
		if(true == _pause)
			return 0;
		
		//是否刚开始统计未初始化数据//////////////////////////////////////////////////////////////////////////
		if(false == _data_initialized)
		{
			initialize(sequence);			
			_data_initialized = true;
			
			return 0;
		}
		
		//包到达间隔时间统计
		packet_arrival_interval_statistics();
		
		//数据包延迟统计
		packet_delay_statistics(packet_timestamp, mark);
		
		//统计流量
		bitrate_statistics(packet_length);
		
		//丢包率统计
		return packet_lost_rate_statistics(sequence);
	}
	
}