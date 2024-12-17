package cn.jgzhan.lrpc.common.handler;

import cn.jgzhan.lrpc.common.dto.RpcRequestMessage;
import cn.jgzhan.lrpc.common.dto.RpcResponseMessage;
import cn.jgzhan.lrpc.server.service.ServiceFactory;
import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/4
 */
@ChannelHandler.Sharable
@Slf4j
public class RpcReqHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) throws Exception {

//        log.info("接收到消息 {}", JSON.toJSON(msg));
        final var interfaceName = msg.getInterfaceName();
        final var methodName = msg.getMethodName();

        final var service = ServiceFactory.getService(interfaceName);

        final var response = new RpcResponseMessage();
        response.setMessageId(msg.getMessageId());
        try {
            final Class<?> aClass = service.getClass();
            final var method = aClass.getMethod(methodName, msg.getParameterTypes());
            final var result = method.invoke(service, msg.getParameterValues());
            response.setReturnValue(result);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("e : ", e);
            response.setExceptionValue(new Error(e.getCause().getMessage()));
        }
        ctx.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                log.info("消息响应成功 {}", JSON.toJSON(msg));
                return;
            }
            log.error("发送消息时有错误发生: ", future.cause());
        });
    }

}
