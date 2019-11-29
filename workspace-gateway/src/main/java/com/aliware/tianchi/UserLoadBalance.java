package com.aliware.tianchi;

import javafx.util.Pair;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author daofeng.xjf
 * <p>
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {

    /**
     * 是否初始化
     * <p>
     * 初始化完成 true
     */
    private volatile boolean initFlag = false;

    /**
     * 线程池
     */
    private final ScheduledThreadPoolExecutor executor;


    /**
     * 权重队列
     */
    private volatile PriorityBlockingQueue<Pair<String, Node>> weightQueue;


    private static final String DEFAULT_THREADS = "100";

    /**
     * 启动一个线程去监控  ,获取权重信息 ........
     */
    public UserLoadBalance() {
        // 降序
        weightQueue = new PriorityBlockingQueue<>(3, (o1, o2) -> o2.getValue().getCurrentWeight() - o1.getValue().getCurrentWeight());

        executor = new ScheduledThreadPoolExecutor(2);

        executor.scheduleAtFixedRate(() -> {
            // 如果需要就执行 刷新节点
            if (CallbackListenerImpl.refreshFlag) {
                doRefresh();
            }

        }, 0, 500, TimeUnit.MILLISECONDS);

    }

    /**
     * 刷新
     */
    private void doRefresh() {

        ConcurrentHashMap<String, URL> serverInfo = CallbackListenerImpl.serverInfo;

        if (serverInfo.size() > 0) {

            HashMap<String, Integer> map = new HashMap<>();

            for (String key : serverInfo.keySet()) {

                URL url = serverInfo.get(key);

                String sThreads = url.getParameter("threads");


                if (null == sThreads) {
                    // 如果为空我们就 无语了 ... 均匀分配
                    sThreads = DEFAULT_THREADS;
                }

                Integer threads = Integer.valueOf(sThreads);

                map.put(key, threads);

            }

            refreshQueue(map);
        }
    }

    /**
     * 队列刷新
     *
     * @param map
     */
    private void refreshQueue(HashMap<String, Integer> map) {

        // 清空队列
        weightQueue.clear();

        //
        Set<String> keys = map.keySet();


        // 计算总权重
        int totalWeight = 0;
        for (String key : keys) {
            Integer currentWeight = map.get(key);
            totalWeight += currentWeight;
        }

        // 添加到队列中去
        for (String key : keys) {
            Integer integer = map.get(key);
            weightQueue.add(new Pair<>(key, new Node(integer, integer, totalWeight)));
        }


        // 遍历
        for (Pair<String, Node> stringNodePair : weightQueue) {
            System.out.println(stringNodePair.getKey() + " : " + stringNodePair.getValue());
        }


        // 刷新结束 - 改变标识符
        CallbackListenerImpl.refreshFlag = false;


        // 初始化完成 ...
        initFlag = true;
    }


    /**
     * 负载均衡
     *
     * @param invokers
     * @param url
     * @param invocation
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {

        // 初始化信息,如果获取不到阻塞
        initWeight();


        // 拿到信息均匀分配
        synchronized (UserLoadBalance.class) {

            // 如果信息大于 已知的节点信息 , 我们才使用  加权轮询算法
            if (weightQueue.size() != 0 && weightQueue.size() >= invokers.size()) {


                Pair<String, Node> hPair = weightQueue.poll();

                // 此时判断一下是否为空 ,以防万一
                if (null == hPair) {
                    return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
                }

                Node value = hPair.getValue();

                String key = hPair.getKey();

                for (int i = 0; i < invokers.size(); i++) {
                    URL url1 = invokers.get(i).getUrl();

                    // 选取相同的节点
                    if (key.equals(URLUtil.getKey(url1))) {

                        // 调整返回节点的权重
                        int afterCurrentWeight = value.getCurrentWeight() - value.getTotalWeight();
                        value.setCurrentWeight(afterCurrentWeight);


                        // 进行剩余节点进行调整
                        value.setCurrentWeight(value.getCurrentWeight() + value.getEffectiveWeight());
                        for (Pair<String, Node> stringNodePair : weightQueue) {
                            Node node = stringNodePair.getValue();
                            int after = node.getCurrentWeight() + node.getEffectiveWeight();
                            node.setCurrentWeight(after);
                        }
                        // 最后插入节点进行调整
                        weightQueue.add(hPair);

                        // 返回节点
                        return invokers.get(i);
                    }
                }

            }
        }

        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }

    /**
     * 初始化信息
     */
    private void initWeight() {
        if (!initFlag) {
            while (true) {
                if (initFlag) {
                    break;
                }
            }
        }
    }


    /**
     * 权重节点信息
     */
    private static class Node {

        private int currentWeight;
        private int effectiveWeight;
        private int totalWeight;


        @Override
        public String toString() {
            return "Node{" +
                    "currentWeight=" + currentWeight +
                    ", effectiveWeight=" + effectiveWeight +
                    ", totalWeight=" + totalWeight +
                    '}';
        }

        public Node() {
        }

        public Node(int currentWeight, int effectiveWeight, int totalWeight) {
            this.currentWeight = currentWeight;
            this.effectiveWeight = effectiveWeight;
            this.totalWeight = totalWeight;
        }

        public int getTotalWeight() {
            return totalWeight;
        }

        public void setTotalWeight(int totalWeight) {
            this.totalWeight = totalWeight;
        }

        public int getEffectiveWeight() {
            return effectiveWeight;
        }

        public void setEffectiveWeight(int effectiveWeight) {
            this.effectiveWeight = effectiveWeight;
        }

        public int getCurrentWeight() {
            return currentWeight;
        }

        public void setCurrentWeight(int currentWeight) {
            this.currentWeight = currentWeight;
        }

    }

}
