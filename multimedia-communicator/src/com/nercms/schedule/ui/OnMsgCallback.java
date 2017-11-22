package com.nercms.schedule.ui;

public abstract interface OnMsgCallback
{
	public abstract void on_msg_callback(int what, Object content);
}
