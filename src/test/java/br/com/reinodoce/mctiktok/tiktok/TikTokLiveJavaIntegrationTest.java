package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.util.UsernameValidator;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.models.ConnectionState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TikTokLiveJavaIntegrationTest {
    private static final String USERNAME_PROPERTY = "reinodoce.tiktok.live.username";
    private static final String USERNAME_ENV = "REINODOCE_TIKTOK_LIVE_USERNAME";
    private static final String HTTP_TIMEOUT_PROPERTY = "reinodoce.tiktok.live.httpTimeoutSeconds";
    private static final String CONNECT_TIMEOUT_PROPERTY = "reinodoce.tiktok.live.connectTimeoutSeconds";
    private static final long DEFAULT_HTTP_TIMEOUT_SECONDS = 15L;
    private static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = 45L;
    private static final long CONNECT_POLL_MILLIS = 100L;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 2L;
    private static final long MINIMUM_TIMEOUT_SECONDS = 1L;
    private static final String LANGUAGE_CODE = "pt-BR";

    @Test
    void liveJavaClientConnectsToConfiguredLiveAccount() {
        String username = configuredUsername();
        assumeTrue(!username.isBlank(), TikTokLiveJavaIntegrationTest::skipMessage);
        assertTrue(UsernameValidator.isValid(username), () -> "Invalid TikTok username: " + username);

        long httpTimeoutSeconds = configuredPositiveLong(HTTP_TIMEOUT_PROPERTY, DEFAULT_HTTP_TIMEOUT_SECONDS);
        long connectTimeoutSeconds = configuredPositiveLong(CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT_SECONDS);
        CountDownLatch connected = new CountDownLatch(1);
        LiveClient client = buildClient(username, httpTimeoutSeconds, connected);
        ExecutorService executor = Executors.newSingleThreadExecutor(command -> {
            Thread thread = new Thread(command, "tiktok-live-java-integration");
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<?> future = executor.submit(client::connect);
            awaitConnect(username, connectTimeoutSeconds, connected, future);
            assertTrue(
                    client.getRoomInfo().getConnectionState() == ConnectionState.CONNECTED,
                    () -> "TikTokLiveJava did not reach CONNECTED for @" + username);
        } finally {
            client.disconnect();
            executor.shutdownNow();
            awaitShutdown(executor);
        }
    }

    private static LiveClient buildClient(String username, long httpTimeoutSeconds, CountDownLatch connected) {
        return TikTokLive.newClient(username)
                .configure(settings -> {
                    settings.setRetryOnConnectionFailure(false);
                    settings.setPrintToConsole(false);
                    settings.setLogLevel(Level.SEVERE);
                    settings.setClientLanguage(LANGUAGE_CODE);
                    settings.getHttpSettings().setTimeout(Duration.ofSeconds(httpTimeoutSeconds));
                })
                .onConnected((liveClient, event) -> connected.countDown())
                .build();
    }

    private static void awaitConnect(
            String username,
            long connectTimeoutSeconds,
            CountDownLatch connected,
            Future<?> future
    ) {
        long timeoutAtNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(connectTimeoutSeconds);
        try {
            while (!connected.await(CONNECT_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                failIfConnectTaskFinished(username, future);
                if (System.nanoTime() >= timeoutAtNanos) {
                    future.cancel(true);
                    fail("TikTokLiveJava did not reach CONNECTED for @" + username
                            + " within " + connectTimeoutSeconds + "s");
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for TikTokLiveJava to connect to @" + username, exception);
        }
    }

    private static void failIfConnectTaskFinished(String username, Future<?> future) {
        if (!future.isDone()) {
            return;
        }
        try {
            future.get();
            fail("TikTokLiveJava connect() returned before CONNECTED for @" + username);
        } catch (ExecutionException exception) {
            fail("TikTokLiveJava failed to connect to @" + username + ": " + describe(exception.getCause()),
                    exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for TikTokLiveJava to connect to @" + username, exception);
        }
    }

    private static void awaitShutdown(ExecutorService executor) {
        try {
            executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String configuredUsername() {
        String raw = System.getProperty(USERNAME_PROPERTY);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(USERNAME_ENV);
        }
        return UsernameValidator.normalize(raw);
    }

    private static long configuredPositiveLong(String propertyName, long fallback) {
        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(raw);
            if (parsed < MINIMUM_TIMEOUT_SECONDS) {
                return fail(propertyName + " must be positive: " + raw);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return fail(propertyName + " must be numeric: " + raw, exception);
        }
    }

    private static String describe(Throwable throwable) {
        if (throwable == null) {
            return "unknown failure";
        }

        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getClass().getName() + ": " + cursor.getMessage();
    }

    private static String skipMessage() {
        return "Set -D" + USERNAME_PROPERTY + "=<live-username> or " + USERNAME_ENV
                + " to run the TikTokLiveJava integration test.";
    }
}
