package cn.jgzhan.lrpc.spring;

import cn.jgzhan.lrpc.client.ConsumerManager;
import cn.jgzhan.lrpc.client.RequestProxyFactory;
import cn.jgzhan.lrpc.common.annotation.LrpcReference;
import cn.jgzhan.lrpc.common.annotation.LrpcService;
import cn.jgzhan.lrpc.common.util.AddressUtils;
import cn.jgzhan.lrpc.common.util.FieldUtils;
import cn.jgzhan.lrpc.common.util.SingletonUtils;
import cn.jgzhan.lrpc.server.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/18
 */
@Component
public class ReferenceBeanPostProcessor implements BeanPostProcessor, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ReferenceBeanPostProcessor.class);

    private final ServiceManager serviceManager;
    private final ConsumerManager consumerManager;

    public ReferenceBeanPostProcessor() {
        this.serviceManager = SingletonUtils.getSingletonInstance(ServiceManager.class);
        // 客户端使用的远程服务缓存
        this.consumerManager = SingletonUtils.getSingletonInstance(ConsumerManager.class);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(LrpcService.class)) {
            log.info("{} 类被注解为服务提供者, benaName: {}", bean.getClass().getName(), beanName);
            serviceManager.registry(bean);
        }
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
            // 生成代理对象
            final Object proxy = RequestProxyFactory.getProxy(field.getType(), addressSet);
            FieldUtils.setField(bean, field, proxy);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public void destroy() {
        consumerManager.stop();
        serviceManager.close();
    }
}
