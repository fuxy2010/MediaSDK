package com.nercms.schedule.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.nercms.schedule.network.NetworkStatus;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class PerformanceStatistics
{
	private final static String T1 = "User ";
	private final static String T2 = "%, System ";
	private final static String T3 = "%, IOW ";
	private final static String T4 = "%, IRQ ";
	private final static String T5 = "%";
	private final static String process_name = "com.nercms.schedule";
	private static String cpu_statistics = "";
		
	public static void get_cpu_usage()  
	{
		String temp = null;
		Process p = null;		
		cpu_statistics = "";
		
		try 
		{
			p = Runtime.getRuntime().exec("top -m 1 -n 1 -d 1");  //����һ�����������
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while(null != (temp = br.readLine()))
			{
				if(1 > temp.trim().length())
				{
					continue;
				}
				else
				{
					if(temp.contains(T1) && temp.contains(T2) && temp.contains(T3) && temp.contains(T4))
					{
						int user = Integer.parseInt(temp.substring(temp.indexOf(T1)+T1.length(), temp.indexOf(T2)));
						int System = Integer.parseInt(temp.substring(temp.indexOf(T2)+T2.length(), temp.indexOf(T3)));
						int IOW = Integer.parseInt(temp.substring(temp.indexOf(T3)+T3.length(), temp.indexOf(T4)));
						int IRQ = Integer.parseInt(temp.substring(temp.indexOf(T4)+T4.length(), temp.indexOf(T5, temp.indexOf(T4)+T4.length())));
						int Total =  user + System + IOW + IRQ;
						
						cpu_statistics += (" CPU:  User " + user +"%, System "+ System +"%, IOW "+ IOW +"%, IRQ "+ IRQ +"%, Total "+ Total +"%, Schedule ");
					}
					else if(true == temp.contains(process_name))
					{
						cpu_statistics += (""+ Integer.parseInt(temp.substring(temp.indexOf(T5)-3, temp.indexOf(T5)).trim()) + "% \r\n");
						break;
					}
				}
			}
			
			cpu_statistics = ((true == GD.is_in_schedule()) ? "Schedule " : "Idle ") + cpu_statistics;
			
			//GD.log_to_db(GD.get_global_context(), 0, "Statistics", cpu_statistics);
			//Ч����
			//CPU:  User 15%, System 5%, IOW 3%, IRQ 0%, Total X%, Schedule XX%
			
			br.close();//�ر�������
			p.destroy();//�رմ����
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void get_process_memory()
	{
		ActivityManager mActivityManager = (ActivityManager)GD.get_global_context().getSystemService(Context.ACTIVITY_SERVICE);
		
		//ͨ������ActivityManager��getRunningAppProcesses()�������ϵͳ�������������еĽ���
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager.getRunningAppProcesses();
		
		for(ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList)
		{
			//if(false == appProcessInfo.processName.equals("com.nercms.schedule"))
			if(false == appProcessInfo.processName.contains(process_name))
			{
				continue;
			}
			
			//����ID��
			int pid = appProcessInfo.pid;
			
			//�û�ID ������Linux��Ȩ�޲�ͬ��IDҲ�Ͳ�ͬ ���� root��
			int uid = appProcessInfo.uid;
			
			//��øý���ռ�õ��ڴ�
			int[] pid_array = new int[] { pid };
			
			//��MemoryInfoλ��android.os.Debug.MemoryInfo���У�����ͳ�ƽ��̵��ڴ���Ϣ
			Debug.MemoryInfo[] memoryInfo = mActivityManager.getProcessMemoryInfo(pid_array);
				
			// ��ȡ����ռ�ڴ�����Ϣ kb��λ
			/*�ڴ���ã�VSS/RSS/PSS/USS
			Terms
			VSS - Virtual Set Size ��������ڴ棨���������ռ�õ��ڴ棩
			RSS - Resident Set Size ʵ��ʹ�������ڴ棨���������ռ�õ��ڴ棩
			PSS - Proportional Set Size ʵ��ʹ�õ������ڴ棨�������乲���ռ�õ��ڴ棩
			USS - Unique Set Size ���̶���ռ�õ������ڴ棨�����������ռ�õ��ڴ棩
			һ����˵�ڴ�ռ�ô�С�����¹��ɣ�VSS >= RSS >= PSS >= USS
			USS is the total private memory for a process, i.e. that memory that is completely unique to that process.USS is an extremely useful number because it indicates the true incremental cost of running a particular process. When a process is killed, the USS is the total memory that is actually returned to the system. USS is the best number to watch when initially suspicious of memory leaks in a process.*/
				
			//int memSize = memoryInfo[0].dalvikPrivateDirty;
			
			int USS = memoryInfo[0].getTotalPrivateDirty();
			int PSS = memoryInfo[0].getTotalPss();
			int RSS = memoryInfo[0].getTotalSharedDirty();
			//GD.log_to_db(GD.get_global_context(), 0, "Statistics",  ((true == GD.is_in_schedule()) ? "Schedule " : "Idle ") + "Memory USS " + USS + "KB, PSS " + PSS + "KB, RSS " + RSS + "KB\r\n");
			
			pid_array = null;
			
			break;
		}
	}
}
