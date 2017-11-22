package com.nercms.schedule.misc;

public class CPUStypeDistinct {
	static {
		System.loadLibrary("CPUStype");  //¼ÓÔØ¼ì²â¿â
	}
	public native int GetCPUStype(); //µ×²ã¿âº¯Êı£¨·µ»ØÖµ1£º´¿C±àÂë           2£ºarmV4»ã±à±àÂë           3£ºarmV7»ã±à±àÂë+neonÓÅ»¯£©
}
