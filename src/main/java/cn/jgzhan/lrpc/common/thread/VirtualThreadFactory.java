package cn.jgzhan.lrpc.common.thread;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author jgzhan
 * @version 1.0
 * @date 2024/12/6
 */
public class VirtualThreadFactory extends DefaultThreadFactory {
    public VirtualThreadFactory(Class<?> poolType) {
        super(poolType);
    }

    @Override
    protected Thread newThread(Runnable r, String name) {
        return new PoolVirtualThread(threadGroup, r, name);
    }
}
