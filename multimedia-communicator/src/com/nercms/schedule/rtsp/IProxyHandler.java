package com.nercms.schedule.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface IProxyHandler {

    /**
     * 代理在CONNECT或者UDP ASSOCIATE命令成功后调用这个方法，或者在代理验证成功时调用
     * @param bindAddress 返回的BND.ADDR和BND.PORT。这个参数只对UDP方式有用处
     * @throws IOException
     */
    public void proxyReady(InetSocketAddress bindAddress) throws IOException;
    
    /**
     * 代理验证失败时调用此方法
     */
    public void proxyAuthFail();
    
    /**
     * 如果代理服务器发生未知错误，调用这个方法
     * 
     * @param err
     * 		错误信息
     */
    public void proxyError(String err);

}
