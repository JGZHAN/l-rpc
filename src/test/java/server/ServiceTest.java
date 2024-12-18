package server;

import cn.jgzhan.lrpc.server.LrpcServerBootstrap;
import org.junit.Test;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/10
 */
public class ServiceTest {


    public static void main(String[] args) {
        ServiceTest serviceTest = new ServiceTest();
        try {
            serviceTest.testStartServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testStartServer() throws InterruptedException, IOException {
        final var scanner = new Scanner(System.in);
//        LrpcServerBootstrap.start();
        Thread.sleep(3000);
        while (true) {
            System.out.println("是否结束测试？");
            final var next = scanner.next();
            if ("y".equals(next)) {
                break;
            }
        }
//        LrpcServerBootstrap.stop();
    }


}
