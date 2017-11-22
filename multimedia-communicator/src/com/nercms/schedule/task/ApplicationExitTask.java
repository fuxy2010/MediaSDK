package com.nercms.schedule.task;

import com.nercms.schedule.misc.AppManager;
import com.nercms.schedule.misc.GD;
import com.nercms.schedule.misc.GID;
import com.nercms.schedule.ui.MessageHandlerManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class ApplicationExitTask extends AsyncTask<Void, Integer, String>
{
	//private ProgressDialog _exit_process_dialog;
	//public static Context _activity_context;
	
	//该方法运行在UI线程当中,并且运行在UI线程当中 可以对UI空间进行设置  
    @Override  
    protected void onPreExecute()
    {
    	GD.set_schedule_state(GD.SCHEDULE_STATE.closing);
    	
    	/*_exit_process_dialog = new ProgressDialog(_activity_context);//此处必须传入界面Activity的Context
		_exit_process_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//.STYLE_SPINNER);//风格
		_exit_process_dialog.setTitle("正在退出...");//标题
		_exit_process_dialog.setMessage("正在退出");//提示
		//dialog.setIcon(android.R.drawable.ic_dialog_map);//图标
		_exit_process_dialog.setIndeterminate(false);//进度明确
		_exit_process_dialog.setCancelable(false);//按返回键不可取消
		_exit_process_dialog.setMax(100);//最大值
		_exit_process_dialog.setProgress(0);//进度值
		_exit_process_dialog.show();//显示*/
	}
  
    /**  
     * 这里的Void参数对应AsyncTask中的第一个参数   
     * 这里的String返回值对应AsyncTask的第三个参数  
     * 该方法并不运行在UI线程当中，主要用于异步操作，所有在该方法中不能对UI当中的空间进行设置和修改  
     * 但是可以调用publishProgress方法触发onProgressUpdate对UI进行操作  
     */  
    @Override
    protected String doInBackground(Void... params)
    {
    	try
		{
			final long interval = 60;
			
			Log.i("Temp", "正在关闭");
			
			//关闭呼叫过滤
			publishProgress(0);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 0, 0, "正在关闭呼叫过滤"));
			GD.shutdown_sip_media_service();
			Log.i("Temp", "已关闭音视频及通信服务");
			//Thread.sleep(interval);
			
			//向定位服务器注销
			//publishProgress(60);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 60, 0, "正在通知服务器注销"));
			
			//即将关闭
			publishProgress(100);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 100, 0, "即将关闭"));
			MessageHandlerManager.get_instance().handle_message(GID.MSG_DESTORY, GD.MEDIA_INSTANCE);
			MessageHandlerManager.get_instance().unregister_all();
			Log.i("Temp", "即将关闭");
			//Thread.sleep(interval);
			
			Thread.sleep(200);
			
			//不可放在onPostExecute中执行，否则finish不触发onPause和onDestroy
			AppManager.get_instance().app_exit(GD.get_global_context());
		}
		catch(Exception e)
		{
			Log.i("Temp", "Exit error " + e.toString());
		}
    	
    	return "exit completely!";
    }
    
    /**  
     * 这里的String参数对应AsyncTask中的第三个参数（也就是接收doInBackground的返回值）  
     * 在doInBackground方法执行结束之后在运行，并且运行在UI线程当中 可以对UI空间进行设置
     */  
    @Override  
    protected void onPostExecute(String result)
    {
    	Log.v("Temp", "AsyncTask Result: " + result);
		//_exit_process_dialog.dismiss();
    }
    
    /**  
     * 这里的Intege参数对应AsyncTask中的第二个参数  
     * 在doInBackground方法当中，，每次调用publishProgress方法都会触发onProgressUpdate执行  
     * onProgressUpdate是在UI线程中执行，所有可以对UI空间进行操作
     */  
    @Override  
    protected void onProgressUpdate(Integer... values)
    {
    	/*_exit_process_dialog.setProgress(values[0]);
    	
    	switch(values[0])
    	{
    		case 0:
    			_exit_process_dialog.setMessage("正在关闭呼叫过滤");
    			break;
    			
    		case 10:
    			_exit_process_dialog.setMessage("正在关闭SOS呼叫服务");
    			break;
    			
    		case 20:
    			_exit_process_dialog.setMessage("正在关闭GPS服务");
    			break;
    			
    		case 30:
    			_exit_process_dialog.setMessage("正在关闭音视频及通信服务");
    			break;
    			
    		case 60:
    			_exit_process_dialog.setMessage("正在通知服务器注销");
    			break;
    			
    		case 80:
    			_exit_process_dialog.setMessage("正在销毁界面");
    			break;
    			
    		case 100:
    			_exit_process_dialog.setMessage("即将关闭");
    			break;
    	}*/
    }
    
    //onCancelled方法用于在取消执行中的任务时更改UI  
    @Override  
    protected void onCancelled()
    {
    }
}