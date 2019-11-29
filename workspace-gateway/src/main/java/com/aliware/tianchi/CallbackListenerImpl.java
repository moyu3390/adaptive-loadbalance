package com.aliware.tianchi;

import org.apache.dubbo.common.URL;

import org.apache.dubbo.rpc.listener.CallbackListener;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author daofeng.xjf
 * <p>
 * 客户端监听器
 * 可选接口
 * 用户可以基于获取获取服务端的推送信息，与 CallbackService 搭配使用
 */
public class CallbackListenerImpl implements CallbackListener {

    static ConcurrentHashMap<String, URL> serverInfo = new ConcurrentHashMap<>();

    /**
     * 开始刷新  true
     * 完成刷新  false
     */
    static volatile boolean refreshFlag = false;

    @Override
    public void receiveServerMsg(String msg) {

        if (msg == null) {
            return;
        }
        URL value = URL.valueOf(msg);
        String key = URLUtil.getKey(value);


        /**
         * 判断是否存在节点信息
         */
        if (!serverInfo.containsKey(key)) {
            serverInfo.put(key, value);
            refreshFlag = true;
        }
    }

}
