package cn.jgzhan.lrpc.client;

import cn.jgzhan.lrpc.common.annotation.AnnotationScanner;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.exception.LRPCTimeOutException;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/11
 */
public class ServiceTable implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ServiceTable.class);

    // 本地缓存连接池
    public static final Map<String, FixedChannelPool> ADDRESS_POOL_MAP = new ConcurrentHashMap<>();
    // 本地缓存注册中心的服务提供者
    public static Map<String, Set<Pair<String, Integer>>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();

    private final ChannelManager channelManager = new ChannelManager();

    /**
     * 预热：
     * 1. 项目启动时，加载所有LrpcReference注解的类
     * 2. 通过反射获取所有的方法
     * 3. 将方法名和方法对应的类名去注册中心查询对应的服务提供者
     * 4. 将服务提供者的地址和端口号缓存到本地
     * 5. 给每个服务提供者建立一个连接，放到连接池中，达到预热的目的
     */
    public void init() throws LRPCTimeOutException {
        // 1. 项目启动时，加载所有LrpcReference注解的类

        final Map<Class<?>, Set<String>> reference = getAllLrpcReference();
        final Set<Pair<String, Integer>> serviceAddress = Consumer.initAndWatchServiceAddress(reference);
        for (Pair<String, Integer> address : serviceAddress) {
            channelManager.executeWithChannel(address.left, address.right, channel -> {
                // do nothing
                return null;
            });
        }
    }

    public void stop() {
        ADDRESS_POOL_MAP.values().forEach(ChannelPool::close);
        try {
            RegistryFactory.getRegistry().close();
        } catch (Exception e) {
            log.error("关闭注册中心失败", e);
            throw new RuntimeException(e);
        }
    }


    public Map<Class<?>, Set<String>/*注解上的address*/> getAllLrpcReference() {
        // 扫描
        final var consumerScanPackage = Config.consumerScanPackage();
        final var loader = Thread.currentThread().getContextClassLoader();
        // 扫描consumerScanPackage下所有的类

        final var classSet = AnnotationScanner.getClasses(consumerScanPackage, loader);

        // 获取所有类属性包含LrpcReference注解的属性的类
        return AnnotationScanner.getFieldsForLrpcReference(classSet);
    }


    @Override
    public void close() throws Exception {
        stop();
    }
}
