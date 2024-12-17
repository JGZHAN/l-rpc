package cn.jgzhan.lrpc.server;

import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.config.HandlerConfig;
import cn.jgzhan.lrpc.common.group.VirtualThreadNioEventLoopGroup;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import cn.jgzhan.lrpc.server.service.ServiceFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/10
 */
@Slf4j
public class LrpcServerBootstrap {

    private static ServerBootstrap bootstrap;

    public static void start() {
        // 1. 创建一个服务端对象
        bootstrap = new ServerBootstrap();
        final var boss = new NioEventLoopGroup(1);
        final var worker = new VirtualThreadNioEventLoopGroup(Config.Server.workerMax());
//        final var worker = new NioEventLoopGroup(Config.getServerWorkerMax());
        bootstrap.group(boss, worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                final var pipeline = ch.pipeline();
                pipeline.addLast(HandlerConfig.getStickyPackHalfPackHandler());
//                pipeline.addLast(HandlerConfig.getLoggingHandler());
                pipeline.addLast(HandlerConfig.getRpcDecoder());
                pipeline.addLast(HandlerConfig.getRpcReqHandler());
            }
        });
        // 2. 启动服务端
        final var future = bootstrap.bind(Config.Server.port());
        // 3. 服务端开始监听
        try {
            future.sync();
            log.info("服务启动成功，准备注册服务");
            ServiceFactory.registry();
        } catch (InterruptedException e) {
            log.error("服务启动失败");
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
                RegistryFactory.getRegistry().close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }

    }

    public static void stop() {
        if (bootstrap == null) {
            log.error("服务未启动");
            return;
        }
        bootstrap.config().group().shutdownGracefully();
        try {
            RegistryFactory.getRegistry().close();
        } catch (Exception e) {
            log.error("关闭注册中心失败", e);
            throw new RuntimeException(e);
        }
    }
}
