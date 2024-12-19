package cn.jgzhan.lrpc.client;

import cn.jgzhan.lrpc.client.loadbalance.LoadBalanceFactory;
import cn.jgzhan.lrpc.client.loadbalance.LoadBalancer;
import cn.jgzhan.lrpc.client.net.ChannelManager;
import cn.jgzhan.lrpc.client.net.LrpcChannelPoolFactory;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Message;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.dto.Provider;
import cn.jgzhan.lrpc.common.dto.RpcRequestMessage;
import cn.jgzhan.lrpc.common.exception.LRPCTimeOutException;
import cn.jgzhan.lrpc.common.handler.RpcRespHandler;
import cn.jgzhan.lrpc.common.util.SingletonUtils;
import cn.jgzhan.lrpc.registry.Change;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;


/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
public class ConsumerManager {

    private static final Logger log = LoggerFactory.getLogger(ConsumerManager.class);

    // 本地缓存连接池
    private static Map<String, FixedChannelPool> ADDRESS_POOL_MAP;
    // 本地缓存注册中心的服务提供者
    private static Map<String, Set<Pair<String, Integer>>> SERVICE_ADDRESS_MAP;

    private final LoadBalancer loadBalancer;

    private final ChannelManager channelManager = SingletonUtils.getSingletonInstance(ChannelManager.class);


    public ConsumerManager() {
        ADDRESS_POOL_MAP = new ConcurrentHashMap<>();
        SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
        loadBalancer = LoadBalanceFactory.byType(Config.loadBalance());
        watch();
    }

    // 监听注册中心的变化
    public void watch() {

        // 观察者模式，监听注册中心的变化
        final var registry = RegistryFactory.getRegistry();
        registry.watch((change, provider) -> {
            switch (change) {
                case Change.ADD -> addServiceAddress(provider);
                case Change.UPDATE -> updateServiceAddress(provider);
                case Change.REMOVE -> deleteServiceAddress(provider);
            }
        });
    }

    // 关闭
    public void stop() {
        try {
            ADDRESS_POOL_MAP.values().forEach(ChannelPool::close);
            log.info("关闭消费者连接池成功");
        } catch (Exception e) {
            log.error("关闭消费者连接池失败", e);
            throw new RuntimeException(e);
        }
    }

    // 发送请求
    public Promise<Object> send(RpcRequestMessage msg, Method method, Set<Pair<String, Integer>> addressSet) throws LRPCTimeOutException {
        final Function<Channel, Promise<Object>> channelExeFunction = channelExeFunction(msg);
        // 负载均衡选择服务地址
        final var address = clazzToAddress(method, addressSet);
        // 获取连接池
        final var channelPool = getChannelPool(address);
        // 在连接池中执行请求
        return channelManager.executeWithChannelPool(channelPool, channelExeFunction);
    }

    // 获取服务地址
    public Set<Pair<String, Integer>> getServiceAddress(Method method) {
        return putToAddressMap(method);
    }

    // 将服务地址放入缓存
    public Set<Pair<String, Integer>> putToAddressMap(Method method) {
        return SERVICE_ADDRESS_MAP.computeIfAbsent(method.toString(),
                _ -> new CopyOnWriteArraySet<>(RegistryFactory.getRegistry().getService(method)));
    }

    private void addOrUpdateServiceAddress(String methodStr, Pair<String, Integer> address) {
        SERVICE_ADDRESS_MAP.computeIfAbsent(methodStr, _ -> new CopyOnWriteArraySet<>())
                .add(address);
    }

    private void deleteServiceAddress(Provider provider) {
        log.info("删除服务提供者: {}", provider);
        final var serviceName = provider.getServiceName();
        final var address = provider.getAddress();
        final Set<Pair<String, Integer>> addresses = SERVICE_ADDRESS_MAP.get(serviceName);
        if (addresses != null) {
            addresses.remove(address);
        }
        FixedChannelPool pool = ADDRESS_POOL_MAP.remove(address.left + address.right);
        if (pool != null) {
            pool.close();
        }
    }

    private void updateServiceAddress(Provider provider) {
        log.info("更新服务提供者: {}", provider);
        addOrUpdateServiceAddress(provider.getServiceName(), provider.getAddress());
    }

    private void addServiceAddress(Provider provider) {
        log.info("新增服务提供者: {}", provider);
        addOrUpdateServiceAddress(provider.getServiceName(), provider.getAddress());
    }

    private static GenericFutureListener<Future<? super Void>> processAftermath(DefaultPromise<Object> promise, Message msg) {
        return future -> {
            log.info("发送请求结束 {}", JSON.toJSON(msg));
            if (future.isSuccess()) {
                return;
            }
            log.error("发送请求失败", future.cause());
            promise.setFailure(future.cause());
        };
    }

    // 选择服务地址，负载均衡
    private Pair<String, Integer> clazzToAddress(Method method, Set<Pair<String, Integer>> addressSet) {
        if (addressSet != null && !addressSet.isEmpty()) {
            // 若指定了服务地址，则在指定的服务地址中选择
            return loadBalancer.selectServiceAddress(addressSet);
        }
        // 若未指定服务地址，则在注册中心的服务地址中选择
        return loadBalancer.selectServiceAddress(method);
    }

    private static FixedChannelPool getChannelPool(Pair<String, Integer> address) {
        final var host = address.left;
        final var port = address.right;
        return ADDRESS_POOL_MAP.computeIfAbsent(host + port,
                _ -> LrpcChannelPoolFactory.createFixedChannelPool(host, port));
    }

    private static Function<Channel, Promise<Object>> channelExeFunction(RpcRequestMessage msg) {
        // 发送请求，且处理写失败
        return channel -> {
            final var promise = new DefaultPromise<>(channel.eventLoop());
            RpcRespHandler.addPromise(msg.getMessageId(), promise);
            // 发送请求，且处理写失败
            final var channelFuture = channel.writeAndFlush(msg);
            channelFuture.addListener(processAftermath(promise, msg));
            return promise;
        };
    }

}
