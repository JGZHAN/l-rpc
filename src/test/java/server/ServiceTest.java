package server;

import cn.jgzhan.lrpc.common.util.SingletonUtils;
import cn.jgzhan.lrpc.example.impl.TestServiceImpl;
import cn.jgzhan.lrpc.server.LrpcServerBootstrap;
import cn.jgzhan.lrpc.server.ServiceManager;
import cn.jgzhan.lrpc.spring.ReferenceBeanPostProcessor;
import org.junit.Test;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/10
 */
public class ServiceTest {


    @Test
    public void testStartServer() throws InterruptedException, IOException {

        final var referenceBeanPostProcessor = new ReferenceBeanPostProcessor();
        referenceBeanPostProcessor.postProcessBeforeInitialization(new TestServiceImpl(), "testService");
        System.in.read();
    }


}
