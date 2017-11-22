package com.nercms.schedule.task;

import java.io.File;

import com.nercms.schedule.misc.GD;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class DownloadApplicationTask extends AsyncTask<String, Integer, Boolean>
{
	//����������ɺ��intent  
    private class DownloadCompleteReceiver extends BroadcastReceiver
    {
    	@Override
    	public void onReceive(Context context, Intent intent)
    	{
    		if(intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    		{
    			Log.i("Baidu", "download completely " + intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));
            }  
        }  
    }
    
    private DownloadManager _download_manager;
    private DownloadCompleteReceiver _download_receiver;
    private DownloadManager.Request _download_request;
    
    public static long _download_id = -1;
    
	//�÷���������UI�̵߳���,����������UI�̵߳��� ���Զ�UI�ռ��������  
    @Override  
    protected void onPreExecute()
    {
    	_download_id = 0;//�������������Ѵ���
    	Log.i("Baidu", "start download new application.");
    	
    	_download_manager = (DownloadManager)GD.get_global_context().getSystemService(Context.DOWNLOAD_SERVICE);
    	
    	_download_receiver = new DownloadCompleteReceiver();
	}
    
    private void delete_old_file()
    {
    	File file = new File(Environment.getExternalStorageDirectory() + "/schedule/schedule.apk");
    	
    	if(file == null || false == file.exists() || true ==file.isDirectory())
    		return;
    	
    	file.delete();
    }
    
    private void start_downloading(String url)
    {
    	try
		{
    		delete_old_file();
    		
    		Log.i("Baidu", "download url " + url);
    		//String url = "http://10.0.2.2/android/film/G3.mp4";
    		_download_request = new DownloadManager.Request(Uri.parse(url));
            
            //������������
    		_download_request.setAllowedNetworkTypes(Request.NETWORK_MOBILE | Request.NETWORK_WIFI);
            //����ʱ������
    		_download_request.setAllowedOverRoaming(false);
            
            //��֪ͨ������ʾ�������̨��Ĭ����
    		_download_request.setShowRunningNotification(true);
            //��ʾ���ؽ���
    		_download_request.setVisibleInDownloadsUi(true);
    		
    		//��������·��Ϊsdcard��Ŀ¼�µ�schedule�ļ���
    		//_download_request.setDestinationInExternalPublicDir("/schedule/", "schedule.apk");
    		//֧����TF������
    		//����·��null����Ĭ��Ϊdata/data/com.android.provider.downloads/cache/
    		_download_request.setDestinationInExternalFilesDir(GD.get_global_context(), null, "schedule.apk");
    		
    		_download_request.setTitle("���ȳ������");
            
            //�������ض���
            _download_id = _download_manager.enqueue(_download_request);
            
            GD.get_global_context().registerReceiver(_download_receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		}
		catch(Exception e)
		{
			Log.i("Baidu", "update error " + e.toString());
		}
    }
    
    int query_status(long id)
    {
    	Cursor cursor = _download_manager.query(new DownloadManager.Query().setFilterById(id));
    	
    	if(null == cursor || false == cursor.moveToFirst())//�������moveToFirst
    	{
    		Log.i("Baidu", "query cursor is null");
    		return DownloadManager.STATUS_SUCCESSFUL;
    	}
    	
    	/*switch(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
    	{ 
    	    case DownloadManager.STATUS_FAILED:
    	        return "Download failed";
    	    case DownloadManager.STATUS_PAUSED: 
    	       return "Download paused";
    	    case DownloadManager.STATUS_PENDING:
    	        return "Download pending";
    	    case DownloadManager.STATUS_RUNNING: 
    	        return "Download in progress!";
    	    case DownloadManager.STATUS_SUCCESSFUL:  
    	        return "Download finished";
    	    default: 
    	        return "Unknown Information";
    	}*/
    	
    	return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
    }
    
    //�÷�������������UI�̵߳��У���Ҫ�����첽�����������ڸ÷����в��ܶ�UI���еĿռ�������ú��޸�
    @Override
    protected Boolean doInBackground(String... params)
    {
    	start_downloading(params[0]);
    	
    	Log.i("Baidu", "Download app id: " + _download_id);
    	
    	if(0 >= _download_id)
    		return false;
    	
    	int status = -1;
    	while(true)
    	{
    		status = query_status(_download_id);
    		
    		publishProgress(status);
    		
    		if(DownloadManager.STATUS_FAILED == status)
    		{
    			Log.i("Baidu", "Download app fail " + status);
    			_download_manager.remove(_download_id);
    			return false;
    		}
    		else if(DownloadManager.STATUS_PAUSED == status ||
    				DownloadManager.STATUS_PENDING == status ||
    				DownloadManager.STATUS_RUNNING == status)
    		{
    			//Log.v("Baidu", "Downloading app " + status);
    			try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		else if(DownloadManager.STATUS_SUCCESSFUL == status)
    		{
    			Log.i("Baidu", "Download app success " + status);
    			break;
    		}
    	}
    	
    	return true;
    }

	//��doInBackground����ִ�н���֮�������У�����������UI�̵߳��� ���Զ�UI�ռ��������
    @Override  
    protected void onPostExecute(Boolean result)
    {
    	//Log.v("Baidu", "install new apk 1");
    	
    	if(true == result)
    	{
    		//Log.v("Baidu", "install new apk 2");
    		
    		Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/schedule/schedule.apk")), "application/vnd.android.package-archive"); 
            //intent.setDataAndType(Uri.parse(url), "application/vnd.android.package-archive");//url�����"file://"
            //intent.setDataAndType(Uri.parse("file://sdcard/XXX.apk"), "application/vnd.android.package-archive");
            GD.get_global_context().startActivity(intent);
    	}
    	
    	_download_id = -1;
    }
    
    //onProgressUpdate����UI�߳���ִ�У����п��Զ�UI�ռ���в���
    @Override  
    protected void onProgressUpdate(Integer... values)
    {
    	//Log.v("Baidu", "download onProgressUpdate " + values[0]);
    }
    
    //onCancelled����������ȡ��ִ���е�����ʱ����UI  
    @Override  
    protected void onCancelled()
    {
    	_download_id = -1;
    }
}