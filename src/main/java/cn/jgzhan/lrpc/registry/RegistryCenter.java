package cn.jgzhan.lrpc.registry;

import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.dto.Provider;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/17
 */
public interface RegistryCenter extends AutoCloseable {

    /**
     * 服务注册
     *
     * @param clz 类
     */
    default void registerService(Class clz) {
        for (var declaredMethod : clz.getDeclaredMethods()) {
            this.registerService(declaredMethod);
        }
    }

    /**
     * 服务注册
     *
     * @param method 方法
     */
    void registerService(Method method);

    /**
     * 发现服务
     *
     * @param service 服务名称
     * @return 服务地址
     */
    Set<Pair<String, Integer>> getService(Method service);

    /**
     * 监听服务
     *
     * @param changeListener 服务变化监听器
     */
    void watch(BiConsumer<Change, Provider> changeListener);


    default String getHost() {
        // 获取本机的公网ip
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return localHost.getHostAddress();
    }

}
