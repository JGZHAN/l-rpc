package consumer;

import cn.jgzhan.lrpc.client.RequestProxy;
import cn.jgzhan.lrpc.client.ServiceTable;
import cn.jgzhan.lrpc.common.config.Config;
import cn.jgzhan.lrpc.common.dto.Pair;
import cn.jgzhan.lrpc.common.exception.LRPCTimeOutException;
import cn.jgzhan.lrpc.example.api.TestService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.concurrent.Executors;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/11
 */
@Slf4j
@SpringBootTest
public class ClientTest {

    @Resource
    private RequestProxy requestProxy = new RequestProxy();

    @Autowired
    private ServiceTable serviceTable = new ServiceTable();

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
        serviceTable.init();
        final var service = requestProxy.getProxy(TestService.class, Set.of(Pair.of("127.0.0.1", Config.Server.port())));
//        final var service = requestProxy.getProxy(TestService.class);
//        final var result = service.hello("张三");
//        final var scanner = new Scanner(System.in);
//        while (true) {
//            System.out.println("是否开始测试？");
//            final var next = scanner.next();
//            if ("y".equals(next)) {
//                break;
//            }
//        }
        final var executorService = Executors.newVirtualThreadPerTaskExecutor();
//        final var executorService = Executors.newCachedThreadPool();

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            executorService.execute(() -> {
//            Thread.ofVirtual().start(() -> {
                log.info("开始测试{}", finalI);
                final var result = service.hello("张三" + finalI);
                log.info("测试结果{}", result);
//            });
        }
        executorService.close();
        serviceTable.stop();
        System.out.println("测试结束");
    }
}
