package cn.jgzhan.lrpc.client.loadbalance;

import cn.jgzhan.lrpc.common.dto.Pair;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/16
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private static final AtomicInteger INDEX = new AtomicInteger(0);

    @Override
    public LoadBalancerType getLoadBalancerType() {
        return LoadBalancerType.ROUND_ROBIN;
    }
    @Override
    public Pair<String, Integer> selectServiceAddress(Method service) {
        final var serviceAddress = this.getServiceAddress(service);
        final var nowIndex = INDEX.getAndIncrement() % serviceAddress.size();
        return serviceAddress.stream()
                .skip(nowIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("获取地址失败"));
    }
}
