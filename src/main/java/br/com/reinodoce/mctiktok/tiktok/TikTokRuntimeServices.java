package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.LanguageSetting;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;

import java.util.function.Supplier;

/**
 * Runtime side-effect services shared by TikTok event handlers.
 *
 * @param chatGateway rendered event sink
 * @param sessionEventLogger local session logger
 * @param alertService local alert dispatcher
 * @param languageSupplier effective language supplier
 * @param runtimeLanguageSupplier whether fixed text should use Minecraft runtime translations
 */
public record TikTokRuntimeServices(
        ChatEventSink chatGateway,
        SessionEventLogger sessionEventLogger,
        AlertService alertService,
        Supplier<String> languageSupplier,
        Supplier<Boolean> runtimeLanguageSupplier
) {
    public TikTokRuntimeServices(
            ChatEventSink chatGateway,
            SessionEventLogger sessionEventLogger,
            AlertService alertService
    ) {
        this(chatGateway, sessionEventLogger, alertService, () -> LanguageSetting.DEFAULT_LOCALE, () -> true);
    }
}
