package cn.jgzhan.lrpc.common.config;


import cn.jgzhan.lrpc.client.loadbalance.LoadBalancerType;
import cn.jgzhan.lrpc.common.serializer.SerializerImpl;
import cn.jgzhan.lrpc.registry.RegistryType;

import java.util.Properties;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/3
 */
public class Config {
    static final Properties PROPERTIES;

    // 静态代码块，加载配置文件，文件固定为resource目录下的lrpc_config.properties
    static {
        try (final var resourceAsStream = Config.class.getClassLoader().getResourceAsStream("lrpc_config.properties")) {
            PROPERTIES = new Properties();
            PROPERTIES.load(resourceAsStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取序列化算法（默认为JSON）
    public static SerializerImpl serializerAlgorithm() {
        final String serializerEnumName = PROPERTIES.getProperty("serializer.algorithm", "JSON");
        return SerializerImpl.valueOf(serializerEnumName);
    }

    // 获取消费者扫描的包
    public static String consumerScanPackage() {
        return PROPERTIES.getProperty("consumer.scan.package");
    }

    public static String providerScanPackage() {
        return PROPERTIES.getProperty("provider.scan.package");
    }

    // 获取负载均衡算法，默认为轮询
    public static LoadBalancerType loadBalance() {
        return LoadBalancerType.valueOf(PROPERTIES.getProperty("loadbalance.type", "ROUND_ROBIN"));
    }


    //注册中心
    public static class Registry {

        public static RegistryType type() {
            return RegistryType.valueOf(PROPERTIES.getProperty("registry.type", "LOCAL"));
        }

        /**
         * 获取zk注册中心的地址
         */
        public static class Zookeeper {
            public static String address() {
                return PROPERTIES.getProperty("registry.zk.address");
            }

            public static String account() {
                return PROPERTIES.getProperty("registry.zk.account");
            }

            public static byte[] passWord() {
                var pw = PROPERTIES.getProperty("registry.zk.password");
                return pw == null ? null : pw.getBytes();
            }

            public static String rootPath() {
                return PROPERTIES.getProperty("registry.zk.path", "lrpc");
            }
        }
    }

    /**
     * 获取server信息
     */
    public static class Server {
        public static int port() {
            return Integer.parseInt(PROPERTIES.getProperty("server.port", "23923"));
        }

        public static int workerMax() {
            return Integer.parseInt(PROPERTIES.getProperty("server.worker.max", "1000"));
        }
    }
    /**
     * 获取client信息
     */
    public static class Client {

        public static int addressMaxConnection() {
            return Integer.parseInt(PROPERTIES.getProperty("client.address.max.connection", "1000"));
        }
    }
}
