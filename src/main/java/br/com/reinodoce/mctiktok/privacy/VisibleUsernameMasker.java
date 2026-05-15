package br.com.reinodoce.mctiktok.privacy;

import br.com.reinodoce.mctiktok.chat.RichLiveMessage;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

import java.util.List;

/**
 * Applies username masking to local mirrored output without changing source events.
 */
public final class VisibleUsernameMasker {
    /** Placeholder shown when visible username masking is enabled. */
    public static final String MASKED_USERNAME = "viewer";

    private VisibleUsernameMasker() {
    }

    /**
     * Returns a username suitable for visible mirrored output.
     *
     * @param config runtime configuration
     * @param username original username
     * @return masked or original username
     */
    public static String username(ReinodoceConfig config, String username) {
        String safeUsername = username == null ? "" : username;
        return config != null && config.isMaskUsernamesInOutput() ? MASKED_USERNAME : safeUsername;
    }

    /**
     * Returns a rich message suitable for visible mirrored output.
     *
     * @param config runtime configuration
     * @param message original rich message
     * @return masked or original message
     */
    public static RichLiveMessage message(ReinodoceConfig config, RichLiveMessage message) {
        if (message == null || config == null || !config.isMaskUsernamesInOutput()) {
            return message;
        }
        return message.withAuthorSegments(List.of(new RichLiveMessage.TextSegment(MASKED_USERNAME)));
    }
}
