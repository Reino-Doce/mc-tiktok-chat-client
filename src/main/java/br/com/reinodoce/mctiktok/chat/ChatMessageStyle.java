package br.com.reinodoce.mctiktok.chat;

import br.com.reinodoce.mctiktok.config.ChatFormatTemplate;
import br.com.reinodoce.mctiktok.config.ReinodoceConfig;

/**
 * Chat presentation settings used by the formatter.
 *
 * @param prefix visible marker for external LIVE-originated messages
 * @param format validated message template
 */
public record ChatMessageStyle(String prefix, String format) {
    /**
     * Normalizes style values to safe defaults.
     *
     * @param prefix visible marker for external LIVE-originated messages
     * @param format message template
     */
    public ChatMessageStyle {
        prefix = prefix == null || prefix.isBlank() ? ReinodoceConfig.DEFAULT_CHAT_PREFIX : prefix.trim();
        format = ChatFormatTemplate.sanitize(format);
    }

    /**
     * Creates a style snapshot from persisted config.
     *
     * @param config source configuration
     * @return normalized chat style
     */
    public static ChatMessageStyle from(ReinodoceConfig config) {
        if (config == null) {
            return new ChatMessageStyle(ReinodoceConfig.DEFAULT_CHAT_PREFIX, ReinodoceConfig.DEFAULT_CHAT_FORMAT);
        }
        return new ChatMessageStyle(config.getChatPrefix(), config.getChatFormat());
    }
}
