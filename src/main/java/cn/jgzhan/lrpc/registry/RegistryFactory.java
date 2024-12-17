package cn.jgzhan.lrpc.registry;

import cn.jgzhan.lrpc.common.config.Config;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/17
 */
@Slf4j
public class RegistryFactory {

    private static RegistryCenter REGISTRY_CENTER;

    public static RegistryCenter getRegistry() {
        if (REGISTRY_CENTER != null) {
            return REGISTRY_CENTER;
        }
        synchronized (RegistryFactory.class) {
            if (REGISTRY_CENTER == null) {
                final var registryType = Config.Registry.type();
                switch (registryType) {
                    case ZOOKEEPER -> REGISTRY_CENTER = new ZookeeperRegistryCenter(){{log.info("使用Zookeeper注册中心");}};
                    case LOCAL -> REGISTRY_CENTER = new LocalRegistryCenter(){{log.info("使用本地注册中心, 直连模式");}};
                    default -> throw new RuntimeException("不支持的注册中心类型");
                }
            }
        }
        return REGISTRY_CENTER;
    }
}
