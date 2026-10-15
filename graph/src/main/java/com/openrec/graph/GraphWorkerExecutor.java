package com.openrec.graph;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** Process-wide worker configuration; independent of Spring and graph definitions. */
final class GraphWorkerExecutor {

    private GraphWorkerExecutor() {}

    static ExecutorService create(Function<String, String> properties, Function<String, String> environment) {
        String mode = setting(properties, environment, "openrec.graph.executor", "OPENREC_GRAPH_EXECUTOR", "virtual")
            .trim().toLowerCase(Locale.ROOT);
        if ("virtual".equals(mode)) {
            return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("graph-engine-virtual-", 0).factory());
        }
        if (!"platform".equals(mode)) {
            throw new IllegalArgumentException(
                "openrec.graph.executor / OPENREC_GRAPH_EXECUTOR must be virtual or platform: " + mode);
        }
        int threads = positiveInt(
            setting(properties, environment, "openrec.graph.worker.threads", "OPENREC_GRAPH_WORKER_THREADS",
                String.valueOf(Math.max(8, Runtime.getRuntime().availableProcessors() * 4))),
            "openrec.graph.worker.threads");
        int capacity = positiveInt(setting(properties, environment, "openrec.graph.worker.queue-capacity",
            "OPENREC_GRAPH_WORKER_QUEUE_CAPACITY", "1024"), "openrec.graph.worker.queue-capacity");
        return new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity),
            Thread.ofPlatform().daemon().name("graph-engine-pool-", 0).factory(), new ThreadPoolExecutor.AbortPolicy());
    }

    private static String setting(Function<String, String> properties, Function<String, String> environment,
        String property, String variable, String fallback) {
        String value = properties.apply(property);
        if (value != null)
            return value;
        value = environment.apply(variable);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int positiveInt(String value, String property) {
        try {
            int number = Integer.parseInt(value.trim());
            if (number > 0)
                return number;
        } catch (NumberFormatException ignored) {
            // Report the configuration key as well as the invalid value below.
        }
        throw new IllegalArgumentException(property + " must be a positive integer: " + value);
    }
}
