package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface IEvent {

    /**
    * ��channel�õ�connect�¼�ʱ�����������
    * 
    * @param key
    * @throws IOException
    */
    void handle_connect(SelectionKey key) throws IOException;

    /**
    * ��channel�ɶ�ʱ�����������
    * 
    * @param key
    * @throws IOException
    */
    void handle_read(SelectionKey key) throws IOException;

    /**
    * ��channel��дʱ�����������
    * 
    * @throws IOException
    */
    void handle_write() throws IOException;

    /**
    * ��channel��������ʱ����
    * 
    * @param e
    */
    void handle_error(Exception e);
}
