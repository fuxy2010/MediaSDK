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
	
	//�÷���������UI�̵߳���,����������UI�̵߳��� ���Զ�UI�ռ��������  
    @Override  
    protected void onPreExecute()
    {
    	GD.set_schedule_state(GD.SCHEDULE_STATE.closing);
    	
    	/*_exit_process_dialog = new ProgressDialog(_activity_context);//�˴����봫�����Activity��Context
		_exit_process_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//.STYLE_SPINNER);//���
		_exit_process_dialog.setTitle("�����˳�...");//����
		_exit_process_dialog.setMessage("�����˳�");//��ʾ
		//dialog.setIcon(android.R.drawable.ic_dialog_map);//ͼ��
		_exit_process_dialog.setIndeterminate(false);//������ȷ
		_exit_process_dialog.setCancelable(false);//�����ؼ�����ȡ��
		_exit_process_dialog.setMax(100);//���ֵ
		_exit_process_dialog.setProgress(0);//����ֵ
		_exit_process_dialog.show();//��ʾ*/
	}
  
    /**  
     * �����Void������ӦAsyncTask�еĵ�һ������   
     * �����String����ֵ��ӦAsyncTask�ĵ���������  
     * �÷�������������UI�̵߳��У���Ҫ�����첽�����������ڸ÷����в��ܶ�UI���еĿռ�������ú��޸�  
     * ���ǿ��Ե���publishProgress��������onProgressUpdate��UI���в���  
     */  
    @Override
    protected String doInBackground(Void... params)
    {
    	try
		{
			final long interval = 60;
			
			Log.i("Temp", "���ڹر�");
			
			//�رպ��й���
			publishProgress(0);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 0, 0, "���ڹرպ��й���"));
			GD.shutdown_sip_media_service();
			Log.i("Temp", "�ѹر�����Ƶ��ͨ�ŷ���");
			//Thread.sleep(interval);
			
			//��λ������ע��
			//publishProgress(60);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 60, 0, "����֪ͨ������ע��"));
			
			//�����ر�
			publishProgress(100);//_exit_process_handler.sendMessage(_exit_process_handler.obtainMessage(0, 100, 0, "�����ر�"));
			MessageHandlerManager.get_instance().handle_message(GID.MSG_DESTORY, GD.MEDIA_INSTANCE);
			MessageHandlerManager.get_instance().unregister_all();
			Log.i("Temp", "�����ر�");
			//Thread.sleep(interval);
			
			Thread.sleep(200);
			
			//���ɷ���onPostExecute��ִ�У�����finish������onPause��onDestroy
			AppManager.get_instance().app_exit(GD.get_global_context());
		}
		catch(Exception e)
		{
			Log.i("Temp", "Exit error " + e.toString());
		}
    	
    	return "exit completely!";
    }
    
    /**  
     * �����String������ӦAsyncTask�еĵ�����������Ҳ���ǽ���doInBackground�ķ���ֵ��  
     * ��doInBackground����ִ�н���֮�������У�����������UI�̵߳��� ���Զ�UI�ռ��������
     */  
    @Override  
    protected void onPostExecute(String result)
    {
    	Log.v("Temp", "AsyncTask Result: " + result);
		//_exit_process_dialog.dismiss();
    }
    
    /**  
     * �����Intege������ӦAsyncTask�еĵڶ�������  
     * ��doInBackground�������У���ÿ�ε���publishProgress�������ᴥ��onProgressUpdateִ��  
     * onProgressUpdate����UI�߳���ִ�У����п��Զ�UI�ռ���в���
     */  
    @Override  
    protected void onProgressUpdate(Integer... values)
    {
    	/*_exit_process_dialog.setProgress(values[0]);
    	
    	switch(values[0])
    	{
    		case 0:
    			_exit_process_dialog.setMessage("���ڹرպ��й���");
    			break;
    			
    		case 10:
    			_exit_process_dialog.setMessage("���ڹر�SOS���з���");
    			break;
    			
    		case 20:
    			_exit_process_dialog.setMessage("���ڹر�GPS����");
    			break;
    			
    		case 30:
    			_exit_process_dialog.setMessage("���ڹر�����Ƶ��ͨ�ŷ���");
    			break;
    			
    		case 60:
    			_exit_process_dialog.setMessage("����֪ͨ������ע��");
    			break;
    			
    		case 80:
    			_exit_process_dialog.setMessage("�������ٽ���");
    			break;
    			
    		case 100:
    			_exit_process_dialog.setMessage("�����ر�");
    			break;
    	}*/
    }
    
    //onCancelled����������ȡ��ִ���е�����ʱ����UI  
    @Override  
    protected void onCancelled()
    {
    }
}