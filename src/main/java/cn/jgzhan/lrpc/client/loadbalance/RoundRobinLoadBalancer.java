package cn.jgzhan.lrpc.client.loadbalance;

import cn.jgzhan.lrpc.common.dto.Pair;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/16
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final Map<Method, AtomicInteger> INDEX_MAP;
    private final Map<Set<Pair<String, Integer>>, AtomicInteger> INDEX_MAP_OF_SPECIFY_THE_ADDRESS;
    public RoundRobinLoadBalancer(){
        INDEX_MAP = new ConcurrentHashMap<>();
        INDEX_MAP_OF_SPECIFY_THE_ADDRESS = new ConcurrentHashMap<>();
    }

    @Override
    public LoadBalancerType getLoadBalancerType() {
        return LoadBalancerType.ROUND_ROBIN;
    }
    @Override
    public Pair<String, Integer> selectServiceAddress(Method service) {
        final var serviceAddress = this.getServiceAddress(service);
        assert serviceAddress != null && !serviceAddress.isEmpty();
        final var nowIndex = INDEX_MAP.computeIfAbsent(service, k -> new AtomicInteger(0)).getAndIncrement();
        return select(serviceAddress, nowIndex);
    }

    @Override
    public Pair<String, Integer> selectServiceAddress(Set<Pair<String, Integer>> serviceAddress) {
        assert serviceAddress != null && !serviceAddress.isEmpty();
        final var nowIndex = INDEX_MAP_OF_SPECIFY_THE_ADDRESS.computeIfAbsent(serviceAddress, k -> new AtomicInteger(0)).getAndIncrement();
        return select(serviceAddress, nowIndex);
    }

    private static Pair<String, Integer> select(Set<Pair<String, Integer>> serviceAddress, int nowIndex) {
        nowIndex = nowIndex % serviceAddress.size();
        return serviceAddress.stream()
                .skip(nowIndex)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("获取地址失败"));
    }
}
