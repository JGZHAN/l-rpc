package cn.jgzhan.lrpc.client.loadbalance;

import cn.jgzhan.lrpc.common.dto.Pair;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/16
 */
public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public LoadBalancerType getLoadBalancerType() {
        return LoadBalancerType.RANDOM;
    }

    @Override
    public Pair<String, Integer> selectServiceAddress(Method service) {

        final var serviceAddress = this.getServiceAddress(service);

        final var skipNum = ThreadLocalRandom.current().nextInt(0, serviceAddress.size());

        return serviceAddress.stream()
                .skip(skipNum)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("获取地址失败"));
    }

}
