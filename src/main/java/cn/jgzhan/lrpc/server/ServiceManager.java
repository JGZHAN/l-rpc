package cn.jgzhan.lrpc.server;

import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.util.SingletonUtils;
import cn.jgzhan.lrpc.registry.RegistryCenter;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/19
 */
public class ServiceManager {
    private static final Map<Class<?>, Object> INTERFACE_PROVIDER_MAP = new ConcurrentHashMap<>();

    private final RegistryCenter registryCenter;
    private final LrpcServerBootstrap serverBootstrap;

    public ServiceManager() {
        this.registryCenter = RegistryFactory.getRegistry();
        this.serverBootstrap = SingletonUtils.getSingletonInstance(LrpcServerBootstrap.class);
        serverBootstrap.start();
    }

    public <T> void registry(T instance) {
        final Class<?> interfaceClz = getInterface(instance);
        registryCenter.registerService(interfaceClz);
        INTERFACE_PROVIDER_MAP.put(interfaceClz, instance);
    }

    public Object getService(String interfaceName) {
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("接口名错误");
        }

        return serviceClass.cast(INTERFACE_PROVIDER_MAP.get(serviceClass));
    }

    @SneakyThrows
    public void close() {
        registryCenter.close();
        serverBootstrap.stop();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> getInterface(T instance) {
        final var interfaces = instance.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new RuntimeException("没有实现接口");
        }
        return (Class<? extends T>) interfaces[0];
    }

}
