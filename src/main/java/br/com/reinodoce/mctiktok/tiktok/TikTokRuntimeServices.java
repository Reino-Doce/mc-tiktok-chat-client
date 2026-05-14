package br.com.reinodoce.mctiktok.tiktok;

import br.com.reinodoce.mctiktok.alert.AlertService;
import br.com.reinodoce.mctiktok.alert.AlertSink;
import br.com.reinodoce.mctiktok.chat.ChatEventSink;
import br.com.reinodoce.mctiktok.config.LanguageSetting;
import br.com.reinodoce.mctiktok.logging.SessionEventLogger;
import br.com.reinodoce.mctiktok.pinned.PinnedMessageSink;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Runtime side-effect services shared by TikTok event handlers.
 *
 * @param chatGateway rendered event sink
 * @param sessionEventLogger local session logger
 * @param alertService local alert dispatcher
 * @param pinnedMessageSink local pinned-message overlay sink
 * @param languageSupplier effective language supplier
 * @param runtimeLanguageSupplier whether fixed text should use Minecraft runtime translations
 */
public record TikTokRuntimeServices(
        ChatEventSink chatGateway,
        SessionEventLogger sessionEventLogger,
        AlertService alertService,
        PinnedMessageSink pinnedMessageSink,
        Supplier<String> languageSupplier,
        Supplier<Boolean> runtimeLanguageSupplier
) {
    public TikTokRuntimeServices(
            ChatEventSink chatGateway,
            SessionEventLogger sessionEventLogger,
            AlertService alertService,
            Supplier<String> languageSupplier,
            Supplier<Boolean> runtimeLanguageSupplier
    ) {
        this(
                chatGateway,
                sessionEventLogger,
                alertService,
                PinnedMessageSink.noop(),
                languageSupplier,
                runtimeLanguageSupplier);
    }

    public TikTokRuntimeServices(
            ChatEventSink chatGateway,
            SessionEventLogger sessionEventLogger,
            AlertService alertService
    ) {
        this(
                chatGateway,
                sessionEventLogger,
                alertService,
                PinnedMessageSink.noop(),
                () -> LanguageSetting.DEFAULT_LOCALE,
                () -> true);
    }

    /**
     * Runtime side-effect boundaries before language suppliers are attached by the core service.
     *
     * @param chatGateway rendered event sink
     * @param alertSink local alert sink
     * @param pinnedMessageSink local pinned-message overlay sink
     * @param sessionLogDirectory local session log directory
     */
    public record SideEffects(
            ChatEventSink chatGateway,
            AlertSink alertSink,
            PinnedMessageSink pinnedMessageSink,
            Path sessionLogDirectory
    ) {
        public SideEffects(ChatEventSink chatGateway, AlertSink alertSink, Path sessionLogDirectory) {
            this(chatGateway, alertSink, PinnedMessageSink.noop(), sessionLogDirectory);
        }

        public TikTokRuntimeServices withLanguageSuppliers(
                Supplier<String> languageSupplier,
                Supplier<Boolean> runtimeLanguageSupplier
        ) {
            return new TikTokRuntimeServices(
                    Objects.requireNonNull(chatGateway, "chatGateway"),
                    new SessionEventLogger(Objects.requireNonNull(sessionLogDirectory, "sessionLogDirectory")),
                    new AlertService(Objects.requireNonNull(alertSink, "alertSink")),
                    Objects.requireNonNull(pinnedMessageSink, "pinnedMessageSink"),
                    languageSupplier,
                    runtimeLanguageSupplier);
        }
    }
}
