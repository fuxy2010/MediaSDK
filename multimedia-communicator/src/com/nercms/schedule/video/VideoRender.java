package com.nercms.schedule.video;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.misc.MediaThreadManager;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

public class VideoRender 
{
	private SurfaceView _video_render_view = null;
	private SurfaceView _video_capture_view = null;
	
	private int _current_camera = 0;//0-��������ͷ��1-ǰ������ͷ
	private Camera _camera = null;//��������ͷ��������ڲ�������ͷ
	private boolean _previewing = false;//����ͷԤ�����б�־
	private byte[] _video_buffer = null;//new byte[GD.VIDEO_WIDTH * GD.VIDEO_HEIGHT * 3 / 2];
	
	//public void add_video_view(View video_view, ImageView video_render_view)
	private SurfaceViewCallback _video_render_view_callback = new SurfaceViewCallback();
	public void set_video_view(SurfaceView video_render_view, SurfaceView video_capture_view)
	{
		_video_render_view = video_render_view;
		
		_video_capture_view = video_capture_view;
		video_capture_view.getHolder().addCallback(_video_render_view_callback);
	}
	
	private void start_video_render()
	{
		if(null == _video_render_view)
			return;
		
		stop_video_capture();
		
		_video_render_view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		
		_video_render_view.setZOrderMediaOverlay(true);
		//_video_render_view.setZOrderOnTop(true);
		
		//�����Ƶ���ܿؼ�
		MediaThreadManager.get_instance().set_video_render_view(_video_render_view);
		MediaThreadManager.get_instance()._video_receive_idle = false;
		
		GD._i_am_video_source = false;
	}
	
	private void start_video_capture()
	{
		if(null == _video_capture_view)
			return;
		
		stop_video_render();
		
		_video_capture_view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		start_camera();
		
		//������ƵԴIMSI
		GD._video_source_imsi = "";
		
		//�����Ƶ���Ϳؼ�
		_video_capture_view.setZOrderMediaOverlay(true);
		//_video_capture_view.setZOrderOnTop(true);
		
		//���²����͵�ͼͼ��
		GD._i_am_video_source = true;
	}
	
	//������Ƶ״̬
	public void set_video_state()
	{
		Log.v("Baidu", "set_video_state");
		if(false == GD._i_am_video_source)
		{
			//_video_render_view.setVisibility(View.VISIBLE);
			//_video_capture_view.setVisibility(View.GONE);
			//_video_capture_view.getHolder().setFormat(PixelFormat.TRANSPARENT);
			//_video_render_view.setZOrderOnTop(true);
			start_video_render();
		}
		else
		{
			//_video_render_view.setVisibility(View.GONE);
			//_video_capture_view.setVisibility(View.VISIBLE);
			//_video_render_view.getHolder().setFormat(PixelFormat.TRANSPARENT);
			//_video_capture_view.setZOrderOnTop(true);
			start_video_capture();
		}
		
		if(null != GD._msg_callback) GD._msg_callback.on_msg_callback(GID.MSG_REFRESH_VIDEO_VIEW, null);
	}

	// �رս�����Ƶ�Ŀؼ�
	public void stop_video_render()
	{
		if(null == _video_render_view)
			return;
		
		MediaThreadManager.get_instance()._video_receive_idle = true;
		MediaThreadManager.get_instance().set_video_render_view(null);
	}

	// �رշ�����Ƶ�Ŀؼ�
	public void stop_video_capture()
	{
		stop_camera();
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(null == _video_capture_view)
			return;	
	}
	
	//��ʼ������Ƶ
	private void start_camera()
	{
		if(true == _previewing)
			return;
		
		try
		{
			//Log.v("Video", "start_camera 0.1");
			
			stop_camera();
			
			//Log.v("Video", "start_camera 0.2");
			
			_current_camera = (0 > _current_camera) ? 0 : _current_camera;
			
			int selected_camera = (Camera.getNumberOfCameras() > _current_camera) ? _current_camera : 0;
			
			//Log.v("Video", "startCamera " + selected_camera);
			
			if(_camera == null) 
			{
				//Log.v("Video", "start_camera 0.3");
				//�׳���fail to connect to camera service����
				//��ʱ��ʹ����ϵͳ������౨�������������ָ�����
				_camera = Camera.open(selected_camera);
			}
			
			//Log.v("Video", "start_camera 0.5 " + _camera);
			
			if(true == _previewing) 
			{
				_camera.stopPreview();
			}
			
			//Log.v("Video", "start_camera 0.6");
			
			Camera.Parameters camera_parameters = _camera.getParameters();
			
			camera_parameters.setPreviewSize(GD.VIDEO_WIDTH, GD.VIDEO_HEIGHT);
			camera_parameters.setPreviewFrameRate(1000 / GD.VIDEO_SAMPLE_INTERVAL);//15);//����ֵ��ʵ�������ھ���Ӳ��
			
			//Log.v("Baidu", "cap " + GD.VIDEO_WIDTH +  ", " + GD.VIDEO_HEIGHT);
			
			//MOTO��֧�ָı�����ͷԤ���Ƕ�
			if(false == Build.MANUFACTURER.equalsIgnoreCase("motorola"))
			{
				_camera.setDisplayOrientation(90);
			}
			
			_camera.setParameters(camera_parameters);
			
			_video_buffer = new byte[GD.VIDEO_WIDTH * GD.VIDEO_HEIGHT * 3 / 2];
			_camera.addCallbackBuffer(_video_buffer);
			
			//Log.v("Video", "start_camera 0.7");
			
			//_camera.setPreviewCallback(new PreviewCallback()
			_camera.setPreviewCallbackWithBuffer(new PreviewCallback()
			{
			    public void onPreviewFrame(byte[] data, Camera camera) 
			    {
			    	//Log.v("Baidu", "enc1 " + data.length + ", " + GD.VIDEO_WIDTH +  ", " + GD.VIDEO_HEIGHT);
			    	//long timestamp = System.currentTimeMillis();		    	
			    	MediaThreadManager.get_instance().video_encode(data, (0 == _current_camera) ? false : true);		    	
			    	//Log.v("Video", "Encode " + (System.currentTimeMillis() - timestamp));
			    	
			    	_camera.addCallbackBuffer(_video_buffer);
			    }
			});
			
			//Log.v("Video", "start_camera 0.8");
			
			_video_capture_view.setZOrderOnTop(true);
			
			//_video_render_view.invalidate();
			//_video_render_view.postInvalidate();
			
			//Log.v("Video", "start_camera 1.0");
			
			_camera.setPreviewDisplay(_video_capture_view.getHolder());
			
			//Log.v("Video", "start_camera 1.1");
			
			_camera.startPreview();
			_previewing = true;
			
			//Log.v("Video", "start_camera 1.2");
		}
		catch(Exception e)
		{
			Log.v("Video", "start camera error: " + e.toString());
			
			if(null != _camera)
			{
				_camera.stopPreview();
				_camera.release();
			}
		}
	}
	
	public void stop_camera() 
	{
		if(_camera != null)
		{
			_camera.setPreviewCallback(null);
			_camera.stopPreview();
			_camera.release();
			_camera = null;	
		}
		
		_previewing = false;
		
		Log.v("Video","VideoCapture finish");
	}
	
	public void reverse()
	{
		stop_camera();
		
		_current_camera = (0 == _current_camera) ? 1 : 0;
		
		start_camera();
	}
}
