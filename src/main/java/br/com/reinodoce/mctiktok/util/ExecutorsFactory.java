package br.com.reinodoce.mctiktok.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for named daemon executors used by client background work.
 */
public final class ExecutorsFactory {
    private ExecutorsFactory() {
    }

    /**
     * Creates a single-thread daemon executor with numbered thread names.
     *
     * @param namePrefix thread name prefix
     * @return single-thread executor
     */
    public static ExecutorService newSingleThreadExecutor(String namePrefix) {
        return Executors.newSingleThreadExecutor(daemonFactory(namePrefix));
    }

    /**
     * Creates a single-thread daemon scheduled executor with numbered thread names.
     *
     * @param namePrefix thread name prefix
     * @return single-thread scheduled executor
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String namePrefix) {
        return Executors.newSingleThreadScheduledExecutor(daemonFactory(namePrefix));
    }

    private static ThreadFactory daemonFactory(String namePrefix) {
        AtomicInteger index = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, namePrefix + "-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
