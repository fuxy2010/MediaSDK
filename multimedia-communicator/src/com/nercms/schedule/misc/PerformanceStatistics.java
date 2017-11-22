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
			p = Runtime.getRuntime().exec("top -m 1 -n 1 -d 1");  //定义一个处理机对象
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
			//效果：
			//CPU:  User 15%, System 5%, IOW 3%, IRQ 0%, Total X%, Schedule XX%
			
			br.close();//关闭输入流
			p.destroy();//关闭处理机
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void get_process_memory()
	{
		ActivityManager mActivityManager = (ActivityManager)GD.get_global_context().getSystemService(Context.ACTIVITY_SERVICE);
		
		//通过调用ActivityManager的getRunningAppProcesses()方法获得系统里所有正在运行的进程
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager.getRunningAppProcesses();
		
		for(ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList)
		{
			//if(false == appProcessInfo.processName.equals("com.nercms.schedule"))
			if(false == appProcessInfo.processName.contains(process_name))
			{
				continue;
			}
			
			//进程ID号
			int pid = appProcessInfo.pid;
			
			//用户ID 类似于Linux的权限不同，ID也就不同 比如 root等
			int uid = appProcessInfo.uid;
			
			//获得该进程占用的内存
			int[] pid_array = new int[] { pid };
			
			//此MemoryInfo位于android.os.Debug.MemoryInfo包中，用来统计进程的内存信息
			Debug.MemoryInfo[] memoryInfo = mActivityManager.getProcessMemoryInfo(pid_array);
				
			// 获取进程占内存用信息 kb单位
			/*内存耗用：VSS/RSS/PSS/USS
			Terms
			VSS - Virtual Set Size 虚拟耗用内存（包含共享库占用的内存）
			RSS - Resident Set Size 实际使用物理内存（包含共享库占用的内存）
			PSS - Proportional Set Size 实际使用的物理内存（比例分配共享库占用的内存）
			USS - Unique Set Size 进程独自占用的物理内存（不包含共享库占用的内存）
			一般来说内存占用大小有如下规律：VSS >= RSS >= PSS >= USS
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
