package com.nercms.schedule.misc;

public class CPUStypeDistinct {
	static {
		System.loadLibrary("CPUStype");  //���ؼ���
	}
	public native int GetCPUStype(); //�ײ�⺯��������ֵ1����C����           2��armV4������           3��armV7������+neon�Ż���
}
