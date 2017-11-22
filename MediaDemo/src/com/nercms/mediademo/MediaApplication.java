package com.nercms.mediademo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import com.nercms.schedule.ui.MediaInstance;
import com.tencent.bugly.crashreport.CrashReport;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

@ReportsCrashes(formKey = "",
				mailTo = "fuym@whu.edu.cn", customReportContent =
											{
												ReportField.APP_VERSION_NAME, ReportField.APP_VERSION_CODE, ReportField.ANDROID_VERSION,
												ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT
											},
				mode = ReportingInteractionMode.SILENT,
				forceCloseDialogAfterToast = false)

public class MediaApplication extends Application
{
	@Override
	public void onCreate()
	{
		//System.loadLibrary("H264Decoder_neon");
		
		System.loadLibrary("iLBCModule");
		
		ACRA.init(this);
		ErrorReporter.getInstance().setReportSender(new CrashReportSender(getApplicationContext()));
		
		super.onCreate();
		
		Log.v("Baidu", "MediaApplication");
		
		CrashReport.initCrashReport(getApplicationContext(), "c13caeb1b7", true);
		
		if(false)
		{
			MediaInstance.instance().api_start(getApplicationContext(),
										//1280, 720,
										//640, 480,
										352, 288,
										MediaDemo.server_ip_wan, MediaDemo.server_ip_lan, MediaDemo.server_in_lan,
										MediaDemo.server_port, MediaDemo.self_id, MediaDemo.encrypt_info);
			MediaInstance.instance().api_set_video_render_scale(1.2f);
		}
	}
	
	@Override
    protected void attachBaseContext(Context base)
	{
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
	
	// 2014-6-24
	public class CrashReportSender implements ReportSender
	{
		private Context context = null;

		public CrashReportSender(Context context) {
			this.context = context;
		}

		@Override
		public void send(CrashReportData arg0) throws ReportSenderException
		{
			// 在SD卡上重写crash日志
			// 然后一步上传到服务器
			// ..

			String appVersionName = arg0.getProperty(ReportField.APP_VERSION_NAME);
			String appVersionCode = arg0.getProperty(ReportField.APP_VERSION_CODE);
			String androidVersion = arg0.getProperty(ReportField.ANDROID_VERSION);
			String phoneModel = arg0.getProperty(ReportField.PHONE_MODEL);
			String customData = arg0.getProperty(ReportField.CUSTOM_DATA);
			String stackTrace = arg0.getProperty(ReportField.STACK_TRACE);

			String logName = "CrashReport_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis())) + ".txt";
			File logFile = new File(Environment.getExternalStorageDirectory().getPath() + "/ACAR/Log/", logName);
			if (!logFile.exists())
			{
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				FileWriter filerWriter = new FileWriter(logFile, true);// 后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
				BufferedWriter bufWriter = new BufferedWriter(filerWriter);
				bufWriter.write("APP_VERSION_NAME=" + appVersionName);
				bufWriter.newLine();
				bufWriter.write("APP_VERSION_CODE=" + appVersionCode);
				bufWriter.newLine();
				bufWriter.write("ANDROID_VERSION=" + androidVersion);
				bufWriter.newLine();
				bufWriter.write("PHONE_MODEL=" + phoneModel);
				bufWriter.newLine();
				bufWriter.write("CUSTOM_DATA=" + customData);
				bufWriter.newLine();
				bufWriter.write("STACK_TRACE=" + stackTrace);
				bufWriter.newLine();
				bufWriter.close();
				filerWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}