package br.com.reinodoce.mctiktok.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecutorsFactory {
    private ExecutorsFactory() {
    }

    public static ExecutorService newSingleThreadExecutor(String namePrefix) {
        return Executors.newSingleThreadExecutor(daemonFactory(namePrefix));
    }

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
