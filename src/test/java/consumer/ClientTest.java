package consumer;

import cn.jgzhan.lrpc.client.RequestProxyFactory;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.exception.LRPCTimeOutException;
import cn.jgzhan.lrpc.example.api.TestService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/11
 */
public class ClientTest {
    private static final Logger log = LoggerFactory.getLogger(ClientTest.class);

    private RequestProxyFactory requestProxyFactory = new RequestProxyFactory();

    public static void main(String[] args) {
        final var clientTest = new ClientTest();
        try {
            clientTest.testClient();
        } catch (LRPCTimeOutException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testClient() throws LRPCTimeOutException {
//        final var service = requestProxyFactory.getProxy(TestService.class, Set.of(Pair.of("127.0.0.1", Config.Server.port())));
        final var service = requestProxyFactory.getProxy(TestService.class);
        final var result = service.hello("张三");
        System.out.println("测试结束, 结果: " + result);
    }
    @Test
    public void testClientFor() throws LRPCTimeOutException {
        final var service = requestProxyFactory.getProxy(TestService.class, Set.of(Pair.of("127.0.0.1", Config.Server.port())));

        final var executorService = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executorService.execute(() -> {
                log.info("开始测试{}", finalI);
                final var result = service.hello("张三" + finalI);
                log.info("测试结果{}", result);
            });
        }
        executorService.close();
        System.out.println("测试结束");
    }
}
