package com.nercms.schedule.misc;

public class GID
{
	public static final int BASE_ID = 10000; // 消息ID的基准值
	
	public static final int SCHEDULE_CONTROL_DIALOG_ID = BASE_ID + 1; //调度对话框的ID
	public static final int FILTER_DIALOG_ID = BASE_ID + 2; //查询对话框的ID
	public static final int PERSONAL_DETAIL_DIALOG_ID = BASE_ID + 3; //人员详细信息对话框 
	public static final int SOURCE_DIALOG_ID = BASE_ID + 4; //资源对话框
	
	public static final int MSG_REFRESH_MAP = BASE_ID + 101; //刷新地图数据	
	public static final int MSG_SHOW_VIDEO = BASE_ID + 102; //显示视频源
	
	public static final int MSG_GET_SELF_LOCATION_SUCCESS = BASE_ID + 103; //获取用户位置信息成功
	public static final int MSG_GET_SELF_LOCATION_FAIL = BASE_ID + 104;//获取当前GPS数据失败
	//public static final int MSG_GET_LOCATION_SUCCESS = BASE_ID + 105; //根据IMSI号获取位置信息成功
	public static final int MSG_REFRESH_STATISTIC = BASE_ID + 106; //刷新统计数据
	public static final int MSG_LOCK_SCREEN = BASE_ID + 107; //屏幕变黑
	//public static final int MSG_NEW_SCHEDULE = BASE_ID + 108; //新建调度
	
	public static final int MSG_DESTORY = BASE_ID + 109;   //界面的消息ID
	public static final int MSG_DESTORY_DESCRIPTION = BASE_ID + 110; //销毁欢迎界面的消息
	public static final int MSG_SEND_VIDEO = BASE_ID + 111;//fym 进入发送视频状态
	public static final int MSG_RECV_VIDEO = BASE_ID + 112;//fym 进入接收视频状态
	public static final int MSG_HANG_UP = BASE_ID + 113;//fym 收到BYE信令后界面跳转
	public static final int MSG_SELECTED_VIDEO_SOURCE_CHANGE = BASE_ID + 114; //调度控制时修改视频源消息
	public static final int MSG_SOCKET_ERROR = BASE_ID + 115; //socket出现异常的情况
	public static final int MSG_INVALID_CHANGE_ROLE = BASE_ID + 116; //人员角色控制时，在没有选中其参加调度的情况下，改变其视频，发言状态
	public static final int MSG_STOP_SCHEDULE = BASE_ID + 117; //关闭调度
	public static final int MSG_SELECT_ALL = BASE_ID + 118; //全选
	public static final int MSG_SOURCE_SELECT = BASE_ID + 119; //资源选择
	public static final int MSG_UPDATE_NETWORK_STATUS = BASE_ID + 120; //网络等级发生改变
	public static final int MSG_CHANGE_ROLE = BASE_ID + 121; //调度角色发生了变化
	public static final int MSG_RECV_CANCEL= BASE_ID + 122; //收到cancel消息
	public static final int MSG_ACCELERATION_VARIETY = BASE_ID + 123; //加速度
	public static final int MSG_SCHEDULE_REJECTED = BASE_ID + 124;//调度被拒绝
	public static final int MSG_RTSP_SESSION_RECONNECT = BASE_ID + 125;//RTSP会话断线重连
	public static final int MSG_ADJUST_LOCAL_TIME = BASE_ID + 126;//提醒用户重设本机时间
	public static final int MSG_UPDATE_SYSTEM_TIPS = BASE_ID + 127;//强制更新系统提示
	
	public static final int MSG_UPDATE_APPLICATION = BASE_ID + 128;//下载并安装更新程序
	
	public static final int MSG_REMOVE_PARTICIPANT = BASE_ID + 129; // 删除会议人员的消息ID
	public static final int MSG_QUERY_DETAILS_OF_PERSON = BASE_ID + 130; // 得到单个警员的详细信息
	public static final int MSG_SHOW_PERSON_DETAILS_DIALOG = BASE_ID + 131; //弹出对话框，显示单个警员的详细信息
	
	public static final int MSG_FAIL_IN_QUERY_PERSONS = BASE_ID + 132; //查找指定人员时，没有找到相应的信息
	public static final int MSG_CHECK_BOX_CHANGED = BASE_ID + 133; //复选框控件的状态改变事件
	//public static final int SHUT_SOS_SCHEDULE = BASE_ID + 134; //关闭SOS调度
	public static final int MSG_REGISTER_ICON = BASE_ID + 135; //注册的图标的ID
	public static final int MSG_RETURN_TO_SCHEDULE_MAP = BASE_ID + 136; //返回调度界面
	public static final int MSG_NOTIFICATION = BASE_ID + 137; //通知消息 
	
	public static final int MSG_FILTER_CONDITION = BASE_ID + 138; //过滤条件选择
	
	public static final int MSG_MAP_ACTIVITY = BASE_ID + 139; //地图界面
	
	public static final int MSG_FORCE_EXIT = BASE_ID + 141; //根据综合服务器配置强制下线
	
	public static final int MSG_INCOMING_CALL = BASE_ID + 150;
	
	public static final int MSG_WAKE_UP = BASE_ID + 151;
	
	public static final int MSG_REFRESH_VIDEO_VIEW = BASE_ID + 153;
	
	public static final int MSG_PING_DELAY = BASE_ID + 155;
}
