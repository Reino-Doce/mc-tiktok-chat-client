package br.com.reinodoce.mctiktok.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoticeThrottlerTest {

    @Test
    void suppressesRepeatedKeyInsideCooldown() {
        NoticeThrottler throttler = new NoticeThrottler(Duration.ofSeconds(5));
        Instant now = Instant.parse("2026-03-07T20:00:00Z");

        assertTrue(throttler.shouldEmit("a", now));
        assertFalse(throttler.shouldEmit("a", now.plusSeconds(2)));
        assertTrue(throttler.shouldEmit("a", now.plusSeconds(5)));
    }

    @Test
    void allowsDifferentKeysWithoutWaiting() {
        NoticeThrottler throttler = new NoticeThrottler(Duration.ofSeconds(30));
        Instant now = Instant.parse("2026-03-07T20:00:00Z");

        assertTrue(throttler.shouldEmit("a", now));
        assertTrue(throttler.shouldEmit("b", now.plusSeconds(1)));
    }
}
