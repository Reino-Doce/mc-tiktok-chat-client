package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;

/**
 * Runtime side-effect services shared by TikTok event handlers.
 *
 * @param chatGateway rendered event sink
 * @param sessionEventLogger local session logger
 */
public record TikTokRuntimeServices(ChatEventSink chatGateway, SessionEventLogger sessionEventLogger) {
}
