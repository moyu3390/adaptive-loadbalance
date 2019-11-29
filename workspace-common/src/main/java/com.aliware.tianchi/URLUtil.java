package com.aliware.tianchi;

import org.apache.dubbo.common.URL;


/**
 * @date:2019/11/29 15:13
 * @author: <a href='mailto:fanhaodong516@qq.com'>Anthony</a>
 */

public class URLUtil {


    public static String getKey(URL uri) {

        StringBuilder builder = new StringBuilder();

        builder.append(uri.getProtocol());

        builder.append(uri.getIp());

        builder.append(uri.getPort());

        String url = builder.toString();

        builder = null;

        return url;
    }

}
