package com.openrec.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class BlockingTaskExecutorTest {

    @Test
    public void runsBlockingWorkOutsideCallingThread() throws Exception {
        BlockingTaskExecutor executor = new BlockingTaskExecutor(1, 1);
        try {
            String caller = Thread.currentThread().getName();
            AtomicReference<String> worker = new AtomicReference<>();
            CountDownLatch invoked = new CountDownLatch(1);

            executor.submit(() -> {
                worker.set(Thread.currentThread().getName());
                invoked.countDown();
                return "done";
            }).block();

            assertTrue(invoked.await(1, TimeUnit.SECONDS));
            assertFalse(caller.equals(worker.get()));
            assertTrue(worker.get().startsWith("blocking-api-pool-"));
        } finally {
            executor.shutdown();
        }
    }
}
