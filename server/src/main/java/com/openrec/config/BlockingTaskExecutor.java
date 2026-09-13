package com.openrec.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Moves synchronous controller work off Netty event-loop threads with bounded admission. */
@Component
public class BlockingTaskExecutor {

    private final ThreadPoolExecutor executor;
    private final Scheduler scheduler;

    public BlockingTaskExecutor(@Value("${server.blocking.threads:32}") int threads,
        @Value("${server.blocking.queue-capacity:256}") int queueCapacity) {
        this.executor =
            new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueCapacity),
                new ThreadFactoryBuilder().setNameFormat("blocking-api-pool-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
        this.scheduler = Schedulers.fromExecutorService(executor);
    }

    public <T> Mono<T> submit(Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(scheduler);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.dispose();
    }
}
