package com.nercms.schedule.misc;

public class GID
{
	public static final int BASE_ID = 10000; // ��ϢID�Ļ�׼ֵ
	
	public static final int SCHEDULE_CONTROL_DIALOG_ID = BASE_ID + 1; //���ȶԻ����ID
	public static final int FILTER_DIALOG_ID = BASE_ID + 2; //��ѯ�Ի����ID
	public static final int PERSONAL_DETAIL_DIALOG_ID = BASE_ID + 3; //��Ա��ϸ��Ϣ�Ի��� 
	public static final int SOURCE_DIALOG_ID = BASE_ID + 4; //��Դ�Ի���
	
	public static final int MSG_REFRESH_MAP = BASE_ID + 101; //ˢ�µ�ͼ����	
	public static final int MSG_SHOW_VIDEO = BASE_ID + 102; //��ʾ��ƵԴ
	
	public static final int MSG_GET_SELF_LOCATION_SUCCESS = BASE_ID + 103; //��ȡ�û�λ����Ϣ�ɹ�
	public static final int MSG_GET_SELF_LOCATION_FAIL = BASE_ID + 104;//��ȡ��ǰGPS����ʧ��
	//public static final int MSG_GET_LOCATION_SUCCESS = BASE_ID + 105; //����IMSI�Ż�ȡλ����Ϣ�ɹ�
	public static final int MSG_REFRESH_STATISTIC = BASE_ID + 106; //ˢ��ͳ������
	public static final int MSG_LOCK_SCREEN = BASE_ID + 107; //��Ļ���
	//public static final int MSG_NEW_SCHEDULE = BASE_ID + 108; //�½�����
	
	public static final int MSG_DESTORY = BASE_ID + 109;   //�������ϢID
	public static final int MSG_DESTORY_DESCRIPTION = BASE_ID + 110; //���ٻ�ӭ�������Ϣ
	public static final int MSG_SEND_VIDEO = BASE_ID + 111;//fym ���뷢����Ƶ״̬
	public static final int MSG_RECV_VIDEO = BASE_ID + 112;//fym ���������Ƶ״̬
	public static final int MSG_HANG_UP = BASE_ID + 113;//fym �յ�BYE����������ת
	public static final int MSG_SELECTED_VIDEO_SOURCE_CHANGE = BASE_ID + 114; //���ȿ���ʱ�޸���ƵԴ��Ϣ
	public static final int MSG_SOCKET_ERROR = BASE_ID + 115; //socket�����쳣�����
	public static final int MSG_INVALID_CHANGE_ROLE = BASE_ID + 116; //��Ա��ɫ����ʱ����û��ѡ����μӵ��ȵ�����£��ı�����Ƶ������״̬
	public static final int MSG_STOP_SCHEDULE = BASE_ID + 117; //�رյ���
	public static final int MSG_SELECT_ALL = BASE_ID + 118; //ȫѡ
	public static final int MSG_SOURCE_SELECT = BASE_ID + 119; //��Դѡ��
	public static final int MSG_UPDATE_NETWORK_STATUS = BASE_ID + 120; //����ȼ������ı�
	public static final int MSG_CHANGE_ROLE = BASE_ID + 121; //���Ƚ�ɫ�����˱仯
	public static final int MSG_RECV_CANCEL= BASE_ID + 122; //�յ�cancel��Ϣ
	public static final int MSG_ACCELERATION_VARIETY = BASE_ID + 123; //���ٶ�
	public static final int MSG_SCHEDULE_REJECTED = BASE_ID + 124;//���ȱ��ܾ�
	public static final int MSG_RTSP_SESSION_RECONNECT = BASE_ID + 125;//RTSP�Ự��������
	public static final int MSG_ADJUST_LOCAL_TIME = BASE_ID + 126;//�����û����豾��ʱ��
	public static final int MSG_UPDATE_SYSTEM_TIPS = BASE_ID + 127;//ǿ�Ƹ���ϵͳ��ʾ
	
	public static final int MSG_UPDATE_APPLICATION = BASE_ID + 128;//���ز���װ���³���
	
	public static final int MSG_REMOVE_PARTICIPANT = BASE_ID + 129; // ɾ��������Ա����ϢID
	public static final int MSG_QUERY_DETAILS_OF_PERSON = BASE_ID + 130; // �õ�������Ա����ϸ��Ϣ
	public static final int MSG_SHOW_PERSON_DETAILS_DIALOG = BASE_ID + 131; //�����Ի�����ʾ������Ա����ϸ��Ϣ
	
	public static final int MSG_FAIL_IN_QUERY_PERSONS = BASE_ID + 132; //����ָ����Աʱ��û���ҵ���Ӧ����Ϣ
	public static final int MSG_CHECK_BOX_CHANGED = BASE_ID + 133; //��ѡ��ؼ���״̬�ı��¼�
	//public static final int SHUT_SOS_SCHEDULE = BASE_ID + 134; //�ر�SOS����
	public static final int MSG_REGISTER_ICON = BASE_ID + 135; //ע���ͼ���ID
	public static final int MSG_RETURN_TO_SCHEDULE_MAP = BASE_ID + 136; //���ص��Ƚ���
	public static final int MSG_NOTIFICATION = BASE_ID + 137; //֪ͨ��Ϣ 
	
	public static final int MSG_FILTER_CONDITION = BASE_ID + 138; //��������ѡ��
	
	public static final int MSG_MAP_ACTIVITY = BASE_ID + 139; //��ͼ����
	
	public static final int MSG_FORCE_EXIT = BASE_ID + 141; //�����ۺϷ���������ǿ������
	
	public static final int MSG_INCOMING_CALL = BASE_ID + 150;
	
	public static final int MSG_WAKE_UP = BASE_ID + 151;
	
	public static final int MSG_REFRESH_VIDEO_VIEW = BASE_ID + 153;
	
	public static final int MSG_PING_DELAY = BASE_ID + 155;
}
