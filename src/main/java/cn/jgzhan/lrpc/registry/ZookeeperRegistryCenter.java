package cn.jgzhan.lrpc.registry;

import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.dto.Provider;
import lombok.NonNull;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.jgzhan.lrpc.common.constant.RegistryConstant.PROVIDER;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
public class ZookeeperRegistryCenter implements RegistryCenter {
    private static final Logger log = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);

    private static final Pattern SERVICE_NODE_PATTERN = Pattern.compile("^/provider/([^/]+)/([^:]+):(\\d+)$");

    private static CuratorFramework client;

    {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 10);
        final var builder = CuratorFrameworkFactory.builder()
                .retryPolicy(retryPolicy)
                .sessionTimeoutMs(60 * 1000)
                .connectionTimeoutMs(15 * 1000)
                .connectString(Config.Registry.Zookeeper.address())
                .namespace(Config.Registry.Zookeeper.rootPath());
        if (Config.Registry.Zookeeper.account() != null && Config.Registry.Zookeeper.passWord() != null) {
            builder.authorization(Config.Registry.Zookeeper.account(), Config.Registry.Zookeeper.passWord());
        }
        client = builder.build();
        client.start();
        // 创建根节点
        try {
            // 判断根节点是否存在
            if (client.checkExists().forPath(PROVIDER) == null) {
                create(PROVIDER, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            log.error("创建根节点失败", e);
        }
    }

    @Override
    public void registerService(Method method) {
        String host = this.getHost();
        String providerAddress = method.toString() + "/" + host + ":" + Config.Server.port();
        try {
            this.create(String.join("/", PROVIDER, providerAddress));
            log.info("LRPC 服务提供者注册成功: {}", providerAddress);
        } catch (KeeperException.NodeExistsException e) {
            log.error("服务提供者已存在", e);
            // 将其删除
            try {
                this.delete(String.join("/", PROVIDER, providerAddress));
            } catch (Exception ex) {
                log.error("删除服务提供者失败", ex);
            }
            // 重新注册
            registerService(method);
        } catch (Exception e) {
            log.error("注册服务提供者失败", e);
        }
    }

    @Override
    public Set<Pair<String, Integer>> getService(Method service) {
        Set<String> nodes;
        try {
            nodes = this.getChildrenNode(String.join("/", PROVIDER, service.toString()));
        } catch (Exception e) {
            log.error("获取服务提供者失败", e);
            return Collections.emptySet();
        }
        if (nodes.isEmpty()) {
            return Collections.emptySet();
        }
        return nodes.stream()
                .map(node -> {
                    final String[] split = node.split(":");
                    return new Pair<>(split[0], Integer.parseInt(split[1]));
                })
                .collect(Collectors.toSet());
    }

    @Override
    public void watch(BiConsumer<Change, Provider> changeListener) {
        try {
            this.watch(PROVIDER, (type, oldData, data) -> {
                switch (type) {
                    case NODE_CREATED ->
                            convertToProviderOpt(data).ifPresent(provider -> changeListener.accept(Change.ADD, provider));

                    case NODE_CHANGED ->
                            convertToProviderOpt(data).ifPresent(provider -> changeListener.accept(Change.UPDATE, provider));

                    case NODE_DELETED ->
                            convertToProviderOpt(oldData).ifPresent(provider -> changeListener.accept(Change.REMOVE, provider));
                }
            });
        } catch (Exception e) {
            log.error("监听注册中心失败", e);
        }
    }

    @Override
    public void close() {
        client.close();
        log.info("关闭注册中心成功");
    }

    @NonNull
    private static Optional<Provider> convertToProviderOpt(@NonNull ChildData data) {
        final String path = data.getPath();
        final Matcher matcher = SERVICE_NODE_PATTERN.matcher(path);

        if (matcher.matches()) {
            String serviceName = matcher.group(1);
            String host = matcher.group(2);
            int port = Integer.parseInt(matcher.group(3));
            return Optional.of(new Provider(new Pair<>(host, port), serviceName));
        }
        return Optional.empty();
    }

    public void watch(String path, CuratorCacheListener listener) throws Exception {
        CuratorCache cache = CuratorCache.build(client, path);
        cache.listenable().addListener(listener);
        cache.start();
    }


    /**
     * 创建节点, 默认为临时节点
     *
     * @param path
     * @throws Exception
     */
    private void create(String path) throws Exception {
        create(path, CreateMode.EPHEMERAL);
    }

    private static void create(String path, CreateMode mode) throws Exception {
        client.create()
                .creatingParentsIfNeeded()
                .withMode(mode)
                .forPath(path);
    }

    private void delete(String path) throws Exception {
        client.delete()
                .forPath(path);
    }

    private Set<String> getChildrenNode(String path) {
        final List<String> nodes;
        try {
            nodes = client.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            log.warn("节点不存在子节点: {}", path);
            return Collections.emptySet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new HashSet<>(nodes);

    }
}
