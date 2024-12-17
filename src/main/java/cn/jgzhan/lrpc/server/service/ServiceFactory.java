package cn.jgzhan.lrpc.server.service;

import cn.jgzhan.lrpc.common.annotation.AnnotationScanner;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.registry.RegistryFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/4
 */
public class ServiceFactory {
    static Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    public static void registry() {
        final var classSet = AnnotationScanner.getClasses(Config.providerScanPackage(), Thread.currentThread().getContextClassLoader());
        final Set<Pair<Class<?>, Object>> providerSet = AnnotationScanner.groupByClassAndName(classSet);
        for (Pair<Class<?>, Object> provider : providerSet) {
            final Class<?> clz = provider.left;
            map.put(clz, provider.right);
            // 获取所有的方法
            final var methods = clz.getDeclaredMethods();
            for (var method : methods) {
                RegistryFactory.getRegistry().registerService(method, Config.Server.port());
            }
        }
    }

    public static Object getService(String interfaceName) {
        Class<?> serviceClass;
        try {
            serviceClass = Class.forName(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("接口名错误");
        }

        return serviceClass.cast(map.get(serviceClass));
    }

}
