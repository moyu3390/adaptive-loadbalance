package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;


/**
 * @author daofeng.xjf
 * <p>
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {

    /**
     * 拿到调用信息 , 判断是否拿到了 , 不用每次都去取
     */
    private static volatile boolean flag = false;
    /**
     * 第一步
     *
     * @param invoker
     * @param invocation
     * @return
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            if (!flag) {
                if (invoker.getUrl() != null) {
                    CallbackServiceImpl.url = invoker.getUrl();
                    flag = true;
                }
            }
            return invoker.invoke(invocation);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 第二步
     *
     * @param result
     * @param invoker
     * @param invocation
     * @return
     */
    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }
}
