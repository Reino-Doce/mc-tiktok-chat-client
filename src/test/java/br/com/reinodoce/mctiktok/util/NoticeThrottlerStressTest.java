package br.com.reinodoce.mctiktok.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoticeThrottlerStressTest {

    @Test
    void reconnectNoticeAtOneSecondIntervalIsThrottled() {
        NoticeThrottler throttler = new NoticeThrottler(Duration.ofSeconds(5));
        Instant start = Instant.parse("2026-03-07T21:00:00Z");

        int emitted = 0;
        int total = 60;
        for (int i = 0; i < total; i++) {
            if (throttler.shouldEmit("streamer:falha de conexao", start.plusSeconds(i))) {
                emitted++;
            }
        }

        assertEquals(12, emitted, "Com cooldown de 5s, 60 eventos em 1s devem gerar 12 avisos.");
        assertTrue(emitted < total / 2);
    }

    @Test
    void errorNoticeBurstIsThrottledByKey() {
        NoticeThrottler throttler = new NoticeThrottler(Duration.ofSeconds(8));
        Instant start = Instant.parse("2026-03-07T21:00:00Z");

        int emitted = 0;
        int total = 100;
        for (int i = 0; i < total; i++) {
            Instant at = start.plusMillis(i * 100L);
            if (throttler.shouldEmit("runtime:TimeoutException", at)) {
                emitted++;
            }
        }

        assertEquals(2, emitted, "Burst de 10s deve emitir so no inicio e apos 8s.");
        assertTrue(emitted <= 2);
    }
}
