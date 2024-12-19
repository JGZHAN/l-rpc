package cn.jgzhan.lrpc.server;

import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.config.HandlerConfig;
import cn.jgzhan.lrpc.common.group.VirtualThreadNioEventLoopGroup;
import cn.jgzhan.lrpc.registry.RegistryFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/10
 */
public class LrpcServerBootstrap {
    private static final Logger log = LoggerFactory.getLogger(LrpcServerBootstrap.class);

    private static ServerBootstrap bootstrap;

    public void start() {
        // 1. 创建一个服务端对象
        bootstrap = new ServerBootstrap();
        final var boss = new NioEventLoopGroup(1);
        final var worker = new VirtualThreadNioEventLoopGroup(Config.Server.workerMax());
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
            log.info("服务启动成功, 端口: {}", Config.Server.port());
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

    public void stop() {
        if (bootstrap == null) {
            log.error("服务未启动");
            return;
        }
        bootstrap.config().group().shutdownGracefully().addListener(future -> {
            if (future.isSuccess()) {
                log.info("Lrpc服务端关闭成功");
            } else {
                log.error("Lrpc服务端关闭失败", future.cause());
            }
        });
    }
}
