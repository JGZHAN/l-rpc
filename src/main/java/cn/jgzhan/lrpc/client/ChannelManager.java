package cn.jgzhan.lrpc.client;

import cn.jgzhan.lrpc.client.loadbalance.LoadBalanceFactory;
import cn.jgzhan.lrpc.client.pool.LrpcChannelPoolFactory;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.exception.LRPCTimeOutException;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static cn.jgzhan.lrpc.client.ServiceTable.ADDRESS_POOL_MAP;


/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
@Data
public class ChannelManager {

    private static final Logger log = LoggerFactory.getLogger(ChannelManager.class);

    private final LoadBalanceFactory loadBalanceFactory = new LoadBalanceFactory();


    public <R> R executeWithChannel(Method method, Set<Pair<String, Integer>> addressSet, Function<Channel, R> function) throws LRPCTimeOutException {
        if (addressSet == null || addressSet.isEmpty()) {
            return executeWithChannel(method, function);
        }
        Consumer.putToAddressMap(method, addressSet);
        return executeWithChannel(method, function);
    }


    public <R> R executeWithChannel(Method method, Function<Channel, R> function) throws LRPCTimeOutException {
        final var address = clazzToAddress(method);
        return executeWithChannel(address.left, address.right, function);
    }


    public <R> R executeWithChannel(String host, int port, Function<Channel, R> function) throws LRPCTimeOutException {
        final var pool = ADDRESS_POOL_MAP.computeIfAbsent(host + port, _ -> LrpcChannelPoolFactory.createFixedChannelPool(host, port));

        // 1. 从连接池中获取连接，等待超市时间，未获取连接则抛出异常
        final Future<Channel> future = pool.acquire();
        Channel channel;
        try {
            final boolean acquired = future.await(3, TimeUnit.SECONDS);
            if (!acquired) {
                log.error("获取连接超时");
                throw new LRPCTimeOutException("获取连接超时");
            }
            channel = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        final var result = function.apply(channel);
        pool.release(channel);
        return result;
    }

    // 选择服务地址，负载均衡
    private Pair<String, Integer> clazzToAddress(Method method) {
        final var loadBalancer = loadBalanceFactory.byType(Config.loadBalance());
        return loadBalancer.selectServiceAddress(method);
    }
}
