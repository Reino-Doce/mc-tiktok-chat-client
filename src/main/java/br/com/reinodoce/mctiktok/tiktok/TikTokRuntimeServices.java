package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;

/**
 * Runtime side-effect services shared by TikTok event handlers.
 *
 * @param chatGateway rendered event sink
 * @param sessionEventLogger local session logger
 * @param alertService local alert dispatcher
 */
public record TikTokRuntimeServices(
        ChatEventSink chatGateway,
        SessionEventLogger sessionEventLogger,
        AlertService alertService
) {
}
