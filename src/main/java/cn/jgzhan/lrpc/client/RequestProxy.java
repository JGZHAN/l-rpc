package cn.jgzhan.lrpc.client;

import cn.jgzhan.lrpc.common.dto.Message;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.dto.RpcRequestMessage;
import com.alibaba.fastjson2.JSON;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static cn.jgzhan.lrpc.common.handler.RpcRespHandler.PROMISES;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/9
 */
@Slf4j
public class RequestProxy {


    private final ChannelManager channelManager = new ChannelManager();

    public <T> T getProxy(Class<T> clazz) {
        return this.getProxy(clazz, null);
    }
    public <T> T getProxy(Class<T> clazz, Set<Pair<String, Integer>> serviceAddress) {
        final var proxyInstance = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (proxy, method, args) ->
                {
                    final Function<Channel, ?> channelExeFunction = channel -> {
                        RpcRequestMessage msg = buildRpcRequestMessage(clazz, method, args);
                        final var promise = new DefaultPromise<>(channel.eventLoop());
                        PROMISES.put(msg.getMessageId(), promise);
                        // 发送请求，且处理写失败
                        final var channelFuture = channel.writeAndFlush(msg);
                        channelFuture.addListener(processAftermath(promise, msg));
                        return getResult(promise);
                    };
                    return channelManager.executeWithChannel(method, serviceAddress, channelExeFunction);
                }
        );
        return clazz.cast(proxyInstance);
    }

    private <T> RpcRequestMessage buildRpcRequestMessage(Class<T> clazz, Method method, Object[] args) {
        final var msg = new RpcRequestMessage();
        msg.setInterfaceName(clazz.getName());
        msg.setMethodName(method.getName());
        msg.setParameterTypes(method.getParameterTypes());
        msg.setParameterValues(args);
        msg.setReturnType(method.getReturnType());
        msg.setMessageId(UUID.randomUUID().hashCode());
        return msg;
    }

    private Object getResult(DefaultPromise<Object> promise) {
        try {
            // 超时等待
            promise.await(5, TimeUnit.SECONDS);
            if (promise.isSuccess()) {
                return promise.getNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(promise.cause());
    }


    private GenericFutureListener<Future<? super Void>> processAftermath(DefaultPromise<Object> promise, Message msg) {
        return future -> {
            log.info("发送请求结束 {}", JSON.toJSON(msg));
            if (future.isSuccess()) {
                return;
            }
            log.error("发送请求失败", future.cause());
            promise.setFailure(future.cause());
        };
    }
}
