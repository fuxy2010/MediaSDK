package com.nercms.schedule.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AACHardEncoder
{
	private MediaCodec _media_codec;
	private BufferedOutputStream _output_stream;
	private String _media_type = "OMX.google.aac.encoder";

	ByteBuffer[] _input_buffers = null;
	ByteBuffer[] _output_buffers = null;

	//"OMX.qcom.audio.decoder.aac";
	//"audio/mp4a-latm";

	public AACHardEncoder(int sample_rate, int bit_rate, int channel_num)
	{
		/*File f = new File(Environment.getExternalStorageDirectory(), "Download/audio_encoded.aac");
		touch(f);
		
		try
		{
			_output_stream = new BufferedOutputStream(new FileOutputStream(f));
			Log.e("AudioEncoder", "outputStream initialized");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}*/
		
		init_codec(sample_rate, bit_rate, channel_num);
	}
	
	private void init_codec(int sample_rate, int bit_rate, int channel_num)
	{
		//Log.v("Baidu", "init aac codec sample: " + sample_rate + " bitrate: " + bit_rate);
		// mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
		_media_codec = MediaCodec.createByCodecName(_media_type);
		//final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
		//final int kBitRates[] = { 64000,96000,128000 };
		MediaFormat media_format = MediaFormat.createAudioFormat("audio/mp4a-latm", sample_rate/*kSampleRates[3]*/, channel_num);//2);
		media_format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		media_format.setInteger(MediaFormat.KEY_BIT_RATE, bit_rate);//kBitRates[1]);
		media_format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192);//It will increase capacity of inputBuffers
		_media_codec.configure(media_format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
				
		_media_codec.start();

		_input_buffers = _media_codec.getInputBuffers();
		_output_buffers = _media_codec.getOutputBuffers();
		
	}

	public void close_codec()
	{
		try
		{
			if(null != _media_codec)
			{
				_media_codec.stop();
				_media_codec.release();
			}
			
			_output_stream.flush();
			_output_stream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	// called AudioRecord's read
	public synchronized void aac_encode(byte[] data, int offset, int length)
	{
		Log.i("Baidu", data.length + " is coming");
		
		Log.i("Baidu", "aac_encode 1");

		int input_index = _media_codec.dequeueInputBuffer(-1);
		if(0 <= input_index)
		{
			ByteBuffer input_buf = _input_buffers[input_index];
			input_buf.clear();
			input_buf.put(data, offset, length);
			_media_codec.queueInputBuffer(input_index, 0, data.length, 0, 0);
		}

		Log.i("Baidu", "aac_encode 2");
		
		MediaCodec.BufferInfo buffer_info = new MediaCodec.BufferInfo();
		int output_index = _media_codec.dequeueOutputBuffer(buffer_info, 0);

		// //trying to add a ADTS
		/*while(0 <= output_index)
		{
			int frame_size = buffer_info.size;
			int packet_size = frame_size + 7; // 7 is ADTS size
			
			ByteBuffer output_buf = _output_buffers[output_index];

			output_buf.position(buffer_info.offset);
			output_buf.limit(buffer_info.offset + frame_size);

			byte[] outData = new byte[packet_size];
			addADTStoPacket(outData, packet_size);

			output_buf.get(outData, 7, frame_size);
			output_buf.position(buffer_info.offset);

			// byte[] outData = new byte[bufferInfo.size];
			try {
				_output_stream.write(outData, 0, outData.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("AudioEncoder", outData.length + " bytes written");

			_media_codec.releaseOutputBuffer(output_index, false);
			output_index = _media_codec.dequeueOutputBuffer(buffer_info, 0);

		}*/
		
		Log.i("Baidu", "aac_encode 3 " + output_index);
		
		//写mp4无需ADTS
		while(0 <= output_index)
		{
			ByteBuffer output_buf = _output_buffers[output_index];
			
			byte[] frame = new byte[buffer_info.size];
			output_buf.get(frame);
			
			Log.i("Baidu", "AAC: " + frame[0] + ", " + frame[1] + ", " + frame[2] + ", " + frame[3] + ", " + frame[4] + ", " + frame[5] + ", " + frame[6]);
			
			//处理编码后数据////////////////////////////////////////////////////
			//if(null != G._mp4_creator)
			{
				//int ret = G._mp4_creator.mp4_add_audio(frame, frame.length);
				//Log.v("Baidu", "mp4_add_audio " + ret);
			}
			//////////////////////////////////////////////////////
			
			_media_codec.releaseOutputBuffer(output_index, false);
			output_index = _media_codec.dequeueOutputBuffer(buffer_info, 0);

		}
	}

	/**
	 * Add ADTS header at the beginning of each and every AAC packet. This is
	 * needed as MediaCodec encoder generates a packet of raw AAC data.
	 * 
	 * Note the packetLen must count in the ADTS header itself.
	 **/
	private void add_adts(byte[] packet, int packetLen)
	{
		int profile = 2; // AAC LC
		// 39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
		int freqIdx = 4; // 44.1KHz
		int chanCfg = 2; // CPE

		// fill in ADTS data
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF9;
		packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
		packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	public void touch(File f)
	{
		try {
			if (!f.exists())
				f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}