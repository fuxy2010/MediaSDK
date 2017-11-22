package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface INIOHandler {
    public void processConnect(SelectionKey sk) throws IOException;
   
    public void processRead(SelectionKey sk) throws IOException;
    
    public void processWrite() throws IOException;
    
    public void processError(Exception e);
}
