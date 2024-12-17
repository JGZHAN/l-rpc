package cn.jgzhan.lrpc.client.loadbalance;

import cn.jgzhan.lrpc.client.Consumer;
import cn.jgzhan.lrpc.common.dto.Pair;
import lombok.NonNull;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/16
 */
public interface LoadBalancer {

    /**
     * Get load balancer type load balancer type.
     *
     * @return the load balancer type
     */
    LoadBalancerType getLoadBalancerType();

    /**
     * Select service address string.
     *
     * @param service the service name
     * @return the string
     */
    Pair<String, Integer> selectServiceAddress(Method service);



    @NonNull
    default Set<Pair<String, Integer>> getServiceAddress(Method service) {
        return Optional.ofNullable(Consumer.getServiceAddress(service))
                .filter(addresses -> !addresses.isEmpty())
                .orElseThrow(() -> new RuntimeException("没有可用的服务提供者"));
    }

}
