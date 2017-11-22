package com.nercms.schedule.misc;

import android.util.Log;

public class MediaStatistics
{
	//ͳ�������Ƿ�δ��ʼ��
	private boolean _data_initialized = false;

	//�Ƿ���ͣͳ��
	private boolean _pause = true;
	
	//ͳ����������
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
		
		//����_data_initialized��Ϊtrue�������յ����ݰ������sequence
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
		//���ڵ����в�ͳ�����ݰ���������ȡ��������ж�����״̬ʱ����
		if(true == _pause)
			return -2;
		
		//�����ظ�ͳ�ƣ���ʼ���ȣ�������15�����޷���ȡ�հ�������ȡ��������ж�����״̬ʱ����
		if(System.currentTimeMillis() < _resume_timestamp + _PACKET_LOST_RATE_STATISTICS_INTERVAL)
			return -1;
		
		//��_packet_count����δ������ʵ�������ݰ�����
		return (System.currentTimeMillis() > (_packet_count_update_timestamp + _PACKET_LOST_RATE_STATISTICS_INTERVAL)) ? 0 : _packet_count;
	}
	
	//����ͳ��
	private final int _PACKET_LOST_RATE_STATISTICS_INTERVAL = 15000;//ÿ10��ͳ��һ�ζ�����
	private int _latest_packet_sequence = -1;//���һ���յ������ݰ������
	private long _lost_packet_count = 0;//����ͳ�����ۼƶ�����
	private long _packet_count_for_lost_rate_statistics = 0;//����ͳ�����յ��İ���
	private double _packet_lost_rate = 0.0;//����ͳ�ƵĶ����ʣ��ٷ���
	private long _latest_lost_rate_statistics_timestamp = 0;//�ϴ�ͳ��ʱ��
	
	private long _packet_count = 0;//���15�����յ������ݰ���
	private long _packet_count_update_timestamp = 0;//���һ�θ���_packet_count��ʱ��
	
	private int packet_lost_rate_statistics(int sequence)
	{
		//short s1 = 0;
		//short s2 = (short) 0xFFFF;
		//short s3 = (short) (s2 - s1);
		//Log.v("Baidu", "seq " + s3);
				
		//�����ۼƶ�����//////////////////////////////////////////////////////////////////////////
		int seq_diff = 0;
		if(-1 != _latest_packet_sequence)
		{
			seq_diff = (int) ((int)sequence - _latest_packet_sequence);
			if(1 < seq_diff)//��������������
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
		
		//���㶪����//////////////////////////////////////////////////////////////////////////
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
	
	//��������ͳ��
	private final int _PACKET_ARRIVAL_TIMESTAMP_INTERVAL_STATISTICS_INTERVAL = 5000;//ÿ60��ͳ��һ�ΰ�������ƽ��ֵ
	private long _latest_packet_arrival_timestamp = 0;//���һ���յ������ݰ������ʱ��
	private long _accumulative_packet_arrival_timestamp_interval = 0;//����ͳ�����ۼ����ݰ���������
	private long _packet_count_for_packet_arrival_timestamp_interval = 0;//����ͳ�����յ��İ���
	private double _average_packet_arrival_timestamp_interval = 0.0;//����ͳ�Ƶİ�����ʱ�����ƽ��ֵ
	private long _latest_packet_arrival_interval_statistics_timestamp = 0;//�ϴ�ͳ�����ʱ��
	private void packet_arrival_interval_statistics()
	{
		if(0 != _media_type)
			return;
		
		//���¿�ʼͳ��ʱ
		if(0 == _latest_packet_arrival_interval_statistics_timestamp)
		{
			_latest_packet_arrival_interval_statistics_timestamp = System.currentTimeMillis();
			_packet_count_for_packet_arrival_timestamp_interval = 0;
			_accumulative_packet_arrival_timestamp_interval = 0;
		}
		
		//�����ۼư�����ʱ����//////////////////////////////////////////////////////////////////////////
		long timestamp = System.currentTimeMillis();

		//������ʱ������ֵ�����޵İ�
		if(timestamp >= _latest_packet_arrival_timestamp)
		{
			_accumulative_packet_arrival_timestamp_interval += timestamp - _latest_packet_arrival_timestamp;

			_packet_count_for_packet_arrival_timestamp_interval++;
		}

		_latest_packet_arrival_timestamp = timestamp;		

		//���������ʱ��ƽ��ֵ//////////////////////////////////////////////////////////////////////////
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
	
	//��ʱͳ��
	private final int _PACKET_DELAY_STATISTICS_INTERVAL = 15000;//ÿ10��ͳ��һ�ΰ������ӳ�
	private long _latest_packet_timestamp = 0;//���һ���յ������ݰ���ʱ��
	private long _latest_packet_relative_timestamp = 0;//���һ�����ݰ������ʱ����������ʱ��)
	private long _accumulative_packet_timestamp_relative_delay = 0;//����ͳ�����ܼ����ݰ�����ӳ�
	private long _accumulative_packet_timestamp_absolute_delay = 0;//����ͳ�����ܼ����ݰ������ӳ�
	private long _packet_count_for_packet_delay_statistics = 0;//����ͳ�����յ��İ���
	private double _average_packet_relative_delay = 0.0;//����ͳ�Ƶİ�����ӳ�ƽ��ֵ�����룩
	private double _average_packet_absolute_delay = 0.0;//����ͳ�Ƶİ������ӳ�ƽ��ֵ�����룩
	private long _latest_packet_delay_statistics_timestamp = 0;//�ϴ�ͳ�����ʱ��	
	private void packet_delay_statistics(long packet_timestamp, boolean mark)
	{
		//ֻ����markΪtrue����Ƶ��
		//��Ƶ��ȫ������
		if(1 == _media_type && false == mark)
		{
			return;
		}
		
		long timestamp = System.currentTimeMillis();
		
		//���¿�ʼͳ��ʱ
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
			//��packet_timestamp����ֵ�����޻�תʱ���¼���
			_latest_packet_timestamp = packet_timestamp;
			_latest_packet_relative_timestamp = timestamp;			
			_latest_packet_delay_statistics_timestamp = timestamp;
			_packet_count_for_packet_delay_statistics = 0;
			
			return;
		}
		
		//�����ܼ����ݰ�����ӳ�
		_accumulative_packet_timestamp_relative_delay += (timestamp - _latest_packet_relative_timestamp) - (packet_timestamp - _latest_packet_timestamp);
		
		//������Ƶ���ݰ������ӳ�
		if(1 == _media_type)
		{
			//_accumulative_packet_timestamp_absolute_delay += ((timestamp % 0x100000000L) - packet_timestamp);
			_accumulative_packet_timestamp_absolute_delay += ((timestamp & 0xFFFFFFFFL) - packet_timestamp);
		}
		
		_latest_packet_timestamp = packet_timestamp;
		_latest_packet_relative_timestamp = timestamp;
		
		_packet_count_for_packet_delay_statistics++;
		
		//�����ӳ�//////////////////////////////////////////////////////////////////////////
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
	
	//����ͳ��
	private final int _BITRATE_STATISTICS_INTERVAL = 15000;//ÿ10��ͳ��һ������
	private long _accumulative_packet_length = 0;//����ͳ�����ܼ����ݰ�����
	private double _average_bitrate = 0.0;//����ͳ�Ƶ�����(kbps)
	private long _latest_bitreate_statistics_timestamp = 0;//�ϴ�ͳ�����ʱ��
	private void bitrate_statistics(int packet_length)
	{
		//���¿�ʼͳ��ʱ
		if(0 == _latest_bitreate_statistics_timestamp)
		{
			_latest_bitreate_statistics_timestamp = System.currentTimeMillis();
			_accumulative_packet_length = 0;
		}
		
		//�������ݰ��ܼƳ���
		_accumulative_packet_length += packet_length;
			
		//��������//////////////////////////////////////////////////////////////////////////
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
		//���������
		_latest_packet_sequence = sequence;
		_packet_count_for_lost_rate_statistics = 1;//�����1��ʼ
		_lost_packet_count = 0;
		_latest_lost_rate_statistics_timestamp = System.currentTimeMillis();//�ϴ�ͳ��ʱ��
				

		//�����������
		_latest_packet_arrival_timestamp = System.currentTimeMillis();
		_packet_count_for_packet_arrival_timestamp_interval = 0;//�����0��ʼ
		_latest_packet_arrival_interval_statistics_timestamp = 0;
		_accumulative_packet_arrival_timestamp_interval = 0;			
				
		//���ӳ����
		_latest_packet_timestamp = 0;
		_latest_packet_relative_timestamp = 0;
		_accumulative_packet_timestamp_relative_delay = 0;
		_accumulative_packet_timestamp_absolute_delay = 0;
		_packet_count_for_packet_delay_statistics = 0;//�����0��ʼ
		_latest_packet_delay_statistics_timestamp = 0;
				
		//�������
		_accumulative_packet_length = 0;
		_latest_bitreate_statistics_timestamp = 0;
	}

	//�յ����ݰ������
	public int update_packet_statistics(int sequence, long packet_timestamp, boolean mark, int packet_length)
	{
		if(false == GD.MEDIA_STATISTICS)
			return 0;
		
		if(true == _pause)
			return 0;
		
		//�Ƿ�տ�ʼͳ��δ��ʼ������//////////////////////////////////////////////////////////////////////////
		if(false == _data_initialized)
		{
			initialize(sequence);			
			_data_initialized = true;
			
			return 0;
		}
		
		//��������ʱ��ͳ��
		packet_arrival_interval_statistics();
		
		//���ݰ��ӳ�ͳ��
		packet_delay_statistics(packet_timestamp, mark);
		
		//ͳ������
		bitrate_statistics(packet_length);
		
		//������ͳ��
		return packet_lost_rate_statistics(sequence);
	}
	
}