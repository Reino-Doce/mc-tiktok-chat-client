package br.com.reinodoce.mctiktok.tiktok;

import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveOfflineHostException;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveUnknownHostException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikTokClientFacadePolicyTest {

    @Test
    void unknownHostIsTerminalButOfflineAndGenericFailuresRetry() {
        assertFalse(TikTokClientFacade.shouldRetryConnectFailure(new TikTokLiveUnknownHostException("unknown", null, null)));
        assertTrue(TikTokClientFacade.shouldRetryConnectFailure(new TikTokLiveOfflineHostException("offline", null, null)));
        assertTrue(TikTokClientFacade.shouldRetryConnectFailure(new RuntimeException("Invalid status code received: 200 Status line: HTTP/1.1 200 OK")));
    }
}
