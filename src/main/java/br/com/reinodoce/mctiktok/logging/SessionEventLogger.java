package br.com.reinodoce.mctiktok.logging;

import br.com.reinodoce.mctiktok.config.ReinodoceConfig;
import br.com.reinodoce.mctiktok.util.ExecutorsFactory;
import br.com.reinodoce.mctiktok.util.ReinodoceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous writer for opt-in local TikTok LIVE session logs.
 */
public final class SessionEventLogger implements AutoCloseable {
    private static final int AWAIT_SECONDS = 5;

    private final Clock clock;
    private final ExecutorService executor;
    private final Thread shutdownHook;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger generationCounter = new AtomicInteger();
    private final SessionLogWriter logWriter;
    private final Object queueLock = new Object();

    private volatile int activeGeneration;

    /**
     * Creates a session event logger.
     *
     * @param logDirectory directory that will contain session log files
     */
    public SessionEventLogger(Path logDirectory) {
        this(
                logDirectory,
                Clock.systemUTC(),
                ExecutorsFactory.newSingleThreadExecutor("reinodoce-session-log"),
                true);
    }

    SessionEventLogger(Path logDirectory, Clock clock, ExecutorService executor) {
        this(logDirectory, clock, executor, false);
    }

    private SessionEventLogger(Path logDirectory, Clock clock, ExecutorService executor, boolean installShutdownHook) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.logWriter = new SessionLogWriter(Objects.requireNonNull(logDirectory, "logDirectory"), clock);
        this.shutdownHook = installShutdownHook ? new Thread(
                () -> close(true),
                "reinodoce-session-log-shutdown") : null;
        if (shutdownHook != null) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    /**
     * Starts a new session log if logging is enabled.
     *
     * @param config runtime configuration
     * @param username connected TikTok username
     */
    public void startSession(ReinodoceConfig config, String username) {
        ReinodoceConfig safeConfig = Objects.requireNonNull(config, "config");
        if (!safeConfig.isSessionLoggingEnabled()) {
            stopSession();
            return;
        }
        SessionLogFormat format = SessionLogFormat.fromString(safeConfig.getSessionLoggingFormat());
        SessionLogPrivacyOptions privacyOptions = SessionLogPrivacyOptions.from(safeConfig);
        String safeUsername = username == null ? "" : username.trim();
        synchronized (queueLock) {
            int generation = activateNewGeneration();
            submit(() -> {
                if (!logWriter.start(generation, format, privacyOptions, safeUsername)
                        && activeGeneration == generation) {
                    activeGeneration = 0;
                }
            });
        }
    }

    /**
     * Applies config updates without rotating an already-open session file.
     *
     * @param config runtime configuration
     * @param connected whether a LIVE session is currently connected
     * @param username connected username
     */
    public void refreshSession(ReinodoceConfig config, boolean connected, String username) {
        ReinodoceConfig safeConfig = Objects.requireNonNull(config, "config");
        if (!connected || !safeConfig.isSessionLoggingEnabled()) {
            stopSession();
            return;
        }
        if (activeGeneration == 0) {
            startSession(safeConfig, username);
        }
    }

    /**
     * Stops the active session log after queued writes complete.
     */
    public void stopSession() {
        synchronized (queueLock) {
            if (activeGeneration == 0) {
                return;
            }
            activeGeneration = 0;
            generationCounter.incrementAndGet();
            submit(logWriter::closeQuietly);
        }
    }

    /**
     * Queues a mirrored event for logging.
     *
     * @param event event to log
     */
    public void log(SessionLogEvent event) {
        synchronized (queueLock) {
            int generation = activeGeneration;
            if (generation == 0 || event == null) {
                return;
            }
            SessionLogRecord record = new SessionLogRecord(Instant.now(clock), event);
            submit(() -> writeOnWorker(generation, record));
        }
    }

    @Override
    public void close() {
        close(false);
    }

    private void close(boolean fromShutdownHook) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        activeGeneration = 0;
        try {
            Future<?> closeFuture = executor.submit(logWriter::closeQuietly);
            closeFuture.get(AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (RejectedExecutionException | TimeoutException exception) {
            ReinodoceLogger.LOGGER.debug("Session log close did not complete cleanly", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            ReinodoceLogger.LOGGER.warn("Failed to close TikTok session log", cause);
        } finally {
            removeShutdownHook(fromShutdownHook);
            executor.shutdown();
        }
    }

    void awaitIdle() throws Exception {
        Future<?> future = executor.submit(() -> {
        });
        future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
    }

    private int activateNewGeneration() {
        int generation = generationCounter.incrementAndGet();
        activeGeneration = generation;
        return generation;
    }

    private void writeOnWorker(int generation, SessionLogRecord record) {
        if (!logWriter.write(generation, record.timestamp(), record.event()) && activeGeneration == generation) {
            activeGeneration = 0;
        }
    }

    private void submit(Runnable task) {
        if (closed.get()) {
            return;
        }
        try {
            executor.execute(task);
        } catch (RejectedExecutionException exception) {
            ReinodoceLogger.LOGGER.debug("Session log task rejected", exception);
        }
    }

    private void removeShutdownHook(boolean fromShutdownHook) {
        if (fromShutdownHook || shutdownHook == null) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException exception) {
            ReinodoceLogger.LOGGER.debug("Shutdown already in progress while removing session log hook", exception);
        }
    }

    private record SessionLogRecord(Instant timestamp, SessionLogEvent event) {
    }
}
