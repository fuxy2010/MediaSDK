package com.nercms.schedule.video;

import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.MediaThreadManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceViewCallback  implements SurfaceHolder.Callback 
{
	
	public SurfaceViewCallback() 
	{
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		Log.v("Baidu", "surfaceChanged: " + ", " + width + ", " + height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
		Log.v("Baidu", "surfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		Log.v("Baidu", "surfaceDestroyed");
	}

}
