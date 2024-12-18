package cn.jgzhan.lrpc.client;

import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.dto.Provider;
import cn.jgzhan.lrpc.registry.Change;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import io.netty.channel.pool.FixedChannelPool;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static cn.jgzhan.lrpc.client.ServiceTable.ADDRESS_POOL_MAP;
import static cn.jgzhan.lrpc.client.ServiceTable.SERVICE_ADDRESS_MAP;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
public class Consumer {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);


    public static Set<Pair<String, Integer>> getServiceAddress(Method method) {
        return putToAddressMap(method);
    }

    @NonNull
    public static Set<Pair<String, Integer>> initAndWatchServiceAddress(@NonNull Map<Class<?>, Set<String>> clzAddressMap) {
        doInitAndWatch(clzAddressMap);
        return SERVICE_ADDRESS_MAP.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private static void doInitAndWatch(@NonNull Map<Class<?>, Set<String>> clzAddressMap) {

        if (!clzAddressMap.isEmpty()) {
            for (var entry : clzAddressMap.entrySet()) {
                // 获取所有的方法
                final var methods = entry.getKey().getDeclaredMethods();
                for (var method : methods) {
                    // 从注册中心获取服务提供者
                    final var addresses = entry.getValue().stream()
                            .map(address -> {
                                final String[] split = address.split(":");
                                if (split.length != 2) {
                                    throw new IllegalArgumentException("地址格式错误, 格式为: ip:port");
                                }
                                return new Pair<>(split[0], Integer.parseInt(split[1]));
                            })
                            .collect(Collectors.toSet());
                    putToAddressMap(method, addresses);
                }
            }
        }

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

    public static Set<Pair<String, Integer>> putToAddressMap(Method method) {
        return SERVICE_ADDRESS_MAP.computeIfAbsent(method.toString(),
                _ -> new CopyOnWriteArraySet<>(RegistryFactory.getRegistry().getService(method)));
    }

    public static Set<Pair<String, Integer>> putToAddressMap(Method method, Set<Pair<String, Integer>> addresses) {
        final var computeIfAbsent = SERVICE_ADDRESS_MAP.computeIfAbsent(method.toString(), _ -> new CopyOnWriteArraySet<>());
        computeIfAbsent.addAll(addresses);
        return computeIfAbsent;
    }

    private static void addOrUpdateServiceAddress(String methodStr, Pair<String, Integer> address) {
        SERVICE_ADDRESS_MAP.computeIfAbsent(methodStr, _ -> new CopyOnWriteArraySet<>())
                .add(address);
    }

    private static void deleteServiceAddress(Provider provider) {
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

    private static void updateServiceAddress(Provider provider) {
        log.info("更新服务提供者: {}", provider);
        addOrUpdateServiceAddress(provider.getServiceName(), provider.getAddress());
    }

    private static void addServiceAddress(Provider provider) {
        log.info("新增服务提供者: {}", provider);
        addOrUpdateServiceAddress(provider.getServiceName(), provider.getAddress());
    }

}
