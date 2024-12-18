package cn.jgzhan.lrpc.spring;

import cn.jgzhan.lrpc.client.RequestProxy;
import cn.jgzhan.lrpc.client.ServiceTable;
import cn.jgzhan.lrpc.common.annotation.LrpcReference;
import cn.jgzhan.lrpc.common.util.AddressUtils;
import cn.jgzhan.lrpc.common.util.FieldUtils;
import cn.jgzhan.lrpc.common.util.SingletonUtils;
import cn.jgzhan.lrpc.server.LrpcServerBootstrap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/18
 */
// 再加一个容器关闭的时候关闭连接池和注册中心
@Component
public class ReferenceBeanPostProcessor implements BeanPostProcessor, DisposableBean {
//    private static final Logger log = LoggerFactory.getLogger(ReferenceBeanPostProcessor.class);

    private final LrpcServerBootstrap serverBootstrap;
    private final ServiceTable serviceTable;
    private final RequestProxy requestProxy;

    public ReferenceBeanPostProcessor() {
        this.serverBootstrap = SingletonUtils.getSingletonInstance(LrpcServerBootstrap.class);
        this.serviceTable = SingletonUtils.getSingletonInstance(ServiceTable.class);
        this.requestProxy = SingletonUtils.getSingletonInstance(RequestProxy.class);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        final var fields = bean.getClass().getDeclaredFields();
        for (var field : fields) {
            if (!field.isAnnotationPresent(LrpcReference.class)) {
                continue;
            }
            final var annotation = field.getAnnotation(LrpcReference.class);
            final var addressSet = AddressUtils.toAddressPair(annotation.addressArr());
            final Object proxy = requestProxy.getProxy(field.getType(), addressSet);
            FieldUtils.setField(bean, field, proxy);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public void destroy() {
        serviceTable.stop();
        serverBootstrap.stop();
    }
}
